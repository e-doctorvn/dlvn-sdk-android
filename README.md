# dlvn-sdk-android

EDR - DLVN Android SDK 

## Version 1.0.4

## Requirements

This dependency requires:

* **Compile SDK API level 33 (or later)** in the main application (```compileSdk 33 //or later```)
* Target at least **Android 5.0 - API level 21 (or later)** (```minSdk 21```)

## Installation

- In ```settings.gradle``` add the following line: 

  ```sh
    repositories {
        maven { url "https://jitpack.io" }
    }
  ```

- In ```build.gradle (Module :app)``` add the following line: 

  ```sh
    dependencies {
        implementation 'com.github.e-doctorvn:dlvn-sdk-android:1.0.4'
    }
  ```
    
## Usage
```kotlin
import com.example.dlvn_sdk.EdoctorDlvnSdk

// Initialize SDK instance with context before using any functions
val dlvnSdk = EdoctorDlvnSdk(context: Context, env: Env)
```
In this constructor:
- `env` is an enum of `Env { LIVE, SANDBOX }`. It already has the ***default value*** of `Env.SANDBOX`, so adding it to the constructor is **optional**.

## Environment notes
- On `SANDBOX`, default webview's URL is `khuat.dai-ichi-life.com.vn/tu-van-suc-khoe` and eDoctor's API on `dev` 
- On `LIVE`, default webview's URL is `kh.dai-ichi-life.com.vn/tu-van-suc-khoe` and eDoctor's API on `production`

## API References

#### Open WebView

Open given URL using WebView dialog. If URL is null, use the default ```/tu-van-suc-khoe```

```kotlin
  dlvnSdk.openWebView(fm: FragmentManager, url: String?): Unit
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `fm` | `FragmentManager` | **Required** |
| `url` | `String?` | Can be null |

#### DLVNSendData

Function for DLVN to pass data to the SDK **before opening** webview.
This function returns:
* `true` if successfully receive the data
* `false` if `params` is empty or missing `dcid` field

```kotlin
  dlvnSdk.DLVNSendData(params: JSONObject): Boolean
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `params` | `JSONObject` | **Required** |

#### onAuthenDataResult

Function for DLVN to listen to the SDK for receiving credentials include `token` & `dcid`
This function returns with object of `AuthenData (token: String, dcid: String)` class.

```kotlin
  dlvnSdk.onAuthenDataResult: ((data: AuthenData) -> Unit)
```

| Return | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `Unit` with `data` | `AuthenData` |  |

* ##### Example

    * **Java**:
    ```java
    dlvnSdk.setOnAuthenDataResult(authenData -> {
        String token = authenData.getToken();
        String dcid = authenData.getDcid();
        return null;
    });
    ```
    
    * **Kotlin**:
    ```kotlin
    dlvnSdk.onAuthenDataResult = { it ->
        val token = it.token
        val dcid = it.dcid
    }
    ```

#### Sample function

Takes the name and return a hello string

```kotlin
  dlvnSdk.sampleFunc(name: String): String
```

| Parameter | Type     | Description                       |
| :-------- | :------- | :-------------------------------- |
| `name`      | `String` | **Required**|