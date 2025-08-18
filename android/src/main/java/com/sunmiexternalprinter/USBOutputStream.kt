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

/**
 * USB communication stream for USB-connected thermal printers.
 * 
 * This class provides USB Host mode communication for thermal printers connected
 * via USB cable. It extends PipedOutputStream to integrate with the ESC/POS
 * library while handling the complex USB communication protocol in a background thread.
 * 
 * Key features:
 * - USB interface claiming and endpoint management
 * - Bulk transfer operations for reliable data transmission
 * - Background thread processing for non-blocking operations
 * - Automatic resource cleanup and error handling
 * - Promise-based result reporting to React Native
 * 
 * @param usbEndpoint USB endpoint for data transmission
 * @param usbDevice Target USB printer device
 * @param usbManager System USB manager for device operations
 * @param usbInterface USB interface for printer communication
 * @param context Application context for receiver management
 * @param usbBroadcastReceiver Receiver instance for cleanup
 * @param promise React Native promise for operation completion
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
class USBOutputStream(private val usbEndpoint: UsbEndpoint, private val usbDevice: UsbDevice, private val usbManager: UsbManager, private val usbInterface: UsbInterface, private val context: Context, private val usbBroadcastReceiver:USBBroadcastReceiver, private val promise: Promise):PipedOutputStream() {
  /** Input stream for receiving data from the piped output stream */
  private var pipedInputStream: PipedInputStream? = null
  
  /** USB device connection for communication operations */
  private var usbConnection:UsbDeviceConnection?=null
  
  /** Background thread for handling USB communication */
  private var threadPrint: Thread? = null
  
  /** Default exception handler for the communication thread */
  private var uncaughtException =
    Thread.UncaughtExceptionHandler { t: Thread?, e: Throwable ->
      Logger.getLogger(
        this.javaClass.name
      ).log(Level.SEVERE, e.message, e)
    }
    
  /**
   * Sets a custom exception handler for the communication thread.
   * This allows for specialized error handling and logging behavior.
   * 
   * @param uncaughtException Custom exception handler for the communication thread
   */
  fun setCustomUncaughtException(uncaughtException: Thread.UncaughtExceptionHandler?) {
    threadPrint!!.uncaughtExceptionHandler = uncaughtException
  }
  
  /**
   * Establishes USB connection and starts the communication thread.
   * 
   * This method opens the USB device, claims the interface, sets up piped streams,
   * and launches a background thread for data transfer operations. The thread
   * handles bulk transfer operations and automatic resource cleanup.
   */
  public fun openSocketThread(){
    try{
      this.usbConnection = usbManager.openDevice(usbDevice)
      if (!this.usbConnection!!.claimInterface(this.usbInterface, true)) {
        throw IOException("Error during claim USB interface.");
      }
      pipedInputStream = PipedInputStream()

      super.connect(pipedInputStream)
      val printRunnable=Runnable{
        try{
          val mmBuffer= ByteArray(1024)
          while (true) {
            val n = pipedInputStream!!.read(mmBuffer)
            if(n<0){
              break;
            }
            usbConnection!!.bulkTransfer(usbEndpoint,mmBuffer,0,n,0)

          }
          println("Passed unregisterReceiver")
          promise.resolve("Print Successfully")
        }catch (e: Exception){
          promise.reject("Error",e)
        }finally {
          usbConnection!!.close()
          pipedInputStream!!.close()
          context.unregisterReceiver(usbBroadcastReceiver)
        }




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
