package com.wms.util;


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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Generic (bank-agnostic) preprocessing to improve Tesseract accuracy on
 * phone-camera/screenshot bank transfer receipts: upscale small images,
 * strip color, and binarize with a global Otsu threshold.
 */
public final class OcrImagePreprocessor {

    private static final int TARGET_MIN_WIDTH = 1600;
    private static final int MAX_UPSCALE_FACTOR = 4;
    private static final int GRAY_LEVELS = 256;

    private OcrImagePreprocessor() {
    }

    public static BufferedImage preprocess(BufferedImage source) {
        BufferedImage upscaled = upscaleIfSmall(source);
        BufferedImage gray = toGrayscale(upscaled);
        return binarizeOtsu(gray);
    }

    private static BufferedImage upscaleIfSmall(BufferedImage source) {
        if (source.getWidth() >= TARGET_MIN_WIDTH) {
            return source;
        }
        double scale = Math.min(MAX_UPSCALE_FACTOR, (double) TARGET_MIN_WIDTH / source.getWidth());
        int newWidth = (int) Math.round(source.getWidth() * scale);
        int newHeight = (int) Math.round(source.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(source, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage toGrayscale(BufferedImage source) {
        BufferedImage gray = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return gray;
    }

    private static BufferedImage binarizeOtsu(BufferedImage gray) {
        int width = gray.getWidth();
        int height = gray.getHeight();
        int[] histogram = new int[GRAY_LEVELS];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                histogram[gray.getRaster().getSample(x, y, 0)]++;
            }
        }

        int threshold = otsuThreshold(histogram, width * height);

        BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int level = gray.getRaster().getSample(x, y, 0);
                binary.getRaster().setSample(x, y, 0, level > threshold ? 255 : 0);
            }
        }
        return binary;
    }

    /** Standard Otsu's method: pick the threshold maximizing between-class variance. */
    private static int otsuThreshold(int[] histogram, int totalPixels) {
        long sumAll = 0;
        for (int level = 0; level < GRAY_LEVELS; level++) {
            sumAll += (long) level * histogram[level];
        }

        long sumBackground = 0;
        int weightBackground = 0;
        double maxVariance = -1;
        int bestThreshold = GRAY_LEVELS / 2;

        for (int level = 0; level < GRAY_LEVELS; level++) {
            weightBackground += histogram[level];
            if (weightBackground == 0) {
                continue;
            }
            int weightForeground = totalPixels - weightBackground;
            if (weightForeground == 0) {
                break;
            }
            sumBackground += (long) level * histogram[level];

            double meanBackground = (double) sumBackground / weightBackground;
            double meanForeground = (double) (sumAll - sumBackground) / weightForeground;
            double betweenVariance = (double) weightBackground * weightForeground
                    * Math.pow(meanBackground - meanForeground, 2);

            if (betweenVariance > maxVariance) {
                maxVariance = betweenVariance;
                bestThreshold = level;
            }
        }
        return bestThreshold;
    }
}
