import React from 'react';
import {
  Alert,
  Modal,
  StyleSheet,
  Text,
  Pressable,
  View,
  Button,
  FlatList,
  StatusBar,
} from 'react-native';

import { SafeAreaView } from 'react-native-safe-area-context';

const MaskModal = ({ isVisible, elements, setMaskMode, onClose }) => {
  const renderItem = ({ item }) => (
    <Pressable
      onPress={() => {
        setMaskMode(item);
        onClose();
      }}
    >
      <Text style={styles.itemText}>{item}</Text>
    </Pressable>
  );

  return (
    <Modal
      animationType="fade"
      transparent={true}
      visible={isVisible}
      onRequestClose={() => {
        Alert.alert('Modal has been closed.');
        onClose();
      }}
    >
      <View style={styles.centeredView}>
        <View style={styles.modalView}>
          <SafeAreaView style={styles.safeAreaView}>
            <FlatList // FlatList much faster for long lists
              data={elements}
              renderItem={renderItem}
              keyExtractor={(item, index) => index.toString()} 
              style={styles.scrollView}
            />
          </SafeAreaView>
          <View style={styles.closeButtonContainer}>
            <Button title="Close Modal" onPress={onClose} />
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  centeredView: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
  },
  modalView: {
    maxHeight: '60%',
    margin: 20,
    backgroundColor: 'white',
    borderRadius: 4,
    padding: 10,
    alignSelf: 'stretch',
    flex: 1,
    alignItems: 'stretch',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 0,
  },
  safeAreaView: {
    flex: 1,
    paddingTop: StatusBar.currentHeight,
    alignItems: 'stretch',
  },
  scrollView: {
    marginHorizontal: 20,
  },
  itemText: {
    textAlign: 'left',
    marginTop: 5,
    fontSize: 16,
    paddingBottom: 12,
    paddingTop: 7,
    borderBottomWidth: 1,
    borderBottomColor: '#dfdfdf',
  },
  closeButtonContainer: {
    marginTop: 20,
  },
});

export default MaskModal;