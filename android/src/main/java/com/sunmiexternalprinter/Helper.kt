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
import java.util.SortedSet
import java.util.regex.Pattern

class Helper {

  companion object{
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
    fun SetBLDevicestoWriteableArray(bleDevices:MutableList<BTDevice>,context:Context,activity: Activity): WritableMap {
      val resultUnFiltered: WritableArray = Arguments.createArray()
      val resultFiltered:WritableArray= Arguments.createArray()
      var deviceMap: WritableMap = Arguments.createMap()
      var mapResult:WritableMap= Arguments.createMap()

      if(this.checkBluetoothConnectPermission(context,activity)) {
        for (bleDevice in bleDevices) {
          deviceMap.putString("name",bleDevice.name)
          deviceMap.putString("address",bleDevice.address)
          resultUnFiltered.pushMap(deviceMap)
          deviceMap= Arguments.createMap()
        }
        for (bleDevice in bleDevices) {
          Log.d("Helper","device filtered beforee if statement ${bleDevice.name}")
          if(bleDevice.majorDeviceClass==1536 || bleDevice.deviceClass==1572){
            Log.d("Helper","device filtered ${bleDevice.name}")
            deviceMap.putString("name",bleDevice.name)
            deviceMap.putString("address",bleDevice.address)
            resultFiltered.pushMap(deviceMap)
            deviceMap= Arguments.createMap()
          }

        }
      }
      mapResult.putArray("filtered_result",resultFiltered)
      mapResult.putArray("unfiltered_result", resultUnFiltered)
      return mapResult

    }

    fun isMacAddress(input: String?): Boolean {
      // Define regular expressions for valid MAC address patterns
      val macAddressPattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"

      // Compile the regular expression pattern
      val pattern: Pattern = Pattern.compile(macAddressPattern)

      // Check if the input matches the pattern
      return pattern.matcher(input).matches()
    }
    @SuppressLint("MissingPermission")
    fun findBLDevice(nameOraddress: String,bluetoothAdapter:BluetoothAdapter,blescanResults: MutableList<BluetoothDeviceComparable>): BluetoothDevice? {
      try{
        val pairedDevices = bluetoothAdapter.bondedDevices
        val pairedDevice= pairedDevices.find {
          if (Helper.isMacAddress(nameOraddress)) {
            if (nameOraddress == it.address) {
              return it
            }
            if (it.name == nameOraddress) return it
          }

          return it
        }
        val foundDevice:BluetoothDeviceComparable?=blescanResults.find {
          if(Helper.isMacAddress(nameOraddress)){
            if(nameOraddress==it.bluetoothDevice.address){
              return it.bluetoothDevice
            }
          }
          if(it.bluetoothDevice.name==nameOraddress) return it.bluetoothDevice

          return it.bluetoothDevice
        }
        if(pairedDevices!==null){
          return pairedDevice
        }
        return foundDevice!!.bluetoothDevice}
      catch(e:Error){
        Log.e("Error findBL","BluetoothDevice Not Found")
        throw Exception("Bluetooth Device with the name or address ${nameOraddress} Not Found")

      }
    }

  }




}
