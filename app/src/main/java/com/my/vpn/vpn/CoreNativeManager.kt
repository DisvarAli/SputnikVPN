package com.my.vpn.vpn

import android.content.Context
import com.my.vpn.util.AssetUtil
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.util.concurrent.atomic.AtomicBoolean

object CoreNativeManager {

    private val initialized = AtomicBoolean(false)

    fun initCoreEnv(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            Seq.setContext(context.applicationContext)
            val assetPath = AssetUtil.userAssetPath(context)
            Libv2ray.initCoreEnv(assetPath, AssetUtil.getDeviceIdForXUDPBaseKey(context))
        }
    }

    fun measureOutboundDelay(config: String, testUrl: String): Long {
        return Libv2ray.measureOutboundDelay(config, testUrl)
    }

    fun newCoreController(handler: CoreCallbackHandler): CoreController {
        return Libv2ray.newCoreController(handler)
    }
}
