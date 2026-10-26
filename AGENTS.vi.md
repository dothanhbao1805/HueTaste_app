# Hướng dẫn dành cho agent trong HueTaste

Đây là bản dịch tiếng Việt của `AGENTS.md` để chủ dự án tham khảo. File chỉ dẫn chính dành cho agent là `AGENTS.md`.

Các chỉ dẫn áp dụng cho toàn bộ repository HueTaste. Yêu cầu rõ ràng của người dùng được ưu tiên. Tài liệu từ dự án khác là nguồn tham khảo, không phải yêu cầu triển khai các tính năng của dự án đó.

## Hiện trạng và phạm vi dự án

- HueTaste là dự án Android viết bằng Kotlin, sử dụng layout XML và có hai module `app`, `base`. Namespace và application ID là `com.example.huetaste`.
- Màn hình khởi chạy hiện tại nằm ở `app/src/main/java/com/example/huetaste/feature/main/MainActivity.kt`; manifest tham chiếu `.feature.main.MainActivity`. Quy tắc bắt buộc kế thừa base bên dưới cũng áp dụng cho màn hình này.
- `app` có khung điều hướng chính với bốn tab theo thứ tự: Home, AI Assistant, Map và Profile. `MainActivity` dùng `LsActivity`, ViewPager2 và `LsPageAdapter`. Bottom bar dùng menu XML theo yêu cầu thông qua Material `BottomNavigationView` đặt trong container `LsConstraintView`, mỗi icon drawable hiện có nằm trên nhãn chữ luôn hiển thị. Dùng thành phần hỗ trợ menu này cho điều hướng chính; `LsBottomBar` không có cơ chế khai báo menu XML. Bốn Fragment dùng `LsFragment` và hiện có layout rỗng. Giữ thứ tự tab, đồng bộ mục menu đang chọn và khôi phục trạng thái pager. Khai báo package trong `PackageInfo.kt` không có nghĩa database, API, repository hoặc use case đã tồn tại.
- `base` chứa các lớp nền `Ls*`, custom view, extension, tiện ích theme, font và `Prefs`. Phải đọc phần triển khai liên quan trước khi tái sử dụng.
- Mặc định chỉ sửa code trong `app`. Có thể đọc `base` để tái sử dụng; chỉ sửa `base`, Gradle ở thư mục gốc, version catalog hoặc module khác khi người dùng cho phép phạm vi đó. Có thể cập nhật chỉ dẫn và tài liệu ở thư mục gốc khi được yêu cầu.
- Không sao chép tính năng nghiệp vụ, thông tin xác thực, cấu hình ký ứng dụng, tài nguyên hoặc dependency từ dự án khác chỉ vì dự án đó được cung cấp làm mẫu.

## Kiến trúc package

Tuân theo `app/ARCHITECTURE.md`, dùng tên package `feature` ở dạng số ít dưới `com.example.huetaste`:

- `common.extension`: extension nhỏ, không giữ trạng thái, dùng chung giữa các tính năng.
- `common.navigation`: hợp đồng điều hướng và các route dùng chung.
- `common.network`: tiện ích kết nối và chính sách mạng của ứng dụng.
- `common.ui`, `common.widget`: thành phần UI và custom view tái sử dụng.
- `data.local`: nguồn dữ liệu cục bộ và triển khai lưu trữ.
- `data.remote`: khai báo API, DTO và nguồn dữ liệu từ xa.
- `data.mapper`: chuyển đổi giữa kiểu dữ liệu ở lớp data và model nghiệp vụ.
- `data.repository`: triển khai hợp đồng repository của domain.
- `domain.model`: model nghiệp vụ độc lập với framework.
- `domain.repository`: hợp đồng repository độc lập với framework.
- `domain.usecase`: các thao tác nghiệp vụ có trách nhiệm rõ ràng.
- `di`: khai báo liên kết dependency ở cấp ứng dụng.
- `feature.<name>`: màn hình, trạng thái UI, ViewModel, adapter và giao diện modal thuộc một tính năng.

`domain` không phụ thuộc Android, framework mạng/database cụ thể, `data` hoặc `feature`. Lớp data triển khai hợp đồng domain; tính năng sử dụng model và thao tác nghiệp vụ của domain. Liên kết dependency cụ thể trong `di`, không khởi tạo trực tiếp trong màn hình. Hạ tầng dùng chung không import màn hình cụ thể hoặc phân nhánh theo tên tính năng. Chỉ đưa code vào `common` khi ít nhất hai tính năng cần dùng; ưu tiên tái sử dụng `base`. Chỉ thêm package tính năng và lớp trừu tượng khi phần triển khai được yêu cầu cần chúng.

## Điều hướng và vòng đời

- Dùng Activity cho luồng có điểm truy cập, back stack, vòng đời hoặc hành vi cửa sổ riêng. Ưu tiên Fragment cho màn hình dùng chung thanh điều hướng, trạng thái hoặc chuyển tab thường xuyên.
- Không thêm Fragment chỉ để bọc một màn hình Activity. Dùng dialog fragment hoặc bottom-sheet fragment khi cơ chế vòng đời và khôi phục của chúng phù hợp với luồng modal.
- Kiểm tra điều hướng hiện có trước khi thay ranh giới Activity/Fragment. Giữ hành vi nút Back, tab đang chọn, vị trí cuộn và khôi phục trạng thái; tránh tạo trùng Activity hoặc Fragment được khôi phục.
- Dùng child fragment manager cho Fragment lồng nhau. Gắn việc quan sát UI và callback với vòng đời phù hợp; giải phóng tham chiếu view và hủy công việc thuộc view khi view bị hủy.
- Lưu cài đặt đơn giản cần tồn tại lâu dài qua lớp preferences. Tái sử dụng `com.base.data.Prefs` khi phù hợp và đã cấu hình dependency; không giữ trạng thái màn hình bằng đối tượng toàn cục có thể thay đổi.
- Xử lý inset của thanh hệ thống và bàn phím để tránh cộng dồn padding, áp dụng inset hai lần hoặc che khuất điều khiển. Giữ hành vi edge-to-edge hiện tại khi thay màn hình chính.

## Quy tắc UI Android và tích hợp base

- Tất cả Activity, Fragment, dialog, bottom sheet và lớp UI khác trong app có lớp tương ứng trong `base` đều phải kế thừa lớp base đó, trực tiếp hoặc thông qua lớp con trong app. Activity phải dùng `LsActivity`; Fragment phải dùng `LsFragment`, hoặc `LsFragmentNew` hiện có khi cần cơ chế observable theo vòng đời; dialog phải dùng `LsDialog` hoặc `LsDialogFragment`; bottom sheet phải dùng `LsBottomSheet`. Đọc constructor và hook vòng đời trước khi triển khai lớp con. Không bỏ qua base để kế thừa trực tiếp framework hoặc sao chép lại hạ tầng của base.
- UI XML bắt buộc dùng các lớp `com.base.common.widget.Ls*` hiện có khi có thành phần tương ứng, ví dụ `LsTextView`, `LsImageView`, `LsEditText`, `LsConstraintView`, `LsLinearView`, `LsFrameView`, `LsRelativeView`, `LsCardView`, `LsView`. Custom view trong app phải kế thừa view base phù hợp khi lớp đó cho phép kế thừa. Chỉ dùng thành phần framework hoặc thư viện bên ngoài cho loại điều khiển chưa có thành phần tương ứng trong `base`; không tạo lại thành phần base đã có.
- Cấu hình View Binding và Hilt trong app theo yêu cầu của các thành phần base bắt buộc này khi triển khai. Kiểm tra Gradle hiện tại và bổ sung đồng bộ plugin, dependency, Application entry point, injection binding và annotation của nơi chứa view. Thiếu cấu hình là phần dependency cần giải quyết, không phải lý do để bỏ qua `base`. Kiểm tra trường được inject và vòng đời trong code base thực tế.
- Dùng View Binding theo constructor và hook của các lớp UI base. Bật trong `app/build.gradle.kts` khi cần; không giả định lớp binding đã được sinh hoặc bật Data Binding khi chưa có nhu cầu cụ thể.
- Mọi thuộc tính XML cần dimension theo dp phải tham chiếu resource dạng `@dimen/_<số>dp`, ví dụ `@dimen/_8dp`, `@dimen/_10dp`, `@dimen/_24dp`. Áp dụng cho kích thước layout, margin, padding, elevation, bo góc và kích thước nội tại của drawable. Không đặt dp cứng trong XML, kể cả `0dp`: dùng `@dimen/_0dp` cho match constraints. Ngoại lệ duy nhất là giá trị guide offset ban đầu `0dp` của hai Guideline thanh hệ thống bắt buộc bên dưới, theo yêu cầu rõ ràng của người dùng. Vẫn cho phép `match_parent`, `wrap_content`; tọa độ viewport/path không có đơn vị của vector không bị ảnh hưởng. Cỡ chữ dùng resource sp phù hợp hoặc thuộc tính cỡ chữ được base hỗ trợ.
- Tái sử dụng resource `_<số>dp` đã có; nếu thiếu, chỉ khai báo giá trị cần dùng trong `app/src/main/res/values/dimens.xml`, ví dụ `<dimen name="_10dp">10dp</dimen>`. Giá trị dp cứng chỉ nằm trong khai báo dimension, không nằm ở thuộc tính XML sử dụng nó. Không thay quy ước `_<số>dp` bằng `_10sdp` hoặc resource SDP khác.
- Lề ngang tiêu chuẩn (`marginHori`) cho các phần tử nội dung trong Activity là `@dimen/_10dp`. Dùng `android:layout_marginHorizontal` hoặc margin start/end tương đương trên phần tử hoặc container nội dung. Chỉ áp dụng một lần; không cộng dồn cùng lề ở cả cha và con. Tính đến màn hình nhỏ, chữ lớn và inset hệ thống.
- Với `LsTextView`, dùng giá trị ngữ nghĩa được hỗ trợ của `app:textSize`, `app:textFont`, `app:textColor` khi khớp thiết kế. Kiểm tra attributes và mapping theme trong `base`; đây là enum, không phải dimension hoặc màu tùy ý.
- Thuộc tính framework như kích thước layout, text, gravity và accessibility giữ namespace `android:`. Đưa chuỗi hiển thị, màu và shape tái sử dụng vào resources.
- Tái sử dụng click helper phù hợp từ `base`, kể cả debounce và animation của helper. Giữ accessibility, content description, vùng chạm và hành vi nhập liệu mong đợi.

### Hai điểm neo thanh hệ thống bắt buộc của Activity

Mọi layout Activity phải có hai Guideline ngang dưới đây bên trong container constraint gốc, giữ đúng ID mà `LsActivity` sử dụng:

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

Khai báo XML namespace `app` trên root. Neo nội dung bên dưới `statusBar` và điều khiển phía dưới bên trên `navigationBar`, trực tiếp hoặc thông qua container nội dung. `LsActivity` cập nhật vị trí neo theo inset thanh hệ thống; không cộng thêm padding inset cho cùng nội dung hoặc thay listener inset của base.

## Font, tài nguyên và thiết kế

- Font nằm trong `base/src/main/res/font`, gồm Inter, SF Pro Display và SF Pro Rounded. Kiểm tra mapping thực tế của `FontManager`; không tự cho rằng `app:textFont` chọn một họ SF Pro cụ thể. Dùng font resource hiện có một cách rõ ràng khi thiết kế yêu cầu.
- Giữ hình được cung cấp, tỷ lệ, độ trong suốt và tên resource. `ic_home`, `ic_map`, `ic_user`, `ic_ai` hiện là XML VectorDrawable.
- Ưu tiên VectorDrawable XML cho icon đơn giản phù hợp. Chỉ chuyển icon raster khi được yêu cầu hoặc công việc bao gồm xử lý tài nguyên đó; giữ hình thức và tránh resource trùng tên trong cùng cấu hình.
- Giữ ảnh chụp, texture và minh họa phức tạp ở dạng raster. Chọn thư mục theo mật độ hoặc `drawable-nodpi` theo cách sử dụng; giữ định dạng hiện tại trừ khi được yêu cầu chuyển hoặc việc chuyển là cần thiết.
- Khi có thiết kế Figma, kiểm tra node được chỉ định, khoảng cách, font, màu và tài nguyên. Triển khai bằng layout Android và thành phần phù hợp của dự án.
- Báo chính xác tài nguyên thiết yếu bị thiếu; không âm thầm thay bằng hình không liên quan. Dùng screenshot thiết bị do người dùng cung cấp để điều chỉnh responsive và giữ ý đồ thiết kế.

## Hiệu năng và quản lý trạng thái

- Đưa đọc/ghi đĩa, truy vấn database, tác vụ mạng, xử lý ảnh và phân tích dữ liệu nặng ra khỏi main thread. Không dùng `allowMainThreadQueries`, `runBlocking` trên UI thread, vòng lặp chờ hoặc chờ đồng bộ để tải màn hình.
- Dùng tác vụ bất đồng bộ gắn với vòng đời và có thể hủy. Tránh `GlobalScope`, tham chiếu Activity/Fragment tồn tại quá lâu và tải dữ liệu lặp lại khi xoay màn hình hoặc điều hướng.
- Giữ khởi động ứng dụng nhẹ. Tải dữ liệu nặng khi cần, chỉ cache kết quả tái sử dụng có lý do và tránh khởi tạo sớm dịch vụ tính năng chưa dùng.
- Ưu tiên model request/result bất biến và xác định rõ nơi sở hữu trạng thái. Chỉ lưu trạng thái cần để khôi phục luồng sau khi bị tạo lại.
- Dùng diffing và phân trang phù hợp cho danh sách lớn. Tải ảnh theo kích thước hiển thị, hủy tìm kiếm/request lỗi thời và debounce input khi hữu ích.
- Giữ logging nhẹ, không ghi dữ liệu nhạy cảm. Giữ kiểm tra chứng chỉ TLS và hostname bình thường. Thêm migration khi đổi dữ liệu lưu trữ thay vì âm thầm xóa dữ liệu người dùng.
- Giải thích quyết định hiệu năng dựa trên code đã kiểm tra; không khẳng định đã kiểm chứng độ mượt hoặc không lag nếu chưa có bằng chứng từ thiết bị.

## Dependency và kỷ luật thay đổi

- Chỉ thêm thư viện hoặc đổi Gradle khi phần triển khai được yêu cầu cần chúng. Kiểm tra dependency và alias hiện có trước; không nhập hàng loạt dependency từ dự án khác.
- Giữ namespace, application ID, tham chiếu resource và hợp đồng công khai trừ khi nhiệm vụ cần thay. Khi chuyển class màn hình, cập nhật manifest và tham chiếu công cụ liên quan.
- Giữ thay đổi không liên quan của người dùng. Tránh refactor ngoài phạm vi, tính năng sản phẩm giả, sửa `.idea`, build output sinh tự động, cache, file ký hoặc cấu hình riêng của máy.
- Không nhúng API secret, mật khẩu ký ứng dụng hoặc đường dẫn tuyệt đối riêng của máy vào source ứng dụng hoặc cấu hình đưa vào repository.
- Cập nhật tài liệu kiến trúc khi trách nhiệm package hoặc chiều phụ thuộc được triển khai thay đổi.

## Kiểm tra và bàn giao

- Mặc định không chạy Gradle build, test, cài app, emulator, thiết bị hoặc ứng dụng. Người dùng kiểm tra runtime và cung cấp kết quả. Chỉ làm các bước này khi người dùng yêu cầu rõ ràng.
- Kiểm tra tĩnh phù hợp: đọc source có mục tiêu, parse XML, kiểm tra tham chiếu resource/package/manifest và `git diff --check` khi có Git repository. Với thay đổi UI, kiểm tra thêm việc bắt buộc kế thừa base, dùng thành phần base trong XML, tham chiếu dimension `_<số>dp` và lề ngang `_10dp`. Không khởi tạo Git chỉ để chạy kiểm tra.
- Không thêm test chỉ vì tạo bộ khung hoặc đổi định dạng tài nguyên. Khi được yêu cầu test có ý nghĩa, viết trong phạm vi cho phép và chỉ chạy nếu việc chạy cũng được cho phép.
- Khi bàn giao, nói rõ đã đổi gì và vì sao, tóm tắt kiểm tra tĩnh thực sự đã làm, nêu chưa build hoặc kiểm chứng runtime và chỉ ra giới hạn đã biết. Dùng ngôn ngữ của người dùng và đưa hướng dẫn xử lý cụ thể khi kết quả test của họ cần sửa tiếp.
