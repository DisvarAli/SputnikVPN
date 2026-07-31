package com.my.vpn.data.model

import java.util.UUID

data class CustomSubscription(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val inlineContent: String? = null,
    val traffic: SubscriptionTraffic = SubscriptionTraffic()
) {
    val isInlineImport: Boolean
        get() = !inlineContent.isNullOrBlank()
}
