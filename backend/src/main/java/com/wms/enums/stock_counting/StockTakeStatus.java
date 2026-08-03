package com.wms.enums.stock_counting;


/**
 * Trạng thái phiếu kiểm kê hàng hóa.
 *
 * DRAFT            — Thủ kho vừa tạo, chưa bắt đầu đếm
 * IN_PROGRESS      — Đang kiểm kê (location bị khóa)
 * PENDING_APPROVAL — Thủ kho hoàn tất đếm, chờ Trưởng kho duyệt
 * APPROVED         — Trưởng kho đã duyệt, tồn kho đã được cập nhật
 * REJECTED         — Trưởng kho trả lại, Thủ kho có thể sửa và nộp lại
 * CANCELLED        — Đã hủy (chỉ từ DRAFT hoặc IN_PROGRESS)
 */
public enum StockTakeStatus {
    DRAFT,
    IN_PROGRESS,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED
}
