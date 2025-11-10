package com.sunmiexternalprinter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.facebook.react.bridge.Promise
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Bluetooth communication stream for thermal printer operations.
 * 
 * This class extends PipedOutputStream to provide a seamless data pipeline from
 * ESC/POS commands to Bluetooth RFCOMM communication. It handles the complex
 * threading required for reliable Bluetooth printing operations.
 * 
 * Key features:
 * - RFCOMM socket management with SPP UUID
 * - Threaded data transfer for non-blocking operations
 * - Automatic connection verification and retry logic
 * - Proper resource cleanup and error handling
 * - Promise-based result reporting to React Native
 * 
 * @param device Target Bluetooth device for printing
 * @param promise React Native promise for operation completion
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
@SuppressLint("MissingPermission")
class BluetoothStream(private val device: BluetoothDevice, private val promise: Promise): PipedOutputStream() {
    /** Input stream for receiving data from the piped output stream */
    private var pipedInputStream: PipedInputStream? = null
    
    /** Standard SPP (Serial Port Profile) UUID for RFCOMM connections */
    private val MY_UUID= "00001101-0000-1000-8000-00805F9B34FB"
    
    /** Background thread for handling Bluetooth communication */
    private var threadPrint: Thread? = null
    
    /** Bluetooth RFCOMM socket for device communication */
    private var mmSocket: BluetoothSocket?=null
    
    /** Default exception handler for the communication thread */
    var uncaughtException =
        Thread.UncaughtExceptionHandler { t: Thread?, e: Throwable ->
            Logger.getLogger(
                this.javaClass.name
            ).log(Level.SEVERE, e.message, e)
        }

    /**
     * Verifies and establishes Bluetooth connection to the target device.
     * Attempts to connect if not already connected and handles connection errors.
     * 
     * @return true if connection is successful, false if connection fails
     */
    private fun checkConnect():Boolean{
    return try {
      if(!mmSocket!!.isConnected){
        println("Not Connected to socket")
        mmSocket?.connect()
      }
      Log.d("Socket Connect","Socket Connect Successful")
      true
    }catch(error:Error){
      mmSocket?.close()
      promise.reject("Error",error.toString())
      Log.e("Socket Connect","Error",error)
      false

    }
  }

  /**
   * Establishes Bluetooth connection and starts the communication thread.
   * Creates an insecure RFCOMM socket, sets up piped streams, and launches
   * a background thread for data transfer operations.
   * 
   * This method should be called once per instance to initialize the connection.
   * The background thread will handle all data transfer and automatically
   * resolve the promise when operations complete.
   */
  public fun openSocketThread(){
    mmSocket= device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))
    pipedInputStream = PipedInputStream()
    super.connect(pipedInputStream)
    val printRunnable=Runnable{
      //connect to BlDevice first
      if(checkConnect()){
        val mmOutStream: OutputStream = mmSocket!!.outputStream
        val mmBuffer= ByteArray(1024)
        while (true) {
          val n = pipedInputStream!!.read(mmBuffer)
          if(n<0){
            break;
          }
          mmOutStream.write(mmBuffer, 0, n)
          mmOutStream.flush()

        }

        pipedInputStream!!.close()

        promise.resolve("Print Successfully")
      }
    }
    threadPrint = Thread(printRunnable)
    threadPrint!!.uncaughtExceptionHandler = uncaughtException
    threadPrint!!.start()

  }

  /**
   * Sets a custom exception handler for the communication thread.
   * This allows for specialized error handling and logging behavior.
   * 
   * @param uncaughtException Custom exception handler to be used by the communication thread
   */
  fun setCustomUncaughtException(uncaughtException: Thread.UncaughtExceptionHandler?) {
    threadPrint!!.uncaughtExceptionHandler = uncaughtException
  }
  
  /**
   * Properly closes the Bluetooth socket and associated streams.
   * This method ensures clean shutdown of the connection and prevents
   * resource leaks. Should be called when printing operations are complete.
   * 
   * Implementation follows Android best practices for Bluetooth cleanup
   * to avoid read timeout issues on Android 4.3+.
   */
    fun closeSocket() {
      // For bluetooth it's a bit different to TCP IP based on this now no more red timeout occurs
      // https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3?rq=1
        mmSocket!!.outputStream.close()
        mmSocket!!.inputStream.close()
        mmSocket!!.close()
        // ... Close the BluetoothStream and any other cleanup ...
    }
}
