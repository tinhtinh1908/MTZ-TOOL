MTZ TOOL 1.0.0

Đây là project standalone mới viết lại từ đầu dựa trên luồng dữ liệu
quan sát được trong source ThemeKit mà bạn đã cung cấp. APK tạo ra không
cần cài ThemeKit và không gọi bất kỳ activity/service nào của ThemeKit.

Không có:
- phụ thuộc APK ThemeKit
- Shizuku
- máy chủ trung gian
- quảng cáo
- theo dõi
- ngôn ngữ khác tiếng Việt
- ThemePCService/Binder

Có:
- nhập trực tiếp font TTF/OTF/TTC
- đọc gói font MTZ có sẵn
- tự đóng gói font theo cấu trúc Xiaomi
- mở trang chi tiết font trong Theme Manager
- đọc description.xml
- quét resource theo quy tắc ThemeKit
- trích xuất preview
- tạo content/*.mrc
- tạo metadata JSON *.mrm
- tạo root theme và subResources
- hash SHA-1
- copy toàn bộ cây staging vào .data bằng root
- kiểm tra file sau khi chép
- mở Theme Manager
- nút mở trang chính thức: https://tinhtinh1908.github.io/home/

BUILD TRÊN TERMUX

1. Giải nén project.
2. Trong thư mục project:

   printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties

3. Nếu gradle.properties chưa có AAPT2 override:

   printf '\nandroid.aapt2FromMavenOverride=%s\n' \
     "$(command -v aapt2)" >> gradle.properties

4. Tạo wrapper nếu cần:

   gradle wrapper --gradle-version 8.9

5. Build bản release:

   bash gradlew clean assembleRelease --no-daemon --console=plain

APK:
app/build/outputs/apk/release/app-release-unsigned.apk


BUILD NHANH MỘT LỆNH

Trong thư mục project:

bash build-termux.sh

APK sẽ được chép ra:

/sdcard/Download/MTZ-Tool-1.0.0-unsigned.apk


THAY ĐỔI BẢN 1.0.0

- Thêm tab Font, nhận TTF, OTF, TTC và MTZ.
- Tự tạo fonts/Roboto-Regular.ttf và description.xml chuẩn MIUI.
- Đăng ký font thành resource `fonts` trong kho Theme Manager bằng root.
- Tạo ảnh xem trước tiếng Việt và mở thẳng trang chi tiết font.
- Dựng lại bố cục gọn theo phong cách HyperOS, tự đổi sáng/tối.
- Đổi tên ứng dụng thành MTZ Tool.
- Đổi package phát hành thành root.dtinh.mtzimporter.
- Dọn các dòng chú thích dư trong giao diện và rút gọn thông báo trạng thái.
- Thêm nút Website trên thanh đầu ứng dụng.
- Xóa nền ngoài artwork Doro MTZ và thêm vùng an toàn trong suốt.
- Dùng icon bitmap trực tiếp để launcher HyperOS không phóng/cắt artwork.
- Hỗ trợ Android 11 trở lên, không giới hạn phiên bản Android tối đa.
- Chừa đúng vùng status bar, tai thỏ và navigation bar trên Android 15.
- Thu gọn header, card và cỡ chữ để không chồng khi đổi DPI/font.
- Thêm tab Đã nhập để xem toàn bộ theme cục bộ trong Theme Manager.
- Chạm vào theme để mở trang chi tiết; có nút làm mới danh sách.
- Xóa đồng bộ metadata, content, subResources và preview bằng root.
- Có hộp xác nhận và kiểm tra ID/path trước khi xóa.
- Gộp thao tác chính vào thẻ chọn MTZ và thẻ trạng thái riêng.
- Bỏ toàn bộ khu vực Lối tắt và các nút mở thủ công dư thừa.
- Sau khi nhập thành công vẫn tự mở trang chi tiết chủ đề.
- Thêm icon adaptive, viền card, trạng thái root và trạng thái xử lý rõ hơn.
- Bỏ quyền thông báo không dùng và chặn cleartext traffic.
- Giữ nguyên parser/importer và fallback mở Theme Manager.
