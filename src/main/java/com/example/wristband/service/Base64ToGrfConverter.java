package com.example.wristband.service;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Converts a base64-encoded PNG/JPEG to a ZPL ^DG command string for inline preview use.
 */
@Component
public class Base64ToGrfConverter {

    /**
     * Converts a base64-encoded image to a ZPL ^DG command string.
     *
     * @param base64Image Base64 string (PNG/JPEG, no data-URL prefix).
     * @param imageName   Image name (without drive prefix or extension), e.g. "STUPLOGO".
     * @return Full ZPL command: ^XA^DGR:{NAME}.GRF,totalBytes,bytesPerRow,{hexData}^XZ
     */
    public String toGrf(String base64Image, String imageName) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new IllegalArgumentException("base64Image must not be null or blank");
        }
        if (imageName == null || imageName.isBlank()) {
            throw new IllegalArgumentException("imageName must not be null or blank");
        }

        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image from base64", e);
        }
        if (original == null) {
            throw new IllegalArgumentException("Unsupported image format for base64 logo");
        }

        BufferedImage mono = ZplGrfEncoder.toMonochrome(original);
        String upperName = imageName.toUpperCase();
        return "^XA^DGR:" + upperName + ".GRF," + ZplGrfEncoder.toDgData(mono) + "^XZ";
    }
}
