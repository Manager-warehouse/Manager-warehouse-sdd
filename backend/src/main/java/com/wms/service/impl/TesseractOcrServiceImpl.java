package com.wms.service.impl;

import com.wms.dto.response.PaymentReceiptOcrResponse;
import com.wms.entity.Dealer;
import com.wms.entity.User;
import com.wms.repository.DealerRepository;
import com.wms.service.OcrService;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

        File tempFile = null;
        try {
            // Chuyển MultipartFile thành File tạm để Tesseract đọc
            tempFile = File.createTempFile("ocr_receipt_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            // Chạy OCR thực tế quét chữ trong ảnh
            String resultText = tesseract.doOCR(tempFile);
            log.info("OCR Result Text Length: {}", resultText.length());

            return parseOcrText(resultText);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Throwable e) {
            log.warn("Actual OCR processing failed. Error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 
                "Không thể xử lý hình ảnh này bằng OCR. Vui lòng kiểm tra định dạng ảnh hoặc nhập tay. Chi tiết: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private PaymentReceiptOcrResponse parseOcrText(String text) {
        BigDecimal amount = null;
        LocalDate paymentDate = LocalDate.now();
        Long dealerId = null;
        String notes = "CK TIEN HANG - GIAO DICH OCR";

        // Chuẩn hóa văn bản: xóa khoảng trắng thừa, đưa về lowercase
        String cleanText = text.replaceAll("\\r?\\n", " ").replaceAll("\\s+", " ");
        String lowercaseText = cleanText.toLowerCase();

        // 1. Tìm số tiền bằng các giải thuật phân lớp ưu tiên

        // Cách 1: Tìm số tiền đi kèm sau từ khóa "số tiền" hoặc "amount"
        // Ví dụ: "số tiền: 100.000 vnd" hoặc "amount 5,000,000"
        java.util.regex.Pattern keywordPattern = java.util.regex.Pattern.compile(
            "(?:so\\s+tien|số\\s+tiền|amount|tong\\s+tien|tổng\\s+tiền|so\\s+tien\\s+chuyen|số\\s+tiền\s+chuyển|gia\\s+tri|giá\\s+trị)\\s*[:\\-\\. ]*\\s*([+\\-\\s]*[\\d.,]+(?:\\s*(?:vnd|vnd|đ|d|dong|đồng))?)", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher keywordMatcher = keywordPattern.matcher(lowercaseText);
        if (keywordMatcher.find()) {
            amount = extractNumberFromText(keywordMatcher.group(1));
        }

        // Cách 2: Nếu chưa tìm được, tìm số đi kèm đơn vị tiền tệ "vnd", "đ", "dong", "đồng" ở phía sau
        if (amount == null) {
            java.util.regex.Pattern currencyPattern = java.util.regex.Pattern.compile(
                "([\\d.,]+)\\s*(?:vnd|đ|dong|đồng)\\b", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher currencyMatcher = currencyPattern.matcher(lowercaseText);
            BigDecimal maxVal = BigDecimal.ZERO;
            while (currencyMatcher.find()) {
                BigDecimal val = extractNumberFromText(currencyMatcher.group(1));
                if (val != null && val.compareTo(maxVal) > 0 && val.compareTo(new BigDecimal("1000")) >= 0) {
                    maxVal = val;
                }
            }
            if (maxVal.compareTo(BigDecimal.ZERO) > 0) {
                amount = maxVal;
            }
        }

        // Cách 3: Nếu vẫn chưa tìm được, tìm số có dấu cộng (+) đằng trước (phổ biến trên screenshot biến động số dư / giao dịch thành công)
        if (amount == null) {
            java.util.regex.Pattern plusPattern = java.util.regex.Pattern.compile(
                "\\+\\s*([\\d.,]+)", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher plusMatcher = plusPattern.matcher(lowercaseText);
            BigDecimal maxVal = BigDecimal.ZERO;
            while (plusMatcher.find()) {
                BigDecimal val = extractNumberFromText(plusMatcher.group(1));
                if (val != null && val.compareTo(maxVal) > 0 && val.compareTo(new BigDecimal("1000")) >= 0) {
                    maxVal = val;
                }
            }
            if (maxVal.compareTo(BigDecimal.ZERO) > 0) {
                amount = maxVal;
            }
        }

        // Cách 4: Fallback cuối cùng - tìm số lớn nhất có định dạng phân cách nghìn (dấu chấm hoặc phẩy) và độ dài hợp lý
        if (amount == null) {
            java.util.regex.Pattern genericPattern = java.util.regex.Pattern.compile(
                "\\b\\d{1,3}(?:[.,]\\d{3})+\\b"
            );
            java.util.regex.Matcher genericMatcher = genericPattern.matcher(cleanText);
            BigDecimal maxVal = BigDecimal.ZERO;
            while (genericMatcher.find()) {
                BigDecimal val = extractNumberFromText(genericMatcher.group());
                if (val != null && val.compareTo(maxVal) > 0 && val.compareTo(new BigDecimal("1000")) >= 0) {
                    // Giới hạn số tiền dưới 1 tỷ để tránh nhầm với số tài khoản/mã giao dịch
                    if (val.compareTo(new BigDecimal("1000000000")) < 0) {
                        maxVal = val;
                    }
                }
            }
            if (maxVal.compareTo(BigDecimal.ZERO) > 0) {
                amount = maxVal;
            }
        }

        // Nếu hoàn toàn không thể nhận diện được số tiền giao dịch hợp lệ, ném lỗi 422
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("OCR parse failed: Could not extract any valid payment amount from OCR text.");
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 
                "Không thể nhận diện số tiền chuyển khoản hợp lệ từ hóa đơn này. Vui lòng nhập thông tin thủ công.");
        }

        // 2. Tìm ngày thanh toán (paymentDate)
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
                    paymentDate = LocalDate.of(year, month, day);
                } else {
                    int year = Integer.parseInt(dateMatcher.group(4));
                    int month = Integer.parseInt(dateMatcher.group(5));
                    int day = Integer.parseInt(dateMatcher.group(6));
                    paymentDate = LocalDate.of(year, month, day);
                }
            } catch (Exception e) {
                // Giữ mặc định LocalDate.now()
            }
        }

        // 3. Tìm đại lý (dealerId) khớp tên/code trong database
        List<Dealer> dealers = dealerRepository.findAll();
        for (Dealer dealer : dealers) {
            if (Boolean.TRUE.equals(dealer.getIsActive())) {
                String dealerName = dealer.getName().toLowerCase();
                String dealerCode = dealer.getCode().toLowerCase();
                
                String cleanDealerName = dealerName.replaceAll("[^a-z0-9]", "");
                String cleanOcrText = lowercaseText.replaceAll("[^a-z0-9]", "");

                if (lowercaseText.contains(dealerName) 
                        || lowercaseText.contains(dealerCode)
                        || cleanOcrText.contains(cleanDealerName)
                        || lowercaseText.contains(dealerCode.replace("-", " "))) {
                    dealerId = dealer.getId();
                    notes = "CK TIEN HANG - " + dealer.getName().toUpperCase() + " - GIAO DICH OCR";
                    break;
                }
            }
        }

        // Ghi chú mặc định nếu không khớp đại lý nào
        if (dealerId == null) {
            notes = "CK TIEN HANG - KHONG RO DAI LY (OCR)";
        }

        return PaymentReceiptOcrResponse.builder()
                .amount(amount)
                .paymentDate(paymentDate)
                .dealerId(dealerId)
                .notes(notes)
                .confidenceScore(dealerId != null ? 0.95 : 0.60)
                .build();
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
}
