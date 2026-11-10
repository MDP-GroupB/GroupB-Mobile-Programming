# s-CPD Firestore Schema

Last updated: 10 Nov 2026 · Owner: Area A

## Conventions

Every area builds on these collections and field names, so please comment here before you change one. Area A owns this doc and the Security Rules that enforce it.

- **Names:** collections and fields use camelCase: words joined, each new word capitalised (`slotHashes`, `checkinOpens`). Status values are stored in lowercase (`approved`, never `Approved`), because code compares exact text.
- **Times:** every time is a Firestore `Timestamp`, Firestore's own date-and-time type (stored in UTC), so the app and the rules can compare times. The app shows times in Malaysia time (UTC+8).
- **IDs:** Firestore auto-IDs, except where a fixed ID enforces a rule. `uid` is the unique ID Firebase Authentication gives each account. It is not the student ID and not the QR code. A booking and its attendance record both use the student's `uid`, so there is one of each per student per event.
- **Who writes what:** the app writes only what the Security Rules allow. Admin (the s-CPD office) works in the Firebase console, which bypasses the rules. Admin approves events, sets points, assigns roles and adds point reversals there.
- **No backend code:** on the free Spark plan the backend is the Firestore database plus its Security Rules. Admin types its few records into the Firebase console.
- **Copied fields:** some fields are copied from another document so one read is enough. For example, a booking stores the student's name, so the scanner downloads one list with every name in it. The rules check that each copy matches its source when it is written.
- **No stored totals:** points are never stored as awards. They are calculated from attendance records and reversals (see Points).

## Collection map

```
Top-level collections
├── users/{uid}               roles, set by Admin
├── directory/{email}         email lookup for adding team members
├── clubs/{clubId}            club name, Drive folder link, officers
├── events/{eventId}          proposal + approved event
│   ├── team/{email}          scan and review permission
│   ├── bookings/{uid}        one booking per student
│   ├── attendance/{uid}      one attendance record per booking
│   ├── waitlist/{uid}        nice-to-have
│   └── revisions/{autoId}    nice-to-have
└── pointReversals/{autoId}   added by Admin
```

Keeping team, bookings and attendance under the event lets each Security Rule read the event, the booking and the team entry straight from the path, with no extra lookups.

## users and clubs

Roles live in `users/{uid}` and only Admin sets them, in the console. Committee scanners and reviewers are ordinary students who are listed in an event's `team` (below), so they need no separate role.

**users/{uid}** (document ID = Firebase Auth uid)

| Field | Type | Written by | Notes |
| --- | --- | --- | --- |
| name | string | Admin | Shown on bookings and scan results |
| email | string | Admin | Same as the Auth email, lowercase |
| maskedId | string | Admin | Student ID with the middle hidden, e.g. `203***12` |
| role | string | Admin | `student` or `admin` |
| managedClubId | string or null | Admin | The one club this user manages (C1, A2). null = not a manager |
| createdAt | timestamp | Admin | |

A user can read only their own document. The app never writes to `users`. Admin assigns only the event manager (`managedClubId`). The manager then adds committee members per event (see team).

**directory/{email}** (document ID = the student's lowercase email)

The manager types a student's email to add them to an event team. The app reads `directory/{email}` and shows the name and masked ID, so the manager can confirm it is the right person. Club managers can read one entry by its exact email but cannot list the collection, so nobody can browse every student.

| Field | Type | Written by | Notes |
| --- | --- | --- | --- |
| name | string | Admin | Same as in users |
| maskedId | string | Admin | Same as in users |

**clubs/{clubId}**

| Field | Type | Written by | Notes |
| --- | --- | --- | --- |
| name | string | Admin | Shown as the organiser in Discover |
| driveFolderUrl | string | Admin | The club's Google Drive folder, opened by the Open club folder button |
| officerEmails | array of strings | Admin | Lowercase emails of the club's president and vice-president. They can review attendance for every event of the club |

Any signed-in user can read clubs.

## events

One `events/{eventId}` document holds both the proposal and the approved event, so Admin approves or rejects by changing one `status` field in the console. Once `status` is `approved`, the club can no longer edit the event (R1).

| Field | Type | Written by | Notes |
| --- | --- | --- | --- |
| clubId | string | Manager | Must equal the manager's `managedClubId`, so a manager can only create events for the club Admin assigned them (A2) |
| clubName | string | Manager | Copied from `clubs`, so Discover can filter by organiser without reading every club |
| createdBy | string (uid) | Manager | |
| title, description, venue | string | Manager | |
| startTime, endTime | timestamp | Manager | endTime after startTime |
| bookingCutoff | timestamp | Manager | startTime − 15 min. One cut-off for booking, cancelling and waiting-list actions. Rules check the value |
| checkinOpens | timestamp | Manager | startTime − 10 min. Rules check the value |
| checkinCloses | timestamp | Manager | startTime + 30 min. Rules check the value |
| capacity | int | Manager | 1 or more |
| bookedCount | int | Booking transaction | Starts at 0. Changes by exactly +1 or −1 together with a booking (Area B) |
| requestedPoints | int | Manager | Points the manager asks for |
| points | int or null | Admin | Approved points, which can differ from requestedPoints. null until approved |
| documentsUploaded | bool | Manager | The documents uploaded tick for the club Drive folder |
| status | string | Manager / Admin | See below |
| reviewNote | string | Admin | Reason for rejecting, shown to the club |
| decidedBy, decidedAt | string, timestamp | Admin | Who decided and when (A1) |
| createdAt, submittedAt | timestamp | Manager | When the draft was made and when it was sent. Used to sort proposals and show Admin which came first |

**Status values**

| Status | Set by | Meaning |
| --- | --- | --- |
| draft | Manager | Being written. Visible to the club only |
| pending | Manager | Submitted. Locked while Admin reviews |
| rejected | Admin | reviewNote (the reason) is required. The manager can fix the event and resubmit, which sets it back to pending |
| approved | Admin | Live in Discover and locked. Admin also sets points |
| cancelled | Admin | Nice-to-have (A4). The club asks Admin to cancel an approved event. Bookings stop, and the event stays in history instead of being deleted |

No `ended` status is stored. Points are credited only after an event ends, and nothing on the free plan can change a status at the right moment. The app therefore treats an event as ended when `endTime` is before now.

## events/{eventId}/team

A student can scan for an event only if `events/{eventId}/team/{email}` exists, and can review only if that document has `canReview: true` (C3, C10). The manager adds a member by typing their email. The app looks it up in `directory`, shows the name and masked ID to confirm, then saves the team document with the email as its ID. The rules check that ID against `request.auth.token.email`, the email of the signed-in user.

| Field | Type | Written by | Notes |
| --- | --- | --- | --- |
| name, maskedId | string | Manager | Copied from `directory` when the member is added |
| canScan | bool | Manager | Always true for a team member |
| canReview | bool | Manager | Separate review permission |
| addedBy | string (uid) | Manager | |
| addedAt | timestamp | Manager | |

Only the manager of the event's club can add or remove members. Three groups can review an event's attendance: the club's event manager, the president and vice-president listed in the club's `officerEmails`, and team members with `canReview: true`. Removing someone takes away their scan permission at once, and the rules re-check it on every upload.

## events/{eventId}/bookings

The booking ID is the student's `uid`, so a student has at most one booking per event. Cancelling sets `status` to `cancelled`. Booking again sets it back to `confirmed` with new hashes. Area B writes the booking and Area C supplies `slotHashes`.

| Field | Type | Written by | Notes |
| --- | --- | --- | --- |
| userId | string (uid) | Student | Must equal the document ID and the signed-in user |
| studentName, maskedId | string | Student | Copied from `users/{uid}` for the scanner's list |
| eventTitle | string | Student | Copied from the event, for My bookings |
| eventStart, eventEnd | timestamp | Student | Copied from the event, for the clash check |
| status | string | Student | `confirmed` or `cancelled` |
| bookedAt | timestamp | Student | Must equal the server time of the write |
| cancelledAt | timestamp or null | Student | |
| slotHashes | array of 120 strings | Student | See below |

**slotHashes:** check-in runs from `checkinOpens` to `checkinCloses` (40 minutes), cut into 120 slots of 20 seconds. Slot `n` starts at `checkinOpens + 20n` seconds. The phone keeps a secret seed (it never leaves the phone) and makes one code per slot with HMAC-SHA256. It stores only `slotHashes[n]` = SHA-256 of slot n's code, as 64 lowercase hex characters. Areas C and D agree the exact code format in their shared function.

**Rules for this collection:**

- A student can create or update only their own booking, and only before `bookingCutoff`.
- A booking can be created only when the event is `approved` and not full. The same write must change the event's `bookedCount` by exactly +1, and cancelling must change it by −1.
- The clash check runs in the app, because a rule cannot search a student's other bookings.
- A student reads their own bookings with a collection-group query on `bookings` where `userId` == uid.
- Team members of the event can read its whole booking list (Area D's Prepare scanner).

## events/{eventId}/attendance

The record ID is the student's `uid` (the same as the booking ID), and records can only be created, never overwritten. A replayed or second scan therefore fails, and the scanner shows it as a duplicate. Scanned passes and manual requests share this collection, told apart by `method`.

| Field | Type | Written by | Notes |
| --- | --- | --- | --- |
| userId | string (uid) | Scanner | Equals the document ID |
| method | string | Scanner | `qr` or `manual` |
| slot | int | Scanner | 0 to 119. qr only |
| code | string | Scanner | The scanned code. Rules check SHA-256(code) == the booking's `slotHashes[slot]`. qr only |
| scannedAt | timestamp | Scanner | Device time of the scan. Must fall inside the check-in window, match the slot (or the slot before it during the first 5 seconds of a new slot), and not be in the future |
| uploadedAt | timestamp | Scanner | Must equal the server time of the write |
| scannedBy | string (uid) | Scanner | The signed-in team member |
| points | int | Scanner | Copied from the event, checked by rules |
| eventEnd | timestamp | Scanner | Copied from the event, used by the points total |
| status | string | Scanner / reviewer | See below |
| reason | string | Scanner / reviewer | Manual: why there was no pass. Rejected: why |
| idCheckMethod | string | Scanner | Manual only, e.g. "student card checked" (C9) |
| reviewedBy, reviewedAt | string, timestamp | Reviewer | Set on confirm or reject (C10) |

**Status values**

| Status | When |
| --- | --- |
| verified | A qr record that passes every rule and is uploaded within 24 hours of `checkinCloses` |
| needs_review | Every manual request, and any qr record uploaded more than 24 hours after `checkinCloses` (the rules force this) |
| rejected | Set by a reviewer or Admin, with a reason |
| escalated | Nice-to-have: sent to Admin by a reviewer (C11) |

A scan that breaks a rule (wrong hash, outside the window, no permission) is refused by Firestore and never stored. The scanner shows the reason from its own local check. Only reviewers (see team) can change `status`, `reason` and `reviewedBy`/`reviewedAt`.

The student's Pending state (S10) is shown by the app while a record is still in the scanner's upload queue, so it is never stored.

## Points

A student's total is calculated, not stored. It is the sum of `points` over their verified attendance records whose `eventEnd` has passed, minus the sum of their reversals (S11, A3).

```
total = sum(points of verified records whose eventEnd < now) − sum(points of reversals)
```

The app gets the records with a collection-group query on `attendance` where `userId` == uid.

**pointReversals/{autoId}** (Admin adds these in the console. Students read only their own)

| Field | Type | Notes |
| --- | --- | --- |
| userId | string (uid) | The student |
| eventId | string | The event whose award is reversed |
| points | int | The positive number taken off the total |
| reason | string | Required. Shown in the points history |
| reversedBy | string | Admin's name or uid |
| createdAt | timestamp | |

The original record stays verified, and the history shows the award next to the reversal.

## Nice-to-have collections

These two are only built if the must-haves are solid. Their owners can refine the fields.

**events/{eventId}/waitlist/{uid}** (Area B, S12)

| Field | Type | Notes |
| --- | --- | --- |
| userId | string (uid) | Equals the document ID |
| joinedAt | timestamp | Sets the queue order |
| status | string | `waiting`, `offered`, `accepted` or `lapsed` |
| offerDeadline | timestamp or null | Set when a place is offered |

**events/{eventId}/revisions/{autoId}** (Area A, C5)

| Field | Type | Notes |
| --- | --- | --- |
| changes | map | Only the fields that change, e.g. `{venue, startTime, endTime}` |
| reason | string | Required |
| status | string | `pending`, `approved` or `rejected` |
| createdBy, createdAt | string, timestamp | |

The event stays live and unchanged while a revision is pending. When Admin approves it, Admin copies the new values onto the event in the console, which keeps the event ID so existing bookings stay valid.

## Changes from the Part 1 ERD

These go in the Part 2 report's "Changes from Part 1" table. Part 1 itself stays unchanged.

| Part 1 | Part 2 | Why |
| --- | --- | --- |
| EVENT_PROPOSAL and EVENT as separate entities | One `events` document with a status | Admin approves with one field edit in the console |
| Returned for changes status (C2, A1) | Removed. Admin rejects with a reason, and the manager can fix and resubmit | One fewer status for Admin and the club |
| ATTENDANCE_PASS rows signed by a server key | `slotHashes` on the booking. The seed stays on the phone | No Cloud Functions on the Spark plan, so there is no server key |
| SCANNER_DEVICE with a scanner signature | Removed. Scan permission comes from `team`, and the record is tied to the signed-in user | Security Rules cannot verify a device signature |
| SCANNER_PERMISSION per device | `events/{id}/team/{email}` with canScan and canReview | Simpler, and checked by rules on every write |
| Reviewer = club member with review permission | Also the club's event manager, president and vice-president by default | A club does not depend on one person to review |
| CLUB_ASSIGNMENT | `users.managedClubId`, set by Admin | One club per manager, as in Part 1 |
| MANUAL_ATTENDANCE_REQUEST | An attendance record with `method: manual` | One record per booking covers both methods |
| POINTS_LEDGER_ENTRY awards and reversals | Awards calculated from verified records. Only reversals are stored | No server code could write awards at the right time |
| AUDIT_LOG | decidedBy/decidedAt on events, reviewedBy/reviewedAt on records, and reversal documents | Admin works in the console, so the app cannot log those actions |
| Check-in opens 15 min before start | 10 min before start, with the booking cut-off at 15 min | Gives the scanner time to download the final list |
| Attendance pass valid for 20 seconds | 20-second slots, plus a 5-second grace for the previous slot (up to 25 seconds) | A genuine student is not rejected when the code changes mid-scan or the phone clocks differ slightly |

## Decisions

- **Neighbouring slot:** only the previous slot, and only during the first 5 seconds of a new slot, so a code is valid for at most 25 seconds.
- **QR code format:** `eventId|uid|slot|code`.
- **Clash check (S5):** runs in the app only, because Security Rules cannot search all of a student's bookings. Area B accepted this; it is listed as a known weakness in the report.
- **maskedId format:** 8 digits, first 3 and last 2 shown, e.g. `203***12`.
- **Collection-group indexes:** `bookings.userId` and `attendance.userId`, set in `firestore.indexes.json`, so My bookings and My SCPD points can search across all events.