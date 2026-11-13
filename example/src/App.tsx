import React, { useCallback, useState, useEffect } from 'react';
import { launchCamera, launchImageLibrary } from 'react-native-image-picker';
import {
  checkNotifications,
  PERMISSIONS,
  request,
} from 'react-native-permissions';

import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { OCRStudioSDKModule as OcrModule } from 'ocrstudiosdk';
import { EngineDescription } from './jsonClassEngineDescription';
import { ResultObject } from './jsonClassEngineResult';
import {
  StyleSheet,
  Button,
  Alert,
  View,
  Text,
  Image,
  NativeModules,
  NativeEventEmitter,
  Platform,
} from 'react-native';

import MaskModal from './modalDocMask';
import Loader from './loader';
import GridView from './gridView';


//const eventEmitter = new NativeEventEmitter(NativeModules.Idengine);
const isFacesEnabled = false;
// For preliminary Results
//let isPaused: boolean = false;
let onFrameUpdateSubscription: any;

const signature: string = 'INSERT_SIGNATURE_FROM_README.html\';

const onFrameUpdate = async (e: object) => {
  // Ignore onFrameUpdate stream
  if (!e) {
    return;
  }

  if (e.hasOwnProperty('frameIndex')) {
    let a = ['😉', '👺', '😡', '😱', '👮🏻‍♂️', '🚀', '🚢', '🚩'][
      Math.floor(Math.random() * 8)
    ];

    await OcrModule.setLabelText(`From Js: ${e.frameIndex} ${a}`);
  }
};


const getDocTypes = async () => {
  let types: Array<string> = [];

  try {
    // Parse json
    const jData: string = await OcrModule.engineDescription() ?? '';
    const parsedJson = JSON.parse(jData);
    //console.log(parsedJson);
    const targets = EngineDescription.fromJson(parsedJson);
    //console.log(JSON.stringify(targets))

    for (let v of targets.targetGroups) {
      types.push(`${v.targetGroupType}:${v.targetMasks[0]}`);
    }
  } catch { }
  return types;
};


function MainContent() {
  const defaultUrl = 'https://reactnative.dev/docs/assets/p_cat2.png';

  const [_version, _setVersion] = React.useState<string | undefined>();
  let [_info, _setInfo] = React.useState<string | undefined>();
  let [_docTypes, _setDocTypes] = useState<string[]>(['default:*']);
  const [_modalVisible, _setModalVisible] = useState(false);

  let [_faceA, _setFaceA] = useState(defaultUrl);
  let [_faceB, _setFaceB] = useState(defaultUrl);

  let [_mode, _setMode] = React.useState<string>();
  let [_mask, _setMask] = React.useState<string>();
  let [_showLoader, _setShowLoader] = React.useState<boolean>();

  let [_maskMode, _setMaskMode] = React.useState<string | 'Select mask'>();
  let [_result, _setResult] = React.useState<ResultObject | void>();

  const [_isLoading, _setIsLoading] = useState(true);

  // preload data
  React.useEffect(() => {
    async function call() {
      try {
        // 1. Init engine.
        // It is important to wait for a response before any engine call!
        console.log('1. Init engine...');
        OcrModule.init(signature).then(async () => {
          // 1.1 Get engine  description for UI purpose
          console.log('Init engine done');

          //_setDocTypes(await OcrModule.getDocumentsList());
          _setDocTypes(await getDocTypes());
          _setVersion(await OcrModule.getVersion());
          _setIsLoading(false);
        });
      } catch (e) {
        _setInfo('Exception init engine');
        console.error(e);
      }
    }
    call();
  }, []);

  useEffect(() => {
    faceComparison();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [_faceA, _faceB]);

  const onModalClose = () => {
    _setModalVisible(false);
  };

  const setMaskMode = (el: string) => {
    let a = el.split(':');
    _setMaskMode(el);
    _setMode(a[0]);
    _setMask(a[1]);
  };

  function isMaskEmpty() {
    if (_maskMode === undefined) {
      Alert.alert('Set document mask');
      return true;
    }
    return false;
  }

  async function Gallery(_isGallery = false) {
    clearFaces();
    try {
      if (isMaskEmpty()) {
        return;
      }

      let result;
      _setResult();
      if (_isGallery) {
        result = await launchImageLibrary({ mediaType: 'photo' });
      } else {
        result = await launchCamera({ mediaType: 'photo' });
      }

      console.log('image-picker:  ', result);


      if (result.didCancel) {
        _setInfo('Canceled');
        return;
      }

      // Get uri
      const uri = result?.assets?.[0]?.uri;
      if (uri === undefined) {
        _setInfo('Something wrong');
        return;
      }
      _setInfo('');
      _setShowLoader(true);

      const engineConfig = {
        mode: _mode,
        mask: [_mask],
        options: {
          'sessionTimeout': '5.0',
          'extractTemplateImages': 'true',
          //'fraudAttemtEnabled': 'true'
        },
      };

      // 2. Set settings
      console.log('2. Set settings...', engineConfig);
      await OcrModule.initSettings(engineConfig);
      //await IdEngine.setLabelText('Please scan document');

      // reset data store
      _setResult();

      // 3. Waiting result
      console.log('3. Waiting result...');
      let data: any = await OcrModule.fileRecognition(uri);

      // is canceled?
      if (data == null) {
        _setInfo('Canceled');
        _setShowLoader(false);
        return;
      }

      const map = JSON.parse(data);

      printResult(map);

    } catch (e) {
      _setInfo('Exception');
      console.error(e);

    } finally {
      _setShowLoader(false);
    }
  }

  const printResult = (map: Record<string, any>) => {
    const response = ResultObject.fromJson(map);
    console.log("response", response);

    const target = response.targets[0];
    const template = target?.items.template ?? {};

    // is document found
    if (Object.keys(template).length !== 0) {
      _setInfo("Found: " + Object.keys(template)[0]);

      const image = Object.values(target.items.image)[0];

      if (image?.name === 'photo') {
        console.log(image.name);
        _setFaceA('data:image/jpg;base64,' + image.image_base64);
      }

      _setResult(response);
    } else {
      _setInfo('Not Found');
    }
  };

  const faceComparison = async () => {
    const isValidImage = (i: string) =>
      i.startsWith('data:image/jpg') || i.startsWith('file:///');

    if (isValidImage(_faceA) && isValidImage(_faceB)) {
      _setShowLoader(true);
      try {
        const data: any = await OcrModule.faceComparison(_faceA, _faceB);
        //console.log(data);
        _setInfo(data);
      } catch (e) {
        _setInfo('Exception');
        console.error(e);

      } finally {
        _setShowLoader(false);
      }

    }
  };

  async function getSelfie(_faceID = 'faceA', _isGallery = false) {
    try {
      let result;

      if (_isGallery) {
        result = await launchImageLibrary({ mediaType: 'photo' });
      } else {
        result = await launchCamera({ mediaType: 'photo' });
      }

      console.log('image-picker:  ', result);

      if (result.didCancel) {
        _setInfo('Canceled');
        return;
      }

      // Get uri
      const uri = result?.assets?.[0]?.uri;
      if (uri === undefined) {
        _setInfo('Something wrong');
        return;
      }

      if (_faceID === 'faceA') {
        _setFaceA(uri);
      }

      if (_faceID === 'faceB') {
        _setFaceB(uri);
      }
    } catch (e) {
      _setInfo('Exception');
      console.error(e);

    }
  }

  function clearFaces() {
    _setFaceA(defaultUrl);
    _setFaceB(defaultUrl);
  }

  async function Camera() {
    clearFaces();

    _setShowLoader(true);
    try {
      // Create listener for callbacks onFrameUpdate,
      // Do not forget to remove listener
      // onFrameUpdateSubscription = eventEmitter.addListener(
      //  'onFrameUpdate',
      //  onFrameUpdate
      // );

      if (isMaskEmpty()) {
        return;
      }

      let p = await request(Platform.OS === 'ios' ? PERMISSIONS.IOS.CAMERA : PERMISSIONS.ANDROID.CAMERA);
      console.log('Camera permissions: ', p);

      const engineConfig = {
        mode: _mode,
        mask: [_mask],
        options: {
          'sessionTimeout': '5.0',
          'extractTemplateImages': 'true',
        },
      };

      // 2. Set settings
      console.log('2. Set settings...', engineConfig);
      await OcrModule.initSettings(engineConfig);
      //await OcrModule.setLabelText('Please scan document');

      // reset data store
      _setResult();

      // 3. Waiting result
      console.log('3. Waiting result...');
      const data: any = await OcrModule.startRecognition();
      //console.log(data);

      // is canceled?
      if (data == null) {
        _setInfo('Canceled');
        return;
      }

      const map = JSON.parse(data);
      printResult(map);

    } catch (e) {
      _setInfo('Exception');
      console.error(e);
    } finally {
      _setShowLoader(false);
    }
  }

  if (_isLoading) {
    return (
      <SafeAreaView style={styles.main} edges={['top', 'bottom']}>
        <View style={styles.container}>
          <Text style={styles.centerText}>
            {_isLoading ? 'Loading...' : 'Done'}
          </Text>
        </View>
      </SafeAreaView>
    );
  } else {
    return (
      <SafeAreaView style={styles.main} edges={['top', 'bottom']}>
        {isFacesEnabled && (
          <View style={stylesPhoto.photoContainer}>
            <View style={stylesPhoto.facea}>
              <View style={stylesPhoto.photoImg}>
                <Image
                  source={{ uri: _faceA }}
                  style={{ width: 100, height: 100, alignSelf: 'center' }}
                />
              </View>
              <View style={stylesPhoto.photoBcontainer}>
                <Button
                  title="Photo A"
                  onPress={() => getSelfie('faceA', false)}
                />
                <Button
                  title="Upload A"
                  onPress={() => getSelfie('faceA', true)}
                />
              </View>
            </View>
            <View style={stylesPhoto.faceb}>
              <View style={stylesPhoto.photoImg}>
                <Image
                  source={{ uri: _faceB }}
                  style={{ width: 100, height: 100, alignSelf: 'center' }}
                />
              </View>
              <View style={stylesPhoto.photoBcontainer}>
                <Button
                  title="Photo B"
                  onPress={() => getSelfie('faceB', false)}
                />
                <Button
                  title="Upload B"
                  onPress={() => getSelfie('faceB', true)}
                />
              </View>
            </View>
          </View>
        )}
        <View style={styles.contentContainer}>
          {_showLoader ? (
            <View style={styles.loaderContainer}>
              <Loader isLoading={true} />
            </View>
          ) : (
            <View style={styles.info}>
              <Text style={styles.header}>{_info}</Text>
            </View>
          )}
        </View>

        <MaskModal
          isVisible={_modalVisible}
          setMaskMode={setMaskMode}
          elements={_docTypes}
          onClose={onModalClose}
        />
        <View style={styles.container}>
          <View style={styles.scrollContainer}>
            <GridView
              style={styles.scroll}
              result={_result}
              setFaceA={_setFaceA}
            />
            <View style={styles.backImg}>
              <Image
                source={{
                  uri: 'https://reactnative.dev/docs/assets/p_cat2.png',
                }}
                style={{ width: 150, height: 150, alignSelf: 'center' }}
              />
            </View>
          </View>
        </View>
        <View style={styles.mask_field}>
          <Text>{_maskMode}</Text>
        </View>
        <View style={styles.button_container}>
          <View style={styles.fixToText}>
            <Button title="Mask" onPress={() => _setModalVisible(true)} />
            <Button
              title="Scan"
              onPress={() => {
                Camera();
              }}
            />
            <Button title="Upload" onPress={() => Gallery(true)} />
          </View>
        </View>
        <View style={styles.footer}>
          <Text>Version: {_version}</Text>
        </View>
      </SafeAreaView>
    );
  }
}


export default function App() {
  return (
    <SafeAreaProvider>
      <MainContent />
    </SafeAreaProvider>
  );
}


const stylesPhoto = StyleSheet.create({
  photoContainer: {
    flex: 0,
    flexDirection: 'row',
    gap: 5,
  },
  photoBcontainer: {
    flex: 0,
    flexDirection: 'row',
    justifyContent: 'center',
  },
  facea: {
    backgroundColor: '#eaeaea',
    borderRadius: 8,
    padding: 6
  },
  faceb: {
    backgroundColor: '#eaeaea',
    borderRadius: 8,
    padding: 6
  },
  photoImg: {
    justifyContent: 'center',
  },
});

const styles = StyleSheet.create({
  header: {
    fontSize: 18,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  loaderContainer: {
    flex: 0,
    justifyContent: 'center',
  },
  contentContainer: {},

  scroll: {
    zIndex: 1,
    justifyContent: 'center',
  },
backImg: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: -1, // Оставляем на заднем плане
  },
  fixToText: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  mask_field: {
    flexDirection: 'row',
    justifyContent: 'center',
  },
  center: {
    justifyContent: 'center',
  },
  centerText: {
    textAlign: 'center',
  },
  button_container: {
    minWidth: 230,
  },
  h1: {
    fontSize: 16,
  },
  table: {},
  titleContainer: {
    maxWidth: '100%',
  },
  valueContainer: {},
  dot: {
    fontSize: 14,
    paddingRight: 14,
    paddingLeft: 5,
  },
  item: {
    margin: 1,
    padding: 6,
    backgroundColor: '#FFF',
    flex: 1,
    alignSelf: 'center',
    flexDirection: 'row',
    borderRadius: 5,
    minHeight: 50,
    minWidth: '100%',
  },
  title: {
    width: '100%',
    fontSize: 13,
    color: '#7a7a7a',
  },
  attrContainer: {
    fontSize: 12,
    flexShrink: 1,
    alignItems: 'flex-start',
    color: '#7a7a7a',
  },
  attr: {
    backgroundColor: '#e5e5e5',
    paddingHorizontal: 8,
    paddingVertical: 2,
    marginTop: 2,
    fontSize: 12,
    borderRadius: 6,
    color: '#7a7a7a',
  },
  value: {
    fontSize: 16,
  },
  scrollContainer: {
    backgroundColor: '#eaeaea',
    flex: 1,
    justifyContent: 'flex-start',
    position: 'relative'
  },
  container: {
    flex: 1,
    alignSelf: 'stretch',
    justifyContent: 'flex-start',
  },

  main: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'flex-start',
  },
  info: {
    textAlign: 'center',
    maxWidth: 250,
  },
  footer: {
    flexGrow: 0,
    alignItems: 'center',
    justifyContent: 'center',
    flex: 0,
    paddingBottom: 10,
  },
});
