# Privacy statement

_Last updated: 15 November 2026_

This statement describes the privacy behavior of the ISP Network Tester Android app and the Go test server included in this project. The app is a client for a test server that is configured by the person using the app. The server may be operated by an ISP or another organization; it is not necessarily operated by the project maintainer.

## Information stored on your device

The app stores the settings you choose using Android Preferences DataStore:

- Test server hostname or IP address and port.
- Optional custom ping target.
- Whether to use the test server as the ping target.
- Selected download-test size.

These settings are stored in the app's private data on your device. The app does not require an account. You can remove the settings by clearing the app's storage or uninstalling it. Android device backup settings may affect whether app data is backed up or restored.

## Information sent when you run a test

The app does not send analytics, advertising identifiers, crash reports, or usage telemetry to the project maintainer. It does not upload your files, contacts, or other personal content.

When you run a connection check, the app sends an HTTP GET request to the configured server's `/healthz` endpoint. A download test sends an HTTP GET request to `/download` with the requested test-data size and receives generated test data. A ping test sends ICMP echo requests to the configured ping target. The ping target is either the configured test server or the separate address you entered.

The configured server and ping target, as well as network equipment between your device and those destinations, can observe your device's network address and the test traffic. Other network services may apply their own logging and retention practices.

## Server logs

The Go server included in this project writes structured request logs to standard output. Its request log records:

- Client IP address.
- HTTP method, request path, and host.
- User-agent.
- HTTP response status.
- Number of response bytes written.
- Request duration.

The server does not normally include the query string in its request log, but invalid download-size values and error details may be written to logs. Proxies, ingress controllers, hosting platforms, or other infrastructure in front of the server may record additional information, potentially including full request URLs.

The server does not define a log-retention period or send these logs to the project maintainer. The organization operating the server controls where logs are stored, who can access them, and how long they are retained. Contact that organization for information about its logging practices.

## Network security

The app currently uses plain HTTP for its server requests; TLS is not implemented. HTTP traffic can be observed or modified by parties with access to the network path. Use the app with a server and network you trust, preferably within a controlled ISP network. ICMP traffic is also visible to the ping target and network devices along the path.

## Your choices and questions

You choose the server address and ping target in the app. Do not enter sensitive information as a hostname or ping target. For questions about a particular deployment's data handling, contact the organization that operates the configured server or the person who provided the app.

This statement describes the behavior of the project code. A server operator may configure additional infrastructure, monitoring, proxies, or logging that is outside this project's control.
