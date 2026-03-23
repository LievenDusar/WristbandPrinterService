package com.example.wristband.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for a print job request")
public class PrintJobResponse {

    @Schema(description = "Status of the request", example = "OK")
    private String status;

    @Schema(description = "Additional info or error message", example = "Print job accepted")
    private String message;
}

