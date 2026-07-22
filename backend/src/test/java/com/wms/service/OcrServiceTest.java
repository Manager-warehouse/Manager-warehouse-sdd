package com.wms.service;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.service.user_configuration.*;
import com.wms.service.user_configuration.impl.*;
import com.wms.service.audit_trail.*;
import com.wms.service.access_control.*;
import com.wms.service.dealer_management.*;
import com.wms.service.dealer_management.impl.*;
import com.wms.service.billing_payment.*;
import com.wms.service.billing_payment.impl.*;
import com.wms.service.stock_receiving.*;
import com.wms.service.stock_control.*;
import com.wms.service.stock_control.impl.*;
import com.wms.service.notification_delivery.*;
import com.wms.service.notification_delivery.impl.*;
import com.wms.service.order_fulfillment.*;
import com.wms.service.order_fulfillment.impl.*;
import com.wms.service.price_management.*;
import com.wms.service.price_management.impl.*;
import com.wms.service.reporting_alerting.*;
import com.wms.service.reporting_alerting.impl.*;
import com.wms.service.return_disposal.*;
import com.wms.service.stock_counting.*;
import com.wms.service.fleet_management.*;
import com.wms.service.fleet_management.impl.*;
import com.wms.service.warehouse_location.*;
import com.wms.service.warehouse_location.impl.*;

import com.wms.dto.response.PaymentReceiptOcrResponse;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.access_control.User;
import com.wms.repository.dealer_management.DealerRepository;
import com.wms.service.billing_payment.impl.TesseractOcrServiceImpl;
import net.sourceforge.tess4j.ITesseract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OcrServiceTest {

    private TesseractOcrServiceImpl ocrService;

    @Mock
    private DealerRepository dealerRepository;

    @Mock
    private ITesseract tesseract;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Khởi tạo ocrService và set tesseract mock để bỏ qua init native DLL
        ocrService = new TesseractOcrServiceImpl(dealerRepository);
        ocrService.setTesseract(tesseract);
        ocrService.init();
    }

    @Test
    @DisplayName("processOcr — Trích xuất số tiền đi kèm từ khóa 'Số tiền' và khớp đại lý thành công")
    void processOcr_extractsAmountFromKeywordAndMatchesDealer() throws Exception {
        Dealer dealer = new Dealer();
        dealer.setId(3L);
        dealer.setCode("DL-MINH-TRI");
        dealer.setName("Dai Ly Minh Tri");
        dealer.setIsActive(true);

        when(dealerRepository.findAll()).thenReturn(List.of(dealer));

        String mockOcrText = "GIAO DICH CHUYEN KHOAN THANH CONG\n" +
                            "Nguoi chuyen: DAI LY MINH TRI\n" +
                            "So tai khoan: 19028392182\n" +
                            "So tien: 15.000.000 VND\n" +
                            "Noi dung: Thanh toan tien hang thang 6";
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn(mockOcrText);

        MockMultipartFile file = tinyPngFile("receipt.png");

        PaymentReceiptOcrResponse response = ocrService.processOcr(file, new User());

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("15000000.00");
        assertThat(response.getDealerId()).isEqualTo(3L);
        assertThat(response.getNotes()).contains("MINH TRI");
        assertThat(response.getConfidenceScore()).isEqualTo(0.95);
    }

    @Test
    @DisplayName("processOcr — Khớp đại lý có tên tiếng Việt có dấu dù OCR đọc ra chữ không dấu")
    void processOcr_matchesDealerWithVietnameseDiacriticsAgainstAsciiOcrText() throws Exception {
        Dealer dealer = new Dealer();
        dealer.setId(9L);
        dealer.setCode("DL-001");
        dealer.setName("Đại lý Máy Tính Phúc Anh");
        dealer.setIsActive(true);

        when(dealerRepository.findAll()).thenReturn(List.of(dealer));

        // Ảnh chụp màn hình ngân hàng OCR ra chữ không dấu, như thực tế Tesseract thường gặp
        String mockOcrText = "So tien giao dich: 15.000.000 VND\n" +
                            "Ngay: 17/07/2026\n" +
                            "Nguoi chuyen: DAI LY MAY TINH PHUC ANH\n" +
                            "Noi dung: Thanh toan tien hang thang 7";
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn(mockOcrText);

        MockMultipartFile file = tinyPngFile("receipt_diacritics.png");

        PaymentReceiptOcrResponse response = ocrService.processOcr(file, new User());

        assertThat(response.getDealerId()).isEqualTo(9L);
        assertThat(response.getConfidenceScore()).isEqualTo(0.95);
    }

    @Test
    @DisplayName("processOcr — Trích xuất số tiền có từ đệm giữa từ khóa và giá trị (VD: 'Số tiền giao dịch')")
    void processOcr_extractsAmountWithPaddingWordsAfterKeyword() throws Exception {
        when(dealerRepository.findAll()).thenReturn(List.of());

        String mockOcrText = "Số tiền giao dịch: 100.000 đ\nNội dung: thanh toan don hang";
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn(mockOcrText);

        MockMultipartFile file = tinyPngFile("padded_keyword.png");

        PaymentReceiptOcrResponse response = ocrService.processOcr(file, new User());

        assertThat(response.getAmount()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("processOcr — Trích xuất số tiền đi kèm hậu tố tiền tệ 'đ' và ngày giao dịch thành công")
    void processOcr_extractsAmountWithSuffixAndDate() throws Exception {
        Dealer dealer = new Dealer();
        dealer.setId(1L);
        dealer.setCode("DL-GENERIC");
        dealer.setName("Dai Ly Generic");
        dealer.setIsActive(true);

        when(dealerRepository.findAll()).thenReturn(List.of(dealer));

        String mockOcrText = "BIEN LAI GIAO DICH GIAY\n" +
                            "Ngay thuc hien: 17/06/2026 15:30:22\n" +
                            "So du: +500.000đ\n" +
                            "Noi dung: DL-GENERIC chuyen khoan";
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn(mockOcrText);

        MockMultipartFile file = tinyPngFile("bill.jpg");

        PaymentReceiptOcrResponse response = ocrService.processOcr(file, new User());

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("500000");
        assertThat(response.getPaymentDate()).isEqualTo(LocalDate.of(2026, 6, 17));
        assertThat(response.getDealerId()).isEqualTo(1L);
        assertThat(response.getNotes()).contains("GENERIC");
    }

    @Test
    @DisplayName("processOcr — Trích xuất số tiền có dấu cộng (+) nhận tiền thành công")
    void processOcr_extractsAmountWithPlusSign() throws Exception {
        when(dealerRepository.findAll()).thenReturn(List.of());

        String mockOcrText = "GD: +12,500,000 VND vao 18/06/2026. ND: thanh toan don hang.";
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn(mockOcrText);

        MockMultipartFile file = tinyPngFile("balance.png");

        PaymentReceiptOcrResponse response = ocrService.processOcr(file, new User());

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("12500000");
        assertThat(response.getPaymentDate()).isEqualTo(LocalDate.of(2026, 6, 18));
        assertThat(response.getDealerId()).isNull();
        assertThat(response.getNotes()).contains("KHONG RO DAI LY");
        assertThat(response.getConfidenceScore()).isEqualTo(0.60);
    }

    @Test
    @DisplayName("processOcr — Không khớp đại lý bằng số tài khoản ngân hàng khi tên/mã đại lý không xuất hiện")
    void processOcr_doesNotMatchDealerByBankAccountNumber() throws Exception {
        Dealer dealer = new Dealer();
        dealer.setId(7L);
        dealer.setCode("DL-BANKMATCH");
        dealer.setName("Cong Ty TNHH Khong Xuat Hien Ten");
        dealer.setIsActive(true);

        when(dealerRepository.findAll()).thenReturn(List.of(dealer));

        String mockOcrText = "Chuyen tien den STK: 0123456789 0\nSo tien: 2.000.000 VND";
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn(mockOcrText);

        MockMultipartFile file = tinyPngFile("bank_match.png");

        PaymentReceiptOcrResponse response = ocrService.processOcr(file, new User());

        assertThat(response.getDealerId()).isNull();
        assertThat(response.getNotes()).contains("KHONG RO DAI LY");
        assertThat(response.getConfidenceScore()).isEqualTo(0.60);
    }

    @Test
    @DisplayName("processOcr — Trả về lỗi 422 khi Tesseract ném exception hoặc không tìm thấy số tiền")
    void processOcr_throws422OnOcrErrorOrMissingAmount() throws Exception {
        // Tình huống 1: Tesseract bị lỗi
        when(tesseract.doOCR(any(BufferedImage.class))).thenThrow(new RuntimeException("Native memory error"));

        MockMultipartFile file = tinyPngFile("corrupted.png");

        assertThatThrownBy(() -> ocrService.processOcr(file, new User()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không thể xử lý hình ảnh này bằng OCR")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);

        // Tình huống 2: Tesseract chạy thành công nhưng text không có số tiền hợp lệ
        String mockTextNoAmount = "No amount info here. Just some text about warehouse.";
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn(mockTextNoAmount);

        assertThatThrownBy(() -> ocrService.processOcr(file, new User()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không thể nhận diện số tiền chuyển khoản hợp lệ")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("processOcr — Trả về lỗi 422 khi không thể đọc được dữ liệu ảnh (bytes không hợp lệ)")
    void processOcr_throws422OnUnreadableImageBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "not_an_image.png", "image/png", "this is not image data".getBytes()
        );

        assertThatThrownBy(() -> ocrService.processOcr(file, new User()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không thể đọc được tệp ảnh này")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private static MockMultipartFile tinyPngFile(String filename) throws IOException {
        BufferedImage image = new BufferedImage(80, 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 80, 30);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("file", filename, "image/png", out.toByteArray());
    }
}
