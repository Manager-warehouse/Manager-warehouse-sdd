package com.wms.service.impl;

import com.wms.dto.response.PaymentReceiptOcrResponse;
import com.wms.entity.Dealer;
import com.wms.entity.User;
import com.wms.repository.DealerRepository;
import com.wms.service.OcrService;
import com.wms.util.OcrImagePreprocessor;
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

            return parseOcrText(resultText);
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

        BigDecimal amount = extractAmount(cleanText, lowercaseText);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("OCR parse failed: Could not extract any valid payment amount from OCR text.");
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Không thể nhận diện số tiền chuyển khoản hợp lệ từ hóa đơn này. Vui lòng nhập thông tin thủ công.");
        }

        LocalDate paymentDate = extractPaymentDate(cleanText);
        DealerMatchResult dealerMatch = matchDealer(lowercaseText);

        return PaymentReceiptOcrResponse.builder()
                .amount(amount)
                .paymentDate(paymentDate)
                .dealerId(dealerMatch.dealerId())
                .notes(dealerMatch.notes())
                .confidenceScore(dealerMatch.confidenceScore())
                .build();
    }

    private BigDecimal extractAmount(String cleanText, String lowercaseText) {
        // Cách 1: Tìm số tiền đi kèm sau từ khóa "số tiền" hoặc "amount", cho phép tối đa
        // 3 từ đệm ở giữa (ví dụ "Số tiền giao dịch: 100.000")
        java.util.regex.Pattern keywordPattern = java.util.regex.Pattern.compile(
            "(?:so\\s+tien|số\\s+tiền|amount|tong\\s+tien|tổng\\s+tiền|so\\s+tien\\s+chuyen|"
            + "số\\s+tiền\\s+chuyển|gia\\s+tri|giá\\s+trị)(?:\\s+\\p{L}+){0,3}\\s*[:\\-\\. ]*\\s*"
            + "([+\\-\\s]*[\\d.,]+(?:\\s*(?:vnd|đ|d|dong|đồng))?)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher keywordMatcher = keywordPattern.matcher(lowercaseText);
        if (keywordMatcher.find()) {
            BigDecimal amount = extractNumberFromText(keywordMatcher.group(1));
            if (amount != null) {
                return amount;
            }
        }

        // Cách 2: Nếu chưa tìm được, tìm số đi kèm đơn vị tiền tệ "vnd", "đ", "dong", "đồng" ở phía sau
        BigDecimal byCurrencySuffix = maxMatchAtLeast(lowercaseText,
                "([\\d.,]+)\\s*(?:vnd|đ|dong|đồng)\\b", null);
        if (byCurrencySuffix != null) {
            return byCurrencySuffix;
        }

        // Cách 3: Nếu vẫn chưa tìm được, tìm số có dấu cộng (+) đằng trước (phổ biến trên screenshot
        // biến động số dư / giao dịch thành công)
        BigDecimal byPlusSign = maxMatchAtLeast(lowercaseText, "\\+\\s*([\\d.,]+)", null);
        if (byPlusSign != null) {
            return byPlusSign;
        }

        // Cách 4: Fallback cuối cùng - tìm số lớn nhất có định dạng phân cách nghìn và độ dài hợp lý,
        // giới hạn dưới 1 tỷ để tránh nhầm với số tài khoản/mã giao dịch
        return maxMatchAtLeast(cleanText, "\\b\\d{1,3}(?:[.,]\\d{3})+\\b", new BigDecimal("1000000000"));
    }

    private BigDecimal maxMatchAtLeast(String text, String regex, BigDecimal exclusiveUpperBound) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex,
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        BigDecimal maxVal = BigDecimal.ZERO;
        while (matcher.find()) {
            BigDecimal val = extractNumberFromText(matcher.group(matcher.groupCount() > 0 ? 1 : 0));
            if (val == null || val.compareTo(maxVal) <= 0 || val.compareTo(new BigDecimal("1000")) < 0) {
                continue;
            }
            if (exclusiveUpperBound != null && val.compareTo(exclusiveUpperBound) >= 0) {
                continue;
            }
            maxVal = val;
        }
        return maxVal.compareTo(BigDecimal.ZERO) > 0 ? maxVal : null;
    }

    private LocalDate extractPaymentDate(String cleanText) {
        // Tìm ngày tháng năm định dạng dd/mm/yyyy, yyyy-mm-dd hoặc dd-mm-yyyy
        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile(
            "\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})\\b|\\b(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})\\b"
        );
        java.util.regex.Matcher dateMatcher = datePattern.matcher(cleanText);
        if (dateMatcher.find()) {
            try {
                if (dateMatcher.group(1) != null) {
                    int day = Integer.parseInt(dateMatcher.group(1));
                    int month = Integer.parseInt(dateMatcher.group(2));
                    int year = Integer.parseInt(dateMatcher.group(3));
                    return LocalDate.of(year, month, day);
                }
                int year = Integer.parseInt(dateMatcher.group(4));
                int month = Integer.parseInt(dateMatcher.group(5));
                int day = Integer.parseInt(dateMatcher.group(6));
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                // Giữ mặc định LocalDate.now()
            }
        }
        return LocalDate.now();
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
        String foldedOcrText = foldToAsciiAlphanumeric(lowercaseText);
        for (Dealer dealer : dealers) {
            if (!Boolean.TRUE.equals(dealer.getIsActive())) {
                continue;
            }
            String dealerCode = dealer.getCode().toLowerCase();
            String foldedDealerName = foldToAsciiAlphanumeric(dealer.getName());

            if (foldedOcrText.contains(foldedDealerName)
                    || lowercaseText.contains(dealerCode)
                    || lowercaseText.contains(dealerCode.replace("-", " "))) {
                return new DealerMatchResult(dealer.getId(), dealerNotes(dealer), 0.95);
            }
        }
        return null;
    }

    private String foldToAsciiAlphanumeric(String text) {
        String withoutMarks = java.text.Normalizer.normalize(text.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd');
        return withoutMarks.replaceAll("[^a-z0-9]", "");
    }

    private String dealerNotes(Dealer dealer) {
        return "CK TIEN HANG - " + dealer.getName().toUpperCase() + " - GIAO DICH OCR";
    }

    private BigDecimal extractNumberFromText(String text) {
        if (text == null) return null;
        // Loại bỏ các từ khóa tiền tệ ra khỏi chuỗi để lấy phần số
        String clean = text.toLowerCase().replaceAll("[^\\d.,]", "");

        // Loại bỏ phần thập phân VND lẻ .00 hoặc ,00 ở cuối nếu có
        if (clean.endsWith(".00") || clean.endsWith(",00")) {
            clean = clean.substring(0, clean.length() - 3);
        }

        // Loại bỏ toàn bộ dấu phân tách hàng nghìn để parse thành số nguyên
        clean = clean.replace(".", "").replace(",", "");
        if (clean.isEmpty()) return null;
        try {
            return new BigDecimal(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private record DealerMatchResult(Long dealerId, String notes, double confidenceScore) {
    }
}
