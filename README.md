# Bus Reservation System

A Java Swing desktop application backed by MySQL for searching buses, selecting seats interactively, calculating passenger-specific fares, generating QR-coded PDF e-tickets, and sending booking confirmations via email.

---

## Features & Recent Updates

### Core Functionality
* **User Authentication & Dashboard:** Secure login flow leading into bus search, booking, booking history, and ticket cancellation modules.
* **Bus Search & Availability:** Filter available buses by source and destination and check real-time seat availability for a selected journey date.
* **Interactive Seat Map (`SeatSelectorDialog`):**
  * Visual seat layout showing available vs. already-booked seats for the chosen date.
  * **Strict Ticket-Count Enforcement:** Locks seat selection to the exact number of tickets entered by the user, preventing over-selection or incomplete confirmations.
* **Per-Passenger Modeling & Dynamic Fare Calculation (`Passenger`, `Fare`, `TicketService`):**
  * Captures individual passenger details (name, age, gender, student status) per selected seat.
  * Automatically applies age-based and student fare discounts and computes the itemized total.
* **Simulated Payment Validation:** Validates UPI IDs (`name@bank`) and 16-digit card numbers prior to committing reservations.
* **Transactional Seat Locking:** Uses row-level database locking (`SELECT ... FOR UPDATE`) and atomic transactions to prevent double-booking under concurrent requests.

### E-Tickets, Email & Booking History
* **PDF & QR Code E-Ticket Generation (`TicketPdf`, `QrCode`):** Automatically generates a downloadable `Ticket_<PNR>.pdf` receipt containing trip details, passenger breakdown, discounted fares, and an embedded verification QR code.
* **Asynchronous SMTP Email Delivery (`Mailer`):** Sends the generated PDF e-ticket directly to the user's email address via Gmail SMTP (`mail.properties`), with graceful local fallback if SMTP is unconfigured.
* **Grouped Booking History & Exact Timestamps (`MyBookings`):**
  * Displays multi-seat reservations booked together as clean, consolidated rows (grouped by bus, journey date, and booking timestamp) with bus names, routes, seat lists, and payment methods.
  * Records exact booking timestamps (`booked_at DATETIME`) instead of date-only midnight defaults, with one-click table refresh.

---

## Tech Stack

* **Language & UI:** Java (JDK 17+), Java Swing (Cross-Platform Look & Feel)
* **Database:** MySQL 8.x (`mysql-connector-j`)
* **Email:** Jakarta Mail / Angus Mail (`jakarta.mail-api`, `angus-mail`, `jakarta.activation-api`, `angus-activation`)
* **PDF & QR Generation:** Pure Java PDF & QR builder (`TicketPdf`, `QrCode`)

---

## Project Structure

```text
bus-ticket-booking-system/
├── src/
│   ├── Main.java
│   ├── auth/
│   ├── bus/
│   │   ├── BookTicket.java
│   │   ├── CancelTicket.java
│   │   ├── Fare.java
│   │   ├── MyBookings.java
│   │   ├── Passenger.java
│   │   ├── SearchBus.java
│   │   ├── SearchBusFinal.java
│   │   ├── SeatSelectorDialog.java
│   │   ├── TicketDetails.java
│   │   └── TicketService.java
│   ├── dashboard/
│   ├── db/
│   │   └── DBConnection.java
│   └── util/
│       ├── Mailer.java
│       ├── QrCode.java
│       └── TicketPdf.java
├── lib/                    # JDBC & Jakarta Mail JAR dependencies
├── mail.properties         # Local SMTP credentials (git-ignored)
└── .gitignore
```

---

## Setup & Installation

### 1. Database Configuration (MySQL)
Create the `tickets` database and ensure the `bus` and `booking` tables are set up:

```sql
CREATE DATABASE IF NOT EXISTS tickets;
USE tickets;

CREATE TABLE IF NOT EXISTS bus (
    bus_id INT PRIMARY KEY AUTO_INCREMENT,
    bus_name VARCHAR(100) NOT NULL,
    source VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    total_seats INT NOT NULL,
    departure_time VARCHAR(20),
    journey_hours DOUBLE,
    price DOUBLE NOT NULL
);

CREATE TABLE IF NOT EXISTS booking (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    bus_id INT NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    seat_number INT NOT NULL,
    journey_date DATE NOT NULL,
    payment_method VARCHAR(50),
    booked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bus_id) REFERENCES bus(bus_id)
);
```

*(If upgrading an existing `booking` table, run: `ALTER TABLE booking ADD COLUMN booked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;`)*

Update your database credentials in `src/db/DBConnection.java` if needed.

### 2. Email Configuration (Optional)
To enable live PDF e-ticket emails, create a `mail.properties` file in the project root (ignored by Git):

```properties
smtp.user=your.email@gmail.com
smtp.password=your_16_char_gmail_app_password
smtp.host=smtp.gmail.com
smtp.port=587
```

### 3. Compile & Run
Include all JARs in `lib/` on the classpath when compiling and running:

```bash
javac -cp "lib/*:src" -d out $(find src -name "*.java")
java -cp "lib/*:out" Main
```