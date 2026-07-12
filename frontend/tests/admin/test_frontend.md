Hãy kiểm tra và hoàn thiện hệ thống test frontend cho dự án:

D:\swp\Manager-warehouse-sdd

Frontend sử dụng React 18, Vite, JavaScript, Zustand, Axios và React Router.

Hiện trạng:
- Có hai file test:
  frontend/tests/admin/config.test.js
  frontend/tests/admin/rbac.test.js
- package.json chưa có test script.
- Chưa cài Jest hoặc Vitest.
- GitHub Actions đang dùng `npm test --if-present`, nên có thể bỏ qua toàn bộ frontend test nhưng vẫn báo thành công.

Mục tiêu:
1. Kiểm tra toàn bộ source và test frontend hiện có.
2. Sử dụng Vitest vì dự án đang dùng Vite.
3. Cài và cấu hình:
   - vitest
   - jsdom
   - @testing-library/react
   - @testing-library/jest-dom
   - @testing-library/user-event
   - @vitest/coverage-v8
4. Thêm các script:
   - `test`: chạy test một lần
   - `test:watch`: chạy test watch mode
   - `test:coverage`: chạy test và tạo coverage
5. Cấu hình môi trường jsdom và file test setup.
6. Không xóa test hiện có trừ khi chứng minh được test sai, trùng hoặc lỗi thời.
7. Sửa test hiện có để kiểm tra code production thật.
8. Không sao chép logic production vào file test. Hãy export và import function cần kiểm tra từ source.
9. Đảm bảo `config.test.js` kiểm tra validation thật của SystemConfig.
10. Đảm bảo `rbac.test.js` kiểm tra Zustand auth store và quyền kho thật.

Phạm vi cần test:

A. Utility và validation
- Dữ liệu hợp lệ.
- Null, undefined và chuỗi rỗng.
- Giá trị âm, bằng 0 và vượt giới hạn.
- Boundary values.
- Dữ liệu sai kiểu.
- Nhiều lỗi xuất hiện đồng thời.

B. Zustand store
- Login thành công.
- Logout xóa user và token.
- Khôi phục authentication state.
- ADMIN và CEO truy cập đúng.
- WAREHOUSE_MANAGER chỉ truy cập kho được phân công.
- User không có kho bị từ chối.
- State không bị rò rỉ giữa các test.

C. Component
- Render loading, empty, success và error state.
- Hiển thị dữ liệu đúng.
- Nhập và submit form.
- Hiển thị validation error.
- Button bị disable khi đang gửi.
- Hiển thị thông báo thành công/thất bại.
- Modal mở, đóng và xác nhận đúng.

D. Routing và phân quyền
- Chưa đăng nhập bị chuyển về login.
- Sai role không truy cập được route.
- Đúng role truy cập được route.
- User không truy cập được warehouse ngoài phạm vi.
- Route không tồn tại hiển thị trang phù hợp.

E. API
- Mock Axios hoặc dùng MSW.
- Test API success.
- Test 400, 401, 403, 404 và 500.
- Test network error và timeout.
- Test loading state.
- Không gọi backend thật trong unit/component test.

F. Luồng nghiệp vụ ưu tiên
- Login.
- Quản lý sản phẩm.
- Nhập kho.
- QC và putaway.
- Xuất kho và giao hàng.
- Chuyển kho.
- Kiểm kê.
- Trả hàng.
- Phân quyền người dùng và warehouse.

Coverage:
1. Xuất báo cáo:
   - coverage/index.html
   - coverage/lcov.info
2. Báo line, branch, function và statement coverage.
3. Ưu tiên coverage cho store, utility, validation, route guard và component nghiệp vụ.
4. Không viết test vô nghĩa chỉ để tăng coverage.
5. Cấu hình SonarQube đọc:
   frontend/coverage/lcov.info

Các lệnh phải chạy thành công:

cd D:\swp\Manager-warehouse-sdd\frontend
npm ci
npm run lint
npm run test
npm run test:coverage
npm run build

GitHub Actions:
1. Thay `npm test --if-present` bằng test bắt buộc.
2. CI cần chạy:
   - npm ci
   - npm run lint
   - npm run test:coverage
   - npm run build
3. Upload coverage artifact khi phù hợp.
4. Gửi LCOV sang SonarQube.
5. CI phải thất bại nếu lint, test hoặc build thất bại.
6. Không dùng `|| true` hoặc `--if-present` để che giấu lỗi.

Quy trình thực hiện:
1. Kiểm tra git status trước khi chỉnh sửa.
2. Không ghi đè hoặc xóa thay đổi chưa commit của người dùng.
3. Đọc package.json, cấu hình Vite, source và test hiện có.
4. Chạy baseline lint/build trước khi sửa.
5. Cấu hình Vitest.
6. Sửa hai test hiện có để test source thật.
7. Bổ sung các test quan trọng nhất.
8. Chạy lint, test, coverage và build.
9. Phân tích lỗi và sửa trong phạm vi frontend.
10. Không sửa logic production chỉ để làm test pass. Nếu phát hiện bug production, giải thích bằng bằng chứng trước khi sửa.
11. Không tự commit, push hoặc merge khi chưa được yêu cầu.
12. Không lưu token hoặc secret trong repository.

Kết quả cuối cùng cần báo cáo:
1. Những file đã thay đổi.
2. Tổng số test suite và test case.
3. Số pass, fail và skipped.
4. Coverage theo line, branch, function và statement.
5. Component/module chưa được test đầy đủ.
6. Bug production phát hiện được.
7. Cấu hình GitHub Actions và SonarQube đã thay đổi.
8. Những vấn đề còn lại và bước tiếp theo.

Tiêu chí hoàn thành:
- `npm run lint` thành công.
- `npm run test` thực sự chạy test.
- `npm run test:coverage` tạo HTML và LCOV.
- `npm run build` thành công.
- GitHub Actions không còn bỏ qua frontend test.
- SonarQube nhận được frontend coverage.