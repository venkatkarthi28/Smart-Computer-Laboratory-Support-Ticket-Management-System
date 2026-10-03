# Smart Computer Laboratory IT Support & Ticket Management System

A web application where students report problems on lab computers, technicians fix them, and administrators
monitor laboratories, SLAs and repeated hardware problems.

## Problem statement
In a college with several computer laboratories, faults are usually reported verbally or on paper. Nobody can see
which computer failed how often, who is working on it, or whether the repair was finished in time. This system gives
every fault a ticket with an owner, a deadline (SLA), a full status history and a maintenance record per computer.

## Features
**Student**: register and log in, report a problem on an existing computer, track tickets, comment, confirm or reopen a
resolved ticket, rate the service (1-5), see in-app notifications and a personal dashboard.

**Technician**: see open tickets of the laboratories they serve, accept, start work, record diagnosis and resolution,
view the maintenance history of a computer, dashboard with workload and average resolution time.

**Administrator**: manage students, technicians, laboratories, computers, categories and priorities; assign tickets;
monitor SLA breaches; dashboard with charts, technician workload and a list of computers with repeated problems.

**System rules**: strict status workflow, status history and notification for every transition, SLA deadlines
(LOW 72 h, MEDIUM 24 h, HIGH 4 h, CRITICAL 1 h) checked by a background job every minute, JWT security with three roles.

### Ticket workflow
```
OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED
                         ^             |
                         |             v
                         +----- REOPENED   (student answers "no")
```
Invalid transitions are rejected with HTTP 409.

## Architecture
```
Browser (HTML/CSS/JS, Bootstrap 5, Chart.js)
        | fetch + JWT
REST controllers  ->  Services (business rules)  ->  Repositories  ->  JPA/Hibernate  ->  MySQL 8
```
Packages (`com.example.labsupport`): `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `exception`, `security`, `config`.
Controllers return DTOs, never entities. Passwords are BCrypt hashed and never returned by the API.

## Tech stack
Java 21, Spring Boot 4.1.1 (Web MVC, Data JPA, Security, OAuth2 resource server for JWT/HS256), Hibernate, MySQL 8, Maven,
HTML/CSS/JavaScript, Bootstrap 5, Chart.js, JUnit 5 + Spring Boot Test, Postman.

## Installation
Requirements: JDK 21, MySQL 8, Git. (Maven is included as `mvnw`.)

### Database
```sql
CREATE DATABASE lab_support_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'labsupport_user'@'localhost' IDENTIFIED BY 'your-password';
GRANT ALL PRIVILEGES ON lab_support_db.* TO 'labsupport_user'@'localhost';
```
Tables are created automatically on the first start (development profile).

### Environment variables
Create a file named `.env` in the project root (it is git-ignored):
```
DB_USERNAME=labsupport_user
DB_PASSWORD=your-password
SEED_PASSWORD=ChooseADemoPassword1
JWT_SECRET=a-random-string-of-at-least-32-characters
```
`.env.example` shows the format. `DB_URL` and `PORT` are optional.

### Run
```powershell
.\mvnw.cmd spring-boot:run
```
Open http://localhost:8080/ . Demo data (categories, priorities, Lab 1 with LAB1-PC-001..010, three demo users) is created once.

### Tests
```powershell
.\mvnw.cmd test        # needs MySQL running
```
Postman: import `postman/LabSupport.postman_collection.json`, set the `seedPassword` variable, run the collection.

## Demo credentials
| Role | Email | Password |
|---|---|---|
| Admin | admin@college.com | value of `SEED_PASSWORD` in your `.env` |
| Technician | technician@college.com | same |
| Student | student@college.com | same |

## API overview
| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register` (always STUDENT), `POST /api/auth/login`, `GET /api/auth/me` |
| Reference | `GET /api/laboratories`, `/api/computers`, `/api/computers/{id}`, `/api/categories`, `/api/priorities` |
| Tickets | `POST /api/tickets`, `GET /api/tickets`, `GET /api/tickets/{id}`, `/history`, `POST /{id}/comments` |
| Ticket actions | technician: `PUT /api/tickets/{id}/accept`, `/start`, `/resolve`; student: `PUT /confirm`, `/reopen`, `POST /feedback` |
| Dashboards | `GET /api/student/dashboard`, `/api/technician/dashboard`, `/api/admin/dashboard` |
| Technician | `GET /api/technician/tickets`, `/api/technician/tickets/pool` |
| Admin | `/api/admin/students`, `/technicians`, `/laboratories`, `/computers`, `/categories`, `/priorities`, `GET /tickets`, `PUT /tickets/{id}/assign`, `POST /sla/check` |
| Analytics | `/api/admin/technicians/workload`, `/computers/problematic`, `/analytics/by-category`, `/analytics/by-priority` |
| Maintenance | `GET /api/computers/{id}/maintenance-history` (staff only) |
| Notifications | `GET /api/notifications`, `/unread-count`, `PUT /{id}/read`, `/read-all` |

Errors use one JSON shape: `{ "timestamp", "status", "error", "message" }` (validation errors add `fieldErrors`).

## Screenshots
*(add your own screenshots to `docs/screenshots/` and link them here)*
- Login page: `docs/screenshots/login.png`
- Student dashboard: `docs/screenshots/student-dashboard.png`
- Create ticket: `docs/screenshots/create-ticket.png`
- Technician tickets: `docs/screenshots/technician-tickets.png`
- Admin dashboard with charts: `docs/screenshots/admin-dashboard.png`

## Deployment
See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Future enhancements
- E-mail or SMS notifications in addition to in-app notifications
- File/photo attachments on tickets
- Export reports to PDF/Excel
- Scheduled preventive-maintenance reminders per computer
- Refresh tokens and a "log out everywhere" option
