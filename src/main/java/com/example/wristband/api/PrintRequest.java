package com.example.wristband.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Print data for a wristband")
public class PrintRequest {

    @NotBlank
    @Schema(description = "Barcode value encoded on the wristband", example = "1234567890")
    private String barcode;

    @NotBlank
    @Schema(description = "First name of the person", example = "Jan")
    private String firstName;

    @NotBlank
    @Schema(description = "Last name of the person", example = "Peeters")
    private String lastName;

    @NotBlank
    @Schema(description = "Name of the organisation/association", example = "Chiro X")
    private String organisationName;

    @NotBlank
    @Schema(description = "Name of the project or event", example = "Pukkelpop 2026")
    private String projectName; // e.g. "Pukkelpop 2026"

    // Optional: identifiers for logo resources used in ZPL
    @Schema(description = "Identifier of the main event logo stored on the printer", example = "pukkelpop_logo")
    private String eventLogoId;       // e.g. "pukkelpop_logo"

    @Schema(description = "Identifier of the sponsor logo stored on the printer", example = "stup_logo")
    private String sponsorLogoId;     // e.g. "stup_logo"

    @Schema(description = "Base64 encoded image for the event logo (PNG/JPEG). If provided, it will be uploaded to the printer and mapped to an internal eventLogoId.", format = "byte", example = "iVBORw0KGgoAAAANSUhEUgAA...")
    private String eventLogoBase64;

    @Schema(description = "Base64 encoded image for the sponsor logo (PNG/JPEG). If provided, it will be uploaded to the printer and mapped to an internal sponsorLogoId.", format = "byte", example = "iVBORw0KGgoAAAANSUhEUgAA...")
    private String sponsorLogoBase64;
}

