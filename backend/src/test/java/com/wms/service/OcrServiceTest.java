package com.wms.service;

import com.wms.dto.response.PaymentReceiptOcrResponse;
import com.wms.entity.Dealer;
import com.wms.entity.User;
import com.wms.repository.DealerRepository;
import com.wms.service.impl.TesseractOcrServiceImpl;
import net.sourceforge.tess4j.ITesseract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        
        // Mock kết quả quét chữ của Tesseract chứa từ khóa số tiền
        String mockOcrText = "GIAO DICH CHUYEN KHOAN THANH CONG\n" +
                            "Nguoi chuyen: DAI LY MINH TRI\n" +
                            "So tai khoan: 19028392182\n" +
                            "So tien: 15.000.000 VND\n" +
                            "Noi dung: Thanh toan tien hang thang 6";
        when(tesseract.doOCR(any(File.class))).thenReturn(mockOcrText);

        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.png", "image/png", "dummy-image-content".getBytes()
        );

        PaymentReceiptOcrResponse response = ocrService.processOcr(file, new User());

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("15000000.00");
        assertThat(response.getDealerId()).isEqualTo(3L);
        assertThat(response.getNotes()).contains("MINH TRI");
        assertThat(response.getConfidenceScore()).isEqualTo(0.95);
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
        
        // Mock kết quả quét chữ chứa đuôi tiền tệ 'đ' và ngày tháng định dạng dd/mm/yyyy
        String mockOcrText = "BIEN LAI GIAO DICH GIAY\n" +
                            "Ngay thuc hien: 17/06/2026 15:30:22\n" +
                            "So du: +500.000đ\n" +
                            "Noi dung: DL-GENERIC chuyen khoan";
        when(tesseract.doOCR(any(File.class))).thenReturn(mockOcrText);

        MockMultipartFile file = new MockMultipartFile(
                "file", "bill.jpg", "image/jpeg", "dummy-image-content".getBytes()
        );

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
        
        // Mock biến động số dư Vietcombank: "+12,500,000 VND"
        String mockOcrText = "GD: +12,500,000 VND vao 18/06/2026. ND: thanh toan don hang.";
        when(tesseract.doOCR(any(File.class))).thenReturn(mockOcrText);

        MockMultipartFile file = new MockMultipartFile(
                "file", "balance.png", "image/png", "dummy-image-content".getBytes()
        );

        PaymentReceiptOcrResponse response = ocrService.processOcr(file, new User());

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("12500000");
        assertThat(response.getPaymentDate()).isEqualTo(LocalDate.of(2026, 6, 18));
        assertThat(response.getDealerId()).isNull(); // Không khớp dealer nào
        assertThat(response.getNotes()).contains("KHONG RO DAI LY");
        assertThat(response.getConfidenceScore()).isEqualTo(0.60);
    }

    @Test
    @DisplayName("processOcr — Trả về lỗi 422 khi Tesseract ném exception hoặc không tìm thấy số tiền")
    void processOcr_throws422OnOcrErrorOrMissingAmount() throws Exception {
        // Tình huống 1: Tesseract bị lỗi
        when(tesseract.doOCR(any(File.class))).thenThrow(new RuntimeException("Native memory error"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "corrupted.png", "image/png", "dummy-image-content".getBytes()
        );

        assertThatThrownBy(() -> ocrService.processOcr(file, new User()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không thể xử lý hình ảnh này bằng OCR")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);

        // Tình huống 2: Tesseract chạy thành công nhưng text không có số tiền hợp lệ
        String mockTextNoAmount = "No amount info here. Just some text about warehouse.";
        when(tesseract.doOCR(any(File.class))).thenReturn(mockTextNoAmount);

        assertThatThrownBy(() -> ocrService.processOcr(file, new User()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không thể nhận diện số tiền chuyển khoản hợp lệ")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
