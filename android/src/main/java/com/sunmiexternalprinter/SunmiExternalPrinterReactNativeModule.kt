package com.sunmiexternalprinter


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Base64
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.github.anastaciocintra.escpos.EscPos
import com.github.anastaciocintra.escpos.image.*
import com.github.anastaciocintra.output.TcpIpOutputStream
import com.izettle.html2bitmap.Html2Bitmap
import com.izettle.html2bitmap.content.WebViewContent
import java.io.ByteArrayOutputStream
import java.net.InetAddress


class SunmiExternalPrinterReactNativeModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
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
                putString("Service Discovery",serviceName)
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
            println("Found")
            nsdManager?.resolveService(p0,  resolveListener)


        }

        override fun onServiceLost(p0: NsdServiceInfo?) {

        }

    }
    override fun getName(): String {
        return NAME
    }
  @ReactMethod
  fun convertHTMLtoBase64(htmlString:String,width:Int, promise:Promise){
    this.promise=promise
    Thread {
      try {
        val bitmap: Bitmap? =
          Html2Bitmap.Builder().setContext(reactApplicationContext.applicationContext)
            .setContent(WebViewContent.html(htmlString)).setBitmapWidth(width)
            .build().bitmap
//        val resizedBitmap = Bitmap.createScaledBitmap(
//          bitmap as Bitmap,
//          631,
//          bitmap.height,
//          true
//        )/// what works the best so far 80mm
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
        promise.resolve(base64String)

      }catch(e: java.lang.Exception){
        e.printStackTrace()
        promise.reject("Error",e.toString())

      }

    }.start()


  }
    @ReactMethod
    fun printImageWithTCPRasterBitImageWrapper(base64Image:String,ipAddress:String,port:String,promise: Promise) {
        this.promise=promise

        Thread {
            try {
                val encodedBase64 = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(encodedBase64, 0, encodedBase64.size)
                val scaledBitmap= Bitmap.createScaledBitmap(bitmap,bitmap.width,bitmap.height,true)
                val  stream = TcpIpOutputStream(ipAddress,port.toInt())
                val escpos= EscPos(stream)
                val algorithm= BitonalOrderedDither()
                val imageWrapper = RasterBitImageWrapper()
                val escposImage = EscPosImage(CoffeeImageAndroidImpl(scaledBitmap), algorithm)
                ImageHelper(scaledBitmap.width,scaledBitmap.height).write(
                escpos,
                CoffeeImageAndroidImpl(scaledBitmap),
                imageWrapper,
                BitonalThreshold()
              )
              escpos.feed(5).cut(EscPos.CutMode.FULL)
                promise.resolve("Print Successfully")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                promise.reject("Error",e.toString())
            }
        }.start()

}
  @ReactMethod
  fun printImageWithTCPBitImageWrapper(base64Image:String,ipAddress:String,port:String,promise: Promise){
    this.promise=promise

    Thread {
      try {
        val encodedBase64 = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(encodedBase64, 0, encodedBase64.size)
        val scaledBitmap= Bitmap.createScaledBitmap(bitmap,bitmap.width,bitmap.height,true)
        val  stream = TcpIpOutputStream(ipAddress,port.toInt())
        val escpos= EscPos(stream)
        val algorithm= BitonalOrderedDither()
        val imageWrapper = BitImageWrapper()
        val escposImage = EscPosImage(CoffeeImageAndroidImpl(scaledBitmap), algorithm)
        ImageHelper(scaledBitmap.width,scaledBitmap.height).write(
          escpos,
          CoffeeImageAndroidImpl(scaledBitmap),
          imageWrapper,
          BitonalThreshold()
        )
        escpos.cut(EscPos.CutMode.FULL)
        promise.resolve("Print Successfully")



      } catch (e: java.lang.Exception) {
        e.printStackTrace()
        promise.reject("Error",e.toString())
      }
    }.start()


  }
  @ReactMethod
  fun printImageWithGraphicsImageWrapper(base64Image:String,ipAddress:String,port:String,promise: Promise){
    this.promise=promise

    Thread {
      try {
        val encodedBase64 = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(encodedBase64, 0, encodedBase64.size)
        val scaledBitmap= Bitmap.createScaledBitmap(bitmap,bitmap.width,bitmap.height,true)
        val  stream = TcpIpOutputStream(ipAddress,port.toInt())
        val escpos= EscPos(stream)
        val algorithm= BitonalOrderedDither()
        val imageWrapper = GraphicsImageWrapper()
        val escposImage = EscPosImage(CoffeeImageAndroidImpl(scaledBitmap), algorithm)
        ImageHelper(scaledBitmap.width,scaledBitmap.height).write(
          escpos,
          CoffeeImageAndroidImpl(scaledBitmap),
          imageWrapper,
          BitonalThreshold()
        )
        escpos.cut(EscPos.CutMode.FULL)
        promise.resolve("Print Successfully")

      } catch (e: java.lang.Exception) {
        e.printStackTrace()
        promise.reject("Error",e.toString())
      }
    }.start()


  }

    @ReactMethod
    fun startDiscovery(promise:Promise){
        try {
            nsdManager = reactApplicationContext.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
            nsdManager?.discoverServices(
             "_afpovertcp._tcp",
                NsdManager.PROTOCOL_DNS_SD,
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
