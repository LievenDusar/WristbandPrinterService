package com.example.wristband.api;

import com.example.wristband.service.PrintService;
import com.example.wristband.service.ZplImageConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@RestController
@RequestMapping("/api/print")
@Validated
@RequiredArgsConstructor
@Tag(name = "Wristband printing", description = "Endpoints to print and preview Zebra wristbands")
public class PrintController {

    private final PrintService printService;

    @PostMapping("/wristband")
    @Operation(
            summary = "Send wristband to printer",
            description = "Builds ZPL for the wristband and sends it to the configured Zebra printer",
            responses = {
                    @ApiResponse(
                            responseCode = "202",
                            description = "Print job accepted",
                            content = @Content(schema = @Schema(implementation = PrintJobResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    public ResponseEntity<PrintJobResponse> printWristband(@Valid @RequestBody PrintRequest request) {
        printService.printWristband(request);
        PrintJobResponse response = new PrintJobResponse("OK", "Print job accepted");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Development/preview endpoint: build ZPL but do NOT send it to the printer.
     * You can copy the output into a Zebra emulator or label designer.
     */
    @PostMapping("/wristband/preview")
    @Operation(
            summary = "Preview wristband ZPL",
            description = "Builds the ZPL for the wristband but does not send it to the printer. Returns the ZPL as plain text.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "ZPL generated",
                            content = @Content(mediaType = "text/plain")
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    public ResponseEntity<String> previewWristband(@Valid @RequestBody PrintRequest request) {
        String zpl = printService.buildWristbandZpl(request);
        return ResponseEntity.ok()
                .header("Content-Type", "text/plain; charset=UTF-8")
                .body(zpl);
    }

    @PostMapping("/wristband/preview-with-image")
    @Operation(
            summary = "Preview wristband ZPL including inline logo image",
            description = "Builds the ZPL for the wristband and includes an inline ^DG definition for the logo so that online ZPL renderers can display the image.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "ZPL with inline image data generated",
                            content = @Content(mediaType = "text/plain")
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    public ResponseEntity<String> previewWristbandWithImage(@Valid @RequestBody PrintRequest request) {
        String zpl = printService.buildWristbandZplWithInlineLogo(request);
        return ResponseEntity.ok()
                .header("Content-Type", "text/plain; charset=UTF-8")
                .body(zpl);
    }

    public static class WristbandLayoutEngine {

        static class Config {
            int totalWidth = 3300;
            int totalHeight = 300;

            int safeLeft = 80;
            int safeRight = 80;
            int safeTop = 20;
            int safeBottom = 20;

            int gap = 30;

            int contentX() {
                return safeLeft;
            }

            int contentY() {
                return safeTop;
            }

            int contentWidth() {
                return totalWidth - safeLeft - safeRight;
            }

            int contentHeight() {
                return totalHeight - safeTop - safeBottom;
            }
        }

        static class PersonData {
            String firstName;
            String lastName;
            String organization;
            String eventName;
            String barcodeValue;

            PersonData(String firstName, String lastName, String organization, String eventName, String barcodeValue) {
                this.firstName = firstName;
                this.lastName = lastName;
                this.organization = organization;
                this.eventName = eventName;
                this.barcodeValue = barcodeValue;
            }

            String fullName() {
                String first = safe(firstName);
                String last = safe(lastName);

                if (first.isEmpty()) return last;
                if (last.isEmpty()) return first;
                return first + " " + last;
            }
        }

        static class Block {
            int x;
            int y;
            int width;
            int height;

            Block(int x, int y, int width, int height) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }
        }

        private final Config config = new Config();
        private final BufferedImage logo;

        public WristbandLayoutEngine(String resourcePath) throws IOException {
            this.logo = loadAndPrepareLogoFromClasspath(resourcePath);
        }

        private BufferedImage loadAndPrepareLogoFromClasspath(String resourcePath) throws IOException {
            String normalizedPath = normalizeResourcePath(resourcePath);

            try (InputStream inputStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(normalizedPath)) {

                if (inputStream == null) {
                    throw new IOException("Resource niet gevonden in classpath: " + normalizedPath);
                }

                BufferedImage original = ImageIO.read(inputStream);
                if (original == null) {
                    throw new IOException("Kon image niet lezen uit resource: " + normalizedPath);
                }

                BufferedImage resized = ZplImageConverter.resize(original, 140, 140);
                BufferedImage mono = ZplImageConverter.toMonochrome(resized);
                return ZplImageConverter.rotate90CounterClockwise(mono);
            }
        }

        private String normalizeResourcePath(String path) {
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalArgumentException("resourcePath mag niet leeg zijn");
            }
            return path.startsWith("/") ? path.substring(1) : path;
        }

        public String generate(PersonData data) {
            int contentX = config.contentX();
            int contentY = config.contentY();
            int contentW = config.contentWidth();
            int contentH = config.contentHeight();

            int logoW = 180;
            int textW = 1150;
            int barcodeW = 500;

            int groupWidth = logoW + config.gap + textW + config.gap + barcodeW + config.gap + logoW;
            int groupStartX = contentX + Math.max(0, (contentW - groupWidth) / 2);

            Block leftLogo = new Block(groupStartX, contentY, logoW, contentH);
            Block textBlock = new Block(leftLogo.x + leftLogo.width + config.gap, contentY, textW, contentH);
            Block barcodeBlock = new Block(textBlock.x + textBlock.width + config.gap, contentY, barcodeW, contentH);
            Block rightLogo = new Block(barcodeBlock.x + barcodeBlock.width + config.gap, contentY, logoW, contentH);

            StringBuilder zpl = new StringBuilder();
            zpl.append("^XA\n");
            zpl.append("^PW").append(config.totalWidth).append("\n");
            zpl.append("^LL").append(config.totalHeight).append("\n");
            zpl.append("^CI28\n");

            placeLogo(zpl, leftLogo);
            placeCenteredTextBlock(zpl, textBlock, data);
            placeBarcode(zpl, barcodeBlock, data.barcodeValue);
            placeLogo(zpl, rightLogo);

            zpl.append("^XZ");
            return zpl.toString();
        }

        private void placeLogo(StringBuilder zpl, Block block) {
            int x = block.x + Math.max(0, (block.width - logo.getWidth()) / 2);
            int y = block.y + Math.max(0, (block.height - logo.getHeight()) / 2);

            zpl.append("^FO").append(x).append(",").append(y).append("\n");
            zpl.append(ZplImageConverter.toZplGFA(logo)).append("^FS\n");
        }

        private void placeCenteredTextBlock(StringBuilder zpl, Block block, PersonData data) {
            String line1 = safe(data.eventName);
            String line2 = safe(data.fullName());
            String line3 = "-" + safe(data.organization);

            int font1H = 28;
            int font1W = 28;

            int font2H = 42;
            int font2W = 42;

            int font3H = 26;
            int font3W = 26;

            int lineGap = 12;

            int width1 = estimateTextWidth(line1, font1W);
            int width2 = estimateTextWidth(line2, font2W);
            int width3 = estimateTextWidth(line3, font3W);

            int centerX = block.x + (block.width / 2);

            int totalTextHeight = font1H + lineGap + font2H + lineGap + font3H;
            int startY = block.y + Math.max(0, (block.height - totalTextHeight) / 2);

            int y1 = startY + font1H;
            int y2 = y1 + lineGap + font2H;
            int y3 = y2 + lineGap + font3H;

            int x1 = centerX - (width1 / 2);
            int x2 = centerX - (width2 / 2);
            int x3 = centerX - (width3 / 2);

            zpl.append("^FO").append(x1).append(",").append(y1).append("\n");
            zpl.append("^A0N,").append(font1H).append(",").append(font1W).append("\n");
            zpl.append("^FD").append(escape(line1)).append("^FS\n");

            zpl.append("^FO").append(x2).append(",").append(y2).append("\n");
            zpl.append("^A0N,").append(font2H).append(",").append(font2W).append("\n");
            zpl.append("^FD").append(escape(line2)).append("^FS\n");

            zpl.append("^FO").append(x3).append(",").append(y3).append("\n");
            zpl.append("^A0N,").append(font3H).append(",").append(font3W).append("\n");
            zpl.append("^FD").append(escape(line3)).append("^FS\n");
        }

        private void placeBarcode(StringBuilder zpl, Block block, String barcodeValue) {
            int barcodeHeight = 160;
            int barcodeModuleWidth = 2;

            String safeBarcode = safe(barcodeValue);

            int estimatedBarcodeWidth = Math.min(
                    block.width - 40,
                    Math.max(220, safeBarcode.length() * 22)
            );

            int x = block.x + Math.max(0, (block.width - estimatedBarcodeWidth) / 2);
            int y = block.y + Math.max(0, (block.height - barcodeHeight) / 2);

            zpl.append("^BY").append(barcodeModuleWidth).append(",2,").append(barcodeHeight).append("\n");
            zpl.append("^FO").append(x).append(",").append(y).append("\n");
            zpl.append("^BCN,").append(barcodeHeight).append(",N,N,N\n");
            zpl.append("^FD").append(escape(safeBarcode)).append("^FS\n");
        }

        private int estimateTextWidth(String text, int fontWidth) {
            return (int) Math.round(text.length() * fontWidth * 0.60);
        }

        private static String safe(String value) {
            return value == null ? "" : value.trim();
        }

        private String escape(String value) {
            return value == null ? "" : value.replace("^", " ").replace("~", " ");
        }
    }

    public static void main(String[] args) throws Exception {
        // Voorbeeld:
        // src/main/resources/images/logo.png
        // => gebruik hieronder: "images/logo.png"
        String logoResourcePath = "images/stup-logo.png";

        WristbandLayoutEngine engine = new WristbandLayoutEngine(logoResourcePath);

        WristbandLayoutEngine.PersonData data = new WristbandLayoutEngine.PersonData(
                "Jan Pieter",
                "Van den Broeck",
                "Chiro Sint-Martinus Oostham",
                "Pukkelpop 26",
                "1234567890"
        );

        String zpl = engine.generate(data);

        System.out.println("=== GENERATED ZPL ===");
        System.out.println(zpl);

        writeToFile(zpl, "wristband.zpl");

        // Optioneel:
        // sendToPrinter(zpl, "192.168.1.50", 9100);
    }

    private static void writeToFile(String content, String fileName) {
        try (FileWriter writer = new FileWriter(fileName, StandardCharsets.UTF_8)) {
            writer.write(content);
            System.out.println("ZPL opgeslagen in: " + fileName);
        } catch (IOException e) {
            throw new RuntimeException("Kon ZPL file niet wegschrijven", e);
        }
    }

}

