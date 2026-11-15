# Android application

Native Android client for ISP Network Tester, implemented in Kotlin with Jetpack Compose.

The app connects to a Go test server hosted inside an ISP network. It provides HTTP download throughput measurements and ICMP ping statistics.

## Features

- Configurable test server hostname or IP address and port.
- Server connection check using `GET /healthz`.
- Download speed test using `GET /download?size=<bytes>`, with selectable sizes from 10 MB to 1 GB.
- Streaming download measurement with progress, elapsed time, and average throughput.
- ICMP ping test with 10 probes, latency, packet loss, and jitter statistics.
- Option to ping either the configured server or a separate custom target.
- Persistent settings.
- Cancellation of running download and ping tests.
- Input validation for hostnames, IPv4, IPv6, and ports.

**Measurement scope:** download throughput measures the path to the configured test server, not general Internet transit performance. ICMP replies may be filtered or deprioritized by some networks, so ping packet loss does not always indicate a general connectivity problem.

## Requirements

- Android Studio with support for Android Gradle Plugin 8.13.
- JDK 21 (Gradle/Kotlin toolchain; application bytecode targets Java 17).
- Android SDK Platform 36.
- Android 8.0 (API 26) or newer.

The project uses the checked-in Gradle wrapper. Run Gradle commands from this `android/` directory.

## Open and run

1. Open the `android/` directory in Android Studio.
2. Allow Gradle sync to complete.
3. Select the `app` run configuration.
4. Choose an Android emulator or physical device and run the app.
5. Open Settings and enter the hostname or IP address and port of your deployed test server.

The default server port is `8080`. The app does not require a server address to be hardcoded into the source.

### Build from the command line

Build a debug APK:

```sh
./gradlew assembleDebug
```

The APK is generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Run the local unit tests:

```sh
./gradlew testDebugUnitTest
```

### Build a release APK

Release signing credentials are read from this file outside the repository:

```
~/.config/isp-network-tester/signing.properties
```

Create the directory and restrict access:

```sh
mkdir -p ~/.config/isp-network-tester
chmod 700 ~/.config/isp-network-tester
```

Create `signing.properties` with the following properties. Use the absolute path to your keystore:

```properties
storeFile=/home/your-user/.android/keystores/isp-network-tester-release.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=isp-network-tester
keyPassword=YOUR_KEY_PASSWORD
```

Restrict the file permissions:

```sh
chmod 600 ~/.config/isp-network-tester/signing.properties
```

Do not commit the keystore or this properties file, and keep secure backups of the keystore and passwords. If the signing properties file is absent, Gradle creates an unsigned release APK. If the file exists but is incomplete or points to a missing keystore, the build fails rather than silently producing an unsigned APK.

Build the release APK:

```sh
./gradlew assembleRelease
```

The output is named using the app version, for example:

```
app/build/outputs/apk/release/isp-network-tester-v1.0.0.apk
```

The APK is signed when the local signing properties are configured. You can verify its signature with Android's `apksigner` tool from the Android SDK Build Tools:

```sh
$ANDROID_HOME/build-tools/<build-tools-version>/apksigner verify --verbose app/build/outputs/apk/release/isp-network-tester-<version>.apk
```

Never share the release keystore or its passwords. The same signing key must be retained for future updates to install over existing installations.

## Network configuration

The app currently communicates with the test server over HTTP. Cleartext traffic is permitted by the app's network security configuration to support the intended ISP-controlled deployment environment.

Use this only on a network where the server and traffic path are appropriately controlled. TLS is not currently implemented.

When using the Android emulator to reach a server running on the development host, use `10.0.2.2` as the host address (with the server listening on port `8080`, unless configured otherwise).

## Tests

The local unit test suite covers app-owned logic, including:

- Hostname, IPv4, IPv6, and port validation.
- Supported download sizes.
- Download success, HTTP errors, unexpected or incomplete responses, invalid inputs, and cancellation.
- Ping statistics, including packet loss and jitter calculations.

The suite does not attempt to test Android DataStore internals or replace manual testing on an emulator or physical device. End-to-end checks against a running ISP test server should be performed separately.

## Related documentation

- [Project overview](../README.md)
- [Go test server: setup, configuration, and deployment](../server/README.md)
