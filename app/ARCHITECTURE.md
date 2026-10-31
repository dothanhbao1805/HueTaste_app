# App package structure

The `app` module uses feature-oriented presentation code on top of clean data and domain layers.

```text
com.example.huetaste
|-- common
|   |-- App            Hilt application extending LsApplication
|   |-- extension      Small stateless extensions shared by multiple features (openMap)
|   |-- navigation     App routes and navigation contracts
|   |-- network        Connectivity helpers and app-wide network policies
|   |-- ui             Reusable UI components (gallery/: photo carousel adapter, full-screen viewer, zoom)
|   `-- widget         Reusable custom Android views
|-- data
|   |-- local          Local data sources and persistence implementations
|   |-- mapper         Data-to-domain mapping (scan and dish DTOs)
|   |-- remote         Retrofit HueTasteApi, DTOs, and apiCall error mapping
|   `-- repository     ScanRepositoryImpl, DishRepositoryImpl
|-- di                 AppModule, NetworkModule (OkHttp/Retrofit), RepositoryModule
|-- domain
|   |-- model          ScanResult, DishDetail, PlaceDetail, Review, OpeningHours, Place, AppError
|   |-- repository     ScanRepository, DishRepository
|   `-- usecase        ScanDish, GetScanResult, GetDishDetail, GetPlaceDetail, GetPlaceReviews, SubmitPlaceReview, SetPlaceFavorite
`-- feature
    |-- main           LsActivity entry point, ViewPager2, and main tab definitions
    |-- home           Home fragment extending LsFragment (static Stitch-based UI)
    |-- context_suggestion  Context suggestion Activity extending LsActivity (static Stitch-based UI)
    |-- dish_detail    Dish detail Activity + ViewModel (backend data, or the static Stitch sample) with a hero photo carousel
    |-- place_detail   Place detail Activity + ViewModel + review sheet (backend data), opened from a place in the dish detail
    |-- scan           Camera scan Activity, ViewModel, image processing, tips sheet, message dialog
    |-- search         Empty Search fragment extending LsFragment
    |-- favorites      Empty Favorites fragment extending LsFragment
    `-- profile        Empty Profile fragment extending LsFragment
```


## Place detail

`PlaceDetailActivity` implements the Stitch "Chi Tiết Quán Ăn" screen and is opened with `newIntent(placeId)`. `PlaceDetailViewModel` loads `GET /dia-diem/{id}` and the first page of `GET /dia-diem/{id}/danh-gia` in parallel through `PlaceRepository`; the place still shows when the reviews fail to load. The hero reuses `common/ui/gallery`, with the backend cover photo put first because the backend gallery leaves it out. Today's opening hours and the open or closed badge come from `OpeningHours`, a framework-independent parser of the backend `gioMoCua` lines (Vietnamese or English day names, several ranges per day); the badge is hidden when the text cannot be read. Parts of the design the backend has no data for (amenities, heritage badges, peak hours, rating breakdown, distance, reservations) are not built. The heart in the header and the "Lưu quán" action share one favorite state saved with `POST`/`DELETE /users/me/yeu-thich`, applied immediately and reverted when the request fails. `ReviewSheet` (an `LsBottomSheet`) posts `POST /danh-gia`; it shares the Activity's ViewModel, which keeps the submit progress, and the first page of reviews is reloaded after a successful post. The backend currently skips token checks in debug mode and the app has no sign-in yet, so favorites and reviews work against that debug backend only.

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

## Backend access

`NetworkModule` builds one OkHttp/Retrofit client for the HueTaste FastAPI backend. Its base URL comes from `BuildConfig.API_BASE_URL`, read from `huetaste.apiBaseUrl` in the untracked `local.properties` and defaulting to `http://10.0.2.2:8000/api/v1/` (the emulator's host). Debug builds allow cleartext HTTP through `src/debug/AndroidManifest.xml`. The backend currently runs without authentication, so no token is sent. `apiCall` turns HTTP failures with the backend `{code, message}` body and I/O failures into the domain `AppError`; repositories run on the injected IO dispatcher.

## Scan screen

`ScanActivity` follows the reference project's scan layout: a CameraX preview, the corner frame, a Lottie scan line that plays while identifying, tips, shutter, torch, zoom, and a gallery picker (the reference's AR mode and type tabs are not used). Home's "Quét món" button opens it. Camera frames are rotated, cropped to the on-screen frame, and saved as JPEG of at most 1280 px by `ScanImageProcessor` off the main thread; gallery picks are decoded through Coil. `ScanViewModel` owns the flow: it uploads to `POST /scan`, polls `GET /scan/{id}` while the status is `DANG_CAP_NHAT`, and then opens the dish screen with the scan id. A pending scan id survives process death so polling resumes without another upload. Permission and error prompts use `ScanMessageDialog` (an `LsDialog`) with fragment results, and the tips use `ScanTipsSheet` (an `LsBottomSheet`).

## Dish detail screen

`DishDetailActivity` implements the Stitch "Chi Tiết Món Ăn - Bún Bò Huế" screen as its own Activity, opened from the context suggestion screen's "Xem chi tiết món & cách ăn" button (`dishDetailsButton`). Its layout holds the sample content: hero photo, name and price, story, ingredients, places, reviews, and a food-assistant prompt, with a fixed bottom action bar whose background reaches the screen edge while its content stays above the `navigationBar` guide. Opened with `newScanIntent(scanId)` or `newIntent(dishId)`, `DishDetailViewModel` loads `GET /scan/{id}` or `GET /mon-an/{id}` and the Activity replaces the sample with backend data: pills, ingredients, places, and similar dishes are inflated from `item_dish_*` layouts. Dishes that are not verified or not Huế specialties show a status banner, the scanned photo, the recognised name, and similar dishes. Rating, nutrition, reviews, and the assistant prompt have no backend data yet and are hidden in that mode; the favorite heart starts from `daYeuThich` but is not saved to the backend. Opened with `newIntent()` alone (the context suggestion screen), it keeps the sample. The Activity keeps one favorite state shared by the header and hero hearts and shares the dish text through the system share sheet. The hero is a `ViewPager2` (`heroPager`, `GalleryPagerAdapter`) over the backend gallery `anh[]` mapped to `DishDetail.images`; it falls back to the cover `hinhAnh`, then to the scanned photo, and shows a page counter when there is more than one photo. Its carousel and photo viewer live in `common/ui/gallery/` because the place detail screen uses them too: `GalleryPagerAdapter` for the pages, and `ImageViewerDialog` (an `LsDialogFragment` with a `ViewPager2` of `ImageViewerAdapter` pages) for the full-screen view, which reports its last page back through the fragment result API so the hero follows. Zoom (pinch, pan, double-tap) is `ImageZoomController`, attached by composition because `LsImageView` is final; it hands horizontal swipes back to the pager once the image is panned to an edge. Each place in the places section opens `PlaceDetailActivity`.

## Shared shapes

`bg_shape_pill`, `bg_shape_circle`, and `bg_shape_round_4/6/8/12` are white shapes that screens tint per Stitch token with `android:backgroundTint`, so each colour does not need its own drawable. They are used by both the context suggestion and dish detail screens.

`common.App` supplies the Hilt application entry point and uses base preferences for the initial theme. `di.AppModule` provides the application context and preferences infrastructure required by the injected base classes. View Binding and Hilt are configured in the `app` module.
