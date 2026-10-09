# LightAlarmClock

A hardware-software alarm clock system that combines an Android app, BLE communication, and an ESP32 firmware controller to trigger a pre-alarm light sequence before the alarm rings.

This project demonstrates end-to-end product engineering across mobile, embedded firmware, and electronics design.

## Why this project

Traditional alarms wake you up abruptly with sound. LightAlarmClock introduces a softer wake-up flow:

1. 10 minutes before alarm: Android app sends a BLE command
2. ESP32 receives command: firmware starts a timed IR/light transmission sequence
3. Alarm time: full alarm rings on phone with dismiss/snooze UI

This creates a practical blueprint for a smart alarm + ambient wake-up device.

## System Architecture

### 1) Android App (`Android/`)
- Built with Kotlin + Jetpack Compose
- Vibecoded Android implementation focused on fast iteration and feature validation
- Lets users:
  - create/edit/delete alarms
  - configure recurring schedules
  - choose ringtone
  - toggle vibration/notification
  - configure BLE target (device address, service UUID, characteristic UUID, message)
- Uses `AlarmManager.setExactAndAllowWhileIdle` for precise alarms
- Triggers a BLE foreground service (`BleMessageService`) 10 minutes before alarm
- Launches a full-screen alarm activity (`AlarmActivity`) and ringing service (`AlarmRingingService`) at alarm time

### 2) ESP32 Firmware (`Rust - ESP32/`)
- Built in Rust using `esp-idf-svc` + `esp32-nimble`
- Exposes a custom BLE service/characteristic
- On write, starts a state-machine-driven transmission (PWM + timer interrupts)
- Generates a modulated sequence (38.2 kHz carrier) suitable for IR/light control workflows

### 3) Hardware Shield (`LED_shield/`)
- KiCad project for custom LED/IR driver shield
- Includes:
  - schematic (`.kicad_sch`)
  - PCB layout (`.kicad_pcb`)
  - Gerber outputs (`GBR/`)
  - BOM (`BOM.csv`)
- Components include an IR LED (LD271), regulator (LM7805), and supporting passives/connectors

## Key Features

- End-to-end alarm pipeline from mobile UI to physical actuation
- Foreground services for reliable background BLE and alarm behavior
- Lock-screen alarm UX with Snooze / Dismiss actions
- Recurring alarm scheduling and day selection
- Hardware manufacturing assets included (Gerbers + BOM)

## Tech Stack

### Android
- Kotlin
- Jetpack Compose (Material 3)
- Android AlarmManager / BroadcastReceiver / Foreground Service

### Embedded
- Rust (edition 2021)
- `esp-idf-svc`
- `esp32-nimble` (BLE stack)

### Hardware
- KiCad (schematic + PCB)
- Through-hole + connector-based shield design

## Repository Structure

```text
LightAlarmClock/
├── Android/           # Kotlin Android app (UI, alarms, BLE client)
├── Rust - ESP32/      # ESP32 Rust firmware (BLE peripheral + signal generation)
├── LED_shield/        # KiCad hardware design + Gerbers + BOM
└── LICENSE            # GPL-3.0
```

## Getting Started

## Prerequisites

- Android Studio (latest stable)
- Android device running API 33+ (for current app config)
- ESP32 development board
- Rust + esp-idf toolchain for ESP32 builds
- BLE-capable phone and ESP32 in range

## 1) Run Android App

```bash
cd Android
./gradlew assembleDebug
```

Then install from Android Studio or via adb.

## 2) Flash ESP32 Firmware

From `Rust - ESP32/`, build/flash with your ESP-IDF Rust setup (tooling varies by environment).
The firmware advertises BLE service:

- Service UUID: `6827989e-079a-404f-946a-da39f968ce82`
- Characteristic UUID: `c696a5a1-827f-4df2-8260-5cdc7f0a4f44`

## 3) Configure BLE in App

In the app:
- Open Bluetooth Settings
- Enter ESP32 MAC address
- Confirm service/characteristic UUIDs
- Set message payload
- Use Test Connection and Send Test Message

## Alarm Flow (Runtime)

1. User schedules alarm in app
2. App registers:
   - main alarm trigger
   - BLE pre-alarm trigger (10 minutes earlier)
3. Pre-alarm fires -> `BlePreAlarmReceiver` starts `BleMessageService`
4. BLE write triggers ESP32 transmission sequence
5. Main alarm fires -> alarm UI + ringing foreground service
6. User snoozes or dismisses

## Recruiter Highlights

This project showcases:

- Cross-domain engineering: mobile + embedded + PCB
- Real-world Android architecture: alarms, receivers, services, lock-screen behavior
- Low-level firmware work: timers, PWM, BLE GATT handling
- System integration mindset: one product, multiple layers, clear signal path
- Product thinking: user-centric wake-up experience, not just a technical demo

## Known Improvements / Roadmap

- Add automated tests (unit + instrumentation) for alarm scheduling edge cases
- Improve BLE retry/backoff and connection lifecycle handling
- Add screenshots/GIFs for app UX in this README
- Add hardware bring-up and calibration guide
- Harden production alarm reliability across OEM battery optimizations

## License

This project is licensed under GNU GPL v3.0.
See [`LICENSE`](LICENSE).
