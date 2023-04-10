import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'sunmi-external-printer' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const SunmiExternalPrinterReactNative =
  NativeModules.SunmiExternalPrinterReactNative
    ? NativeModules.SunmiExternalPrinterReactNative
    : new Proxy(
        {},
        {
          get() {
            throw new Error(LINKING_ERROR);
          },
        }
      );

export function multiply(a: number, b: number): Promise<number> {
  return SunmiExternalPrinterReactNative.multiply(a, b);
}
export const SunmiSetBTPrinter = async () => {
  try {
    return await SunmiExternalPrinterReactNative.setBTPrinter();
  } catch (error) {
    return error;
  }
};
export const SunmiConnect = async () => {
  try {
    return await SunmiExternalPrinterReactNative.connect();
  } catch (error) {
    return error;
  }
};

export const SunmiPrintImage = async (base64Image: string) => {
  try {
    return await SunmiExternalPrinterReactNative.printImage(base64Image);
  } catch (error) {
    return error;
  }
};
