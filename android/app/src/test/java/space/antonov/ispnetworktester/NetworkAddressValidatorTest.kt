package space.antonov.ispnetworktester

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import space.antonov.ispnetworktester.validation.NetworkAddressValidator

class NetworkAddressValidatorTest {
    @Test
    fun acceptsHostnamesAndInternationalizedDomainNames() {
        listOf(
            "example.com",
            "test-server.example.net",
            "router",
            "localhost",
            "example.com.",
            "bücher.example",
            "xn--bcher-kva.example",
        ).forEach { address ->
            assertTrue("$address should be valid", NetworkAddressValidator.isValidHost(address))
        }
    }

    @Test
    fun acceptsHostnamesAtDnsLengthBoundaries() {
        val maximumLengthHostname = listOf(
            "a".repeat(63),
            "b".repeat(63),
            "c".repeat(63),
            "d".repeat(61),
        ).joinToString(".")

        val tooLongHostname = listOf(
            "a".repeat(63),
            "b".repeat(63),
            "c".repeat(63),
            "d".repeat(62),
        ).joinToString(".")

        assertTrue(NetworkAddressValidator.isValidHost(maximumLengthHostname))
        assertFalse(NetworkAddressValidator.isValidHost(tooLongHostname))
        assertTrue(NetworkAddressValidator.isValidHost("a".repeat(63) + ".example"))
        assertFalse(NetworkAddressValidator.isValidHost("a".repeat(64) + ".example"))
    }

    @Test
    fun rejectsMalformedHostnames() {
        listOf(
            "",
            " ",
            " example.com",
            "example.com ",
            "example.com\n",
            "bad host",
            "example..com",
            ".example.com",
            "example.com..",
            "-invalid.example",
            "invalid-.example",
            "invalid_label.example",
            "https://example.com",
            "example.com:8080",
            "example.com/path",
            "123",
        ).forEach { address ->
            assertFalse("$address should be invalid", NetworkAddressValidator.isValidHost(address))
        }
    }

    @Test
    fun acceptsIpv4AddressesAtOctetBoundaries() {
        listOf(
            "0.0.0.0",
            "127.0.0.1",
            "192.168.1.1",
            "255.255.255.255",
        ).forEach { address ->
            assertTrue("$address should be valid", NetworkAddressValidator.isValidHost(address))
        }
    }

    @Test
    fun rejectsMalformedIpv4Addresses() {
        listOf(
            "192.168.1",
            "192.168.1.1.1",
            "256.1.1.1",
            "192.168.1.256",
            "192.168.-1.1",
            "192.168.1.",
            ".192.168.1.1",
            "192..1.1",
            "1.2.3.1234",
        ).forEach { address ->
            assertFalse("$address should be invalid", NetworkAddressValidator.isValidHost(address))
        }
    }

    @Test
    fun acceptsCompressedAndExpandedIpv6Addresses() {
        listOf(
            "::1",
            "::",
            "2001:db8::1",
            "2001:db8::",
            "2001:0db8:0000:0000:0000:ff00:0042:8329",
            "1:2:3:4:5:6:7:8",
        ).forEach { address ->
            assertTrue("$address should be valid", NetworkAddressValidator.isValidHost(address))
        }
    }

    @Test
    fun rejectsMalformedOrDecoratedIpv6Addresses() {
        listOf(
            "2001:db8:::1",
            "2001:db8::1::1",
            "2001:db8::gg",
            "1:2:3:4:5:6:7",
            "[::1]",
            "[2001:db8::1]",
            "fe80::1%wlan0",
            "fe80::1%3",
        ).forEach { address ->
            assertFalse("$address should be invalid", NetworkAddressValidator.isValidHost(address))
        }
    }

    @Test
    fun acceptsPortsAtRangeBoundariesAndWithLeadingZeros() {
        listOf("1", "80", "8080", "65535", "00080").forEach { port ->
            assertTrue("$port should be valid", NetworkAddressValidator.isValidPort(port))
        }
    }

    @Test
    fun rejectsPortsOutsideRangeOrWithInvalidFormatting() {
        listOf(
            "",
            "0",
            "00000",
            "65536",
            "-1",
            "+80",
            "80.0",
            " 80",
            "80 ",
            "8 0",
            "abc",
            "999999999999999999999999",
        ).forEach { port ->
            assertFalse("$port should be invalid", NetworkAddressValidator.isValidPort(port))
        }
    }
}
