# MyBLEEx - Simple BLE Device Manager

A simplified Android application for managing and measuring data from Bluetooth Low Energy (BLE) medical devices.

## Features

- **Scan for BLE Devices**: Discover nearby BLE medical devices
- **Manual Device Addition**: Add devices by entering MAC address and device type
- **Real-time Measurements**: Connect to devices and receive measurements in real-time
- **Local Storage**: Save devices and measurement history locally using SharedPreferences
- **No Background Service**: All operations run in foreground only when app is active

## Supported Devices

- **BM1000C** - Pulse Oximeter
- **AD805** - Pulse Oximeter
- **U807** - Blood Pressure Monitor
- **LD575** - Blood Pressure Monitor
- **Contour** - Glucose Meter
- **CF516** - Weight Scale

## Architecture

### Key Components

1. **BLEManager** - Singleton class managing BLE operations (scanning, connecting, measurements)
2. **PreferencesUtils** - Handles local device storage using SharedPreferences with Gson
3. **DevicesActivity** - Main screen showing list of saved devices
4. **MeasureActivity** - Screen for real-time device measurements
5. **AddDeviceDialog** - Dialog for manually adding devices

### BLE Layer

- **Callbacks** - Device-specific GATT callbacks for each supported device type
- **DataParser** - Parses raw BLE data into meaningful measurements
- **Device Models** - Data classes for device information

## Permissions

The app requires the following permissions:
- `BLUETOOTH` / `BLUETOOTH_ADMIN` (Android 11 and below)
- `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` (Android 12+)
- `ACCESS_FINE_LOCATION` (Required for BLE scanning)

## Usage

1. Launch the app and grant required permissions
2. Click the scan button (🔍) to discover nearby devices, or add button (+) to manually add a device
3. Tap on a device to connect and start measurement
4. View real-time measurement data
5. Measurements are automatically saved to device history

## Development

### Build

```bash
./gradlew assembleDebug
```

### Requirements

- Android Studio Hedgehog or later
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9+

## License

This project is for demonstration purposes.

