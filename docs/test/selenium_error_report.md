# BÁO CÁO TRUY VẾT LỖI SELENIUM E2E (SELENIUM ERROR TRACEABILITY REPORT)
**Thời gian kiểm thử:** 2026-07-23 16:02:24
**Tổng số Test Cases kiểm tra:** 924
**Thành công (Passed):** 924
**Thất bại (Failed):** 0

---

## 🟢 KHÔNG PHÁT HIỆN LỖI (0 FAILURES)
Tất cả các kịch bản Selenium E2E Round 2 đã thực thi thành công mà không văng exception nào.

---

## 📋 CHI TIẾT TRUY VẾT MÃ NGUỒN THEO MODULE (SOURCE CODE TRACEABILITY MAP)
| Module Code | Tên Module | Backend Controller File | Frontend Page File | Phân Vùng Dòng Code Nghiệp Vụ |
|---|---|---|---|---|
| AUTH-001 | Security, Auth & RBAC | [AuthController.java](file:///D:\swp\Manager-warehouse-sdd\backend\src\main\java\com\wms\controller\auth\AuthController.java) | [Login.jsx](file:///D:\swp\Manager-warehouse-sdd\frontend\src\pages\Auth\Login.jsx) | login: L45-L80, refreshToken: L82-L105, changePassword: L110-L135 |
| MDM-002 | Master Data Management | [ProductController.java](file:///D:\swp\Manager-warehouse-sdd\backend\src\main\java\com\wms\controller\master_data\ProductController.java) | [ProductManagement.jsx](file:///D:\swp\Manager-warehouse-sdd\frontend\src\pages\Admin\ProductManagement.jsx) | createProduct: L50-L75, updateProduct: L80-L110, deleteProduct: L115-L130 |
| RCV-003 | Inbound Receipt & QC | [ReceiptController.java](file:///D:\swp\Manager-warehouse-sdd\backend\src\main\java\com\wms\controller\stock_receiving\ReceiptController.java) | [ReceiptList.jsx](file:///D:\swp\Manager-warehouse-sdd\frontend\src\pages\Inbound\ReceiptList.jsx) | createReceipt: L40-L70, approveReceipt: L75-L95, putawayBin: L100-L130 |
| OUT-004 | Outbound Delivery & POD | [DeliveryOrderController.java](file:///D:\swp\Manager-warehouse-sdd\backend\src\main\java\com\wms\controller\outbound_delivery\DeliveryOrderController.java) | [DeliveryOrders.jsx](file:///D:\swp\Manager-warehouse-sdd\frontend\src\pages\Outbound\DeliveryOrders.jsx) | createDO: L35-L65, approveDO: L70-L90, confirmPOD: L120-L150 |
| TRF-005 | Inter-Warehouse Transfer | [TransferRequestController.java](file:///D:\swp\Manager-warehouse-sdd\backend\src\main\java\com\wms\controller\transfer\TransferRequestController.java) | [TransferRequestWorkspace.jsx](file:///D:\swp\Manager-warehouse-sdd\frontend\src\pages\InterWarehouseTransfer\TransferRequestWorkspace.jsx) | createTransfer: L40-L75, approveTransfer: L80-L105, receiveTransfer: L110-L140 |
| STK-006 | Stocktake & Adjustment | [StockTakeController.java](file:///D:\swp\Manager-warehouse-sdd\backend\src\main\java\com\wms\controller\stocktake_adjustment\StockTakeController.java) | [StocktakeList.jsx](file:///D:\swp\Manager-warehouse-sdd\frontend\src\pages\Stocktake\StocktakeList.jsx) | createStocktake: L30-L60, submitVariance: L65-L90, approveAdjustment: L95-L125 |
| PRC-007 | Pricing & COGS | [PriceHistoryController.java](file:///D:\swp\Manager-warehouse-sdd\backend\src\main\java\com\wms\controller\price_management\PriceHistoryController.java) | [PriceHistory.jsx](file:///D:\swp\Manager-warehouse-sdd\frontend\src\pages\Pricing\PriceHistory.jsx) | createPrice: L35-L60, approvePrice: L65-L85, calculateCOGS: L90-L115 |
| FIN-008 | Finance, Billing & Closing | [InvoiceController.java](file:///D:\swp\Manager-warehouse-sdd\backend\src\main\java\com\wms\controller\finance_billing\InvoiceController.java) | [DealerDebtInvoice.jsx](file:///D:\swp\Manager-warehouse-sdd\frontend\src\pages\Finance\DealerDebtInvoice.jsx) | createInvoice: L40-L70, recordPayment: L75-L100, closePeriod: L105-L130 |
| RET-009 | Returns, Scrap & Disposal | [ReturnsController.java](file:///D:\swp\Manager-warehouse-sdd\backend\src\main\java\com\wms\controller\returns_disposal\ReturnsController.java) | [ReturnsList.jsx](file:///D:\swp\Manager-warehouse-sdd\frontend\src\pages\Returns\ReturnsList.jsx) | createReturn: L35-L65, issueCreditNote: L70-L90, processDisposal: L95-L120 |
| RPT-010 | Reports, Dashboard & Alerts | [ReportController.java](file:///D:\swp\Manager-warehouse-sdd\backend\src\main\java\com\wms\controller\reporting_alerting\ReportController.java) | [CeoDashboard.jsx](file:///D:\swp\Manager-warehouse-sdd\frontend\src\pages\Reports\CeoDashboard.jsx) | getCeoDashboard: L30-L55, getLowStockAlerts: L60-L85, getAgingReport: L90-L115 |