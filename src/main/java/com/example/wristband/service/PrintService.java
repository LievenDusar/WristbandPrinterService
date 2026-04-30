package com.example.wristband.service;

import com.example.wristband.api.PrintRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrintService {

    private final ZplTemplateBuilder zplTemplateBuilder;
    private final ZebraPrinterClient zebraPrinterClient;
    private final LogoUploadService logoUploadService;
    private final Base64ToGrfConverter base64ToGrfConverter;
    private final MeterRegistry meterRegistry;

    public void printWristband(PrintRequest request) {
        Counter total = meterRegistry.counter("wristband.print.jobs.total");
        Counter failed = meterRegistry.counter("wristband.print.jobs.failed");
        Timer timer = meterRegistry.timer("wristband.print.duration");

        total.increment();
        try {
            timer.record(() -> {
                PrintRequest resolved = resolveLogos(request);
                String zpl = zplTemplateBuilder.buildWristbandZpl(resolved);
                zebraPrinterClient.sendToPrinter(zpl);
            });
        } catch (RuntimeException e) {
            failed.increment();
            throw e;
        }
    }

    public String previewWristbandZpl(PrintRequest request) {
        return zplTemplateBuilder.buildWristbandZpl(request);
    }

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
