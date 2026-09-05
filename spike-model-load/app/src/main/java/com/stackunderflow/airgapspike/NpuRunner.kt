package com.stackunderflow.airgapspike

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Runs a .litertlm model through the LiteRT-LM runtime.
 *
 * WHY THIS EXISTS:
 * MediaPipe tasks-genai cannot load the NPU-targeted .litertlm build - it only
 * ships the XNNPACK (CPU) executor, which rejects the file with
 * "Unsupported model signature" (llm_litert_xnnpack_executor.cc:183).
 * The .litertlm format needs com.google.ai.edge.litertlm, where you can ask for
 * Backend.NPU explicitly. That is the Hexagon Tensor Processor path on SM8850.
 *
 * Same API is used by Google's own AI Edge Gallery app.
 */
object NpuRunner {

    /** Result of one load+infer run. */
    data class Run(
        val ok: Boolean,
        val backend: String,
        val loadMs: Long,
        val inferMs: Long,
        val answer: String,
        val error: String? = null,
    )

    fun run(context: Context, path: String, prompt: String, useNpu: Boolean): Run {
        val backendName = if (useNpu) "NPU (Hexagon HTP)" else "CPU"
        val f = File(path)
        if (!f.exists()) return Run(false, backendName, 0, 0, "", "file not found: $path")

        var engine: Engine? = null
        return try {
            val backend =
                if (useNpu) Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
                else Backend.CPU()

            // The runtime cannot write next to a model in /data/local/tmp,
            // so give it a writable cache dir. The Gallery does the same.
            val cacheDir =
                if (path.startsWith("/data/local/tmp")) context.getExternalFilesDir(null)?.absolutePath
                else null

            // ---- number 1: cold load ----
            val t0 = System.currentTimeMillis()
            engine = Engine(
                EngineConfig(
                    modelPath = path,
                    backend = backend,
                    maxNumTokens = 256,
                    cacheDir = cacheDir,
                )
            )
            engine.initialize()
            val conversation = engine.createConversation(ConversationConfig(samplerConfig = null))
            val loadMs = System.currentTimeMillis() - t0

            // ---- number 2: one inference ----
            val sb = StringBuilder()
            val latch = CountDownLatch(1)
            var err: Throwable? = null

            val t1 = System.currentTimeMillis()
            conversation.sendMessageAsync(
                Contents.of(prompt),
                object : MessageCallback {
                    override fun onMessage(message: Message) { sb.append(message.toString()) }
                    override fun onDone() { latch.countDown() }
                    override fun onError(throwable: Throwable) { err = throwable; latch.countDown() }
                },
            )
            val finished = latch.await(120, TimeUnit.SECONDS)
            val inferMs = System.currentTimeMillis() - t1

            when {
                !finished -> Run(false, backendName, loadMs, inferMs, "", "timed out after 120s")
                err != null -> Run(false, backendName, loadMs, inferMs, "", "${err!!::class.java.simpleName}: ${err!!.message}")
                else -> Run(true, backendName, loadMs, inferMs, sb.toString().trim())
            }
        } catch (t: Throwable) {
            Run(false, backendName, 0, 0, "", "${t::class.java.simpleName}: ${t.message}")
        } finally {
            try { engine?.close() } catch (_: Throwable) {}
        }
    }
}
