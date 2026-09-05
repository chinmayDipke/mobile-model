package com.stackunderflow.airgap

import android.content.Context
import org.json.JSONObject

/** Result of running the 55 labelled messages through whichever detector is plugged in. */
data class TestSetResult(
    val scamTotal: Int,
    val scamCaught: Int,
    val genuineTotal: Int,
    val genuinePassed: Int,
    val falseAlarms: List<String>,
    val missed: List<String>,
    val millisTotal: Long
) {
    val summary: String
        get() = "Scams caught " + scamCaught + "/" + scamTotal +
                "   Clean passed " + genuinePassed + "/" + genuineTotal +
                "   " + millisTotal + " ms"
}

/**
 * Gives us real numbers to show judges instead of adjectives.
 * Works with the stub today and with Shan's model later, unchanged.
 */
object TestSetRunner {

    fun run(context: Context, detector: Detector): TestSetResult =
        run(context, detector, "testset.json")

    fun run(context: Context, detector: Detector, asset: String): TestSetResult {
        val raw = context.assets.open(asset).bufferedReader().use { it.readText() }
        val root = JSONObject(raw)

        var scamTotal = 0
        var scamCaught = 0
        var genuineTotal = 0
        var genuinePassed = 0
        val falseAlarms = mutableListOf<String>()
        val missed = mutableListOf<String>()

        val started = System.currentTimeMillis()

        val scam = root.getJSONArray("scam")
        for (i in 0 until scam.length()) {
            val o = scam.getJSONObject(i)
            scamTotal++
            // Through GATE 0 first, exactly as a real message would be. A scam
            // the scope filter drops never reaches the detector, so it must
            // count as missed - otherwise the suite cannot see a filter that is
            // too aggressive, which is the whole risk of having one.
            val text = o.getString("text")
            val inScope = ScopeFilter.decide("VK-ALERTS", text).examine
            val v = if (inScope) detector.check(Normaliser.normalise("VK-ALERTS", text))
                    else Verdict.clean()
            if (v.isScam) scamCaught++
            else missed.add(o.getString("id") + " " + o.getString("pattern") +
                            if (!inScope) " [FILTERED OUT]" else "")
        }

        val genuine = root.getJSONArray("genuine")
        for (i in 0 until genuine.length()) {
            val o = genuine.getJSONObject(i)
            genuineTotal++
            val text = o.getString("text")
            val v = if (ScopeFilter.decide("VM-SBIINB", text).examine)
                        detector.check(Normaliser.normalise("VM-SBIINB", text))
                    else Verdict.clean()
            if (!v.isScam) genuinePassed++ else falseAlarms.add(o.getString("id") + " -> " + v.pattern)
        }

        return TestSetResult(
            scamTotal, scamCaught, genuineTotal, genuinePassed,
            falseAlarms, missed, System.currentTimeMillis() - started
        )
    }
}
