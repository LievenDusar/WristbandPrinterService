## Wristband Printer Service (Spring Boot)

REST service to send Zebra ZPL print jobs for wristbands from external systems (e.g. Symfony website) to a Zebra-compatible wristband printer. Designed to run in Docker.

### API

- **POST** `/wristband-printer/api/print/wristband`
  - JSON body:

```json
{
  "barcode": "1234567890",
  "firstName": "Jan",
  "lastName": "Peeters",
  "organisationName": "Chiro X",
  "projectName": "Pukkelpop 2026",
  "eventLogoId": "pukkelpop_logo",
  "sponsorLogoId": "stup_logo"
}
```

Returns HTTP `202 Accepted` on success.

- **POST** `/wristband-printer/api/print/wristband/preview`
  - Zelfde JSON body als hierboven
  - Retourneert **plain text** met de gegenereerde ZPL, zonder dat er effectief geprint wordt.
  - Handig in development: je kan deze ZPL plakken in een Zebra emulator of label designer om het bandje visueel te controleren.

### Configuration

- `printer.host` / env `PRINTER_HOST` – hostname or IP of the Zebra printer.
- `printer.port` / env `PRINTER_PORT` – usually `9100`.

### Build & Run (local)

```bash
mvn clean package
java -jar target/wristband-printer-service-0.0.1-SNAPSHOT.jar
```

### Build & Run (Docker)

```bash
mvn clean package
docker build -t wristband-printer-service .
docker run --rm -p 8080:8080 \
  -e PRINTER_HOST=10.0.0.50 \
  -e PRINTER_PORT=9100 \
  wristband-printer-service
```

