# Sunmi External Printer

[![npm version](https://badge.fury.io/js/sunmi-external-printer.svg)](https://badge.fury.io/js/sunmi-external-printer)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> ⚠️ **Android Only**: This package only works on Android devices.

A React Native library for printing with Sunmi external printers via TCP/IP, Bluetooth, and USB connections. Tested primarily on Sunmi Cloud Printer NT311.

⚠️ **Important**: This library uses semaphore locking to prevent concurrent printing. Do not use `Promise.all()` for multiple print operations.

## Features

- 🖨️ **Multiple Connection Types**: TCP/IP, Bluetooth, and USB printing
- 🖼️ **Image Printing**: Support for base64 encoded images with multiple rendering methods
- 🔍 **Device Discovery**: Network and Bluetooth device scanning
- 🎨 **HTML to Image**: Convert HTML content to printable images
- 💰 **Cash Drawer**: Open cash drawers connected to printers
- 🔧 **Print Management**: Semaphore locking for print queue management

## Installation

```bash
npm install sunmi-external-printer
```

### Android Setup

Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

## Quick Start

```typescript
import {
  startNetworkDiscovery,
  EscPosImageWithTCPConnectionRasterBitImageWrapper,
  printImageByBluetooth,
  scanBLDevice,
  convertHTMLtoBase64,
} from 'sunmi-external-printer';
import { DeviceEventEmitter } from 'react-native';

// Network Discovery
const discoverPrinters = async () => {
  await startNetworkDiscovery();

  DeviceEventEmitter.addListener('OnPrinterFound', (event) => {
    console.log('Found printer:', event.printername, event.ip, event.port);
  });
};

// Print via TCP/IP
const printViaTCP = async () => {
  const result = await EscPosImageWithTCPConnectionRasterBitImageWrapper(
    base64Image,
    '192.168.1.100',
    '9100'
  );
  console.log('Print result:', result);
};
```

## API Reference

### Types

```typescript
interface printerDevice {
  name: string;
  address: string; // MAC address for Bluetooth devices
}

interface usbPrinterDevice {
  id: string;
  name: string;
  productName: string;
  manufacturerName: string;
  vendorId: string;
  version: string;
  productId: string;
}
```

### Network Printing

#### Discovery

```typescript
// Start discovery
await startNetworkDiscovery();

DeviceEventEmitter.addListener('OnPrinterFound', (event) => {
  // event: { printername: string, ip: string, port: string }
});

// Stop discovery
await stopNetworkDiscovery();
DeviceEventEmitter.removeAllListeners('OnPrinterFound');
```

#### TCP/IP Printing

All methods accept: `base64Image`, `ipAddress`, `port`, and optional `cut` parameter.

```typescript
// Recommended - Raster bit image (best quality)
await EscPosImageWithTCPConnectionRasterBitImageWrapper(
  base64Image,
  '192.168.1.100',
  '9100',
  'FULL'
);

// Alternative methods
await EscPosImageWithTCPConnectionBitImageWrapper(base64Image, ip, port);
await EscPosImageWithTCPConnectionGraphicsImageWrapper(base64Image, ip, port);
```

### Bluetooth Printing

```typescript
// Request permissions first
import { PermissionsAndroid } from 'react-native';
await PermissionsAndroid.requestMultiple([
  PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
  PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
  PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
]);

// Scan for devices (takes ~12 seconds)
const devices = await scanBLDevice();

// Get paired devices
const pairedDevices = await getPairedDevices();

// Print
await printImageByBluetooth(device, base64Image, 'FULL');
```

### USB Printing

```typescript
// Discover USB devices
const usbDevices = await searchUSBDevices();

// Print
await printUSBDevice(device.productId, device.vendorId, base64Image, 'FULL');
```

### Cash Drawer

```typescript
// TCP/IP
await openDrawer('192.168.1.100', '9100');

// Bluetooth
await openDrawerBluetooth(device);

// USB
await openDrawerUSB(productID, vendorID);
```

### Utilities

#### HTML to Image

```typescript
const base64Image = await convertHTMLtoBase64(
  `<html><body><h1>Receipt</h1><p>Total: $25.00</p></body></html>`,
  400 // width in pixels
);
```

#### Print Queue Management

```typescript
await lockPrintingSemaphore();
try {
  await printImageByBluetooth(device, image);
} finally {
  await unlockPrintingSemaphore();
}
```

#### Connection Management

```typescript
await closePrinterSocket();
await closeTCPPrinterSocket();
```

## Best Practices

### Image Optimization

- **Width**: ~576 pixels for 80mm thermal printers
- **Format**: High contrast black and white images
- **Base64**: Remove `data:image/...;base64,` prefix

### Error Handling

```typescript
try {
  const result = await EscPosImageWithTCPConnectionRasterBitImageWrapper(
    base64Image,
    ipAddress,
    port
  );
} catch (error) {
  console.error('Print failed:', error);
}
```

### Memory Management

- Remove event listeners on component unmount
- Close connections when done
- Use semaphore locking to prevent concurrent operations

## Troubleshooting

| Issue                       | Solution                                                      |
| --------------------------- | ------------------------------------------------------------- |
| Bluetooth permission denied | Grant BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION |
| Network printer not found   | Check same network, port 9100 accessibility                   |
| Image not printing          | Remove base64 prefix, check image width (~576px)              |
| USB issues                  | Verify OTG support, USB permissions                           |

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
Prints to a USB connected printer.

| Parameter   | Type   | Required | Description                    |
| ----------- | ------ | -------- | ------------------------------ |
| `productID` | string | Yes      | USB product ID                 |
| `vendorID`  | string | Yes      | USB vendor ID                  |
| `base64`    | string | Yes      | Base64 encoded image           |
| `cut`       | string | Yes      | Cut type ('PARTIAL' or 'FULL') |

```typescript
const result = await printUSBDevice(
  device.productId,
  device.vendorId,
  base64Image,
  'FULL'
);
```

### Cash Drawer Control

#### `openDrawer()` (TCP/IP)

Opens cash drawer connected to network printer.

```typescript
await openDrawer('192.168.1.100', '9100');
```

#### `openDrawerBluetooth()`

Opens cash drawer via Bluetooth.

```typescript
await openDrawerBluetooth(device);
```

#### `openDrawerUSB()`

Opens cash drawer via USB.

```typescript
await openDrawerUSB(productID, vendorID);
```

### Utility Functions

#### `convertHTMLtoBase64()`

Converts HTML content to a base64 image for printing.

| Parameter | Type   | Required | Description           |
| --------- | ------ | -------- | --------------------- |
| `html`    | string | Yes      | HTML content          |
| `width`   | number | Yes      | Image width in pixels |

```typescript
const base64Image = await convertHTMLtoBase64(
  `
  <html>
    <body style="font-family: Arial;">
      <h1>Receipt</h1>
      <p>Total: $25.00</p>
    </body>
  </html>
`,
  400
);
```

#### Print Management

##### `lockPrintingSemaphore()` / `unlockPrintingSemaphore()`

Manage print queue to prevent overlapping print jobs.

```typescript
await lockPrintingSemaphore();
try {
  await printImageByBluetooth(device, image);
} finally {
  await unlockPrintingSemaphore();
}
```

#### Connection Management

##### `closePrinterSocket()` / `closeTCPPrinterSocket()`

Close printer connections.

```typescript
await closePrinterSocket(); // General connection
await closeTCPPrinterSocket(); // TCP specific
```

#### Additional Bluetooth Functions

##### `printBLCut()` / `printBLFeed()`

Send cut or feed commands via Bluetooth.

```typescript
await printBLCut(device); // Cut paper
await printBLFeed(device); // Feed paper
```

## Best Practices

### Image Optimization

- **Width**: Optimize images for 80mm thermal printers (approximately 576 pixels)
- **Format**: Use high contrast black and white images for best results
- **Size**: Keep file sizes small to improve printing speed

### Error Handling

Always wrap print operations in try-catch blocks:

```typescript
try {
  const result = await EscPosImageWithTCPConnectionRasterBitImageWrapper(
    base64Image,
    ipAddress,
    port
  );
  console.log('Print successful:', result);
} catch (error) {
  console.error('Print failed:', error);
}
```

### Memory Management

- Remove event listeners when component unmounts
- Close connections when done
- Use semaphore locking for concurrent print operations

## Troubleshooting

### Common Issues

**Bluetooth Permission Denied**

- Ensure all Bluetooth permissions are granted
- Target Android SDK 31+ requires BLUETOOTH_SCAN and BLUETOOTH_CONNECT

**Network Printer Not Found**

- Verify printer and device are on same network
- Check if printer port 9100 is accessible
- Ensure printer supports ESC/POS commands

**Image Not Printing**

- Remove `data:image/...;base64,` prefix from base64 string
- Ensure image width is appropriate for printer (80mm ≈ 576px)
- Try different image rendering methods

**USB Printer Issues**

- Verify USB OTG support on device
- Check USB permissions in Android manifest
- Ensure printer supports USB printing

## Example Project

See the `/example` folder for a complete implementation demonstrating all features.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
