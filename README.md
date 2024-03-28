# dlvn-sdk-android

EDR - DLVN Android SDK

## Version 1.2.7

## Requirements

This dependency requires:

- **Compile SDK API level 33 (or later)** in the main application (`compileSdk 33 //or later`)
- Target at least **Android 5.0 - API level 21 (or later)** (`minSdk 21`)

## Installation

- In `settings.gradle` add the following line:

  ```sh
    repositories {
        maven { url "https://jitpack.io" }
        maven { url "https://repo.sendbird.com/public/maven" }
    }
  ```

- In `build.gradle (Module :app)` add the following line:

  ```sh
    dependencies {
        implementation 'com.github.e-doctorvn:dlvn-sdk-android:1.2.7'
        implementation 'com.google.firebase:firebase-messaging:23.4.0'
    }
  ```

## Usage

```kotlin
import com.example.dlvn_sdk.EdoctorDlvnSdk

// Initialize SDK instance with context before using any functions
val dlvnSdk = EdoctorDlvnSdk(context: Context, intent: Intent?, env: Env)
```

In this constructor:

- `Intent` should be the intent from MainActivity of application. SDK uses this to handle notifications which belong to chat/video consultant service. This parameter can be `null`.
- `env` is an enum of `Env { LIVE, SANDBOX }`. It already has the **_default value_** of `Env.SANDBOX`, so adding it to the constructor is **optional**.

## Environment notes

- On `SANDBOX`, default webview's URL is `khuat.dai-ichi-life.com.vn/tu-van-suc-khoe` and eDoctor's API on `dev`
- On `LIVE`, default webview's URL is `kh.dai-ichi-life.com.vn/tu-van-suc-khoe` and eDoctor's API on `production`

## API References

#### Open WebView

Open given URL using WebView dialog. If URL is null, use the default `/tu-van-suc-khoe`

```kotlin
  dlvnSdk.openWebView(fm: FragmentManager, url: String?): Unit
```

| Parameter | Type              | Description  |
| :-------- | :---------------- | :----------- |
| `fm`      | `FragmentManager` | **Required** |
| `url`     | `String?`         | Can be null  |

#### DLVNSendData

Function for DLVN to pass data to the SDK **before opening** webview.
This function returns:

- `true` if successfully receive the data
- `false` if `params` is empty OR missing `dcid` || `token` field

```kotlin
  dlvnSdk.DLVNSendData(params: JSONObject): Boolean
```

| Parameter | Type         | Description  |
| :-------- | :----------- | :----------- |
| `params`  | `JSONObject` | **Required** |

#### onSdkRequestLogin

Function for DLVN to listen to the SDK when it needs to request login from DC app.

```kotlin
  dlvnSdk.onSdkRequestLogin: ((callbackUrl: String) -> Unit)
```

| Return        | Type     | Description |
| :------------ | :------- | :---------- |
| `callbackUrl` | `String` |             |

- ##### Example

  - **Java**:

  ```java
  dlvnSdk.setOnSdkRequestLogin(callbackUrl -> {
      String url = callbackUrl;
      return null;
  });
  ```

  - **Kotlin**:

  ```kotlin
  dlvnSdk.onSdkRequestLogin = { it ->
      val url = it
  }
  ```

#### deauthenticateEDR

Clear SDK's data when DC app logs out.

```kotlin
  dlvnSdk.deauthenticateEDR(): Unit
```

#### authenticateEDR

Call this function after user logined successfully to initialize SDK's listener for chat/call events.

```kotlin
  dlvnSdk.authenticateEDR(params: JSONObject): Unit
```

| Parameter | Type         | Description  |
| :-------- | :----------- | :----------- |
| `params`  | `JSONObject` | **Required** |

#### isEdrMessage

Call this function to check if the remote message is from EDR.

```kotlin
  EdoctorDlvnSdk.Companion.isEdrMessage(message: RemoteMessage): Boolean
```

| Parameter | Type            | Description  |
| :-------- | :-------------- | :----------- |
| `message` | `RemoteMessage` | **Required** |

#### handleEdrRemoteMessage

Call this function to let SDK handle the remote message from EDR.
If parameter `icon` is null, SDK will use its default icon for notification.

```kotlin
  EdoctorDlvnSdk.Companion.handleEdrRemoteMessage(context: Context, message: RemoteMessage, icon: Int?): Unit
```

| Parameter | Type            | Description   |
| :-------- | :-------------- | :------------ |
| `context` | `Context`       | **Required**  |
| `message` | `RemoteMessage` | **Required**  |
| `icon`    | `Int?`          | Can be `null` |

- ##### Example

  - **Java**:

  ```java
  @Override
  public void onMessageReceived(@NonNull RemoteMessage message) {
      super.onMessageReceived(message);
      if (EdoctorDlvnSdk.Companion.isEdrMessage(message)) {
          EdoctorDlvnSdk.Companion.handleEdrRemoteMessage(this, message, R.drawable.dc_app_icon);
      } else {
          // DLVN handle
      }
  }
  ```

#### handleNewToken

Call this function inside the override function `onNewToken` of `FirebaseMessagingService`.

```kotlin
  EdoctorDlvnSdk.Companion.handleNewToken(context: Context, token: String): Unit
```

| Parameter | Type      | Description  |
| :-------- | :-------- | :----------- |
| `context` | `Context` | **Required** |
| `token`   | `String`  | **Required** |

#### setAppState

Call this function inside `@OnLifecycleEvent(Lifecycle.Event.ON_START)` and `@OnLifecycleEvent(Lifecycle.Event.ON_STOP)` to let SDK knows whether DC app is in foreground or not.

```kotlin
  EdoctorDlvnSdk.Companion.setAppState(isForeground: Boolean): Unit
```

| Parameter      | Type      | Description  |
| :------------- | :-------- | :----------- |
| `isForeground` | `Boolean` | **Required** |

#### openWebViewWithEncodedData

Open URL from dynamic link. The URL should be full format with `data` and other params.

```kotlin
  dlvnSdk.openWebViewWithEncodedData(fm: FragmentManager, url: String): Unit
```

| Parameter | Type              | Description  |
| :-------- | :---------------- | :----------- |
| `fm`      | `FragmentManager` | **Required** |
| `url`     | `String`          | **Required** |
