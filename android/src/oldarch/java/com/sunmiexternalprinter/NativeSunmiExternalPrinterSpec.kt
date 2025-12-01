package com.sunmiexternalprinter

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule

/**
 * Base class for old architecture (bridge) compatibility.
 * This is used when New Architecture is disabled.
 */
abstract class NativeSunmiExternalPrinterSpec(context: ReactApplicationContext) :
    ReactContextBaseJavaModule(context)
