# OWNGAME HELPI

Android compatibility work for PylaAI.

## Build APK

1. Open the **Actions** tab.
2. Run **Build OWNGAME HELPI APK** or push a commit to `main`.
3. Open the workflow run and download `OWNGAME-HELPI-debug-apk`.
4. Extract `app-debug.apk` and install it on Android.

## Ported components so far

- Android-native ONNX Runtime dependency instead of Windows DirectML.
- Select a PylaAI `.onnx` file and test whether Android ONNX Runtime can load it; the app displays input/output metadata.
- API diagnostics for the PylaAI `/check_version` (GET) and `/get_brawler_list` (POST) endpoints.
- HTTPS networking permission for API requests.

## Limitations

The full bot is not yet ported. Windows-specific capture/control code and the original inference pipeline still require Android replacements. The public PylaAI source defaults its API host to `localhost`; on Android, that means the phone itself, so enter a reachable API hostname to test a server.

Original source: https://github.com/PylaAI/PylaAI

PylaAI is licensed CC BY-NC 4.0. Keep attribution and do not sell or monetize derivative work.
