# Feature Specification: Quan ly danh muc va ton kho

**Feature Branch**: `001-warehouse-inventory-control`

**Created**: 2026-05-19

**Status**: Draft

**Input**: User description: "Phan he quan ly danh muc va ton kho ho tro theo doi so luong thuc te tai Kho trung tam Hai Phong cung 2 kho khu vuc, cho phep thiet lap dinh muc toi thieu va ghi nhat ky moi thao tac chinh sua. Doi voi nghiep vu nhap kho thanh pham, he thong yeu cau lien ket voi Lenh san xuat, chi cho phep nhap san pham dat chat luong (QC), tu dong cong ton kho va theo doi luong nguyen lieu dau vao so voi san pham dau ra. Quy trinh dieu chuyen noi bo giua cac kho se thuc hien dong thoi viec tru kho nguon va cong kho dich, dong thoi tu dong tam dung giao dich neu xay ra su co mang de tranh sai lech du lieu. Khi xuat kho cho 50 dai ly, he thong se chan xuat hang neu vuot qua ton thuc te hoac dai ly vuot han muc no, tu dong phat sinh cong no khi thanh cong va ghi log lai cac no luc that bai. Cuoi cung, he thong cung cap bao cao thoi gian thuc va dashboard truc quan cho Ban Giam doc, phan quyen khong cho nhan vien kho xem du lieu luong, nhan su va ghi log lai toan bo thao tac truy xuat bao cao."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Theo doi danh muc va ton kho thuc te (Priority: P1)

Nhan vien kho quan ly danh muc hang hoa, xem so luong thuc te theo tung kho, thiet lap dinh muc toi thieu va kiem tra lich su chinh sua de dam bao ton kho van hanh chinh xac.

**Why this priority**: Day la nen tang cho moi nghiep vu nhap, dieu chuyen, xuat va bao cao ton kho.

**Independent Test**: Tao/cap nhat mot mat hang, gan dinh muc toi thieu tai Kho trung tam Hai Phong va 2 kho khu vuc, sau do xac minh ton kho hien thi dung theo kho va moi thay doi deu co nhat ky.

**Acceptance Scenarios**:

1. **Given** mot mat hang da duoc tao, **When** nhan vien kho cap nhat dinh muc toi thieu cho tung kho, **Then** he thong luu dinh muc rieng theo kho va ghi nhat ky nguoi thuc hien, thoi diem, gia tri cu va gia tri moi.
2. **Given** so luong thuc te cua mot mat hang thap hon dinh muc toi thieu, **When** nguoi dung xem danh sach ton kho, **Then** he thong danh dau mat hang can bo sung tai dung kho bi thieu.

---

### User Story 2 - Nhap kho thanh pham theo lenh san xuat va QC (Priority: P1)

Nhan vien kho nhap thanh pham vao kho khi co Lenh san xuat hop le, chi nhan hang dat QC, tu dong cong ton va doi chieu nguyen lieu dau vao voi thanh pham dau ra.

**Why this priority**: Nhap kho thanh pham anh huong truc tiep den ton kho ban duoc va truy xuat chat luong san xuat.

**Independent Test**: Thuc hien nhap mot lo thanh pham gan voi Lenh san xuat co ket qua QC dat va xac minh ton kho tang, nhat ky duoc ghi, bang doi chieu dau vao/dau ra duoc cap nhat.

**Acceptance Scenarios**:

1. **Given** Lenh san xuat da hoan tat va lo thanh pham dat QC, **When** nhan vien kho xac nhan nhap kho, **Then** he thong cong so luong thanh pham vao kho duoc chon va ghi lien ket den Lenh san xuat.
2. **Given** lo thanh pham chua dat QC hoac thieu ket qua QC, **When** nhan vien kho co gang nhap kho, **Then** he thong chan thao tac va ghi nhat ky lan thu khong thanh cong.
3. **Given** lenh san xuat co dinh muc nguyen lieu dau vao, **When** thanh pham duoc nhap kho, **Then** he thong hien thi doi chieu so luong nguyen lieu dau vao voi san pham dau ra cho lenh do.

---

### User Story 3 - Dieu chuyen noi bo giua cac kho (Priority: P2)

Nhan vien kho dieu chuyen hang giua Kho trung tam Hai Phong va 2 kho khu vuc, trong do ton kho nguon bi tru va ton kho dich duoc cong nhu mot giao dich thong nhat.

**Why this priority**: Dieu chuyen noi bo giup phan bo hang hoa dung noi can ban, nhung sai lech giua kho nguon va kho dich se gay rui ro van hanh.

**Independent Test**: Tao mot giao dich dieu chuyen hop le tu kho nguon sang kho dich va xac minh ton kho nguon giam, ton kho dich tang, lich su giao dich the hien mot ma giao dich duy nhat.

**Acceptance Scenarios**:

1. **Given** kho nguon co du hang, **When** nhan vien kho xac nhan dieu chuyen sang kho dich, **Then** he thong thuc hien dong thoi tru kho nguon va cong kho dich voi cung mot ma giao dich.
2. **Given** xay ra su co mang trong luc dieu chuyen, **When** giao dich chua duoc xac nhan tron ven, **Then** he thong tam dung giao dich, khong ghi nhan trang thai nua voi, va thong bao can xu ly lai hoac doi soat.

---

### User Story 4 - Xuat kho cho dai ly va kiem soat cong no (Priority: P2)

Nhan vien kinh doanh hoac kho xuat hang cho toi da 50 dai ly, he thong chi cho phep xuat khi con du ton thuc te va dai ly chua vuot han muc no, dong thoi phat sinh cong no sau khi xuat thanh cong.

**Why this priority**: Nghiep vu xuat kho gan voi doanh thu va rui ro cong no, nen can chan sai ngay tai thoi diem thao tac.

**Independent Test**: Thu xuat hang cho dai ly trong ba truong hop: du dieu kien, vuot ton thuc te, vuot han muc no; xac minh chi truong hop du dieu kien duoc xuat va phat sinh cong no.

**Acceptance Scenarios**:

1. **Given** dai ly con han muc no va kho co du ton thuc te, **When** nhan vien xac nhan xuat hang, **Then** he thong tru ton kho, tao chung tu xuat va ghi nhan cong no cho dai ly.
2. **Given** so luong xuat lon hon ton thuc te, **When** nhan vien xac nhan xuat hang, **Then** he thong chan thao tac, khong tru ton va ghi log lan xuat that bai.
3. **Given** dai ly da vuot han muc no, **When** nhan vien xac nhan xuat hang, **Then** he thong chan thao tac, hien ly do bi chan va ghi log lan xuat that bai.

---

### User Story 5 - Bao cao thoi gian thuc va phan quyen truy xuat (Priority: P3)

Ban Giam doc xem dashboard truc quan va bao cao thoi gian thuc ve danh muc, ton kho, nhap kho, dieu chuyen, xuat kho, cong no va canh bao dinh muc; nhan vien kho chi xem du lieu phu hop voi vai tro.

**Why this priority**: Bao cao giup dieu hanh va kiem soat, nhung phai bao ve du lieu luong va nhan su ngoai pham vi kho.

**Independent Test**: Dang nhap bang vai tro Ban Giam doc va nhan vien kho, xac minh dashboard hien dung pham vi du lieu, nhan vien kho bi chan khi truy cap du lieu luong/nhan su, va moi lan xem bao cao deu co log.

**Acceptance Scenarios**:

1. **Given** Ban Giam doc co quyen xem bao cao, **When** mo dashboard ton kho, **Then** he thong hien so lieu moi nhat ve ton theo kho, canh bao duoi dinh muc, nhap/xuat/dieu chuyen va cong no dai ly.
2. **Given** nhan vien kho khong co quyen xem du lieu luong va nhan su, **When** co gang truy cap bao cao lien quan, **Then** he thong tu choi truy cap va ghi log su kien.
3. **Given** nguoi dung co quyen xem mot bao cao, **When** bao cao duoc mo hoac loc du lieu, **Then** he thong ghi log nguoi xem, thoi diem, loai bao cao va bo loc chinh.

### Edge Cases

- Kho nguon khong du ton thuc te tai thoi diem dieu chuyen hoac xuat kho.
- Dinh muc toi thieu khac nhau giua Kho trung tam Hai Phong va tung kho khu vuc.
- Lenh san xuat bi huy, chua hoan tat, hoac bi nhap kho trung lap.
- Lo thanh pham co mot phan dat QC va mot phan khong dat QC.
- Mat ket noi trong qua trinh dieu chuyen noi bo, nhap kho hoac xuat kho.
- Dai ly dat dung han muc no sau giao dich hien tai.
- Bao cao khong co du lieu trong khoang thoi gian duoc chon.
- Nguoi dung het phien dang nhap khi dang xem hoac loc bao cao.
- Nhieu nguoi cung thao tac tren cung mot mat hang trong thoi gian gan nhau.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST manage product/category master data used by inventory, production receipt, transfer, outbound shipment and reporting workflows.
- **FR-002**: System MUST track actual inventory quantity separately for Kho trung tam Hai Phong and 2 regional warehouses.
- **FR-003**: System MUST allow authorized users to define minimum stock thresholds per item and per warehouse.
- **FR-004**: System MUST flag inventory items below their configured minimum threshold for the relevant warehouse.
- **FR-005**: System MUST record an audit log for every create, update, delete or threshold change affecting master data or inventory quantities.
- **FR-006**: System MUST require each finished-goods receipt to reference a valid Production Order.
- **FR-007**: System MUST allow finished-goods receipt only for quantities that have passed QC.
- **FR-008**: System MUST reject finished-goods receipt attempts for missing, failed or unapproved QC results and record the failed attempt.
- **FR-009**: System MUST increase actual inventory in the selected receiving warehouse when a finished-goods receipt succeeds.
- **FR-010**: System MUST track input material quantities against finished-goods output for each linked Production Order.
- **FR-011**: System MUST execute internal warehouse transfer as one complete business transaction that decreases source inventory and increases destination inventory together.
- **FR-012**: System MUST pause an internal transfer when a network incident prevents the full transfer from being confirmed.
- **FR-013**: System MUST prevent partial or inconsistent transfer results from being shown as completed.
- **FR-014**: System MUST support outbound shipments to a managed list of 50 dealers.
- **FR-015**: System MUST block outbound shipment when requested quantity exceeds actual available inventory in the source warehouse.
- **FR-016**: System MUST block outbound shipment when the dealer would exceed the approved debt limit.
- **FR-017**: System MUST decrease inventory and create dealer receivable debt when an outbound shipment succeeds.
- **FR-018**: System MUST log failed outbound shipment attempts with reason, actor, dealer, item, quantity and time.
- **FR-019**: System MUST provide real-time management reports and dashboard views for inventory, minimum-stock alerts, production receipts, transfers, outbound shipments and dealer receivables.
- **FR-020**: System MUST prevent warehouse staff from viewing salary and HR data.
- **FR-021**: System MUST log every report access attempt, including successful access, denied access and report filtering actions.
- **FR-022**: System MUST expose clear user-facing status for pending, paused, rejected and completed inventory transactions.

### User Experience Requirements *(include for user-facing features)*

- **UX-001**: Primary inventory flows MUST use consistent terms for warehouse, actual inventory, minimum threshold, QC status, Production Order, dealer and debt limit.
- **UX-002**: System MUST show clear empty, loading, error and success states for inventory lists, transaction forms, transfer status and dashboard reports.
- **UX-003**: User-facing changes MUST support keyboard navigation, visible focus, readable labels and responsive layouts for warehouse and management users.
- **UX-004**: Blocking messages MUST explain the business reason without exposing restricted salary, HR or private financial details beyond the user's permission.

### Key Entities *(include if feature involves data)*

- **Product/Category**: Master data item tracked in inventory, with identity, classification, status and units of measure.
- **Warehouse**: Storage location, including Kho trung tam Hai Phong and 2 regional warehouses, each with separate stock levels and thresholds.
- **Inventory Balance**: Actual quantity of a product at a warehouse, including threshold status and last change information.
- **Minimum Stock Threshold**: Warehouse-specific minimum quantity for a product that triggers replenishment attention.
- **Inventory Audit Log**: Immutable record of user action, timestamp, object changed, old value, new value and reason where applicable.
- **Production Order**: Manufacturing record linked to finished-goods receipt and input/output material comparison.
- **QC Result**: Quality status that determines whether finished goods are eligible for receipt.
- **Finished-Goods Receipt**: Transaction that adds QC-passed products to a warehouse and links them to a Production Order.
- **Material Input/Output Trace**: Record comparing materials consumed or issued with finished goods received for a Production Order.
- **Warehouse Transfer**: Internal transaction moving stock from source warehouse to destination warehouse with paused/completed/rejected status.
- **Dealer**: Customer outlet eligible for outbound shipments, with assigned debt limit and current receivable balance.
- **Outbound Shipment**: Transaction that decreases warehouse stock and creates dealer receivable debt when successful.
- **Dealer Receivable**: Debt record generated by successful outbound shipment and used for debt-limit checks.
- **Report Access Log**: Record of report view, filter and denied-access attempts.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of inventory-changing transactions produce an audit log containing actor, time, affected item, affected warehouse and outcome.
- **SC-002**: Warehouse users can identify all items below minimum threshold for any warehouse in under 30 seconds.
- **SC-003**: 100% of finished-goods receipts without approved QC are blocked before inventory is increased.
- **SC-004**: 100% of internal transfers affected by a network incident are left in a paused or unresolved state rather than marked completed with mismatched source/destination quantities.
- **SC-005**: 100% of outbound shipment attempts that exceed actual stock or dealer debt limit are blocked and logged.
- **SC-006**: Successful outbound shipments create dealer receivable debt in the same business event visible to authorized users.
- **SC-007**: Management dashboard reflects completed inventory transactions within 5 seconds under normal operating conditions.
- **SC-008**: Warehouse staff have 0 successful accesses to salary or HR report data during permission testing.
- **SC-009**: Ban Giam doc can view inventory, transfer, outbound and receivable summaries for all warehouses and dealers from a single dashboard view.

## Assumptions

- The 2 regional warehouses are fixed for the first release, while their display names can be configured during setup.
- Only authorized warehouse, business and management roles can perform or view inventory-related workflows.
- Production Orders, QC results, dealer list, debt limits and current receivable balances are available to this feature as business records.
- "Real-time" reporting means business users see completed transaction effects within seconds under normal operating conditions.
- Failed and denied attempts are retained in audit/report logs according to the organization's normal audit retention policy.
