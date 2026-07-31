package com.my.vpn.data.repository

import com.my.vpn.data.model.SubscriptionListType

/** Ограничение пинга/обновления одной подпиской, а не всем кэшем. */
sealed interface ConfigPingScope {
    data class Custom(val id: String) : ConfigPingScope
    data class Builtin(val type: SubscriptionListType) : ConfigPingScope
}
