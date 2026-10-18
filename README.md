# Hotel Booking Application

Spring Boot 3 + Thymeleaf + Spring Security + JPA. Implements the 24 user stories (US001–US024) from the
Sprint_2 sheet for three roles: **Customer**, **Admin** and **Staff**.

> **Important:** this code was written without being able to compile or run it (the environment it was
> produced in could not download Maven dependencies). It was reviewed by hand and checked for consistency,
> but expect a few small compile or runtime fixes on the first build. Please run it and report any error text.

## Requirements
* Java 17 or newer
* Maven 3.8+ (internet access for the first build)

## Run
```
mvn spring-boot:run
```
Open http://localhost:8080. The default database is a file-based H2 database in `./data` (created automatically,
sample rooms and accounts are added on first start). To build a jar: `mvn package` (add `-DskipTests` to skip tests).

### MySQL instead of H2
```
CREATE DATABASE hoteldb;
java -jar target/hotel-booking-1.0.0.jar --spring.profiles.active=mysql
```
Edit credentials in `src/main/resources/application-mysql.properties`.

## Demo accounts
| Username | Password | Role |
|---|---|---|
| admin | Admin@123 | Admin |
| billing.staff / maintenance.staff / housekeeping.staff | Staff@123 | Staff |
| customer1 | Customer@123 | Customer |

Set `hotel.seed-demo-data=false` in `application.properties` to disable the sample data.

## What is where
| Area | Customer | Admin | Staff |
|---|---|---|---|
| Account | register, login, forgot password, profile | login, user management (create, edit, reset password, activate/deactivate) | login |
| Rooms | search, book, pay, receipt | add, edit, delete, bulk CSV upload, search/filter/sort | – |
| Bookings | history, details, modify, cancel, invoice PDF | reservation list, create, edit, cancel | – |
| Billing | – | bills with line items, search/filter | – |
| Complaints | raise, edit (while Open), confirm/reopen | assign, prioritise, update, filter | work on assigned complaints |
| Reporting | – | dashboard and reports | – |

## Business rules (all configurable in `application.properties`)
* Price = nights × rate + GST 12 % + service charge 5 %.
* Cancellation refund: 100 % at least 48 h before check-in (14:00), 50 % between 24 and 48 h, 0 % after. Admin cancellation refunds in full.
* Modification of a confirmed booking allowed up to 24 h before check-in; price difference is charged or refunded.
* A pending booking holds the room for 10 minutes; the payment page expires after 10 minutes of inactivity.
* Account locks for 15 minutes after 5 failed logins. Admin-created and admin-reset accounts must change the temporary password at first login.
* Complaints are auto-assigned to the least-loaded staff member whose expertise matches the category.

## Things to know
* **Payments are simulated.** No real gateway is called. For card payments use any 16 digits; a number ending in `0000` is declined, to demonstrate the failure path.
* E-mail/SMS notifications are written to the application log and shown in the in-app notification list; no mail server is configured.
* Admin lists filter, sort and paginate in memory (10 per page) – fine for a hotel-sized data set, not for very large ones.
* The sheet has a few inconsistencies; where it contradicted itself the more specific rule was used.

## Tests
`mvn test` runs unit tests for the validation patterns, password generator, CSV parser and stay validation.
