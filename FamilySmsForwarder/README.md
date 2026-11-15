# Family SMS Forwarder (Native Android — Kotlin & Jetpack Compose)

A native Android application built with Kotlin, Jetpack Compose (Material 3), Room, DataStore, and WorkManager that automatically forwards incoming SMS messages from selected senders to a designated family member's mobile number.

## Architecture Overview
- **UI**: Jetpack Compose + Material 3 (`DashboardScreen`, `SendersScreen`, `SettingsScreen`) with `enableEdgeToEdge()` for Android 14, 15, and 16.
- **SMS Detection**: `SmsReceiver` (`BroadcastReceiver`) listening to `android.provider.Telephony.SMS_RECEIVED`, reassembling multipart incoming PDUs by originating address, and handing off to `WorkManager` via `goAsync()`.
- **SMS Forwarding**: `ForwardSmsWorker` (`CoroutineWorker` with expedited execution) using `SmsManager.divideMessage()` and `sendMultipartTextMessage()` / `sendTextMessage()`, with explicit `FLAG_MUTABLE` `PendingIntent`s targeting `SmsStatusReceiver` for per-part `SENT` and `DELIVERED` carrier tracking.
- **Persistence**: Room SQLite database (`sender_rules`, `forward_logs`) and Jetpack Preferences DataStore (`family_sms_settings`), excluded from cloud backups (`allowBackup="false"`, `data_extraction_rules.xml`).
- **Safety**: SHA-256 duplicate protection across configurable time windows, infinite forwarding loop guards, and runtime `SEND_SMS` checks.

## Required Android Permissions & Restrictions
1. `RECEIVE_SMS` (Runtime Dangerous Permission): Required to receive `SMS_RECEIVED` broadcasts when new messages arrive.
2. `SEND_SMS` (Runtime Dangerous Permission): Required for `SmsManager` to transmit forwarded messages via your active SIM card.
3. `POST_NOTIFICATIONS` (Runtime on Android 13 / API 33+): Optional/recommended to display error alerts and temporary worker notifications.
4. `RECEIVE_BOOT_COMPLETED` & `WAKE_LOCK`: Automatically granted normal permissions used by `BootCompletedReceiver` (with `UserManager.isUserUnlocked` Direct Boot check) and `WorkManager` to resume interrupted queued work.
5. `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SHORT_SERVICE` (Android 14 / API 34+): Required by `WorkManager` `SystemForegroundService` when expedited work falls back to a short foreground service on older/throttled devices.
6. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Lets you exempt this personal app from Doze mode so SMS forwarding is not delayed while the phone screen is off.

### Android 13, 14, 15 & 16 Compatibility Notes
- **Sideloaded APK Restricted Settings**: When installing a signed APK directly (outside Google Play), Android 13–16 may initially restrict `RECEIVE_SMS` / `SEND_SMS` permissions. Go to **Android Settings → Apps → Family SMS Forwarder → 3-dot menu (top-right) → Allow restricted settings**, then grant SMS permissions.
- **No Background Restriction Bypass**: `SMS_RECEIVED` is an implicit broadcast explicitly exempted by Android 8–16 for manifest receivers, provided the user does **not** manually tap **Force Stop** in App Info.
- **Android 15/16 Edge-to-Edge & Explicit Mutable PendingIntents**: Uses `enableEdgeToEdge()` and explicit `Intent(context, SmsStatusReceiver::class.java)` with `PendingIntent.FLAG_MUTABLE` on API 31+ so `SmsManager` can attach `resultCode` and `errorCode` without triggering Android 14+ `SecurityException`.
- **Android 15+ Sensitive OTP Protection**: Android 15 introduces stricter protections on OTP messages for untrusted notification listeners and non-default SMS apps on certain builds; test with your carrier/device build if forwarding bank OTPs.

## Building & Installing a Signed APK

### 1. Open in Android Studio
Open the `FamilySmsForwarder/` directory in **Android Studio (Ladybug / Meerkat or newer)** with **JDK 17** and **Android SDK Platform 36** (Android 16) installed. Allow Gradle sync to download dependencies and generate the Gradle wrapper if running from Android Studio, or run `gradle wrapper` locally.

### 2. Generate a Release Keystore
```bash
keytool -genkeypair -v \
  -keystore family-sms-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias family_sms_key
```

### 3. Configure `keystore.properties`
Copy `keystore.properties.example` to `keystore.properties` in `FamilySmsForwarder/` and fill in your keystore path and passwords:
```properties
storeFile=/absolute/path/to/family-sms-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=family_sms_key
keyPassword=YOUR_KEY_PASSWORD
```

### 4. Build the Signed Release APK
```bash
./gradlew assembleRelease
```
The signed APK will be output to:
`app/build/outputs/apk/release/app-release.apk`

### 5. Install on Your Phone via ADB (or Copy APK to Phone)
Enable **USB Debugging** on your Android phone, connect via USB, and run:
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```
*(Or transfer `app-release.apk` to your phone and open it to install; remember to enable "Allow restricted settings" in App Info if SMS permissions are blocked on Android 13–16.)*
