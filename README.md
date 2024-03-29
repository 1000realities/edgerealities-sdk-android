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
1. In your web browser, navigate to the Edge Realities server instance URL provided along with your license.
2. Log to the admin panel website with the credentials provided along with your license.
3. You should be redirected to the home page of the Edge Realities' platform admin panel.
![home page](https://raw.githubusercontent.com/1000realities/edgerealities-sdk-android/master/doc/images/admin-panel-home.png)
4. On the home page, start the 6DoF tracking server module by pressing the "Restart CloudSLAM" button.
5. After a short while the "CloudSLAM status" panel will change to green and the value will be "Running". 

### 2. Connect the client app
1. Run the example app on your device. If the application is ran for the first time it is important that you **accept all permission requests**.
2. Click the cog icon in the top right corner (or double swipe forward on some smartglass models). You should see a popup menu with two options:
   - Scan QR code
   - Connection settings.
3. Select "Scan QR code", and point the device to the configuration QR code show on the home page of the Edge Realities admin panel (in your web browser).
4. The application should show a blue popup “Configuration complete” once it was successfully configured for usage with Edge Realities.
5. Click the “Play” icon in the bottom right corner (or double swipe backwards on some smartglass models) to connect/disconnect to the Edge Realities server intsance.
6. The red cross icon in the top left corner should now change to a yellow excalmation mark, indicating that the connection has been succesfully established.

### 3. Initialize and build an environment map
1. Once connected, the "video" section (or "Map" section) of the Edge Realities administration panel should show a live preview from the camera on the device.
2. In not environment data is present, Red lines will be displayed over the preview indicating that the system is trying to initialize environment tracking.
![env init](https://raw.githubusercontent.com/1000realities/edgerealities-sdk-android/master/doc/images/env-initialize.png)
3. To initialize the environment data, move your device around the environment while looking at the video preview. Try to avoid rapid movement, and make sure that the device is changing it's position (i.e. not just rotating).
4. After few seconds the yellow exclamation mark icon in the client app should change to a green tick mark, indicating that  the envionrment data has been initialized tracking is in progress. On the video preview you will see that the red lines have been replaced by green dots.
![env tracking](https://raw.githubusercontent.com/1000realities/edgerealities-sdk-android/master/doc/images/env-tracking.png)
5. Note: The initialization process can take anywhere from a few seconds to 15 minutes (depending on the environment, more clutter = faster initialization), so please be patient.
6. Move your device around the environment to map it. Try to capture different viewing angles to obtain well defined environment data. You can see the data being built up live in the "Map" section.
7. Once happy with the results, use the "Restart CloudSLAM" button on the home page to save your environment.
8. In case you don't want to map the environment further and wish to focus only on tracking your device, building the environment map can be disabled in the "Map" section.

## SDK core concepts / TL;DR
1. The CloudSLAM class represents the core of the SDK. Creating an object of this class should be your starting point.
2. The CloudSLAM.start(...) method is used to connect the the EdgeRealities server instance and commence environment tracking/mapping.
3. The CSConfig object required by the start method may be filled in manually, however the example app provides a means of automatic configuration through scanning a QR code.
4. Use the CloudSLAM.Callback nested class to fetch relevant events from the SDK.
5. If you want the video stream to be previewed on your device, provide a Serface to the CloudSLAM.start(...) method.
6. Use CloudSLAM.stop() to disconnect from the Edge Realities server instance.
7. The CloudSLAM class will create and start it's own background thread to manage all communication with the server instance. However you can force it to run on your thread of choice by providing a Looper object to the CloudSLAM.start(...) method.
8. The Edge Realities android SDK requires the Java-Websocket library (org.java-websocket) v 1.3.8 or newer.

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
