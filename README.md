# Employee Management System

A web app for managing employee records. It has pages to add an employee, view details, edit a record and delete a record. Employee data is saved in PostgreSQL.

![Employee list](docs/screenshots/employee-list.png)

[Run the app](#run-with-docker) · [API guide](#api-guide) · [Tests](#run-the-tests) · [Screenshots](#screenshots)

## Features

- Add, view, edit and delete employee records.
- Search by name or email and filter by department or status.
- Split long lists into pages, with a choice of rows per page.
- Ask for confirmation before deleting a record.
- Show clear errors for missing fields, wrong values and repeated emails or employee IDs.
- Show employee counts and department counts.
- Download matching records as a CSV file.
- Use the app on a computer or phone.

## Employee details

Each employee record contains these fields:

| Field | Example |
| --- | --- |
| Employee ID | `EMP-101` |
| Full name | Ananya Rao |
| Email address | `ananya.rao@example.com` |
| Phone number | `+91 98765 43210` |
| Department | Engineering |
| Designation | Backend Developer |
| Date of joining | `2025-08-12` |
| Annual salary in INR | `650000.00` |
| Employment status | Active or Inactive |

## Tools used

| Part | Tool |
| --- | --- |
| Backend | Java 17, Spring Boot 3.5.7, Spring Data JPA |
| Database | PostgreSQL 17, Flyway |
| Frontend | HTML, CSS and JavaScript |
| Tests | JUnit 5, Mockito, MockMvc, Playwright |
| Build | Maven |
| API guide | Swagger UI and OpenAPI |

The frontend files are in `src/main/resources/static`. Spring Boot runs the frontend and backend together.

## Run with Docker

You need Git and Docker Desktop with Docker Compose. Start Docker Desktop, then open a terminal and run:

```sh
git clone https://github.com/aryan8434/employee-management-system.git
cd employee-management-system
docker compose up --build -d
```

If you downloaded the source ZIP, extract it and run the Docker command inside the folder that contains `compose.yaml`.

The first build needs the internet and may take a few minutes. Java and Maven are included in the Docker build. Node.js is not needed to run the app. Check the startup log with:

```sh
docker compose logs -f app
```

When you see `Started EmployeeApplication`, open [http://localhost:8080](http://localhost:8080). Press Ctrl+C to leave the log view. The app keeps running.

Docker creates the database, and Flyway creates the tables. The employee list is empty at first. Records stay in the database volume when you stop the app.

Useful local addresses:

| Page | Address |
| --- | --- |
| App | [localhost:8080](http://localhost:8080) |
| Swagger UI | [localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| OpenAPI JSON | [localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

To stop the app and keep the data:

```sh
docker compose down
```

## Add sample records

You can use this step to add 12 sample employees. These records use example email addresses. If you run the script again, it skips records that are already saved.

On Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/load-demo.ps1
```

On macOS or Linux:

```sh
sh scripts/load-demo.sh
```

Refresh the page to see the sample employees.

## Run with local Java and Maven

For this method, install JDK 17 or later and Maven 3.6.3 or later. You also need PostgreSQL 17. You can use Docker to run PostgreSQL.

If the Docker app is already running, stop it first with `docker compose stop app`. Then run:

```sh
docker compose up -d db
mvn spring-boot:run
```

The Docker app and the local Maven app both use port 8080. Run one app at a time on that port.

Default database settings:

| Setting | Default value |
| --- | --- |
| Host and port | `localhost:15432` |
| Database | `employees` |
| User | `employee_app` |
| Password | `employee_local` |

If you installed PostgreSQL yourself, run this once as the `postgres` user on a new setup:

```sh
psql -U postgres -v ON_ERROR_STOP=1 -f database/setup-local.sql
```

This script creates the app user and two databases. If you already have a user, you can create an empty database named `employees` for that user instead.

Before starting Maven, set the database address, user and password in your terminal. The app uses Flyway to create the tables.

Example in PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/employees'
$env:DB_USER = 'employee_app'
$env:DB_PASSWORD = 'employee_local'
mvn spring-boot:run
```

## Change Docker ports or settings

Copy `.env.example` to a file named `.env`. Change the values in that file if needed. The app uses port `8080`. The database uses port `15432`.

If port 8080 is busy, set `APP_PORT=8086`. Then open `http://localhost:8086`.

Run `docker compose up --build -d` after changing the file. Docker sets the database name, user and password when it first creates the database. Changing the file later does not change a user that is already saved in the database.

Docker Compose reads the `.env` file. When you run Maven yourself, set the values in your terminal. Use `PORT` to change the port for a local Maven run.

## API guide

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- [Postman collection](docs/employee-management.postman_collection.json)
- [Sample API requests](docs/api-examples.http)

| Method | Path | Use |
| --- | --- | --- |
| GET | `/api/employees` | List, search and filter employees |
| GET | `/api/employees/{id}` | View one employee |
| POST | `/api/employees` | Add an employee |
| PUT | `/api/employees/{id}` | Update all employee fields |
| DELETE | `/api/employees/{id}` | Delete an employee |
| GET | `/api/employees/departments` | List departments used in records |
| GET | `/api/employees/summary` | Count all employees and departments |
| GET | `/api/employees/export` | Export all matching records as CSV |

Example:

```text
/api/employees?search=rao&department=Engineering&status=ACTIVE&page=0&size=5
```

Page numbers start at `0`. Each page can have `1` to `100` records. The list is sorted by name. If two names are the same, the record ID decides their order.

You can search with part of a name or email. Capital and small letters give the same result. This also applies to the department filter. You can use search and both filters together. The CSV file includes all records that match these filters.

Each saved record has a number called `id`. Use this number in an API path, such as `/api/employees/1`. The form has another field called `employeeId`. This is the employee's ID, such as `EMP-101`.

## Record rules

- Fill in every field. The app removes extra spaces from the start and end of text.
- An employee ID can have up to 20 letters, numbers or hyphens. The app saves its letters as capital letters.
- The app saves email addresses in small letters. Two employees cannot have the same email or employee ID. Changing capital letters to small letters does not make them different.
- Salary means yearly salary in INR. It can be zero or more. It can have up to two digits after the decimal point.
- A phone number can start with `+`. It can also have spaces, brackets and hyphens. Use 7 to 20 characters.
- Dates use `YYYY-MM-DD`. Future joining dates are allowed.
- Status must be `ACTIVE` or `INACTIVE`.
- Enter the department name in the employee form. Saved department names appear in the filter list.

The API returns `400` for wrong input and `404` if a record is not found. It returns `409` if an email or employee ID is already in use. The form shows an error next to the field that needs a change.

## Run the tests

The completed local check on 15 October 2026 passed all 38 tests. It used a fresh copy of the source ZIP and a new PostgreSQL database.

| Test group | Passed |
| --- | ---: |
| Service tests | 14 |
| API tests | 12 |
| PostgreSQL tests | 8 |
| Browser tests | 4 |
| **Total** | **38** |

Service and API tests do not need a database:

```sh
mvn test
```

Or run them with Docker:

```sh
docker compose --profile test run --rm tests mvn -B test
```

To run all Java tests, including real PostgreSQL tests:

```sh
docker compose --profile test run --rm tests
```

The setup creates a separate database named `employee_tests`. Database tests use and clear this database. Before clearing it, they check that its name ends in `_tests`.

For a local Maven run, start the database and use:

```sh
mvn verify -Pintegration
```

If you use your own PostgreSQL server, create `employee_tests` first. Set its address, user and password with `TEST_DB_URL`, `TEST_DB_USER` and `TEST_DB_PASSWORD`. Keep test data in this separate database.

Browser tests need Node.js 20 or later. Start the app, then run:

```sh
npm ci
npx playwright install chromium
npm run test:ui
```

If the app uses a different address, set `APP_URL` to that address. For example, use `http://localhost:8086`. Browser tests add records for testing and delete them after each test.

See the [test notes](docs/test-results.md) for the checks and report locations.

## Project files

```text
src/main/java/com/example/employees/
  employee/             Employee model, API, service, filters and database access
  error/                Error messages and response handling
  config/               OpenAPI setup
src/main/resources/
  static/               Frontend pages, styles and scripts
  db/migration/         Table setup through Flyway
src/test/               Service, API and PostgreSQL tests
database/               Test database setup and sample records
e2e/                    Browser tests
docs/                   API requests, test notes and screenshots
scripts/                Sample data scripts
```

The controller receives an API request. The service checks the employee details. The repository reads and saves the data in PostgreSQL. The database also checks emails and employee IDs. This stops the same value from being saved twice, even when two requests come at the same time.

## Screenshots

The main screenshot above shows the employee list. These images show the other pages and the phone layout:

- [Add employee form](docs/screenshots/add-employee.png)
- [Edit employee form](docs/screenshots/edit-employee.png)
- [Employee details](docs/screenshots/employee-details.png)
- [Phone view](docs/screenshots/mobile-list.png)

## Notes

This is a local app with no login page. Departments are entered on the employee form, and saved department names appear in the filter. There is no separate department management page.

The CSV export handles commas and quotes in values. It also adds a single quote before text that could be read as a spreadsheet formula.

Tool references: [Spring Boot setup](https://docs.spring.io/spring-boot/3.5/system-requirements.html) and [springdoc compatibility](https://springdoc.org/v2/).
