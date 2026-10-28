## 2. Challenges and Assumptions

### 2.1 Challenges and Risk Mitigation

#### Secure Attendance Verification

One of the main challenges is preventing fraudulent attendance while maintaining an efficient check-in process. Rather than letting students check themselves in via a publicly displayed event QR code, the student app generates a booking-specific, digitally signed QR attendance pass that automatically refreshes every 20 seconds. At the venue entrance, only authorised committee members assigned to that event (C3) can scan attendance passes. Both online and prepared offline scanners validate the event, booking status, attendance pass expiry, check-in window, and the backend's digital signature before recording attendance.

The table below analyses each potential method of attendance cheating, how our design prevents it, and where the approach remains weak:

| Way of cheating | How it is prevented | Remaining weakness |
|---|---|---|
| From home | Students cannot check in using a shared event code. An authorised club member must scan each student's QR attendance pass at the event entrance (C3, C4). | A student at home could send their QR attendance pass to a friend at the venue, who could present it before the 20-second expiry time. |
| Old screenshot | Each QR attendance pass is valid for only 20 seconds. The scanner rejects expired attendance passes, and the app restricts screenshots on the attendance pass screen (S7, S8). | Someone could take a photo of the attendance pass using another phone or share it quickly while it is still valid. |
| Repeated scan | When online, scanners reject attendance passes that have already been used. When offline, the same scanner blocks duplicate scans. When records sync, the server checks for duplicates and awards points only once per student per event (S8, C4). | If two separate scanners are offline, both could temporarily accept the same attendance pass. The duplicate would only be detected when their records sync. |
| Scan after the event | Scanners only accept check-ins during the event's allowed check-in period. The app records when the attendance pass was scanned separately from when it was uploaded, allowing valid offline records to sync later. | When offline, the scanner relies on its stored time and device clock. Someone who changes the device's time settings could affect the recorded scan time. |
| Fake QR | Each attendance pass contains booking details and a digital signature created by the server. The scanner checks this signature to confirm that the attendance pass is genuine and has not been changed. | If a club member's account or scanner device is compromised, or a club member helps someone cheat, false attendance could still be recorded. |
| Friend using a phone | An authorised club member supervises the entrance and scans each student's attendance pass, making it harder to check in friends without being noticed. | To keep the queue moving, staff may not check every student's ID. A friend presenting someone else's phone or attendance pass could therefore still be marked as present. |
| Fake or replayed offline record | Each saved record carries a unique scan ID, the signed attendance pass, and a signature from the registered scanner. The server rejects a repeated scan ID or pass and re-checks the pass signature, check-in window and scanner permission. | The signature does not prove the student was physically present, the time was accurate, or the committee member was honest. |
| Unauthorised or outdated scanner | Scanning needs an event-specific permission linked to a registered scanner. The server rechecks it when records sync. | If permission is withdrawn while the scanner is offline, the problem is found only at sync. |

Overall, this design makes casual cheating harder but cannot fully prevent impersonation.

#### Offline Attendance and Data Synchronisation

Another significant challenge is maintaining reliable attendance recording when internet connectivity is unavailable. Our system allows students to download signed attendance passes before an event, while authorised committee scanners store the necessary verification information locally. When offline, scanners save attendance records in protected local storage and upload them automatically once connectivity returns.

However, multiple offline scanners may record the same student, and locally stored booking information may become outdated. To address these issues, the backend verifies uploaded records, detects duplicates, and reconciles conflicting information. Attendance remains provisional until verification is completed. Records that cannot be verified automatically require further review. In addition, offline timing depends on previously established server-time references, which may become unreliable if device settings are manipulated.

**What is stored on the device:** the student's phone keeps the downloaded attendance pass data for booked events, and a prepared scanner keeps the event's check-in data and verification key plus the records it has saved. Both are kept in the app's protected storage. A saved record contains the pass ID, event ID, scan time and scanner ID and cannot be edited from the app. When records upload, the server re-checks the pass signature, the check-in window and the scanner's permission, ignores records that do not match or that repeat an earlier one, and counts each student once per event. This stops forged or replayed records from earning points. The scanner screen shows how many records are pending upload, and each student sees Pending, Needs review, Accepted or Not accepted in My SCPD points.

#### Booking Conflicts and System Reliability

Managing event capacity presents another challenge, particularly when several students attempt to reserve the final available place simultaneously. To prevent overbooking, the backend will validate capacity, existing reservations, and overlapping event times before confirming a booking. Atomic database operations are proposed to ensure that only one student can successfully reserve the final place.

#### Role-Based Access and Point Accuracy

**Authentication:** users sign in with their university username and password, or with registered test accounts if university sign-in is not available, through the backend's authentication service. The backend stores each user's role (student, club event manager, committee scanner, authorised reviewer or administrator) and checks it on every request, so hiding a button in the app is not the only protection. Scanning is allowed only for committee members added to that event, and attendance review needs a separate review permission.

The application must also protect sensitive information and prevent unauthorised actions. Students, club managers, committee scanners, and administrators have different permissions. Authentication and backend authorisation checks will ensure that users can only access permitted functions and records.

Furthermore, points must only be awarded after an event has ended and attendance has been verified. Duplicate point awards must be prevented, and any incorrectly awarded points must be reversible without deleting the original transaction history.

### 2.2 Assumptions and Constraints

The proposed system is based on several assumptions.

First, all users are assumed to have registered accounts with appropriate permissions. Club management and attendance-scanning privileges are assigned by authorised personnel rather than selected freely during registration. University authentication may be used if available; otherwise, registered test accounts will support development and testing.

Second, students and committee scanners are expected to connect to the internet before events to download the necessary attendance passes and verification information. Each event has a check-in window, which we assume opens 15 minutes before the event starts and closes 30 minutes after it starts. Scanners accept attendance passes only inside this window, and the automatic-sync deadline of 24 hours counts from its closing time. Records that arrive later are reviewed rather than discarded. Offline attendance is supported only when this preparation has been completed successfully. Booking and cancellation functions still require an internet connection.

Third, the backend database is assumed to provide a central and reliable source of information for events, bookings, attendance, and points. Approved event details cannot be modified directly; changes require a new proposal and administrative approval. So the answer to whether a club can change an event after approval is no: approved details are locked, a club submits a revised proposal, the original stays active until Admin approves it, then the revision becomes active and the original is kept in history.

Finally, the application will be developed for the specified Android testing environment. Since emulator capabilities and real-world network conditions may differ, testing will include simulated network failures, duplicate scans, invalid attendance passes, and booking conflicts. Because emulators cannot scan a real QR code reliably, the scanner will offer a test mode that accepts a pasted attendance pass code and runs the same checks. These tests will help evaluate the system's reliability and identify remaining limitations.
