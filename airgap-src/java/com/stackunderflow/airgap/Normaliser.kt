package com.stackunderflow.airgap

/** A message after the noise is stripped out. */
data class NormalisedMessage(
    val raw: String,
    val sender: String,
    val amount: String?,
    val urls: List<String>,
    val mobileNumbers: List<String>,   // personal 10-digit numbers, suspicious in a bank SMS
    val tollFreeNumbers: List<String>, // 1800-xxx, normal for real banks
    val compact: String                // the short string handed to the model
)

/**
 * Turns a raw SMS into something short and clean.
 * Short input is what keeps model inference fast enough for the notification path.
 */
object Normaliser {

    private val URL = Regex(
        """((?:https?://|www\.)[^\s]+|\b[a-z0-9][a-z0-9-]+\.(?:in|com|xyz|top|net|org|link|info|site)\b(?:/[^\s]*)?)""",
        RegexOption.IGNORE_CASE
    )
    private val AMOUNT = Regex("""(?:rs\.?|inr|\u20B9)\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val TOLL_FREE = Regex("""\b1800[-\s]?[0-9]{3}[-\s]?[0-9]{3,4}\b""")
    // also matches masked numbers like 9835xxxx21, which is how our test set stores them
    private val MOBILE = Regex("""\b(?:\+?91[-\s]?)?(?:[6-9][0-9]{9}|[6-9][0-9]{3}[xX*]{2,6}[0-9]{2,4})\b""")

    /**
     * Scammers write "sbi-kyc-verify [dot] in slash update" so a filter looking
     * for a URL sees plain prose. Undo that before anything reads the text -
     * the stress run found one of these getting through untouched.
     */
    private val DEOBFUSCATE = listOf(
        Regex("""\s*[\[(]\s*dot\s*[\])]\s*""", RegexOption.IGNORE_CASE) to ".",
        Regex("""\s+dot\s+""", RegexOption.IGNORE_CASE) to ".",
        Regex("""\s*[\[(]\s*at\s*[\])]\s*""", RegexOption.IGNORE_CASE) to "@",
        Regex("""\s+slash\s+""", RegexOption.IGNORE_CASE) to "/",
        Regex("""h\s*x\s*x\s*p""", RegexOption.IGNORE_CASE) to "http",
    )

    fun normalise(sender: String, body: String): NormalisedMessage {
        var text = body.replace(Regex("""\s+"""), " ").trim()
        DEOBFUSCATE.forEach { (re, to) -> text = re.replace(text, to) }

        val tollFree = TOLL_FREE.findAll(text).map { it.value }.distinct().toList()
        // remove toll-free hits before hunting for personal mobiles so they do not double count
        var stripped = text
        tollFree.forEach { stripped = stripped.replace(it, " ") }
        val mobiles = MOBILE.findAll(stripped).map { it.value }.distinct().toList()

        val urls = URL.findAll(text).map { it.value.trimEnd('.', ',') }.distinct().toList()
        val amount = AMOUNT.find(text)?.groupValues?.get(1)

        val compact = buildString {
            append("from=").append(sender)
            amount?.let { append(" | amount=").append(it) }
            if (urls.isNotEmpty()) append(" | links=").append(urls.joinToString(","))
            if (mobiles.isNotEmpty()) append(" | mobile=").append(mobiles.joinToString(","))
            append(" | text=").append(text.take(280))
        }

        return NormalisedMessage(text, sender, amount, urls, mobiles, tollFree, compact)
    }
}
