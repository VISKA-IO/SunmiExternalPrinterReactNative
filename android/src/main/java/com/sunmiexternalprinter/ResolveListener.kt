package com.sunmiexternalprinter

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.net.InetAddress

/**
 * Network Service Discovery resolve listener for TCP/IP printer discovery.
 * 
 * This class handles the resolution of network services discovered during
 * NSD scanning. When a printer service is found, the system first discovers
 * its name, then uses this resolver to get detailed connection information
 * including IP address and port number.
 * 
 * Successfully resolved printers trigger "OnPrinterFound" events that are
 * sent to the React Native JavaScript layer for UI updates.
 * 
 * @param reactContext React Native context for event emission
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
class ResolveListener (private val reactContext:ReactContext): NsdManager.ResolveListener {
  
  /**
   * Sends events from native Android to React Native JavaScript layer.
   * 
   * @param reactContext The React application context
   * @param eventName Name of the event to emit
   * @param params Parameters to send with the event
   */
  private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }
  
  /**
   * Called when service resolution fails.
   * 
   * This can happen due to network issues, service unavailability,
   * or timeout conditions. Currently handles failures silently.
   * 
   * @param serviceInfo The service that failed to resolve
   * @param errorCode Error code indicating the failure reason
   */
  override fun onResolveFailed(serviceInfo: NsdServiceInfo?, p1: Int) {
    // Handle resolution failure silently
  }

  /**
   * Called when a network service is successfully resolved.
   * 
   * Extracts connection details (IP address, port, service name) from the
   * resolved service and emits an "OnPrinterFound" event to React Native
   * with the printer information.
   * 
   * @param serviceInfo Resolved service information containing connection details
   */
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
