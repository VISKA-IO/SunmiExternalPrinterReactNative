package com.sunmiexternalprinter

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface

/**
 * Utility object for USB device operations and interface discovery.
 * 
 * This object provides static methods for working with USB devices,
 * particularly for identifying printer interfaces and communication endpoints.
 * It handles the USB Host mode operations required for direct USB printer
 * communication.
 * 
 * The helper methods abstract the complexity of USB interface enumeration
 * and endpoint discovery, making it easier to establish communication
 * with USB thermal printers.
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
object UsbDeviceHelper {
  /**
   * Finds the printer interface in a USB device.
   * 
   * Searches through all available interfaces on the USB device to locate
   * the one with USB_CLASS_PRINTER class, which indicates it's suitable
   * for printer communication operations.
   * 
   * @param usbDevice The USB device to search for printer interface
   * @return The printer USB interface, or null if no printer interface is found
   */
  fun findPrinterInterface(usbDevice: UsbDevice?): UsbInterface? {
    if (usbDevice == null) {
      return null
    }
    val interfacesCount = usbDevice.interfaceCount
    for (i in 0 until interfacesCount) {
      val usbInterface = usbDevice.getInterface(i)
      if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
        return usbInterface
      }
    }
    return null
  }

  /**
   * Finds the output endpoint for data transmission to the printer.
   * 
   * Searches the USB interface for a bulk transfer endpoint with output
   * direction (USB_DIR_OUT). Bulk transfer endpoints are used for reliable,
   * high-throughput data transmission to printers.
   * 
   * @param usbInterface The USB interface to search for output endpoint
   * @return The output endpoint for data transmission, or null if not found
   */
  fun findEndpointIn(usbInterface: UsbInterface?): UsbEndpoint? {
    if (usbInterface != null) {
      val endpointsCount = usbInterface.endpointCount
      for (i in 0 until endpointsCount) {
        val endpoint = usbInterface.getEndpoint(i)
        if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK && endpoint.direction == UsbConstants.USB_DIR_OUT) {
          return endpoint
        }
      }
    }
    return null
  }
}
