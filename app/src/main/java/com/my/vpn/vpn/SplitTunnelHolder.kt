package com.my.vpn.vpn

import com.my.vpn.data.model.SplitTunnelMode

object SplitTunnelHolder {
    @Volatile
    var mode: SplitTunnelMode = SplitTunnelMode.FULL_TUNNEL

    @Volatile
    var allowedPackages: Set<String> = emptySet()
}
