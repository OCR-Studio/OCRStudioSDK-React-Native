#import "Ocrstudiosdk.h"

#ifdef RCT_NEW_ARCH_ENABLED
 #import "generated/RNOcrstudiosdkSpec/RNOcrstudiosdkSpec.h"
#endif

#import "OCRStudioSDKInstance.h"
#import "OCRStudioSDKViewController.h"


@interface Ocrstudiosdk() <OCRStudioSDKViewControllerDelegate, OCRStudioSDKInitializationDelegate>
@property(strong, nonatomic) OCRStudioSDKInstance *engineInstance;
@property(strong, nonatomic) OCRStudioSDKViewController *ocrViewController;

@property(atomic) RCTPromiseResolveBlock response_resolver;
@property(atomic) RCTPromiseRejectBlock response_rejecter;

@end

#pragma mark - init

@implementation Ocrstudiosdk {
  bool hasListeners;
}

RCT_EXPORT_MODULE()

// Will be called when this module's first listener is added.
-(void)startObserving {
  hasListeners = YES;
  // Set up any upstream listeners or background tasks as necessary
}

// Will be called when this module's last listener is removed, or on dealloc.
-(void)stopObserving {
  hasListeners = NO;
  // Remove upstream listeners, stop unnecessary background tasks
}

- (NSArray<NSString *> *)supportedEvents {
  return @[@"onFrameUpdate"];
}

- (void)init:(nonnull NSString *)signature
     resolve:(nonnull RCTPromiseResolveBlock)resolve
      reject:(nonnull RCTPromiseRejectBlock)reject {
  
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    @try {
      if (![self.engineInstance engineInitialized]) {
        self.engineInstance = [[OCRStudioSDKInstance alloc] initWithSignature:signature];
        [self.engineInstance initializeEngine:[self pathForSingleDataArchive]];
        NSLog(@">>>>>>>>>>>> engineInstance first load <<<<<<<<<<<<");
      }
      resolve(nil);
    } @catch (NSException *e) {
      reject(@"init", e.reason, nil);
    }
  });
}

- (void)initSettings:(nonnull NSDictionary *)settings
             resolve:(nonnull RCTPromiseResolveBlock)resolve
              reject:(nonnull RCTPromiseRejectBlock)reject {
  @try {
    
    if (![self.engineInstance engineInitialized]) {
      NSException* exc = [NSException
                          exceptionWithName:@"EngineInstanceError"
                          reason:@"initSettings cannot call while engine is not yet initialized"
                          userInfo:nil];
      @throw exc;
    }


    NSString *mode = settings[@"mode"];
    NSArray *mask = settings[@"mask"];
    NSDictionary *options = settings[@"options"];
    
    dispatch_async(dispatch_get_main_queue(), ^{
      // attach engine
      self.ocrViewController = [[OCRStudioSDKViewController alloc] init];
      [self.ocrViewController attachEngineInstance:self.engineInstance];
      // for callbacks
      self.ocrViewController.ocrDelegate = self;
      
      [self.ocrViewController.sessionParams setTargetGroupType:(mode)];
      [self.ocrViewController.sessionParams clearTargetMasks];
      [self.ocrViewController.sessionParams setTargetMasks:(mask)];
      [self.ocrViewController.sessionParams clearOptions];
      
      for (id key in options) {
        [self.ocrViewController.sessionParams setOptionWithName:key to:[options objectForKey:key]];
      }
      
      resolve(nil);
    });
    
  } @catch (NSException *e) {
    reject(@"initSettings", e.reason, nil);
  }
}

- (void)startRecognition:(nonnull RCTPromiseResolveBlock)resolve
                  reject:(nonnull RCTPromiseRejectBlock)reject {
  @try {
    
    // Save callback. We will use it after recognition in another method
    self->_response_resolver = resolve;
    self->_response_rejecter = reject;
    
    if (![self.engineInstance engineInitialized]) {
      NSException* exc = [NSException
                          exceptionWithName:@"EngineInstanceError"
                          reason:@"startRecognition cannot call while engine is not yet initialized"
                          userInfo:nil];
      @throw exc;
    }
    
    dispatch_async(dispatch_get_main_queue(), ^{
      
      [self.ocrViewController.sessionParams setSessionType:(@"video_recognition")];
      [[self ocrViewController] setCaptureButtonDelegate:[self ocrViewController]];
      
      self.ocrViewController.displayDocumentQuadrangle = YES;
      self.ocrViewController.displayZonesQuadrangles = YES;
      self.ocrViewController.displayProcessingFeedback = YES;
      
      // [[self engineInstance] initVideoSession];
      // delete dismissSession for immediately recognition
      // [[self engineInstance] dismissVideoSession];
      
      UINavigationController *contactNavigator = [[UINavigationController alloc]
                                                  initWithRootViewController:self.ocrViewController];
      contactNavigator.modalPresentationStyle = UIModalPresentationFullScreen;
      contactNavigator.navigationBar.hidden = YES;
      [[[UIApplication sharedApplication] delegate].window.rootViewController
       presentViewController:contactNavigator
       animated:NO
       completion:nil];
    });
    
  } @catch (NSException *e) {
    _response_rejecter(@"startRecognition", e.reason, nil);
  }
}

#pragma mark - file recogntion

- (void)fileRecognition:(nonnull NSString *)uri
                resolve:(nonnull RCTPromiseResolveBlock)resolve
                 reject:(nonnull RCTPromiseRejectBlock)reject {
  
  [self.ocrViewController.sessionParams setSessionType:(@"document_recognition")];
  
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    @try {
      NSURL *imageURL = [NSURL URLWithString:uri];
      NSData *imageData = [NSData dataWithContentsOfURL:imageURL];
      
      if(!imageData) {
        return reject(@"Error", @"input image not found", nil);
      }
      
      UIImage* image = [UIImage imageWithData:imageData];
      int rotationRules[] = {0, 2, 3, 1, 0, 2, 1, 3};
      OBJCOCRStudioSDKResult* imageResult = [self.engineInstance processSingleImageFromUIImage:image rotations:rotationRules[image.imageOrientation]];
      NSString* json_result = [imageResult.getRef serialize];

      resolve(json_result);
      
    } @catch (NSException *e) {
      reject(@"fileRecognition", e.reason, nil);
    }
  });
}

- (void)faceComparison:(nonnull NSString *)facea
                 faceb:(nonnull NSString *)faceb
               resolve:(nonnull RCTPromiseResolveBlock)resolve
                reject:(nonnull RCTPromiseRejectBlock)reject {
  
  // Dispatching math operations to a background queue
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    @try {
      OBJCOCRStudioSDKImageRef *imageA = [[self stringParser2Image:facea] getRef];
      OBJCOCRStudioSDKImageRef *imageB = [[self stringParser2Image:faceb] getRef];

      // Fix: Now actually comparing imageA with imageB instead of imageA with imageA
      OBJCOCRStudioSDKResult *result = [[self engineInstance] compareFacesFromDocument:imageA andSelfie:imageB];
      
      if (!result) {
        return reject(@"faceComparison", @"Engine returned null result", nil);
      }
                
      OBJCOCRStudioSDKTargetRef* target = [[result getRef] targetByIndex:0];
      if (!target) {
        return reject(@"faceComparison", @"No targets found in matching result", nil);
      }

      OBJCOCRStudioSDKItemIterator* item_it = [target itemsBegin:@"string"];
      OBJCOCRStudioSDKItemIterator* item_end = [target itemsEnd:@"string"];

      NSString* status = @"";
      NSString* similarity_estimation = @"";

      for (; ![item_it isEqualTo:item_end]; [item_it step]) {
        OBJCOCRStudioSDKItemRef *item = [item_it item];

        if ([[item name] isEqualToString:@"status"]) {
            status = [item value];
        }
        if ([[item name] isEqualToString:@"similarity_estimation"]) {
            similarity_estimation = [item value];
        }
      }
      
      NSLog(@"Face score: %@", similarity_estimation);
      resolve(similarity_estimation);
      
    } @catch (NSException *e) {
      reject(@"faceComparison", e.reason, nil);
    }
  });
}

- (void)setLabelText:(nonnull NSString *)text
             resolve:(nonnull RCTPromiseResolveBlock)resolve
              reject:(nonnull RCTPromiseRejectBlock)reject {
  @try {
    dispatch_async(dispatch_get_main_queue(), ^{
      [self.ocrViewController configureDocumentTypeLabel:text];
      resolve(nil);
    });
  } @catch (NSException *e) {
    reject(@"SetLabelText", e.reason, nil);
  }
}

- (void)getVersion:(nonnull RCTPromiseResolveBlock)resolve
            reject:(nonnull RCTPromiseRejectBlock)reject {
  @try {
    NSString *version = [OBJCOCRStudioSDKInstance libraryVersion];
    resolve(version);
  } @catch (NSException *exception) {
    reject(@"get_version_error", exception.reason, nil);
  }
}

#pragma mark - engine description

- (void)engineDescription:(nonnull RCTPromiseResolveBlock)resolve
            reject:(nonnull RCTPromiseRejectBlock)reject {
  if (![self.engineInstance engineInitialized]) {
    NSException* exc = [NSException
                        exceptionWithName:@"EngineInstanceError"
                        reason:@"engineDescription cannot call while engine is not yet initialized"
                        userInfo:nil];
    @throw exc;
  }
  
  @try {
    NSString *desc = [[self.engineInstance engine] description];
    resolve(desc);
  } @catch (NSException *e) {
    reject(@"engineDescription", e.reason, nil);
  }
}

- (void)setNativeTimeout:(double)ms resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  
}


- (void)startRecognitionVAuth:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  
}


#pragma mark Callbacks

- (void) OCRStudioSDKInitialized {
  NSLog(@">>>>>>>>>>>> engineInstance first load <<<<<<<<<<<<");
}

- (OBJCOCRStudioSDKImage *)stringParser2Image:(NSString *)img {
  
  NSString *prefix = @"data:image/jpg;base64,";
  if ([img hasPrefix:prefix]) {
    NSString *cleanedImage = [img substringFromIndex:prefix.length];
    
    NSString *trimmedString = [cleanedImage stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
    return [[OBJCOCRStudioSDKImage alloc] initFromBase64FileBuffer:trimmedString
                                                    withPageNumber:0
                                                      withMaxWidth:5000
                                                     withMaxHeight:5000];
    
  } else if ([img hasPrefix:@"file:///"]) {
    NSURL *fileURL = [NSURL fileURLWithPath:[img stringByReplacingOccurrencesOfString:@"file://" withString:@""]];
    UIImage *fileImage = nil;
    fileImage = [UIImage imageWithContentsOfFile:fileURL.path];
    if (!fileImage) {
        @throw [NSException exceptionWithName:@"ImageLoadError" reason:@"Could not load image from path" userInfo:nil];
    }
    return [[OBJCOCRStudioSDKImage alloc] initFromUIImage:fileImage];
    
  } else {
    @throw [NSException exceptionWithName:@"UnsupportedImageFormatException" reason:@"Unsupported image format" userInfo:nil];
  }
  return nil;
}

- (void)ocrViewControllerDidActivationRequest:(nonnull NSString *)dynKey
                    withCompletionHandler:(void (^)(NSString * _Nonnull actKey))completionHandler {
  
  NSLog(@"DidActivationRequest");
  
  NSString *apiKey = @"";
  NSString *actUrl = @"";
  
  // Fill json string
  NSString *jsonString = [NSString stringWithFormat: @"{\"action\": \"activate_id_session\", \"message\": \"%@\"}", dynKey];
  // Encode to bytes
  NSData *body = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
  
  NSMutableURLRequest *urlRequest = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:actUrl]];
  [urlRequest setHTTPMethod:@"POST"];
  NSLog(@"Sending http request...");
  [urlRequest setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
  [urlRequest addValue:@"application/json" forHTTPHeaderField:@"accept"];
  [urlRequest addValue:apiKey forHTTPHeaderField:@"X-API-Key"];
  
  [urlRequest setHTTPBody: body];
  urlRequest.timeoutInterval = 4;
  
  NSURLSession *session = [NSURLSession sharedSession];
  NSURLSessionDataTask *dataTask = [session dataTaskWithRequest: urlRequest completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
    NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *) response;
    if (httpResponse.statusCode == 200) {
      
      NSDictionary *response = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
      
      NSString *actKey = [response valueForKey:@"message"];
      completionHandler(actKey);
      
    } else {
      NSLog(@"Error during HTTP Connection");
    }
  }];
  [dataTask resume];
}

#pragma mark - result callback

NSInteger frameCounter = 0;
- (void)ocrViewControllerDidRecognize:(OBJCOCRStudioSDKResult *)result
                           fromBuffer:(CMSampleBufferRef)buffer {
  
  NSLog(@">>>>>>>>>>>> RESULT <<<<<<<<<<<<");
  frameCounter++;
  
  /* Send to UI preliminary Results. We send number of frames, for example. */
  NSMutableDictionary *dictField = [[NSMutableDictionary alloc] init];
  [dictField setValue:@(frameCounter) forKey:@"frameIndex"];
  
  /* Send to UI preliminary Results. We send number of frames, for example. */
  if (hasListeners) {
    // NSLog(@"Sending result...[default mode]");
    [self sendEventWithName:@"onFrameUpdate" body:@{@"frameIndex": @(frameCounter) }];
  }
  
  // If is not in terminal condition, engines wait for more frames for recognition
  if (![result.getRef allTargetsFinal]) {
    return;
  }
  
  // dismiss view controller
  [self.ocrViewController dismissViewControllerAnimated:YES completion:nil];
  
  frameCounter = 0;
  
  NSString* json_result = [result.getRef serialize];
  _response_resolver(json_result);
}

// File recognition callback
- (void) ocrViewControllerDidRecognizeSingleImage:(OBJCOCRStudioSDKResult *)result {
  // NSString* json_result = [result.getRef serialize];
  //_response_resolver(json_result);
}


// Cancel button event
- (void) ocrViewControllerDidCancel {
  // dismiss view controller
  [self.ocrViewController dismissViewControllerAnimated:YES completion:nil];
  _response_rejecter(@"Canceled", @"Canceled by user", nil);
}

- (NSString *) pathForSingleDataArchive {
  NSBundle *bundle = [NSBundle bundleForClass:[self classForCoder]];
  NSString *dataPath = [bundle pathForResource:@"ocrresources.bundle" ofType:nil];
  NSArray *listdir = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:dataPath error:nil];
  NSPredicate *zipFilter = [NSPredicate predicateWithFormat:@"self ENDSWITH '.ocr'"];
  NSArray *zipArchives = [listdir filteredArrayUsingPredicate:zipFilter];
  NSAssert(zipArchives.count == 1, @"data folder must contain single .ocr archive");
  NSString *zipName = [zipArchives objectAtIndex:0];
  NSString *zipPath = [dataPath stringByAppendingPathComponent:zipName];
  return zipPath;
}

// Don't compile this code when we build for the old architecture.
#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeOcrstudiosdkSpecJSI>(params);
}
#endif

#pragma mark old Architecture

#ifndef RCT_NEW_ARCH_ENABLED

RCT_REMAP_METHOD(init,
                 signature: (NSString *)signature
                 initWithResolver: (RCTPromiseResolveBlock)resolve
                 rejecter: (RCTPromiseRejectBlock)reject) {
  [self init:signature resolve:resolve reject:reject];
}

RCT_REMAP_METHOD(initSettings,
                 settings : (NSDictionary *)settings
                 initWithResolver : (RCTPromiseResolveBlock)resolve
                 rejecter : (RCTPromiseRejectBlock)reject) {
  
  [self initSettings:settings resolve:resolve reject:reject];
}

RCT_REMAP_METHOD(startRecognition,
                 startRecognitionTypesWithResolver:(RCTPromiseResolveBlock)result
                 rejecter:(RCTPromiseRejectBlock)reject) {
  [self startRecognition:result reject:reject];
}

RCT_EXPORT_METHOD(fileRecognition: (NSString *)uri
                  resolver: (RCTPromiseResolveBlock)result
                  rejecter: (RCTPromiseRejectBlock)reject ) {
  [self fileRecognition:uri resolve:result reject:reject];
}

RCT_REMAP_METHOD(faceComparison,
                 faceA: (NSString *)faceA
                 faceB: (NSString *)faceB
                 resolver: (RCTPromiseResolveBlock)resolve
                 rejecter: (RCTPromiseRejectBlock)reject) {
  [self faceComparison:faceA faceb:faceB resolve:resolve reject:reject];
}

RCT_EXPORT_METHOD(getVersion:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  [self getVersion:resolve reject:reject];
}

RCT_REMAP_METHOD(setLabelText,
                 text: (NSString *)text
                 resolve: (RCTPromiseResolveBlock)resolve
                 reject: (RCTPromiseRejectBlock)reject) {
  [self setLabelText:text resolve:resolve reject:reject];
}

RCT_REMAP_METHOD(engineDescription,
                 resolver: (RCTPromiseResolveBlock)resolve
                 rejecter: (RCTPromiseRejectBlock)reject) {
  [self engineDescription:resolve reject:reject];
}

#endif

@end
