#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import "generated/RNOcrstudiosdkSpec/RNOcrstudiosdkSpec.h"
  @interface Ocrstudiosdk : RCTEventEmitter <NativeOcrstudiosdkSpec>
#else
  // old architecture
  @interface Ocrstudiosdk : RCTEventEmitter <RCTBridgeModule>
#endif
@end
