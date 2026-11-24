
# COMP3040 Mobile Device Programming Group B — s-CPD

## Project Overview

The s-CPD platform is an Android application designed to help students discover approved activities, book places, record attendance and track their earned points. It also allows clubs to manage events and the s-CPD office to review event proposals.

## Team Members

| Name | GitHub | Area |
|---|---|---|
| Yew Xin Nie | [@NekomaPizza10](https://github.com/NekomaPizza10) | A: Foundation, security, club side |
| Lishuai | [@yllnaf](https://github.com/yllnaf) | B: Student booking |
| Zheng Kai Qi | [@Zheng123-AA](https://github.com/Zheng123-AA) | C: Pass, points, reviewer |
| Ahmed Gobran | [@Ahmedgobran](https://github.com/Ahmedgobran) | D: Committee scanner |

## Getting started

### How to build and run

1. Install the latest stable **Android Studio**.
2. Clone this repository and open the project folder in Android Studio (**File › Open**). Wait for **Gradle sync** to finish.
   `app/google-services.json` is included, so the app connects to our Firebase project (`mdp-scpd-groupb`) with no extra setup.
3. Create the test emulator: **Device Manager › + › Create Virtual Device › Pixel 2**, with a system image of **Android 12 (API 31) or higher**.
4. Select the Pixel 2 emulator and click **Run ▶** (app module).
5. The app needs an internet connection to sign in.

### How to run the tests

- **Unit tests** (run on your computer, no emulator): `./gradlew testDebugUnitTest` (on Windows: `.\gradlew testDebugUnitTest`)
  Covers sign-in (ViewModel with a fake repository), booking rules (clash and cut-off), the pass code generator, the check-in window and slots, the QR code checker and the Prepare scanner ViewModel.
- **Instrumented test** (needs the emulator running): `./gradlew connectedDebugAndroidTest`
  Covers the scanner's Room database (`ScannerDaoTest`).

### Test accounts

These are throwaway test accounts in our Firebase project. Do not reuse these passwords anywhere.

| Role | Email | Password    | What to try |
|---|---|-------------|---|
| Student | student1@example.com | `Test1234!` | Discover events, My pass |
| Student | student2@example.com | `Test1234!`  | Same as student1 |
| Event manager | manager@example.com | `Test1234!`  | Home shows Manage events (robotics-club) |
| Committee member | committee@example.com | `Test1234!`  | Committee tools › Prepare scanner for `test-event-1` |
| Reviewer | reviewer@example.com | `Test1234!`  | Reviewer screens come in the final release |
| Admin | admin@example.com | `Test1234!`  | Admin work is done in the Firebase console |

## Releases

- [Release 2 — 24 Nov 2026](docs/releases/release-2.md): test data, what works and what's coming

## Project Documentation

### Part 1 — Requirements and Analysis

1. [Requirements Analysis](docs/part1/requirements-analysis.md)
2. [Challenges and Assumptions](docs/part1/challenges-assumptions.md)
3. [Technical Design](docs/part1/technical-design.md)
4. [User Interface Design](docs/part1/ui-design.md)

### Part 2 — Implementation
- [Firestore schema](docs/part2/firestore-schema.md)

## Team areas and workflow

| Area | Owner        | Builds |
  |---|--------------|---|
| A: Foundation, security, club side | Yew Xin Nie  | Firebase, sign-in and roles, Security Rules, club manager screens, README |
| B: Student booking | Lishuai      | Discover, event details, booking, cancel, My bookings |
| C: Pass, points, reviewer | Zheng Kai Qi | Attendance pass, My SCPD points, reviewer screens |
| D: Committee scanner | Ahmed Gobran | Scanner, offline save, upload sync, manual request |

Each area works on its own branch (`area-a` … `area-d`), pushes weekly,
and merges into `main` by pull request after review by another member.

## Third-party libraries and sources

Add a row whenever you add a library, or use a tutorial or example code.

| Library / source | Used for | Area | Licence |
|---|---|---|---|
| AndroidX (Core, Activity, Lifecycle, ViewModel) | App components, ViewModels | All | Apache 2.0 |
| Jetpack Compose + Material 3 | All screens | All | Apache 2.0 |
| Navigation Compose | Moving between screens | A | Apache 2.0 |
| Firebase Android SDK (Auth, Firestore) | Sign-in and shared data | All | Apache 2.0 |
| kotlinx-coroutines-play-services | Awaiting Firebase calls | A | Apache 2.0 |
| Firebase CLI (firebase-tools) | Deploying and testing Security Rules | A | MIT |
| JUnit 4 | Unit tests | All | EPL 1.0 |
| AndroidX Test / Espresso | UI tests | All | Apache 2.0 |
| ZXing core | Drawing the pass QR code | C | Apache 2.0 |
| Room | Offline scanner storage | D | Apache 2.0 |
| KSP (Kotlin Symbol Processing) | Generating Room's database code | D | Apache 2.0 |
| kotlinx-coroutines-test | Testing ViewModels and coroutines | D | Apache 2.0 |
