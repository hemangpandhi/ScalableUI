# Face Login in Scalable UI

## Confirmed use-case flow

1. **Car started / home** → `face_login_panel` enters `face_login_scanning`
2. **Oval frame** shows camera/face viewport (`face_login_view.xml`)
3. **Match enrolled face** → neon **blue** glow → Android user switch → restore HVAC/seat prefs → dismiss
4. **No match** → neon **orange** glow → “You are not a registered user.” → **Switch to Guest?**

## Scalable UI mapping

| Piece | Implementation |
|-------|----------------|
| Panel | `DecorPanel` `face_login_panel` in MultiPanelLandscapeRRO |
| Variants | `scanning` / `success` / `failed` / `hidden` |
| Events | `car_started`, `face_login_success/failed/dismiss/retry`, `_System_OnHomeEvent` |
| Controller | `FaceLoginViewController` |
| Enrollments | `face_login_enrolled_profiles` (RRO) + SharedPreferences |
| User switch | `CarUserSwitchHelper` → `UserManager.switchUser` (+ demo broadcast) |
| Prefs | `UserPreferenceRestorer` → `HVAC_TEMPERATURE_SET`, `HVAC_SEAT_TEMPERATURE` |

## GestureDetection bridge

[GestureDetection](https://github.com/hemangpandhi/GestureDetection) already computes landmark-ratio signatures.
Wire its match callback to:

```java
faceLoginController.onFaceMatch(FaceMatchResult.matched(profile));
// or
faceLoginController.onFaceMatch(FaceMatchResult.unregistered("You are not a registered user."));
```

Without MediaPipe (Cuttlefish), demo mode auto-resolves using
`face_login_demo_match_success` / `face_login_demo_scan_ms`.

## AOSP wiring required

1. Link `CarSysuiScalableBarControllers` into CarSystemUI `static_libs`
2. Register `FaceLoginViewController` in panel controller Dagger map (`face_login_panel`)
3. Privileged permission for `UserManager.switchUser` (SystemUI already privileged)
4. Rebuild MultiPanelLandscapeRRO after XML changes

## Demo toggle

```xml
<!-- res/values/face_login_config.xml -->
<bool name="face_login_demo_match_success">true</bool>  <!-- false → orange + Guest CTA -->
```

## dumpsys

```bash
adb shell dumpsys activity service SystemUI | grep -A20 FaceLogin
```
