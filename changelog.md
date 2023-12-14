# dlvn-sdk-android

EDR - DLVN Android SDK

Latest version: **1.0.21 (Updated: 14/12/2023)**

## Version 1.0.21

- Update color of status bar
- Rename package to `com.edoctor`

## Version 1.0.20

- Fix onReceivedSslError

## Version 1.0.19

- Fix Jitpack error

## Version 1.0.18

- Fix rename `accessToken` of EDR

## Version 1.0.17

- Fix Vietnamese message of error toast when opening SDK

## Version 1.0.16

- Update using `sessionStorage` for token saving from SDK to web.
- Fix app crashing when showing 'Request permission pop-up' in picking attachment.
- Fix uploading taken photo from camera in picking attachment.

## Version 1.0.15

- Try to fix app crashing when open SDK after reading DLVN's post. **(Worked)**

## Version 1.0.14

- Try to fix app crashing when open SDK after reading DLVN's post. **(Failed)**

## Version 1.0.13

- Fix request camera permission when picking photos for question's attachment.

## Version 1.0.12

- Fix auto redirecting to login page when open Q&A notification
- Fix `go back` function in many pages

## Version 1.0.11

- Handle `Back` and `Share` buttons from DLVN's article

## Version 1.0.10

- Fix totally clear WebView's cache in `clearWebViewCache` again

## Version 1.0.9

- Fix not clearing WebView's cache in `clearWebViewCache` -> Failed

## Version 1.0.8

- Fix app crashing when call function `clearWebViewCache`:
  - `Pass`: Fixed app crashing
  - `Fail`: Cache is not totally cleared

## Version 1.0.7

- Change name of `logOutWebView` function to `clearWebViewCache`
- WebView is now able to perform: User (not logged in) types his question, then hits "Submit" -> back to DC app to request login but save current form data in WebView.

## Version 1.0.6

- Addding `logOutWebView` function
- `token` is now a required field in JSONObject of `DLVNSendData` function

## Version 1.0.5

- Changing flow of login case 2: Using DC app's login module
- Adding `setOnSdkRequestLogin` function
