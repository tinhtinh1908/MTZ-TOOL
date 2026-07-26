MTZ TOOL NON-ROOT 1.0.0

Thông tin:
- Tên ứng dụng: MTZ Tool
- Package: nonroot.dtinh.mtzimporter
- Version: 1.0.0
- Android: 11 trở lên
- Nền tảng: Xiaomi HyperOS/MIUI

Chức năng:
- Nhập theme MTZ bằng Shizuku.
- Nhập TTF, OTF, TTC hoặc gói font MTZ.
- Xem, mở và xóa theme đã nhập.
- Bật/tắt bảo vệ chống đặt lại theme.
- Tự chạy lại bảo vệ sau khi khởi động máy.
- Mở thiết lập Tự khởi động và Không giới hạn pin.
- Mở website: https://tinhtinh1908.github.io/home/

Thiết lập:
1. Khởi động Shizuku.
2. Mở MTZ Tool và cấp quyền Shizuku.
3. Bật Bảo vệ theme.
4. Cho phép Tự khởi động và đặt pin thành Không giới hạn.

Build:

  gradle wrapper --gradle-version 8.9
  bash gradlew clean assembleRelease --no-daemon --console=plain

APK:
app/build/outputs/apk/release/app-release-unsigned.apk
