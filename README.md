# Resonance Lock

Resonance Lock is a Material 3 Expressive lock-screen controller for media already playing in
Spotify, YouTube Music, Apple Music, podcast apps, and other Android players. It is intentionally
not an audio player.

## Features

- Discovers active media sessions after the user grants Android notification access.
- Shows title, artist, album, source app, artwork, playback position, and remaining time.
- Sends play/pause, previous, next, and seek commands to the source app.
- Maps the physical volume up, down, and mute keys to the device music stream and mirrors each
  change in the player with a fast eased slider animation.
- Supports horizontal swipe gestures on the player for previous/next.
- Keeps the custom surface available while the selected player owns an Android media session,
  including while paused. Closing the player, removing its media notification, destroying its
  session, or stopping playback starts a 15-second grace timer; the surface then exits smoothly
  unless that same session becomes active again.
- Extracts two lightweight color tones from each album cover and smoothly infuses them into the
  fully opaque lock-screen background, player surface, border, shadow, controls, and ambient glow.
  The phone wallpaper cannot bleed into or tint the cover-derived composition. Tracks without
  artwork immediately use the neutral placeholder and default palette instead of retaining the
  previous song's cover.
- Includes five selectable visual themes with independent layouts—not color-only presets:
  Liquid Glass, Material 3 Expressive, Studio Neon, Scenic Ambient, and Album
  Spotlight. They vary background composition, clock typography, source capsule, metadata
  alignment, progress density, control grouping, button geometry, and volume placement. Every
  theme remains fully opaque against the phone wallpaper while its buttons and controls respond
  to a restrained blend of the current cover colors. Material 3 Expressive uses the current Material 3 Expressive color, type, shape,
  containment, and motion system.
- Uses an expanded rounded Material 3 Expressive shape scale across setup cards, dialogs, artwork,
  player surfaces, transport buttons, queue controls, and volume containers.
- Uses short, coordinated artwork scale and metadata slide transitions so a track change reads as
  one state change instead of briefly pairing a new title with the previous cover.
- Renders a cover-colored clock/date composition while Android's real keyguard remains underneath
  as the security boundary; system status and navigation chrome remain system-owned.
- Remains above the keyguard without bypassing the user's PIN, pattern, fingerprint, or face
  authentication.
- Blocks obscured touch input and does not launch another app directly from the locked surface.
- Uses a narrowly scoped `TYPE_ACCESSIBILITY_OVERLAY` while the real keyguard is locked. It does
  not request Android's **Display over other apps** permission, retrieve window content, inspect
  text, or perform accessibility gestures.
- Updates playback progress locally inside the slider, and updates volume through a separate
  lightweight state path, so neither interaction repeatedly rebuilds media metadata or artwork.
- Detaches the complete Compose visual tree while the screen is off or the phone is unlocked. It
  uses no wake lock, polling loop, foreground-service notification, location, or battery exemption.

## Install and set up on a phone

Download `resonance-lock.apk` from the repository's **Releases** page. Android
may require allowing installation from the browser or file manager used to open
the APK. GitHub Actions also publishes a debug APK artifact for contributors
and testing; it is not the primary end-user download.

1. Install the APK and open **Resonance Lock**.
2. Tap **Grant access** under **Media notification access**, then enable **Resonance media access**.
3. Read the in-app disclosure under **Lock-screen display access**, continue to Android's
   Accessibility settings, and enable **Resonance lock-screen player**.
4. Turn on **Enable lock-screen player**.
5. Start a song in Spotify, YouTube Music, Apple Music, or another media app.
6. Tap **Preview lock-screen player** once to verify the selected app and controls.
7. Lock the phone and wake the screen. The artwork/player surface appears within the
   visual lock-screen composition while the real Android keyguard continues to protect the device.

The two system-bound services remain subject to OEM accessibility and notification-listener
behavior. No battery-optimization exemption is required or requested.

## Build

This project targets Android 15 / API 35, compiles against API 36, and supports
Android 6.0 / API 23 and newer.

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

The Gradle debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The included GitHub Actions workflow runs the unit tests, lint, and debug APK
build on every pull request and push to `main`. The checked-in
`resonance-lock.apk` is the original installable APK supplied with this
project. Its SHA-256 is published with the GitHub release. The project
intentionally does not include a signing key; future release signing should be
configured privately using GitHub Actions secrets or Android Play App Signing.

## Privacy and permissions

Resonance Lock has no network client, analytics, account system, audio files,
or `INTERNET` permission. It requires notification access to discover media
sessions and accessibility access only to place its own overlay above the
real keyguard and observe volume keys. It does not retrieve window content,
inspect text, perform accessibility gestures, or bypass authentication.

Shared playlist entries are stored locally. Imported links are never opened
from the locked surface; unlock the device before opening an external app or
URI.

## Android security boundary

The app can display controls above the keyguard, but it cannot replace or silently dismiss a
secure Android lock screen. Swiping the custom surface away reveals Android's normal
authentication UI. Media notification access and accessibility access are both special permissions
that the user must grant explicitly in system settings.
