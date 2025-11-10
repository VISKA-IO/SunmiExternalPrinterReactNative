package com.sunmiexternalprinter

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

/**
 * React Native package class for the Sunmi External Printer module.
 * 
 * This class serves as the entry point for registering the native printer
 * functionality with React Native. It implements ReactPackage to provide
 * the bridge between JavaScript and native Android printer operations.
 * 
 * The package creates and manages the SunmiExternalPrinterReactNativeModule
 * instance that contains all the printer functionality. It does not provide
 * any custom UI components (ViewManagers).
 * 
 * This class should be registered in the React Native application's
 * MainApplication.java file to make printer functionality available
 * to JavaScript code.
 * 
 * @author Sunmi External Printer Team
 * @since 1.0.0
 */
class SunmiExternalPrinterReactNativePackage : ReactPackage {
  
  /**
   * Creates and returns the list of native modules provided by this package.
   * 
   * @param reactContext React Native application context
   * @return List containing the SunmiExternalPrinterReactNativeModule instance
   */
  override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
    return listOf(SunmiExternalPrinterReactNativeModule(reactContext))
  }

  /**
   * Creates and returns the list of view managers provided by this package.
   * 
   * This package does not provide any custom UI components, so returns
   * an empty list.
   * 
   * @param reactContext React Native application context
   * @return Empty list (no custom view managers)
   */
  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
    return emptyList()
  }
}
