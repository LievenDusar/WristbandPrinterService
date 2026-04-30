package com.example.wristband.api;

import com.example.wristband.service.LogoUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logos")
@RequiredArgsConstructor
@Tag(name = "Logo cache", description = "Manage the in-memory logo upload cache")
public class LogoCacheController {

    private final LogoUploadService logoUploadService;

    @DeleteMapping("/cache")
    @Operation(
            summary = "Clear logo cache",
            description = "Clears the in-memory cache of uploaded logos. Call this after a printer power cycle "
                    + "so that logos are re-uploaded on the next print job (the printer's RAM drive is wiped on restart).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cache cleared")
            }
    )
    public ResponseEntity<PrintJobResponse> clearCache() {
        logoUploadService.clearCache();
        return ResponseEntity.ok(new PrintJobResponse("OK", "Logo cache cleared"));
    }
}
