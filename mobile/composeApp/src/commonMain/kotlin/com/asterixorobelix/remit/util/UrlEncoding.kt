package com.asterixorobelix.remit.util

private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
private const val HEX_DIGITS = "0123456789ABCDEF"
private const val NIBBLE_BITS = 4
private const val NIBBLE_MASK = 0x0F
private const val BYTE_MASK = 0xFF

/** RFC 3986 percent-encoding of UTF-8 text (spaces become `%20`, as `mailto:` requires). */
fun String.percentEncode(): String = buildString {
    for (byte in this@percentEncode.encodeToByteArray()) {
        val char = byte.toInt().and(BYTE_MASK).toChar()
        if (char in UNRESERVED) {
            append(char)
        } else {
            val value = byte.toInt().and(BYTE_MASK)
            append('%')
            append(HEX_DIGITS[value shr NIBBLE_BITS])
            append(HEX_DIGITS[value and NIBBLE_MASK])
        }
    }
}
