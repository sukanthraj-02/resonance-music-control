# Security policy

## Reporting a vulnerability

Please do not report security vulnerabilities in public issues. Use GitHub's
private security advisory flow for this repository, or contact the repository
maintainer privately through the GitHub profile associated with the project.

Include the affected version, Android version/device, reproduction steps, and
the impact. Do not include personal data, credentials, or private media.

## Security boundaries

Resonance Lock uses Android notification access and accessibility overlay
capabilities to display media controls above the real keyguard. It does not
read window content, inspect passwords, perform accessibility gestures, or
bypass device authentication. These permissions are powerful and should only
be enabled when the user understands and accepts their purpose.
