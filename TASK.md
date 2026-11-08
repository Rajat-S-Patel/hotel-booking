# Hotel Booking System

## Problem Statement

Backend for a hotel booking platform. Owners list their properties; guests find a property, book it for a date range, pay, and may cancel later.

### Property Discovery
- Search by city or locality, stay dates, and guest count
- Optional filters (price range, amenities, star rating), with new filters addable without rewriting search
- Results include only properties with availability for the requested dates

### Property Onboarding
- Register a property with name, location, room types, amenities, and pricing
- Owners can be a single standalone property or a chain managing many properties under one account
- A standalone property is handled as a chain of one, not as a separate code path

### Booking
- Reserve a given room type at a property for a date range and guest count
- Verify availability and block double-booking across overlapping dates
- Bookings move through a defined lifecycle of states

### Payment
- Collect payment for a booking
- Card and UPI sit behind one shared payment abstraction, so wallet and future methods can be added the same way
- Payment result updates booking state (e.g. success confirms the booking)

### Cancellation
- Cancel an existing booking
- Refund according to a pluggable cancellation policy
- Return held rooms to inventory so they can be found and booked again

## Technical Requirements

- Java 17 or later, built on Spring Boot
- Core operations (discover, onboard, book, pay, cancel) exposed through REST endpoints; a tested service layer is an acceptable alternative
- In-memory storage (collections or H2) is enough, but kept behind repository interfaces so a real database can replace it later
- External dependencies such as the payment gateway are mocked behind the project's own abstraction
- README explains how to build and run the service and lists key assumptions
