# Chặn cuộc gọi — Android

Ứng dụng Android tối giản bằng Kotlin: quản lý danh sách số cần chặn và dùng `CallScreeningService` để từ chối cuộc gọi đến khớp chính xác.

## Yêu cầu

- Android Studio bản hỗ trợ Android Gradle Plugin 8.7.3
- JDK 17
- Android SDK 35
- Thiết bị Android 10 (API 29) trở lên

## Mở và chạy

1. Clone repository và mở thư mục trong Android Studio.
2. Chờ Gradle Sync hoàn tất.
3. Chạy cấu hình `app` trên thiết bị Android.
4. Trong ứng dụng, chọn **Bật quyền sàng lọc cuộc gọi** và xác nhận vai trò trên màn hình hệ thống.
5. Thêm các số điện thoại cần chặn.

## Tạo APK

Trong Android Studio chọn **Build → Build APK(s)**. APK debug thường được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

Ứng dụng không yêu cầu quyền đọc danh bạ, lịch sử cuộc gọi hay truy cập mạng. Android yêu cầu người dùng cấp vai trò sàng lọc cuộc gọi để hệ thống gọi dịch vụ. Danh sách chặn được lưu cục bộ trên thiết bị. Việc khớp số là chính xác theo chữ số sau chuẩn hóa; số ẩn danh hoặc số ở định dạng quốc gia khác có thể không khớp.
