package com.example.wristband.api;

import com.example.wristband.service.PrintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "503", description = "Printer unavailable")
            }
    )
    public ResponseEntity<PrintJobResponse> printWristband(@Valid @RequestBody PrintRequest request) {
        printService.printWristband(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new PrintJobResponse("OK", "Print job accepted"));
    }

    @PostMapping(value = "/wristband/preview", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
            summary = "Preview wristband ZPL",
            description = "Builds the ZPL for the wristband but does not send it to the printer. Returns the ZPL as plain text.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "ZPL generated",
                            content = @Content(mediaType = "text/plain")),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    public ResponseEntity<String> previewWristband(@Valid @RequestBody PrintRequest request) {
        return ResponseEntity.ok(printService.previewWristbandZpl(request));
    }

    @PostMapping(value = "/wristband/preview-with-image", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
            summary = "Preview wristband ZPL including inline logo image",
            description = "Builds the ZPL for the wristband and includes an inline ^DG definition for the logo so that online ZPL renderers can display the image.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "ZPL with inline image data generated",
                            content = @Content(mediaType = "text/plain")),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    public ResponseEntity<String> previewWristbandWithImage(@Valid @RequestBody PrintRequest request) {
        return ResponseEntity.ok(printService.buildWristbandZplWithInlineLogo(request));
    }
}
