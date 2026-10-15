const { test, expect } = require('@playwright/test');

test('add, view, edit, filter, export and delete an employee', async ({ page, request }) => {
  const suffix = Date.now().toString().slice(-9);
  const code = `E2E-${suffix}`;
  const email = `test${suffix}@example.com`;
  let recordId;
  const pageErrors = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  try {
    await page.goto('/');
    await page.getByRole('link', { name: 'Add employee', exact: true }).first().click();
    await page.getByLabel('Full name', { exact: false }).fill('Test Employee');
    await page.getByLabel('Email address', { exact: false }).fill(email);
    await page.getByLabel('Phone number', { exact: false }).fill('+91 98765 43210');
    await page.getByLabel('Employee ID', { exact: false }).fill(code);
    await page.getByLabel('Department', { exact: false }).fill('Test Department');
    await page.getByLabel('Designation', { exact: false }).fill('Test Engineer');
    await page.getByLabel('Date of joining', { exact: false }).fill('2025-08-12');
    await page.getByLabel('Annual salary', { exact: false }).fill('600000.25');
    await page.getByRole('button', { name: 'Add employee', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Employee details', exact: true })).toBeVisible();
    recordId = Number(page.url().match(/employees\/(\d+)/)[1]);
    await expect(page.locator('main')).toContainText(email);
    await expect(page.locator('main')).toContainText('6,00,000.25');
    await page.getByRole('link', { name: 'Edit employee', exact: true }).click();
    await page.getByLabel('Designation', { exact: false }).fill('Senior Test Engineer');
    await page.getByLabel('Employment status', { exact: false }).selectOption('INACTIVE');
    await page.getByRole('button', { name: 'Save changes' }).click();
    await expect(page.getByRole('heading', { name: 'Employee details', exact: true })).toBeVisible();
    await expect(page.locator('main')).toContainText('Senior Test Engineer');
    await page.getByRole('link', { name: 'Back to employees', exact: true }).click();
    await page.getByLabel('Search by name or email', { exact: true }).fill(email);
    await expect(page.getByRole('row').filter({ hasText: email })).toHaveCount(1);
    await page.getByLabel('Filter by department').selectOption('Test Department');
    await page.getByLabel('Filter by status').selectOption('INACTIVE');
    await expect(page.getByRole('row').filter({ hasText: email })).toContainText('Inactive');
    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('link', { name: 'Export CSV' }).click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toBe('employees.csv');
    await page.getByRole('button', { name: 'Delete Test Employee', exact: true }).click();
    await expect(page.getByRole('dialog')).toContainText(code);
    await page.getByRole('button', { name: 'Cancel', exact: true }).click();
    await expect(page.getByRole('row').filter({ hasText: email })).toHaveCount(1);
    await page.getByRole('button', { name: 'Delete Test Employee', exact: true }).click();
    await page.getByRole('button', { name: 'Delete employee', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'No matching employees' })).toBeVisible();
    recordId = undefined;
    expect(pageErrors).toEqual([]);
  } finally {
    if (recordId) await request.delete(`/api/employees/${recordId}`);
    else {
      const response = await request.get(`/api/employees?search=${encodeURIComponent(email)}`);
      if (response.ok()) for (const employee of (await response.json()).content) {
        if (employee.email === email) await request.delete(`/api/employees/${employee.id}`);
      }
    }
  }
});

test('shows duplicate email errors and keeps the entered values', async ({ page, request }) => {
  const suffix = Date.now().toString().slice(-9);
  const email = `duplicate${suffix}@example.com`;
  const employee = { employeeId: `DU-${suffix}`, fullName: 'Existing Employee', email, phone: '9876543210',
    department: 'Test Department', designation: 'Engineer', dateOfJoining: '2025-08-12', salary: 500000, status: 'ACTIVE' };
  const response = await request.post('/api/employees', { data: employee });
  expect(response.status()).toBe(201);
  const saved = await response.json();
  try {
    await page.goto('/#/employees/new');
    await page.getByLabel('Full name', { exact: false }).fill('New Employee');
    await page.getByLabel('Email address', { exact: false }).fill(email);
    await page.getByLabel('Phone number', { exact: false }).fill('9876543210');
    await page.getByLabel('Employee ID', { exact: false }).fill(`NEW-${suffix}`);
    await page.getByLabel('Department', { exact: false }).fill('Engineering');
    await page.getByLabel('Designation', { exact: false }).fill('Engineer');
    await page.getByLabel('Date of joining', { exact: false }).fill('2025-08-12');
    await page.getByLabel('Annual salary', { exact: false }).fill('500000');
    await page.getByRole('button', { name: 'Add employee', exact: true }).click();
    await expect(page.locator('#email-error')).toHaveText('This email is already in use.');
    await expect(page.getByLabel('Full name', { exact: false })).toHaveValue('New Employee');
    await expect(page.locator('#email')).toHaveAttribute('aria-invalid', 'true');
  } finally { await request.delete(`/api/employees/${saved.id}`); }
});

test('list and add form fit a phone screen', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Employees', exact: true })).toBeVisible();
  await expect(page.getByLabel('Search by name or email')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByRole('link', { name: 'Add employee', exact: true }).first().click();
  await expect(page.getByLabel('Full name', { exact: false })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test('pagination returns to the last valid page after a delete', async ({ page, request }) => {
  const suffix = Date.now().toString().slice(-9);
  const department = `Paging ${suffix}`;
  const ids = [];
  try {
    for (let index = 1; index <= 6; index++) {
      const response = await request.post('/api/employees', { data: {
        employeeId: `PG-${suffix}-${index}`, fullName: `Paging Employee ${index}`,
        email: `page${suffix}.${index}@example.com`, phone: '9876543210', department,
        designation: 'Engineer', dateOfJoining: '2025-08-12', salary: 500000, status: 'ACTIVE'
      } });
      expect(response.status()).toBe(201);
      ids.push((await response.json()).id);
    }
    await page.goto('/');
    await page.getByLabel('Filter by department').selectOption(department);
    await expect(page.locator('#result-count')).toHaveText('6 people');
    await page.getByLabel('Rows', { exact: true }).selectOption('5');
    await expect(page.locator('#pagination')).toContainText('Showing 1–5 of 6 employees');
    await page.getByRole('button', { name: 'Next', exact: true }).click();
    await expect(page.locator('#pagination')).toContainText('Showing 6–6 of 6 employees');
    await page.getByRole('button', { name: 'Delete Paging Employee 6', exact: true }).click();
    await page.getByRole('button', { name: 'Delete employee', exact: true }).click();
    await expect(page.locator('#pagination')).toContainText('Showing 1–5 of 5 employees');
    await expect(page.getByRole('button', { name: 'Next', exact: true })).toBeDisabled();
  } finally {
    for (const id of ids) await request.delete(`/api/employees/${id}`);
  }
});
