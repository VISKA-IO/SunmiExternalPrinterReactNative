package com.sunmiexternalprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbRequest
import com.facebook.react.bridge.Promise
import java.io.IOException
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger


class USBOutputStream(private val usbEndpoint: UsbEndpoint, private val usbDevice: UsbDevice, private val usbManager: UsbManager, private val usbInterface: UsbInterface, private val context: Context, private val usbBroadcastReceiver:USBBroadcastReceiver, private val promise: Promise):PipedOutputStream() {
  private var pipedInputStream: PipedInputStream? = null
  private var usbConnection:UsbDeviceConnection?=null
  private var threadPrint: Thread? = null
  private var uncaughtException =
    Thread.UncaughtExceptionHandler { t: Thread?, e: Throwable ->
      Logger.getLogger(
        this.javaClass.name
      ).log(Level.SEVERE, e.message, e)
    }
  fun setCustomUncaughtException(uncaughtException: Thread.UncaughtExceptionHandler?) {
    threadPrint!!.uncaughtExceptionHandler = uncaughtException
  }
  public fun openSocketThread(){
    try{
      this.usbConnection = usbManager.openDevice(usbDevice)
      if (!this.usbConnection!!.claimInterface(this.usbInterface, true)) {
        throw IOException("Error during claim USB interface.");
      }
      pipedInputStream = PipedInputStream()

      super.connect(pipedInputStream)
      val printRunnable=Runnable{
        //connect to BlDevice first
        val mmBuffer= ByteArray(1024)
        while (true) {
          val n = pipedInputStream!!.read(mmBuffer)
          if(n<0){
            break;
          }
          usbConnection!!.bulkTransfer(usbEndpoint,mmBuffer,0,n,0)

        }
        usbConnection!!.close()

        pipedInputStream!!.close()
        context.unregisterReceiver(usbBroadcastReceiver)
        println("Passed unregisterReceiver")
        promise.resolve("Print Successfully")
      }
      threadPrint = Thread(printRunnable)
      threadPrint!!.uncaughtExceptionHandler = uncaughtException
      threadPrint!!.start()
    }catch (e:Exception){
      context.unregisterReceiver(usbBroadcastReceiver)
      promise.reject("Error",e)
    }

    }
  }
