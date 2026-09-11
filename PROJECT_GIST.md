# Resonance Lock — complete project handoff

## 1. Product identity and purpose

- App name: **Resonance Lock**.
- Android application ID / namespace: `com.sukanth.resonance`.
- Version: `1.0.0` (`versionCode = 1`).
- Root project name: `Resonance`.
- Workspace: `/home/sukanth/Documents/ChatGPT/fyp`.
- Resonance is a custom lock-screen media controller for media that is already playing in another Android app. It is deliberately **not an audio player** and has no playback engine, library database, account system, network client, or audio files of its own.
- It observes Android `MediaSession`s exposed by apps such as Spotify, YouTube Music, Apple Music, podcasts, and Poweramp, renders a full-screen Compose controller over the real Android keyguard, and forwards transport/seek/queue commands to the selected external session.
- Android's real keyguard remains the security boundary. Resonance cannot bypass a PIN, pattern, fingerprint, face authentication, or SystemUI.

## 2. Current user-visible state

The current selectable themes are exactly:

1. **Liquid Glass** — `LIQUID_GLASS`, reintroduced from the supplied HTML visual reference.
2. **Material 3 Expressive** — internal enum name remains `ANDROID_17_GLASS` for compatibility.
3. **Studio Neon** — `NEON_PULSE`.
4. **Scenic Ambient** — `SCENIC_AMBIENT`.
5. **Album Spotlight** — `ALBUM_SPOTLIGHT`.

Retired/hidden implementations still present in source:

- `EXPRESSIVE_BLOOM` is also filtered out of `selectableEntries` and is not selectable.
- Expressive Bloom remains compiled in the enum, visual style, player, and setup preview branches. This is compatibility/technical debt, not a user-facing feature.
- The old enum name `ANDROID_17_GLASS` has not been renamed because doing so would break stored preference values and require a broader migration. Only its display label is now Material 3 Expressive.

Latest requested visual behavior:

- The app uses a deep, rounded Material 3 Expressive design language across setup and player UI.
- Cover-derived colors affect backgrounds and controls moderately, not at full saturation.
- No button or theme uses bouncy/overshooting spring damping. All declared spring tokens and player springs use `Spring.DampingRatioNoBouncy`.
- Button press feedback is restrained scaling only: typically `0.96`, Material 3 Expressive lock controls `0.97`, setup transport `0.94`, and preview `0.97`. Press-driven rotation was removed.
- A swipe-following card rotation remains in one interactive card path (`rotationZ = cardTranslation / 220f`); this is direct drag feedback, not a spring bounce.
- Missing artwork must immediately show the neutral placeholder/default palette, never the previous song's cover.

The current Liquid Glass theme translates Apple's consolidated Now Playing glass console widget into native Compose. It features a unified 32dp rounded frosted glass card enclosing the thumbnail artwork, track title/artist, source app indicator, 3.5dp glass progress scrubber, centered circular play/pause transport controls, and integrated volume slider into one continuous frosted glass surface with chromatic edge refraction and top specular reflections.

## 3. Latest Scenic Ambient implementation

The Scenic Ambient theme is based on Apple Music's full-screen animated cover cinema and floating glass dock:

- **Atmospheric Scenic Backdrop**: A full-bleed, softly blurred (`46.dp`) copy of the album artwork forms the background with subtle continuous linear drift and scale (`1.24x`), overlaid with a multi-stage cinematic gradient scrim ensuring OLED black contrast at the base and vivid cover aura in the middle/top.
- **Standalone Hero Artwork**: Elevated interactive artwork panel (`292.dp`, `28.dp` rounded corners) with luminous specular border, deep ambient shadow (`24.dp`), and smooth horizontal swipe gestures for track navigation.
- **Open Minimalist Metadata**: High-contrast bold title and subtitle floating seamlessly directly over the atmospheric scene.
- **Edge-to-Edge Glass Scrubber**: Uses the `GLASS` progress treatment with sleek 3dp rail, smooth live position tracking, and high-legibility duration formatting.
- **Floating Liquid Glass Transport Dock**: Dedicated rounded 36.dp pill dock with 1dp chromatic refraction border, top white specular highlight line, prominent circular play/pause button (70dp), and transparent side buttons.
- **Floating Liquid Glass Volume Capsule**: Dedicated standalone frosted volume pill with speaker icons and bottom Apple-style gesture affordance bar.
- **Incoming Call Immunity**: Automatically detects incoming and ongoing calls (cellular and VoIP via `TelephonyManager`, `AudioManager.MODE_IN_COMMUNICATION`, and CallStyle notifications) to instantly dismiss the overlay (0ms) so the incoming call screen is immediately visible and unobstructed, and automatically restores the player when the call cuts/ends.

## 4. Material 3 Expressive player state

- The former “Android 17 Expressive” theme is now displayed as **Material 3 Expressive**.
- It has an independent Compose layout, not merely a recolor: dedicated clock/date composition, hero artwork, metadata surface, progress surface, grouped transport controls, volume container, and gesture affordance.
- Previous screenshot refinements removed decorative/instructional labels such as the Android 17 / Resonance header, swipe label, playback label, and drag-to-seek label.
- Date is placed beneath the time.
- The design avoids a separate source tab beside the cover.
- Playback controls stand on their own rather than inside an unnecessary labeled panel.
- Rounded geometry and moderate cover-adaptive Material semantic colors are used throughout.

## 5. Theme/style architecture

- `lockscreen/PlayerVisualTheme.kt` defines theme identity, display text, selectable filtering, safe fallback, and retired-theme migration.
- `ui/lockscreen/PlayerVisualStyle.kt` turns a theme plus two artwork colors into a complete immutable token set:
  - card/artwork/transport/play shapes;
  - card, border, and artwork brushes;
  - border width and shadow;
  - primary/secondary foreground;
  - badge, primary control, general control, and inactive slider colors.
- Each theme has an independent token branch. Color helpers scale, lift, and blend colors.
- `ui/lockscreen/LockScreenPlayerScreen.kt` is the main and largest UI file. It contains the root gestures/transitions, theme backdrop, theme routing, clocks, artwork rendering, queue sheet, all theme layouts, progress treatments, metadata, transport controls, source capsule, volume controls, and empty state.
- `ui/theme/ResonanceTokens.kt` is the app-wide Material 3 Expressive token source for colors, spacing, shape, type, and motion.
- `ui/theme/Theme.kt` creates a dark-only Material 3 `MaterialTheme`. `darkTheme` and `dynamicColor` parameters are retained for source compatibility but ignored. Dynamic color is not used.
- Typography uses Android `FontFamily.SansSerif`; no font files are bundled.
- Shape scale: 0, 4, 8, 12, 16, 20, 28, 32, 48, and full/999 dp, with compatibility aliases for compact/control/card/hero/expressive.

## 6. Artwork and adaptive color pipeline

- `MediaSessionMonitor` reads artwork in this order: `METADATA_KEY_ART`, `METADATA_KEY_ALBUM_ART`, then `METADATA_KEY_DISPLAY_ICON`.
- Artwork is capped to a maximum side of 640 px. Hardware bitmaps are copied to ARGB_8888 where needed.
- Cache identity uses both object identity and `Bitmap.generationId`, preventing in-place bitmap mutations from being mistaken for unchanged art.
- A sampled 8×8-style content hash produces `artworkSignature`. Equivalent republished bitmaps keep the same visual identity, so pause/play callbacks do not unnecessarily rerun the cover transition.
- `NO_ARTWORK_SIGNATURE` is `Long.MIN_VALUE`.
- If settled metadata has no artwork, `sanitizedArtwork(null)` calls `resetArtworkCache()` and returns null. It clears the cached bitmap, source, generation ID, signature, and both palette colors. This is the explicit fix for a coverless song showing the previous track's art.
- The UI renders a radial Material-colored music-note placeholder when artwork is null.
- Palette extraction samples at most roughly 24×24 points, ignores pixels below alpha 160, quantizes into 512 RGB buckets, weights chroma, chooses a dominant primary and a distant weighted secondary, then constrains saturation/value for dark-surface readability.
- Default palette values are stored in `DEFAULT_ARTWORK_PRIMARY` and `DEFAULT_ARTWORK_SECONDARY` and mirrored by `ExternalMediaState` defaults.
- The Compose root animates palette changes over roughly 260–290 ms and blends them into theme-specific bases. Controls should remain only moderately cover-colored.

## 7. Media state and session selection

`ExternalMediaState` contains:

- access/connection/eligibility flags;
- current package and readable source-app name;
- title, artist, album;
- bitmap, artwork signature, primary/secondary palette ARGB;
- playlist entries;
- position, duration, playback speed;
- playing, buffering, active-playback flags;
- play/pause, previous, next, and seek capabilities;
- current/max music volume;
- call-active flag and optional error.

`isLockScreenEligible` requires notification access, a connected session, an active qualifying media notification, and no active call.

Session selection rules:

- Only external sessions whose package currently has a recognized media notification are candidates.
- The current valid session is retained to avoid app-label/session flicker from out-of-order callbacks.
- Otherwise a playing session is preferred, then buffering, then the first candidate.
- Stopped/none/error current sessions yield to another playing candidate.
- Playback callbacks are coalesced to 32 ms and update only playback/position/capability fields.
- Metadata publication is delayed 110 ms to let player metadata/artwork settle.
- Notification removal hides eligibility after an 850 ms debounce so pause/play notification replacement does not flash the overlay.
- Position is estimated from `PlaybackState.position`, `lastPositionUpdateTime`, elapsed realtime, and clamped playback speed (±8x).
- Duration is clamped to seven days.
- Metadata text removes control characters, trims, caps to 180 characters (source label 64), and supplies fallbacks.
- The playlist is capped to 100 items in final state.

## 8. Notification listener

- `MediaAccessService` extends `NotificationListenerService` and is protected by `BIND_NOTIFICATION_LISTENER_SERVICE`.
- It initializes/connects the monitor, tracks active media notification keys, and refreshes sessions on media post/removal.
- Media detection accepts transport-category notifications or notifications containing `EXTRA_MEDIA_SESSION`.
- Call notifications and audio call/ringtone modes take priority; call activity suppresses the lock-screen player.
- It publishes a full active-notification snapshot after short settling delays (40 ms after post, 180 ms after removal).
- It performs no rendering, polling loop, wake lock, foreground notification, or activity launch.

## 9. Accessibility overlay and security model

- `ResonanceAccessibilityService` is used only to place a `TYPE_ACCESSIBILITY_OVERLAY` over Android's real locked keyguard.
- The XML service config requests filter-key events but sets no event types, `canRetrieveWindowContent=false`, and `canRequestFilterKeyEvents=true`; Android 12+ marks it not an accessibility tool.
- `onAccessibilityEvent` intentionally does nothing. The service does not inspect node trees, text, passwords, or other app content and does not perform accessibility gestures.
- The overlay uses `FLAG_LAYOUT_IN_SCREEN`, `FLAG_LAYOUT_NO_LIMITS`, `FLAG_NOT_FOCUSABLE`, `FLAG_NOT_TOUCH_MODAL`, `FLAG_HARDWARE_ACCELERATED`, and `FLAG_SECURE`.
- Obscured touches are filtered. The manifest requests `HIDE_OVERLAY_WINDOWS`; the activity also uses `setHideOverlayWindows(true)` on Android 12+.
- Overlay visibility requires: interactive screen, not dismissed for this lock cycle, keyguard locked, preference enabled, and eligible media state.
- Incoming/active calls immediately remove it.
- On screen-off the Compose view is suspended/invisible and may be kept warm; on screen-on it refreshes media state, prewarms if appropriate, and reevaluates after a 70 ms keyguard-settle delay.
- On user-present/unlock it exits and is removed. The Compose lifecycle/view-model/saved-state owner is explicitly created and destroyed with the overlay.
- Hardware volume key-down is observed only while the overlay and keyguard are active. The UI publishes an optimistic slider step but returns false, allowing Android/SystemUI to perform the real volume operation. A system volume broadcast/content observer reconciles the confirmed value.
- There is no overlay permission (`SYSTEM_ALERT_WINDOW` / “Display over other apps”).

## 10. Lock-screen activity and gestures

- `LockScreenActivity` is a preview/fallback full-screen activity shown when locked. It is not exported, is excluded from recents, uses its own task affinity, singleTask launch, show-when-locked behavior, edge-to-edge layout, obscured-touch filtering, and secure overlay hiding.
- Preview mode ignores normal eligibility so the setup screen can preview the selected theme.
- Non-preview mode dismisses when the state is no longer eligible or the keyguard unlocks.
- Volume up/down/mute keys are mapped to music-stream changes.
- Swipe up requests Android's real keyguard dismissal after the custom 300 ms exit animation. Swipe down dismisses the custom surface. Horizontal artwork/player gestures invoke previous/next where implemented.
- Unlock failure restores the custom state; successful unlock removes the task.
- The accessibility overlay routes both unlock and close gestures to “dismiss for current lock”; it does not itself bypass keyguard.

## 11. Playback, seek, queue, and volume controls

- Play/pause checks current session state and sends pause when playing/buffering, otherwise play.
- Previous/next call standard MediaSession transport controls.
- Seek clamps to nonnegative values and uses `seekTo` only when the session exposes the action and duration is valid.
- The UI locally advances playback position every 250 ms while playback is active, using playback speed. Slider interactions keep a local drag value and send seek only on drag completion.
- Time formatting is `m:ss` below one hour and `h:mm:ss` at/above one hour; negative input clamps to zero.
- Volume changes use `AudioManager.STREAM_MUSIC`. UI-only updates avoid rebuilding artwork/metadata.
- The global playlist floating button opens a bottom sheet. If the player exposes a MediaSession queue, rows call `skipToQueueItem`. If there is no queue, the sheet explains the limitation and offers “Open in <source app>” through the session activity.

## 12. Poweramp integration

- Manifest package visibility includes `com.maxmpz.audioplayer`.
- `PowerampPlaylistBridge` optionally requests Poweramp provider access with `com.maxmpz.audioplayer.ACTION_ASK_FOR_DATA_PERMISSION`.
- If Poweramp's MediaSession queue is empty, it queries `content://com.maxmpz.audioplayer.data/queue`, tolerates multiple possible column names, and constructs playlist entries defensively.
- Selecting a provider-loaded Poweramp queue item broadcasts `com.maxmpz.audioplayer.API_COMMAND`, command 20 (`OPEN_TO_PLAY`), using the provider queue URI.
- Setup shows a one-time-per-detected-session Poweramp permission dialog.

## 13. Setup screen and persistence

- `MainActivity` is the launcher and hosts a Compose setup dashboard.
- Setup shows current session, artwork, status, source-app launch button, transport controls, horizontal theme chooser, three access cards, enable toggle, and preview button.
- Access steps shown are notification listener, accessibility overlay, and unrestricted battery access. The first two are required before enabling; unrestricted battery access is presented as a readiness step but is not required by the enable gating logic.
- Android 13+ accessibility flow first opens App Info so the user can allow restricted settings, then accessibility settings if still necessary.
- Accessibility use has an explicit in-app disclosure.
- Preferences file: `lock_screen_preferences`.
- Keys: `lock_screen_enabled`, `player_visual_theme`, and `accessibility_disclosure_accepted`.
- Invalid, missing, hidden, and retired OLED/Reference Dark values fall back to Material 3 Expressive (`ANDROID_17_GLASS`). Liquid Glass is selectable and restores normally.

## 14. Build configuration and dependencies

- Single module: `:app`.
- Kotlin + Jetpack Compose, no XML screen layouts.
- Compile/target SDK 37 (Android 17); minimum SDK 23 (Android 6.0).
- Java source/target 17.
- Android Gradle Plugin 9.2.0; Kotlin Compose plugin 2.3.21.
- Compose BOM `2026.06.00`.
- Key libraries: Activity Compose 1.13.0, Core KTX 1.19.0, Lifecycle ViewModel/Runtime Compose 2.11.0, Compose animation/foundation/Material3/material-icons/ui/ui-graphics/tooling-preview.
- Unit tests use JUnit 4.13.2. Android test dependencies exist, but there are currently no instrumented/UI tests.
- English resources only (`localeFilters = ["en"]`).
- Both debug and release builds are minified and resource-shrunk. Debug is intentionally `isDebuggable=false`, so the “debug APK” is installable but not attach-debuggable.
- Cleartext network traffic and app backup are disabled.
- There is no networking dependency, analytics, database, dependency injection framework, or image-loading library.

Build/verify:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Gradle needs access to `/home/sukanth/.gradle`; in the Codex sandbox this command was run with escalation. Normal output:

`app/build/outputs/apk/debug/app-debug.apk`

## 15. Tests and last verification

There are 7 passing JVM unit tests:

- `ExternalMediaStateTest` (2): missing-artwork identity and eligibility requirements.
- `PlayerVisualThemeTest` (3): known theme restore (including Liquid), safe fallback, and retired OLED fallback.
- `TimeFormatterTest` (2): short/long duration formatting and negative clamping.

Last full command passed compilation, unit tests, lint, R8, resource shrinking, and APK packaging. No build error remained.

## 16. APK artifacts

- **Current/latest:** `/home/sukanth/Documents/ChatGPT/fyp/Resonance-liquid-glass-html-match-debug.apk`
  - SHA-256: `7b31d5f7647ae059ef725794c6ad54bebd2e0cc06430508cf86737775b463187`
  - Size: about 1.1 MB.
- Older: `Resonance-expressive-debug.apk`
  - SHA-256: `4d6e4b369384876c1edc175c231a6e0d80c7ee8b7fb909900acf8ab370bb7a4d`.
- Older/outdated Liquid build: `Resonance-liquid-glass-debug.apk`
  - SHA-256: `847368f57d2aea6f8b00a812e52f8390a5d0b1eb28673d6e56ccd1dc4e8c559e`.
- Do not distribute the two older artifacts as the current implementation.

## 17. Source map

- `MainActivity.kt`: setup host, permission routing, preferences, preview.
- `LockScreenActivity.kt`: show-when-locked activity, preview, hardware keys, unlock/dismiss flow.
- `lockscreen/ExternalPlayerViewModel.kt`: thin ViewModel facade over the singleton monitor and permission checks.
- `lockscreen/LockScreenPreferences.kt`: SharedPreferences and theme migration/filtering.
- `lockscreen/MediaAccessService.kt`: notification-listener bridge and call/media notification snapshot.
- `lockscreen/MediaSessionMonitor.kt`: central state machine, MediaSession controller, artwork/palette/cache, playlist, playback, eligibility, and volume.
- `lockscreen/PlayerVisualTheme.kt`: theme enum/display/filter/migration.
- `lockscreen/PowerampPlaylistBridge.kt`: optional Poweramp queue provider and command broadcasts.
- `lockscreen/ResonanceAccessibilityService.kt`: secure lock-screen overlay lifecycle and volume-key mirroring.
- `ui/icons/ResonanceIcons.kt`: custom/vector icon source.
- `ui/lockscreen/LockScreenPlayerScreen.kt`: all lock-screen Compose UI and theme-specific layouts.
- `ui/lockscreen/PlayerVisualStyle.kt`: artwork-adaptive per-theme visual tokens.
- `ui/setup/SetupScreen.kt`: setup dashboard, dialogs, theme previews, current-session card.
- `ui/theme/ResonanceTokens.kt`: Material color/shape/type/motion tokens.
- `ui/theme/Theme.kt`: dark Material theme assembly.
- `util/TimeFormatter.kt`: playback duration text.
- `design/demo_artwork.svg`: design/demo artwork asset.
- `app/src/main/res/xml[-v31]/accessibility_service_config.xml`: deliberately restricted accessibility capability.

## 18. Known inconsistencies and technical debt

- `README.md` may need a future wording pass if it drifts from the current five selectable themes and the user-facing Material 3 Expressive name.
- Expressive Bloom code remains in the enum, style file, main player file, and setup preview branches, but is unreachable through normal preference selection.
- Internal names such as `ANDROID_17_GLASS`, `AndroidExpressiveLayout`, and related functions remain even though the user-facing name is Material 3 Expressive.
- Setup describes unrestricted battery access as keeping the bridge responsive even though the README says no exemption is required; enabling the player does not require that permission. This messaging should be reconciled if documentation/UX cleanup is requested.
- The playlist empty-state can open the source app even from the lock overlay, while older README security wording says it does not launch another app directly from the locked surface. Code behavior is authoritative; review the security/product decision if changing this.
- The source directory is not currently recognized as a Git repository by `git status` in this environment. Preserve files carefully and do not assume commits/history are available.
- There is no emulator/device screenshot automation or instrumented Compose coverage in the project. Visual QA has been based on user screenshots plus compilation/lint/unit tests.

## 19. Figma context

- Existing Figma design URL: `https://www.figma.com/design/mVT2P8AJKBfhUaPwI4r8a6`.
- The connected account reported handle `Claude`, email `claudeproject.ac@gmail.com`, plan `Claude's team`, Starter tier, View seat.
- Figma MCP calls were blocked because the Starter-plan tool quota was exhausted; therefore the latest Material/Scenic code changes were **not written back to Figma**.
- Do not claim that Figma is synchronized with the current APK.

## 20. Non-negotiable product constraints for future work

- Keep Android's real keyguard intact; never fake or bypass authentication.
- Do not broaden accessibility privileges or retrieve window content.
- Do not request generic overlay permission.
- Do not retain previous artwork when the new item has no cover.
- Keep cover color adaptation moderate and readable.
- Keep all theme/button springs non-bouncy unless the user explicitly reverses that decision.
- Keep the current native Liquid Glass implementation faithful to its supplied reference and do not add fake controls for unsupported session actions.
- Use “Material 3 Expressive” in user-facing copy, even while compatibility symbols remain named Android 17.
- Maintain clear separation between the Scenic playlist button and progress/duration area.
- Preserve the brighter, gently moving Scenic background and full-width foreground cover treatment.
