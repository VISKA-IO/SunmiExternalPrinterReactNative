import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  // Semaphore operations
  lockPrintingSemaphore(): Promise<void>;
  unlockPrintingSemaphore(): Promise<void>;

  // HTML to Base64 conversion
  convertHTMLtoBase64(htmlString: string, width: number): Promise<string>;

  // Service names
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

  printImageWithGraphicsImageWrapper(
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

  // Bluetooth operations
  scanBLDevice(): Promise<Object>;
  printImageByBluetooth(
    address: string,
    base64Image: string,
    cut: string
  ): Promise<string>;
  printCutByBluetooth(address: string): Promise<string>;
  closePrinterSocket(): Promise<string>;
  closeTCPPrinterSocket(): Promise<string>;
  getPairedDevices(): Promise<Object[]>;
  openDrawerBluetooth(macAddress: string): Promise<boolean>;

  // USB operations
  printUSBDevice(
    productID: string,
    vendorId: string,
    base64String: string,
    cut: string
  ): Promise<string>;
  openDrawerUSB(productID: string, vendorId: string): Promise<boolean>;
  searchUSBDevices(): Promise<Object[]>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'SunmiExternalPrinterReactNative'
);
