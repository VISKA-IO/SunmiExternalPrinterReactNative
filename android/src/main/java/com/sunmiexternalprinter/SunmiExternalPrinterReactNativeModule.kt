package com.sunmiexternalprinter

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.github.anastaciocintra.escpos.EscPos
import com.github.anastaciocintra.escpos.EscPos.CutMode
import com.github.anastaciocintra.escpos.image.*
import com.github.anastaciocintra.output.TcpIpOutputStream
import com.izettle.html2bitmap.Html2Bitmap
import com.izettle.html2bitmap.Html2BitmapConfigurator
import com.izettle.html2bitmap.content.WebViewContent
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.util.SortedSet
import java.util.TreeSet
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore

/**
 * Main React Native bridge module that provides comprehensive external printer functionality.
 *
 * This module supports multiple connection methods for thermal printers:
 * - Bluetooth (Classic)
 * - TCP/IP (Network)
 * - USB (Host mode)
 *
 * Features include:
 * - Device discovery (Bluetooth scanning, Network service discovery, USB enumeration)
 * - Image printing with multiple ESC/POS wrapper types
 * - HTML to image conversion for receipt printing
 * - Cash drawer control
 * - Concurrent printing control with semaphores
 * - Event emission for device state changes
 *
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
class SunmiExternalPrinterReactNativeModule(reactContext: ReactApplicationContext) :
        ReactContextBaseJavaModule(reactContext) {
  /** Current promise for async operations - ensures single operation at a time */
  private var promise: Promise? = null

  /** Network service discovery manager for finding TCP/IP printers */
  private var nsdManager: NsdManager? = null

  /** Bluetooth system service manager */
  val bluetoothManager: BluetoothManager =
          ContextCompat.getSystemService(
                  this.reactApplicationContext,
                  BluetoothManager::class.java
          )!!

  /** Bluetooth adapter for device operations */
  val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

  /** ESC/POS command processor instance */
  var escpos: EscPos? = null

  /** Flag indicating if Bluetooth scanning is in progress */
  private var scanning = false

  /** Main thread handler for UI operations */
  private val handler = Handler(Looper.getMainLooper())

  /** Sorted set of discovered Bluetooth devices */
  private val bleScanResults: SortedSet<BluetoothDeviceComparable> = TreeSet()

  /** List of Bluetooth devices with class changes */
  private val bleScanResultsClassChanged = mutableListOf<BluetoothDeviceComparable>()

  /** List of Bluetooth device data objects */
  private val bleScanResultsDataClass = mutableListOf<BTDevice>()

  /** Semaphore for controlling concurrent printing operations */
  private val printingSemaphore = Semaphore(1)

  /** Active Bluetooth communication stream */
  var stream: BluetoothStream? = null

  /** Active TCP/IP communication stream */
  var tcpStream: com.sunmiexternalprinter.TcpIpOutputStream? = null

  /**
   * Broadcast receiver for handling Bluetooth device disconnection events. Listens for
   * ACTION_ACL_DISCONNECTED and emits BTDeviceDisconnected event to React Native.
   */
  private var receiverDisconnectedBluetoothDeviceReceiver: BroadcastReceiver =
          object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
              val action: String? = intent?.action
              when (action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                  val device: BluetoothDevice =
                          intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                  Log.d(
                          "Disconnected",
                          " On Device Found where find ==null \n Device Info \n Name:${device.name} \n Address:${device.address} \n MajorDeviceClass: ${device.bluetoothClass.majorDeviceClass}\n Device Class ${device.bluetoothClass.deviceClass}"
                  )
                  val resultMap: WritableMap =
                          Arguments.createMap().apply {
                            putString("name", device.name)
                            putString("address", device.address)
                          }
                  sendEvent(
                          this@SunmiExternalPrinterReactNativeModule.reactApplicationContext,
                          "BTDeviceDisconnected",
                          resultMap
                  )
                }
              }
            }
          }

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

  init {

    val filter = IntentFilter()
    filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    this.reactApplicationContext.registerReceiver(
            receiverDisconnectedBluetoothDeviceReceiver,
            filter
    )
  }

  /**
   * Network service discovery resolve listener for TCP/IP printer discovery. Handles successful
   * resolution of network services and emits OnPrinterFound events.
   */
  private val resolveListener =
          object : NsdManager.ResolveListener {

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
              // Called when the resolve fails. Use the error code to debug.

            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {

              val port: Int = serviceInfo.port
              val host: InetAddress = serviceInfo.host
              val serviceName = serviceInfo.serviceName
              val payload =
                      Arguments.createMap().apply {
                        putString("Service Discovery", serviceName)
                        putString("ip", host.hostAddress)
                        putString("port", port.toString())
                      }
              sendEvent(reactContext, "OnPrinterFound", payload)
            }
          }

  /**
   * Network service discovery listener for finding TCP/IP printers. Manages the discovery lifecycle
   * and handles found/lost services.
   */
  private val discoveryListener: NsdManager.DiscoveryListener =
          object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(p0: String?, p1: Int) {
              nsdManager?.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(p0: String?, p1: Int) {

              nsdManager?.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(p0: String?) {}

            override fun onDiscoveryStopped(p0: String?) {}

            override fun onServiceFound(p0: NsdServiceInfo?) {
              println("Found")
              nsdManager?.resolveService(p0, ResolveListener(reactContext))
            }

            override fun onServiceLost(p0: NsdServiceInfo?) {}
          }

  override fun getName(): String {
    return NAME
  }

  /**
   * Acquires the printing semaphore to ensure exclusive access to printing operations. This
   * prevents multiple concurrent print jobs that could interfere with each other.
   *
   * @param promise Promise that resolves when semaphore is acquired
   */
  @ReactMethod
  fun lockPrintingSemaphore(promise: Promise) {
    Thread {
              runBlocking {
                printingSemaphore.acquire()
                promise.resolve(null)
              }
            }
            .start()
  }

  /**
   * Releases the printing semaphore after printing operations are complete. This allows other print
   * jobs to proceed.
   *
   * @param promise Promise that resolves when semaphore is released
   */
  @ReactMethod
  fun unlockPrintingSemaphore(promise: Promise) {
    Thread {
              runBlocking {
                printingSemaphore.release()
                promise.resolve(null)
              }
            }
            .start()
  }

  /**
   * Converts HTML content to a Base64-encoded bitmap image for printing. Uses WebView to render
   * HTML and converts to bitmap suitable for thermal printers.
   *
   * @param htmlString HTML content to convert to image
   * @param width Target width for the resulting bitmap
   * @param promise Promise that resolves with Base64 string or rejects with error
   */
  @ReactMethod
  fun convertHTMLtoBase64(htmlString: String, width: Int, promise: Promise) {
    this.promise = promise
    Thread {
              try {
                val html2BitmapConfigurator: Html2BitmapConfigurator =
                        object : Html2BitmapConfigurator() {
                          @SuppressLint("SetJavaScriptEnabled")
                          override fun configureWebView(webview: WebView) {
                            webview.settings.javaScriptEnabled = true
                          }
                        }
                val bitmap: Bitmap? =
                        Html2Bitmap.Builder()
                                .setContext(reactApplicationContext.applicationContext)
                                .setConfigurator(html2BitmapConfigurator)
                                .setContent(WebViewContent.html(htmlString))
                                .setBitmapWidth(width)
                                .build()
                                .bitmap
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
              } catch (e: java.lang.Exception) {
                e.printStackTrace()
                promise.reject("Error", e.toString())
              }
            }
            .start()
  }

  /**
   * Retrieves a list of all running Android services. Useful for debugging and system monitoring.
   *
   * @param promise Promise that resolves with array of service class names
   */
  @Suppress("DEPRECATION")
  @ReactMethod
  fun getListofServiceNames(promise: Promise) {
    val activityManager: ActivityManager =
            reactApplicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val services: WritableArray = Arguments.createArray()
    for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
      services.pushString(service.service.className)
    }
    promise.resolve(services)
  }

  /**
   * Prints a Base64-encoded image via TCP/IP using RasterBitImageWrapper. RasterBitImageWrapper
   * provides high-quality image output suitable for graphics.
   *
   * @param base64Image Base64-encoded image data
   * @param ipAddress Target printer IP address
   * @param port Target printer port (typically 9100)
   * @param cut Paper cutting mode: "PARTIAL" or "FULL"
   * @param promise Promise that resolves on success or rejects on error
   */
  @ReactMethod
  fun printImageWithTCPRasterBitImageWrapper(
          base64Image: String,
          ipAddress: String,
          port: String,
          cut: String,
          promise: Promise
  ) {
    this.promise = promise

    Thread {
              try {
                val encodedBase64 = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(encodedBase64, 0, encodedBase64.size)
                val scaledBitmap =
                        Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
                tcpStream =
                        com.sunmiexternalprinter.TcpIpOutputStream(ipAddress, port.toInt(), promise)
                tcpStream!!.setUncaughtException { t, e ->
                  promise.reject("Error", e.toString())
                  e.printStackTrace()
                }
                val escpos = EscPos(tcpStream!!)
                // Initialize printer - ESC @ command resets printer to default settings
                // This ensures the printer is ready to receive image data correctly
                escpos.write(27) // ESC
                escpos.write(64) // @ - Initialize printer
                val imageWrapper = RasterBitImageWrapper()
                ImageHelper(scaledBitmap.width, scaledBitmap.height)
                        .write(
                                escpos,
                                CoffeeImageAndroidImpl(scaledBitmap),
                                imageWrapper,
                                BitonalThreshold()
                        )
                if (cut == "PARTIAL") {
                  escpos.feed(5).cut(EscPos.CutMode.PART).close()
                } else {
                  escpos.feed(5).cut(EscPos.CutMode.FULL).close()
                }
              } catch (e: java.lang.Exception) {
                e.printStackTrace()
                promise.reject("Error", e.toString())
              }
            }
            .start()
  }

  /**
   * Prints a Base64-encoded image via TCP/IP using BitImageWrapper. BitImageWrapper provides
   * standard image printing with good compatibility.
   *
   * @param base64Image Base64-encoded image data
   * @param ipAddress Target printer IP address
   * @param port Target printer port (typically 9100)
   * @param cut Paper cutting mode: "PARTIAL" or "FULL"
   * @param promise Promise that resolves on success or rejects on error
   */
  @ReactMethod
  fun printImageWithTCPBitImageWrapper(
          base64Image: String,
          ipAddress: String,
          port: String,
          cut: String,
          promise: Promise
  ) {
    this.promise = promise

    Thread {
              try {
                val encodedBase64 = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(encodedBase64, 0, encodedBase64.size)
                val scaledBitmap =
                        Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
                val stream = TcpIpOutputStream(ipAddress, port.toInt())
                val escpos = EscPos(stream)
                // Initialize printer - ESC @ command resets printer to default settings
                // This ensures the printer is ready to receive image data correctly
                escpos.write(27) // ESC
                escpos.write(64) // @ - Initialize printer
                val algorithm = BitonalOrderedDither()
                val imageWrapper = BitImageWrapper()
                val escposImage = EscPosImage(CoffeeImageAndroidImpl(scaledBitmap), algorithm)
                ImageHelper(scaledBitmap.width, scaledBitmap.height)
                        .write(
                                escpos,
                                CoffeeImageAndroidImpl(scaledBitmap),
                                imageWrapper,
                                BitonalThreshold()
                        )
                if (cut == "PARTIAL") {
                  escpos.feed(5).cut(EscPos.CutMode.PART)
                } else {
                  escpos.feed(5).cut(EscPos.CutMode.FULL)
                }
                escpos.close()
                promise.resolve("Print Successfully")
              } catch (e: java.lang.Exception) {
                e.printStackTrace()
                promise.reject("Error", e.toString())
              }
            }
            .start()
  }

  /**
   * Prints a Base64-encoded image via TCP/IP using GraphicsImageWrapper. GraphicsImageWrapper
   * provides optimized graphics rendering for complex images.
   *
   * @param base64Image Base64-encoded image data
   * @param ipAddress Target printer IP address
   * @param port Target printer port (typically 9100)
   * @param cut Paper cutting mode: "PARTIAL" or "FULL"
   * @param promise Promise that resolves on success or rejects on error
   */
  @ReactMethod
  fun printImageWithGraphicsImageWrapper(
          base64Image: String,
          ipAddress: String,
          port: String,
          cut: String,
          promise: Promise
  ) {
    this.promise = promise

    Thread {
              try {
                val encodedBase64 = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(encodedBase64, 0, encodedBase64.size)
                val scaledBitmap =
                        Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
                val stream = TcpIpOutputStream(ipAddress, port.toInt())
                val escpos = EscPos(stream)
                // Initialize printer - ESC @ command resets printer to default settings
                // This ensures the printer is ready to receive image data correctly
                escpos.write(27) // ESC
                escpos.write(64) // @ - Initialize printer
                val algorithm = BitonalOrderedDither()
                val imageWrapper = GraphicsImageWrapper()
                val escposImage = EscPosImage(CoffeeImageAndroidImpl(scaledBitmap), algorithm)
                ImageHelper(scaledBitmap.width, scaledBitmap.height)
                        .write(
                                escpos,
                                CoffeeImageAndroidImpl(scaledBitmap),
                                imageWrapper,
                                BitonalThreshold()
                        )
                if (cut == "PARTIAL") {
                  escpos.feed(5).cut(EscPos.CutMode.PART)
                } else {
                  escpos.feed(5).cut(EscPos.CutMode.FULL)
                }
                escpos.close()
                promise.resolve("Print Successfully")
              } catch (e: java.lang.Exception) {
                e.printStackTrace()
                promise.reject("Error", e.toString())
              }
            }
            .start()
  }

  /**
   * Starts network service discovery to find TCP/IP enabled printers. Discovers printers using
   * "_afpovertcp._tcp" service type over DNS-SD. Found printers will trigger OnPrinterFound events.
   *
   * @param promise Promise that resolves when discovery starts or rejects on error
   */
  @ReactMethod
  fun startDiscovery(promise: Promise) {
    try {
      nsdManager =
              reactApplicationContext.applicationContext.getSystemService(Context.NSD_SERVICE) as
                      NsdManager
      nsdManager?.discoverServices(
              "_afpovertcp._tcp",
              NsdManager.PROTOCOL_DNS_SD,
              discoveryListener
      )
      promise.resolve("Discovery Started")
    } catch (e: Exception) {
      promise.reject("Error", e.toString())
    }
  }

  /**
   * Opens a cash drawer connected to a TCP/IP printer. Sends standard ESC/POS drawer kick commands
   * to trigger drawer opening.
   *
   * @param ipAddress Printer IP address
   * @param port Printer port (typically 9100)
   * @param promise Promise that resolves when drawer command is sent
   */
  @ReactMethod
  fun openDrawer(ipAddress: String, port: String, promise: Promise) {
    this.promise = promise
    Thread {
              try {
                val stream = TcpIpOutputStream(ipAddress, port.toInt())
                val escpos = EscPos(stream)
                escpos.write(27).write(112).write(0).write(25).write(250)
                escpos.write(27).write(0).write(-56).write(-56)
                escpos.close()
                promise.resolve(true)
              } catch (e: Exception) {
                promise.reject("Error", e.toString())
              }
            }
            .start()
  }

  /**
   * Stops network service discovery for TCP/IP printers. Should be called to clean up resources
   * when discovery is no longer needed.
   *
   * @param promise Promise that resolves when discovery is stopped
   */
  @ReactMethod
  fun stopDiscovery(promise: Promise) {
    try {
      if (nsdManager !== null) {
        nsdManager?.stopServiceDiscovery(discoveryListener)
      } else {
        throw Exception("nsdManager cannot be null")
      }
      promise.resolve("Network Discovery stopped")
    } catch (e: Exception) {
      promise.reject("Error", e.toString())
    }
  }

  /**
   * Broadcast receiver for handling Bluetooth device discovery events. Processes ACTION_FOUND,
   * ACTION_CLASS_CHANGED, and ACTION_DISCOVERY_FINISHED.
   */
  private val receiver =
          object : BroadcastReceiver() {
            @SuppressWarnings("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
              val action: String? = intent.action
              when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                  val device: BluetoothDevice =
                          intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                  val majorDeviceClass = device.bluetoothClass.majorDeviceClass
                  val deviceClass = device.bluetoothClass.deviceClass
                  val deviceComparable: BluetoothDeviceComparable =
                          BluetoothDeviceComparable(device)
                  this@SunmiExternalPrinterReactNativeModule.bleScanResults.add(deviceComparable)
                  val find =
                          this@SunmiExternalPrinterReactNativeModule.bleScanResultsDataClass.find {
                            it.address == deviceComparable.bluetoothDevice.address
                          }
                  if (find == null) {
                    Log.d(
                            "Discovery",
                            " On Device Found where find ==null \n Device Info \n Name:${device.name} \n Address:${device.address} \n MajorDeviceClass: ${device.bluetoothClass.majorDeviceClass}\n Device Class ${device.bluetoothClass.deviceClass}"
                    )
                    this@SunmiExternalPrinterReactNativeModule.bleScanResultsDataClass.add(
                            BTDevice(
                                    device.name,
                                    device.address,
                                    device.bluetoothClass.majorDeviceClass,
                                    device.bluetoothClass.deviceClass
                            )
                    )
                  }
                }
                BluetoothDevice.ACTION_CLASS_CHANGED -> {
                  Log.d("Discovery", "Device Action Class Changed ")
                  // find the BL device and then change
                  val device: BluetoothDevice =
                          intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                  val deviceComparable: BluetoothDeviceComparable =
                          BluetoothDeviceComparable(device)
                  if (device.name.contains("Cloud")) {
                    Log.d(
                            "Discovery",
                            "On Action Class Changed Before entering if statement \n" +
                                    " Device Info \n" +
                                    " Name:${device.name} \n" +
                                    " Address:${device.address} \n" +
                                    " MajorDeviceClass: ${device.bluetoothClass.majorDeviceClass}\n" +
                                    " Device Class ${device.bluetoothClass.deviceClass} "
                    )
                  }

                  if (device.bluetoothClass.majorDeviceClass != 7936 ||
                                  device.bluetoothClass.deviceClass != 7936
                  ) {

                    Log.d(
                            "Discovery",
                            "on Action Class Changed \n Passed if statement \n" +
                                    " Device Info \n" +
                                    " Name:${device.name} \n" +
                                    " Address:${device.address} \n" +
                                    " MajorDeviceClass: ${device.bluetoothClass.majorDeviceClass}\n" +
                                    " Device Class ${device.bluetoothClass.deviceClass}"
                    )
                    val findDataClass =
                            this@SunmiExternalPrinterReactNativeModule.bleScanResultsDataClass
                                    .find { it.address == device.address }
                    if (findDataClass != null) {
                      val result =
                              this@SunmiExternalPrinterReactNativeModule.bleScanResultsDataClass
                                      .remove(findDataClass)
                      Log.d(
                              "Discovery",
                              "on Action Class Changed : Changed Remove Scan Data Class Result ${result}"
                      )
                    }
                    this@SunmiExternalPrinterReactNativeModule.bleScanResultsDataClass.add(
                            BTDevice(
                                    device.name,
                                    device.address,
                                    device.bluetoothClass.majorDeviceClass,
                                    device.bluetoothClass.deviceClass
                            )
                    )
                  }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                  val result: WritableMap =
                          Helper.SetBLDevicestoWriteableArray(
                                  this@SunmiExternalPrinterReactNativeModule
                                          .bleScanResultsDataClass,
                                  this@SunmiExternalPrinterReactNativeModule
                                          .reactApplicationContext,
                                  requireNotNull(
                                          this@SunmiExternalPrinterReactNativeModule
                                                  .reactApplicationContext
                                                  .currentActivity
                                  )
                          )
                  Log.d("Discovery", "Discovery Finished")
                  this@SunmiExternalPrinterReactNativeModule.bleScanResults.forEach { it ->
                    Log.d(
                            "Discovery",
                            " On Discovery Finished \n Device Info \n Name:${it.bluetoothDevice.name} \n Address:${it.bluetoothDevice.address} \n MajorDeviceClass: ${it.bluetoothDevice.bluetoothClass.majorDeviceClass}\n Device Class ${it.bluetoothDevice.bluetoothClass.deviceClass}"
                    )
                  }
                  this@SunmiExternalPrinterReactNativeModule.bleScanResultsClassChanged.forEach { it
                    ->
                    Log.d(
                            "Discovery",
                            " On Discovery Finished Class Changed \n Device Info \n Name:${it.bluetoothDevice.name} \n Address:${it.bluetoothDevice.address} \n MajorDeviceClass: ${it.bluetoothDevice.bluetoothClass.majorDeviceClass}\n Device Class ${it.bluetoothDevice.bluetoothClass.deviceClass}"
                    )
                  }
                  this@SunmiExternalPrinterReactNativeModule.bleScanResultsDataClass.forEach { it ->
                    Log.d(
                            "Discovery",
                            " On Discovery Finished Data Class Changed \n Device Info \n Name:${it.name} \n Address:${it.address} \n MajorDeviceClass: ${it.majorDeviceClass}\n Device Class ${it.deviceClass}"
                    )
                  }
                  Log.d(
                          "Discovery",
                          "On Discovery Finished Size of BleScanResult ${bleScanResults.size}, and size of ${bleScanResultsDataClass.size}"
                  )

                  this@SunmiExternalPrinterReactNativeModule.promise!!.resolve(result)
                }
              }
            }
          }

  /**
   * Scans for available Bluetooth devices and returns filtered results. Automatically filters
   * devices by printer-compatible device classes. Results are returned as both filtered and
   * unfiltered arrays.
   *
   * @param promise Promise that resolves with device arrays or rejects on error
   */
  @SuppressLint("MissingPermission")
  @ReactMethod
  private fun scanBLDevice(promise: Promise) {
    bleScanResults.clear()
    bleScanResultsDataClass.clear()
    Log.d("Size:", "${bleScanResults.size}")
    Log.d("Size:", "${bleScanResultsDataClass.size}")
    this.promise = promise
    if (Helper.checkBluetoothScanPermission(
                    this.reactApplicationContext,
                    requireNotNull(
                            this@SunmiExternalPrinterReactNativeModule.reactApplicationContext
                                    .currentActivity
                    )
            )
    ) {
      Thread {
                val filter = IntentFilter()
                filter.addAction(BluetoothDevice.ACTION_FOUND)
                filter.addAction(BluetoothDevice.ACTION_CLASS_CHANGED)
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                this.reactApplicationContext.registerReceiver(receiver, filter)

                bluetoothAdapter?.startDiscovery()
                Log.d("Printer Module", " Bluetooth Discovery Started")
              }
              .start()
    }
  }

  /**
   * Prints a Base64-encoded image via Bluetooth connection. Uses RasterBitImageWrapper for optimal
   * image quality over Bluetooth.
   *
   * @param address Bluetooth MAC address of target printer
   * @param base64Image Base64-encoded image data
   * @param cut Paper cutting mode: "PARTIAL" or "FULL"
   * @param promise Promise that resolves on success or rejects on error
   */
  @SuppressLint("MissingPermission")
  @ReactMethod
  private fun printImageByBluetooth(
          address: String,
          base64Image: String,
          cut: String,
          addresspromise: Promise
  ) {
    this.promise = addresspromise
    Thread {
              try {
                val blDevice = Helper.findBLDevice(address, bluetoothAdapter!!, bleScanResults)!!
                stream = BluetoothStream(blDevice, this.promise!!)
                stream!!.openSocketThread()
                stream!!.setCustomUncaughtException { _, e ->
                  promise!!.reject("Error", e.toString())
                  e.printStackTrace()
                }
                escpos = EscPos(stream)
                val encodedBase64 = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(encodedBase64, 0, encodedBase64.size)
                val scaledBitmap =
                        Bitmap.createScaledBitmap(bitmap, bitmap.width - 40, bitmap.height, true)
                val imageWrapper = RasterBitImageWrapper()
                ImageHelper(scaledBitmap.width, scaledBitmap.height)
                        .write(
                                escpos!!,
                                CoffeeImageAndroidImpl(scaledBitmap),
                                imageWrapper,
                                BitonalThreshold()
                        )
                if (cut == "PARTIAL") {
                  escpos!!.feed(5).cut(EscPos.CutMode.PART).close()
                } else {
                  escpos!!.feed(5).cut(EscPos.CutMode.FULL).close()
                }
              } catch (e: java.lang.Exception) {
                e.printStackTrace()
                promise?.reject("Error", e.toString())
              }
            }
            .start()
  }

  /**
   * Sends a paper cut command via Bluetooth connection. Useful for cutting paper without printing
   * additional content.
   *
   * @param address Bluetooth MAC address of target printer
   * @param promise Promise that resolves when cut command is sent
   */
  @SuppressLint("MissingPermission")
  @ReactMethod
  private fun printCutByBluetooth(address: String, addresspromise: Promise) {
    this.promise = addresspromise
    println("Here Inside the function in android  ")
    Thread {
              try {
                println("Here Inside the try Thread ")
                Log.d("Printing", "BL Device address ${address}")
                println("This is bluetoothAdapter $bluetoothAdapter")
                println("This is bluetoothScanResults $bleScanResults")
                val blDevice = Helper.findBLDevice(address, bluetoothAdapter!!, bleScanResults)!!
                println("This is blDevice ${blDevice}")
                Log.d(
                        "Printing",
                        "BL Device Found \n Name: ${blDevice.name} \n Address:${blDevice.address} \nMajor Device Class: ${blDevice.bluetoothClass.majorDeviceClass} \n Device Class:${blDevice.bluetoothClass.deviceClass}"
                )
                stream = BluetoothStream(blDevice, this.promise!!)
                stream!!.setCustomUncaughtException { _, e ->
                  promise!!.reject("Error", e.toString())
                  e.printStackTrace()
                }
                val escpos = EscPos(stream)
                escpos.cut(CutMode.FULL)
                escpos.close()
              } catch (e: java.lang.Exception) {
                e.printStackTrace()
                promise?.reject("Error", e.toString())
              }
            }
            .start()
  }

  /**
   * Closes the active Bluetooth printer socket connection. Should be called after printing
   * operations to free resources.
   *
   * @param promise Promise that resolves when socket is closed
   */
  @SuppressLint("MissingPermission")
  @ReactMethod
  private fun closePrinterSocket(promise: Promise) {
    try {
      stream!!.closeSocket()
      promise.resolve("Socket close")
    } catch (e: Error) {
      promise.reject(e)
    }
  }

  /**
   * Closes the active TCP/IP printer socket connection. Should be called after printing operations
   * to free resources.
   *
   * @param promise Promise that resolves when socket is closed
   */
  @SuppressLint("MissingPermission")
  @ReactMethod
  private fun closeTCPPrinterSocket(promise: Promise) {
    if (tcpStream != null) {
      tcpStream!!.closeSocket(promise)
    } else {
      promise.reject("TCP_STREAM_NULL", "tcpStream is null")
    }
  }

  /**
   * Retrieves a list of paired (bonded) Bluetooth devices. Returns devices that have been
   * previously paired with this Android device.
   *
   * @param promise Promise that resolves with array of paired devices
   */
  @SuppressLint("MissingPermission")
  @ReactMethod
  private fun getPairedDevices(promise: Promise) {
    try {
      this.promise = promise
      println("Here Inside the function in android  ")
      val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter!!.bondedDevices
      val results = Helper.setBluetoothDevicetoWritableArray(pairedDevices.toMutableList())
      promise.resolve(results)
    } catch (e: Exception) {
      promise.reject(e.toString())
    }
  }

  /**
   * Opens a cash drawer connected to a Bluetooth printer. Sends standard ESC/POS drawer kick
   * commands via Bluetooth.
   *
   * @param macAddress Bluetooth MAC address of target printer
   * @param promise Promise that resolves when drawer command is sent
   */
  @ReactMethod
  fun openDrawerBluetooth(macAddress: String, promise: Promise) {
    this.promise = promise
    Thread {
              try {
                val blDevice = Helper.findBLDevice(macAddress, bluetoothAdapter!!, bleScanResults)!!
                stream = BluetoothStream(blDevice, this.promise!!)
                stream!!.setCustomUncaughtException { t, e ->
                  promise.reject("Error", e.toString())
                  e.printStackTrace()
                }
                val escpos = EscPos(stream)
                escpos.write(27).write(112).write(0).write(25).write(250)
                escpos.write(27).write(0).write(-56).write(-56)
                escpos.close()

                promise.resolve(true)
              } catch (e: Exception) {
                promise.reject("Error", e.toString())
              }
            }
            .start()
  }

  /**
   * Prints a Base64-encoded image via USB connection. Requires USB Host mode and appropriate device
   * permissions.
   *
   * @param productID USB product ID of target printer
   * @param vendorId USB vendor ID of target printer
   * @param base64String Base64-encoded image data
   * @param cut Paper cutting mode: "PARTIAL" or "FULL"
   * @param promise Promise that resolves on success or rejects on error
   */
  @SuppressLint("unused", "UnspecifiedRegisterReceiverFlag", "InlinedApi")
  @ReactMethod
  fun printUSBDevice(
          productID: String,
          vendorId: String,
          base64String: String,
          cut: String,
          promise: Promise
  ) {
    Thread {
              try {
                val manager =
                        reactApplicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
                val deviceList = manager.getDeviceList()
                var selectedPrinter: UsbDevice? = null
                deviceList.forEach { (key, usbDevice) ->
                  if (usbDevice.vendorId == vendorId.toInt() &&
                                  usbDevice.productId == productID.toInt()
                  ) {
                    selectedPrinter = usbDevice
                  }
                }
                val intent =
                        Intent(Constants.ACTION_USB_PERMISSION).apply {
                          putExtra("job", "printBase64")
                          putExtra("base64", base64String)
                          putExtra("cut", cut)
                        }
                if (selectedPrinter !== null) {
                  val permissionIntent: PendingIntent =
                          PendingIntent.getBroadcast(
                                  reactApplicationContext,
                                  System.currentTimeMillis().toInt(),
                                  intent,
                                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                          PendingIntent.FLAG_MUTABLE
                                  else 0
                          )
                  val filter: IntentFilter = IntentFilter(Constants.ACTION_USB_PERMISSION)
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    reactApplicationContext.registerReceiver(
                            USBBroadcastReceiver(promise),
                            filter,
                            Context.RECEIVER_EXPORTED
                    )
                  } else {
                    reactApplicationContext.registerReceiver(
                            USBBroadcastReceiver(promise),
                            filter,
                    )
                  }
                  manager.requestPermission(selectedPrinter, permissionIntent)
                } else {
                  throw Exception("USB Device not found")
                }
              } catch (e: Exception) {
                promise.reject("Error", e)
              }
            }
            .start()
  }

  /**
   * Opens a cash drawer connected to a USB printer. Requires USB Host mode and appropriate device
   * permissions.
   *
   * @param productID USB product ID of target printer
   * @param vendorId USB vendor ID of target printer
   * @param promise Promise that resolves when drawer command is sent
   */
  @SuppressLint("unused", "UnspecifiedRegisterReceiverFlag", "InlinedApi")
  @ReactMethod
  fun openDrawerUSB(productID: String, vendorId: String, promise: Promise) {
    Thread {
              try {
                val manager =
                        reactApplicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
                val deviceList = manager.getDeviceList()
                var selectedPrinter: UsbDevice? = null
                deviceList.forEach { (key, usbDevice) ->
                  if (usbDevice.vendorId == vendorId.toInt() &&
                                  usbDevice.productId == productID.toInt()
                  ) {
                    selectedPrinter = usbDevice
                  }
                }
                val intent =
                        Intent(Constants.ACTION_USB_PERMISSION).apply {
                          putExtra("job", "openDrawer")
                        }
                if (selectedPrinter !== null) {
                  val permissionIntent: PendingIntent =
                          PendingIntent.getBroadcast(
                                  reactApplicationContext,
                                  System.currentTimeMillis().toInt(),
                                  intent,
                                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                          PendingIntent.FLAG_MUTABLE
                                  else 0
                          )
                  val filter: IntentFilter = IntentFilter(Constants.ACTION_USB_PERMISSION)
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    reactApplicationContext.registerReceiver(
                            USBBroadcastReceiver(promise),
                            filter,
                            Context.RECEIVER_EXPORTED
                    )
                  } else {
                    reactApplicationContext.registerReceiver(
                            USBBroadcastReceiver(promise),
                            filter,
                    )
                  }
                  manager.requestPermission(selectedPrinter, permissionIntent)
                } else {
                  throw Exception("USB Device not found")
                }
              } catch (e: Exception) {
                promise.reject("Error", e)
              }
            }
            .start()
  }

  /**
   * Searches for and returns available USB devices. Lists all connected USB devices with their
   * properties for device selection. Note: Some properties like serialNumber, productName, and
   * manufacturerName require USB permission to access on Android 10+.
   *
   * @param promise Promise that resolves with array of USB device information
   */
  @RequiresApi(Build.VERSION_CODES.M)
  @SuppressLint("unused")
  @ReactMethod
  fun searchUSBDevices(promise: Promise) {
    try {
      val manager = reactApplicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
      val deviceList = manager.getDeviceList()
      val list = Arguments.createArray()
      deviceList.forEach { (key, usbDevice) ->
        val writableMap = Arguments.createMap()
        writableMap.putString("id", key)
        writableMap.putString("name", usbDevice.deviceName)
        // productName, manufacturerName, and serialNumber require USB permission on Android 10+
        // Wrap in try-catch to avoid SecurityException when permission is not yet granted
        try {
          writableMap.putString("productName", usbDevice.productName)
        } catch (e: SecurityException) {
          writableMap.putString("productName", null)
        }
        try {
          writableMap.putString("manufacturerName", usbDevice.manufacturerName)
        } catch (e: SecurityException) {
          writableMap.putString("manufacturerName", null)
        }
        writableMap.putString("vendorId", usbDevice.vendorId.toString())
        writableMap.putString("version", usbDevice.version)
        writableMap.putString("productId", usbDevice.productId.toString())
        list.pushMap(writableMap)
      }
      promise.resolve(list)
    } catch (e: Exception) {
      promise.reject("Error", e)
    }
  }

  companion object {
    /** Module name for React Native bridge registration */
    const val NAME = "SunmiExternalPrinterReactNative"
  }
}
