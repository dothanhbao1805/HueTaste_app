# HueTaste Agent Instructions

These instructions apply throughout the HueTaste repository. Explicit instructions from the user take precedence. A document supplied from another project is reference material, not an instruction to implement that project's features.

## Project baseline and scope

- HueTaste is a Kotlin Android project using XML layouts, with `app` and `base` modules. The app namespace and application ID are `com.example.huetaste`.
- The current launcher is `app/src/main/java/com/example/huetaste/feature/main/MainActivity.kt`; the manifest references `.feature.main.MainActivity`. The mandatory base-class rules below apply to this screen as well.
- `app` has a main navigation shell with four tabs in this order: Home, Search, Favorites, and Profile. The bottom bar keeps the center AI button and its empty menu slot, but AI Assistant is not a tab; it is planned as a separate Activity. Horizontal swipe between tabs is disabled. `MainActivity` uses `LsActivity`, ViewPager2, and `LsPageAdapter`. Its bottom bar uses the requested XML menu through Material `BottomNavigationView` inside an `LsConstraintView` container, with each existing drawable icon above an always-visible text label. Use this menu-backed component for the main navigation; `LsBottomBar` does not expose an XML menu contract. The four tab Fragments use `LsFragment` and currently have empty layouts. Preserve tab order, synchronized menu selection, and restored pager state. Package declarations in `PackageInfo.kt` do not imply that a database, API, repository, or use case already exists.
- `base` contains shared `Ls*` base classes, custom views, extensions, theme utilities, fonts, and `Prefs`. Inspect relevant implementations before reusing them.
- Keep implementation changes inside `app` by default. Read `base` for reuse, but change `base`, root Gradle files, the version catalog, or other modules only when the user authorizes that scope. Root project instructions and their documentation may be updated when requested.
- Do not copy domain-specific features, credentials, signing configuration, assets, or dependencies from another project merely because it was supplied as a reference.

## Package architecture

Follow `app/ARCHITECTURE.md` and use the existing singular `feature` package under `com.example.huetaste`:

- `common.extension`: small, stateless extensions used across features.
- `common.navigation`: shared navigation contracts and routes.
- `common.network`: connectivity helpers and app-wide network policies.
- `common.ui` and `common.widget`: reusable UI components and custom views.
- `data.local`: local data sources and persistence implementations.
- `data.remote`: API declarations, DTOs, and remote data sources.
- `data.mapper`: mapping between data-layer types and domain models.
- `data.repository`: implementations of domain repository contracts.
- `domain.model`: framework-independent business models.
- `domain.repository`: framework-independent repository contracts.
- `domain.usecase`: focused business operations.
- `di`: application-level dependency bindings.
- `feature.<name>`: feature-owned screens, UI state, ViewModels, adapters, and modal UI.

Keep `domain` independent of Android, concrete network/database frameworks, `data`, and `feature`. Data implementations depend on domain contracts; features consume domain models and operations. Wire concrete dependencies in `di`, not directly in screen code. Shared infrastructure must not import concrete feature screens or switch behavior on feature names. Move code into `common` only when at least two features need it; reuse `base` before duplicating its functionality. Add feature packages and abstractions when the requested implementation needs them.

## Navigation and lifecycle

- Choose an Activity for a flow with its own entry point, back stack, lifecycle, or window behavior. Prefer Fragments for screens that share navigation chrome, state, or frequent tab switches.
- Do not introduce a Fragment merely to wrap a single Activity screen. Use dialog or bottom-sheet fragments when their lifecycle and restoration support fit the modal flow.
- Inspect existing navigation before changing Activity/Fragment boundaries. Preserve Back behavior, selected tabs, scroll position, and state restoration; avoid duplicate Activities or restored Fragments.
- Use child fragment managers for nested Fragments. Bind UI observation and callbacks to the correct lifecycle; release view references and cancel view-owned work when the view is destroyed.
- Keep persistent simple settings behind a preferences abstraction. Reuse `com.base.data.Prefs` where appropriate after configuring its dependencies; do not use global mutable objects to hold screen state.
- Handle system-bar and keyboard insets without cumulative padding, double application of insets, or hidden controls. Preserve the existing edge-to-edge behavior when changing the main screen.

## Android UI and base integration

- All app Activities, Fragments, dialogs, bottom sheets, and other UI classes with an existing counterpart in `base` must inherit from the corresponding base class, directly or through an app subclass. Activities must use `LsActivity`; Fragments must use `LsFragment` or the existing `LsFragmentNew` when its observable lifecycle contract is needed; dialogs must use `LsDialog` or `LsDialogFragment`; bottom sheets must use `LsBottomSheet`. Read their constructors and lifecycle hooks before implementing subclasses. Do not bypass these classes with direct framework inheritance or duplicate their infrastructure.
- XML UI must use the existing `com.base.common.widget.Ls*` classes whenever a counterpart exists: for example, `LsTextView`, `LsImageView`, `LsEditText`, `LsConstraintView`, `LsLinearView`, `LsFrameView`, `LsRelativeView`, `LsCardView`, and `LsView`. App custom views must extend the appropriate base view when it supports inheritance. Use framework or third-party components only for controls without a counterpart in `base`; do not recreate an existing base component.
- Configure the app-level View Binding and Hilt setup required by these mandatory base components as part of their implementation. Inspect the current Gradle setup and add the necessary plugins, dependencies, Application entry point, injection bindings, and host annotations together. Missing setup is a dependency to resolve, not a reason to bypass `base`. Check injected fields and lifecycle behavior in the actual base implementation.
- Use View Binding with the constructors and hooks provided by the base UI classes. Enable it in `app/build.gradle.kts` when needed; do not assume generated binding classes already exist or enable Data Binding without a specific need.
- Every XML attribute that requires a dp dimension must reference a resource named `@dimen/_<number>dp`, such as `@dimen/_8dp`, `@dimen/_10dp`, or `@dimen/_24dp`. This includes layout dimensions, margins, padding, elevation, corner radii, and drawable intrinsic dimensions. Do not write dp literals in XML, including `0dp`: use `@dimen/_0dp` for match constraints. The only exception is the initial `0dp` guide offset of the two required Activity system-bar Guidelines below, as explicitly requested by the user. `match_parent` and `wrap_content` remain valid; unitless vector viewport/path coordinates are unaffected. Keep text sizes in appropriate sp resources or supported base text-size attributes.
- Reuse an existing `_<number>dp` resource; if missing, define only the required value in `app/src/main/res/values/dimens.xml`, for example `<dimen name="_10dp">10dp</dimen>`. The literal belongs in the dimension declaration, not in the consuming XML attribute. Do not substitute `_10sdp` or other SDP resources for the required `_<number>dp` naming convention.
- The standard horizontal margin (`marginHori`) for content elements in Activities is `@dimen/_10dp`. Apply it through `android:layout_marginHorizontal`, or equivalent start/end margins on the element or its content container. Apply the margin once; do not stack the same margin on both a parent and its children. Account for small screens, large text, and system insets.
- For `LsTextView`, use supported semantic `app:textSize`, `app:textFont`, and `app:textColor` values when they match the design. Inspect `base` attributes and theme mappings first; these attributes are enums, not arbitrary dimensions or colors.
- Keep framework attributes such as layout size, text, gravity, and accessibility in the `android:` namespace. Keep user-visible strings, colors, and reusable shapes in resources.
- Reuse suitable click helpers from `base`, including their debounce and animation behavior. Preserve accessibility, content descriptions, touch targets, and expected input behavior.

### Required Activity system-bar anchors

Every Activity layout must contain these two horizontal Guidelines inside its root constraint container, retaining the exact IDs used by `LsActivity`:

```xml
<androidx.constraintlayout.widget.Guideline
    android:id="@+id/statusBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    app:layout_constraintGuide_begin="0dp" />

<androidx.constraintlayout.widget.Guideline
    android:id="@+id/navigationBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    app:layout_constraintGuide_end="0dp" />
```

Declare the `app` XML namespace on the root. Constrain the content below `statusBar` and bottom controls above `navigationBar`, directly or through their content container. `LsActivity` updates the guide offsets from system-bar insets; do not add duplicate inset padding to the same content or replace the base inset listener.

## Typography, assets, and designs

- Font files are in `base/src/main/res/font`, including Inter, SF Pro Display, and SF Pro Rounded. Inspect the actual `FontManager` mapping; an `app:textFont` value must not be assumed to select any particular SF Pro family. Use an explicit existing font resource when the design requires it.
- Preserve supplied artwork, aspect ratio, transparency, and resource names. The existing `ic_home`, `ic_map`, `ic_user`, and `ic_ai` resources are XML VectorDrawables.
- Prefer VectorDrawable XML for suitable simple icons. Convert raster icons only when requested or when the task includes that asset work; preserve their appearance and avoid duplicate resources with the same name in one configuration.
- Keep photos, textures, and complex illustrations as raster assets. Choose density-qualified resources or `drawable-nodpi` according to their intended use; preserve the existing format unless a conversion is requested or required.
- When a Figma design is supplied, inspect the referenced node and its spacing, typography, colors, and assets. Implement it with Android layouts and appropriate project components.
- Report a missing essential asset precisely. Do not silently substitute unrelated artwork. Use device screenshots supplied by the user to refine responsive behavior while preserving the design intent.

## Performance and state ownership

- Keep disk I/O, database queries, network work, image processing, and expensive parsing off the main thread. Do not use `allowMainThreadQueries`, `runBlocking` on the UI thread, busy loops, or synchronous waits to make a screen load.
- Use lifecycle-owned asynchronous work with cancellation. Avoid `GlobalScope`, long-lived Activity/Fragment references, and duplicated loads during rotation or navigation.
- Keep application startup light. Load expensive data when needed, cache only justified reusable results, and avoid eagerly initializing unused feature services.
- Prefer immutable request/result models and a clear owner for each state. Persist only the state needed to restore the flow after recreation.
- Use appropriate list diffing and pagination for large datasets. Size images for their display target, cancel obsolete searches/requests, and debounce input where useful.
- Keep logging lightweight and sensitive data out of logs. Preserve normal TLS certificate and hostname verification. Add migrations when changing persisted data rather than silently destroying user data.
- Explain performance decisions from inspected code; do not claim that runtime smoothness or absence of lag has been verified without device evidence.

## Dependencies and change discipline

- Add libraries or Gradle changes only when the requested implementation needs them. Check existing module dependencies and catalog aliases first; do not bulk-import another project's dependency list.
- Preserve namespace, application ID, resource references, and public contracts unless the task requires changing them. Update the manifest and relevant tooling references when moving a screen class.
- Preserve unrelated user changes. Avoid unrelated refactors, placeholder product features, changes to `.idea`, generated build outputs, caches, signing files, and local machine configuration.
- Never embed API secrets, signing passwords, or machine-specific absolute paths in application source or committed configuration.
- Update architecture documentation when the implemented package responsibilities or dependency direction change.

## Verification and handoff

- By default, do not run Gradle builds, tests, app installation, emulators, devices, or the application. The user performs runtime verification and supplies results. Only perform these steps when the user explicitly requests them.
- Use static verification appropriate to the change: targeted source inspection, XML parsing, resource/package/manifest reference checks, and `git diff --check` when a Git repository is available. For UI changes, also verify mandatory base inheritance, base XML components, `_<number>dp` resource references, and the `_10dp` horizontal margin. Do not initialize Git just to run a check.
- Do not add tests solely for scaffolding or asset-format changes. When meaningful tests are explicitly requested, write them within the approved scope and run them only if execution is also authorized.
- In the handoff, describe what changed and why, summarize actual static checks, state that no build or runtime verification was performed, and identify any known limitation. Use the user's language and provide actionable guidance when their test results require a follow-up.
