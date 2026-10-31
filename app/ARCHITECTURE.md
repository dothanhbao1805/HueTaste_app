# App package structure

The `app` module uses feature-oriented presentation code on top of clean data and domain layers.

```text
com.example.huetaste
|-- common
|   |-- App            Hilt application extending LsApplication
|   |-- extension      Small stateless extensions shared by multiple features
|   |-- navigation     App routes and navigation contracts
|   |-- network        Connectivity helpers and app-wide network policies
|   |-- ui             Reusable UI components
|   `-- widget         Reusable custom Android views
|-- data
|   |-- local          Local data sources and persistence implementations
|   |-- mapper         Data-to-domain mapping
|   |-- remote         APIs, DTOs, and remote data sources
|   `-- repository     Domain repository implementations
|-- di                 Application-level dependency bindings
|-- domain
|   |-- model          Framework-independent business models
|   |-- repository     Repository contracts
|   `-- usecase        Business operations
`-- feature
    |-- main           LsActivity entry point, ViewPager2, and main tab definitions
    |-- home           Home fragment extending LsFragment (static Stitch-based UI)
    |-- context_suggestion  Context suggestion Activity extending LsActivity (static Stitch-based UI)
    |-- dish_detail    Dish detail Activity extending LsActivity (static Stitch-based UI)
    |-- search         Empty Search fragment extending LsFragment
    |-- favorites      Empty Favorites fragment extending LsFragment
    `-- profile        Empty Profile fragment extending LsFragment
```

## Dependency direction

- `domain` must not import Android UI, network, database, or `data` types.
- `data` may depend on `domain`, never the other way around.
- A `feature` consumes domain contracts/use cases and owns its screen-specific UI state.
- Move code to `common` only when it is used by at least two features.
- Add a feature package only when that feature is implemented; keep its screen, adapter, and view-model code together.

This structure deliberately does not copy plant-specific packages or dependencies from the reference project.

## Main navigation shell

`MainActivity` uses `LsPageAdapter` from `base`. Its bottom bar is an `LsConstraintView` container holding a Material `BottomNavigationView`, which provides the requested XML menu and always-visible labels beneath the icons. `menu_main_bottom_navigation.xml` defines the five menu items (Home, Search, an empty AI slot under the center AI button, Favorites, Profile, matching the Stitch design); `MainTab` maps the Home, Search, Favorites, and Profile menu IDs to their fragments in the same order. The AI slot and center button do not select a page; AI Assistant is planned as its own Activity. ViewPager2 swipe input is disabled, so tabs change only through the bottom navigation. ViewPager2 owns page restoration; menu selection and page callbacks keep the navigation synchronized. All four pages are retained for tab switching; Home is the only page with content. Revisit the retention policy as more pages acquire substantial content.

The Activity layout uses the `statusBar` and `navigationBar` Guidelines expected by `LsActivity`; the base class owns system-bar inset updates. The ViewPager2 carries the single 10dp horizontal margin, so tab layouts must not add it again. Search, Favorites, and Profile own empty `LsConstraintView` layouts. Home owns a scrolling layout built from `Ls*` views with colors from the Stitch palette (`hue_*` in `colors.xml`); its photos are placeholders until image assets and an image-loading approach are supplied.

## Context suggestion screen

`ContextSuggestionActivity` implements the Stitch "Gợi Ý Theo Ngữ Cảnh" screen as its own Activity, because it is a separate flow with its own back stack rather than a main tab; it has a back button instead of the main bottom bar. Its layout is static sample content built from `Ls*` views, with the 10dp horizontal margin applied once per section. The Activity only keeps the dishes/places selection and favorite hearts across recreation; Home's "Gợi ý món ăn ngay" button (`suggestButton`) opens it through `newIntent()`.

## Dish detail screen

`DishDetailActivity` implements the Stitch "Chi Tiết Món Ăn - Bún Bò Huế" screen as its own Activity, opened from the context suggestion screen's "Xem chi tiết món & cách ăn" button (`dishDetailsButton`). Its layout is static sample content: hero photo, name and price, story, ingredients, places, reviews, and a food-assistant prompt, with a fixed bottom action bar whose background reaches the screen edge while its content stays above the `navigationBar` guide. The Activity keeps one favorite state shared by the header and hero hearts and shares the dish text through the system share sheet.

## Shared shapes

`bg_shape_pill`, `bg_shape_circle`, and `bg_shape_round_4/6/8/12` are white shapes that screens tint per Stitch token with `android:backgroundTint`, so each colour does not need its own drawable. They are used by both the context suggestion and dish detail screens.

`common.App` supplies the Hilt application entry point and uses base preferences for the initial theme. `di.AppModule` provides the application context and preferences infrastructure required by the injected base classes. View Binding and Hilt are configured in the `app` module.
