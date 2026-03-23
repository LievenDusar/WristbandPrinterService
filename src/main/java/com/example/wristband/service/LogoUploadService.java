package com.example.wristband.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

@Service
public class LogoUploadService {

    private static final Logger logger = LoggerFactory.getLogger(LogoUploadService.class);

    private final ZebraPrinterClient zebraPrinterClient;
    private final Map<String, String> hashToImageName = new ConcurrentHashMap<>();

    public LogoUploadService(ZebraPrinterClient zebraPrinterClient) {
        this.zebraPrinterClient = zebraPrinterClient;
    }

    /**
     * Ensure the given base64 image is available on the printer as a GRF image.
     * Returns the image name (without drive prefix and extension) to be used in ^XG.
     */
    public String ensureImageOnPrinter(String base64Image, String namePrefix) {
        if (base64Image == null || base64Image.isBlank()) {
            return null;
        }

        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        String hash = sha256Hex(imageBytes);

        // Cache hit: already uploaded earlier
        if (hashToImageName.containsKey(hash)) {
            return hashToImageName.get(hash);
        }

        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) {
                throw new IllegalArgumentException("Unsupported image format for logo");
            }

            BufferedImage mono = toMonochrome(original);
            String gfa = buildGfaFromImage(mono);

            String imageName = (namePrefix + "_" + hash.substring(0, 8)).toUpperCase();
            String dgCommand = "^XA^DG" + imageName + ".GRF," + gfa + "^XZ";

            zebraPrinterClient.sendToPrinter(dgCommand);
            hashToImageName.put(hash, imageName);
            logger.info("Uploaded logo {} to printer with name {}", namePrefix, imageName);

            return imageName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read logo image", e);
        }
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private BufferedImage toMonochrome(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage mono = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = mono.createGraphics();
        try {
            g2d.drawImage(src, 0, 0, null);
        } finally {
            g2d.dispose();
        }
        return mono;
    }

    /**
     * Build the GFA part of a ^DG command from a 1-bit image.
     * Returns the part "totalBytes,bytesPerRow,height,data..." (without the leading ^GFA).
     */
    private String buildGfaFromImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int bytesPerRow = (width + 7) / 8;
        int totalBytes = bytesPerRow * height;

        StringBuilder data = new StringBuilder(totalBytes * 2);

        for (int y = 0; y < height; y++) {
            int bitIndex = 0;
            int currentByte = 0;
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int gray = (int) (0.3 * ((rgb >> 16) & 0xFF)
                        + 0.59 * ((rgb >> 8) & 0xFF)
                        + 0.11 * (rgb & 0xFF));
                boolean isBlack = gray < 128;

                currentByte <<= 1;
                if (isBlack) {
                    currentByte |= 1;
                }

                bitIndex++;
                if (bitIndex == 8) {
                    data.append(String.format("%02X", currentByte & 0xFF));
                    bitIndex = 0;
                    currentByte = 0;
                }
            }

            if (bitIndex > 0) {
                currentByte <<= (8 - bitIndex);
                data.append(String.format("%02X", currentByte & 0xFF));
            }
        }

        return totalBytes + "," + bytesPerRow + "," + height + ", " + data;
    }
}

