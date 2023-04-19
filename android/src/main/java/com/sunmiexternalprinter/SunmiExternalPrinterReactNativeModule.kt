package com.sunmiexternalprinter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Base64
import com.dantsu.escposprinter.EscPosPrinterCommands
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import java.net.InetAddress
import kotlin.math.floor

class SunmiExternalPrinterReactNativeModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  private var promise: Promise? = null
  private var nsdManager:NsdManager?=null

  private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  private val resolveListener = object : NsdManager.ResolveListener {

    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
      // Called when the resolve fails. Use the error code to debug.

    }

    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {

      val port: Int = serviceInfo.port
      val host: InetAddress = serviceInfo.host
      val serviceName= serviceInfo.serviceName
      val payload=Arguments.createMap().apply{
        putString("printername",serviceName)
        putString("ip",host.hostAddress)
        putString("port",port.toString())
      }
      sendEvent(reactContext,"OnPrinterFound",payload)
    }
  }

  private val discoveryListener: NsdManager.DiscoveryListener = object :NsdManager.DiscoveryListener{
    override fun onStartDiscoveryFailed(p0: String?, p1: Int) {
      nsdManager?.stopServiceDiscovery(this)

    }

    override fun onStopDiscoveryFailed(p0: String?, p1: Int) {

      nsdManager?.stopServiceDiscovery(this)
    }

    override fun onDiscoveryStarted(p0: String?) {

    }

    override fun onDiscoveryStopped(p0: String?) {

    }

    override fun onServiceFound(p0: NsdServiceInfo?) {
      nsdManager?.resolveService(p0,resolveListener)

    }

    override fun onServiceLost(p0: NsdServiceInfo?) {

    }

  }
  @ReactMethod
  fun multiply(a: Double, b: Double, promise: Promise) {
    promise.resolve(a * b)
  }
  @ReactMethod
  fun setBTPrinter(promise: Promise) {
    try {
      val macList = SunmiPrinterApi.getInstance().findBleDevice(reactApplicationContext.applicationContext)
      if(macList.size > 0) {
        SunmiPrinterApi.getInstance().setPrinter(SunmiPrinter.SunmiBlueToothPrinter, macList[0])
      }
      promise.resolve(macList[0]);
    }catch (e:ArrayIndexOutOfBoundsException){
      promise.resolve("Bluetooth Printer Not Found")
    }catch (e:java.lang.IndexOutOfBoundsException){
      promise.reject("Error: ","Bluetooth Printer Not Found")
    }catch (e:Exception) {
      promise.reject("Error: ", e.toString())
    }

  }

  @ReactMethod
  fun connect(promise:Promise) {
    try{
      if(!SunmiPrinterApi.getInstance().isConnected) {
        SunmiPrinterApi.getInstance().connectPrinter(reactApplicationContext.applicationContext, object :
          ConnectCallback {

          override fun onFound() {
            promise.resolve("Printer Found")
          }

          override fun onUnfound() {
            promise.resolve("Printer Not Found")
          }

          override fun onConnect() {
            promise.resolve("Printer Connected")
          }

          override fun onDisconnect() {
            promise.resolve("Printer Disconnected")
          }

        })
      }}catch (e:Exception){
      promise.resolve(e.toString());
    }

  }
  @ReactMethod
  fun printImage(base64Image:String,promise:Promise) {
    try{
      if(SunmiPrinterApi.getInstance().isConnected) {
        SunmiPrinterApi.getInstance().printerInit()
        val encodedBase64 = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(encodedBase64, 0, encodedBase64.size)
        val scaledBitmap= Bitmap.createScaledBitmap(bitmap,300,bitmap.height, true)
        SunmiPrinterApi.getInstance().printBitmap(scaledBitmap,1)
        SunmiPrinterApi.getInstance().cutPaper(2,2)
        promise.resolve("Print Success")
      }
    } catch (e:Exception){
      promise.reject("Error: ","Print Failed")

    }
  }
  @ReactMethod
  fun printImageWithTCP(base64Image:String,ipAddress:String,port:String,paperWidth:Int,promise: Promise) {
    this.promise=promise

    Thread {
      try {
        val targetWidth= floor(paperWidth/25.4*203).toInt() -35
        val encodedBase64 = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(encodedBase64, 0, encodedBase64.size)
        val scaledBitmap= Bitmap.createScaledBitmap(bitmap,targetWidth, Math.round(
          bitmap.height.toFloat() * targetWidth.toFloat() / bitmap.width.toFloat()
        ), true)


        val printer=EscPosPrinterCommands(TcpConnection(ipAddress,  port.toInt(),10 ))
        val printerConnection=TcpConnection("100.96.109.236",  9100,10 )
        printer.connect()
        printer.reset()
        printer.printImage(EscPosPrinterCommands.bitmapToBytes(scaledBitmap))
        printer.feedPaper(50)
        printer.cutPaper()

        promise.resolve("Print Completed")
      } catch (e: java.lang.Exception) {
        e.printStackTrace()
        promise.reject("Error",e.toString())
      }
    }.start()

  }

  @ReactMethod
  fun startDiscovery(promise:Promise){
    try {
      nsdManager = reactApplicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
      nsdManager?.discoverServices(
        "_afpovertcp._tcp.",
        1,
        discoveryListener
      )
      promise.resolve("Discovery Started")
    }catch (e:Exception){
      promise.reject("Error",e.toString())
    }


  }
  @ReactMethod
  fun stopDiscovery(promise:Promise){
    try {
      if(nsdManager!==null){
        nsdManager?.stopServiceDiscovery(discoveryListener)
      }
      else{
        throw Exception ("nsdManager cannot be null")
      }
      promise.resolve("Network Discovery stopped")
    }catch (e:Exception){
      promise.reject("Error",e.toString())
    }
  }


  companion object {
    const val NAME = "SunmiExternalPrinterReactNative"
  }
}
