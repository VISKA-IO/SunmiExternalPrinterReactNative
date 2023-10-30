
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNSunmiExternalPrinterReactNativeSpec.h"

@interface SunmiExternalPrinterReactNative : NSObject <NativeSunmiExternalPrinterReactNativeSpec>
#else
#import <React/RCTBridgeModule.h>

@interface SunmiExternalPrinterReactNative : NSObject <RCTBridgeModule>
#endif

@end
