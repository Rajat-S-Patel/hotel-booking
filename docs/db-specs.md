# Database Design — Hotel Booking System

Date: 2026-11-08 · Status: agreed

This document records the persistence design for the hotel booking service, as implemented. Requirements come from the assignment brief (problem statement in [TASK.md](../TASK.md)). Every decision below was discussed and agreed one at a time; the rejected alternatives are kept in Section 12 because the follow-up discussion centres on trade-offs.

---

## 1. Scope and setup

- Persistence uses Spring Data JPA (Hibernate 7.4, from Spring Boot 4) on an in-memory H2 database (`jdbc:h2:mem:hotelbooking`, H2 pinned to 2.3.232, see Section 15). Hibernate generates the schema at start-up with `halt_on_error` on, so a broken mapping stops the app instead of silently skipping a table. There are no migrations, as the brief asks us not to spend time on production persistence.
- All data access goes through repository interfaces owned by the application, so H2 can be swapped for a production database without touching business logic.
- Only overnight stays are modelled: hotels, resorts, homestays and villas. Day-use venues such as party plots are out of scope (Section 13).

## 2. Design principles

1. **Store facts, reference rules.** Things that were settled at booking time (the price paid) are copied onto the booking. Rules applied later (cancellation terms) are referenced through immutable versions.
2. **Behaviour goes in code, pure data goes in tables.** A type that changes how the system behaves (property kind, booking type, payment method, policy kind) is a Java enum. Data with no behaviour attached (amenities, cities, localities) is a reference table, so new values need no deploy.
3. **Enums are saved by name.** Every enum column is mapped as STRING plus `@JdbcTypeCode(SqlTypes.VARCHAR)`: a varchar column with a check constraint listing the allowed values (without the type code, Hibernate's H2 dialect creates a native ENUM column). Never ORDINAL, which silently corrupts data when constants are reordered, and never a native database enum type, which turns every new value into a schema change.
4. **Identifiers are UUID version 7** on every record. They can't be guessed (important with no authentication), and their time ordering keeps index inserts efficient.
5. **Money is exact.** Amounts are BigDecimal with two decimal places, never floating point. Every amount has a currency column beside it (INR only for now). Rounding is half-up, applied in one place.
6. **Time is explicit.** Stay dates are plain dates. Every event timestamp is stored in UTC. Wall-clock times such as check-in time are stored with the property's time zone.
7. **Every record has created and updated timestamps** in UTC.
8. **Hot counters are locked pessimistically; rarely contended records use optimistic versions** (Section 11).

## 3. Entity overview

| Group | Records | Relationships |
|---|---|---|
| Reference data | City, Locality, Amenity | A locality belongs to a city |
| Supply | Owner, Property, Room Type, Variant | Owner has many properties; property has many room types; room type has one or more variants |
| Inventory | Ledger Entry | One per variant per night |
| Cancellation | Policy, Policy Version, Policy Tier | Policy has many immutable versions, one current; a version has zero or more tiers |
| Demand | Customer, Booking, Nightly Price Line | Customer makes many bookings; booking has one price line per night |
| Money | Payment Attempt, Refund Attempt | Booking has many payment attempts; a successful payment has many refund attempts |

Links between groups: a property points to its locality, its cancellation policy and its amenities. A booking points to its customer, property, variant and the policy version in force when it was made. A payment attempt holds the booking's ID as a plain column with no foreign key, so the payment code does not depend on the booking code.

Tables (20): `city`, `locality`, `amenity`, `owner`, `property`, `property_booking_type`, `property_amenity`, `room_type`, `room_type_feature`, `variant`, `variant_feature`, `ledger_entry`, `policy`, `policy_version`, `policy_tier`, `customer`, `booking`, `nightly_price_line`, `payment_attempt`, `refund_attempt`.

## 4. Reference data

### City
Name, unique. Seeded.

### Locality
Name and the city it belongs to. The pair (city, name) is unique. Seeded with a handful of localities per city; new ones are added as data by the platform.

Properties choose their locality from this list during onboarding instead of typing it, so spelling variants ("Navrangpura" vs "Navarangpura") can't split search results. Aliases and autocomplete are future additions.

### Amenity
Code (unique, UPPER_SNAKE_CASE), display name, and scope: PROPERTY (pool, parking, wifi) or ROOM (sea view, balcony, air conditioning). Seeded with a standard set. A new amenity is a new row.

## 5. Supply: owners, properties, rooms

### Owner
- Name, contact email (unique, normalised to lower case), contact phone.
- Created on its own (`POST /owners`); properties are added to it afterwards, one at a time.
- **Owner kind:** INDIVIDUAL or GROUP. A display label only; no logic branches on it, because a new chain may have just one property.
- A standalone hotel is an owner with one property. A chain is the same structure with more properties. There is no separate path for either.

### Property
- Owner, name, and **property kind:** HOTEL, RESORT, HOMESTAY or VILLA. Kinds can be mixed under one owner. Each kind has its own onboarding rules in code: hotels and resorts need a star rating, a resort needs at least one property amenity, a homestay has no star rating and at most 10 rooms, a villa is a single room type with one unit.
- Star rating, 1 to 5, optional at the database level and required by the hotel and resort rules.
- **Address,** embedded as a value: address line and pincode, plus a link to the locality. The city is reached through the locality and not stored twice.
- Check-in time (default 12:00), check-out time (default 11:00), time zone (default Asia/Kolkata).
- Cancellation policy: a link to a Policy, not to a version (Section 7).
- **Accepted booking types:** a set stored in a side table, one row per property per type. PREPAID must always be present; when not specified at onboarding the set is PREPAID only.
- Property amenities: links to Amenity records with PROPERTY scope.
- **Active flag.** An inactive property is hidden from search and can't be booked; its history is kept. There is no endpoint to deactivate a property yet, so in practice every property is active.
- Optimistic version.

### Room Type
- Property, name (for example "Deluxe", unique within the property), max occupancy per room, base price per night with currency.
- Room features (ROOM-scope amenities) shared by all its variants, in `room_type_feature`.
- Holds what all its variants share. It has no room count of its own; its total is the sum of its variants.
- Optimistic version.

### Variant
- Room type, name (unique within the room type), surcharge per night (zero for Standard), **room count**, extra room features (links to Amenity records with ROOM scope, in `variant_feature`), active flag, optimistic version.
- Every room type has exactly one variant named "Standard" with zero surcharge, created automatically with the room type.
- The variant is the **sellable unit**: bookings and inventory refer to the variant, never the room type.
- Variant price per night = room type base price + variant surcharge.
- **Variant room pools are disjoint.** Five deluxe rooms with one sea-facing are modelled as Standard with 4 rooms and Sea View with 1 room, never "5 rooms plus a sea-view add-on" (that would let a sea-view guest and five standard guests share five rooms).

## 6. Inventory ledger

### Ledger Entry
- Variant, night (a plain date), reserved count. No optimistic version: these rows are locked pessimistically (Section 11).
- **(variant, night) is unique.** This constraint is what makes lazy creation safe.
- Rows are created lazily: a missing row means nothing is reserved on that night. Missing rows for a stay are inserted just before the booking transaction starts (Section 11); an empty row (reserved 0) left behind by a failed booking is harmless and gets reused.
- The total is read from the variant's room count, never copied. Available on a night = variant room count − reserved count.
- A booking for N rooms over several nights succeeds only if every night has at least N available, and then reserves on every night, all or nothing.
- **The ledger is the only gate for availability.** Search and booking never check availability by scanning bookings.
- Every booking creation, expiry and cancellation updates the reserved counts in the same transaction as the booking change, so the counts can't drift from the bookings.

## 7. Cancellation policies

### Policy
- Name (for example "Moderate"), a link to its current version.
- Optional owner, stored as the owner's ID rather than a link (a link would make the policy and property packages depend on each other): empty means a platform-standard policy.
- Optional "based on" link to the policy version it was copied from.
- The owner and "based on" fields exist for a future "customise this policy" feature for hotels, which is not built now. Copy-and-customise was chosen over live inheritance (Section 12).

### Policy Version
- Policy, version number (unique per policy), **policy kind**.
- **Immutable.** A version is never updated. "Editing" a policy creates a new version and moves the policy's current-version link; old versions stay.
- A version referenced by any booking is never deleted.
- Only one kind is built: **TIERED.** A new number-only policy (for example "Strict") is new data. A policy with a different shape (for example "first night charged") is a new kind implemented as one new class, with no edits to existing code.

### Policy Tier
- An embedded value, not a separate record type: stored as rows of the version (no ID of its own), holding minimum hours before check-in and refund percentage.
- (version, minimum hours) is unique, and less notice can never refund more than more notice; both are checked when the version is created.
- Refund at cancellation = the percentage from the first tier whose minimum hours is met (tiers are checked from most notice to least), applied to the amount paid and capped at what is still refundable. Hours are whole hours before the check-in moment. No matching tier means no refund.

### Seeded platform policies
| Policy | Tiers |
|---|---|
| Flexible | 24 hours or more → 100% |
| Moderate | 72 hours or more → 100%; 24 hours or more → 50% |
| Strict | 168 hours or more → 50% |

### How bookings use policies
A property points to a Policy. When a booking is created, the policy's **current version** is captured on the booking in the same transaction. A later version applies to new bookings automatically; existing bookings keep the version they were made under. Cancellation always calculates from the booking's captured version, never from the live policy.

There is one policy per property. Selling the same room as both refundable and non-refundable needs rate plans, a future extension.

## 8. Customers, bookings and prices

### Customer
- The person who makes the booking (the booker).
- Name, email (unique, normalised to lower case), phone.
- Found or created by email when a booking is made. An existing customer's name and phone are not overwritten by a later booking request, since there is no login to prove who is asking. Once authentication exists, the customer becomes or links to the user account.
- Because email is unverified until then, there is no endpoint that lists bookings by email.

### Booking
- **Human reference:** a short unique code such as HB7K2M9Q, used in confirmations and support. Separate from the UUID.
- Links: customer, property, variant, captured policy version.
- **Booking type:** PREPAID or PAY_AT_PROPERTY. Must be in the property's accepted set at creation time. Later changes to the property's set don't affect existing bookings.
- **Status:** PENDING_PAYMENT, CONFIRMED, CANCELLED or EXPIRED (Section 10).
- Check-in date and check-out date (plain dates; the nights are check-in up to the day before check-out). The check-in date can't be before today in the property's time zone, and a stay is at most 30 nights.
- Room count and guest count. Guest count is a single total, and guests ≤ rooms × the room type's max occupancy. Adult/child split is a future extension.
- **Optional staying-guest details,** embedded: name and phone. Empty means the booker is the guest. These are a snapshot; they don't change if the customer's details change. The role of a person is shown by which link points at them (booker or staying guest), not by a type column.
- Total amount and currency.
- Hold-expires-at (UTC), set for prepaid bookings to creation time + 15 minutes (`hotelbooking.booking.hold-duration`); empty for pay-at-property.
- Confirmed-at, cancelled-at, expired-at (UTC), set on the matching transitions.
- Idempotency key (unique) and request fingerprint (Section 11).
- Index on (status, hold-expires-at) for the expiry job.
- Optimistic version.

### Nightly Price Line
- Booking, night, base rate, surcharge, room count, line amount, currency.
- One line per night of the stay, written once at booking creation.
- The booking total is computed once as the sum of its lines at creation and never recalculated. Later price changes never touch existing bookings.
- A night's base rate comes from the pricing strategy: the room type's base price, plus 20% on Friday and Saturday nights. The surcharge is the variant's surcharge.
- The breakdown supports date-based pricing (a bonus item) and per-night refund rules without schema changes.

## 9. Payments and refunds

### Payment Attempt
- One record per attempt. A booking can have several (a card declined, then UPI succeeding). Pay-at-property bookings have none.
- Booking ID (indexed, no foreign key), amount and currency. The amount must equal the booking total.
- **Refunded amount** and currency: the sum of successful and in-progress refunds against this payment, kept on the payment so the "refunds never exceed the charge" rule is checked under the payment's own version.
- **Method:** CARD or UPI. (Wallet was dropped from scope; it would be added as a new method the same way.)
- **Status:** INITIATED, SUCCEEDED or FAILED.
- Gateway reference and failure reason.
- **Method details:** a JSON column holding a typed, masked object defined per method in code: card network and last four digits; UPI ID. The full card number, CVV and expiry are never stored. The display summary ("VISA **** 4242") is derived from these details, not stored.
- Rule: anything the logic uses or queries is a real column; display and audit data goes in method details.
- Idempotency key (unique) and request fingerprint.
- Optimistic version.
- **At most one SUCCEEDED attempt per booking.** There is no database constraint for this; it is enforced when a payment starts: a new attempt is refused while the booking has an INITIATED or SUCCEEDED one, and starting a payment bumps the booking's version so two parallel starts can't both pass that check.
- An attempt still INITIATED after 15 minutes (`hotelbooking.booking.payment-timeout`, no answer from the gateway) is marked FAILED by a scheduled job, so it stops blocking new payments.
- A new payment method is a new enum constant, a new processor class and a new details type, with no schema change.

### Refund Attempt
- One record per refund attempt, linked to the successful payment attempt it reverses.
- Amount and currency, status (INITIATED, SUCCEEDED or FAILED), reason (CANCELLATION; PAYMENT_AFTER_EXPIRY or PAYMENT_AFTER_CANCELLATION when a payment succeeds after its booking was already expired or cancelled), gateway refund reference, optimistic version.
- Successful plus in-progress refunds for a payment can never exceed the amount that payment charged.
- Refunds always go back to the original payment method.
- A failed refund does not block cancellation, and gives its amount back to the payment's refundable balance. It is recorded as FAILED; retrying it is out of scope.

## 10. Booking lifecycle

| From | Event | To | Effect on ledger | Money |
|---|---|---|---|---|
| (new, prepaid) | create | PENDING_PAYMENT | Reserve all nights | — |
| (new, pay at property) | create | CONFIRMED | Reserve all nights | — |
| PENDING_PAYMENT | payment succeeds | CONFIRMED | None | Payment SUCCEEDED |
| PENDING_PAYMENT | payment fails | PENDING_PAYMENT | None | Payment FAILED; retry allowed while the hold lasts |
| PENDING_PAYMENT | hold expires | EXPIRED | Release all nights | — |
| PENDING_PAYMENT | guest cancels | CANCELLED | Release all nights | Nothing to refund |
| CONFIRMED | guest cancels before check-in | CANCELLED | Release all nights | Refund per captured policy version (prepaid only) |

- CANCELLED and EXPIRED are final.
- Cancelling is refused once the check-in moment has passed. The check-in moment is the check-in date at the property's check-in time, in the property's time zone. Cancellation tiers count hours back from this moment.
- Checked-in, completed and no-show are not stored. "Completed" may be shown as a derived label: confirmed with a check-out date in the past.
- **Expiry job:** runs every minute and expires PENDING_PAYMENT bookings whose hold-expires-at has passed, in batches of 50, each booking in its own transaction. If the job runs late, rooms are released late: the system can undersell briefly but never oversell.
- **Late payment:** a payment that succeeds while the booking is still PENDING_PAYMENT is accepted, even if the deadline has just passed, because the ledger still holds the rooms. The state decides, not the clock.

## 11. Concurrency and idempotency

### Inventory: pessimistic locking
The booking-creation transaction:
Before it starts, any missing ledger rows for the stay are inserted, each in its own short transaction. If a concurrent request inserts the same row first, the unique constraint rejects the second insert and that request simply carries on. Doing this outside the booking transaction keeps a duplicate-key error from spoiling the booking transaction, and keeps each request to one database connection at a time.

1. Load the variant, its room type and property, and check the stay and the offer (active, booking type accepted, capacity).
2. Lock the ledger rows for every night (`SELECT ... FOR UPDATE`, 3-second lock timeout), **in ascending date order** (prevents deadlocks between overlapping stays).
3. Check every night has enough rooms available; if not, roll back with "sold out".
4. Add the room count to every night.
5. Create the booking, its nightly price lines and the captured policy version.
6. Commit, releasing the locks.

The lock is held for milliseconds. **The payment gateway is never called while a ledger lock is held.** Expiry and cancellation take the same locks, in the same order, to release rooms.

Pessimistic locking is used because the last rooms on popular dates are contended by design. Optimistic retries under that load waste work and produce false errors.

### Booking and other records: optimistic versions
Booking, payment attempt, refund attempt, property, room type and variant carry an optimistic version. Policy versions don't need one (they are immutable); ledger rows don't use one (they are locked pessimistically).

Races on one booking (payment vs expiry, payment vs cancel, double cancel, two payments) are settled by the version check. Starting a payment loads the booking with an optimistic force-increment lock, so two payments started at the same time for one booking can't both go ahead. The loser reloads and re-decides once, or gets a 409. If a successful payment loses to expiry or cancellation, the payment stays SUCCEEDED and a full refund is created with reason PAYMENT_AFTER_EXPIRY or PAYMENT_AFTER_CANCELLATION, matching what it lost to.

### Idempotency
- Create-booking and pay requests must carry an idempotency key.
- The key is stored on the record the request creates (booking or payment attempt), with a unique constraint and a fingerprint of the request.
- A repeated request with the same key and the same content returns the existing record's current state, and the gateway is not called again (a payment still INITIATED comes back as 202). A new payment with a different key is refused with "in progress" while an INITIATED attempt exists.
- Concurrent duplicates are settled by the unique constraint.
- The same key with different content is rejected.
- The key and its effect are written in one transaction, so they can't disagree. No responses are stored and no cleanup job is needed.
- Defence in depth: a CONFIRMED booking refuses any further payment, even with a new key.

## 12. Decision log: alternatives considered

| Topic | Chosen | Rejected and why |
|---|---|---|
| Room model | Count per variant | Individual physical rooms: room picking and harder concurrency for something the brief doesn't ask |
| Room capabilities | Variants with disjoint room pools | Add-on counter on top of the room type: allows double booking |
| Availability | Per-night ledger | Counting overlapping bookings: false "sold out" and nothing to lock |
| Owner structure | Owner with many properties | Optional chain link (separate path, ruled out by the brief); group tree (more than the brief needs) |
| Enum storage | STRING mapping | ORDINAL (corrupts on reorder); native database enum (schema change per value) |
| Hold timing | Reserve at booking with a 15-minute hold | Reserve on payment: charges guests for rooms already gone |
| Booking type config | Set of accepted types per property | Boolean flag per type: a column per new type |
| Price on booking | Total plus nightly lines | Total only (can't explain or support date pricing); versioned price inputs (many inputs, recalculation drifts) |
| Guest model | Customer table plus embedded staying guest | Person table with a BOOKER/GUEST type (a role belongs to the relationship) |
| Payment history | One record per attempt | One record per booking: loses history |
| Method details | Typed JSON details | Nullable columns per method; table per method |
| Refunds | Refund attempt table | Columns on payment; negative payments |
| Policy definition | Kind in code, tiers as data | Class per policy (duplication); tiers only (can't express other shapes) |
| Policy on booking | Immutable versions referenced by ID | Full JSON copy per booking (simpler, but less normalised) |
| Hotel-specific policies | Copy and customise (fields only, not built) | Live inheritance with overrides: platform edits change hotel terms silently |
| Inventory locking | Pessimistic, ordered | Optimistic (retry storms); conditional update (harder to read); in-JVM locks (one instance only) |
| Hold expiry | Job plus hold-expires-at on booking | Expiry time on the ledger row: one row can't hold many deadlines |
| Location | City and locality reference tables, embedded address | Free-text locality (typos split results); separate address table (join, sharing trap) |
| Identifiers | UUID v7 | Sequential numbers (guessable); UUID v4 (scattered index inserts) |
| Idempotency | Key on the domain record | Generic key table (second record to keep in step); in-memory map |

## 13. Out of scope and future extensions

- Party plots and other day-use venues: a new booking type with slot-based inventory.
- Rate plans: the same room sold refundable and non-refundable at different prices.
- Hotel copy-and-customise of policies (fields already present).
- New policy kinds such as "first night charged".
- Checked-in, completed and no-show states.
- Adult/child guest split and child ages.
- Locality aliases, autocomplete, and distance-based search (needs coordinates and a spatial index).
- Refund retry job and gateway reconciliation.
- Authentication: customers become accounts; booking history by account.
- At scale: Redis holds with time-to-live, a generic idempotency key store, transactional outbox for events.

## 14. Seed data

Loaded on start-up when the database is empty (`hotelbooking.seed.enabled`):

- Cities and localities: Ahmedabad (SG Highway, Navrangpura, Satellite), Mumbai (Bandra, Colaba, Juhu), Goa (Calangute, Panjim), Bengaluru (Indiranagar, Koramangala).
- Amenities: PROPERTY scope `WIFI`, `POOL`, `PARKING`, `SPA`, `GYM`, `RESTAURANT`; ROOM scope `AIR_CONDITIONING`, `SEA_VIEW`, `BALCONY`, `BATHTUB`.
- The three platform policies (Flexible, Moderate, Strict) with version 1 each.
- Two owners with five properties: Sea Breeze Hotel (Mumbai, with a Deluxe Sea View variant), Palm Grove Resort (Goa), Hotel Skyline (Ahmedabad), Asha's Homestay (Ahmedabad) and Fontainhas Heritage Villa (Goa).

## 15. Implementation notes

- The JSON column for payment method details works natively with Hibernate 7.4 (`@JdbcTypeCode(SqlTypes.JSON)`); no custom converter was needed. Stored as, for example, `{"type":"CARD","network":"VISA","last4":"4242"}`.
- STRING enums on H2 need `@JdbcTypeCode(SqlTypes.VARCHAR)` to become a varchar with a check constraint; without it Hibernate creates a native ENUM column (Section 2).
- UUID v7 ids come from Hibernate's `@UuidGenerator(style = VERSION_7)`.
- H2 is pinned to 2.3.232. In 2.4.240 a `check (column in (...))` constraint (the enum checks) stops working once the connection that created the table is closed, which the connection pool does after 30 minutes, so every booking insert failed after that.
- The in-memory database URL keeps `DB_CLOSE_DELAY=-1`, otherwise H2 drops the database whenever its last connection closes.
