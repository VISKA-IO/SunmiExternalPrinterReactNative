package com.sunmiexternalprinter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Base64
import com.facebook.react.bridge.Promise
import com.github.anastaciocintra.escpos.EscPos
import com.github.anastaciocintra.escpos.image.BitonalThreshold
import com.github.anastaciocintra.escpos.image.RasterBitImageWrapper

class USBBroadcastReceiver(val promise: Promise):BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (Constants.ACTION_USB_PERMISSION == intent.action) {
      synchronized(this) {
        try{
          val usbDevice: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
          println("Init USB service")
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            val usbInterface = UsbDeviceHelper.findPrinterInterface(usbDevice)
            val usbEndpoint= UsbDeviceHelper.findEndpointIn(usbInterface)
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            if(intent.getStringExtra("job")=="printBase64"){
              val base64String=intent.getStringExtra("base64")
              val cut=intent.getStringExtra("cut")
              val encodedBase64 = Base64.decode(base64String, Base64.DEFAULT)
              val bitmap = BitmapFactory.decodeByteArray(encodedBase64, 0, encodedBase64.size)
              val scaledBitmap =
                Bitmap.createScaledBitmap(bitmap, bitmap.width - 40, bitmap.height, true)
              val imageWrapper = RasterBitImageWrapper()
              val stream = USBOutputStream(usbEndpoint!!,usbDevice!!,usbManager,usbInterface!!,context,this,promise)
              stream.openSocketThread()
              stream.setCustomUncaughtException { _, e ->
                promise.reject("Error", e.toString())
                e.printStackTrace()
              }
              val escpos = EscPos(stream)
              ImageHelper(scaledBitmap.width, scaledBitmap.height).write(
                escpos,
                CoffeeImageAndroidImpl(scaledBitmap),
                imageWrapper,
                BitonalThreshold()
              )
              if(cut=="PARTIAL"){
                escpos.feed(5).cut(EscPos.CutMode.PART).close()
              }
              else{
                escpos.feed(5).cut(EscPos.CutMode.FULL).close()
              }
            }
            if(intent.getStringExtra("job")=="openDrawer"){
              val stream = USBOutputStream(usbEndpoint!!,usbDevice!!,usbManager,usbInterface!!,context,this,promise)
              stream.openSocketThread()
              stream.setCustomUncaughtException { _, e ->
                promise.reject("Error", e.toString())
                e.printStackTrace()
              }
              val escpos = EscPos(stream)
              escpos.write(27).write(112).write(0).write(25).write(250);
              escpos.write(27).write(0).write(-56).write(-56)
              escpos.close()
            }
          } else {
            throw Exception("Permission not granted")
          }
        }catch (e:Exception){
          context.unregisterReceiver(this)
          promise.reject("Error",e)
        }

      }
    }
  }
}
