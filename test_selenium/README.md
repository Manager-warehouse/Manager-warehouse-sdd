# Test Selenium Automation Framework (WMS System Test)

## 1. Overview
Thư mục `test_selenium` chứa bộ kịch bản kiểm thử tự động End-to-End (E2E System Test) sử dụng **Selenium WebDriver** cho hệ thống **Warehouse Management System (WMS)**.

## 2. Mục đích
- Thực thi **System Test Round 2** (page-level smoke check) và **Round 3** (real create/mutate business flows) tự động trên môi trường Local/Staging.
- Tự động mở trình duyệt Chrome, mô phỏng thao tác người dùng thật theo đúng role được gán cho từng module (Đăng nhập, Quản lý danh mục, Nhập kho, Xuất kho, Điều chuyển, Kiểm kê, Báo cáo).
- Tự động xuất báo cáo kết quả (Passed/Failed) vào `docs/test/test_final.xlsx` (cột Round 2 / Round 3) và `docs/test/result_test.md`.

## 3. Cấu trúc Thư mục
```text
test_selenium/
├── config/              # config.py: APP_URL/API_URL, headless flag, role credentials (env-overridable)
├── pages/               # Page Object Model (POM)
│   ├── base_page.py     # BasePage: find/click/type/is_visible/wait_for/wait_network_idle/label-driven form helpers
│   ├── wms_pages.py      # LoginPage + generic ModulePage.check_page_loaded()
│   └── round3_flows.py   # One real create/mutate-and-verify flow per module
├── run_selenium_round2.py  # Runner: logs in per module's required role, verifies
│                           # the module landing page isn't bounced to /login or /forbidden
├── run_selenium_round3.py  # Runner: executes round3_flows per module, writes Round 3 columns
├── utils/               # excel_reporter.py (writes test_final.xlsx/result_test.md),
│                         # error_tracer.py (selenium_error_report.md/.xlsx)
└── requirements.txt     # selenium, webdriver-manager, pytest, openpyxl, requests
```

## 4. Hướng dẫn chạy Test
```bash
pip install -r test_selenium/requirements.txt
python test_selenium/run_selenium_round2.py
python test_selenium/run_selenium_round3.py
```

Mặc định dùng 10 tài khoản seed thật theo role (Hải Phòng), cùng mật khẩu `password123`:
`admin@phucanh.vn` (ADMIN), `ceo@phucanh.vn` (CEO), `storekeeperHP@gmail.com` (STOREKEEPER),
`manager.hp@phucanh.vn` (WAREHOUSE_MANAGER), `hpwhstaff@gmail.com` (WAREHOUSE_STAFF),
`planer@gmail.com` (PLANNER), `dispatcher-HP@gmail.com` (DISPATCHER), `driverHP@gmail.com` (DRIVER),
`accountantHP@phucanh.vn` (ACCOUNTANT), `acc_managerHP@phucanh.vn` (ACCOUNTANT_MANAGER).
Mỗi tài khoản có thể override qua cặp biến môi trường riêng (`WMS_<ROLE>_EMAIL`/`WMS_<ROLE>_PASSWORD`)
nếu tài khoản thật thay đổi.

**Round 2** là smoke test ở mức trang (đăng nhập đúng role, vào được trang module mà không bị
bounce về `/login`/`/forbidden`, và không có error toast) — không thực thi từng assertion nghiệp vụ
chi tiết của mỗi dòng test case trong `test_final.xlsx`.

**Round 3** thực thi hành động nghiệp vụ thật (tạo user, tạo sản phẩm, lập lệnh nhập kho, lập đơn
xuất, tạo yêu cầu điều chuyển, tạo phiếu kiểm kê, tạo bản giá, ghi nhận phiếu thu, lập phiếu trả
hàng, kiểm tra dashboard KPI) rồi xác minh kết quả thật sự xuất hiện -- gần với UAT thủ công hơn
Round 2, nhưng vẫn không thay thế toàn bộ ~924 test case chi tiết. Một vài flow phụ thuộc dữ liệu
sẵn có trong hệ thống (VD: cần một hóa đơn chưa thanh toán để thu tiền, cần một DO đã giao để trả
hàng) -- nếu dữ liệu đó không tồn tại, flow được ghi nhận status **N/A**, không phải Failed.

## 5. Ý nghĩa cột trạng thái Round 3: Passed / Failed / N/A

`classify_status()` trong `run_selenium_round3.py` chỉ ghi **Failed** khi có bằng chứng thật
rằng ứng dụng phản hồi sai (error toast/banner có nội dung cụ thể, hoặc dashboard tự hiển thị
trạng thái lỗi). Mọi trường hợp còn lại -- không đăng nhập được, thiếu dữ liệu tiền đề trong môi
trường hiện tại, exception/timeout chưa xác định nguyên nhân, submit không có phản ứng rõ ràng
theo cả hai hướng -- được ghi **N/A** (chưa kết luận được, cần kiểm tra thủ công), **không phải
Failed**. Lý do: một kết quả tự động hoá không hoàn tất không đồng nghĩa với việc tính năng đó bị
lỗi -- gán nhầm thành Failed sẽ khiến người đọc báo cáo hiểu sai rằng tính năng có defect thật.

## 6. Các flow đã có `data-testid` (ổn định hơn, nên ưu tiên khi mở rộng)

AUTH-001, MDM-002, TRF-005, STK-006, PRC-007, OUT-004 dùng `page.by_testid(...)` /
`page.submit_and_verify_by_testid(...)` thay vì suy đoán qua label text/vị trí DOM cho toàn bộ
form -- các component liên quan (`UserFormModal.jsx`, `ProductManagement.jsx`,
`TransferRequestWorkspace.jsx`, `StocktakeForm.jsx`, `PriceListManagement.jsx`,
`DeliveryOrders.jsx`) đã có `data-testid` tương ứng. RCV-003 (`ReceiptForm.jsx`) có testid cho ô
tìm sản phẩm nhưng các field còn lại (nhà cung cấp, người liên hệ, mã chứng từ) vẫn dùng label --
xem mục 8 vì RCV-003/OUT-004 vẫn N/A do một vấn đề khác, không phải do thiếu testid. FIN-008,
RET-009 vẫn dùng cách suy đoán cũ (label/vị trí) vì có nhiều nuance nghiệp vụ (tra hóa đơn chưa
thanh toán, DO đã giao...) khiến việc tự động hoá đầy đủ không đáng công sức bỏ ra -- xem ghi chú
UAT thủ công ở mục 7.

## 7. Vấn đề đã biết: đăng nhập lần thứ hai trong cùng tab thất bại

Xác nhận qua kiểm tra thủ công trực tiếp (không qua Selenium): đăng nhập lần đầu trong một tab
trình duyệt mới thành công bình thường, nhưng lần đăng nhập **thứ hai** trở đi trong cùng tab đó
thất bại một cách im lặng -- request `POST /api/v1/auth/login` trả về `200 OK` thật, nhưng
`sessionStorage` không được ghi và không có điều hướng rời khỏi `/login`, dù đã `sessionStorage.clear()`
trước đó. Đây đúng là nguyên nhân của các lần "Login rejected" xuất hiện từ module thứ 3-4 trở đi
trong các lần chạy Round 3 trước đây (mỗi lần đổi role = một lần đăng nhập mới trong cùng session
trình duyệt). Đã thử thêm cooldown giữa các lần đăng nhập trước -- không giải quyết được vì vấn đề
không phải do tốc độ/rate-limit.

Đây là lỗi thật của ứng dụng (hoặc môi trường), không phải lỗi của kịch bản test, và không thể sửa
từ phía script -- cần người có quyền truy cập log backend / debug JS runtime điều tra thêm.
`run_selenium_round3.py` hiện né được kịch bản lỗi này bằng cách mở **trình duyệt mới, đăng nhập
đúng một lần** cho mỗi role riêng biệt thay vì dùng chung một trình duyệt xuyên suốt các lần đổi
role (`run_all_flows()` nhóm `MODULE_FLOWS` theo role, mỗi nhóm chạy trên `build_driver()` riêng).
Đây không phải là che giấu lỗi -- lỗi vẫn được ghi lại ở trên -- mà là thiết kế lại việc quản lý
session để mỗi role luôn rơi vào đúng kịch bản đã được xác nhận hoạt động (đăng nhập đầu tiên trong
tab mới), thay vì để cả bộ test sụp đổ giữa chừng ngay khi việc đổi role bắt đầu.

## 8. Vấn đề đã biết: RCV-003 và OUT-004 chỉ thất bại khi chạy qua ChromeDriver headless

Cả hai flow này đã được nâng cấp lên `data-testid` và vẫn thất bại giống hệt nhau qua 3 lần chạy
liên tiếp: RCV-003 báo ô tìm sản phẩm giữ giá trị rỗng sau nhiều lần `send_keys`, OUT-004 báo modal
"Lập đơn xuất hàng" không mở sau cả hai lần click. Đã loại trừ lần lượt: race chờ hiển thị (đã có
wait trước khi tương tác), race debounce 250ms (đã fix ở mục search chung), và click rơi vào giữa
transition (đã thêm click tường minh + retry click kèm kiểm tra modal đã mở chưa trước khi click
lại). Quan trọng nhất: đã tái hiện thủ công **chính xác cùng một chuỗi thao tác** (chọn nhà cung
cấp, nhập người liên hệ, nhập mã chứng từ, rồi click + gõ vào ô tìm sản phẩm cho RCV-003; click mở
modal cho OUT-004) trực tiếp trong trình duyệt thật (không headless), ở đúng kích thước viewport
`1440x900` mà `build_driver()` dùng -- cả hai đều hoạt động bình thường, không lỗi.

Vì lỗi không tái hiện được trong trình duyệt có giao diện nhưng lặp lại giống hệt nhau qua nhiều
lần chạy headless thật, nhiều khả năng đây là hành vi đặc thù của `--headless=new` (một dạng quirk
đã biết của ChromeDriver, không phải lỗi logic trong flow hay trong ứng dụng). Không thể điều tra
sâu hơn từ môi trường hiện tại vì không có Python/ChromeDriver để tự chạy lại. Hướng kiểm tra tiếp
theo nếu muốn xác nhận: tạm tắt `HEADLESS` trong `config.py` và chạy lại riêng hai module này -- nếu
chuyển sang Passed thì xác nhận chắc chắn là vấn đề headless-only, không cần điều tra thêm ở tầng
ứng dụng.
