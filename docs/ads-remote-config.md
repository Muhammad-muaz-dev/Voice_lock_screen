# Ads and Firebase Remote Config

Ads use the Google Mobile Ads **Next-Gen SDK** (`com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk`).
Ad unit IDs are Google's official **test IDs** in `ads/AdIds.kt` and the manifest; replace them before release.

Global kill switch (unchanged): ads are off for Pro users or when the server JSON has `ads_status` other than `"on"`.

## Per-screen ad JSON

Each screen has its own key. Value format (all fields optional; missing fields use the default):

```json
{"AppOpenEnable":0,"InterstitialEnable":1,"BannerEnable":1,"NativeEnable":0,
 "SplashConsentTimeout":7000,"NativeFormat":2,"AdCtaColor":"#A167FE",
 "AdCtaTextColor":"#FFFFFF","CtaTextStyle":"bold"}
```

| Key | Screen | Default |
|---|---|---|
| `splash_ads_config` | Splash | App open on, native bottom ad, compact native, 7000 ms |
| `language_ads_config` | Language | Native on, compact native |
| `onboarding_ads_config` | Onboarding | Native on, native with media |

Field meaning:

| Field | Meaning |
|---|---|
| `AppOpenEnable` / `InterstitialEnable` | Splash full-screen ad. App open is tried first; if it fails and interstitial is also 1, an interstitial is loaded. |
| `NativeEnable` / `BannerEnable` | Bottom ad. Native wins when both are 1. |
| `SplashConsentTimeout` | Longest the splash waits for its full-screen ad (ms, clamped 1000–20000). |
| `NativeFormat` | `1` compact (icon, headline, button) · `2` large with media. |
| `AdCtaColor` / `AdCtaTextColor` | Native button colours (`#RRGGBB`). |
| `CtaTextStyle` | `bold`, `semibold`, `medium` or `normal`. |

## Screen and placement switches

| Key | Type | Default | Effect |
|---|---|---|---|
| `inapp_screen_enabled` | bool | true | Premium screen after splash (every launch, non-Pro users) |
| `language_screen_enabled` | bool | true | Language screen on first launch |
| `onboarding_enabled` | bool | true | Onboarding on first launch |
| `onboarding_page_1_enabled` … `onboarding_page_4_enabled` | bool | true | Individual onboarding pages |
| `home_banner_enabled` | bool | true | Banner at the bottom of Home |
| `cooldown_inter_enabled` | bool | true | Cooldown interstitial |
| `inter_cooldown_seconds` | number | 30 | Minimum time between full-screen ads |

## Behaviour notes

- Onboarding: with a native ad the content uses 65% of the height and the ad 35%; without one it uses the full screen. Each page loads a fresh native the first time it is shown.
- Cooldown interstitial: shown on every navigation in the main app (opening a feature or going back) once the cooldown has passed. Any full-screen ad, splash included, restarts the cooldown, counted from when the ad is closed. Not shown on the lock-screen preview or Premium.
