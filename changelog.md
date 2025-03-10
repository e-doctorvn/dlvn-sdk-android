# dlvn-sdk-android

EDR - DLVN Android SDK

Latest version: **1.2.27 (Updated: 18/02/2025)**

## Version 1.2.27 (18/02/2025)

- Fix loading issue when open SDK.

## Version 1.2.26 (17/09/2024)

- Replace `domain` with `defaultDomain`.

## Version 1.2.25 (10/09/2024)

- Close webview on homepage by checking if `currentUrl` contains `domain` with no extra slashes.

## Version 1.2.24 (27/08/2024)

- Comment block of code that init permission request launcher.

## Version 1.2.23 (26/08/2024)

- Remove incompatible service types.

## Version 1.2.22 (26/08/2024)

- Add `foregroundServiceType` and `FOREGROUND_SERVICE` permissions.
- Upgrade `targetSdk` to `34`
- Handle request call permissions in SDK's init function.

## Version 1.2.21 (27/07/2024)

- Null and empty check results of call permissions request launcher.
- Add flag `RECEIVER_NOT_EXPORTED` to `registerReceiver` function on call ringing event.

## Version 1.2.20 (17/07/2024)

- Request call `permissions`: Camera, Microphone & Notification when open WebView with `authenticated` state.

## Version 1.2.19 (27/06/2024)

- Update handling WebView `goBack`.

## Version 1.2.18 (31/05/2024)

- Set `sdkSupportConsultant` as session storage's item on `onPageFinished`.

## Version 1.2.17 (30/05/2024)

- Uncomment `authenticateSendBirdCall` function 😭.

## Version 1.2.16 (10/05/2024)

- Add `callId` into `eClinicEndCall` api.

## Version 1.2.15 (23/04/2024)

- Re-publish to make version number clear.
- Same code as version `1.2.13`

## Version 1.2.14 (19/04/2024) (BATCH FIX)

- Fix app crashing when handling `getAppointmentDetail`'s `null` data due to missing authen token.
- Back to different branch which does not have widget implementation.

## Version 1.2.13 (19/04/2024)

- Fix app crashing when handling `getAppointmentDetail`'s `null` data due to missing authen token.

## Version 1.2.12 (15/04/2024)

- Handling `display/hide` scroll indicator when widget list has data or empty.
- Handle cleaning up widget list & subscription when user logs out.

## Version 1.2.11 (09/04/2024)

- Expose `com.edoctor.dlvn_sdk.widget.AppointmentWidgetList` for DLVN to add into xml layout.
- Remove widget after succeeded in canceling.

## Version 1.2.10 (08/04/2024)

- Add `textColor="@color/black"` for warning message in CancelConfirmDialog.

## Version 1.2.9 (08/04/2024)

- Release appointment widget support to use in DC app:
  - Add `ApolloClient`, `AppointmentListAdapter`
  - Add dialogs for widget's actions (cancel, confirm)

## Version 1.2.8 (08/04/2024)

- Fix crashing app bug when meets error in parsing response from `checkAccountExist`: add null safe with `JSONObject.NULL`

## Version 1.2.7 (28/03/2024)

- (Failed) Fix crashing app bug when meets error in parsing response from `checkAccountExist`.

## Version 1.2.6 (26/03/2024)

- Detect whether user is using old SDK version which does not support video calling.

## Version 1.2.5 (25/03/2024)

- **Remove**: Request Notification permission when user opens app (already used EDR's services) to fix app crashing.

## Version 1.2.4 (22/03/2024)

- Only request Camera + Microphone permissions when booking VIDEO consultant.
- Request Notification permission when user opens app (already used EDR's services).

## Version 1.2.3 (21/03/2024)

- Remove `pictureInPicture` support for VideoCallActivity.
- Fix infinite loading when upload image to doctor while having video call.

## Version 1.2.2 (20/03/2024)

- Remove `silent = true` for call notification.
- Lock orientation `portrait` for Call activities.

## Version 1.2.1 (19/03/2024)

- Update agree call from notification in quit state.
- Remove `checkAccountExist`
- Add toast when on/off mic/cam in VideoCallActivity

## Version 1.2.0 (12/03/2024)

- Update call duration in VideoCallActivity.
- Update saving Sendbird's credentials.

## Version 1.1.14 (29/02/2024)

- Fix crashing bug when choose attachments on devices below Android 11.
- Call `getSendbirdAccount` when user succeeded in booking.

## Version 1.1.13 (27/02/2024)

- Fix crashing bugs.
- Add `openWebViewWithEncodedData` function.
- Update open webview flow to match `consent handling` with WEB.

## Version 1.1.12 (22/02/2024)

- Add `checkAccountExist` function.
- Fix `authenticateEDR` function to not call `initDLVNAccount` if account does exist.

## Version 1.1.11 (06/02/2024)

- Fix error in publishing version `1.1.10`.

## Version 1.1.10 (06/02/2024)

- Fix app crashing when calling `handleNewToken` function.

## Version 1.1.9 (05/02/2024)

- Fix clearing Sendbird tokens when log out.
- Add `setAppState` function.

## Version 1.1.8 (29/01/2024)

- Fix receiving chat noti unconsistently.

## Version 1.1.7 (29/01/2024)

- Fix not showing chat noti from same doctor when already in channel.

## Version 1.1.6 (29/01/2024)

- Fix not to remove iOS pushToken when disconnect Sendbird's ChatSDK.

## Version 1.1.5 (29/01/2024)

- Make `intent` parameter in SDK constructor **nullable**

## Version 1.1.4 (26/01/2024)

- Update flags for chat notification: `Intent.FLAG_ACTIVITY_CLEAR_TOP` + `PendingIntent.FLAG_UPDATE_CURRENT`

## Version 1.1.3 (26/01/2024)

- Fix main classname of D-Connect app in showing notification. (`com.dlvn.mcustomerportal.activity.DashboardActivity`)

## Version 1.1.2 (26/01/2024)

- Fix displaying chat notification in DC app.
- Add `EdrLifecycleObserver` interface.
- Add 03 functions: `isEdrMessage`, `handleEdrRemoteMessage` and `handleNewToken`

## Version 1.1.1 - Includes Chat + Call libraries (22/01/2024)

- Add Chat library and handling notifications.
- Handle checking SDK version on web to use Chat/Call with doctor features.
- Rename `clearWebViewCache` function to `deauthenticateEDR`
- Add function `authenticateEDR`

## Version 1.1.0 - Pre-test native calling (28/12/2023)

- Add SendbirdCalls SDK to implement native calling => DLVN UAT pre-test

## Version 1.0.26 - Without native calling (21/12/2023)

- Enhance loading when open SDK

## Version 1.0.25

- Fix build error

## Version 1.0.24

- Fix app crashing issued by Google Store, by: move `show` WebView outside `initDLVNAccount`'s response

## Version 1.0.23

- Fix old URL was cached in WebView
- Update support DLVN & external links in post detail

## Version 1.0.22

- Fix import of new package's name

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
