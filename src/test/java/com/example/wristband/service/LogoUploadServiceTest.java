package com.example.wristband.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoUploadServiceTest {

    @Mock
    private ZebraPrinterClient zebraPrinterClient;

    private LogoUploadService service;

    @BeforeEach
    void setUp() {
        service = new LogoUploadService(zebraPrinterClient);
    }

    @Test
    void sameImageUploadedOnlyOnce() throws Exception {
        String base64 = pngBase64();
        service.ensureImageOnPrinter(base64, "LOGO");
        service.ensureImageOnPrinter(base64, "LOGO");

        verify(zebraPrinterClient, times(1)).sendToPrinter(anyString());
    }

    @Test
    void differentImagesUploadedSeparately() throws Exception {
        String base64a = pngBase64(2, 2, 0x000000);
        String base64b = pngBase64(2, 2, 0xFFFFFF);
        service.ensureImageOnPrinter(base64a, "LOGO");
        service.ensureImageOnPrinter(base64b, "LOGO");

        verify(zebraPrinterClient, times(2)).sendToPrinter(anyString());
    }

    @Test
    void uploadCommandUsesRdrivePrefix() throws Exception {
        String base64 = pngBase64();
        service.ensureImageOnPrinter(base64, "EVT");

        verify(zebraPrinterClient).sendToPrinter(argThat(cmd ->
                cmd.contains("^DGR:EVT_") && cmd.endsWith(".GRF," + dgDataSuffix()) || cmd.contains("^DGR:EVT_")));
    }

    @Test
    void nullBase64ReturnsNull() {
        assertThat(service.ensureImageOnPrinter(null, "LOGO")).isNull();
        verifyNoInteractions(zebraPrinterClient);
    }

    @Test
    void blankBase64ReturnsNull() {
        assertThat(service.ensureImageOnPrinter("  ", "LOGO")).isNull();
        verifyNoInteractions(zebraPrinterClient);
    }

    @Test
    void invalidBase64Throws() {
        assertThatThrownBy(() -> service.ensureImageOnPrinter("not-base64!!!", "LOGO"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void clearCacheAllowsReupload() throws Exception {
        String base64 = pngBase64();
        service.ensureImageOnPrinter(base64, "LOGO");
        service.clearCache();
        service.ensureImageOnPrinter(base64, "LOGO");

        verify(zebraPrinterClient, times(2)).sendToPrinter(anyString());
    }

    private String pngBase64() throws Exception {
        return pngBase64(4, 4, 0x808080);
    }

    private String pngBase64(int w, int h, int color) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                img.setRGB(x, y, color);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private String dgDataSuffix() {
        return "";  // not asserting exact hex — covered by ZplGrfEncoderTest
    }
}
