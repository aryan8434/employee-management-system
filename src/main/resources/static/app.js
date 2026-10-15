const main = document.querySelector('#main');
const state = { search: '', department: '', status: '', page: 0, size: 10, listVersion: 0, routeVersion: 0 };
const dialog = document.querySelector('#delete-dialog');
let pendingDelete = null;
let toastTimer;
let searchTimer;

const icon = name => `<svg class="icon" aria-hidden="true"><use href="#icon-${name}"/></svg>`;
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
const initials = name => name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();
const statusBadge = status => `<span class="status ${status === 'INACTIVE' ? 'inactive' : ''}">${status === 'ACTIVE' ? 'Active' : 'Inactive'}</span>`;
const formatDate = date => new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(`${date}T12:00:00`));
const formatSalary = salary => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(salary);

async function api(path, options = {}) {
  let response;
  try {
    response = await fetch(`/api${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...options.headers } });
  } catch {
    throw new Error('Cannot reach the server. Check that the app is running.');
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    const error = new Error(body.message || 'Could not complete this request. Please try again.');
    error.fields = body.errors || {};
    throw error;
  }
  return response.status === 204 ? null : response.json();
}

function toast(message, isError = false) {
  const element = document.querySelector('#toast');
  clearTimeout(toastTimer);
  element.textContent = message;
  element.classList.toggle('error', isError);
  element.hidden = false;
  toastTimer = setTimeout(() => { element.hidden = true; }, 4500);
}

function queryString(withPage = true) {
  const query = new URLSearchParams();
  for (const key of ['search', 'department', 'status']) if (state[key]) query.set(key, state[key]);
  if (withPage) { query.set('page', state.page); query.set('size', state.size); }
  return query.toString();
}

function pageError(message) {
  main.innerHTML = `<a class="back-link" href="#/employees">${icon('arrow')}Back to employees</a><div class="panel empty-state"><h2>Could not load this page</h2><p>${escapeHtml(message)}</p><button id="retry-page" class="button secondary">Try again</button></div>`;
  document.querySelector('#retry-page').addEventListener('click', renderRoute);
}

function listPage() {
  document.title = 'Employees | People';
  const cards = [ ['Total employees', 'total', 'people', 'All employee records'], ['Active employees', 'active', 'check', 'Employees marked Active'], ['Inactive employees', 'inactive', 'people', 'Employees marked Inactive'], ['Departments', 'departments', 'building', 'Departments in the list'] ];
  main.innerHTML = `<div class="page-heading"><div><p class="eyebrow">EMPLOYEE MANAGEMENT</p><h1>Employees</h1><p class="lead">Add, view, edit and delete employee records.</p></div><div class="heading-actions"><a class="button secondary" id="export-link" href="/api/employees/export">${icon('download')}Export CSV</a><a class="button" href="#/employees/new">${icon('plus')}Add employee</a></div></div>
    <section class="stats" aria-label="All employee counts">${cards.map(([title, key, symbol, note]) => `<div class="stat"><div class="stat-top"><span>${title}</span><span class="stat-icon">${icon(symbol)}</span></div><div class="stat-number" id="stat-${key}">—</div><div class="stat-note">${note}</div></div>`).join('')}</section>
    <section class="panel"><div class="panel-title"><h2>Employee list<span class="count-tag" id="result-count">Loading</span></h2><span class="panel-note">View and update saved records.</span></div>
    <div class="filters"><div class="search-box">${icon('search')}<label class="sr-only" for="search">Search by name or email</label><input id="search" type="search" placeholder="Search by name or email..." maxlength="150" value="${escapeHtml(state.search)}"></div><label class="sr-only" for="department-filter">Filter by department</label><select id="department-filter"><option value="">All departments</option></select><label class="sr-only" for="status-filter">Filter by status</label><select id="status-filter"><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select><button class="filter-reset" id="clear-filters" type="button">Clear</button></div>
    <div id="list-content" aria-live="polite"><div class="loading">Loading employees...</div></div><div id="pagination"></div></section>`;
  document.querySelector('#status-filter').value = state.status;
  document.querySelector('#search').addEventListener('input', event => {
    state.search = event.target.value;
    state.page = 0;
    clearTimeout(searchTimer);
    searchTimer = setTimeout(loadList, 250);
  });
  for (const key of ['department', 'status']) {
    document.querySelector(`#${key}-filter`).addEventListener('change', event => {
      state[key] = event.target.value;
      state.page = 0;
      loadList();
    });
  }
  document.querySelector('#clear-filters').addEventListener('click', () => {
    state.search = ''; state.department = ''; state.status = ''; state.page = 0;
    document.querySelector('#search').value = '';
    document.querySelector('#department-filter').value = '';
    document.querySelector('#status-filter').value = '';
    clearTimeout(searchTimer);
    loadList();
  });
  loadList();
}

async function loadList() {
  const version = ++state.listVersion;
  const routeVersion = state.routeVersion;
  if (!document.querySelector('#list-content')) return;
  document.querySelector('#list-content').setAttribute('aria-busy', 'true');
  document.querySelector('#export-link').href = `/api/employees/export?${queryString(false)}`;
  try {
    const [page, summary, departments] = await Promise.all([
      api(`/employees?${queryString()}`), api('/employees/summary'), api('/employees/departments')
    ]);
    if (version !== state.listVersion || routeVersion !== state.routeVersion) return;
    if (state.page > Math.max(0, page.totalPages - 1)) {
      state.page = Math.max(0, page.totalPages - 1);
      return loadList();
    }
    for (const key of ['total', 'active', 'inactive', 'departments']) document.querySelector(`#stat-${key}`).textContent = summary[key];
    const departmentSelect = document.querySelector('#department-filter');
    departmentSelect.innerHTML = '<option value="">All departments</option>' + [...new Set([...departments, ...(state.department ? [state.department] : [])])].map(name => `<option>${escapeHtml(name)}</option>`).join('');
    departmentSelect.value = state.department;
    document.querySelector('#result-count').textContent = `${page.totalElements} ${page.totalElements === 1 ? 'person' : 'people'}`;
    const content = document.querySelector('#list-content');
    content.removeAttribute('aria-busy');
    if (page.content.length === 0) {
      const filtered = state.search || state.department || state.status;
      content.innerHTML = `<div class="empty-state"><div class="empty-icon">${icon('people')}</div><h2>${filtered ? 'No matching employees' : 'No employees yet'}</h2><p>${filtered ? 'Try another name, email or filter.' : 'Add your first employee to get started.'}</p>${filtered ? '' : `<a class="button" href="#/employees/new">${icon('plus')}Add employee</a>`}</div>`;
    } else {
      content.innerHTML = `<div class="table-wrap"><table><caption class="sr-only">Employee records</caption><thead><tr><th scope="col">Employee</th><th scope="col">Employee ID</th><th scope="col">Department</th><th scope="col">Designation</th><th scope="col">Status</th><th scope="col">Actions</th></tr></thead><tbody>${page.content.map((employee, index) => `<tr><td><div class="employee-cell"><span class="avatar tone-${index % 4}" aria-hidden="true">${escapeHtml(initials(employee.fullName))}</span><div><a class="employee-name" href="#/employees/${employee.id}">${escapeHtml(employee.fullName)}</a><span class="employee-email">${escapeHtml(employee.email)}</span></div></div></td><td><span class="employee-id">${escapeHtml(employee.employeeId)}</span></td><td>${escapeHtml(employee.department)}</td><td>${escapeHtml(employee.designation)}</td><td>${statusBadge(employee.status)}</td><td><div class="row-actions"><a class="view-link" href="#/employees/${employee.id}">View</a><a class="icon-button" href="#/employees/${employee.id}/edit" aria-label="Edit ${escapeHtml(employee.fullName)}">${icon('edit')}</a><button class="icon-button delete" data-delete="${employee.id}" aria-label="Delete ${escapeHtml(employee.fullName)}">${icon('delete')}</button></div></td></tr>`).join('')}</tbody></table></div>`;
      content.querySelectorAll('[data-delete]').forEach(button => button.addEventListener('click', () => askDelete(page.content.find(employee => employee.id === Number(button.dataset.delete)))));
    }
    const start = page.totalElements ? page.page * page.size + 1 : 0;
    const end = Math.min((page.page + 1) * page.size, page.totalElements);
    document.querySelector('#pagination').innerHTML = `<div class="pagination"><span>Showing ${start}–${end} of ${page.totalElements} employees</span><div class="page-controls"><label for="page-size">Rows</label><select id="page-size">${[5, 10, 20, 50].map(size => `<option value="${size}" ${size === state.size ? 'selected' : ''}>${size}</option>`).join('')}</select><button class="button secondary small" id="previous-page" ${page.first ? 'disabled' : ''}>Previous</button><span class="page-label">${page.totalPages ? page.page + 1 : 0} / ${page.totalPages}</span><button class="button secondary small" id="next-page" ${page.last ? 'disabled' : ''}>Next</button></div></div>`;
    document.querySelector('#previous-page').addEventListener('click', () => { state.page--; loadList(); });
    document.querySelector('#next-page').addEventListener('click', () => { state.page++; loadList(); });
    document.querySelector('#page-size').addEventListener('change', event => { state.size = Number(event.target.value); state.page = 0; loadList(); });
  } catch (error) {
    if (version !== state.listVersion || routeVersion !== state.routeVersion) return;
    document.querySelector('#list-content').innerHTML = `<div class="empty-state"><h2>Could not load employees</h2><p>${escapeHtml(error.message)}</p><button class="button secondary" id="retry-list">Try again</button></div>`;
    document.querySelector('#list-content').removeAttribute('aria-busy');
    document.querySelector('#pagination').innerHTML = '';
    document.querySelector('#result-count').textContent = 'Unavailable';
    document.querySelector('#retry-list').addEventListener('click', loadList);
  }
}

const field = (name, label, type, value = '', extra = '', note = '') => `<div class="field"><label for="${name}">${label}<span>*</span></label><input id="${name}" name="${name}" type="${type}" value="${escapeHtml(value)}" required ${extra} aria-describedby="${name}-error">${note ? `<small>${note}</small>` : ''}<small class="field-error" id="${name}-error"></small></div>`;

async function formPage(id, routeVersion) {
  const editing = Boolean(id);
  const employee = editing ? await api(`/employees/${id}`) : {};
  if (routeVersion !== state.routeVersion) return;
  document.title = `${editing ? 'Edit' : 'Add'} employee | People`;
  main.innerHTML = `<div class="form-layout"><a class="back-link" href="${editing ? `#/employees/${id}` : '#/employees'}">${icon('arrow')}Back to ${editing ? 'employee details' : 'employees'}</a><div class="page-heading"><div><p class="eyebrow">EMPLOYEE RECORD</p><h1>${editing ? 'Edit employee' : 'Add employee'}</h1><p class="lead">${editing ? 'Make changes and save the record.' : 'Enter the employee details below.'}</p></div></div><form id="employee-form"><div id="form-error" class="error-banner" role="alert" hidden></div>
    <section class="panel form-panel"><div class="section-heading"><h2>Personal details</h2><p>Name, email and phone number.</p></div><div class="form-grid">
    ${field('fullName', 'Full name', 'text', employee.fullName, 'maxlength="100" autocomplete="name" placeholder="e.g. Ananya Rao"')}
    ${field('email', 'Email address', 'email', employee.email, 'maxlength="150" autocomplete="email" placeholder="e.g. ananya@example.com"')}
    ${field('phone', 'Phone number', 'tel', employee.phone, 'minlength="7" maxlength="20" autocomplete="tel" placeholder="e.g. +91 98765 43210"')}
    ${field('employeeId', 'Employee ID', 'text', employee.employeeId, 'maxlength="20" pattern="[A-Za-z0-9][A-Za-z0-9\\-]{0,19}" placeholder="e.g. EMP-101"', 'Use letters, numbers or hyphens. Each ID must be different.')}
    </div></section><section class="panel form-panel"><div class="section-heading"><h2>Work details</h2><p>Job, department and salary details.</p></div><div class="form-grid">
    ${field('department', 'Department', 'text', employee.department, 'maxlength="60" placeholder="e.g. Engineering"')}
    ${field('designation', 'Designation', 'text', employee.designation, 'maxlength="80" placeholder="e.g. Backend Developer"')}
    ${field('dateOfJoining', 'Date of joining', 'date', employee.dateOfJoining)}
    ${field('salary', 'Annual salary (INR)', 'number', employee.salary, 'min="0" max="9999999999.99" step="0.01" placeholder="e.g. 600000"')}
    <div class="field"><label for="status">Employment status<span>*</span></label><select id="status" name="status" required aria-describedby="status-error"><option value="ACTIVE" ${employee.status !== 'INACTIVE' ? 'selected' : ''}>Active</option><option value="INACTIVE" ${employee.status === 'INACTIVE' ? 'selected' : ''}>Inactive</option></select><small class="field-error" id="status-error"></small></div></div></section>
    <div class="form-actions"><p>All fields marked * are required.</p><div><a class="button secondary" href="${editing ? `#/employees/${id}` : '#/employees'}">Cancel</a> <button class="button" id="save-employee" type="submit">${editing ? 'Save changes' : 'Add employee'}</button></div></div></form></div>`;
  document.querySelector('#employee-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const submit = document.querySelector('#save-employee');
    const banner = document.querySelector('#form-error');
    banner.hidden = true;
    form.querySelectorAll('.field-error').forEach(element => { element.textContent = ''; });
    form.querySelectorAll('[aria-invalid]').forEach(element => element.removeAttribute('aria-invalid'));
    const body = Object.fromEntries(new FormData(form));
    submit.disabled = true;
    submit.textContent = 'Saving...';
    try {
      const saved = await api(editing ? `/employees/${id}` : '/employees', { method: editing ? 'PUT' : 'POST', body: JSON.stringify(body) });
      toast(editing ? 'Employee details saved.' : 'Employee added.');
      location.hash = `#/employees/${saved.id}`;
    } catch (error) {
      if (routeVersion !== state.routeVersion) return;
      banner.textContent = error.message;
      banner.hidden = false;
      for (const [name, message] of Object.entries(error.fields || {})) {
        const input = form.elements.namedItem(name);
        const target = document.getElementById(`${name}-error`);
        if (input && target) { input.setAttribute('aria-invalid', 'true'); target.textContent = message; }
      }
      const invalid = form.querySelector('[aria-invalid="true"]');
      if (invalid) invalid.focus(); else banner.scrollIntoView({ block: 'center' });
    } finally {
      submit.disabled = false;
      submit.textContent = editing ? 'Save changes' : 'Add employee';
    }
  });
}

async function detailsPage(id, routeVersion) {
  const employee = await api(`/employees/${id}`);
  if (routeVersion !== state.routeVersion) return;
  document.title = `${employee.fullName} | People`;
  const detail = (name, value) => `<div><dt>${name}</dt><dd>${escapeHtml(value)}</dd></div>`;
  main.innerHTML = `<a class="back-link" href="#/employees">${icon('arrow')}Back to employees</a><div class="page-heading"><div><p class="eyebrow">EMPLOYEE RECORD</p><h1>Employee details</h1><p class="lead">View this employee's contact and job details.</p></div><div class="heading-actions"><button class="button secondary" id="delete-employee">${icon('delete')}Delete</button><a class="button" href="#/employees/${id}/edit">${icon('edit')}Edit employee</a></div></div>
    <section class="panel profile-header"><span class="avatar large" aria-hidden="true">${escapeHtml(initials(employee.fullName))}</span><div><h2>${escapeHtml(employee.fullName)}</h2><p>${escapeHtml(employee.designation)} · ${escapeHtml(employee.department)}</p>${statusBadge(employee.status)}<span class="employee-id">${escapeHtml(employee.employeeId)}</span></div></section>
    <div class="detail-grid"><section class="panel"><div class="section-heading"><h2>Contact details</h2></div><dl class="detail-list">${detail('Full name', employee.fullName)}${detail('Email address', employee.email)}${detail('Phone number', employee.phone)}</dl></section><section class="panel"><div class="section-heading"><h2>Work details</h2></div><dl class="detail-list">${detail('Employee ID', employee.employeeId)}${detail('Department', employee.department)}${detail('Designation', employee.designation)}${detail('Date of joining', formatDate(employee.dateOfJoining))}${detail('Annual salary (INR)', formatSalary(employee.salary))}<div><dt>Employment status</dt><dd>${statusBadge(employee.status)}</dd></div></dl></section></div>`;
  document.querySelector('#delete-employee').addEventListener('click', () => askDelete(employee));
}

function askDelete(employee) {
  pendingDelete = employee;
  document.querySelector('#delete-message').textContent = `Delete ${employee.fullName} (${employee.employeeId})?`;
  document.querySelector('#delete-error').hidden = true;
  dialog.showModal();
}

document.querySelector('#confirm-delete').addEventListener('click', async event => {
  if (!pendingDelete) return;
  const button = event.currentTarget;
  button.disabled = true;
  try {
    await api(`/employees/${pendingDelete.id}`, { method: 'DELETE' });
    dialog.close();
    pendingDelete = null;
    toast('Employee deleted.');
    if (location.hash === '#/employees' || !location.hash) loadList();
    else location.hash = '#/employees';
  } catch (error) {
    const banner = document.querySelector('#delete-error');
    banner.textContent = error.message;
    banner.hidden = false;
  } finally { button.disabled = false; }
});
dialog.addEventListener('close', () => { pendingDelete = null; });

async function renderRoute() {
  clearTimeout(searchTimer);
  const routeVersion = ++state.routeVersion;
  const route = location.hash.replace(/^#/, '') || '/employees';
  main.innerHTML = '<div class="loading">Loading...</div>';
  try {
    if (route === '/employees') listPage();
    else if (route === '/employees/new') await formPage(null, routeVersion);
    else {
      const match = route.match(/^\/employees\/([1-9]\d*)(\/edit)?$/);
      if (!match) return pageError('This page was not found.');
      if (match[2]) await formPage(match[1], routeVersion);
      else await detailsPage(match[1], routeVersion);
    }
  } catch (error) { if (routeVersion === state.routeVersion) pageError(error.message); }
}
window.addEventListener('hashchange', () => { window.scrollTo(0, 0); renderRoute(); });
renderRoute();
