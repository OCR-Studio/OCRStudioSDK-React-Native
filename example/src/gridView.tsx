import {
  Image,
  SectionList,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import type { ResultObject } from './jsonClassEngineResult';
import { SafeAreaView } from 'react-native-safe-area-context';
import React from 'react';

const GridView = ({ result }: {result: ResultObject | undefined | void;}) => {
  
  //console.log("result: ", result)

  if (!result) {
    console.log('Result undefined');
    return null;
  }

  //console.log("objc", Object.entries(result.items.string))

  const DATAlists = [];

  const target = result?.targets?.[0];

  DATAlists.push({
    title: 'Fields',
    sectionId: 'doc',
    data: Object.entries(target?.items?.string ?? {}),
  });

  DATAlists.push({
    title: 'Images',
    sectionId: 'image',
    data: Object.entries(target?.items?.image ?? {}),
  });

  // eslint-disable-next-line react/no-unstable-nested-components
  const Item = ({ el, section }) => {

    let attr: any[] = [];
    let item = el[1];

    const title = item.name;
    const val = item.value;
    const attributes = item.attributes;
    const accepted = item.accepted;

    let itemContainer = <Text style={styles.value}>{val}</Text>;
    
    console.log('accepted', accepted);
    
    let dotContainer = (
      <View>
        <Text style={{ ...styles.dot, color: accepted ? 'green' : 'red' }}>
          {'\u2B24'}
        </Text>
      </View>
    );

    if (attributes) {
      Object.entries(attributes)?.map(([key, value]) => {
        attr.push(
          <Text style={styles.attr} key={key}>
            {key} : {value}
          </Text>
        );
      });
    }
    // console.log(value);
    if (section.sectionId === 'image') {
      // console.log("item: ",item?.image_base64)
      itemContainer = (
        <View>
          <Image
            style={styles.img}
            source={{ uri: 'data:image/jpg;base64,' + item?.image_base64 }}
          />
        </View>
      );
      dotContainer = <View />;
    }

    return (
      <View style={styles.item}>
        <View style={styles.titleContainer}>
          <View>{itemContainer}</View>
          <View>
            <View>
              <Text style={styles.title}>{title}</Text>
            </View>
            <View style={styles.attrContainer}>{attr}</View>
          </View>
        </View>
        <View style={styles.dotContainer}>{dotContainer}</View>
      </View>
    );
  };

  const renderItem = ({ item, index, section }) => {
    return ( <Item el={item} section={section} /> );
  };

  const styles = StyleSheet.create({
    container: {
      paddingHorizontal: 12,
      alignItems: 'center',
      //justifyContent: 'center',
      flexDirection: 'row',
    },

    item: {
      margin: 1,
      padding: 6,
      backgroundColor: '#FFF',
      //width: '100%',
      //alignSelf: 'center',
      flexDirection: 'row',
      borderRadius: 6,
      minHeight: 50,
    },
    img: {
      minWidth: '100%',
      height: 150,
      resizeMode: 'contain',
      backgroundColor: '#f5f5f5',
    },

    table: {},
    titleContainer: {
      flexGrow: 1,
      flexShrink: 1,
    },
    dotContainer: {
      flexGrow: 0,
      flexShrink: 0,
    },

    dot: {
      fontSize: 16,
      paddingRight: 5,
      paddingLeft: 14,
      marginTop: 0,
    },

    title: {
      fontSize: 13,
      color: '#7a7a7a'
    },
    attrContainer: {
      alignItems: 'flex-start',
    },
    attr: {
      borderRadius: 8,
      backgroundColor: '#e5e5e5',
      paddingHorizontal: 8,
      paddingVertical: 2,
      marginTop: 2,
      fontSize: 12,
      color: '#7a7a7a',
    },
    value: {
      fontSize: 16,
    }
  });


  // console.log(SE.fields);

  return (
    <SafeAreaView style={styles.container}>
      <SectionList
        style={{ paddingBottom: 26 }}
        sections={DATAlists}
        stickySectionHeadersEnabled={false}
        //keyExtractor={(item, index) => item + index}
        renderItem={renderItem}
        renderSectionHeader={({ section: { title } }) => (
          <Text style={{ fontSize: 18, fontWeight: 'bold', paddingTop: 26 }}>
            {title}
          </Text>
        )}
        ListFooterComponent={<Text style={{ paddingTop: 26 }}> </Text>}
      />
    </SafeAreaView>
  );
};

export default GridView;
