# Release 2 — 24 Nov 2026

Work-in-progress release for SQA testing. Screens that are not finished say so on screen ("coming in the final release"). The final release is on 1 Dec 2026.

### Build, run and test steps, and the test accounts, are in the [README](../../README.md#getting-started).

## Test data
- **Test event ID:** `test-event-1` (Intro to Robotics Workshop, robotics-club, approved, 25 Nov 2026 14:00–16:00).
- It has one confirmed sample booking (Student One), and `committee@example.com` is on its team.
- **Try:** sign in as the committee account › **Committee tools** › type `test-event-1` › **Download Roster for Offline Scanning**. It shows "1 confirmed booking(s) cached" and a **Proceed to Scanner** button.

## What works in this release
- Sign in with email and password, with friendly messages for wrong details or no internet. Sign out.
- Role-based Home: students, event managers (Manage events) and admins see different options.
- Security Rules in Firestore: users read only their own profile, only approved events are public, students write only their own booking, only the club's manager changes an event team, and only team members can create attendance records.
- **Discover events:** search by event or club, filter by organiser, and a "No events found" message. Uses **sample events** for now.
- **My pass:** a QR code that changes every 20 seconds with a countdown. Uses a **sample pass** (test mode) for now.
- **Prepare scanner:** downloads an event's confirmed bookings into the phone's Room database so scanning can work offline.

## Coming in the final release

- Discover shows live events from Firestore, plus event details and booking (capacity, clash and 15-minute cut-off), cancel and My bookings.
- The real attendance pass for each booking, with screenshot blocking and offline use.
- Scanning QR codes (camera and paste-code test mode), offline save and upload, and attendance results.
- Manage events for club managers: create and submit a proposal, proposal status, manage the event team and the club folder button.
- My SCPD points, reviewer screens and reminders.