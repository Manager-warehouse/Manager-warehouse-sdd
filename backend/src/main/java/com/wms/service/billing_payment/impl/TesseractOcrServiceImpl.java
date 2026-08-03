package com.wms.service.billing_payment.impl;


import com.wms.dto.response.PaymentReceiptOcrResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.dealer_management.Dealer;
import com.wms.repository.dealer_management.DealerRepository;
import com.wms.service.billing_payment.OcrService;
import com.wms.util.OcrImagePreprocessor;
import com.wms.util.OcrTextParser;
import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TesseractOcrServiceImpl implements OcrService {

    private static final String UNREADABLE_IMAGE_MESSAGE =
            "Không thể đọc được tệp ảnh này. Vui lòng kiểm tra định dạng ảnh hoặc nhập tay.";

    private final DealerRepository dealerRepository;
    private ITesseract tesseract;
    private boolean isOcrReady = false;

    // Setter hỗ trợ unit test inject Mock Tesseract
    public void setTesseract(ITesseract tesseract) {
        this.tesseract = tesseract;
        this.isOcrReady = tesseract != null;
    }

    @PostConstruct
    public void init() {
        if (this.tesseract != null) {
            log.info("Tesseract OCR initialized via setTesseract (testing/mocking).");
            return;
        }
        try {
            // Ưu tiên dùng tessdata của hệ thống (cài qua apk trong Docker)
            // Fallback sang ./tessdata cho môi trường dev local
            String tessdataPath = resolveSystemTessdataPath();

            Tesseract impl = new Tesseract();
            impl.setDatapath(tessdataPath);
            impl.setLanguage("vie+eng");
            this.tesseract = impl;
            this.isOcrReady = true;
            log.info("Tesseract OCR initialized successfully with path: {} (eng+vie)", tessdataPath);
        } catch (Throwable e) {
            log.warn("Failed to initialize Tesseract OCR. Error: {}", e.getMessage());
            this.isOcrReady = false;
        }
    }

    /**
     * Tìm đường dẫn tessdata hợp lệ theo thứ tự ưu tiên:
     * 1. /usr/share/tessdata (Alpine apk install — môi trường production Docker)
     * 2. /usr/share/tesseract-ocr/5/tessdata (Debian/Ubuntu apt install)
     * 3. ./tessdata (relative — môi trường dev local với traineddata tải sẵn)
     */
    private String resolveSystemTessdataPath() {
        String[] candidates = {
            "/usr/share/tessdata",
            "/usr/share/tesseract-ocr/5/tessdata",
            "/usr/share/tesseract-ocr/4.00/tessdata"
        };
        for (String path : candidates) {
            File dir = new File(path);
            File engFile = new File(dir, "eng.traineddata");
            if (dir.isDirectory() && engFile.exists()) {
                log.info("Found tessdata at system path: {}", path);
                return path;
            }
        }
        // Fallback: local dev — tạo thư mục và để dev tự đặt traineddata vào
        log.warn("System tessdata not found. Falling back to ./tessdata for local development.");
        File localDir = new File("tessdata");
        localDir.mkdirs();
        return localDir.getAbsolutePath();
    }

    @Override
    public PaymentReceiptOcrResponse processOcr(MultipartFile file, User actor) {
        String resultText = extractRawText(file);
        return parseOcrText(resultText);
    }

    @Override
    public String extractRawText(MultipartFile file) {
        if (!isOcrReady || tesseract == null) {
            log.warn("Tesseract OCR service is not ready/initialized on this system.");
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Dịch vụ OCR hiện tại không hoạt động trên máy chủ. Vui lòng nhập thông tin thủ công.");
        }

        BufferedImage sourceImage = readImage(file);

        try {
            // Ảnh chỉ được giữ trong bộ nhớ để xử lý OCR, không ghi ra đĩa.
            BufferedImage processedImage = OcrImagePreprocessor.preprocess(sourceImage);
            String resultText = tesseract.doOCR(processedImage);
            log.info("OCR Result Text Length: {}", resultText.length());
            return resultText;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Throwable e) {
            log.warn("Actual OCR processing failed. Error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Không thể xử lý hình ảnh này bằng OCR. Vui lòng kiểm tra định dạng ảnh hoặc nhập tay. Chi tiết: " + e.getMessage());
        }
    }

    private BufferedImage readImage(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, UNREADABLE_IMAGE_MESSAGE);
            }
            return image;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, UNREADABLE_IMAGE_MESSAGE);
        }
    }

    private PaymentReceiptOcrResponse parseOcrText(String text) {
        // Chuẩn hóa văn bản: xóa khoảng trắng thừa, đưa về lowercase
        String cleanText = text.replaceAll("\\r?\\n", " ").replaceAll("\\s+", " ");
        String lowercaseText = cleanText.toLowerCase();

        BigDecimal amount = OcrTextParser.extractAmount(cleanText, lowercaseText);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("OCR parse failed: Could not extract any valid payment amount from OCR text.");
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Không thể nhận diện số tiền chuyển khoản hợp lệ từ hóa đơn này. Vui lòng nhập thông tin thủ công.");
        }

        LocalDate paymentDate = OcrTextParser.extractPaymentDate(cleanText);
        DealerMatchResult dealerMatch = matchDealer(lowercaseText);

        return PaymentReceiptOcrResponse.builder()
                .amount(amount)
                .paymentDate(paymentDate)
                .dealerId(dealerMatch.dealerId())
                .notes(dealerMatch.notes())
                .confidenceScore(dealerMatch.confidenceScore())
                .build();
    }

    private DealerMatchResult matchDealer(String lowercaseText) {
        List<Dealer> dealers = dealerRepository.findAll();

        DealerMatchResult byNameOrCode = matchByNameOrCode(lowercaseText, dealers);
        if (byNameOrCode != null) {
            return byNameOrCode;
        }

        return new DealerMatchResult(null, "CK TIEN HANG - KHONG RO DAI LY (OCR)", 0.60);
    }

    private DealerMatchResult matchByNameOrCode(String lowercaseText, List<Dealer> dealers) {
        // So khớp trên bản đã bỏ dấu tiếng Việt: Tesseract thường đọc sai/rớt dấu thanh trên
        // ảnh chụp màn hình ứng dụng ngân hàng, nên so khớp có dấu chính xác tuyệt đối là quá chặt.
        String foldedOcrText = OcrTextParser.foldToAsciiAlphanumeric(lowercaseText);
        for (Dealer dealer : dealers) {
            if (!Boolean.TRUE.equals(dealer.getIsActive())) {
                continue;
            }
            String dealerCode = dealer.getCode().toLowerCase();
            String foldedDealerName = OcrTextParser.foldToAsciiAlphanumeric(dealer.getName());

            if (foldedOcrText.contains(foldedDealerName)
                    || lowercaseText.contains(dealerCode)
                    || lowercaseText.contains(dealerCode.replace("-", " "))) {
                return new DealerMatchResult(dealer.getId(), dealerNotes(dealer), 0.95);
            }
        }
        return null;
    }

    private String dealerNotes(Dealer dealer) {
        return "CK TIEN HANG - " + dealer.getName().toUpperCase() + " - GIAO DICH OCR";
    }

    private record DealerMatchResult(Long dealerId, String notes, double confidenceScore) {
    }
}
