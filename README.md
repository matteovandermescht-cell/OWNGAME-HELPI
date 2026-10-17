# OWNGAME HELPI

Android porting work for PylaAI.

## Build APK

1. Open the **Actions** tab.
2. Run **Build OWNGAME HELPI APK** or push a commit to `main`.
3. Open the workflow run and download `OWNGAME-HELPI-debug-apk`.
4. Extract `app-debug.apk` and install it on Android.

## Android compatibility work

- Uses Microsoft's Android-native ONNX Runtime instead of the original Windows DirectML runtime.
- Lets you select a PylaAI `.onnx` model and checks whether Android ONNX Runtime can load it, then displays its input/output metadata.
- The full PylaAI bot is not yet ported. Its Windows-specific code and device-control pipeline still need Android replacements.

Original source: https://github.com/PylaAI/PylaAI

PylaAI is licensed CC BY-NC 4.0. Keep attribution and do not sell or monetize derivative work.
