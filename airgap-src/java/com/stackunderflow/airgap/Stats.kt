package com.stackunderflow.airgap

import android.content.Context

/**
 * Counters, and nothing else. No message text ever reaches this file.
 *
 * This exists so the answer to "does it read all my messages?" can be a number
 * on the screen instead of a promise:
 *
 *     147  messages arrived
 *      12  examined      (they mentioned money or asked you to act)
 *     135  ignored       (never read past the first check)
 *       1  blocked
 *
 * SharedPreferences rather than a file because four integers do not deserve
 * more machinery than that.
 */
object Stats {

    private const val PREFS = "airgap_stats"
    private const val ARRIVED = "arrived"
    private const val EXAMINED = "examined"
    private const val BLOCKED = "blocked"

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun recordArrival(c: Context, examined: Boolean) {
        try {
            val p = prefs(c)
            p.edit()
                .putInt(ARRIVED, p.getInt(ARRIVED, 0) + 1)
                .putInt(EXAMINED, p.getInt(EXAMINED, 0) + if (examined) 1 else 0)
                .apply()
        } catch (_: Throwable) {
            // Counters must never be able to break detection.
        }
    }

    @Synchronized
    fun recordBlocked(c: Context) {
        try {
            val p = prefs(c)
            p.edit().putInt(BLOCKED, p.getInt(BLOCKED, 0) + 1).apply()
        } catch (_: Throwable) {
        }
    }

    data class Snapshot(val arrived: Int, val examined: Int, val ignored: Int, val blocked: Int)

    fun snapshot(c: Context): Snapshot = try {
        val p = prefs(c)
        val a = p.getInt(ARRIVED, 0)
        val e = p.getInt(EXAMINED, 0)
        Snapshot(a, e, a - e, p.getInt(BLOCKED, 0))
    } catch (_: Throwable) {
        Snapshot(0, 0, 0, 0)
    }

    fun reset(c: Context) {
        try { prefs(c).edit().clear().apply() } catch (_: Throwable) {}
    }
}
