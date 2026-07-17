package com.wms.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class OcrImagePreprocessorTest {

    @Test
    void preprocess_upscalesNarrowImage() {
        BufferedImage small = solidImage(100, 50, Color.WHITE);

        BufferedImage result = OcrImagePreprocessor.preprocess(small);

        assertThat(result.getWidth()).isGreaterThan(small.getWidth());
        assertThat(result.getWidth()).isLessThanOrEqualTo(small.getWidth() * 4);
    }

    @Test
    void preprocess_leavesWideImageWidthUnchanged() {
        BufferedImage wide = solidImage(2000, 800, Color.WHITE);

        BufferedImage result = OcrImagePreprocessor.preprocess(wide);

        assertThat(result.getWidth()).isEqualTo(wide.getWidth());
        assertThat(result.getHeight()).isEqualTo(wide.getHeight());
    }

    @Test
    void preprocess_producesBinarizedGrayscaleOutput() {
        // Half-black, half-white image: Otsu should split cleanly into 0/255 pixels.
        BufferedImage halfSplit = new BufferedImage(2000, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = halfSplit.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 1000, 100);
        g.setColor(Color.WHITE);
        g.fillRect(1000, 0, 1000, 100);
        g.dispose();

        BufferedImage result = OcrImagePreprocessor.preprocess(halfSplit);

        assertThat(result.getType()).isEqualTo(BufferedImage.TYPE_BYTE_GRAY);
        int sampleBlackSide = result.getRaster().getSample(10, 10, 0);
        int sampleWhiteSide = result.getRaster().getSample(1990, 10, 0);
        assertThat(sampleBlackSide).isEqualTo(0);
        assertThat(sampleWhiteSide).isEqualTo(255);
    }

    private static BufferedImage solidImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }
}
