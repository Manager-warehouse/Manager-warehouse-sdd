# Mobile Build Guide — Phúc Anh WMS

Hướng dẫn này dành riêng cho dự án `Manager-warehouse-sdd`, dùng frontend React/Vite hiện tại để đóng app mobile bằng Capacitor.

## 1. Thông tin hiện tại của app

- Frontend: `frontend/`
- Web build output: `frontend/dist`
- Capacitor config: `frontend/capacitor.config.json`
- App name: `Phúc Anh WMS`
- App ID / Bundle ID / Application ID: `vn.phucanh.wms`
- iOS project: `frontend/ios`
- Android project: `frontend/android`
- API production: `https://manager-warehouse.online/api/v1`

File production env cần có trên máy build:

```env
VITE_API_BASE_URL=https://manager-warehouse.online/api/v1
```

Đặt tại:

```text
frontend/.env.production
```

Lưu ý: file `.env.production` đang bị `.gitignore` ignore, nên máy mới clone repo phải tự tạo lại file này.

## 2. Quy trình build chung sau mỗi lần sửa frontend

Chạy từ thư mục frontend:

```bash
cd /Users/haison/Documents/GitHub/Manager-warehouse-sdd/frontend

npm run build
npx cap sync
```

Lệnh `npm run build` tạo bundle web trong `frontend/dist`.

Lệnh `npx cap sync` copy bundle mới vào iOS/Android project.

## 3. Build Android APK debug

### 3.1. Yêu cầu

- Cài Android Studio trên Mac.
- Android SDK nằm tại:

```text
/Users/haison/Library/Android/sdk
```

Nếu Gradle báo `SDK location not found`, tạo file:

```text
frontend/android/local.properties
```

Nội dung:

```properties
sdk.dir=/Users/haison/Library/Android/sdk
```

`local.properties` là file local theo máy, không nên commit.

### 3.2. Build APK debug

```bash
cd /Users/haison/Documents/GitHub/Manager-warehouse-sdd/frontend

npm run build
npx cap sync android

cd android
./gradlew assembleDebug
```

File APK tạo ra tại:

```text
frontend/android/app/build/outputs/apk/debug/app-debug.apk
```

Có thể đổi tên file để gửi test nội bộ, ví dụ:

```text
phuc-anh-wms-debug.apk
```

Người nhận cài trên Android cần bật quyền cài app ngoài CH Play (`Install unknown apps`).

## 4. Build Android release APK

Debug APK dùng test nội bộ. Release APK cần ký bằng keystore riêng.

### 4.1. Tạo keystore một lần

Chạy trong thư mục Android:

```bash
cd /Users/haison/Documents/GitHub/Manager-warehouse-sdd/frontend/android

keytool -genkeypair \
  -v \
  -keystore phucanh-wms-release.keystore \
  -alias phucanh-wms \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Phải lưu kỹ:

- file `phucanh-wms-release.keystore`
- alias `phucanh-wms`
- store password
- key password

Mất keystore thì sau này không update được app cùng chữ ký.

### 4.2. Tạo `keystore.properties`

Tạo file:

```text
frontend/android/keystore.properties
```

Mẫu nội dung:

```properties
storeFile=phucanh-wms-release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=phucanh-wms
keyPassword=YOUR_KEY_PASSWORD
```

Không commit `keystore.properties` và không commit file `.keystore`.

### 4.3. Build release

Sau khi cấu hình signing trong `frontend/android/app/build.gradle`, chạy:

```bash
cd /Users/haison/Documents/GitHub/Manager-warehouse-sdd/frontend/android

./gradlew assembleRelease
```

Release APK nằm tại:

```text
frontend/android/app/build/outputs/apk/release/app-release.apk
```

Nếu sau này đưa lên Google Play, build AAB:

```bash
./gradlew bundleRelease
```

File AAB nằm tại:

```text
frontend/android/app/build/outputs/bundle/release/app-release.aab
```

## 5. Build và chạy iOS app

### 5.1. Yêu cầu

- Mac có Xcode.
- Xcode đã cài iOS Simulator.
- Apple ID đã đăng nhập trong Xcode.

Kiểm tra Xcode:

```bash
xcodebuild -version
```

### 5.2. Sync iOS

```bash
cd /Users/haison/Documents/GitHub/Manager-warehouse-sdd/frontend

npm run build
npx cap sync ios
npx cap open ios
```

Lệnh cuối mở project iOS bằng Xcode.

Nếu cần mở thủ công, chọn file:

```text
frontend/ios/App/App.xcworkspace
```

Nên mở `.xcworkspace`, không mở `.xcodeproj`.

### 5.3. Chạy bằng Simulator

Trong Xcode:

1. Chọn scheme `App`.
2. Chọn device simulator, ví dụ `iPhone 17 Pro Max`.
3. Bấm Run.

Simulator không yêu cầu provisioning profile như iPhone thật.

### 5.4. Chạy trực tiếp trên iPhone thật

Điều kiện bắt buộc:

- Mac/Finder phải nhận iPhone.
- iPhone đã Trust máy Mac.
- iPhone đã bật Developer Mode.
- Xcode phải thấy iPhone trong dropdown device.

Nếu Finder không thấy iPhone:

1. Đảm bảo Finder đã bật `CD, DVD và thiết bị iOS` trong `Finder -> Settings -> Thanh bên`.
2. Mở khóa iPhone trước khi cắm.
3. Dùng dây USB-C có truyền dữ liệu, không chỉ sạc.
4. Nếu Trust popup không hiện, trên iPhone chạy:

```text
Cài đặt -> Cài đặt chung -> Chuyển hoặc đặt lại iPhone -> Đặt lại -> Đặt lại Vị trí & Quyền riêng tư
```

Kiểm tra Mac có nhận iPhone qua USB:

```bash
system_profiler SPUSBDataType | grep -i -A 20 -B 5 "iPhone"
```

Khi Xcode thấy iPhone thật:

1. Chọn iPhone trong dropdown device.
2. Vào `Signing & Capabilities`.
3. Chọn Team.
4. Bấm Run.

Với Apple ID miễn phí, cách thực tế nhất là cài trực tiếp bằng Run từ Xcode. Xuất `.ipa` để gửi cho người khác thường cần Apple Developer Program trả phí.

## 6. Xuất iOS IPA

Trong Xcode:

```text
Product -> Archive
```

Sau khi archive xong:

```text
Distribute App
```

Các lựa chọn thường gặp:

- Development: cài thử cho thiết bị dev.
- Ad Hoc: gửi IPA cho danh sách thiết bị đã đăng ký UDID.
- TestFlight/App Store: phân phối chuẩn qua Apple.

Lưu ý: iOS không giống Android. File `.ipa` không thể gửi cho ai cũng cài được nếu không có signing/provisioning phù hợp.

## 7. CORS backend cho app mobile

App mobile không chạy từ domain web `https://manager-warehouse.online`.

Origin thường gặp:

- iOS Capacitor: `capacitor://localhost`
- Ionic/Capacitor fallback: `ionic://localhost`
- Android Capacitor có thể dùng `http://localhost`

Backend production phải cho phép các origin này trong CORS. File local hiện liên quan:

```text
backend/src/main/java/com/wms/config/SecurityConfig.java
```

Nếu app login báo `AxiosError: Network Error`, kiểm tra CORS production:

```bash
curl -i -X OPTIONS 'https://manager-warehouse.online/api/v1/auth/login' \
  -H 'Origin: capacitor://localhost' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: content-type'
```

Nếu trả về:

```text
403 Invalid CORS request
```

thì backend production chưa deploy CORS mới.

Sau khi sửa backend, phải deploy backend lên VPS vì app mobile gọi production API.

## 8. PWA fallback cho iPhone

Nếu chưa cài được iOS app thật, có thể dùng tạm PWA:

1. Mở Safari trên iPhone.
2. Vào:

```text
https://manager-warehouse.online
```

3. Bấm Share.
4. Chọn `Add to Home Screen` / `Thêm vào Màn hình chính`.

Cách này không cần Xcode, không cần dây data, không cần Apple Developer, nhưng không phải file `.ipa`.

## 9. Checklist trước khi gửi tester

- [ ] `frontend/.env.production` trỏ đúng `https://manager-warehouse.online/api/v1`
- [ ] Đã chạy `npm run build`
- [ ] Đã chạy `npx cap sync`
- [ ] Android APK debug/release được build thành công
- [ ] iOS Simulator chạy được
- [ ] Backend production đã deploy CORS cho mobile origin
- [ ] Login production chạy được trên mobile app
- [ ] Không commit secrets, keystore, `keystore.properties`, `.env.production`

