package com.my.vpn

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.my.vpn.vpn.VpnController
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VpnControllerInstrumentedTest {

    @Test
    fun vpnController_defaultState_isDisconnected() {
        assertFalse(VpnController.isConnected)
    }
}
