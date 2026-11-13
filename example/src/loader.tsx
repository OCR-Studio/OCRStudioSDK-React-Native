import { ActivityIndicator, StyleSheet, View } from 'react-native';

const Loader = ({ isLoading = false }) => (
  <View style={[styles.container, styles.horizontal]}>
    <ActivityIndicator animating={isLoading} size="large" />
  </View>
);

const styles = StyleSheet.create({
  container: {
    flex: 0,
    justifyContent: 'center',
    alignSelf: 'center',
   // position: 'absolute',
  },
  horizontal: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingTop: 2,
  },
});

export default Loader;
