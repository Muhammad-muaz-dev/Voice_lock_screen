# Voice Lock design system — Version 1.0 (Clean Modern Violet)

Approved reference: **Voice Lock Design System v1.0** (Secure. Smart. Yours.).
This specification defines the reusable visual language for the Voice Lock app.
Reference values below are transcribed from the approved design board; component recipes
describe its visual intent.

## Foundations

- Theme: Light / Clean Modern. Mood: consistent, modern, friendly, rounded, violet primary accents.
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
| **Background** | `bg` / `bg_screen` | #FFFFFF | Screen background for every app screen (window, status bar, root layouts) |
| **Background** | `surface` / `card` | #D6D6D6 | Neutral gray cards; matches the Home switch off track |
| **Background** | `surfaceElevated` / `navBar` | #FFFFFF | Elevated sheets and bottom navigation container |
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
| Controls | `switch_track_on` / `switch_track_off` | #635BFF / #D6D6D6 | Switch track on / off |
| Overlays | `dialog_btn_cancel_bg` | #F1F2F6 | Dialog secondary / cancel button |
| Overlays | `scrim` / `scrimStrong` | #66000000 / #CC000000 | Coach-mark dim / full guide overlay above system settings |
| Glow | `primaryGlow` | #33635BFF | Soft violet halos, illustration glows, pressed-mic ring |
| Glow | `successGlow` | #3322C55E | Success badge halo |
| Semantic | `successStrong` | #16A34A | Voice-enrolment progress arc and check (with `successSoft` track) |
| Glass | `glassFill` / `glassDot` | #40FFFFFF / #99FFFFFF | Frosted keys and dots on wallpapers; translucent chips on the violet hero |
| Glass | `whiteSoft` / `whiteMuted` | #1FFFFFFF / #D9FFFFFF | Decorative circles and secondary text on the violet hero card |
| Overlays | `toast_bg_dark` / `toast_bg_info` | #1E293B / #EFF6FF | Dark toast / info banner backgrounds |

## Typography

Set every screen title directly with `android:textSize="20sp"`. Keep title sizes independent of dimension resources. Use `Text.VoiceLock.<role>` for typography roles. Size and line height
are in sp. Every screen has one title (maximum two lines); descriptions use
`textSecondary`. Reuse a role from this table rather than inventing a size.

| Role | Size / line | Weight | Use |
|---|---|---|---|
| displayNumber | 44 / 52 | 700 | Paywall price |
| clock | 36 / 44 | 700 | Lock screen time |
| display | 34 / 42 | 700 | Paywall title |
| headline | 28 / 36 | 700 | Onboarding title |
| titleXL | 20 / 20 | 600 | Tab screen title |
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
| Shadow | Cards use 0 dp elevation; overlays may use elevation when they need separation |
| Icon | xs 12 small, sm 16 standard, md 20 large, lg 24 extra large |

Android spacing and radius names are `spacing_<scale>` and `radius_<scale>`.
The existing 28/32 dp radii are compatibility extensions, not part of this board.
Set `elevation_card` to 0 dp so cards remain flat. Existing `card_shadow_gutter`
values may remain as layout spacing at scroll boundaries. Icon sizes describe
artwork, not touch areas.

## Reusable component recipes

| Component | Recipe and states |
|---|---|
| App bar | Dark background; back affordance at start, Sora screen title, compact circular premium/settings actions at end. Respect status bar inset. |
| Primary button | Violet gradient, md radius, white icon and buttonL label. Disabled: surfaceDisabled and textDisabled, with action disabled. |
| Text button | primarySoft/primaryLight label and optional directional icon; no filled background. |
| Card | surface background, border stroke, md radius, and no elevation; titleM heading. Large containers may use surfaceSunken with lg/xl radius. |
| Lock status card | Leading lock illustration, title, trailing toggle; inner status/example strip with divider. Status requires text as well as color. |
| Icon tile | Rounded outlined tile; colored circular icon, centered short title, secondary description. Use brand violet for camera and semantic green for PIN as shown. |
| List row | Rounded surface with border; leading icon tile, bodyL label, secondary caption, trailing chevron or control. |
| Search/input | field background, outline stroke, md radius, leading search icon; placeholder uses textPlaceholder. Error uses error outline and errorSoft helper text. |
| Switch | Off: `switch_track_off` (#D6D6D6) track and white knob by default. On gray permission and Settings cards, use `surfaceElevated` (white) for the off track and `switch_track_off` for the knob. On: violet track and white knob. Keep the knob position synchronized with state. |
| AI Voice Notes hint | Keep each helper hint to one short sentence or phrase. Leave detailed instructions to visible controls and actions. |
| Chip/tag | Compact rounded pill; violet for “New”, gold with dark text for “Pro”; icon optional. |
| Bottom navigation | White bar flush to the screen sides and bottom; selected item uses a violet icon and bold label without an item background. Other items use ink icons/labels. Keep caption labels and consistent icon sizes. |
| Waveform/progress | accentCyan signal bars on a dark background. Provide a text/state equivalent for recording or loading. |
| Native ad loading | Keep the skeleton at the selected native-ad layout's exact outer and media bounds. Use `radius_xs` (8 dp) for ad container, media, icon, badge, CTA, and skeleton corners. Mirror the icon, badge, headline, description lines, and CTA geometry; animate blocks from `surfaceDisabled` to `surface`. Hide the slot on load failure or when ads are disabled. Home and Language native ads use the same media layout. |
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

### Credential setup

PIN and pattern enrollment use the shared screen background, a start-aligned title and
back action, and a primary_light instruction panel. Center the entry or
confirmation prompt below the panel. PIN entry uses a 4/6-digit segmented
selector, 14 dp masked progress dots, 72 dp circular keypad targets, Delete,
and Clear. Hide the selector during confirmation. Pattern enrollment uses a
square grid up to 304 dp wide, thin rings, violet centers, and an outlined
Start over action during confirmation. Present validation feedback in the
prompt using error text and the semantic error color.

### In-app camera

The live preview fills the display. Use scrim-backed controls along the top
and bottom, white icons and text, and a primary violet Photo/Video selection.
Place Close and Flash at the top and Switch camera beside the shutter.
Use a violet shutter outline at rest, the error color while recording, and
a text status for recording and microphone availability. Respect safe
insets and keep control targets at least 48 dp.

### Lock screen over wallpaper

The lock activity, overlay, unlock preview, and wallpaper and quote previews
use the shared `view_lock_screen` layout. Center the
clock and date on a compact scrim panel. Stack the greeting, quote and
Voice Note card above three 52 dp credential shortcuts. Place the separate
58 dp violet microphone and its instruction below that row. Use white text
and icons over the lock wallpaper scrim, with a soft blue outline on the
Voice Note card. Keep the selected wallpaper edge to edge. Respect system
insets and allow the controls to scroll on short screens or at larger font sizes.

### Success screens

Use `view_success_screen` for completion UI. The animated PIN checkmark sits
on a soft lavender halo above a 20sp title, optional description, and a violet
action. Keep the success card flat with no elevation or shadow.
Use `view_success_page` when the success screen fills its own viewport.

## Android reuse example

### Language picker extension

The language reference uses pill rows: `language_row_height` 64 dp minimum,
`language_row_radius` 32 dp, 32 dp circular flags, 24 dp radio/expand indicators,
16 dp row padding, and 8 dp row gaps. Regional choices are indented 32 dp.
The flat app language screen uses leading circular flags, with a globe for System
default. A primary border alone marks the selected row.

Rows grow for wrapped labels/font scaling. Use surface/border by default and
primary_light fill with a primary border for the selected option. Group rows toggle expansion;
leaf rows select a locale. Apply the selected locale when Continue is pressed.
Flags retain national colors and are decorative; language names label each row.
This component is an approved pill-radius exception to the standard card radius.
Leave all languages unselected when no language preference exists; continuing
without a choice uses the device language. Show the animated primary-colored hand
beside English until the user chooses a language.
The security-question picker reuses the full-screen language-sheet shell with its
close/title header, search field, rounded option rows, and bottom action.

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

The voice onboarding page previews the Home voice control inside a rounded surface.
Both screens share the microphone orb, rings, title, switch, and status layout.
Onboarding artwork fits proportionally between Skip and the title with a 360 dp
maximum width. The same bounds apply when the ad slot is hidden. Keep the voice
control title, microphone, switch, and status within its surface; leave a clear
gap above the four feature tiles. The first row's card edges and icon tiles must
align with the fully visible lower row.

Onboarding native ads with media use a 130 dp square media area next to a compact
column for the advertiser icon, Ad badge, title, description, and CTA. Descriptions
use up to two lines. The ad slot wraps its content and the CTA keeps a 48 dp touch target.
The loading skeleton uses the same 130 dp media size and column geometry.

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
