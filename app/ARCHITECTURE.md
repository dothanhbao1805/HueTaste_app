# App package structure

The `app` module uses feature-oriented presentation code on top of clean data and domain layers.

```text
com.example.huetaste
|-- common
|   |-- App            Hilt application extending LsApplication
|   |-- extension      Small stateless extensions shared by multiple features (openMap; ActivityExt = the single entry point for starting Activities, with the right-to-left transition)
|   |-- navigation     App routes and navigation contracts
|   |-- network        Connectivity helpers and app-wide network policies
|   |-- ui             Reusable UI components (gallery/: photo carousel adapter, full-screen viewer, zoom)
|   `-- widget         Reusable custom Android views
|-- data
|   |-- local          Local data sources and persistence implementations (SecureSessionStore, TasteSetupStore, DeviceLocationProvider)
|   |-- mapper         Data-to-domain mapping (scan, dish, place, auth, preference and chat DTOs)
|   |-- remote         Retrofit HueTasteApi, DTOs, and apiCall error mapping
|   `-- repository     ScanRepositoryImpl, DishRepositoryImpl, PlaceRepositoryImpl, AuthRepositoryImpl, PreferenceRepositoryImpl, ChatRepositoryImpl
|-- di                 AppModule, NetworkModule (OkHttp/Retrofit), RepositoryModule
|-- domain
|   |-- model          ScanResult, DishDetail, PlaceDetail, Review, OpeningHours, AuthSession, AuthState, AuthValidator, Place, AppError, DietOption, TasteTag, TastePreference, Ingredient, MealType, MealBudget, DailyNeed, ChatConversation, ChatMessage, ChatReply, GeoPoint
|   |-- repository     ScanRepository, DishRepository, PlaceRepository, AuthRepository, PreferenceRepository, ChatRepository, LocationProvider
|   `-- usecase        ScanDish, GetScanResult, GetDishDetail, GetPlaceDetail, GetPlaceReviews, SubmitPlaceReview, SetPlaceFavorite, Login, Register, ContinueAsGuest, GetAuthState, GetTasteSetupOptions, SearchIngredients, SaveDiets, SaveTastes, SaveDailyNeeds, ShouldShowTasteSetup, MarkTasteSetupHandled, SendChatMessage, GetConversations, GetConversationMessages, DeleteConversation, GetCurrentLocation
`-- feature
    |-- splash         Launcher Activity: reads the remembered session and opens the main screen or the login
    |-- auth           Login and Register Activities with their ViewModels
    |-- taste_setup    Taste setup Activity after sign-up: diet, flavour and today's-needs pages plus a summary, sharing one ViewModel
    |-- main           LsActivity for the main shell, ViewPager2, and main tab definitions
    |-- home           Home fragment extending LsFragment (static Stitch-based UI)
    |-- context_suggestion  Context suggestion Activity extending LsActivity (static Stitch-based UI)
    |-- dish_detail    Dish detail Activity + ViewModel (backend data, or the static Stitch sample) with a hero photo carousel
    |-- place_detail   Place detail Activity + ViewModel + review sheet (backend data), opened from a place in the dish detail
    |-- scan           Camera scan Activity, ViewModel, image processing, tips sheet, message dialog
    |-- ai_chat        AI assistant chat and conversation history Activities, ViewModels, adapters and delete dialog
    |-- search         Empty Search fragment extending LsFragment
    |-- favorites      Empty Favorites fragment extending LsFragment
    `-- profile        Empty Profile fragment extending LsFragment
```


## Place detail

`PlaceDetailActivity` implements the Stitch "Chi Tiết Quán Ăn" screen and is opened with `newIntent(placeId)`. `PlaceDetailViewModel` loads `GET /dia-diem/{id}` and the first page of `GET /dia-diem/{id}/danh-gia` in parallel through `PlaceRepository`; the place still shows when the reviews fail to load. The hero reuses `common/ui/gallery`, with the backend cover photo put first because the backend gallery leaves it out. Today's opening hours and the open or closed badge come from `OpeningHours`, a framework-independent parser of the backend `gioMoCua` lines (Vietnamese or English day names, several ranges per day); the badge is hidden when the text cannot be read. Parts of the design the backend has no data for (amenities, heritage badges, peak hours, rating breakdown, distance, reservations) are not built. The heart in the header and the "Lưu quán" action share one favorite state saved with `POST`/`DELETE /users/me/yeu-thich`, applied immediately and reverted when the request fails. `ReviewSheet` (an `LsBottomSheet`) posts `POST /danh-gia`; it shares the Activity's ViewModel, which keeps the submit progress, and the first page of reviews is reloaded after a successful post. Favorites and reviews send the signed-in user's token; guests have none.

## Launch and sign-in

`SplashActivity` is the only launcher. It reads the remembered session (`GetAuthStateUseCase`) while the brand stays on screen for a short minimum time, then opens `MainActivity` for a signed-in user or a returning guest, and `LoginActivity` otherwise, so a user who signed in once never sees the login again. Both targets are opened with `NEW_TASK | CLEAR_TASK` through `newRootIntent`, so Back never returns to the splash or the login. `MainActivity` leaves for the login if the session ends while the app is open (the backend rejects the refresh token, or the account is blocked) and checks again whenever it starts.

`LoginActivity` and `RegisterActivity` validate with the pure `AuthValidator` before calling the backend, show field errors under each input, and map backend error codes (`INVALID_CREDENTIALS`, `EMAIL_EXISTS`, network failures) to Vietnamese messages. "Tiếp tục không cần tài khoản" stores a guest flag so the next launch also goes straight to the main screen. "Quên mật khẩu?" and the terms links only announce that they are coming, because the design has no screens for them yet.

The session lives in `SecureSessionStore`: the access and refresh tokens are encrypted with AES-GCM under a key held in the Android Keystore and written to their own preferences file (`auth_session`), which both backup rule files exclude. If the key is lost the store signs the user out instead of failing. `AuthInterceptor` adds the access token to backend requests; `TokenAuthenticator` answers a 401 by exchanging the refresh token once (serialized so parallel requests refresh only once) and repeating the request, and clears the session when the backend rejects the refresh token. The sign-in endpoints use a separate client without these two, so a refresh can never trigger another refresh. Guests send no token. The backend currently skips token checks in debug mode; once it checks them again, scan, favorites and reviews need a signed-in user.

## Taste setup

`TasteSetupActivity` follows the Stitch "Thiết Lập Khẩu Vị" and "Hoàn Tất Thiết Lập" screens, reduced to what the backend stores. `RegisterActivity` always opens it after a sign-up; `LoginActivity` opens it when `ShouldShowTasteSetupUseCase` finds no local "handled" flag for the user and both `GET /users/me/che-do-an` and `GET /users/me/so-thich` are empty (a failed check goes to the main screen). The backend has no onboarding flag, so `TasteSetupStore` remembers per user id on this device that the setup was finished or skipped.

The Activity hosts a ViewPager2 (`LsPageAdapter`, swipe disabled) of four `LsFragment` pages that share the Activity's `TasteSetupViewModel` (as `ReviewSheet` does); the header shows the step and "Bỏ qua", the footer the main and secondary buttons. Each "Tiếp tục" saves its page before moving on: diets with `PUT /users/me/che-do-an` (none selected means "Ăn bình thường"), liked flavours and dish styles with `PUT /users/me/so-thich` (level 4, with spiciness mapped to the level of the backend "Cay" flavour), and today's meals, budget and ingredients to avoid with one `PUT /users/me/nhu-cau` per meal (no `ngay`, so the backend uses today). Catalogs come from `GET /che-do-an`, `GET /so-thich` and a debounced `GET /nguyen-lieu?q=` that starts only on the third page. Choices and the current page live in `SavedStateHandle`. Parts of the design the backend cannot store (fast-day calendar and reminder, calorie target, dining companions) are not built. Back steps through the pages; on the first page it skips like "Bỏ qua".

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

## AI assistant

`AiChatActivity` implements the Stitch "Trợ Lý AI Ẩm Thực" screen and `ChatHistoryActivity` "Trợ Lý AI - Lịch Sử Trò Chuyện", both backed by the backend `/tro-chuyen` endpoints through `ChatRepository`. The center AI button of the main bottom bar always opens a new conversation, which is only created on the backend (`POST /tro-chuyen`) when its first question is sent; `SendChatMessageUseCase` also creates a new one and asks again once when the open conversation was deleted meanwhile (`CUOC_TRO_CHUYEN_NOT_FOUND`). The history reopens a conversation with `startAiChatActivity(id)`, which brings the existing chat back (`CLEAR_TOP | SINGLE_TOP`) and hands it the id through `onNewIntent`.

`AiChatViewModel` owns the rows; the conversation id, the location choice and a failed question are kept in `SavedStateHandle`. A question shows at once as a dimmed bubble with the typing bubble (the backend takes 3-8 s); the reply (`POST /tro-chuyen/{id}/tin-nhan`) replaces it with the stored question, the answer, one sideways row of dish cards (`monAn[]`) and one card per place (`diaDiem[]`). Failures become an error bubble with "Thử lại", which sends the same question again (the backend stores a question before asking Gemini, so a retried question can appear twice in the stored history). A stored conversation is read from every page of `GET /tro-chuyen/{id}/tin-nhan` and rebuilt by `ChatItemsBuilder`: consecutive dish suggestions form one row, place suggestions one card each, with a header whenever the day changes. Only suggestions with a backend `GoiY` record are stored, so a reopened conversation can show fewer cards than it did live. Cards open the dish or place detail; "Chỉ đường" uses `openMap`.

The backend only answers signed-in users, so guests (and users whose session ended) see a sign-in bar instead of the composer, leading to the login. The location chip asks for `ACCESS_COARSE_LOCATION`; while it is on, each question carries `viDo`/`kinhDo` from `DeviceLocationProvider`, which uses the platform `LocationManager` (a recent cached network/passive fix, otherwise one network fix within 5 s) and sends none when it finds nothing. `ChatHistoryViewModel` pages through `GET /tro-chuyen` (20 per page, newest first, more when the list nears its end) and groups rows into "Hôm nay", "Hôm qua", "Tuần trước" or their date; deleting removes the row at once and puts it back if `DELETE /tro-chuyen/{id}` fails. Parts of the design the backend has no data for are not built: read receipts, the tip line, review counts, a dish inside the place card, voice input, photo attachments, and search or tags in the history.

## Backend access

`NetworkModule` builds one OkHttp/Retrofit client for the HueTaste FastAPI backend. Its base URL comes from `BuildConfig.API_BASE_URL`, read from `huetaste.apiBaseUrl` in the untracked `local.properties` and defaulting to `http://10.0.2.2:8000/api/v1/` (the emulator's host). Debug builds allow cleartext HTTP through `src/debug/AndroidManifest.xml`. `AuthInterceptor` adds the signed-in user's token (see Launch and sign-in). `apiCall` turns HTTP failures with the backend `{code, message}` body and I/O failures into the domain `AppError`; repositories run on the injected IO dispatcher.

## Scan screen

`ScanActivity` follows the reference project's scan layout: a CameraX preview, the corner frame, a Lottie scan line that plays while identifying, tips, shutter, torch, zoom, and a gallery picker (the reference's AR mode and type tabs are not used). Home's "Quét món" button opens it. Camera frames are rotated, cropped to the on-screen frame, and saved as JPEG of at most 1280 px by `ScanImageProcessor` off the main thread; gallery picks are decoded through Coil. `ScanViewModel` owns the flow: it uploads to `POST /scan`, polls `GET /scan/{id}` while the status is `DANG_CAP_NHAT`, and then opens the dish screen with the scan id. A pending scan id survives process death so polling resumes without another upload. Permission and error prompts use `ScanMessageDialog` (an `LsDialog`) with fragment results, and the tips use `ScanTipsSheet` (an `LsBottomSheet`).

## Dish detail screen

`DishDetailActivity` implements the Stitch "Chi Tiết Món Ăn - Bún Bò Huế" screen as its own Activity, opened from the context suggestion screen's "Xem chi tiết món & cách ăn" button (`dishDetailsButton`). Its layout holds the sample content: hero photo, name and price, story, ingredients, places, reviews, and a food-assistant prompt, with a fixed bottom action bar whose background reaches the screen edge while its content stays above the `navigationBar` guide. Opened with `newScanIntent(scanId)` or `newIntent(dishId)`, `DishDetailViewModel` loads `GET /scan/{id}` or `GET /mon-an/{id}` and the Activity replaces the sample with backend data: pills, ingredients, places, and similar dishes are inflated from `item_dish_*` layouts. Dishes that are not verified or not Huế specialties show a status banner, the scanned photo, the recognised name, and similar dishes. Rating, nutrition, reviews, and the assistant prompt have no backend data yet and are hidden in that mode; the favorite heart starts from `daYeuThich` but is not saved to the backend. Opened with `newIntent()` alone (the context suggestion screen), it keeps the sample. The Activity keeps one favorite state shared by the header and hero hearts and shares the dish text through the system share sheet. The hero is a `ViewPager2` (`heroPager`, `GalleryPagerAdapter`) over the backend gallery `anh[]` mapped to `DishDetail.images`; it falls back to the cover `hinhAnh`, then to the scanned photo, and shows a page counter when there is more than one photo. Its carousel and photo viewer live in `common/ui/gallery/` because the place detail screen uses them too: `GalleryPagerAdapter` for the pages, and `ImageViewerDialog` (an `LsDialogFragment` with a `ViewPager2` of `ImageViewerAdapter` pages) for the full-screen view, which reports its last page back through the fragment result API so the hero follows. Zoom (pinch, pan, double-tap) is `ImageZoomController`, attached by composition because `LsImageView` is final; it hands horizontal swipes back to the pager once the image is panned to an edge. Each place in the places section opens `PlaceDetailActivity`.

## Shared shapes

`bg_shape_pill`, `bg_shape_circle`, and `bg_shape_round_4/6/8/12` are white shapes that screens tint per Stitch token with `android:backgroundTint`, so each colour does not need its own drawable. They are used by both the context suggestion and dish detail screens.

`common.App` supplies the Hilt application entry point and uses base preferences for the initial theme. `di.AppModule` provides the application context and preferences infrastructure required by the injected base classes. View Binding and Hilt are configured in the `app` module.
