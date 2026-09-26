# teleRoll

teleRoll is a native Android assistant for the existing Telegram Android app. It uses Android `AccessibilityService` to inspect Telegram's **visible** UI, wait for visible media to finish downloading, and dispatch upward swipe gestures with a moderate overlap.

It does not clone Telegram, use Telegram APIs, download media itself, send messages, react, delete messages, join or leave groups, change Telegram settings, or perform account actions.

## What is included

- Kotlin + Jetpack Compose Material 3 app UI
- Dark premium interface with animated ambient gradients, glass panels, motion, and live status indicators
- Figtree body typography and Outfit display typography through Android's downloadable-font provider, with Android fallback if the provider is unavailable
- `TelegramAccessibilityService`
- Robust accessibility heuristics for:
  - image, video, document, thumbnail, and media surfaces
  - download actions and content descriptions
  - progress indicators
  - percentage text such as `37%`
  - play buttons and loaded image surfaces
- Safety-first state machine:
  - `IDLE`
  - `SCROLLING`
  - `MEDIA_DETECTED`
  - `WAITING_FOR_DOWNLOAD`
  - `DOWNLOAD_COMPLETE`
  - `CONTINUING`
  - `PAUSED`
  - `FINISHED`
  - `STOPPED`
- Session-level media fingerprints to avoid processing the same visible media repeatedly
- Compact draggable overlay with Start, Pause, Resume, Stop, status, and counters
- Settings for scroll distance, gesture duration, completion delay, scroll limit, time limit, end detection, focus safety, screen awake behavior, and overlay visibility
- Debug screen for current package, state, visible media, percentage, download state, processed count, fingerprint, and relevant accessibility nodes
- Foreground notification for Android versions that require a visible ongoing service

## Build in Android Studio

1. Open the `teleRoll` directory in Android Studio Ladybug or newer.
2. Allow Gradle sync to download the Android Gradle Plugin, Kotlin, Compose, and AndroidX dependencies.
3. Use a device or emulator running Android 8.0/API 26 or newer.
4. Build with **Build → Make Project**.
5. Install with **Run**. If you have Gradle installed globally, the equivalent command from `teleRoll/` is:

```bash
gradle :app:assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The project is a standard Android Studio Gradle project and does not depend on the surrounding JavaScript workspace.

## Build the APK from a phone with GitHub

This repository includes a GitHub Actions workflow for building the debug APK without Android Studio.

1. Open the repository on GitHub.
2. Open **Actions** and choose **Build teleRoll APK**.
3. Select **Run workflow**, choose the `main` branch, and tap **Run workflow**.
4. Open the running workflow after it finishes successfully.
5. Scroll to **Artifacts** and download **teleRoll-debug-apk**.
6. Extract the downloaded ZIP and install `app-debug.apk` on your Android device.

The workflow also runs automatically whenever changes are pushed to `main`. Android may ask you to allow installation from the browser or file manager.

## Device setup

1. Install and open teleRoll.
2. Tap **Enable** for Accessibility service.
3. Choose **teleRoll Telegram assistant** in Android Accessibility settings and enable it.
4. Return to teleRoll.
5. Tap **Allow** for the floating overlay permission.
6. Open Telegram and enter the channel or group you want to inspect.
7. Keep the Telegram channel/group visible, then use teleRoll's **START** button or the floating overlay.

The assistant pauses when Telegram is no longer foreground. It also pauses rather than scrolling when a visible media item has an unknown download state. This is intentional: Android accessibility data is not a stable Telegram API, and false completion is more dangerous than waiting.

## Telegram compatibility

Telegram's accessibility tree changes between versions, devices, and media types. Detection is therefore heuristic and combines:

- visible class names
- text and content descriptions
- Android view IDs when exposed
- bounds and overlapping media surfaces
- progress text
- loading/download semantics
- play controls and loaded image surfaces

The detector does not assume one exact Telegram layout. If Telegram exposes insufficient information, teleRoll waits for a clearer accessibility signal or until the configured session limit is reached.

## Permissions

Only permissions required for the requested behavior are declared:

- Accessibility binding: required to inspect Telegram's visible accessibility nodes and dispatch scroll gestures
- Display over other apps: required for the compact floating control surface
- Foreground service: required for a visible long-running automation session
- Notifications: used for the foreground service notification on Android 13+

No network permission is declared, and there is no Telegram API client.

## Important safety boundaries

The service only:

- reads visible accessibility nodes while Telegram is foreground
- dispatches one upward scroll gesture at a time
- waits for media state to become clearly complete

It never calls click, long-click, global back, text entry, clipboard APIs, Telegram APIs, or account actions. The overlay controls are local to teleRoll.