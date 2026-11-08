# Hotel Booking System

Backend service for a hotel booking platform. See [TASK.md](TASK.md) for the problem statement.

## Prerequisites

- JDK 21

No other setup is needed. The Gradle wrapper downloads Gradle, and the app uses an in-memory H2 database.

## Build

```bash
./gradlew build
```

Compiles the code, runs all tests, and produces `build/libs/hotelbooking-0.0.1-SNAPSHOT.jar`.

## Test

```bash
./gradlew test
```

The test report is written to `build/reports/tests/test/index.html`.

## Run

With Gradle:

```bash
./gradlew bootRun
```

Or from the built jar:

```bash
java -jar build/libs/hotelbooking-0.0.1-SNAPSHOT.jar
```

The service starts on `http://localhost:8080`.

## API Documentation

Once the app is running:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI spec: http://localhost:8080/v3/api-docs

## Payments (mock gateway)

The payment gateway is mocked (`MockPaymentGateway`). Its result depends on the test instrument used:

| Card / UPI | Result |
|---|---|
| card ending `0002`, UPI `fail@<bank>` | declined (402) |
| card ending `0119`, UPI `pending@<bank>` | no answer yet, payment stays `INITIATED` (202) |
| card ending `0341` | charge succeeds, a later refund is rejected |
| anything else, e.g. `4242424242424242` | succeeds (200) |

A payment that stays `INITIATED` (no answer from the gateway) blocks new payments for that booking.
A scheduled job marks such payments `FAILED` after 15 minutes (`hotelbooking.booking.payment-timeout`).

This is a simplification. "No answer" does not always mean the money did not move, so a real
integration would ask the gateway for the payment's status (or handle its webhook) and confirm or
refund based on the actual outcome, instead of failing the payment blindly.
