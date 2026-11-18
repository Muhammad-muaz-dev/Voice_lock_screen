# Onboarding artwork

Generated with the built-in imagegen tool from the three user-supplied onboarding
composition references. Final assets are in `app/src/main/res/drawable-nodpi/`:

- `onboarding_voice_midnight.png`
- `onboarding_camera_midnight.png`
- `onboarding_notes_midnight.png`

Shared prompt: production portrait 2:3 hero artwork only; reproduce the reference
composition, omit native page heading/description/pagination/Next, replace white
backgrounds and bubbles with Midnight Violet (#0B1020 background, #151B33/#18203F
surfaces, #5B3DF5/#7B2FD6 violet, #C4B5FF soft violet, restrained #22D3EE cyan).
Use realistic devices/photos; no surrounding card or watermark; fade the bottom
12% into #0B1020. Preserve natural photography colors.

Voice prompt: close-up black phone with violet dune wallpaper, luminous open lock,
violet/cyan waveform, “Hey! Lock my phone...” bubble, “200+ languages” chip, and
overlapping glowing circular microphone control.

Camera prompt: black phone showing an alpine meadow and a woman in a summer dress,
with “Record Video”, “Take Photo”, and “Open Camera” dark speech bubbles, shutter
circle and Photo/Video selector, following the reference positions.

Notes prompt: tilted six-person video meeting grid with Live badge, layered black
phone showing dark transcript bubbles and speaker avatars. Bubble text: “Are we
still meeting at 3 PM?” and “Yes, let’s discuss the marketing plan.”

These are adapted generated illustrations rather than identical reference pixels.
The headings and descriptions are rendered separately by Android for localization.

Validation: debug APK and instrumented-test APK compile successfully. The
`IntroDesignTest` device test passed on Pixel 6a, exercising group expansion,
regional selection, collapse, Next page advancement, and image/text layout bounds
on all three pages. Final app-view captures are in `artifacts/design-preview/`
with the `final_` prefix. The preview test temporarily shows its activity above
the device lock screen and does not unlock the phone or clear saved app data.
Full lint still reports existing project errors (including the German yearly-save
format string); it is not a passing project-wide validation.
