# Test results

Checked on 15 October 2026.

## Automated checks

| Group | Tests passed | What was checked |
| --- | ---: | --- |
| Service tests | 14 | Add, view, edit, delete, duplicate checks, page data, counts and CSV output |
| API tests | 12 | Response codes, required fields, email, salary, dates, status, page limits and errors |
| PostgreSQL tests | 8 | Full CRUD, stored values, unique database rules, search, filters, pages, counts, CSV and OpenAPI |
| Browser tests | 4 | Full form flow, duplicate errors, phone layout and pages after a delete |
| Total | 38 | All passed |

The Java tests used JUnit 5 and Mockito with Java 17. Database tests used a real PostgreSQL 17 database named `employee_tests`. Browser tests used Playwright with Chrome in headless mode.

## Other checks

- Built the Docker image and started the app with Docker Compose.
- Checked that Flyway makes the tables on an empty database.
- Loaded 12 sample employees through the setup script.
- Checked the API guide and OpenAPI JSON in the running app.
- Checked that test records are removed after browser tests.
- Checked the list and form at a phone width of 390 pixels.
- Took and reviewed five screenshots of the running app.

## Run the checks again

Start the app, then run:

```sh
docker compose --profile test run --rm tests
npm ci
npx playwright install chromium
npm run test:ui
```

If Chrome is already installed, you can set `BROWSER_CHANNEL=chrome` to use it for browser tests. Leave this value unset to use the Chromium browser installed by Playwright.

Service and API test reports are written to `target/surefire-reports`. PostgreSQL test reports are written to `target/failsafe-reports`. These build files are not part of the source ZIP.
