# OWNGAME HELPI

Android compatibility and model-debugging work inspired by [PylaAI](https://github.com/PylaAI/PylaAI).

## Build and download

1. Open the [Actions tab](https://github.com/matteovandermescht-cell/OWNGAME-HELPI/actions).
2. Open the latest successful **Build OWNGAME HELPI APK** run.
3. Download the `OWNGAME-HELPI-debug-apk` artifact and extract `app-debug.apk`.
4. Install the APK on your Android device.

## Android components

- Android-native ONNX Runtime, replacing the Windows-only DirectML/GPU runtime path.
- Load a local ONNX model and inspect its input/output names and tensor metadata.
- Pick a screenshot from device storage, resize it to a rank-4 image tensor, normalize RGB values to 0..1, and run local ONNX inference.
- Inspect a short summary of output values to help debug model compatibility.
- Test PylaAI's `/check_version` (GET) and `/get_brawler_list` (POST) endpoints over HTTPS.
- GitHub Actions builds a debug APK on pushes to the main branch.

## Important model notes

Screenshot inference currently supports rank-4 float image inputs with one or three channels. It uses basic RGB 0..1 normalization and a default 640×640 size when the model dimensions are dynamic. Actual PylaAI models may require different color order, normalization, tensor layout, dimensions, or output decoding; compare these settings with the model's preprocessing code before judging output quality.

## Not yet ported

This is not a complete PylaAI Android port. Windows-specific screen capture, desktop input control, and game-play automation are not included. For debugging, use screenshots captured manually from a game or test environment.

The API test requires a reachable HTTPS server hostname. On Android, `localhost` means the phone itself, not your PC. The open-source PylaAI README says cloud features are disabled by default, so API checks may fail if no server is running.

## Attribution and license

Original project: https://github.com/PylaAI/PylaAI

PylaAI is licensed under CC BY-NC 4.0. Preserve attribution and do not sell or monetize derivative work.
