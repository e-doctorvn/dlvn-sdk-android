# dlvn-sdk-android

EDR - DLVN Android SDK 

## Version 1.0.0

## Requirements
This dependency requires **Android API level 33 (or later)** in the main application

```compileSdk 33 //or later```

## Installation

- In ```settings.gradle``` add the following line: 

  ```
    repositories {
        maven { url "https://jitpack.io" }
    }
  ```

- In ```build.gradle (Module :app)``` add the following line: 

  ```
    dependencies {
        implementation 'com.github.e-doctorvn:dlvn-sdk-android:1.0.0'
    }
  ```
    
## Usage
```
import com.example.dlvn_sdk.EdoctorDlvnSdk

// Initialize SDK instance with context before using any functions
val dlvnSdk = EdoctorDlvnSdk(context: Context)
```

## API References

#### Open WebView

Open given URL using WebView dialog. If URL is null, use the default ```/tu-van-suc-khoe```

```
  dlvnSdk.openWebView(fm: FragmentManager, url: String?): Unit
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `fm` | `FragmentManager` | **Required** |
| `url` | `String?` | Can be null |

#### Sample function

Takes the name and return a hello string

```
  dlvnSdk.sampleFunc(name: String): String
```

| Parameter | Type     | Description                       |
| :-------- | :------- | :-------------------------------- |
| `name`      | `String` | **Required**|
