package com.sunmiexternalprinter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

/**
 * Wrapper class for BluetoothDevice that implements Comparable interface.
 * 
 * This class enables BluetoothDevice objects to be stored in sorted collections
 * like TreeSet, which is essential for maintaining ordered lists of discovered
 * devices and preventing duplicates during Bluetooth scanning operations.
 * 
 * The comparison logic prioritizes device names for readability, falling back
 * to MAC addresses when names are identical or null. This ensures consistent
 * ordering and reliable duplicate detection.
 * 
 * @param device The BluetoothDevice to wrap with comparison capabilities
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
class BluetoothDeviceComparable(device:BluetoothDevice):Comparable<BluetoothDeviceComparable> {
    /** The wrapped BluetoothDevice instance */
    val bluetoothDevice=device

    /**
     * Compares this BluetoothDeviceComparable with another for ordering.
     * 
     * Comparison logic:
     * 1. If device names are identical or either is null, compare by MAC address
     * 2. Otherwise, compare by device name alphabetically
     * 
     * This ensures consistent ordering while handling cases where devices
     * may not have advertised names.
     * 
     * @param other The other BluetoothDeviceComparable to compare with
     * @return Negative if this < other, zero if equal, positive if this > other
     */
    @SuppressLint("MissingPermission")
    override fun compareTo(other: BluetoothDeviceComparable): Int {
        if(this.bluetoothDevice.name == other.bluetoothDevice.name || (this.bluetoothDevice.name==null || other.bluetoothDevice.name==null) ){
            return this.bluetoothDevice.address.compareTo(other.bluetoothDevice.address)
        }
        return this.bluetoothDevice.name.compareTo(other.bluetoothDevice.name)
    }
}
