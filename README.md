# Edge Realities android SDK + example

This repository contains the android client SDK example for the core Edge Realities platform by [1000 realities](http://1000realities.io) (formerly known as CloudSLAM). Edge Realities is a framework for creating 6DoF tracking and augmented reality experiences using a remote server. 
**Please note that while this repository contains the client SDK, an Edge Realities server instance is necessary to operate it. For more information on how to obtain a license for an Edge Realities server instance please contact <info@1000realities.io>.**

## Demo video
A highlight of the Edge realities platform's core AR and tracking capabilities can be found here: <https://youtu.be/7qFAk6t288A>

For more videos please visit the [1000 realities youtube channel](https://www.youtube.com/channel/UCHrD8Ytr5FwLUt706l8dzIQ)

## Getting started
1. Obtain an Edge Realities server license. For more information please contact <info@1000realities.io>
2. Download & install [Android Studio](https://developer.android.com/studio/).
3. Clone the repository.
4. Open the project (i.e. the repository's root folder) in Android Studio.
5. Build the example app and run it on your device of choice. (See below for a list of currently supported devices).

## Using the example app
### 1. Start the Edge Realities server instance
### 2. Connect the client app
### 3. Initialize and build an environment map


## SDK Documentation

The full Edge Realities Android SDK Javadoc can be found at <http://1000realities.io/docs/cloudslam/sdk/android/index.html>.

## Device support
### Currently supported devices
- Smart phones
   - Samsung Galaxy S8
   - Samsung Galaxy S7
   - Samsung Galaxy S5
   - Google Pixel 1
   - Motorola G6
   - MyPhone Fun 18x9
- Samrt glasses
   - Vuzix M300
   - Vuzix M300XL
   - Vuzix Blade
   - RealWear HMT-1
   - RealMax quian
   - Epson Moverio BT 300

### Support for other devices
Any android device may be supported by Edge Realities provided it meets the following minimal requirements:
- Android OS 5.1 or higher.
- At least 1 RGB camera capable of delivering 640x480 video at 30 fps.
- Supported H.264 video encoder/decoder.
- Some form of connectivity, e.g. a Wi-Fi module, 5G modem etc.
- Optional: IMU (accelerometer + gyroscope).

Each device model must be calibrated for camera intrinsics (and other parameters) prior to onboarding to the Edge Realities platform. Currently calibration is carried out by the 1000 realities team. 
We are planning to enable end-users to calibrate their own devices in the future.

Would you like Edge Realities to support your device? Contact us: <info@1000realities.io>