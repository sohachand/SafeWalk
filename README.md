# SafeWalk

An Android (Java) personal-safety app for walking alone. A Mamdani fuzzy-logic engine turns three
inputs - how long you have been stationary, how far you have strayed from your route, and the
time of night - into a 0-100 risk score, and escalates to alerts when risk is high.

## Features
- Fuzzy risk engine (fuzzify -> min-AND rules -> max aggregation -> centroid) with a plain-language explanation of why the risk changed
- Live walk tracking, shake-to-SOS, manual SOS, fake call
- Arrive-by timer (dead-man's switch): no confirmation -> automatic SOS
- Siren + flashlight strobe, SMS alerts with battery level and map link
- Risk timeline chart and GPS route trail coloured by risk
- Firebase Auth login, Firestore walk history sync, live location sharing with a "Follow a walk" screen
- Works fully offline when Firebase is not configured

## Setup
1. Open in Android Studio.
2. (Optional, for cloud features) Create a Firebase project, register the app `com.example.project`,
   download `google-services.json` into `app/`, enable Email/Password auth and create a Firestore database.
3. Run on an emulator or device.
