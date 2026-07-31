package com.my.vpn.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HappLinkResolverTest {

    @Test
    fun resolve_happAdd_extractsHttpsUrl() {
        val r = HappLinkResolver.resolve("happ://add/https://example.com/sub.txt")
        assertTrue(r is HappLinkResolver.Result.SubscriptionUrl)
        assertEquals(
            "https://example.com/sub.txt",
            (r as HappLinkResolver.Result.SubscriptionUrl).url
        )
    }

    @Test
    fun resolve_crypt5_isEncrypted() {
        val r = HappLinkResolver.resolve("happ://crypt5/abc123")
        assertTrue(r is HappLinkResolver.Result.Encrypted)
    }

    @Test
    fun normalize_plainUrl_unchanged() {
        val url = "https://cdn.example.com/list"
        assertEquals(url, HappLinkResolver.normalizeSubscriptionInput(url))
    }
}
