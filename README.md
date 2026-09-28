# Traffic E-Challan Issue and Repeat Offender Tracker

Spring Boot 3.3 · Java 21 · JPA/Hibernate · MySQL 8 · Spring Security (HTTP Basic + roles) · Bootstrap 5

## Run (fastest, no MySQL): in-memory H2
```
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
## Run with MySQL 8
```
# 1. start MySQL; the database `echallan_db` is created automatically
# 2. set your password (Windows PowerShell:  $env:DB_PASSWORD="yourpassword")
export DB_PASSWORD=yourpassword        # optional: DB_HOST DB_PORT DB_NAME DB_USERNAME
mvn spring-boot:run
```
Open http://localhost:8080 — logins: `admin / Admin@123`, `officer1 / Officer@123`
(override with env vars ADMIN_PASSWORD / OFFICER_PASSWORD; demo data loads only when the DB is empty).

## Test
```
mvn test        # FineCalculatorTest (unit) + ChallanFlowTest (H2 integration)
```
Postman: import `postman/echallan.postman_collection.json` (Basic auth via collection variables `user`/`pass`).

## Key design points
* Repeat count = same vehicle + same violation type + 12-month window ending at the violation time (`echallan.rolling-window-months`).
* Multiplier = rule with the highest `fromOccurrence <= occurrence`; type-specific rules override global rules (admin-editable).
* Fine, multiplier, occurrence stored on the challan → later rule changes never alter old challans.
* Payments: row lock on the challan + unique `success_challan_id` column → a second SUCCESS payment is impossible even under concurrent requests.
* Simulated payments/notifications only (`simulateOutcome`: SUCCESS / FAILED / PENDING).
