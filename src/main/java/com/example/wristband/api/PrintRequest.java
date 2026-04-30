package com.example.wristband.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(min = 1, max = 48)
    @Schema(description = "Barcode value encoded on the wristband (max 48 chars)", example = "1234567890")
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
    private String projectName;

    @Pattern(regexp = "[A-Za-z0-9_]{1,16}",
             message = "eventLogoId may only contain letters, digits and underscores (max 16 chars)")
    @Schema(description = "Identifier of the main event logo stored on the printer", example = "pukkelpop_logo")
    private String eventLogoId;

    @Pattern(regexp = "[A-Za-z0-9_]{1,16}",
             message = "sponsorLogoId may only contain letters, digits and underscores (max 16 chars)")
    @Schema(description = "Identifier of the sponsor logo stored on the printer", example = "stup_logo")
    private String sponsorLogoId;

    @Size(max = 700_000, message = "eventLogoBase64 must not exceed ~512 KB")
    @Pattern(regexp = "^[A-Za-z0-9+/=\\r\\n]*$", message = "eventLogoBase64 must be valid base64")
    @Schema(description = "Base64-encoded event logo image (PNG/JPEG, max ~512 KB).",
            format = "byte", example = "iVBORw0KGgoAAAANSUhEUgAA...")
    private String eventLogoBase64;

    @Size(max = 700_000, message = "sponsorLogoBase64 must not exceed ~512 KB")
    @Pattern(regexp = "^[A-Za-z0-9+/=\\r\\n]*$", message = "sponsorLogoBase64 must be valid base64")
    @Schema(description = "Base64-encoded sponsor logo image (PNG/JPEG, max ~512 KB).",
            format = "byte", example = "iVBORw0KGgoAAAANSUhEUgAA...")
    private String sponsorLogoBase64;
}
