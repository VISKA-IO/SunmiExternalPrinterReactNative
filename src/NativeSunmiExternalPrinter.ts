import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  // Semaphore methods
  lockPrintingSemaphore(): Promise<void>;
  unlockPrintingSemaphore(): Promise<void>;

  // HTML conversion
  convertHTMLtoBase64(htmlString: string, width: number): Promise<string>;

  // Service methods
  getListofServiceNames(): Promise<string[]>;

  // TCP/IP printing methods
  printImageWithTCPRasterBitImageWrapper(
    base64Image: string,
    ipAddress: string,
    port: string,
    cut: string
  ): Promise<string>;

  printImageWithTCPBitImageWrapper(
    base64Image: string,
    ipAddress: string,
    port: string,
    cut: string
  ): Promise<string>;

  printImageWithTCPGraphicsImageWrapper(
    base64Image: string,
    ipAddress: string,
    port: string,
    cut: string
  ): Promise<string>;

  // Network discovery
  startDiscovery(): Promise<string>;
  stopDiscovery(): Promise<string>;

  // TCP drawer
  openDrawer(ipAddress: string, port: string): Promise<boolean>;

  // Bluetooth methods
  scanBLDevice(): Promise<Object>;
  printImageByBluetooth(
    address: string,
    base64Image: string,
    cut: string
  ): Promise<string>;
  printCutByBluetooth(address: string): Promise<string>;
  printBluetoothFeed(address: string): Promise<string>;
  closePrinterSocket(): Promise<string>;
  getPairedDevices(): Promise<Object[]>;
  openDrawerBluetooth(macAddress: string): Promise<boolean>;

  // TCP socket
  closeTCPPrinterSocket(): Promise<string>;

  // USB methods
  searchUSBDevices(): Promise<Object[]>;
  printUSBDevice(
    productID: string,
    vendorID: string,
    base64: string,
    cut: string
  ): Promise<string>;
  openDrawerUSB(productID: string, vendorID: string): Promise<boolean>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'SunmiExternalPrinterReactNative'
);
