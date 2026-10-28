# Pseudocode ứng dụng FlickrImageBrowser

Pseudocode dưới đây mô tả code hiện tại của app: tìm ảnh qua Pixabay, xem chi tiết, lưu yêu thích và quản lý bộ sưu tập. Dù tên project là *FlickrImageBrowser*, nguồn ảnh thực tế là **Pixabay API**.

## 1. Dữ liệu sử dụng

```text
ẢNH:
    id
    tiêu_đề
    tác_giả
    mô_tả
    url_ảnh_nhỏ
    url_ảnh_lớn
    là_yêu_thích
    thời_gian_lưu

BỘ_SƯU_TẬP:
    id
    tên
    mô_tả
    thời_gian_tạo

LIÊN_KẾT_ẢNH_BỘ_SƯU_TẬP:
    id_bộ_sưu_tập
    id_ảnh
    thời_gian_thêm
```

Dữ liệu yêu thích, thông tin ảnh và bộ sưu tập được lưu trong **Room Database**. Một ảnh có thể thuộc nhiều bộ sưu tập.

## 2. Khởi động và điều hướng

```text
CHƯƠNG_TRÌNH_CHÍNH
    Khởi tạo Room Database
    Khởi tạo Pixabay API client
    Khởi tạo Repository để làm việc với API và Database
    Khởi tạo ViewModel

    Mở màn hình TÌM_KIẾM

    KHI người dùng chọn tab:
        NẾU chọn "Search":
            Hiển thị màn hình TÌM_KIẾM

        NẾU chọn "Favorites":
            Hiển thị màn hình YÊU_THÍCH

        NẾU chọn "Collections":
            Hiển thị màn hình BỘ_SƯU_TẬP
KẾT_THÚC
```

## 3. Tìm kiếm ảnh

```text
HÀM TÌM_KIẾM(từ_khóa, bắt_buộc_tải_lại)

    từ_khóa ← XÓA_KHOẢNG_TRẮNG_ĐẦU_CUỐI(từ_khóa)

    NẾU từ_khóa rỗng:
        KẾT_THÚC HÀM

    NẾU từ_khóa giống lần tìm trước
       VÀ đã có trạng thái kết quả
       VÀ bắt_buộc_tải_lại = SAI:
        Giữ kết quả hiện tại
        KẾT_THÚC HÀM

    Lưu từ_khóa làm từ_khóa_hiện_tại
    trang_hiện_tại ← 1
    danh_sách_ảnh ← RỖNG
    còn_trang ← ĐÚNG

    Bỏ theo dõi nguồn kết quả tìm kiếm cũ
    Hiển thị trạng thái ĐANG_TẢI

    GỌI LẤY_ẢNH(từ_khóa, trang_hiện_tại, 30)

    KHI nhận kết quả:
        NẾU THÀNH_CÔNG:
            danh_sách_ảnh ← kết_quả

            NẾU số lượng ảnh < 30:
                còn_trang ← SAI

            Hiển thị danh_sách_ảnh dưới dạng lưới 2 cột

        NẾU KHÔNG_CÓ_KẾT_QUẢ:
            còn_trang ← SAI
            Hiển thị thông báo không tìm thấy ảnh

        NẾU LỖI:
            Hiển thị thông báo lỗi và nút "Retry"
KẾT_THÚC HÀM
```

Người dùng có thể nhập từ khóa hoặc chọn một chủ đề gợi ý trên màn hình.

### Lấy dữ liệu từ API

```text
HÀM LẤY_ẢNH(từ_khóa, trang, số_ảnh_mỗi_trang)

    NẾU API key rỗng:
        TRẢ_VỀ LỖI "Chưa cấu hình Pixabay API key"

    NẾU API key là "demo" hoặc key mẫu:
        Tạo danh sách ảnh demo cho từ_khóa và trang
        Đối chiếu trạng thái yêu thích với Database
        TRẢ_VỀ danh sách ảnh demo

    Giới hạn:
        trang tối thiểu là 1
        số_ảnh_mỗi_trang nằm trong khoảng 3..200

    Gửi yêu cầu bất đồng bộ đến Pixabay:
        keyword = từ_khóa
        image_type = "photo"
        safesearch = true
        page = trang
        per_page = số_ảnh_mỗi_trang

    NẾU lỗi mạng:
        TRẢ_VỀ LỖI MẠNG

    NẾU HTTP là 400, 401 hoặc 403:
        TRẢ_VỀ LỖI API KEY HOẶC THAM SỐ

    NẾU HTTP là 429:
        TRẢ_VỀ LỖI VƯỢT GIỚI HẠN API

    NẾU HTTP không thành công hoặc phản hồi rỗng:
        TRẢ_VỀ LỖI MÁY CHỦ

    Lấy danh sách ID ảnh yêu thích từ Database
    Chuyển dữ liệu Pixabay thành danh sách ẢNH
    Đánh dấu ảnh yêu thích theo danh sách ID

    Bỏ qua ảnh không có ID hợp lệ hoặc không có URL ảnh

    NẾU danh sách ảnh rỗng:
        TRẢ_VỀ KHÔNG_CÓ_KẾT_QUẢ

    TRẢ_VỀ THÀNH_CÔNG(danh_sách_ảnh)
KẾT_THÚC HÀM
```

## 4. Phân trang và tải lại

```text
KHI người dùng cuộn gần cuối danh sách ảnh:
    NẾU còn_trang = ĐÚNG
       VÀ không có trang khác đang tải
       VÀ danh_sách_ảnh không rỗng:
        GỌI TẢI_TRANG_TIẾP_THEO()
```

```text
HÀM TẢI_TRANG_TIẾP_THEO()

    trang_mới ← trang_hiện_tại + 1
    Đánh dấu đang tải thêm
    Giữ các ảnh đã hiển thị trên màn hình

    GỌI LẤY_ẢNH(từ_khóa_hiện_tại, trang_mới, 30)

    KHI nhận kết quả:
        NẾU THÀNH_CÔNG:
            Thêm ảnh mới vào danh_sách_ảnh
            trang_hiện_tại ← trang_mới

            NẾU số ảnh mới < 30:
                còn_trang ← SAI

            Cập nhật lưới ảnh

        NẾU KHÔNG_CÓ_KẾT_QUẢ:
            còn_trang ← SAI
            Giữ danh sách ảnh hiện tại

        NẾU LỖI:
            Giữ danh sách ảnh hiện tại
            Hiển thị lỗi và cho phép thử lại trang này

    Bỏ đánh dấu đang tải thêm
KẾT_THÚC HÀM
```

Nếu lỗi ở trang đầu, nút **Retry** tìm lại từ trang đầu. Nếu lỗi khi tải thêm, app thử lại trang tiếp theo.

## 5. Xem chi tiết ảnh

```text
KHI người dùng chọn một ảnh:
    Mở màn hình CHI_TIẾT_ẢNH và truyền thông tin ảnh

MÀN_HÌNH CHI_TIẾT_ẢNH
    NẾU không nhận được ảnh:
        Đóng màn hình
        KẾT_THÚC

    Hiển thị:
        Ảnh kích thước lớn
        Tiêu đề
        Tác giả
        Mô tả

    Theo dõi trạng thái yêu thích của ảnh từ Database
    Cập nhật biểu tượng trái tim khi trạng thái thay đổi

    KHI người dùng nhấn trái tim:
        GỌI ĐỔI_TRẠNG_THÁI_YÊU_THÍCH(ảnh)

    KHI người dùng chọn "Thêm vào bộ sưu tập":
        NẾU chưa có bộ sưu tập:
            Thông báo chưa có bộ sưu tập
        NGƯỢC LẠI:
            Cho người dùng chọn bộ sưu tập
            GỌI THÊM_ẢNH_VÀO_BỘ_SƯU_TẬP(bộ_sưu_tập, ảnh)

    KHI người dùng chọn "Chia sẻ":
        Mở menu chia sẻ của Android
        Nội dung chia sẻ ← tiêu_đề + URL ảnh lớn
KẾT_THÚC MÀN_HÌNH
```

## 6. Lưu và xem ảnh yêu thích

### Đổi trạng thái yêu thích

```text
HÀM ĐỔI_TRẠNG_THÁI_YÊU_THÍCH(ảnh)

    Chạy trên luồng xử lý Database

    ảnh_đã_lưu ← tìm ảnh theo ID trong Database

    NẾU ảnh chưa tồn tại HOẶC chưa được yêu thích:
        trạng_thái_mới ← YÊU_THÍCH
    NGƯỢC LẠI:
        trạng_thái_mới ← KHÔNG_YÊU_THÍCH

    Thêm hoặc cập nhật thông tin ảnh trong Database
    Đặt là_yêu_thích ← trạng_thái_mới
    Cập nhật thời_gian_lưu

    NẾU trạng_thái_mới = KHÔNG_YÊU_THÍCH:
        GỌI XÓA_ẢNH_KHÔNG_CÒN_THAM_CHIẾU(id_ảnh)

    Database thông báo thay đổi cho các màn hình đang theo dõi
KẾT_THÚC HÀM
```

### Danh sách, lọc và sắp xếp

```text
MÀN_HÌNH YÊU_THÍCH

    Theo dõi danh sách ảnh có là_yêu_thích = ĐÚNG

    KHI danh sách, từ khóa lọc hoặc kiểu sắp xếp thay đổi:
        Lọc ảnh theo tiêu đề HOẶC tên tác giả
        Không phân biệt chữ hoa và chữ thường

        NẾU chọn "A–Z":
            Sắp xếp theo tiêu đề tăng dần
        NGƯỢC LẠI:
            Sắp xếp theo thời gian lưu mới nhất

        NẾU danh sách sau lọc rỗng:
            Hiển thị trạng thái rỗng phù hợp
        NGƯỢC LẠI:
            Hiển thị danh sách ảnh

    KHI người dùng bỏ yêu thích một ảnh:
        Lưu tạm thông tin ảnh vừa bỏ
        Đặt là_yêu_thích ← SAI trong Database
        Hiển thị thông báo có nút "Undo"

    KHI người dùng nhấn "Undo":
        Thêm hoặc cập nhật lại ảnh trong Database
        Đặt là_yêu_thích ← ĐÚNG
KẾT_THÚC MÀN_HÌNH
```

## 7. Quản lý bộ sưu tập

### Tạo và đổi tên

```text
HÀM TẠO_BỘ_SƯU_TẬP(tên, mô_tả)

    tên ← XÓA_KHOẢNG_TRẮNG_ĐẦU_CUỐI(tên)

    NẾU tên rỗng:
        Báo lỗi "Tên không được để trống"
        KẾT_THÚC HÀM

    NẾU tên trùng bộ sưu tập khác, không phân biệt hoa/thường:
        Báo lỗi "Tên đã tồn tại"
        KẾT_THÚC HÀM

    Kiểm tra lại tên trong Database
    NẾU vẫn hợp lệ:
        Lưu bộ sưu tập mới vào Database

    Cập nhật danh sách bộ sưu tập
KẾT_THÚC HÀM
```

```text
HÀM ĐỔI_TÊN_BỘ_SƯU_TẬP(bộ_sưu_tập, tên_mới)

    Loại bỏ khoảng trắng đầu và cuối tên_mới

    NẾU tên_mới rỗng:
        Báo lỗi
        KẾT_THÚC HÀM

    NẾU tên_mới trùng tên bộ sưu tập khác:
        Báo lỗi
        KẾT_THÚC HÀM

    Kiểm tra lại tên trong Database
    NẾU hợp lệ:
        Cập nhật tên bộ_sưu_tập
KẾT_THÚC HÀM
```

### Thêm ảnh

```text
HÀM THÊM_ẢNH_VÀO_BỘ_SƯU_TẬP(bộ_sưu_tập, ảnh)

    NẾU ID bộ_sưu_tập không hợp lệ:
        KẾT_THÚC HÀM

    Chạy trên luồng xử lý Database

    ảnh_đã_lưu ← tìm ảnh theo ID

    NẾU ảnh đã tồn tại:
        Giữ trạng thái yêu thích và thời gian lưu hiện tại
    NGƯỢC LẠI:
        Lưu thông tin ảnh với trạng thái không yêu thích

    Thêm liên kết (ID bộ_sưu_tập, ID ảnh)
    NẾU liên kết đã tồn tại:
        Không thêm trùng

    Database cập nhật danh sách ảnh trong bộ sưu tập
KẾT_THÚC HÀM
```

### Xem và xóa

```text
KHI người dùng mở một bộ sưu tập:
    Theo dõi và hiển thị danh sách ảnh của bộ sưu tập

    KHI người dùng chọn một ảnh:
        Mở màn hình CHI_TIẾT_ẢNH

    KHI người dùng chọn xóa ảnh khỏi bộ sưu tập:
        Hiển thị hộp thoại xác nhận

        NẾU người dùng xác nhận:
            Xóa liên kết giữa ảnh và bộ sưu tập
            GỌI XÓA_ẢNH_KHÔNG_CÒN_THAM_CHIẾU(id_ảnh)
```

```text
KHI người dùng chọn xóa bộ sưu tập:
    Hiển thị hộp thoại xác nhận

    NẾU người dùng xác nhận:
        Xóa bộ sưu tập
        Xóa các liên kết thuộc bộ sưu tập đó
        Xóa những ảnh không yêu thích và không còn thuộc bộ sưu tập nào
```

```text
HÀM XÓA_ẢNH_KHÔNG_CÒN_THAM_CHIẾU(id_ảnh)

    NẾU ảnh không được yêu thích
       VÀ ảnh không thuộc bộ sưu tập nào:
        Xóa thông tin ảnh khỏi Database
    NGƯỢC LẠI:
        Giữ ảnh trong Database
KẾT_THÚC HÀM
```

**Lưu ý:** Room lưu thông tin ảnh và trạng thái yêu thích để truy cập cục bộ; việc hiển thị ảnh khi không có mạng còn phụ thuộc ảnh đã nằm trong cache của Glide hay chưa.

## Tham khảo mã nguồn

- [Điều hướng chính](../app/src/main/java/vn/edu/usth/flickrimagebrowser/MainActivity.java)
- [Tìm kiếm và phân trang](../app/src/main/java/vn/edu/usth/flickrimagebrowser/ui/search/SearchViewModel.java)
- [Repository, API và lưu trữ](../app/src/main/java/vn/edu/usth/flickrimagebrowser/data/repository/PhotoRepositoryImpl.java)
- [Ảnh yêu thích](../app/src/main/java/vn/edu/usth/flickrimagebrowser/ui/favorites/FavoritesViewModel.java)
- [Chi tiết ảnh](../app/src/main/java/vn/edu/usth/flickrimagebrowser/ui/detail/PhotoDetailActivity.java)
- [Quản lý bộ sưu tập](../app/src/main/java/vn/edu/usth/flickrimagebrowser/ui/collections/CollectionsViewModel.java)
