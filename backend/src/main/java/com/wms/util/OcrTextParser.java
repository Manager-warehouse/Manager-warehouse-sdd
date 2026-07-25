package com.wms.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Shared amount/date/text-normalization parsing for OCR'd bank-transfer receipts and payment
// vouchers. Used by both the AR (dealer) and AP (supplier) OCR flows so the two never drift
// into divergent regex logic for the same document shape.
public final class OcrTextParser {

    private OcrTextParser() {
    }

    public static BigDecimal extractAmount(String cleanText, String lowercaseText) {
        // Cach 1: Tim so tien di kem sau tu khoa "so tien" hoac "amount", cho phep toi da
        // 3 tu dem o giua (vi du "So tien giao dich: 100.000")
        Pattern keywordPattern = Pattern.compile(
            "(?:so\\s+tien|số\\s+tiền|amount|tong\\s+tien|tổng\\s+tiền|so\\s+tien\\s+chuyen|"
            + "số\\s+tiền\\s+chuyển|gia\\s+tri|giá\\s+trị)(?:\\s+\\p{L}+){0,3}\\s*[:\\-\\. ]*\\s*"
            + "([+\\-\\s]*[\\d.,]+(?:\\s*(?:vnd|đ|d|dong|đồng))?)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher keywordMatcher = keywordPattern.matcher(lowercaseText);
        if (keywordMatcher.find()) {
            BigDecimal amount = extractNumberFromText(keywordMatcher.group(1));
            if (amount != null) {
                return amount;
            }
        }

        BigDecimal byCurrencySuffix = maxMatchAtLeast(lowercaseText,
                "([\\d.,]+)\\s*(?:vnd|đ|dong|đồng)\\b", null);
        if (byCurrencySuffix != null) {
            return byCurrencySuffix;
        }

        BigDecimal byPlusSign = maxMatchAtLeast(lowercaseText, "\\+\\s*([\\d.,]+)", null);
        if (byPlusSign != null) {
            return byPlusSign;
        }

        return maxMatchAtLeast(cleanText, "\\b\\d{1,3}(?:[.,]\\d{3})+\\b", new BigDecimal("1000000000"));
    }

    public static LocalDate extractPaymentDate(String cleanText) {
        Pattern datePattern = Pattern.compile(
            "\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})\\b|\\b(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})\\b"
        );
        Matcher dateMatcher = datePattern.matcher(cleanText);
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
                // Giu mac dinh LocalDate.now()
            }
        }
        return LocalDate.now();
    }

    // Bo dau tieng Viet va ky tu khong phai chu/so, de so khop ten doi tac voi van ban OCR
    // thuong bi rot dau thanh tren anh chup man hinh ngan hang.
    public static String foldToAsciiAlphanumeric(String text) {
        String withoutMarks = java.text.Normalizer.normalize(text.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd');
        return withoutMarks.replaceAll("[^a-z0-9]", "");
    }

    private static BigDecimal maxMatchAtLeast(String text, String regex, BigDecimal exclusiveUpperBound) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
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

    private static BigDecimal extractNumberFromText(String text) {
        if (text == null) return null;
        String clean = text.toLowerCase().replaceAll("[^\\d.,]", "");

        if (clean.endsWith(".00") || clean.endsWith(",00")) {
            clean = clean.substring(0, clean.length() - 3);
        }

        clean = clean.replace(".", "").replace(",", "");
        if (clean.isEmpty()) return null;
        try {
            return new BigDecimal(clean);
        } catch (Exception e) {
            return null;
        }
    }
}
