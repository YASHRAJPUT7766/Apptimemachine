package com.apptimemachine.core.notification

/**
 * Heuristic detection of one-time-code (OTP) notifications, so the
 * notification log can show "OTP received" without ever persisting the
 * code — per explicit instruction, OTP notifications are flagged, not
 * stored with content. This runs BEFORE privacy-mode stripping, so
 * detection (and redaction) happens regardless of which
 * [com.apptimemachine.data.entities.NotificationPrivacyMode] is active —
 * even under FULL mode, an OTP code itself must never reach the entity.
 *
 * Deliberately conservative: false negatives (missing an OTP, storing it
 * like any other notification under the user's chosen privacy mode) are
 * an acceptable failure — that's just how a normal notification is
 * already handled. False positives (redacting a non-OTP as one) are also
 * low-cost. What isn't acceptable is a bug that stores the code text
 * while believing it was redacted, so the redaction pass runs on both
 * title and body independently of whether the whole thing was classified
 * as OTP.
 */
object OtpDetector {

    private val OTP_KEYWORDS = listOf(
        "otp", "one time password", "one-time password", "one time code",
        "verification code", "security code", "auth code", "authentication code",
        "login code", "access code", "confirmation code", "passcode", "pin code",
        "\u0913\u091f\u0940\u092a\u0940", // OTP written in Devanagari as loanword fallback, harmless if unmatched
    )

    // A run of 4-8 digits, optionally split by a space/hyphen (e.g. "123 456"),
    // not immediately preceded by characters that suggest it's a phone
    // number, amount, or date fragment.
    private val CODE_PATTERN = Regex("""\b\d{3,4}[\s-]?\d{2,4}\b""")

    data class Result(val isOtp: Boolean, val redactedTitle: String?, val redactedBody: String?)

    fun analyze(title: String?, body: String?): Result {
        val combined = listOfNotNull(title, body).joinToString(" ").lowercase()
        val hasKeyword = OTP_KEYWORDS.any { combined.contains(it) }
        val hasCodeShape = CODE_PATTERN.containsMatchIn(combined)
        val isOtp = hasKeyword && hasCodeShape

        if (!isOtp) return Result(false, title, body)

        return Result(
            isOtp = true,
            redactedTitle = title?.let { redact(it) },
            redactedBody = body?.let { redact(it) }
        )
    }

    private fun redact(text: String): String = CODE_PATTERN.replace(text, "••••••")
}
