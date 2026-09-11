# Resonance Music Control

Adaptive lock-screen media controls for Android.

Resonance Music Control presents the media already playing in Spotify, YouTube
Music, Apple Music, podcast apps, and other Android players in a secure,
customizable lock-screen surface. It is a controller, not an audio player.

## Features

- Discovers active Android media sessions after the user grants notification access.
- Shows track title, artist, album artwork, source app, playback position, and volume.
- Provides play/pause, previous, next, seek, shuffle, like, playlist, and volume controls.
- Includes four distinct player themes: Material 3 Expressive, Frosted Glass,
  Hi-Fi Studio, and Duotone Canvas.
- Uses album-art color extraction to adapt the player surface and background per track.
- Keeps Android's real lock screen and authentication as the security boundary.
- Uses no internet permission, analytics, account system, wake lock, or battery exemption.

## Download

Download the latest APK from [Releases](../../releases).

Because GitHub APK downloads are sideloaded, Android may ask the user to allow
installs from their browser or file manager. On Android 13 and later, enabling
the Accessibility-based lock-screen controller can also require **Allow
restricted settings** in the app's system settings.

## Setup

1. Install and open **Resonance Music Control**.
2. Grant **Media notification access**.
3. Read the in-app disclosure and enable **Resonance Music Control lock-screen controller** in Accessibility settings.
4. Enable the lock-screen player and start media in a supported Android app.

## Development

### Tech stack

- Kotlin and Jetpack Compose
- Material 3
- Android MediaSession, Notification Listener, and Accessibility APIs

### Build

The project targets Android 15 / API 35, compiles against API 36, and supports
Android 6.0 / API 23 and newer.

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions runs tests, lint, and a debug APK build for main-branch pushes
and pull requests. The GitHub Release APK is currently debug-signed for
testing; configure a private release keystore or Android Play App Signing before
broad production distribution.

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md), keep changes
focused, run the build checks, and preserve the real Android keyguard as the
security boundary.

## License

Resonance Music Control is licensed under the [MIT License](LICENSE).

## Support and feedback

For bugs, ideas, or feature requests, open an [issue](../../issues).

## Contributors

See [CONTRIBUTORS.md](CONTRIBUTORS.md) for project credits.
