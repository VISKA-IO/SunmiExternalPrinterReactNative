package com.sunmiexternalprinter

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.net.InetAddress

class ResolveListener (private val reactContext:ReactContext): NsdManager.ResolveListener {
  private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }
  override fun onResolveFailed(serviceInfo: NsdServiceInfo?, p1: Int) {

  }

  override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
    val port: Int? = serviceInfo?.port
    val host: InetAddress? = serviceInfo?.host
    val serviceName = serviceInfo?.serviceName
    val payload = Arguments.createMap().apply {
      putString("Service Discovery", serviceName)
      putString("ip", host?.hostAddress)
      putString("port", port.toString())
    }
    sendEvent(reactContext, "OnPrinterFound", payload)
  }
}
