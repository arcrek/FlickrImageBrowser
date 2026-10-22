# Kế hoạch Triển khai: Nâng cấp UI/UX Phong cách Pinterest (Masonry Grid, Bo góc 16dp & Glide Animation)

## Mô tả mục tiêu (Goal Description)
Nâng cấp toàn diện giao diện và trải nghiệm cuộn ảnh của ứng dụng theo **phong cách Pinterest (Pinterest Staggered Masonry Grid)**:
1. **Lưới thác nước so le (Staggered Grid):** Thay thế lưới ô vuông đều chằn chặn bằng bố cục 2 cột so le cao thấp tự nhiên (`StaggeredGridLayoutManager`), tạo cảm giác thị giác sinh động như Pinterest thật.
2. **Thẻ ảnh bo góc mềm mại (Rounded Flat Cards):** Tăng bán kính bo góc lên `16dp`, thiết kế phẳng (`cardElevation="0dp"`), loại bỏ viền thô, có hiệu ứng chạm sóng nước (`Ripple`).
3. **Nút thả tim nổi kiểu Pinterest (Floating Action Pill):** Đặt nút trái tim dạng hình tròn nền mờ bán trong suốt (`scrim oval`) nổi tinh tế ở góc trên bên phải ảnh.
4. **Hoạt cảnh mượt mà (Smooth Animations):** Bổ sung hiệu ứng mờ dần êm ái (**Glide Cross-fade 250ms**) khi ảnh tải xong thay vì xuất hiện đột ngột, giúp trải nghiệm lướt ảnh mượt mà ở 60–120 FPS.

---

## Những điểm cần người dùng xem xét (User Review Required)

> [!IMPORTANT]
> **Tương thích Phân trang (Pagination Compatibility):**
> Khi chuyển từ `GridLayoutManager` sang `StaggeredGridLayoutManager`, phương thức xác định vị trí cuộn `findFirstVisibleItemPosition()` sẽ được cập nhật thành `findFirstVisibleItemPositions(int[] into)[0]`. Toàn bộ logic cuộn vô tận (Endless Scrolling) đã làm ở phần trước vẫn hoạt động bình thường 100%.

> [!NOTE]
> **Áp dụng đồng bộ cho cả Màn hình Yêu thích:**
> Do `FavoritesFragment.java` cũng dùng chung `PhotoAdapter.java` và layout `item_photo.xml`, chúng ta sẽ nâng cấp `FavoritesFragment` sang `StaggeredGridLayoutManager` để toàn bộ ứng dụng có phong cách thiết kế đồng nhất.

---

## Đề xuất Thay đổi (Proposed Changes)

### 1. Tạo drawable `res/drawable/bg_favorite_pill.xml`
- Nền mờ tròn màu đen bán trong suốt (`#66000000`) cho nút trái tim nổi trên ảnh.

### 2. Cập nhật `res/layout/item_photo.xml`
- `app:cardCornerRadius="16dp"`, `app:cardElevation="0dp"`, `app:strokeWidth="0dp"`.
- `ImageView`: `layout_height="wrap_content"`, `adjustViewBounds="true"`, `minHeight="140dp"`.
- Nút thả tim: Kích thước tròn `32dp x 32dp` với background `@drawable/bg_favorite_pill` nổi ở góc trên bên phải.

### 3. Cập nhật `PhotoAdapter.java`
- Thêm hiệu ứng Glide: `.transition(DrawableTransitionOptions.withCrossFade(250))`.

### 4. Cập nhật `SearchFragment.java` và `FavoritesFragment.java`
- Khởi tạo `StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)`.
- Cập nhật `OnScrollListener` để lấy vị trí đầu tiên từ `findFirstVisibleItemPositions(null)`.

---

## Kế hoạch Xác minh (Verification Plan)
- Chạy unit test: `./gradlew testDebugUnitTest --tests vn.edu.usth.flickrimagebrowser.ui.search.SearchViewModelTest` (6/6 tests passed).
- Test trực tiếp trên màn hình: Kiểm tra lưới so le, bo góc 16dp, nút tim tròn nổi, hiệu ứng mờ dần khi tải ảnh.
