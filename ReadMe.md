# 🔐 SecureKey 

SecureKey is an interactive Android application that challenges users to unlock a series of virtual locks using various sensor and device features. The app demonstrates the integration of multiple Android hardware components and APIs in a gamified experience.

## 📝 Description

SecureKey presents users with six virtual locks that can be opened by triggering different device conditions and user interactions:

1. **🔋 Battery Lock**: Opens automatically when battery level is 75% or below
2. **🎤 Voice Command Lock**: Opens when user speaks the Hebrew phrase "בבקשה תפתח"
3. **📱 Accelerometer Lock**: Opens after detecting three shake motions
4. **📶 WiFi Lock**: Opens when the device is connected to WiFi
5. **⏰ Time Lock**: Opens when the current time is between 8:00 AM and 6:00 PM
6. **😊 Smile Lock**: Opens when the user smiles at the camera for a sufficient duration

When all six locks are successfully opened, the app displays a congratulatory message. 🎉

## ✨ Features

- Integration with multiple device sensors and system APIs
- Real-time monitoring of device state and environment
- Voice recognition with support for Hebrew language
- Shake detection using accelerometer data
- Battery level monitoring
- WiFi connectivity detection
- Time-based conditions
- Facial expression detection
- Responsive UI with visual feedback

## 🛠️ Technical Implementation

The app is structured using a modular, component-based architecture:

- **MainActivity**: The container activity that hosts fragments and handles permissions
- **LockFragment**: Manages the lock interactions and UI state
- **LockAdapter**: RecyclerView adapter for displaying the lock states
- **SensorManager**: Manages accelerometer and system state (battery, WiFi, time)
- **SpeechRecognitionManager**: Handles voice command recognition
- **FaceDetectionManager**: Processes camera input to detect smiles

### Permissions Required

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CAMERA" />
```

### 🧩 Key Components

#### 📳 Sensor Integration
- Uses `SensorManager` to detect shake gestures
- Implements shake detection algorithm based on accelerometer readings
- Provides utility methods for system status monitoring

#### 🎤 Voice Recognition
- Utilizes Android's `SpeechRecognizer` API
- Configured for Hebrew language with recognition of specific phrases
- Continuous listening with error handling and reconnection logic

#### 📷 Face Detection
- Uses ML Kit's `FaceDetector` to process camera input
- Detects smile probability and tracks consecutive smile detections
- Works with both front and back cameras

#### 📊 System Status Monitoring
- Battery level detection using `BatteryManager`
- WiFi connectivity status via `ConnectivityManager`
- Time-of-day checking with `Calendar` API

#### 🖼️ UI Components
- RecyclerView with custom adapter for displaying locks
- Animated lock state transitions
- Toast notifications for feedback and guidance

## ⚙️ Setup Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Ensure you have the latest Android SDK installed
4. Build and run the application on a device or emulator

## 📋 Requirements

- Android 5.0 (API level 21) or higher
- Device with microphone for voice recognition
- Camera for smile detection
- Accelerometer for shake detection (optional)
- Internet connectivity for WiFi detection

## 📲 Usage

1. Launch the app
2. Grant the requested permissions (microphone and camera)
3. Try to open all locks by:
   - Ensuring battery is below 75% or connecting to a charger
   - Saying "בבקשה תפתח" clearly into the microphone
   - Shaking the device three times
   - Connecting to a WiFi network
   - Using the app between 8:00 AM and 6:00 PM
   - Smiling at the camera until the lock opens

## ⚙️ Customization

The sensitivity and behavior of the locks can be modified by adjusting the following parameters:

- Accelerometer sensitivity: Modify the `gForce > 2.5` threshold in `SensorManager`
- Voice recognition phrase: Change the string in `phrase.contains("בבקשה תפתח")` in `SpeechRecognitionManager`
- Battery threshold: Adjust the value in `batteryPct <= 75` in `SensorManager`
- Time window: Modify the range in `hourOfDay in startHour until endHour` in `SensorManager`
- Smile sensitivity: Change the values in `smileProb > 0.8` and `REQUIRED_CONSECUTIVE_SMILE_DETECTIONS` in `FaceDetectionManager`

## 📚 Project Architecture

The project follows a component-based architecture for better maintainability:

```
com.example.securekey/
├── MainActivity.kt                   # Main activity and entry point
├── fragments/
│   └── LockFragment.kt               # Main UI fragment managing locks
├── adapters/
│   └── LockAdapter.kt                # RecyclerView adapter for locks
└── utilities/
    ├── SensorManager.kt              # Manages sensors and device state
    ├── SpeechRecognitionManager.kt   # Handles voice recognition
    └── FaceDetectionManager.kt       # Manages camera and face detection
```

This modular structure improves code organization, readability, and makes future enhancements easier.

## 📝 Notes

- Voice recognition works best in quiet environments
- Smile detection works better with adequate lighting
- The app continuously monitors for all conditions, so locks will open automatically when their conditions are met
- The app includes fallback mechanisms for devices missing certain hardware capabilities
