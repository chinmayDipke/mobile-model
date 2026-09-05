package com.stackunderflow.airgapspike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Airgap model-load spike.
 *
 * The ONLY goal: prove Gemma loads on this handset and returns text,
 * and capture the two numbers the judges will ask for
 * (cold load time, single inference time) - Team-Handover.md section 5.
 */
class MainActivity : ComponentActivity() {

    companion object {
        // Same paths on both phones. Chinmay's phone has these too.
        const val SAFE_MODEL = "/data/local/tmp/llm/gemma3-1b-int4.task"
        const val NPU_MODEL  = "/data/local/tmp/llm/gemma3-1b-sm8850.litertlm"

        // One of the real scam messages from testset.json (S01)
        const val PROMPT = """You are a fraud detector for Indian bank SMS.
Answer with ONE word only: SCAM or CLEAN.

Message: "Dear Customer, your SBI YONO account KYC has EXPIRED. Account will be blocked within 24hrs. Update now: http://sbi-kyc-verify.in/update"

Answer:"""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Screen() } }
    }

    @Composable
    fun Screen() {
        var log by remember { mutableStateOf("Ready.\n\nTap a button to load the model.") }
        var busy by remember { mutableStateOf(false) }

        fun run(path: String, label: String) {
            busy = true
            log = "Loading $label ...\nThis can take a while the first time. Do not close the app."
            lifecycleScope.launch {
                val out = withContext(Dispatchers.IO) { loadAndAsk(path, label) }
                log = out
                busy = false
            }
        }

        fun runLiteRt(path: String, useNpu: Boolean) {
            busy = true
            val what = if (useNpu) "NPU (Hexagon HTP)" else "CPU"
            log = "Loading .litertlm on " + what + " ...\nFirst NPU run compiles the graph. This can take several minutes. Do not close the app."
            lifecycleScope.launch {
                val r = withContext(Dispatchers.IO) {
                    NpuRunner.run(this@MainActivity, path, PROMPT, useNpu)
                }
                log = buildString {
                    appendLine("=== LiteRT-LM / ${r.backend} ===")
                    appendLine(path)
                    appendLine()
                    if (r.ok) {
                        appendLine("COLD LOAD : ${r.loadMs} ms  (${"%.1f".format(r.loadMs / 1000.0)} s)")
                        appendLine("INFERENCE : ${r.inferMs} ms  (${"%.1f".format(r.inferMs / 1000.0)} s)")
                        appendLine()
                        appendLine("MODEL SAID: ${r.answer}")
                        appendLine()
                        appendLine(">>> WRITE THESE TWO NUMBERS DOWN <<<")
                    } else {
                        appendLine("FAIL on ${r.backend}")
                        appendLine(r.error ?: "unknown")
                    }
                }
                busy = false
            }
        }

        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Airgap - model spike", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(
                    "Goal: does the model answer at all on this phone?",
                    fontSize = 13.sp
                )
                Button(
                    onClick = { run(SAFE_MODEL, "SAFE .task") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("1. Load SAFE model (.task)") }

                Button(
                    onClick = { run(NPU_MODEL, "NPU .litertlm") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("2. Load NPU model (.litertlm)") }

                Button(
                    onClick = { runLiteRt(NPU_MODEL, useNpu = true) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("3. NPU via LiteRT-LM  <-- THE REAL ONE") }

                Button(
                    onClick = { runLiteRt(NPU_MODEL, useNpu = false) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("4. Same file on CPU (for comparison)") }

                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

                Card(Modifier.fillMaxWidth()) {
                    Text(
                        log,
                        Modifier.padding(14.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    /** Loads the model, asks one question, returns a report with both timings. */
    private fun loadAndAsk(path: String, label: String): String {
        val sb = StringBuilder()
        sb.appendLine("=== $label ===")
        sb.appendLine(path)

        val f = File(path)
        if (!f.exists()) return sb.appendLine("\nFAIL: file not found on phone.").toString()
        if (!f.canRead()) return sb.appendLine("\nFAIL: file exists but app cannot read it.").toString()
        sb.appendLine("size: ${f.length()} bytes")
        sb.appendLine()

        var llm: LlmInference? = null
        return try {
            // ---- number 1: cold load ----
            val t0 = System.currentTimeMillis()
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(256)
                .build()
            llm = LlmInference.createFromOptions(this, options)
            val loadMs = System.currentTimeMillis() - t0

            // ---- number 2: one inference ----
            val t1 = System.currentTimeMillis()
            val answer = llm.generateResponse(PROMPT)
            val inferMs = System.currentTimeMillis() - t1

            sb.appendLine("COLD LOAD : $loadMs ms  (${"%.1f".format(loadMs / 1000.0)} s)")
            sb.appendLine("INFERENCE : $inferMs ms  (${"%.1f".format(inferMs / 1000.0)} s)")
            sb.appendLine()
            sb.appendLine("--- prompt was a real scam SMS (testset S01) ---")
            sb.appendLine("MODEL SAID: ${answer.trim()}")
            sb.appendLine()
            sb.appendLine(">>> WRITE THESE TWO NUMBERS DOWN <<<")
            sb.toString()
        } catch (t: Throwable) {
            sb.appendLine("FAIL: ${t::class.java.simpleName}")
            sb.appendLine(t.message ?: "no message")
            sb.toString()
        } finally {
            try { llm?.close() } catch (_: Throwable) {}
        }
    }
}
