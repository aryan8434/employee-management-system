# Employee Management

This is a simple app to manage employee records. An admin can add, view, edit and delete a record. The admin can also search for employees and filter the list.

## Features

- Add, view, edit and delete employees.
- Save all nine fields given in the assignment.
- Search by name or email.
- Filter by department and status.
- Show the employee list in pages.
- Ask before deleting a record.
- Show an error if a field is wrong or missing.
- Stop the same email or employee ID from being used twice.
- Save records in PostgreSQL.
- Download the list as a CSV file.
- View and try the APIs in Swagger UI or Postman.
- Run the app with Docker Compose.

## Tools used

| Part | Tool |
| --- | --- |
| Backend | Java 17, Spring Boot 3.5.7, Spring Data JPA |
| Database | PostgreSQL 17, Flyway |
| Frontend | HTML, CSS and JavaScript |
| Tests | JUnit 5, Mockito, MockMvc, Playwright |
| Build and API guide | Maven, springdoc OpenAPI |

The frontend files are in `src/main/resources/static`. Spring Boot runs the frontend and backend together.

## Start with Docker

Install Docker Desktop and start it. Docker includes the tools needed to run the app. You do not need to install Java, Maven or Node.js for this method.

1. Open a terminal in this project folder.
2. Run:

   ```sh
   docker compose up --build -d
   ```

3. The first build needs the internet. It may take a few minutes. To check if the app has started, run:

   ```sh
   docker compose logs -f app
   ```

   Wait until you see `Started EmployeeApplication`. Then press Ctrl+C. This closes the log view. The app keeps running.

4. Open [http://localhost:8080](http://localhost:8080).

The app creates the database tables on the first start. The employee list is empty at first. Docker keeps the saved records when you stop the app.

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

For this method, install JDK 17 or later and Maven 3.6.3 or later. You also need PostgreSQL 17. You can use Docker to run PostgreSQL:

```sh
docker compose up -d db
mvn spring-boot:run
```

If the Docker app is already running, stop it first with `docker compose stop app`. Then run the commands above. Both apps use port 8080, so they cannot run on that port at the same time.

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
- Postman collection: `docs/employee-management.postman_collection.json`
- Sample requests: `docs/api-examples.http`

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

See `docs/test-results.md` for the checks run on this version.

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

The screenshots show the running app with the sample records.

![Employee list](docs/screenshots/employee-list.png)
![Add employee form](docs/screenshots/add-employee.png)
![Edit employee form](docs/screenshots/edit-employee.png)
![Employee details](docs/screenshots/employee-details.png)
![Phone view](docs/screenshots/mobile-list.png)

## Notes

This app runs on your computer. It has no login page. The CSV export handles commas and quotes in values. It also adds a single quote before text that could be read as a spreadsheet formula.

Tool references: [Spring Boot setup](https://docs.spring.io/spring-boot/3.5/system-requirements.html) and [springdoc compatibility](https://springdoc.org/v2/).
