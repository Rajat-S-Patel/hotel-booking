# Hotel Booking System

Backend service for a hotel booking platform: owners onboard properties, guests search for available rooms, book,
pay and cancel. See [TASK.md](TASK.md) for the problem statement and [docs/db-specs.md](docs/db-specs.md) for
the database design, including the decisions taken and the alternatives that were rejected.

Built with Java 21, Spring Boot 4, Spring Data JPA and an in-memory H2 database.

## Prerequisites

- JDK 21

No other setup is needed. The Gradle wrapper downloads Gradle, and the app uses an in-memory H2 database.

## Build

```bash
./gradlew build
```

Compiles the code, runs all tests, and produces `build/libs/hotelbooking-0.0.1-SNAPSHOT.jar`.

## Run

With Gradle:

```bash
./gradlew bootRun
```

Or from the built jar:

```bash
java -jar build/libs/hotelbooking-0.0.1-SNAPSHOT.jar
```

The service starts on `http://localhost:8080`. All endpoints are under `/api/v1`.

On startup the app loads sample data, so it can be used straight away:

- cities and localities (Ahmedabad, Mumbai, Goa, Bengaluru)
- amenities (`WIFI`, `POOL`, `SEA_VIEW`, ...)
- three platform cancellation policies: Flexible, Moderate, Strict
- two owners with five properties (hotel, resort, homestay, villa)

### Useful URLs

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI spec: http://localhost:8080/v3/api-docs
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:hotelbooking`, user `sa`, empty password)
- Postman collection: [docs/postman/hotel-booking.postman_collection.json](docs/postman/hotel-booking.postman_collection.json)

### Postman collection

Import [docs/postman/hotel-booking.postman_collection.json](docs/postman/hotel-booking.postman_collection.json)
into Postman. It covers the whole flow (reference data, onboarding, search, booking, payment, cancellation) and
every request has a saved example request and response. The `baseUrl` collection variable points to
`http://localhost:8080/api/v1`.

The IDs and booking references in the requests come from a sample run. The database is in memory and gets new IDs
on every restart, so take fresh ones from the earlier responses (cities → localities, cancellation policies,
owner, search → `variantId`, booking → `reference`) before sending the later requests. Use a new
`Idempotency-Key` value for each new booking or payment.

### Configuration

Set in `src/main/resources/application.yaml`, and can be overridden at startup, e.g.
`./gradlew bootRun --args='--hotelbooking.booking.hold-duration=1m'`.

| Property | Default | Meaning |
|---|---|---|
| `hotelbooking.booking.hold-duration` | `15m` | how long a prepaid booking holds rooms while waiting for payment |
| `hotelbooking.booking.payment-timeout` | `15m` | a payment with no gateway answer after this long is marked failed |
| `hotelbooking.booking.expiry-interval` | `60s` | how often the hold-expiry and payment-timeout jobs run |
| `hotelbooking.pricing.weekend-surcharge-percent` | `20` | extra charge on the base rate for Friday and Saturday nights |
| `hotelbooking.seed.enabled` | `true` | load the sample data on startup |

## Test

```bash
./gradlew test
```

The test report is written to `build/reports/tests/test/index.html`.

To check concurrent bookings against a running app (5 rooms, 500 parallel requests by default):

```bash
python3 scripts/booking_concurrency_check.py 5 500
```

What the tests cover:

- **Double booking and concurrency** (`BookingConcurrencyIT`, against the real app and H2): parallel bookings for
  the last room, overlapping stays competing for a shared night, back-to-back stays, and parallel payments for one
  booking (only one is charged). Plus unit tests for the inventory ledger.
- **Booking creation**: the payment hold, pay-at-property, idempotency keys, check-in date checked in the
  property's time zone, and the booking state rules.
- **Payment**: success, decline and pending outcomes, amount check, idempotency, automatic refund when a payment
  arrives after the hold expired, refunds and the payment timeout.
- **Search**: availability, amenity and price filters, sorting and paging.

Not covered, due to the time limit: cancellation and the refund tiers, hold expiry, property onboarding rules,
pricing (weekend surcharge), controller/HTTP error responses and an end-to-end API flow test.

## Typical flow

1. `POST /owners` to onboard an owner, then `POST /owners/{ownerId}/properties` to add each property.
   A standalone hotel is simply an owner with one property.
2. `GET /properties/search?city=Mumbai&checkIn=2026-12-01&checkOut=2026-12-03&guests=2` to find rooms. Each
   offer has a `variantId`.
3. `POST /bookings` with an `Idempotency-Key` header and the `variantId`. A prepaid booking is held as
   `PENDING_PAYMENT`.
4. `POST /bookings/{reference}/payments` with an `Idempotency-Key` header, paying the booking total by card or
   UPI. The booking becomes `CONFIRMED`.
5. `POST /bookings/{reference}/cancellation` to cancel. The refund follows the cancellation policy and the rooms
   become available again.

## Key assumptions

**Properties and rooms**

- An owner can have one or many properties. A standalone property goes through the same flow as a chain.
- Rooms are counted per room type, not tracked as individual room numbers. A booking reserves a number of rooms of a
  type; the actual room is assigned at the property.
- A room type can have variants with extra features and a surcharge (e.g. Deluxe vs Deluxe Sea View). Each variant
  has its own room count, and every room type gets a `Standard` variant automatically.
- Each property kind has simple onboarding rules: hotels and resorts need a star rating, a homestay has at most
  10 rooms, a villa is let as a single unit.
- Every property accepts prepaid bookings. Pay at property is optional per property.
- Each property uses one of the platform cancellation policies. Owner-specific policies are not supported yet.

**Booking**

- A stay is overnight: check-out is after check-in, and at most 30 nights. Check-in is at the property's check-in
  time (12:00 by default) in the property's time zone, and "today" is decided in that time zone too.
- Guests are a single total count per booking (no adults/children split). It must fit the room type's maximum
  occupancy times the number of rooms.
- There is no login. The person booking is identified by email; a new email creates a new customer.
- A prepaid booking holds its rooms for 15 minutes. If it is not paid in time it expires and the rooms are released.
  A pay-at-property booking is confirmed straight away.
- A booking keeps the price per night and the cancellation policy version from the moment it was made. Later
  changes to prices or policies do not affect it.
- Bookings cannot be cancelled after check-in. Check-in, check-out and no-show states are not tracked.
- `POST /bookings` and `POST /bookings/{reference}/payments` need an `Idempotency-Key` header. Retrying with the
  same key and the same body returns the original result; the same key with a different body is rejected.
- Idempotency keys are stored on the records they create, not in a separate idempotency table: the `booking` and
  `payment_attempt` tables each have an `idempotency_key` column (unique) and a `request_fingerprint` column (a hash
  of the request body). Keys never expire, and a key only works for the one kind of request it was first used
  with.

**Pricing and payment**

- All prices are in INR, with two decimals.
- A night costs the room type's base price plus the variant surcharge. Friday and Saturday nights cost 20% more on
  the base price.
- A payment must be for the full booking total, by card or UPI. Wallet is left out; it would be added as one more
  payment method in the same way.
- Only the card network and last four digits are stored. The card number, expiry and CVV are never saved.
- Refunds go back to the original payment. If a refund fails, the booking still stays cancelled and the refund is
  shown as failed.
- Partial refunds follow the policy tiers, e.g. Moderate: 100% with at least 72 hours' notice, 50% with at least
  24 hours, nothing after that.

**Technical**

- Data lives in an in-memory H2 database behind Spring Data repositories, so it is reset on every restart.
  Switching to a real database means changing the datasource settings.
- The payment gateway is a mock (see below). There is no real money movement and no gateway webhooks.

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

## Known gaps

Things left out or simplified, and what would come next with more time:

- **Payments**: timed-out payments are marked failed instead of checking the outcome with the gateway; no gateway
  webhooks; no wallet; no partial payments or deposits.
- **Refunds**: a failed refund is recorded but never retried.
- **Authentication**: none. Anyone with a booking reference can view, pay for or cancel it, and owners are not
  authenticated when adding properties.
- **Property management**: no endpoints to update a property, change prices or room counts, or deactivate a
  property or room type (the `active` flag exists but is only set on creation). No `GET /owners/{id}`.
- **Cancellation policies**: only platform policies; owner-specific or customised policies are not supported.
- **Pricing**: base price plus a weekend surcharge only. No seasonal rates, per-date rate overrides, discounts or
  taxes. Single currency (INR).
- **Booking lifecycle**: no check-in, check-out or no-show states; a booking cannot be modified (dates, rooms),
  only cancelled and made again.
- **Search**: filtering, sorting and paging happen in memory after loading the city's properties, and room features
  are loaded lazily. Fine for the sample data; at scale this would move into database queries.
- **Concurrency edge case**: two first-time bookings with the same new email at the same moment can make one of
  them fail with a generic 409 while the customer record is being created.
- **Storage**: in-memory H2, so all data is lost on restart.
- **Tests**: see the "Not covered" list in the Test section.
