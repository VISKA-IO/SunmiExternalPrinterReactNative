package com.sunmiexternalprinter


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.sunmiexternalprinter.NSD.NSDDevice
import java.util.SortedSet
import java.util.regex.Pattern

/**
 * Utility class providing static helper methods for the Sunmi External Printer module.
 * 
 * This class contains common operations used throughout the module including:
 * - Permission management for Bluetooth and other system features
 * - Data conversion between Android and React Native types
 * - Device discovery and filtering utilities
 * - Validation functions for network addresses
 * 
 * All methods are static and can be called without instantiating the class.
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
class Helper {

  companion object{
    /**
     * Checks and requests Bluetooth connect permission for the application.
     * 
     * This method verifies if the app has BLUETOOTH_CONNECT permission (required
     * on Android 12+) and requests it if not already granted. This permission
     * is required for connecting to Bluetooth devices.
     * 
     * @param context Application context for permission checking
     * @param activity Activity context for requesting permissions
     * @return true (permission will be requested if needed)
     */
    fun checkBluetoothConnectPermission(context: Context,activity: Activity):Boolean{
      if (ActivityCompat.checkSelfPermission(
          context,
          Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        ActivityCompat.requestPermissions(activity,
          arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
          1)
      }
      return true

    }
    /**
     * Checks and requests Bluetooth scan permission for the application.
     * 
     * This method verifies if the app has BLUETOOTH_SCAN permission (required
     * on Android 12+) and requests it if not already granted. This permission
     * is required for discovering nearby Bluetooth devices.
     * 
     * @param context Application context for permission checking
     * @param activity Activity context for requesting permissions
     * @return true (permission will be requested if needed)
     */
    fun checkBluetoothScanPermission(context: Context,activity: Activity):Boolean{
      if (ActivityCompat.checkSelfPermission(
          context,
          Manifest.permission.BLUETOOTH_SCAN
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        ActivityCompat.requestPermissions(activity,
          arrayOf(Manifest.permission.BLUETOOTH_SCAN),
          1)
      }
      return true

    }
    /**
     * Converts a list of discovered Bluetooth devices to React Native compatible format.
     * 
     * This method processes Bluetooth device discovery results and returns both
     * filtered and unfiltered device lists. Filtering is based on device class
     * to identify printer-compatible devices (majorDeviceClass=1536 or deviceClass=1572).
     * 
     * @param blDevices List of discovered Bluetooth devices
     * @param context Application context for permission verification
     * @param activity Activity context for permission requests
     * @return WritableMap containing 'filtered_result' and 'unfiltered_result' arrays
     */
    fun SetBLDevicestoWriteableArray(blDevices:MutableList<BTDevice>,context:Context,activity: Activity): WritableMap {
      val resultUnFiltered: WritableArray = Arguments.createArray()
      val resultFiltered:WritableArray= Arguments.createArray()
      var deviceMap: WritableMap = Arguments.createMap()
      var mapResult:WritableMap= Arguments.createMap()

      if(this.checkBluetoothConnectPermission(context,activity)) {
        for (blDevice in blDevices) {
          deviceMap.putString("name",blDevice.name)
          deviceMap.putString("address",blDevice.address)
          resultUnFiltered.pushMap(deviceMap)
          deviceMap= Arguments.createMap()
        }
        for (blDevice in blDevices) {
          Log.d("Helper","device filtered beforee if statement ${blDevice.name}")
          if(blDevice.majorDeviceClass==1536 || blDevice.deviceClass==1572){
            Log.d("Helper","device filtered ${blDevice.name}")
            deviceMap.putString("name",blDevice.name)
            deviceMap.putString("address",blDevice.address)
            resultFiltered.pushMap(deviceMap)
            deviceMap= Arguments.createMap()
          }

        }
      }
      mapResult.putArray("filtered_result",resultFiltered)
      mapResult.putArray("unfiltered_result", resultUnFiltered)
      return mapResult

    }
    /**
     * Converts a list of paired Bluetooth devices to React Native WritableArray.
     * 
     * This method takes Android BluetoothDevice objects and converts them to
     * a format that can be easily consumed by React Native JavaScript code.
     * 
     * @param pairedDevices List of paired Bluetooth devices from system
     * @return WritableArray containing device name and address pairs
     */
    @SuppressLint("MissingPermission")
    fun setBluetoothDevicetoWritableArray(pairedDevices:MutableList<BluetoothDevice>):WritableArray{
      val results: WritableArray = Arguments.createArray()
      var deviceMap: WritableMap = Arguments.createMap()
      for(blDevice in pairedDevices){
        deviceMap.putString("name",blDevice.name)
        deviceMap.putString("address",blDevice.address)
        results.pushMap(deviceMap)
        deviceMap=Arguments.createMap()
      }
      return results
    }

    /**
     * Validates if a given string is a properly formatted MAC address.
     * 
     * Uses regex pattern matching to verify the input follows standard MAC
     * address format: XX:XX:XX:XX:XX:XX or XX-XX-XX-XX-XX-XX where X is
     * a hexadecimal digit (0-9, A-F, a-f).
     * 
     * @param input String to validate as MAC address
     * @return true if input is a valid MAC address format, false otherwise
     */
    fun isMacAddress(input: String?): Boolean {
      // Define regular expressions for valid MAC address patterns
      val macAddressPattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"

      // Compile the regular expression pattern
      val pattern: Pattern = Pattern.compile(macAddressPattern)

      // Check if the input matches the pattern
      return pattern.matcher(input).matches()
    }
    /**
     * Finds a Bluetooth device by MAC address in paired or discovered devices.
     * 
     * This method searches for a Bluetooth device with the specified address
     * first in the system's paired devices, then in the scan results. This
     * ensures compatibility with both previously paired printers and newly
     * discovered devices.
     * 
     * @param address MAC address of the target Bluetooth device
     * @param bluetoothAdapter System Bluetooth adapter for accessing paired devices
     * @param blescanResults Set of devices discovered during scanning
     * @return BluetoothDevice if found, null if not found
     * @throws Exception if the device cannot be found
     */
    @SuppressLint("MissingPermission")
    fun findBLDevice(address: String, bluetoothAdapter:BluetoothAdapter, blescanResults: SortedSet<BluetoothDeviceComparable>): BluetoothDevice? {
      try {
        println("Here in findBLDevice")
        val pairedDevices = bluetoothAdapter.bondedDevices
        println("This is ${address}")
        pairedDevices.forEach{
          if( address == it.address){
            return it
          }
        }
        blescanResults.forEach{
         if( address == it.bluetoothDevice.address){
          return it.bluetoothDevice
         }
        }
        return null
      }
      catch(e:Error){
        Log.e("Error findBL","BluetoothDevice Not Found")
        throw Exception("Bluetooth Device with the name or address ${address} Not Found")

      }
    }

    /**
     * Converts network service discovery devices to React Native WritableArray.
     * 
     * This method processes NSD (Network Service Discovery) results and formats
     * them for consumption by React Native JavaScript code. Each device includes
     * printer name, IP address, and port information.
     * 
     * @param nsdDevices List of discovered network printers
     * @return WritableArray containing printer information
     */
    fun setNSDDevicesToWritableArray(nsdDevices:MutableList<NSDDevice>):WritableArray{
      val results: WritableArray = Arguments.createArray()
      var deviceMap: WritableMap = Arguments.createMap()
      for(nsdDevice in nsdDevices){
        deviceMap.putString("printername",nsdDevice.serviceName)
        deviceMap.putString("ip",nsdDevice.host)
        deviceMap.putString("port",nsdDevice.port)
        results.pushMap(deviceMap)
        deviceMap=Arguments.createMap()
      }
      return results
    }

  }




}
