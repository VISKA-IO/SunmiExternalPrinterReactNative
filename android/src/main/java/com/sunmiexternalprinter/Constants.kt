package com.sunmiexternalprinter

/**
 * Application-wide constants for the Sunmi External Printer module.
 * 
 * This class contains compile-time constants used throughout the module
 * for consistent string identifiers, intent actions, and other static values.
 * Using constants prevents typos and makes maintenance easier.
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
class Constants {
  companion object{
    /** Intent action for USB device permission requests */
    const val ACTION_USB_PERMISSION = "com.sunmiexternalprinter.USB_PERMISSION"
  }
}
