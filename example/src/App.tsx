/* eslint-disable react-native/no-inline-styles */
import { base64Image } from '../base64image';
import * as React from 'react';
import axios from 'axios';
import {
  StyleSheet,
  View,
  Text,
  Button,
  SafeAreaView,
  ScrollView,
  StatusBar,
  useColorScheme,
  TouchableOpacity,
  PermissionsAndroid,
} from 'react-native';
import { Colors, Header } from 'react-native/Libraries/NewAppScreen';
import {
  EscPosImageWithTCPConnectionBitImageWrapper,
  EscPosImageWithTCPConnectionGraphicsImageWrapper,
  EscPosImageWithTCPConnectionRasterBitImageWrapper,
  closePrinterSocket,
  getListofServiceNames,
  getPairedDevices,
  openDrawer,
  openDrawerUSB,
  printBLCut,
  printBLFeed,
  printImageByBluetooth,
  printUSBDevice,
  scanBLDevice,
  searchUSBDevices,
  startNetworkDiscovery,
} from '@viska-io/sunmi-external-printer';
import type {
  printerDevice,
  usbPrinterDevice,
} from '@viska-io/sunmi-external-printer';
import { useState } from 'react';
import { convertHTMLtoBase64 } from '../../src';
import { html } from '../exampleBigOrder';

const Item2 = ({ item, onPress, backgroundColor, textColor }: any) => (
  <TouchableOpacity
    onPress={onPress}
    style={[styles.item, { backgroundColor }]}
  >
    <Text style={[styles.title, { color: textColor }]}>{item.name}</Text>
    <Text style={[styles.title, { color: textColor }]}>{item.address}</Text>
  </TouchableOpacity>
);

function App(): JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  const [ipAddress, setIpAddress] = useState<string>('');
  const [port, setPort] = useState<string>('');
  const [printerName, setPrinterName] = useState<string>('');
  const [devices, setListofDevices] = useState<ItemData[]>([]);
  const [USBdevices, setListofUSBDevices] = useState<usbPrinterDevice[]>([]);
  const [currPrinter, setCurrPrinter] = useState<printerDevice | null>(null);
  const [currUSBPrinter, setCurrUsbPrinter] = useState<usbPrinterDevice | null>(
    null
  );
  const [blDevices, setListofBlDevices] = useState<printerDevice[]>([]);
  const [showFlatListNetwork, setShowFlatListNetwork] = useState<boolean>(true);
  const [showFlatListBT, setShowFlatListBT] = useState<boolean>(false);
  const [showFlatListUSB, setShowFlatListUSB] = useState<boolean>(false);

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  return (
    <SafeAreaView style={[backgroundStyle, { flex: 1 }]}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={[backgroundStyle, { flex: 1 }]}
      >
        <Header />
        <View
          style={{
            backgroundColor: isDarkMode ? Colors.black : Colors.white,
          }}
        >
          <Button
            title="Print with Graphic Image Wrapper"
            onPress={async () => {
              const Print =
                await EscPosImageWithTCPConnectionGraphicsImageWrapper(
                  base64Image,
                  ipAddress,
                  port,
                  'PARTIAL'
                );
              console.log(Print);
            }}
          />
          <Button
            title="Print with Bit Wrapper"
            onPress={async () => {
              const Print = await EscPosImageWithTCPConnectionBitImageWrapper(
                base64Image,
                ipAddress,
                port,
                'PARTIAL'
              );
              console.log(Print);
            }}
          />
          <Button
            title="Print with USB "
            onPress={async () => {
              try {
                console.log('triggered');
                const html = `
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>QR Code Generator</title>
                </head>
                <body>
                  <div id="qrcode">
                    <script src="https://cdn.jsdelivr.net/gh/davidshimjs/qrcodejs/qrcode.min.js"></script>
                    <script>
                      new QRCode(document.getElementById('qrcode'), {
                        text: 'http://jindo.dev.naver.com/collie',
                        width: 128,
                        height: 128,
                        colorDark: '#000',
                        colorLight: '#fff',
                        correctLevel: QRCode.CorrectLevel.H
                      });
                    </script>
                  </div>
                </body>
                </html>
                `;
                const base64 = await convertHTMLtoBase64(html, 576);
                const printUSB = await printUSBDevice(
                  currUSBPrinter!!.productId,
                  currUSBPrinter!!.vendorId,
                  base64,
                  'FULL'
                );
                console.log('This is printUSB', printUSB);
              } catch (error) {
                console.log('This is error usbPrinting', error);
              }
            }}
          />
          <Button
            title="Print with USB 2 "
            onPress={async () => {
              try {
                console.log('triggered');
                const html = `
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>QR Code Generator</title>
                </head>
                <body>
                  <div>
                   Hello
                  </div>
                </body>
                </html>
                `;
                const base64 = await convertHTMLtoBase64(html, 576);
                const printUSB = await printUSBDevice(
                  currUSBPrinter!!.productId,
                  currUSBPrinter!!.vendorId,
                  base64,
                  'FULL'
                );
                console.log('This is printUSB', printUSB);
              } catch (error) {
                console.log('This is error usbPrinting', error);
              }
            }}
          />
          <Button
            title="Print with Raster Bit Wrapper "
            onPress={async () => {
              const Print =
                await EscPosImageWithTCPConnectionRasterBitImageWrapper(
                  base64Image,
                  '192.168.1.1',
                  '9100',
                  'PARTIAL'
                );

              console.log(Print);
            }}
          />
          <Button
            title="Start Network Discovery"
            onPress={async () => {
              const check = await startNetworkDiscovery();
              console.log(check);
            }}
          />
          <Button
            title="Convert HTMl to Image"
            onPress={async () => {
              const Print = await convertHTMLtoBase64(
                '' +
                  '<html>\n' +
                  '<head>\n' +
                  '<style>\n' +
                  'body {\n' +
                  '  background-color: lightblue;\n' +
                  '}\n' +
                  '\n' +
                  'h1 {\n' +
                  '  text-align: center;\n' +
                  '}\n' +
                  '\n' +
                  'p {\n' +
                  '  font-family: verdana;\n' +
                  '  font-size: 20px;\n' +
                  '}\n' +
                  'p.korean {\n' +
                  '  font-family: Single Day;\n' +
                  '  font-size: 20px;\n' +
                  '}\n' +
                  '</style>\n' +
                  '</head>' +
                  '<body>' +
                  '<h1>Hello, world.</h1>' +
                  '<p>الصفحة الرئيسية \n' + // Arabiac
                  '<br>你好，世界 \n' + // Chinese
                  '<br>こんにちは世界 \n' + // Japanese
                  '<br>Привет мир \n' + // Russian
                  '<br>नमस्ते दुनिया \n' + //  Hindi
                  '<p class="korean"><br>안녕하세요 세계</p>' + // if necessary, you can download and install on your environment the Single Day from fonts.google...
                  '</body>',
                400
              );
            }}
          />
          <Button
            title="Open Drawer"
            onPress={async () => {
              const drawer = await openDrawer(ipAddress, port);
              console.log(drawer);
            }}
          />
          <Button
            title="Open Drawer USB"
            onPress={async () => {
              try {
                if (!currUSBPrinter) {
                  console.log('No USB printer selected');
                  return;
                }
                console.log('Opening drawer via USB...');
                const result = await openDrawerUSB(
                  currUSBPrinter.productId,
                  currUSBPrinter.vendorId
                );
                console.log('Open Drawer USB result:', result);
              } catch (error) {
                console.error('Error opening drawer via USB:', error);
              }
            }}
          />
          <Button
            title="startUSBDiscovery"
            onPress={async () => {
              console.log('In granted');
              const results: usbPrinterDevice[] =
                await searchUSBDevices().catch((error) => {
                  console.error('Error searching USB devices:', error);
                  return [];
                });

              console.log('This is results USB', results);
              setListofUSBDevices(results);
              setShowFlatListBT(false);
              setShowFlatListNetwork(false);
              setShowFlatListUSB(true);
              setListofDevices([]);
              console.log('Passed here ');
            }}
          />
          <Button
            title="startBTDiscovery"
            onPress={async () => {
              const requestBLPermissions = async () => {
                const res = await PermissionsAndroid.request(
                  PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION!!
                );
                await PermissionsAndroid.requestMultiple([
                  PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN!!,
                  PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT!!,
                ]);
                console.log(res);
              };
              await requestBLPermissions();
              const granted = await PermissionsAndroid.request(
                PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN!!,
                {
                  title: 'Android Scan Permission',
                  message: 'Scan Bluetooth Permission',
                  buttonNeutral: 'A ask Me Later',
                  buttonNegative: 'Cancel',
                  buttonPositive: 'OK',
                }
              );
              if (granted) {
                console.log('In granted');
                const results = await scanBLDevice();
                console.log('Passed Results');
                console.log(results);
                console.log(results.filtered_result);
                setListofBlDevices(results.filtered_result);
                setShowFlatListBT(false);
                setShowFlatListNetwork(false);
                setShowFlatListUSB(true);
                console.log('Passed here');
                setListofDevices([]);
              }
            }}
          />
          <Button
            title="getPairedDevices"
            onPress={async () => {
              const requestBLPermissions = async () => {
                const res = await PermissionsAndroid.request(
                  PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION!!
                );
                await PermissionsAndroid.requestMultiple([
                  PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN!!,
                  PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT!!,
                ]);
                console.log(res);
              };
              await requestBLPermissions();
              const granted = await PermissionsAndroid.request(
                PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN!!,
                {
                  title: 'Android Scan Permission',
                  message: 'Scan Bluetooth Permission',
                  buttonNeutral: 'A ask Me Later',
                  buttonNegative: 'Cancel',
                  buttonPositive: 'OK',
                }
              );
              if (granted) {
                const results = await getPairedDevices();
                console.log(results);
                setListofBlDevices(results);
                setShowFlatListBT(true);
                setShowFlatListNetwork(false);
                setListofDevices([]);
              }
            }}
          />

          <Button
            title="printImageByBluetooth"
            onPress={async () => {
              const html = `
              <!DOCTYPE html>
              <html lang="en">
              <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>QR Code Generator</title>
              </head>
              <body>
                <div id="qrcode">
                  <script src="https://cdn.jsdelivr.net/gh/davidshimjs/qrcodejs/qrcode.min.js"></script>
                  <script>
                    new QRCode(document.getElementById('qrcode'), {
                      text: 'http://jindo.dev.naver.com/collie',
                      width: 128,
                      height: 128,
                      colorDark: '#000',
                      colorLight: '#fff',
                      correctLevel: QRCode.CorrectLevel.H
                    });
                  </script>
                </div>
              </body>
              </html>
              `;
              const base64 = await convertHTMLtoBase64(html, 576);
              console.log('This is base64', base64);
              const sleep = async (duration: number) =>
                new Promise((resolve) => setTimeout(resolve, duration));
              for (let i = 2; i > 0; i--) {
                const result = await printImageByBluetooth(
                  currPrinter!!,
                  base64,
                  'PARTIAL'
                );
                await sleep(3000);
                await closePrinterSocket();
                console.log(result);
              }

              // setTimeout(async () => {
              //   // await 2 seconds to close Socket so that printer cut command is able to be carried out

              //   await closePrinterSocket();
              // }, 4000);
            }}
          />

          <Button
            title="Get Services Names "
            onPress={async () => {
              const result = await getListofServiceNames();
              console.log(result);
            }}
          />
          <Button
            title="cutByBluetooth"
            onPress={async () => {
              console.log('Button Here');
              const result = await printBLCut(currPrinter!!);

              setTimeout(async () => {
                // await 2 seconds to close Socket so that printer cut command is able to be carried out

                await closePrinterSocket();
              }, 2000);

              console.log(result);
            }}
          />
          <Text style={{ alignSelf: 'center', fontSize: 20, marginTop: 10 }}>
            Current IP Printer Device:{printerName}
          </Text>
          <Text style={{ alignSelf: 'center', fontSize: 20, marginTop: 10 }}>
            IPAddress:{ipAddress}
          </Text>
          <Text style={{ alignSelf: 'center', fontSize: 20, marginTop: 10 }}>
            Port:{port}
          </Text>
          <Text style={{ alignSelf: 'center', fontSize: 20, marginTop: 10 }}>
            Current BTPrinter:{currPrinter == null ? ' ' : currPrinter.name}{' '}
            {currPrinter == null ? ' ' : currPrinter.address}
          </Text>
          <Text style={{ alignSelf: 'center', fontSize: 20, marginTop: 10 }}>
            Current USB:{currUSBPrinter == null ? ' ' : currUSBPrinter.id}{' '}
            {currUSBPrinter == null ? ' ' : currUSBPrinter?.name}
          </Text>
          <View
            style={{
              marginTop: 20,
              backgroundColor: '#f0f0f0',
              borderRadius: 8,
              marginHorizontal: 10,
              marginBottom: 20,
            }}
          >
            <Text
              style={{
                textAlign: 'center',
                padding: 10,
                fontWeight: 'bold',
                fontSize: 16,
              }}
            >
              {showFlatListUSB
                ? 'USB Devices'
                : showFlatListBT
                ? 'Bluetooth Devices'
                : 'Network Devices'}
              {showFlatListUSB && ` (${USBdevices.length})`}
              {showFlatListBT && ` (${blDevices.length})`}
              {showFlatListNetwork && ` (${devices.length})`}
            </Text>
            {showFlatListBT &&
              blDevices.map((item) => (
                <TouchableOpacity
                  key={item.address}
                  onPress={() => setCurrPrinter(item)}
                  style={[
                    styles.item,
                    {
                      backgroundColor:
                        currPrinter?.address === item.address
                          ? '#00008B'
                          : 'blue',
                    },
                  ]}
                >
                  <Text style={[styles.title, { color: 'white' }]}>
                    {item.name}
                  </Text>
                  <Text style={[styles.title, { color: 'white' }]}>
                    {item.address}
                  </Text>
                </TouchableOpacity>
              ))}
            {showFlatListNetwork &&
              devices.map((item) => (
                <TouchableOpacity
                  key={item.printerIPAddress}
                  onPress={() => {
                    setIpAddress(item.printerIPAddress);
                    setPort(item.printerPort);
                    setPrinterName(item.printerName);
                  }}
                  style={[
                    styles.item,
                    {
                      backgroundColor:
                        ipAddress === item.printerIPAddress
                          ? '#00008B'
                          : 'blue',
                    },
                  ]}
                >
                  <Text style={[styles.title, { color: 'white' }]}>
                    {item.printerIPAddress}
                  </Text>
                  <Text style={[styles.title, { color: 'white' }]}>
                    {item.printerName}
                  </Text>
                  <Text style={[styles.title, { color: 'white' }]}>
                    {item.printerPort}
                  </Text>
                </TouchableOpacity>
              ))}
            {showFlatListUSB &&
              USBdevices.map((item) => (
                <TouchableOpacity
                  key={item.id}
                  onPress={() => setCurrUsbPrinter(item)}
                  style={[
                    styles.item,
                    {
                      backgroundColor:
                        currUSBPrinter?.id === item.id ? '#00008B' : 'blue',
                    },
                  ]}
                >
                  <Text style={[styles.title, { color: 'white' }]}>
                    ID: {item.id}
                  </Text>
                  <Text style={[styles.title, { color: 'white' }]}>
                    Name: {item.name}
                  </Text>
                  <Text style={[styles.title, { color: 'white' }]}>
                    Manufacturer: {item.manufacturerName}
                  </Text>
                  <Text style={[styles.title, { color: 'white' }]}>
                    VendorID: {item.vendorId}
                  </Text>
                  <Text style={[styles.title, { color: 'white' }]}>
                    ProductId: {item.productId}
                  </Text>
                  <Text style={[styles.title, { color: 'white' }]}>
                    ProductName: {item.productName}
                  </Text>
                </TouchableOpacity>
              ))}
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
type ItemData = {
  printerName: string;
  printerIPAddress: string;
  printerPort: string;
};

type ItemProps = {
  item: ItemData;
  onPress: () => void;
  backgroundColor: string;
  textColor: string;
};

const Item = ({ item, onPress, backgroundColor, textColor }: ItemProps) => (
  <TouchableOpacity
    onPress={onPress}
    style={[styles.item, { backgroundColor }]}
  >
    <Text style={[styles.title, { color: textColor }]}>
      {item.printerIPAddress}
    </Text>
    <Text style={[styles.title, { color: textColor }]}>{item.printerName}</Text>
    <Text style={[styles.title, { color: textColor }]}>{item.printerPort}</Text>
  </TouchableOpacity>
);

const UsbItem = ({ item, onPress, backgroundColor, textColor }: any) => (
  <TouchableOpacity
    onPress={onPress}
    style={[styles.item, { backgroundColor }]}
  >
    <Text style={[styles.title, { color: textColor }]}>ID: {item.id}</Text>
    <Text style={[styles.title, { color: textColor }]}>Name: {item.name}</Text>
    <Text style={[styles.title, { color: textColor }]}>
      Manufacturer Name:{item.manufacturerName}
    </Text>
    {/* <Text style={[styles.title, { color: textColor }]}>
      Serial Number:{item.serialNumber}
    </Text> */}
    <Text style={[styles.title, { color: textColor }]}>
      VendorID {item.vendorId}
    </Text>
    <Text style={[styles.title, { color: textColor }]}>
      Version: {item.version}
    </Text>
    <Text style={[styles.title, { color: textColor }]}>
      ProductId: {item.productId}
    </Text>
    <Text style={[styles.title, { color: textColor }]}>
      ProductName: {item.productName}
    </Text>
  </TouchableOpacity>
);
const styles = StyleSheet.create({
  sectionContainer: {
    marginTop: 32,
    paddingHorizontal: 24,
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '600',
  },
  sectionDescription: {
    marginTop: 8,
    fontSize: 18,
    fontWeight: '400',
  },
  highlight: {
    fontWeight: '700',
  },
  item: {
    flex: 1,
    padding: 20,
    marginVertical: 8,
    marginHorizontal: 16,
  },
  title: {
    fontSize: 10,
  },
  devicesContainer: {
    height: '300',
  },
});

export default App;
