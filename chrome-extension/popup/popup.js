/**
 * Privacy Gateway — Chrome Extension Popup Logic
 * Connects to the Spring Boot Privacy Gateway API.
 */

'use strict';

// ── FIELD METADATA ─────────────────────────────────────────────────
const FIELDS = [
  { key: 'name',    label: 'Full Name',   icon: '👤' },
  { key: 'age',     label: 'Age',         icon: '🎂' },
  { key: 'disease', label: 'Diagnosis',   icon: '🩺' },
  { key: 'aadhaar', label: 'Aadhaar No.', icon: '🪪' },
  { key: 'phone',   label: 'Phone',       icon: '📱' },
  { key: 'address', label: 'Address',     icon: '🏠' },
];

// ── STATE ──────────────────────────────────────────────────────────
let state = {
  apiUrl:    '',
  apiKey:    '',
  patientId: null,
  consents:  {},   // { fieldKey: boolean }
  auditPage: 0,
};

// ── DOM SHORTCUTS ──────────────────────────────────────────────────
const $ = (id) => document.getElementById(id);
const show = (id) => $(id).classList.remove('hidden');
const hide = (id) => $(id).classList.add('hidden');
const showScreen = (name) => {
  document.querySelectorAll('.screen').forEach(s => {
    s.classList.toggle('active', s.id === `screen-${name}`);
    s.classList.toggle('hidden', s.id !== `screen-${name}`);
  });
};

// ── API HELPER ─────────────────────────────────────────────────────
async function api(method, path, body = null) {
  const opts = {
    method,
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': state.apiKey,
    },
  };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(`${state.apiUrl}${path}`, opts);
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`HTTP ${res.status}: ${text}`);
  }
  return res.json();
}

// ── STORAGE ────────────────────────────────────────────────────────
function loadSettings() {
  return new Promise((resolve) => {
    chrome.storage.sync.get(['apiUrl', 'apiKey', 'patientId'], (data) => {
      resolve(data);
    });
  });
}

function saveSettings(data) {
  return new Promise((resolve) => {
    chrome.storage.sync.set(data, resolve);
  });
}

// ── LOGIN SCREEN ───────────────────────────────────────────────────
async function initLogin() {
  const saved = await loadSettings();
  if (saved.apiUrl)    $('input-api-url').value    = saved.apiUrl;
  if (saved.apiKey)    $('input-api-key').value    = saved.apiKey;
  if (saved.patientId) $('input-patient-id').value = saved.patientId;

  $('login-form').addEventListener('submit', onLoginSubmit);
  $('link-options').addEventListener('click', (e) => {
    e.preventDefault();
    chrome.runtime.openOptionsPage();
  });
}

async function onLoginSubmit(e) {
  e.preventDefault();
  const apiUrl    = $('input-api-url').value.trim().replace(/\/$/, '');
  const apiKey    = $('input-api-key').value.trim();
  const patientId = parseInt($('input-patient-id').value, 10);

  if (!apiUrl || !apiKey || !patientId) {
    showLoginError('All fields are required.');
    return;
  }

  setLoginLoading(true);
  try {
    // Quick health check
    state = { ...state, apiUrl, apiKey, patientId };
    await api('GET', '/actuator/health');
    await saveSettings({ apiUrl, apiKey, patientId });
    await initDashboard();
    showScreen('main');
  } catch (err) {
    showLoginError(`Connection failed: ${err.message}`);
  } finally {
    setLoginLoading(false);
  }
}

function showLoginError(msg) {
  const el = $('login-error');
  el.textContent = msg;
  el.classList.remove('hidden');
}

function setLoginLoading(loading) {
  $('btn-login').disabled = loading;
  $('btn-login').querySelector('.btn-text').classList.toggle('hidden', loading);
  $('btn-login').querySelector('.btn-spinner').classList.toggle('hidden', !loading);
}

// ── DASHBOARD INIT ─────────────────────────────────────────────────
async function initDashboard() {
  // Patient badge
  $('patient-badge').textContent = `Patient #${state.patientId}`;

  // Tabs
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
  });

  // Logout
  $('btn-logout').addEventListener('click', onLogout);

  // Preview
  $('btn-refresh-preview').addEventListener('click', loadPreview);
  $('preview-org').addEventListener('change', loadPreview);

  // Audit
  $('btn-load-more').addEventListener('click', loadMoreAudit);

  // Load initial data
  await loadConsents();
  await loadPreview();
  await loadAudit(true);
}

function switchTab(name) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
  document.querySelectorAll('.tab-panel').forEach(p => {
    const isActive = p.id === `tab-${name}`;
    p.classList.toggle('active', isActive);
    p.classList.toggle('hidden', !isActive);
  });
}

async function onLogout() {
  await saveSettings({ apiUrl: '', apiKey: '', patientId: null });
  state = { apiUrl: '', apiKey: '', patientId: null, consents: {}, auditPage: 0 };
  showScreen('login');
  $('login-error').classList.add('hidden');
}

// ── CONSENT TAB ────────────────────────────────────────────────────
async function loadConsents() {
  const list = $('consent-list');
  list.innerHTML = loadingHtml();

  try {
    // Fetch current consents as a flat field→boolean map
    const data = await api('GET', `/api/consent/${state.patientId}/map`);
    const consents = data.data || data; // handle ApiResponse envelope
    state.consents = consents;
    renderConsentList(consents);
  } catch (err) {
    list.innerHTML = `<div class="loading-row" style="color:#ef4444">Could not load consents: ${err.message}</div>`;
  }
}

function renderConsentList(consents) {
  const list = $('consent-list');
  list.innerHTML = '';

  FIELDS.forEach((f, idx) => {
    const granted = consents[f.key] !== false; // default to true if not explicitly false
    const item = document.createElement('div');
    item.className = `consent-item ${granted ? 'granted' : 'revoked'}`;
    item.style.animationDelay = `${idx * 0.05}s`;
    item.id = `consent-item-${f.key}`;

    item.innerHTML = `
      <div class="consent-left">
        <div class="consent-field-name">${f.icon} ${f.label}</div>
        <div class="consent-status ${granted ? 'granted' : 'revoked'}" id="consent-status-${f.key}">
          ${granted ? '✓ Access granted' : '✗ Access revoked'}
        </div>
      </div>
      <label class="toggle-switch" title="${granted ? 'Revoke access' : 'Grant access'}">
        <input type="checkbox" id="toggle-${f.key}" ${granted ? 'checked' : ''} />
        <div class="toggle-track">
          <div class="toggle-thumb"></div>
        </div>
      </label>
    `;

    list.appendChild(item);

    // Bind toggle event
    item.querySelector(`#toggle-${f.key}`).addEventListener('change', (e) => {
      onConsentToggle(f.key, e.target.checked, item);
    });
  });
}

async function onConsentToggle(field, granted, itemEl) {
  const toggle = itemEl.querySelector(`#toggle-${field}`);
  toggle.disabled = true;

  try {
    await api('POST', '/api/consent', {
      patientId: state.patientId,
      field: field,
      consentGiven: granted,
    });

    // Update UI
    itemEl.className = `consent-item ${granted ? 'granted' : 'revoked'}`;
    itemEl.querySelector(`#consent-status-${field}`).className = `consent-status ${granted ? 'granted' : 'revoked'}`;
    itemEl.querySelector(`#consent-status-${field}`).textContent = granted ? '✓ Access granted' : '✗ Access revoked';
    state.consents[field] = granted;

    showToast(`${field} consent ${granted ? 'granted ✓' : 'revoked ✗'}`, granted ? 'success' : 'error');

    // Refresh preview if visible
    if (document.querySelector('[data-tab="preview"].active')) {
      await loadPreview();
    }
  } catch (err) {
    // Revert toggle on failure
    toggle.checked = !granted;
    showToast(`Failed: ${err.message}`, 'error');
  } finally {
    toggle.disabled = false;
  }
}

function showToast(msg, type = 'success') {
  const toast = $('consent-toast');
  toast.textContent = msg;
  toast.className = `toast ${type}`;
  toast.classList.remove('hidden');
  clearTimeout(toast._timer);
  toast._timer = setTimeout(() => toast.classList.add('hidden'), 2800);
}

// ── LIVE PREVIEW TAB ───────────────────────────────────────────────
async function loadPreview() {
  const orgId = $('preview-org').value;
  const result = $('preview-result');
  result.innerHTML = loadingHtml();

  try {
    const data = await api('GET', `/api/gateway/patient/${state.patientId}?orgId=${orgId}`);
    const fields = data.data || data;
    renderPreview(fields);
  } catch (err) {
    result.innerHTML = `<div class="loading-row" style="color:#ef4444">Error: ${err.message}</div>`;
  }
}

function renderPreview(fields) {
  const result = $('preview-result');
  result.innerHTML = '';

  FIELDS.forEach(f => {
    const raw = fields[f.key] ?? '—';
    let mode = 'plain';
    let displayVal = raw;

    if (typeof raw === 'string') {
      if (raw === '— removed —' || raw.toLowerCase().includes('removed')) {
        mode = 'hidden';
        displayVal = '— removed —';
      } else if (/^[0-9]{10,16}$/.test(raw.replace(/\s/g, ''))) {
        // numeric only → FPE-encrypted (passes format preservation)
        mode = 'encrypted';
      }
    }

    const row = document.createElement('div');
    row.className = 'preview-field-row';
    row.innerHTML = `
      <span class="preview-field-name">${f.icon} ${f.label}</span>
      <span class="preview-field-value mode-${mode}">${displayVal}</span>
      <span class="mode-badge ${mode}">${mode}</span>
    `;
    result.appendChild(row);
  });
}

// ── AUDIT LOG TAB ──────────────────────────────────────────────────
async function loadAudit(reset = false) {
  if (reset) {
    state.auditPage = 0;
    $('audit-list').innerHTML = loadingHtml();
  }

  try {
    const data = await api('GET', `/api/audit-log?page=${state.auditPage}&size=10`);
    const logs = (data.data?.content) || (data.content) || data.data || [];
    if (reset) $('audit-list').innerHTML = '';
    renderAuditRows(logs);
    state.auditPage++;
  } catch (err) {
    $('audit-list').innerHTML = `<div class="loading-row" style="color:#ef4444">Error: ${err.message}</div>`;
  }
}

async function loadMoreAudit() {
  await loadAudit(false);
}

function renderAuditRows(logs) {
  const list = $('audit-list');

  if (!logs.length && state.auditPage === 1) {
    list.innerHTML = `<div class="loading-row">No audit records found.</div>`;
    return;
  }

  logs.forEach((log, idx) => {
    const item = document.createElement('div');
    item.className = 'audit-item';
    item.style.animationDelay = `${idx * 0.04}s`;

    const ts = log.accessedAt || log.timestamp || '';
    const time = ts ? new Date(ts).toLocaleString() : '—';
    const orgId = log.orgId || log.organizationId || log.org || '—';
    const field = log.fieldName || log.field || '—';
    const mode  = log.mode || log.result || 'unknown';

    let tagClass = 'allowed';
    if (mode === 'hidden') tagClass = 'denied';
    else if (mode === 'encrypted') tagClass = 'encrypted';

    item.innerHTML = `
      <div class="audit-item-top">
        <span class="audit-org">${orgId}</span>
        <span class="audit-time">${time}</span>
      </div>
      <div class="audit-fields">
        <span class="audit-field-tag ${tagClass}">${field}: ${mode}</span>
      </div>
    `;
    list.appendChild(item);
  });
}

// ── HELPERS ────────────────────────────────────────────────────────
function loadingHtml() {
  return `<div class="loading-row">
    <div class="loading-dot"></div>
    <div class="loading-dot"></div>
    <div class="loading-dot"></div>
  </div>`;
}

// ── BOOT ───────────────────────────────────────────────────────────
async function boot() {
  const saved = await loadSettings();

  if (saved.apiUrl && saved.apiKey && saved.patientId) {
    // Try to auto-resume session
    state = { ...state, apiUrl: saved.apiUrl, apiKey: saved.apiKey, patientId: saved.patientId };
    try {
      await api('GET', '/actuator/health');
      await initDashboard();
      showScreen('main');
      return;
    } catch {
      // Fall through to login
    }
  }

  showScreen('login');
  await initLogin();
}

document.addEventListener('DOMContentLoaded', boot);
