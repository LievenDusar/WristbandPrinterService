package com.example.wristband.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base64ToGrfConverterTest {

    private final Base64ToGrfConverter converter = new Base64ToGrfConverter();

    @Test
    void outputHasCorrectDgCommandStructure() throws Exception {
        String base64 = pngBase64(2, 2);
        String result = converter.toGrf(base64, "TESTLOGO");

        assertThat(result).startsWith("^XA^DGR:TESTLOGO.GRF,");
        assertThat(result).endsWith("^XZ");
        // ^DG format: totalBytes,bytesPerRow,hexData — no height field
        // 2x2 image: bytesPerRow=1, totalBytes=2
        assertThat(result).contains("^DGR:TESTLOGO.GRF,2,1,");
    }

    @Test
    void imageNameIsUppercased() throws Exception {
        String base64 = pngBase64(4, 4);
        String result = converter.toGrf(base64, "lowercase");
        assertThat(result).contains("^DGR:LOWERCASE.GRF,");
    }

    @Test
    void blankBase64Throws() {
        assertThatThrownBy(() -> converter.toGrf("", "NAME"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> converter.toGrf("  ", "NAME"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankImageNameThrows() throws Exception {
        String base64 = pngBase64(2, 2);
        assertThatThrownBy(() -> converter.toGrf(base64, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidBase64Throws() {
        assertThatThrownBy(() -> converter.toGrf("not-valid-base64!!!", "NAME"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonImageBytesThrow() {
        String base64 = Base64.getEncoder().encodeToString("this is not an image".getBytes());
        assertThatThrownBy(() -> converter.toGrf(base64, "NAME"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image format");
    }

    private String pngBase64(int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Fill with a simple pattern: top half black, bottom half white
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, y < height / 2 ? 0x000000 : 0xFFFFFF);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
