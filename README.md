
# COMP3040 Mobile Device Programming Group B — s-CPD

## Project Overview

The s-CPD platform is an Android application designed to help students discover approved activities, book places, record attendance and track their earned points. It also allows clubs to manage events and the s-CPD office to review event proposals.

## Team Members
- Ahmed Gobran ([@Ahmedgobran](https://github.com/Ahmedgobran))
- Yew Xin Nie ([@NekomaPizza10](https://github.com/NekomaPizza10))
- Lishuai ([@yllnaf](https://github.com/yllnaf))
- Zheng Kai Qi ([@Zheng123-AA](https://github.com/Zheng123-AA))

## Project Documentation

### Part 1 — Requirements and Analysis

1. [Requirements Analysis](docs/requirements-analysis.md)
2. [Challenges and Assumptions](docs/challenges-assumptions.md)
3. [Technical Design](docs/technical-design.md)
4. [User Interface Design](docs/ui-design.md)

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
