# Voice Lock design system — Version 1.0 (Clean Modern Violet)

Approved reference: **Voice Lock Design System v1.0** (Secure. Smart. Yours.).
This specification defines the reusable visual language for the Voice Lock app.
Reference values below are transcribed from the approved design board; component recipes
describe its visual intent.

## Foundations

- Theme: Light / Clean Modern. Mood: consistent, modern, friendly, rounded, violet accents.
- Typeface: **Plus Jakarta Sans** (with Sora compatibility), weights 400, 500, 600, 700.
- Reference frame: **390 × 844 dp**, adapting to **360–412 dp** widths.
- Android sizes: dp for geometry, sp for text.
- Source resources: `app/src/main/res/values/colors.xml`, `dimens.xml`,
  `typography.xml`, `themes.xml`.

## Color tokens

### 01. Color Palette

| Group | Token / Resource | Hex Value | Role |
|---|---|---|---|
| **Primary** | `primary` / `primaryStart` | #635BFF | Main brand violet; primary buttons, active toggles, accents |
| **Primary** | `primary_light` / `primaryLight` | #EDECFF | Soft lavender; icon backgrounds, tags, selected tints |
| **Primary** | `primary_dark` / `primaryDark` | #5047E2 | Deep indigo; pressed button states, gradient endpoints |
| **Background** | `bg` / `bg_screen` | #E9EEF6 | Screen background for every app screen (window, status bar, root layouts) |
| **Background** | `surface` / `card` | #FFFFFF | Cards, bottom sheet, bottom nav containers |
| **Background** | `border` | #E6E8EF | Card strokes, input outlines, unselected controls |
| **Text** | `primary_text` / `textPrimary` | #171829 | Headings, titles, main body text |
| **Text** | `secondary_text` / `textSecondary` | #74768A | Subtitles, helper text, captions |
| **Text** | `tertiary_text` / `textTertiary` | #A1A4B7 | Placeholders, disabled text, search hints |
| **Semantic** | `success` / `semantic_success` | #22C55E | Success states, active badges, positive toast |
| **Semantic** | `warning` / `semantic_warning` | #F59E0B | Warning indicators, quotes accent |
| **Semantic** | `error` / `semantic_error` | #EF4444 | Error outlines, error text, alerts |
| **Semantic** | `info` / `semantic_info` | #3B82F6 | Info badges, notifications, new tags |

### Component & Badge Specific Tokens

| Group | Token | Value | Role |
|---|---|---|---|
| Badges | `badge_not_setup_bg` / `_text` | #EDECFF / #635BFF | "Not Set Up" status pill |
| Badges | `badge_new_bg` / `_text` | #3B82F6 / #FFFFFF | "New" feature tag |
| Badges | `badge_pro_bg` / `_text` | #FEF3C7 / #D97706 | "Pro" entitlement tag |
| Badges | `badge_active_bg` / `_text` | #DCFCE7 / #16A34A | "Active" status indicator |
| Icons | `icon_bg_voice` | #EDECFF | Voice feature icon container |
| Icons | `icon_accent_pin` / `icon_bg_pin` | #F43F5E / #FFE4E6 | PIN feature icon & container |
| Icons | `icon_accent_fingerprint` / `icon_bg_fingerprint` | #10B981 / #DCFCE7 | Fingerprint feature icon & container |
| Icons | `icon_accent_camera` / `icon_bg_camera` | #3B82F6 / #EFF6FF | Camera feature icon & container |
| Icons | `icon_accent_quotes` / `icon_bg_quotes` | #F59E0B / #FEF3C7 | Quotes feature icon & container |
| Icons | `icon_accent_themes` / `icon_bg_themes` | #8B5CF6 / #F3E8FF | Themes feature icon & container |
| Icons | `icon_accent_settings` / `icon_bg_settings` | #64748B / #F1F5F9 | Settings feature icon & container |
| Controls | `switch_track_on` / `switch_track_off` | #635BFF / #E6E8EF | Switch track on / off |
| Overlays | `dialog_btn_cancel_bg` | #F1F2F6 | Dialog secondary / cancel button |
| Overlays | `scrim` / `scrimStrong` | #66000000 / #CC000000 | Coach-mark dim / full guide overlay above system settings |
| Glow | `primaryGlow` | #33635BFF | Soft violet halos, illustration glows, pressed-mic ring |
| Glow | `successGlow` | #3322C55E | Success badge halo |
| Semantic | `successStrong` | #16A34A | Voice-enrolment progress arc and check (with `successSoft` track) |
| Glass | `glassFill` / `glassDot` | #40FFFFFF / #99FFFFFF | Frosted keys and dots on wallpapers; translucent chips on the violet hero |
| Glass | `whiteSoft` / `whiteMuted` | #1FFFFFFF / #D9FFFFFF | Decorative circles and secondary text on the violet hero card |
| Overlays | `toast_bg_dark` / `toast_bg_info` | #1E293B / #EFF6FF | Dark toast / info banner backgrounds |

## Typography

Use `Text.VoiceLock.<role>` with `text_<role>` dimensions. Size and line height
are in sp. Every screen has one title (maximum two lines); descriptions use
`textSecondary`. Reuse a role from this table rather than inventing a size.

| Role | Size / line | Weight | Use |
|---|---|---|---|
| displayNumber | 44 / 52 | 700 | Paywall price |
| clock | 36 / 44 | 700 | Lock screen time |
| display | 34 / 42 | 700 | Paywall title |
| headline | 28 / 36 | 700 | Onboarding title |
| titleXL | 22 / 28 | 600 | Tab screen title |
| titleL | 20 / 28 | 600; 700 for success | Sub-screen title |
| titleM | 18 / 24 | 700 | Card title |
| titleS | 16 / 24 | 600 | Section heading |
| bodyL | 15 / 22 | 500 | List row, text input |
| bodyM | 14 / 20 | 400 | Description |
| bodyS | 13 / 20 | 400 | Hint, helper text |
| caption | 12 / 16 | 400 | Small hint, navigation label |
| overline | 11 / 14 | 600 | Badge, tag |
| buttonL | 16 / 24 | 700 | Main button |
| buttonS | 14 / 20 | 700 | Small button, chip |

Use bundled `sora_regular`, `sora_medium`, `sora_semibold`, and `sora_bold`
for their respective weights, or resolve the corresponding weight through the
`sora` font family. Text must wrap without clipping at increased font scale.

## Geometry

| Scale | Tokens and values (dp) |
|---|---|
| Spacing | xs 4, sm 8, md 12, lg 16, xl 20, xxl 24, xxxl 32, huge 40, max 48 |
| Radius | xs 8 (tags), sm 12 (icon squares), md 16 (cards/buttons), lg 20 (large cards), xl 24 (big surfaces) |
| Border | 1 default, 2 selected outline |
| Shadow | xs 2 subtle, sm 4 cards, md 8 elevated cards, lg 16 modals/sheets |
| Icon | xs 12 small, sm 16 standard, md 20 large, lg 24 extra large |

Android spacing and radius names are `spacing_<scale>` and `radius_<scale>`.
The existing 28/32 dp radii are compatibility extensions, not part of this board.
Shadow values describe the reference scale; Android elevation does not reproduce
identical shadows on every device. Icon sizes describe artwork, not touch areas.

## Reusable component recipes

| Component | Recipe and states |
|---|---|
| App bar | Dark background; back affordance at start, Sora screen title, compact circular premium/settings actions at end. Respect status bar inset. |
| Primary button | Violet gradient, md radius, white icon and buttonL label. Disabled: surfaceDisabled and textDisabled, with action disabled. |
| Text button | primarySoft/primaryLight label and optional directional icon; no filled background. |
| Card | surface background, border stroke, md radius; titleM heading. Large containers may use surfaceSunken with lg/xl radius. |
| Lock status card | Leading lock illustration, title, trailing toggle; inner status/example strip with divider. Status requires text as well as color. |
| Icon tile | Rounded outlined tile; colored circular icon, centered short title, secondary description. Use brand violet for camera and semantic green for PIN as shown. |
| List row | Rounded surface with border; leading icon tile, bodyL label, secondary caption, trailing chevron or control. |
| Search/input | field background, outline stroke, md radius, leading search icon; placeholder uses textPlaceholder. Error uses error outline and errorSoft helper text. |
| Switch | Off: `switch_track_off` (#D4D7E1) track and white knob. On: violet track and white knob. Keep the knob position synchronized with state. |
| Chip/tag | Compact rounded pill; violet for “New”, gold with dark text for “Pro”; icon optional. |
| Bottom navigation | navBar rounded container; selected item has violet pill, other items use soft secondary icons/labels. caption labels and consistent icon sizes. |
| Waveform/progress | accentCyan signal bars on a dark background. Provide a text/state equivalent for recording or loading. |
| Divider | border color, 1 dp; align with the content it separates. |
| Radio | Unselected radioRing outline; selected primarySelected ring and filled center. |
| Permission dialog | Elevated rounded dark surface, short title and secondary explanation; subdued Cancel and gradient Allow; scrim behind. |
| Empty state | Centered violet illustration, concise primary title, secondary recovery hint or action. |

Exact heights, illustration sizes, and padding for these components are not
dimensioned in the board. **Implementation guidance:** default to 16 dp screen
gutters/card padding, 8–12 dp internal gaps, and 24 dp section gaps, using shared
tokens; adjust to the actual content instead of treating these defaults as measured
reference values. Interactive targets should be at least 48 × 48 dp. Account for
system bars, keyboard, localization, and font scaling. Do not rely on color alone
for status, or claim contrast compliance without checking the actual pairing.

## Android reuse example

### Language picker extension

The language reference uses pill rows: `language_row_height` 64 dp minimum,
`language_row_radius` 32 dp, 32 dp circular flags, 24 dp radio/expand indicators,
16 dp row padding, and 8 dp row gaps. Regional choices are indented 32 dp.
Rows grow for wrapped labels/font scaling. Use surface/border by default and
surfaceTint/primarySelected for the selected option. Group rows toggle expansion;
leaf rows select a locale. Apply the selected locale when Continue is pressed.
Flags retain national colors and are decorative; language names label each row.
This component is an approved pill-radius exception to the standard card radius.

### Onboarding extension

Three full-width photographic hero artworks introduce voice commands, camera
control, and voice notes/AI summaries. Artwork fades into bg at the lower edge;
headings and descriptions remain native, localized text below the image. Use
headline/Sora bold for centered headings and bodyL/Sora regular/textSecondary
for descriptions. Keep a bottom navigation row: three 8 dp dots at start (active
dot 20 dp wide in primaryLight), and a primaryLight outlined pill Next button at
end, minimum 48 dp touch height. Swipe and Next advance pages; Next on page three
finishes onboarding through the existing navigation flow. The outlined action
and pill shape match the approved onboarding reference.

### Home, settings, and notification extension

Home follows the approved reference composition: a violet gradient Unlock Screen
card, a surface Lock Screen card with primaryLight outline, large phone/lock/voice
illustrations, trailing command-edit controls, and labelled OFF/ON switches. Card
headers are 94 dp, illustrations 88 dp wide, status/example strips at least 64 dp,
and switches 80 × 48 dp with 28 dp knobs. Use successSoft for status text. The four
feature tiles use horizontal icon/title/description layouts, minimum 104 dp height,
12 dp padding, and 16 dp grid gaps. National/feature illustrations are decorative;
interactive controls retain clear accessible labels and state descriptions.

Settings uses a 138 dp minimum premium banner with existing theme previews at the
left and native title/description/Upgrade action at the right. Group General
settings and Other in rounded surface containers; each row is at least 68 dp with
a 24 dp primaryLight icon, bodyL title, bodyS secondary description, and chevron.
Keep Rate Us inside the Other group as the fourth row. Content scrolls as needed.

The home notification explanation is a bottom sheet: surfaceElevated, top radius
24 dp, centered overlapping 104 dp badge with a violet bell and error-colored dot,
titleXL heading, bodyL explanation, Later and Allow pills with 48 dp minimum touch
height, and 62% dimming behind. Show once on home on Android 13+ when permission
is missing and the explanation has not been answered. Later/cancellation records
the explanation as seen; Allow records it and opens Android's permission prompt.
The app cannot restyle the operating system permission prompt itself.

```xml
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    style="@style/Text.VoiceLock.titleM"
    android:fontFamily="@font/sora_bold"
    android:textColor="@color/textPrimary"
    android:layout_marginBottom="@dimen/spacing_sm" />
```

Reuse shared drawable and widget styles for shapes and states; avoid copying
complete component markup into each screen when a reusable layout/view suffices.

## Implementation status and future changes

The current color and size resources already contain most board tokens. The Sora
family bundles all four required weights. This definition does not imply every
existing screen conforms to the board.

Known gaps found while defining the system:

- `typography.xml` defines size and line height but not explicit font weights;
  consumers must currently select the correct font/weight themselves.
- `values-night/themes.xml` supplies a separate base theme with fewer semantic
  mappings than `values/themes.xml`; its navigation bar uses the screen background.
- Border widths, shadow levels, and icon sizes are specified here but are not yet
  a complete shared dimension set in `dimens.xml`.
- Existing extra colors/radii and literal values need screen-by-screen review
  before migration; do not assume they are approved system tokens.

For any new or updated UI, check palette, type role/weight, spacing, radius,
component states, 360/390/412 dp layouts, and font scaling. Changes to a shared
token require reviewing its consumers and updating this specification. Keep this
document and the reference board available as the project’s reusable design source.


## Typography implementation note

The app theme sets only `android:fontFamily` (Sora family). Do not add the AppCompat
`fontFamily` theme attribute: AppCompat gives it priority over a view's
`android:fontFamily`, which silently forces every `sora_semibold`/`sora_bold` back to
regular weight.
