# Wear OS MVP

This folder contains a working Android smartwatch MVP for the neuro-symbolic emergency detection concept.

## What is included

- Wear OS app module (`app`)
- Start/stop monitoring UI
- Foreground monitoring service running periodic risk evaluations
- Neuro-symbolic decision pipeline:
  - sample ingestion model
  - neural hypothesis stub
  - symbolic rule engine
  - confidence-weighted truth resolution
  - escalation level mapping
- HTTP alert client for backend escalation

## Build prerequisites

- Android Studio Iguana+ (or compatible)
- Android SDK 35
- JDK 17

## Build and run

From this folder:

```bash
./gradlew :app:assembleDebug
```

Install on emulator or watch:

```bash
./gradlew :app:installDebug
```

## MVP workflow

1. Open the app on a watch or emulator.
2. Tap Start to run continuous monitoring.
3. The service evaluates risk roughly every 15 seconds.
4. Alerts at level 2+ are posted to the backend `/v1/alerts` endpoint.

## Backend integration

The app reads backend settings from shared preferences keys:

- `backend_url`
- `user_id`
- `region`

The current MVP ships with defaults in `SettingsStore` and should be wired to an onboarding/settings screen for production use.

## Next implementation steps

1. Replace generated sample windows with Health Services + SensorManager streams.
2. Replace `SimpleNeuralModel` with a TensorFlow Lite model wrapper.
3. Add on-watch settings/onboarding for backend URL and user identity.
4. Persist rationale and events using Room.
5. Add companion-phone Data Layer support for contact and emergency workflows.
