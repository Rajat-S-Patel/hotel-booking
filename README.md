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
