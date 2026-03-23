package com.example.wristband.service;

import com.example.wristband.api.PrintRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrintService {

    private final ZplTemplateBuilder zplTemplateBuilder;
    private final ZebraPrinterClient zebraPrinterClient;
    private final LogoUploadService logoUploadService;
    private final Base64ToGrfConverter base64ToGrfConverter;

    public void printWristband(PrintRequest request) {
        PrintRequest resolved = resolveLogos(request);
        String zpl = zplTemplateBuilder.buildWristbandZpl(resolved);
        zebraPrinterClient.sendToPrinter(zpl);
    }

    /**
     * Build ZPL without sending it to the printer.
     * Handy for development / preview.
     */
    public String buildWristbandZpl(PrintRequest request) {
        // For preview we do NOT talk to the printer. We only use
        // already-known logo identifiers (eventLogoId / sponsorLogoId),
        // and ignore any incoming base64 fields.
        return zplTemplateBuilder.buildWristbandZpl(request);
    }

    /**
     * Build ZPL including an inline ^DG definition for the sponsor/event logo,
     * so that online ZPL renderers can also show the image without having a printer.
     */
    public String buildWristbandZplWithInlineLogo(PrintRequest request) {
        String mainZpl = zplTemplateBuilder.buildWristbandZpl(request);

        String base64 = request.getSponsorLogoBase64();
        String imageName = null;

        if (base64 != null && !base64.isBlank()) {
            imageName = "SPONSOR";
        } else if (request.getEventLogoBase64() != null && !request.getEventLogoBase64().isBlank()) {
            base64 = request.getEventLogoBase64();
            imageName = "EVENT";
        }

        if (base64 == null || base64.isBlank() || imageName == null) {
            return mainZpl;
        }

        String dg = base64ToGrfConverter.toGrf(base64, imageName);

        // Plak de ^DG-definitie vóór de hoofd-ZPL, zodat ^XGIMAGE.GRF in de template werkt.
        return dg + "\n" + mainZpl;
    }

    private PrintRequest resolveLogos(PrintRequest request) {
        String eventLogoId = request.getEventLogoId();
        String sponsorLogoId = request.getSponsorLogoId();

        if (request.getEventLogoBase64() != null && !request.getEventLogoBase64().isBlank()) {
            eventLogoId = logoUploadService.ensureImageOnPrinter(request.getEventLogoBase64(), "EVT");
        }
        if (request.getSponsorLogoBase64() != null && !request.getSponsorLogoBase64().isBlank()) {
            sponsorLogoId = logoUploadService.ensureImageOnPrinter(request.getSponsorLogoBase64(), "SPONSOR");
        }

        return PrintRequest.builder()
                .barcode(request.getBarcode())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .organisationName(request.getOrganisationName())
                .projectName(request.getProjectName())
                .eventLogoId(eventLogoId)
                .sponsorLogoId(sponsorLogoId)
                .eventLogoBase64(request.getEventLogoBase64())
                .sponsorLogoBase64(request.getSponsorLogoBase64())
                .build();
    }
}

