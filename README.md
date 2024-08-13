# React Native In App Purchases

`react-native-in-app-purchases` is a module built on top of `expo-in-app-purchases` that allows you to accept payments for in-app products. Development of `expo-in-app-purchases` was paused in June 2022, and the package was deprecated in August 2023. Those who appreciated the stability and ease of use of `expo-in-app-purchases` may have been disappointed by the need to switch to other packages like `react-native-iap` or `react-native-purchases`.

The goal of this package is to provide a similar simplicity and API to `expo-in-app-purchases` while keeping dependencies and features up to date. Currently, the package only supports Expo projects configured with `react-native-unimodules`, but the long-term goal is to extend support to all React Native projects while maintaining compatibility with Expo.

# Installation

You must ensure that you have [installed and configured the `react-native-unimodules` package](https://github.com/expo/expo/tree/main/packages/react-native-unimodules) before continuing.

```sh
// NPM
npm install @drift-mobile/react-native-in-app-purchases
// YARN
yarn add @drift-mobile/react-native-in-app-purchases
```

# Table of Contents

- [Configure for iOS](./docs/configure-for-ios.md)
- [Configure for Android](./docs/configure-for-android.md)
- [API](./docs/api.md)

### Configure for iOS

Run `npx pod-install` after installing the npm package.

### Configure for Android

No additional set up necessary.

# Contributing

Contributions are very welcome! Please refer to guidelines described in the [contributing guide](https://github.com/expo/expo#contributing).
