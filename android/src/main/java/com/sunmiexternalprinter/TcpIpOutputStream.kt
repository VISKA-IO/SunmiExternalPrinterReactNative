package com.sunmiexternalprinter

import android.util.Log
import com.facebook.react.bridge.Promise
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.lang.RuntimeException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.logging.Level
import java.util.logging.Logger



/**
 * TCP/IP output stream for network-connected thermal printers.
 * 
 * This class provides a reliable TCP/IP communication channel for sending
 * ESC/POS commands to network printers. It extends PipedOutputStream to
 * integrate seamlessly with the ESC/POS library while handling network
 * communication in a background thread.
 * 
 * Key features:
 * - Automatic socket connection with configurable timeout
 * - Background thread processing for non-blocking operations
 * - Piped stream architecture for efficient data transfer
 * - Promise-based result reporting to React Native
 * - Proper resource cleanup and error handling
 * 
 * The class establishes a TCP connection to the specified host and port,
 * typically port 9100 which is the standard for network printers.
 * 
 * @param host IP address of the target printer
 * @param port Port number for printer communication (default: 9100)
 * @param promise React Native promise for operation completion
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
class TcpIpOutputStream @JvmOverloads constructor(host: String?, port: Int = 9100,promise:Promise) :
  PipedOutputStream() {
  /** Input stream for receiving data from the piped output stream */
  protected val pipedInputStream: PipedInputStream = PipedInputStream()
  
  /** Background thread for handling TCP/IP communication */
  protected val threadPrint: Thread
  
  /** TCP socket connection to the printer */
  private var socket:Socket?=null

  /**
   * Initializes TCP/IP connection and starts background communication thread.
   * 
   * Creates a socket connection to the specified host and port with a 5-second
   * timeout. Sets up piped streams for data transfer and launches a background
   * thread to handle the communication loop.
   */
  init {
    super.connect(pipedInputStream)
    val uncaughtException =
      Thread.UncaughtExceptionHandler { t: Thread?, e: Throwable ->
        Logger.getLogger(
          javaClass.name
        ).log(Level.SEVERE, e.message, e)
      }
    this.socket= Socket()
    val runnablePrint = Runnable {
      try {
          this@TcpIpOutputStream.socket!!.connect(InetSocketAddress(host, port), 5000)
          val outputStream = this@TcpIpOutputStream.socket!!.getOutputStream()
          val buf = ByteArray(1024)
          while (true) {
            val n = pipedInputStream.read(buf)
            if (n < 0) break
            outputStream.write(buf, 0, n)
            outputStream.flush()
          }
          pipedInputStream.close()
          promise.resolve("Printing Completed")
      } catch (ex: Exception) {
        promise.reject("Error from TCP IP",ex)
        throw RuntimeException(ex)
      }
    }



    threadPrint = Thread(runnablePrint)
    threadPrint.uncaughtExceptionHandler = uncaughtException
    threadPrint.start()
  }

  /**
   * Sets a custom exception handler for the communication thread.
   * 
   * This allows for specialized error handling and logging behavior
   * when network communication errors occur.
   *
   * @param uncaughtException Custom exception handler for the communication thread
   */
  fun setUncaughtException(uncaughtException: Thread.UncaughtExceptionHandler?) {
    threadPrint.uncaughtExceptionHandler = uncaughtException
  }

  /**
   * Closes the TCP socket connection and resolves the promise.
   * 
   * This method should be called to properly clean up network resources
   * after printing operations are complete.
   * 
   * @param promise Promise to resolve when socket is closed
   */
  fun closeSocket(promise: Promise){
    try {
      // For TCP IP only need to close it once
      socket?.close()
      promise.resolve(null)
    } catch (e:Exception ) {
      promise.reject("Error from closeSocket in TCPIPOutputStream",e)
    }
}
}


