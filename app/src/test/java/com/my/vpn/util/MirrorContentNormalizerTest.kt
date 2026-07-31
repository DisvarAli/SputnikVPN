package com.my.vpn.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MirrorContentNormalizerTest {

    @Test
    fun normalize_keepsPlainSubscription() {
        val raw = "vless://uuid@host:443?encryption=none#test\nss://method:pass@host:8388#ss"
        assertEquals(raw, MirrorContentNormalizer.normalize(raw))
    }

    @Test
    fun normalize_extractsLinksFromHtml() {
        val html = """
            <html><body>
            <pre>vless://abc@1.2.3.4:443?encryption=none#node
            trojan://pw@5.6.7.8:443#t</pre>
            </body></html>
        """.trimIndent()
        val out = MirrorContentNormalizer.normalize(html)
        assertTrue(out.contains("vless://"))
        assertTrue(out.contains("trojan://"))
    }

    @Test
    fun looksLikeSubscription_detectsShareLinks() {
        assertTrue(ConfigFetchBypass.looksLikeSubscription("vless://x@h:1"))
        assertTrue(ConfigFetchBypass.looksLikeSubscription("<html>ss://abc@h:1</html>"))
        assertFalse(ConfigFetchBypass.looksLikeSubscription("hello"))
    }
}
