package com.stackunderflow.airgap

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * What Airgap has blocked, kept on this phone and nowhere else.
 *
 * Two reasons this exists:
 *
 *  - Without it the product is a warning that flashes and vanishes. A user has
 *    no way to look back and see what was caught, and no reason to trust that
 *    anything is happening when nothing is being blocked.
 *
 *  - It restates the privacy claim where someone will actually look. Every
 *    other scam app that keeps a history keeps it on a server. This one is a
 *    file in the app's own directory, which Android does not let other apps
 *    read, and it never leaves the handset.
 *
 * Deliberately a plain JSON file, not a database: fifty rows do not need Room,
 * and a dependency we do not add is a dependency that cannot fail on stage.
 */
object History {

    private const val FILE = "history.json"
    private const val MAX = 50

    data class Entry(
        val at: Long,
        val pattern: String,
        val reason: String,
        val message: String,
        val engine: String,
        val source: String,
    )

    private fun file(c: Context) = File(c.applicationContext.filesDir, FILE)

    @Synchronized
    fun record(c: Context, verdict: Verdict, message: String, source: String) {
        if (!verdict.isScam) return
        try {
            val arr = readArray(c)
            arr.put(
                JSONObject()
                    .put("at", System.currentTimeMillis())
                    .put("pattern", verdict.pattern)
                    .put("reason", verdict.reason)
                    // Truncated on purpose. We need enough to recognise it, not
                    // a full copy of someone's messages sitting on disk.
                    .put("message", message.take(160))
                    .put("engine", verdict.let { Airgap.detector.engineName })
                    .put("source", source)
            )
            // Keep the newest MAX, drop the rest.
            val trimmed = JSONArray()
            val from = maxOf(0, arr.length() - MAX)
            for (i in from until arr.length()) trimmed.put(arr.getJSONObject(i))
            file(c).writeText(trimmed.toString())
        } catch (_: Throwable) {
            // History is a nice-to-have. It must never break detection.
        }
    }

    /** Newest first. */
    @Synchronized
    fun all(c: Context): List<Entry> = try {
        val arr = readArray(c)
        (0 until arr.length()).map { arr.getJSONObject(it) }.map {
            Entry(
                at = it.optLong("at"),
                pattern = it.optString("pattern"),
                reason = it.optString("reason"),
                message = it.optString("message"),
                engine = it.optString("engine"),
                source = it.optString("source"),
            )
        }.reversed()
    } catch (_: Throwable) {
        emptyList()
    }

    @Synchronized
    fun clear(c: Context) {
        try { file(c).delete() } catch (_: Throwable) {}
    }

    private fun readArray(c: Context): JSONArray {
        val f = file(c)
        if (!f.exists()) return JSONArray()
        return JSONArray(f.readText())
    }
}
