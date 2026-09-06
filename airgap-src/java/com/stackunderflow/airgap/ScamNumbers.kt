package com.stackunderflow.airgap

import android.content.Context

/**
 * Phone numbers that appeared inside a message we blocked.
 *
 * The gap this closes: a fake-customer-care SMS says "call 9835442621". We stop
 * the message, and then the person rings that number anyway, and the actual
 * theft happens on the call. We already knew the number was dangerous and said
 * nothing.
 *
 * Stored as the last ten digits so +91, 0-prefixed and plain forms all match.
 * Numbers only - no message text, nothing that leaves the phone.
 */
object ScamNumbers {

    private const val PREFS = "airgap_scam_numbers"
    private const val KEY = "numbers"
    private const val MAX = 200

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Last 10 digits, which is what actually identifies an Indian number. */
    fun key(raw: String): String = raw.filter { it.isDigit() }.takeLast(10)

    @Synchronized
    fun remember(c: Context, numbers: List<String>) {
        if (numbers.isEmpty()) return
        try {
            val keys = numbers.map(::key).filter { it.length == 10 }
            if (keys.isEmpty()) return
            val p = prefs(c)
            val all = LinkedHashSet(p.getStringSet(KEY, emptySet()) ?: emptySet())
            all.addAll(keys)
            val trimmed = if (all.size > MAX) all.toList().takeLast(MAX).toSet() else all
            p.edit().putStringSet(KEY, trimmed).apply()
        } catch (_: Throwable) {
            // Never allowed to break detection.
        }
    }

    @Synchronized
    fun isKnown(c: Context, raw: String?): Boolean = try {
        val k = key(raw.orEmpty())
        k.length == 10 && (prefs(c).getStringSet(KEY, emptySet()) ?: emptySet()).contains(k)
    } catch (_: Throwable) {
        false
    }

    fun count(c: Context): Int = try {
        (prefs(c).getStringSet(KEY, emptySet()) ?: emptySet()).size
    } catch (_: Throwable) {
        0
    }
}
