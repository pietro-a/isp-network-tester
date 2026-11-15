package space.antonov.ispnetworktester.validation

import java.net.IDN
import java.net.Inet6Address
import java.net.InetAddress

object NetworkAddressValidator {
    fun isValidHost(value: String): Boolean {
        if (value.isEmpty() || value != value.trim() || value.any(Char::isWhitespace)) {
            return false
        }

        if (value.contains(':')) {
            return isIpv6Address(value)
        }

        if (value.all { it in '0'..'9' || it == '.' }) {
            return isIpv4Address(value)
        }

        val hostname = try {
            IDN.toASCII(value.removeSuffix("."), IDN.USE_STD3_ASCII_RULES)
        } catch (_: IllegalArgumentException) {
            return false
        }

        if (hostname.isEmpty() || hostname.length > 253) {
            return false
        }

        return hostname.split('.').all { label ->
            label.isNotEmpty() &&
                label.length <= 63 &&
                label.first().isLetterOrDigit() &&
                label.last().isLetterOrDigit()
        }
    }

    fun isValidPort(value: String): Boolean =
        value.isNotEmpty() &&
            value.all { it in '0'..'9' } &&
            value.toIntOrNull()?.let { it in 1..65535 } == true

    private fun isIpv4Address(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() &&
                part.length <= 3 &&
                part.all { it in '0'..'9' } &&
                part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }

    private fun isIpv6Address(value: String): Boolean {
        if ('%' in value || value.startsWith('[') || value.endsWith(']')) return false
        return try {
            InetAddress.getByName(value) is Inet6Address
        } catch (_: Exception) {
            false
        }
    }
}
