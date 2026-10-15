# Test results

Checked on 15 October 2026.

## Tests run

| Group | Tests passed | What was checked |
| --- | ---: | --- |
| Service tests | 14 | Add, view, edit and delete records. Check repeated emails and IDs, pages, counts and CSV files. |
| API tests | 12 | Check API replies, missing fields, wrong values and page limits. |
| PostgreSQL tests | 8 | Check saved data, search, filters, pages, counts, CSV files and the API guide. |
| Browser tests | 4 | Use the forms, check errors, test the phone view and check pages after a delete. |
| Total | 38 | All passed |

Java tests used Java 17, JUnit 5 and Mockito. Database tests used PostgreSQL 17 and a database named `employee_tests`. Browser tests used Playwright and Chrome. Chrome ran in the background without an open browser window.

## Other checks

- Built the Docker image and started the app with Docker Compose.
- Checked that the app makes the tables in an empty database.
- Loaded 12 sample employees through the setup script.
- Checked the API guide and OpenAPI JSON in the running app.
- Checked that browser tests delete their test records.
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

If Chrome is already installed, set `BROWSER_CHANNEL=chrome` to use it for the tests. Otherwise, leave this value empty. The tests will use the Chromium browser installed by Playwright.

Service and API test reports are saved in `target/surefire-reports`. Database test reports are saved in `target/failsafe-reports`. The source ZIP does not include these build files.
