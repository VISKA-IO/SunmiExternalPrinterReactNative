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
 * Supply OutputStream to the TCP/IP printer.
 *
 *
 * Send data directing to the printer. The instance cannot
 * be reused and the last command should be `close()`, after that,
 * you need to create another instance to send data to the printer.
 */
class TcpIpOutputStream @JvmOverloads constructor(host: String?, port: Int = 9100,promise:Promise) :
  PipedOutputStream() {
  protected val pipedInputStream: PipedInputStream = PipedInputStream()
  protected val threadPrint: Thread
  private var socket:Socket?=null

  /**
   * creates one instance of TcpIpOutputStream.
   *
   *
   *
   * @param host - the IP address
   * @param port - the port number
   * @exception IOException if an I/O error occurs.
   * @exception RuntimeException if an error occurs while in thread
   * @see java.net.Socket
   */
  /**
   * creates one instance of TcpIpOutputStream using default port 9100
   *
   *
   *
   * @param host - the IP address
   * @exception IOException if an I/O error occurs.
   * @exception RuntimeException if an error occurs while in thread
   * @see java.net.Socket
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
          promise.resolve(null)
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
   * Set UncaughtExceptionHandler to make special error treatment.
   *
   *
   * Make special treatment of errors on your code.
   *
   * @param uncaughtException used on (another thread) print.
   */
  fun setUncaughtException(uncaughtException: Thread.UncaughtExceptionHandler?) {
    threadPrint.uncaughtExceptionHandler = uncaughtException
  }

  fun closeSocket(promise: Promise){
    try {
      // process socket
      socket!!.close()
      promise.resolve(null)
    } catch (e:Exception ) {
      promise.reject("Error from closeSocket in TCPIPOutputStream",e)
    }
}
}
