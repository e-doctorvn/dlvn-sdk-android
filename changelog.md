# dlvn-sdk-android

EDR - DLVN Android SDK 

Latest version: **1.0.9 (Updated: 06/11/2023)**

## Version 1.0.9
- Fix totally clear WebView's cache in `clearWebViewCache`

## Version 1.0.8
- Fix app crashing when call function `clearWebViewCache`:
    * `Pass`: Fixed app crashing
    * `Fail`: Cache is not totally cleared

## Version 1.0.7
- Change name of `logOutWebView` function to `clearWebViewCache`
- WebView is now able to perform: User (not logged in) types his question, then hits "Submit" -> back to DC app to request login but save current form data in WebView.

## Version 1.0.6
- Addding `logOutWebView` function
- `token` is now a required field in JSONObject of `DLVNSendData` function

## Version 1.0.5
- Changing flow of login case 2: Using DC app's login module
- Adding `setOnSdkRequestLogin` function