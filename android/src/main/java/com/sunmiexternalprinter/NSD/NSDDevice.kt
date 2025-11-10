package com.sunmiexternalprinter.NSD

/**
 * Data class representing a network service discovery device.
 * 
 * This class encapsulates the essential connection information for network
 * printers discovered through Android's Network Service Discovery (NSD) API.
 * It contains all the information needed to establish a TCP/IP connection
 * to a network-enabled thermal printer.
 * 
 * @property port Port number for printer communication (typically 9100 for network printers)
 * @property host IP address or hostname of the network printer
 * @property serviceName Advertised service name of the printer on the network
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
data class NSDDevice(val port:String, val host:String, val serviceName:String)
