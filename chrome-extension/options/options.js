/**
 * Privacy Gateway — Options Page Logic
 */

'use strict';

const $ = (id) => document.getElementById(id);

function showStatus(msg, type) {
  const el = $('status-msg');
  el.textContent = msg;
  el.className = `status-msg ${type}`;
  el.style.display = 'block';
  setTimeout(() => { el.style.display = 'none'; }, 3500);
}

// Load saved settings
chrome.storage.sync.get(['apiUrl', 'apiKey', 'patientId', 'checkInterval'], (data) => {
  if (data.apiUrl)       $('opt-api-url').value      = data.apiUrl;
  if (data.apiKey)       $('opt-api-key').value      = data.apiKey;
  if (data.patientId)    $('opt-patient-id').value   = data.patientId;
  if (data.checkInterval) $('opt-check-interval').value = data.checkInterval;
});

// Save settings
$('settings-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const apiUrl       = $('opt-api-url').value.trim().replace(/\/$/, '');
  const apiKey       = $('opt-api-key').value.trim();
  const patientId    = parseInt($('opt-patient-id').value, 10) || null;
  const checkInterval = parseInt($('opt-check-interval').value, 10);

  chrome.storage.sync.set({ apiUrl, apiKey, patientId, checkInterval }, () => {
    // Reschedule alarm
    chrome.alarms.clear('auditCheck', () => {
      chrome.alarms.create('auditCheck', { periodInMinutes: checkInterval });
    });
    showStatus('✓ Settings saved successfully!', 'success');
  });
});

// Test connection
$('btn-test').addEventListener('click', async () => {
  const apiUrl = $('opt-api-url').value.trim().replace(/\/$/, '');
  const apiKey = $('opt-api-key').value.trim();

  if (!apiUrl || !apiKey) {
    showStatus('Please enter API URL and Key first.', 'error');
    return;
  }

  $('btn-test').textContent = 'Testing...';
  $('btn-test').disabled = true;

  try {
    const res = await fetch(`${apiUrl}/actuator/health`, {
      headers: { 'X-API-Key': apiKey },
    });
    const data = await res.json();
    const status = data?.status || (res.ok ? 'UP' : 'DOWN');
    if (res.ok) {
      showStatus(`✓ Connected! Server status: ${status}`, 'success');
    } else {
      showStatus(`Server responded but status is: ${status}`, 'error');
    }
  } catch (err) {
    showStatus(`✗ Connection failed: ${err.message}`, 'error');
  } finally {
    $('btn-test').textContent = 'Test Connection';
    $('btn-test').disabled = false;
  }
});
