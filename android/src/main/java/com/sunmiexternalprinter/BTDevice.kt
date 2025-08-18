package com.sunmiexternalprinter

/**
 * Data class representing a Bluetooth device with essential printer identification information.
 * 
 * This class encapsulates the key properties needed to identify and categorize
 * Bluetooth devices during discovery, particularly for filtering printer-compatible devices.
 * The device class information is used to determine if a device is likely to be a printer.
 * 
 * @property name Device name as advertised by the Bluetooth device (may be null)
 * @property address MAC address of the Bluetooth device (unique identifier)
 * @property majorDeviceClass Bluetooth major device class (used for filtering printers)
 * @property deviceClass Specific Bluetooth device class (used for printer identification)
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
data class BTDevice(val name:String?,val address:String, val majorDeviceClass:Int, val deviceClass:Int)
