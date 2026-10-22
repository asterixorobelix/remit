package com.asterixorobelix.remit.platform

/**
 * Removes personal data and credentials from text that leaves the device (crash reports): email
 * addresses, bearer tokens, JWTs such as Firebase ID tokens, Google API keys, and `name=value` or
 * `name: value` secrets (token, password, secret, API key, key, authorization).
 */
object Redaction {
    private val rules = listOf(
        Regex("""bearer\s+[A-Za-z0-9._~+/=-]+""", RegexOption.IGNORE_CASE) to "Bearer [redacted]",
        Regex("""eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*""") to "[jwt]",
        Regex("""AIza[0-9A-Za-z_-]{35}""") to "[api-key]",
        Regex(
            """\b(id_?token|refresh_?token|token|password|passwd|secret|api[_-]?key|key|authorization)""" +
                """(["']?\s*[=:]\s*["']?)[^\s,&;"']+""",
            RegexOption.IGNORE_CASE,
        ) to "$1$2[redacted]",
        Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""") to "[email]",
    )

    fun redact(text: String): String = rules.fold(text) { result, (pattern, replacement) ->
        pattern.replace(result, replacement)
    }
}

/**
 * Stands in for an exception whose message (or a cause's) held personal data or a credential.
 * [originalType] keeps the real exception class visible in the report.
 */
class RedactedException(val originalType: String, message: String?, cause: Throwable?) :
    Exception(if (message == null) originalType else "$originalType: $message", cause)

/**
 * This throwable with [Redaction] applied to its message and its causes' messages. Returns it
 * unchanged when nothing needed redacting, so reports keep the original exception type; otherwise
 * a [RedactedException] with the same stack trace, so crash reports still group the same way.
 */
fun Throwable.redacted(): Throwable = redacted(depth = 0)

private fun Throwable.redacted(depth: Int): Throwable {
    val original = message
    val safeMessage = original?.let(Redaction::redact)
    val safeCause = cause?.takeIf { it !== this && depth < MAX_CAUSE_DEPTH }?.redacted(depth + 1) ?: cause
    if (safeMessage == original && safeCause === cause) return this
    val type = this::class.qualifiedName ?: this::class.simpleName ?: "Throwable"
    return RedactedException(type, safeMessage, safeCause).also { copyStackTraceTo(it) }
}

/** Causes deeper than this are passed on as they are; real chains are a handful long. */
private const val MAX_CAUSE_DEPTH = 8

/** Gives [target] this throwable's stack trace where the platform allows it (the JVM). */
internal expect fun Throwable.copyStackTraceTo(target: Throwable)

/**
 * Applies [Redaction] to everything a crash logger sends: exceptions and their causes, messages,
 * and custom key values. `observabilityModule` wraps every platform's crash logger in it, so no
 * caller has to remember to redact.
 */
class RedactingCrashLogger(private val delegate: CrashLogger) : CrashLogger by delegate {
    override fun logException(throwable: Throwable, message: String?) =
        delegate.logException(throwable.redacted(), message?.let(Redaction::redact))

    override fun setCustomKey(key: String, value: String) = delegate.setCustomKey(key, Redaction.redact(value))

    override fun log(message: String) = delegate.log(Redaction.redact(message))
}
