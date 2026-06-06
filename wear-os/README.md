# PathFinder for Galaxy Watch

Galaxy Watch runner companion based on `docs/wear-os-development-plan.md`.

## Target devices

- Galaxy Watch 4 and newer
- Wear OS 3 or newer
- Android API 30 or newer

The app uses Android Health Services, which is the supported exercise API on
Galaxy Watch Wear OS devices. It does not require Samsung Health SDK.

## Included MVP

- Free, time, distance, and interval run setup
- Health Services exercise session for distance, heart rate, cadence, and GPS state
- Pause, resume, finish, abandon, and summary flows
- Data Layer commands under `/runway/watch/command`
- Offline-first local session UI when the phone is disconnected

## Build

```bash
./gradlew :app:assembleDebug
```

Open this directory as a separate Android Studio project and select a Galaxy
Watch device. On first launch, allow location, physical activity, and sensor
permissions. GPS, heart rate, cadence, and distance are collected directly on
the watch, so a run can continue without the phone.

For wireless installation, enable Developer options and Wireless debugging on
the Galaxy Watch, pair it with Android Studio, and run the `app` configuration.

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The watch and phone apps intentionally use the same `applicationId`
(`com.runway.android`). Data Layer communication also requires both APKs to be
signed with the same certificate.

The phone app receives `/runway/watch/command` through
`WatchCommandListenerService`, stores the run through its existing backend API,
and mirrors session status over `/runway/watch/state`.
