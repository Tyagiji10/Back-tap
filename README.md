# Back Tap 📱✨

A modern, flagship-tier Android system utility that utilizes your device's hardware accelerometer to recognize double and triple taps on the *back* of your phone, allowing you to trigger custom system actions instantly. 

Designed with a sleek, pure OLED-black interface and built for maximum performance and reliability.

## 🌟 Features

* **Hardware-Level Gesture Recognition:** Analyzes Z-axis accelerometer data to accurately detect physical back taps while filtering out false positives from normal screen interaction.
* **Premium OLED UI:** A beautifully crafted, fully monochromatic Material 3 design optimized for OLED displays (#000000 backgrounds, high-contrast cards).
* **Lightweight Background Engine:** Runs securely and efficiently as an Android Accessibility Service.
* **Haptic Feedback:** Provides crisp, physical tactile confirmation using `VibrationEffect.EFFECT_TICK` upon successful gesture recognition.
* **Intelligent Pausing:** Automatically pauses the gesture engine when battery drops below 10% to conserve power.
* **Custom Actions:** Map gestures to launch the camera, take screenshots, pull down notifications, control media, adjust volume, and more.
* **Hardware Fallbacks:** Built-in sensor diagnostics will automatically alert users if their device lacks the required hardware to run the engine.

## 🛠️ Technologies Used

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Dependency Injection:** Dagger Hilt
* **Asynchrony & State:** Kotlin Coroutines & StateFlow
* **Background Processing:** Android AccessibilityService & BroadcastReceivers

## 🚀 Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/Tyagiji10/Back-tap.git
   ```
2. Open the project in **Android Studio**.
3. Let Gradle sync and resolve dependencies.
4. Build and run the app on a physical device. *(Note: Emulators generally do not support physical accelerometer gesture simulation).*

## ⚠️ Requirements
* **Android SDK:** Minimum API 26 (Android 8.0), Target API 35.
* **Hardware:** A physical Android device with a built-in `TYPE_ACCELEROMETER` sensor.

## 🎨 Design Philosophy
*Designed & built with ♡ and a lot of coffee.* 
The UI intentionally bypasses standard Material You dynamic colors in favor of a strict, high-contrast greyscale aesthetic, ensuring the app looks and feels like a premium OEM-level system setting.

---
&copy; 2026 Shaurya Tyagi. All rights reserved.
