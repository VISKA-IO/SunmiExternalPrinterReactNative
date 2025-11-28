# Sunmi External Printer React Native

## Project Overview

This is a React Native library for interfacing with Sunmi external printers. It provides a bridge between React Native applications and Sunmi's external printer hardware via Bluetooth, USB, and TCP/IP connections.

## Project Structure

```
├── src/                    # TypeScript source code
│   ├── index.tsx          # Main entry point and exported API
│   ├── @types/            # TypeScript type definitions
│   └── __tests__/         # Unit tests
├── android/               # Android native module
│   ├── src/main/java/com/sunmiexternalprinter/
│   │   ├── SunmiExternalPrinterReactNativeModule.kt  # Main React Native module
│   │   ├── SunmiExternalPrinterReactNativePackage.kt # Package registration
│   │   ├── BluetoothStream.kt    # Bluetooth connectivity
│   │   ├── TcpIpOutputStream.kt  # TCP/IP connectivity
│   │   ├── USBOutputStream.kt    # USB connectivity
│   │   ├── BTDevice.kt           # Bluetooth device model
│   │   └── ...
│   └── libs/              # External AAR libraries
│       ├── BluetoothBinding.aar
│       └── externalprinterlibrary2-1.0.1.aar
├── ios/                   # iOS native module (stub)
├── example/               # Example React Native app
│   └── src/App.tsx       # Example usage
└── lib/                   # Compiled output
```

## Key Technologies

- **React Native**: Cross-platform mobile framework
- **TypeScript**: Primary language for JS/TS code
- **Kotlin/Java**: Android native implementation
- **Objective-C++**: iOS native implementation (currently minimal)

## Connection Types

The library supports three connection methods:

1. **Bluetooth** - Classic Bluetooth printing
2. **USB** - Direct USB connection
3. **TCP/IP** - Network printing via IP address

## Development Commands

```bash
# Install dependencies
yarn install

# Build the library
yarn build

# Run example app (Android)
cd example && yarn android

# Run tests
yarn test

# Lint code
yarn lint
```

## Architecture Notes

- The Android module uses Sunmi's external printer library (AAR files in `android/libs/`)
- Communication with printers uses ESC/POS commands
- The module emits events for connection status changes
- Supports printing text, images, barcodes, and QR codes

## Important Files

- `src/index.tsx` - Main API surface, all exported functions
- `android/.../SunmiExternalPrinterReactNativeModule.kt` - Android native implementation
- `example/src/App.tsx` - Usage examples and testing interface
- `package.json` - Dependencies and scripts

## Testing

Run the example app on a physical Android device with a Sunmi external printer to test functionality. The example app provides buttons to:

- Scan for Bluetooth devices
- Connect to printers
- Print test content
- Test various print commands
