# Home, settings, and notification redesign

Implemented the supplied reference composition using Midnight Violet tokens:
gradient unlock card, outlined lock card, large phone/lock illustrations, editable
voice-command controls, labelled switches, status/example strips, a horizontal
four-tile feature grid, and three-tab navigation. Existing feature destinations
and permission gates remain connected. Command-edit controls open the existing
voice setup/guide rather than deleting a saved command.

Settings uses a native premium banner with the app's existing theme images, two
rounded settings groups, and Rate Us as the fourth Other row. Existing premium,
language, recovery, system settings, share, feedback, privacy, and store actions
are retained.

The home notification explanation is now a reusable BottomSheetDialogFragment.
Home shows it once on Android 13+ when notifications are not permitted and the
explanation has not been answered. Later and cancellation record it as seen.
Allow opens the operating system permission request. No permission is granted
by displaying or accepting the app's explanation alone.

Validation:

- Debug APK and instrumented-test APK compiled successfully.
- `HomeSettingsDesignTest` passed on Pixel 6a: settings navigation/back, automatic
  notification explanation when needed, Later dismissal, and no repeat prompt
  after resuming. The test restores the original notification-explanation flag.
- Production home and settings layouts measured successfully at 360, 390, and
  412 dp widths with 1.3 font scale; visible text was checked for vertical clipping.
- Final native view captures were visually inspected. Home remains scrollable on
  shorter screens; settings rows grow or scroll with text size/localization.
- Full lint remains blocked by existing project errors, including the German
  yearly-save format string. It is not a passing project-wide validation.

Previews in `artifacts/design-preview/`:

- `final_home_midnight.png`
- `final_settings_midnight.png`
- `notification_home_finished.png`
