# SafeWalk – Real-Device Test Notes

Tester: Atharv Dixit (I013, SAP 70122400069)
Device: Oppo phone (Android) – installed from the debug APK (`app-debug.apk`)

> Edit the Result column to match exactly what you saw on your phone.

|#|Test|Steps|Expected|Result|
|-|-|-|-|-|
|1|Install|Open APK, tap Install|App installs and opens||
|2|Splash + onboarding|Launch app|Splash, then onboarding pages||
|3|Sign-up / guest|Create account or continue as guest|Reaches permissions screen||
|4|Permissions|Allow Location, Contacts, Microphone, SMS|Permissions granted||
|5|Trusted circle|Add a contact (name, phone, relation)|Contact appears in list||
|6|Start walk|Enter from/to, choose sensitivity, start|Active Walk screen with risk score||
|7|Risk Lab|Home → Risk Lab, move sliders / presets|Score, tier and curves update||
|8|Manual SOS|Hold SOS button 5 s|Alert Sent screen, SMS sent||
|9|Shake SOS|Shake phone during a walk|SOS triggered||
|10|Emergency Help|Tap Call 112 / helplines|Dialer opens with number||
|11|Siren / strobe|Start siren from alert screen|Alarm tone and torch flash||
|12|Walk history|Finish a walk, open History|Walk listed with trail/timeline||

## Issue found: SMS permission blocked on Oppo

* Symptom: SMS permission was denied and the "Allow" option was greyed out.
* Cause: Android "restricted settings" blocks sensitive permissions (like SMS) for apps installed from an APK file instead of the Play Store.
* Fix: Settings → Apps → SafeWalk → ⋮ (top right) → **Allow restricted settings**, then Permissions → SMS → Allow.
* Note: the ⋮ menu option only appears after the permission has been denied once.

## Other observations

SMS Alert : Siren mode was not sending SMS on trusted contact phone 

How to FIX : Contact number must be right and SMS permissions should be allowed via PHONE Settings

