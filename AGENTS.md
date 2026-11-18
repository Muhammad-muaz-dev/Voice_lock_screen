# Voice Lock UI design rules

For all app UI work, read `docs/design-system.md` before designing or editing screens.
The approved visual system is **Voice Lock Design System v1.0** (Clean Modern Violet),
documented in `docs/design-system.md`.

## Layout and code structure

- Use ConstraintLayout for screen layouts and component positioning. Define clear
  horizontal and vertical constraints for every child; avoid nested LinearLayouts
  and weight-based positioning. Scroll views, RecyclerViews, and required widget
  containers may wrap constraint-based content as appropriate.
- Write readable XML: consistent indentation, one attribute per line, descriptive
  IDs, and logical grouping of related views. Do not compress layouts into long lines.
- Write structured Kotlin with focused methods for setup, event handling, state
  rendering, and navigation. Keep responsibilities clear and avoid duplicated logic.
- Apply these requirements whenever creating or modifying UI code.

- Reuse the semantic colors in `app/src/main/res/values/colors.xml`, spacing and
  radius tokens in `dimens.xml`, and `Text.VoiceLock.*` styles in `typography.xml`.
- Use Sora and the documented type size, line height, and weight for each role.
- Use the documented component recipes and states; extend existing components
  before introducing a new visual treatment.
- Use `@color/surface`, `@dimen/spacing_lg`, and `@dimen/radius_md` instead of
  duplicating their literal values in new layouts and drawables.
- Treat legacy palette entries and extra radii as compatibility values, not new
  design choices. Preserve existing behavior when migrating a screen.
- Keep layouts usable at 360–412 dp width, with system insets, font scaling,
  accessible touch targets, and clear selected/disabled/error states.
- When intentionally extending the system, update `docs/design-system.md` and
  the shared resources together. Do not silently invent per-screen tokens.
