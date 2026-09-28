// ---------- session / api ----------
const AUTH = sessionStorage.getItem('auth');
const ME = JSON.parse(sessionStorage.getItem('user') || 'null');
if (!AUTH || !ME) location.href = 'login.html';
const isAdmin = ME && ME.role === 'ADMIN';
function logout() { sessionStorage.clear(); location.href = 'login.html'; }

async function api(path, opts = {}) {
  const res = await fetch('/api' + path, { ...opts, headers: { 'Content-Type': 'application/json', 'Authorization': 'Basic ' + AUTH } });
  if (res.status === 401) { logout(); return; }
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(data && data.errors ? Object.values(data.errors).join(', ') : (data && data.message) || res.statusText);
  return data;
}
const post = (p, b) => api(p, { method: 'POST', body: JSON.stringify(b) });
const put = (p, b) => api(p, { method: 'PUT', body: JSON.stringify(b) });

// ---------- helpers ----------
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => '₹' + Number(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
const dt = s => s ? new Date(s).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '-';
const badge = s => `<span class="badge bg-${{ PAID: 'success', SUCCESS: 'success', UNPAID: 'danger', FAILED: 'danger', PENDING: 'warning', HABITUAL: 'danger', REPEAT: 'warning', HIGH: 'danger', MEDIUM: 'warning', LOW: 'secondary' }[s] || 'secondary'}">${esc(s)}</span>`;
const $ = id => document.getElementById(id);
const val = id => { const v = $(id).value.trim(); return v === '' ? null : v; };
function notify(msg, type = 'success') {
  $('alert').innerHTML = `<div class="alert alert-${type} alert-dismissible">${esc(msg)}<button class="btn-close" data-bs-dismiss="alert"></button></div>`;
  window.scrollTo(0, 0);
}
const guard = fn => async (...a) => { try { await fn(...a); } catch (e) { notify(e.message, 'danger'); } };
function modal(title, html) { $('mTitle').textContent = title; $('mBody').innerHTML = html; bootstrap.Modal.getOrCreateInstance($('modal')).show(); }
function closeModal() { bootstrap.Modal.getOrCreateInstance($('modal')).hide(); }
function table(heads, rows) {
  return `<div class="table-responsive"><table class="table table-striped table-hover bg-white align-middle"><thead class="table-dark"><tr>${heads.map(h => `<th>${h}</th>`).join('')}</tr></thead><tbody>${rows.join('') || `<tr><td colspan="${heads.length}" class="text-center text-muted">No records</td></tr>`}</tbody></table></div>`;
}
function pager(p, fn) {
  return `<div class="d-flex justify-content-between align-items-center"><span class="small text-muted">Page ${p.page + 1} of ${Math.max(p.totalPages, 1)} · ${p.totalElements} records</span>
  <div><button class="btn btn-sm btn-outline-primary me-1" ${p.page === 0 ? 'disabled' : ''} onclick="${fn}(${p.page - 1})">Prev</button>
  <button class="btn btn-sm btn-outline-primary" ${p.page + 1 >= p.totalPages ? 'disabled' : ''} onclick="${fn}(${p.page + 1})">Next</button></div></div>`;
}
const setView = h => { $('view').innerHTML = h; };
const title = t => `<h3 class="mb-3">${t}</h3>`;

// ---------- navigation ----------
const VIEWS = [
  ['dashboard', 'Dashboard'], ['vehicles', 'Vehicles'], ['issue', 'Issue E-Challan'], ['challans', 'All Challans'],
  ['history', 'Vehicle History'], ['offenders', 'Repeat Offenders'], ['pay', 'Pay Fines'], ['payments', 'Payment History'],
  ['reports', 'Reports'], ['notifications', 'Notifications'], ['admin', 'Admin Panel', true]];
$('who').textContent = ME.fullName + ' (' + ME.role + ')';
$('menu').innerHTML = VIEWS.filter(v => !v[2] || isAdmin).map(v => `<li><a class="nav-link" data-v="${v[0]}" onclick="show('${v[0]}')">${v[1]}</a></li>`).join('');
function show(v) {
  document.querySelectorAll('#menu .nav-link').forEach(a => a.classList.toggle('active', a.dataset.v === v));
  $('alert').innerHTML = '';
  guard(() => window['view_' + v]())();
}

// ---------- dashboard / reports ----------
function statCards(r) {
  const c = (t, v, cls = '') => `<div class="col-6 col-lg-3"><div class="card stat shadow-sm"><div class="card-body"><div class="text-muted small">${t}</div><div class="fs-4 fw-bold ${cls}">${v}</div></div></div></div>`;
  return `<div class="row g-3 mb-4">${c('Total challans', r.totalChallans)}${c('Paid fines', money(r.paidAmount), 'text-success')}${c('Pending fines', money(r.pendingAmount), 'text-danger')}${c('Repeat-offender vehicles', r.repeatOffenderVehicles, 'text-warning')}</div>`;
}
function monthly(r) {
  const max = Math.max(1, ...r.monthly.map(m => m.challans));
  return table(['Month', 'Challans', '', 'Fines issued'], r.monthly.map(m => `<tr><td>${m.month}</td><td>${m.challans}</td><td style="width:40%"><div class="progress"><div class="progress-bar" style="width:${m.challans / max * 100}%"></div></div></td><td>${money(m.fines)}</td></tr>`));
}
async function view_dashboard() {
  const [r, recent] = await Promise.all([api('/challans/reports'), api('/challans?size=5')]);
  setView(title(isAdmin ? 'Admin Dashboard' : 'Traffic Officer Dashboard') + statCards(r) + '<h5>Latest challans</h5>' + challanTable(recent.content) + '<h5 class="mt-4">Monthly statistics</h5>' + monthly(r));
}
async function view_reports() {
  const r = await api('/challans/reports');
  setView(title('Reports & Statistics') + statCards(r) + `<div class="row g-3 mb-3"><div class="col-md-4"><div class="card"><div class="card-body">Paid challans: <b>${r.paidChallans}</b><br>Unpaid challans: <b>${r.unpaidChallans}</b></div></div></div></div>` + monthly(r));
}

// ---------- challan table + details ----------
function challanTable(list, withPay = false) {
  return table(['Challan No', 'Vehicle', 'Violation', 'Date', 'Occurrence', 'Fine', 'Status', ''], list.map(c => `<tr>
    <td>${c.challanNumber}</td><td>${c.registrationNumber}</td><td>${esc(c.violationDescription)}</td><td>${dt(c.violationDateTime)}</td>
    <td>${c.occurrenceNumber}${c.repeatOffender ? ' <span class="badge bg-warning text-dark">repeat</span>' : ''}</td>
    <td>${money(c.finalFine)}</td><td>${badge(c.status)}</td>
    <td class="text-nowrap"><button class="btn btn-sm btn-outline-primary" onclick="showChallan(${c.id})">Details</button>
    ${c.status === 'UNPAID' ? `<button class="btn btn-sm btn-success ms-1" onclick="payModal(${c.id})">Pay</button>` : ''}</td></tr>`));
}
function breakdown(c) {
  return `<table class="table table-bordered w-auto"><tr><th>Base fine</th><td>${money(c.baseFine)}</td></tr>
   <tr><th>Occurrence (same vehicle + violation, last 12 months)</th><td>${c.occurrenceNumber} (${c.previousViolationCount} previous)</td></tr>
   <tr><th>Escalation multiplier</th><td>× ${c.multiplier}</td></tr>
   <tr class="table-primary"><th>Final fine</th><td><b>${money(c.finalFine)}</b></td></tr></table>`;
}
const showChallan = guard(async id => {
  const c = await api('/challans/' + id);
  const prev = c.previousViolations.length ? table(['Challan', 'Date', 'Fine'], c.previousViolations.map(p => `<tr><td>${p.challanNumber}</td><td>${dt(p.violationDateTime)}</td><td>${money(p.finalFine)}</td></tr>`)) : '<p class="text-muted">No previous violations of this type in the rolling 12-month window.</p>';
  modal('Challan ' + c.challanNumber, `<p><b>${c.registrationNumber}</b> · ${esc(c.ownerName)} · ${esc(c.violationDescription)}<br>${dt(c.violationDateTime)} at ${esc(c.location)}<br>Officer: ${esc(c.officerName)} · Due: ${c.dueDate} · ${badge(c.status)} ${c.repeatOffender ? '<span class="badge bg-warning text-dark">REPEAT OFFENDER</span>' : ''}</p>${breakdown(c)}<h6>Previous violations in window</h6>${prev}`);
});

// ---------- vehicles ----------
async function view_vehicles() {
  setView(title('Vehicles') + `<div class="card mb-4"><div class="card-body"><h5>Register vehicle</h5><div class="row g-2">
   <div class="col-md-3"><input id="v_reg" class="form-control" placeholder="TN34AB1234"></div>
   <div class="col-md-3"><select id="v_type" class="form-select">${['TWO_WHEELER', 'THREE_WHEELER', 'CAR', 'BUS', 'TRUCK', 'OTHER'].map(t => `<option>${t}</option>`).join('')}</select></div>
   <div class="col-md-3"><input id="v_owner" class="form-control" placeholder="Owner name"></div>
   <div class="col-md-3"><input id="v_phone" class="form-control" placeholder="Owner mobile (10 digits)"></div>
   <div class="col-md-3"><input id="v_email" class="form-control" placeholder="Email (optional)"></div>
   <div class="col-md-3"><input id="v_make" class="form-control" placeholder="Make"></div>
   <div class="col-md-3"><input id="v_model" class="form-control" placeholder="Model"></div>
   <div class="col-md-3"><button class="btn btn-primary w-100" onclick="saveVehicle()">Register</button></div></div></div></div>
   <div class="input-group mb-3"><input id="v_q" class="form-control" placeholder="Search registration or owner"><button class="btn btn-outline-primary" onclick="loadVehicles(0)">Search</button></div><div id="v_list"></div>`);
  await loadVehicles(0);
}
const saveVehicle = guard(async () => {
  const v = await post('/vehicles', { registrationNumber: val('v_reg'), vehicleType: $('v_type').value, ownerName: val('v_owner'), ownerContact: val('v_phone'), ownerEmail: val('v_email'), make: val('v_make'), model: val('v_model') });
  notify('Vehicle ' + v.registrationNumber + ' registered'); view_vehicles();
});
const loadVehicles = guard(async page => {
  const p = await api(`/vehicles?page=${page}&size=8&q=${encodeURIComponent($('v_q').value)}`);
  $('v_list').innerHTML = table(['ID', 'Registration', 'Type', 'Owner', 'Contact', 'Make/Model', ''], p.content.map(v => `<tr><td>${v.id}</td><td>${v.registrationNumber}</td><td>${v.vehicleType}</td><td>${esc(v.ownerName)}</td><td>${v.ownerContact}</td><td>${esc(v.make || '')} ${esc(v.model || '')}</td>
   <td><button class="btn btn-sm btn-outline-primary" onclick="openHistory('${v.registrationNumber}')">History</button></td></tr>`)) + pager(p, 'loadVehicles');
});
function openHistory(reg) { show('history'); setTimeout(() => { $('h_reg').value = reg; loadHistory(); }, 200); }

// ---------- issue challan ----------
async function view_issue() {
  const types = (await api('/violations')).filter(t => t.active);
  const now = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  setView(title('Issue E-Challan') + `<div class="card"><div class="card-body"><div class="row g-3">
   <div class="col-md-4"><label class="form-label">Vehicle registration</label><input id="i_reg" class="form-control" placeholder="TN34AB1234"></div>
   <div class="col-md-4"><label class="form-label">Violation</label><select id="i_type" class="form-select">${types.map(t => `<option value="${t.id}">${t.code} – ${esc(t.description)} (${money(t.baseFine)})</option>`).join('')}</select></div>
   <div class="col-md-4"><label class="form-label">Date &amp; time</label><input id="i_dt" type="datetime-local" class="form-control" value="${now}" max="${now}"></div>
   <div class="col-md-6"><label class="form-label">Location</label><input id="i_loc" class="form-control"></div>
   <div class="col-md-6"><label class="form-label">Remarks</label><input id="i_rem" class="form-control"></div>
   <div class="col-12"><button class="btn btn-danger" onclick="issueChallan()">Issue challan</button></div></div></div></div><div id="i_res" class="mt-4"></div>`);
}
const issueChallan = guard(async () => {
  const v = await api('/vehicles/registration/' + encodeURIComponent(val('i_reg') || ''));
  const c = await post('/challans', { vehicleId: v.id, violationTypeId: Number($('i_type').value), violationDateTime: $('i_dt').value, location: val('i_loc'), remarks: val('i_rem') });
  notify('Challan ' + c.challanNumber + ' issued'); 
  $('i_res').innerHTML = `<div class="card border-danger"><div class="card-body"><h5>${c.challanNumber} ${c.repeatOffender ? '<span class="badge bg-warning text-dark">REPEAT OFFENDER – occurrence ' + c.occurrenceNumber + '</span>' : ''}</h5>${breakdown(c)}
   <h6>Previous violations in window</h6>${c.previousViolations.map(p => `<div>${p.challanNumber} · ${dt(p.violationDateTime)} · ${money(p.finalFine)}</div>`).join('') || '<span class="text-muted">None</span>'}</div></div>`;
});

// ---------- challans list ----------
async function view_challans() {
  const types = await api('/violations');
  setView(title('All Challans') + `<div class="row g-2 mb-3">
   <div class="col-md-2"><input id="f_reg" class="form-control" placeholder="Registration"></div>
   <div class="col-md-2"><input id="f_no" class="form-control" placeholder="Challan no"></div>
   <div class="col-md-2"><select id="f_type" class="form-select"><option value="">All violations</option>${types.map(t => `<option value="${t.id}">${t.code}</option>`).join('')}</select></div>
   <div class="col-md-2"><select id="f_status" class="form-select"><option value="">Any status</option><option>UNPAID</option><option>PAID</option></select></div>
   <div class="col-md-2"><input id="f_from" type="date" class="form-control"></div><div class="col-md-2"><input id="f_to" type="date" class="form-control"></div>
   <div class="col-md-3"><select id="f_sort" class="form-select"><option value="issuedAt,desc">Newest issued</option><option value="violationDateTime,desc">Violation date ↓</option><option value="finalFine,desc">Fine ↓</option><option value="finalFine,asc">Fine ↑</option></select></div>
   <div class="col-md-2"><button class="btn btn-primary w-100" onclick="loadChallans(0)">Search</button></div></div><div id="c_list"></div>`);
  await loadChallans(0);
}
const loadChallans = guard(async page => {
  const q = new URLSearchParams({ page, size: 10, sort: $('f_sort').value });
  [['registration', 'f_reg'], ['number', 'f_no'], ['violationTypeId', 'f_type'], ['status', 'f_status'], ['from', 'f_from'], ['to', 'f_to']].forEach(([k, id]) => { if ($(id).value) q.set(k, $(id).value); });
  const p = await api('/challans?' + q);
  $('c_list').innerHTML = challanTable(p.content) + pager(p, 'loadChallans');
});

// ---------- vehicle history ----------
async function view_history() {
  setView(title('Vehicle Violation History') + `<div class="input-group mb-3" style="max-width:500px"><input id="h_reg" class="form-control" placeholder="Registration number"><button class="btn btn-primary" onclick="loadHistory()">Show history</button></div><div id="h_res"></div>`);
}
const loadHistory = guard(async () => {
  const v = await api('/vehicles/registration/' + encodeURIComponent(val('h_reg') || ''));
  const list = await api('/challans/vehicle/' + v.id);
  const now = new Date(); const from = new Date(); from.setFullYear(now.getFullYear() - 1);
  const inWin = list.filter(c => new Date(c.violationDateTime) >= from).length;
  $('h_res').innerHTML = `<div class="card mb-3"><div class="card-body"><b>${v.registrationNumber}</b> · ${esc(v.ownerName)} · ${v.vehicleType}<br>Total violations: <b>${list.length}</b> · In last 12 months: <b>${inWin}</b> ${inWin >= 3 ? badge('HABITUAL') : inWin >= 2 ? badge('REPEAT') : ''}</div></div>` + challanTable(list);
});

// ---------- repeat offenders ----------
async function view_offenders() {
  const list = await api('/challans/repeat-offenders');
  setView(title('Repeat Offenders (last 12 months)') + table(['Registration', 'Owner', 'Contact', 'Violations', 'Total fines', 'Last violation', 'Status', ''], list.map(r => `<tr><td>${r.registrationNumber}</td><td>${esc(r.ownerName)}</td><td>${r.ownerContact}</td><td>${r.violationsInWindow}</td><td>${money(r.totalFines)}</td><td>${dt(r.lastViolation)}</td><td>${badge(r.status)}</td><td><button class="btn btn-sm btn-outline-primary" onclick="openHistory('${r.registrationNumber}')">History</button></td></tr>`)));
}

// ---------- payments ----------
async function view_pay() {
  const p = await api('/challans/pending?size=50');
  setView(title('Unpaid Challans') + challanTable(p.content));
}
const payModal = guard(async id => {
  const c = await api('/challans/' + id);
  modal('Pay challan ' + c.challanNumber, `<p>Amount payable: <b>${money(c.finalFine)}</b> (${c.registrationNumber})</p>
   <div class="mb-3"><label class="form-label">Payment method</label><select id="pm" class="form-select"><option>UPI</option><option>CARD</option><option>NET_BANKING</option><option>CASH</option></select></div>
   <div class="mb-3"><label class="form-label">Simulated outcome (demo)</label><select id="po" class="form-select"><option value="SUCCESS">Success</option><option value="FAILED">Failed</option><option value="PENDING">Pending</option></select></div>
   <button class="btn btn-success" onclick="doPay(${c.id})">Pay now</button>`);
});
const doPay = guard(async id => {
  const r = await post('/payments', { challanId: id, method: $('pm').value, simulateOutcome: $('po').value });
  closeModal();
  notify(`Payment ${r.status} · ref ${r.transactionReference}`, r.status === 'SUCCESS' ? 'success' : r.status === 'FAILED' ? 'danger' : 'warning');
  const v = document.querySelector('#menu .active'); if (v) setTimeout(() => window['view_' + v.dataset.v](), 50);
});
async function view_payments() {
  setView(title('Payment History') + `<div id="p_list"></div>`); await loadPayments(0);
}
const loadPayments = guard(async page => {
  const p = await api(`/payments/history?page=${page}&size=10`);
  $('p_list').innerHTML = table(['Ref', 'Challan', 'Vehicle', 'Amount', 'Method', 'Status', 'Date', ''], p.content.map(x => `<tr><td>${x.transactionReference}</td><td>${x.challanNumber}</td><td>${x.registrationNumber}</td><td>${money(x.amount)}</td><td>${x.method}</td><td>${badge(x.status)}</td><td>${dt(x.paymentDate)}</td>
   <td>${x.status === 'PENDING' ? `<button class="btn btn-sm btn-success" onclick="resolvePay(${x.id},'SUCCESS')">Confirm</button> <button class="btn btn-sm btn-danger" onclick="resolvePay(${x.id},'FAILED')">Fail</button>` : ''}</td></tr>`)) + pager(p, 'loadPayments');
});
const resolvePay = guard(async (id, o) => { await post(`/payments/${id}/resolve?outcome=${o}`); notify('Payment marked ' + o); loadPayments(0); });

async function view_notifications() {
  const p = await api('/notifications?size=30');
  setView(title('Notifications (simulated SMS)') + table(['Time', 'Challan', 'To', 'Message', 'Fine', 'Status', 'Due'], p.content.map(n => `<tr><td>${dt(n.createdAt)}</td><td>${n.challanNumber}</td><td>${n.recipient}</td><td>${esc(n.message)}</td><td>${money(n.fineAmount)}</td><td>${badge(n.paymentStatus)}</td><td>${n.dueDate || ''}</td></tr>`)));
}

// ---------- admin ----------
async function view_admin() {
  const [types, rules, users] = await Promise.all([api('/violations'), api('/penalty-rules'), api('/users')]);
  setView(title('Admin Panel') + `<h5>Violation types</h5>` + table(['Code', 'Description', 'Base fine', 'Severity', 'Active'], types.map(t => `<tr><td>${t.code}</td><td>${esc(t.description)}</td><td>${money(t.baseFine)}</td><td>${badge(t.severity)}</td><td>${t.active}</td></tr>`)) +
   `<div class="row g-2 mb-4"><div class="col-md-2"><input id="a_code" class="form-control" placeholder="Code"></div><div class="col-md-4"><input id="a_desc" class="form-control" placeholder="Description"></div><div class="col-md-2"><input id="a_fine" type="number" class="form-control" placeholder="Base fine"></div><div class="col-md-2"><select id="a_sev" class="form-select"><option>LOW</option><option>MEDIUM</option><option>HIGH</option></select></div><div class="col-md-2"><button class="btn btn-primary w-100" onclick="addType()">Add type</button></div></div>
   <h5>Penalty rules (multiplier applies from that occurrence onward)</h5>` + table(['Violation', 'From occurrence', 'Multiplier', 'Active', 'Change multiplier'], rules.map(r => `<tr><td>${r.violationTypeCode}</td><td>${r.fromOccurrence}</td><td>× ${r.multiplier}</td><td>${r.active}</td><td><div class="input-group input-group-sm" style="max-width:200px"><input id="m_${r.id}" type="number" step="0.25" min="1" class="form-control" value="${r.multiplier}"><button class="btn btn-outline-primary" onclick="editRule(${r.id},${r.violationTypeId},${r.fromOccurrence})">Save</button></div></td></tr>`)) +
   `<div class="row g-2 mb-4"><div class="col-md-3"><select id="r_type" class="form-select"><option value="">ALL violations</option>${types.map(t => `<option value="${t.id}">${t.code}</option>`).join('')}</select></div><div class="col-md-3"><input id="r_from" type="number" min="1" class="form-control" placeholder="From occurrence"></div><div class="col-md-3"><input id="r_mult" type="number" step="0.25" min="1" class="form-control" placeholder="Multiplier"></div><div class="col-md-3"><button class="btn btn-primary w-100" onclick="addRule()">Add rule</button></div></div>
   <h5>Users</h5>` + table(['Username', 'Name', 'Badge', 'Role', 'Active', ''], users.map(u => `<tr><td>${u.username}</td><td>${esc(u.fullName)}</td><td>${u.badgeNumber || ''}</td><td>${u.role}</td><td>${u.active}</td><td><button class="btn btn-sm btn-outline-secondary" onclick="toggleUser(${u.id},${!u.active})">${u.active ? 'Deactivate' : 'Activate'}</button></td></tr>`)) +
   `<div class="row g-2"><div class="col-md-2"><input id="u_name" class="form-control" placeholder="Username"></div><div class="col-md-3"><input id="u_pw" type="password" class="form-control" placeholder="Password (8+)"></div><div class="col-md-3"><input id="u_full" class="form-control" placeholder="Full name"></div><div class="col-md-2"><input id="u_badge" class="form-control" placeholder="Badge"></div><div class="col-md-1"><select id="u_role" class="form-select"><option>OFFICER</option><option>ADMIN</option></select></div><div class="col-md-1"><button class="btn btn-primary w-100" onclick="addUser()">Add</button></div></div>`);
}
const addType = guard(async () => { await post('/violations', { code: val('a_code'), description: val('a_desc'), baseFine: val('a_fine'), severity: $('a_sev').value }); notify('Violation type added'); view_admin(); });
const addRule = guard(async () => { await post('/penalty-rules', { violationTypeId: val('r_type') ? Number(val('r_type')) : null, fromOccurrence: Number(val('r_from')), multiplier: val('r_mult') }); notify('Rule added'); view_admin(); });
const editRule = guard(async (id, typeId, from) => { await put('/penalty-rules/' + id, { violationTypeId: typeId, fromOccurrence: from, multiplier: $('m_' + id).value, active: true }); notify('Rule updated. Existing challans keep their original fines.'); view_admin(); });
const addUser = guard(async () => { await post('/users', { username: val('u_name'), password: val('u_pw'), fullName: val('u_full'), badgeNumber: val('u_badge'), role: $('u_role').value }); notify('User created'); view_admin(); });
const toggleUser = guard(async (id, v) => { await api(`/users/${id}/active?value=${v}`, { method: 'PATCH' }); view_admin(); });

show('dashboard');
