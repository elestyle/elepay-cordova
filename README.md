# elepay-cordova

Cordova plugin for elepay SDK.

## Install
```
cordova plugin add cordova-plugin-elepay --save
```

### Android
Cordova plugin for elepay SDK requires "minSdkVersion" being at 21 or above.
You may need to adjust this value from your project's `build.gradle` file.

## Usage

```javascript
// All modules are exported to the `cordova.plugins.ElepayCordova`

// Setup elepay SDK.
//
// The parameter object could contain the following fields:
// "publicKey": String value. Required. Can be retrieved from your elepay account's dashboard page.
// "hostUrl": String value. Optional. Indicates the server url that you want to customised. Omitted to use elepay's server.
// "googlePayEnvironment": String value. "test" or "production". Used to setup Google Pay, can be omitted if Google Pay is not used.
cordova.plugins.ElepayCordova.initElepay({
  publicKey: "the public key string",
  apiUrl: "a customised url string, can be omitted",
  googlePayEnvironment: "either 'test' or 'product' if presented. Can be omitted if Google Pay is not used"
})

// Process payment after charging.
//
// "payload" is a JSON object that the charge API returned. For API details, please refer to https://developer.elepay.io/reference
//
// The result is passed through the callbacks.
// The first callback is called when the payment handling is succeeded. The "response" parameter is a JSON object in a structure of:
// {
//   "state": "succeeded",
//   "paymentId": "the payment id"
// }
//
// The second callback is called when the payment handling is either cancelled or failed.
// When user cancelled the payment, the "failure" parameter is:
// {
//   "state": "cancelled",
//   "paymentId": "the payment id"
// }
// When something is wrong while processing the payment, the parameter is in a structure of:
// {
//   "state": "cancelled",
//   "paymentId": "the payment id",
//   "error": {
//     "code": "error code"
//     "reasose": "the reason of the error"
//     "message": "the detail message"
//   }
// }
cordova.plugins.ElepayCordova.handlePayment(
  payload,
  function(response) {
      // Called when payment is handled successfully.
  },
  function(failure) {
      // Called when payment handling is failed or cancelled.
  })
```

## Callback

Some payment methods(like Line Pay, PayPay, etc.) require to process the payment outside your app. You need to setup the app with extras configurations.

### iOS

Your app needs to be configured with URL scheme and `LSApplicationQueriesSchemes`.
For detail configurations, please [refere to elepay iOS SDK document](https://developer.elepay.io/docs/ios-sdk)

As the cordova's [offical document](https://github.com/apache/cordova-ios/blob/master/guides/Cordova%20Custom%20URL%20Scheme%20Handling.md) suggested, in your app's JavaScript source file, add the following code to let Cordova framework invokes the callback.
```JavaScript
function handleOpenURL(url) {
    setTimeout(function () {
        cordova.plugins.ElepayCordova.handleOpenUrl(url);
    }, 0);
}
```

### Android

Url schemes configurations are required. Please refer to [elepay Android SDK document](https://developer.elepay.io/docs/android-sdk) for detail.

## Miscellaneous

1. If you see the building error indicating that "SWIFT_VERSION" is missing when building for iOS, you should change the configuration of the iOS project by specifying the SWIFT_VERSION item. The configuration item can be found in the "Build Settings" tab of your iOS project if you open the project from Xcode.

2. Because elepay SDK uses [AndroidX](https://developer.android.com/jetpack/androidx) internally, the callback `Activity`s uses themes defined from `Theme.AppCompat`. If your application has not defined themes inheriting from `Theme.AppCompat`, you need to add definations to the `styles.xml` file of your project in android platform folder and use it as the theme of the callback `Activity`s.
Take LinePay callback Activity as example:
```xml
    <!-- in styles.xml -->
    <!-- Elepay activity theme. -->
    <style name="ElepayTheme" parent="Theme.AppCompat.Light.NoActionBar">
    </style>
```
```xml
<!-- in AndroidManifest.xml -->
<activity android:exported="true" android:name="jp.elestyle.androidapp.elepay.activity.linepay.LinePayActivity" android:theme="@style/ElepayTheme">
...
</activity>
```