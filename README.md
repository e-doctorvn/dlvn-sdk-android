# DLVN SDK Android

[![](https://jitpack.io/v/e-doctorvn/dlvn-sdk-android.svg)](https://jitpack.io/#e-doctorvn/dlvn-sdk-android)

> SDK Android của eDoctor dành cho Dai-ichi Life Vietnam - Tích hợp dịch vụ tư vấn sức khỏe.

## Mục lục

- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cài đặt](#cài-đặt)
- [Bắt đầu nhanh](#bắt-đầu-nhanh)
- [Cấu hình môi trường](#cấu-hình-môi-trường)
- [API Reference](#api-reference)
- [Push Notifications](#push-notifications)
- [Widget](#widget)
- [Changelog](#changelog)

---

## Yêu cầu hệ thống

| Yêu cầu | Phiên bản tối thiểu |
|---------|---------------------|
| **Compile SDK** | API 35 (Android 15) |
| **Min SDK** | API 23 (Android 6.0) |
| **Target SDK** | API 35 |
| **Java** | 17 |
| **Kotlin** | 2.0+ |

---

## Cài đặt

### Bước 1: Thêm repositories

Trong file `settings.gradle` hoặc `settings.gradle.kts` của project:

```groovy
dependencyResolutionManagement {
    repositories {
        // ... các repositories khác
        maven { url "https://jitpack.io" }
        maven { url "https://repo.sendbird.com/public/maven" }
    }
}
```

### Bước 2: Thêm dependencies

Trong file `build.gradle` của module app:

```groovy
dependencies {
    implementation 'com.github.e-doctorvn:dlvn-sdk-android:1.3.3'
    implementation 'com.google.firebase:firebase-messaging:25.0.1'
}
```

---

## Bắt đầu nhanh

### Khởi tạo SDK

```kotlin
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.Env

class MainActivity : AppCompatActivity() {
    private lateinit var dlvnSdk: EdoctorDlvnSdk

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Khởi tạo SDK
        dlvnSdk = EdoctorDlvnSdk(
            context = this,
            intent = intent,      // có thể null - dùng để xử lý notification
            env = Env.SANDBOX     // hoặc Env.LIVE cho production
        )
    }
}
```

### Tham số khởi tạo

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---------|------|----------|-------|
| `context` | `Context` | ✅ | Context của Application hoặc Activity |
| `intent` | `Intent?` | ❌ | Intent từ MainActivity để xử lý notification |
| `env` | `Env` | ❌ | Môi trường (mặc định: `Env.SANDBOX`) |

---

## Cấu hình môi trường

| Môi trường | WebView URL | API |
|------------|-------------|-----|
| `SANDBOX` | `khuat.dai-ichi-life.com.vn/tu-van-suc-khoe` | Development |
| `LIVE` | `kh.dai-ichi-life.com.vn/tu-van-suc-khoe` | Production |

---

## API Reference

### Các phương thức WebView

#### openWebView

Mở WebView dialog với URL được chỉ định. Sử dụng URL mặc định `/tu-van-suc-khoe` nếu URL là null.

```kotlin
dlvnSdk.openWebView(fm: FragmentManager, url: String?)
```

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---------|------|----------|-------|
| `fm` | `FragmentManager` | ✅ | Fragment manager để hiển thị dialog |
| `url` | `String?` | ❌ | URL tùy chỉnh |

#### openWebViewWithEncodedData

Mở URL từ dynamic link với các tham số data đã được encode.

```kotlin
dlvnSdk.openWebViewWithEncodedData(fm: FragmentManager, url: String)
```

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---------|------|----------|-------|
| `fm` | `FragmentManager` | ✅ | Fragment manager để hiển thị dialog |
| `url` | `String` | ✅ | URL đầy đủ với `data` và các params khác |

---

### Các phương thức truyền dữ liệu

#### DLVNSendData

Truyền dữ liệu cho SDK **trước khi** mở WebView.

```kotlin
dlvnSdk.DLVNSendData(params: JSONObject): Boolean
```

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---------|------|----------|-------|
| `params` | `JSONObject` | ✅ | Phải chứa các trường `dcid` và `token` |

**Giá trị trả về:**
- `true` - Nhận dữ liệu thành công
- `false` - Params rỗng hoặc thiếu trường bắt buộc

---

### Các phương thức xác thực

#### authenticateEDR

Khởi tạo các listener của SDK cho sự kiện chat/call sau khi user đăng nhập.

```kotlin
dlvnSdk.authenticateEDR(params: JSONObject)
```

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---------|------|----------|-------|
| `params` | `JSONObject` | ✅ | Dữ liệu xác thực user |

#### deauthenticateEDR

Xóa dữ liệu SDK khi user đăng xuất.

```kotlin
dlvnSdk.deauthenticateEDR()
```

---

### Callback Listeners

#### onSdkRequestLogin

Lắng nghe yêu cầu đăng nhập từ SDK.

**Kotlin:**
```kotlin
dlvnSdk.onSdkRequestLogin = { callbackUrl ->
    // Xử lý yêu cầu đăng nhập
    val url = callbackUrl
}
```

**Java:**
```java
dlvnSdk.setOnSdkRequestLogin(callbackUrl -> {
    String url = callbackUrl;
    return null;
});
```

---

## Push Notifications

### Thiết lập Firebase Messaging Service

Implement các phương thức sau trong `FirebaseMessagingService`:

#### isEdrMessage

Kiểm tra xem remote message có phải từ EDR không.

```kotlin
EdoctorDlvnSdk.isEdrMessage(message: RemoteMessage): Boolean
```

#### handleEdrRemoteMessage

Xử lý remote message từ EDR và hiển thị notification.

```kotlin
EdoctorDlvnSdk.handleEdrRemoteMessage(
    context: Context,
    message: RemoteMessage,
    icon: Int?  // null sẽ sử dụng icon mặc định của SDK
)
```

#### handleNewToken

Cập nhật FCM token cho SDK.

```kotlin
EdoctorDlvnSdk.handleNewToken(context: Context, token: String)
```

### Ví dụ đầy đủ

```kotlin
class MyFirebaseService : FirebaseMessagingService() {
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        if (EdoctorDlvnSdk.isEdrMessage(message)) {
            EdoctorDlvnSdk.handleEdrRemoteMessage(
                context = this,
                message = message,
                icon = R.drawable.ic_notification
            )
        } else {
            // Xử lý các notification khác
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        EdoctorDlvnSdk.handleNewToken(this, token)
    }
}
```

---

### App Lifecycle

#### setAppState

Thông báo cho SDK biết trạng thái foreground/background của app. Gọi trong lifecycle observer.

```kotlin
EdoctorDlvnSdk.setAppState(isForeground: Boolean)
```

**Ví dụ với ProcessLifecycleOwner:**

```kotlin
class MyApplication : Application(), DefaultLifecycleObserver {
    
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    override fun onStart(owner: LifecycleOwner) {
        EdoctorDlvnSdk.setAppState(true)
    }
    
    override fun onStop(owner: LifecycleOwner) {
        EdoctorDlvnSdk.setAppState(false)
    }
}
```

---

## Widget

### Appointment Widget

Hiển thị widget lịch hẹn từ EDR trong layout:

```xml
<com.edoctor.dlvn_sdk.widget.AppointmentWidgetList
    android:id="@+id/widgetList"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Widget tự động xử lý:
- Lấy dữ liệu lịch hẹn
- Hiển thị danh sách lịch hẹn
- Các thao tác hủy/xác nhận

---

## Changelog

Xem [CHANGELOG.md](CHANGELOG.md) để biết lịch sử phiên bản và ghi chú phát hành.

---

## License

Copyright (c) 2023-2025 eDoctor Vietnam. All rights reserved.
