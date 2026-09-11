# Contributing

## Development setup

1. Install Android Studio with Android SDK 37 and a JDK supported by the
   Android Gradle Plugin.
2. Clone the repository and open it in Android Studio, or run the Gradle
   wrapper from the project root.
3. Run:

   ```bash
   ./gradlew testDebugUnitTest assembleDebug lintDebug
   ```

Changes involving notification access, accessibility overlays, keyguard
behavior, or external URI handling should include focused tests and an
explanation of the security implications.

## Pull requests

- Keep changes focused and document user-visible behavior.
- Do not commit keystores, credentials, generated build directories, or
  personal media.
- Verify the debug build and unit tests before opening a pull request.
- Preserve the real Android keyguard as the security boundary.
