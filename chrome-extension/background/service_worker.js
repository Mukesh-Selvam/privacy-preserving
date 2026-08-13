/**
 * Privacy Gateway — MV3 Service Worker
 * Handles badge updates and background notifications.
 */

'use strict';

const BADGE_COLORS = {
  safe:    '#22c55e',  // green — all consents OK
  warning: '#f59e0b',  // amber — some fields encrypted
  alert:   '#ef4444',  // red   — recent access detected
};

// ── INSTALL ────────────────────────────────────────────────────────
self.addEventListener('install', () => {
  self.skipWaiting();
});

self.addEventListener('activate', () => {
  clients.claim();
});

// ── ALARM: Periodic audit check ────────────────────────────────────
chrome.runtime.onInstalled.addListener(() => {
  chrome.alarms.create('auditCheck', { periodInMinutes: 5 });
  setBadge('safe');
});

chrome.alarms.onAlarm.addListener(async (alarm) => {
  if (alarm.name === 'auditCheck') {
    await checkRecentAudit();
  }
});

// ── BADGE HELPER ───────────────────────────────────────────────────
function setBadge(level, count = '') {
  const color = BADGE_COLORS[level] || BADGE_COLORS.safe;
  chrome.action.setBadgeBackgroundColor({ color });
  chrome.action.setBadgeText({ text: count ? String(count) : '' });
}

// ── CHECK RECENT AUDIT ─────────────────────────────────────────────
async function checkRecentAudit() {
  const stored = await getSettings();
  if (!stored.apiUrl || !stored.apiKey) return;

  try {
    const res = await fetch(`${stored.apiUrl}/api/audit-log?page=0&size=5`, {
      headers: { 'X-API-Key': stored.apiKey },
    });

    if (!res.ok) return;
    const data = await res.json();
    const logs = data?.data?.content || data?.content || [];

    if (logs.length > 0) {
      // Recent access found — show alert badge
      setBadge('alert', '!');
      notifyRecentAccess(logs[0], stored.apiUrl);
    } else {
      setBadge('safe');
    }
  } catch {
    // If API is down, remove badge
    setBadge('safe');
  }
}

// ── NOTIFICATION ───────────────────────────────────────────────────
function notifyRecentAccess(log, apiUrl) {
  const org   = log.orgId || log.organizationId || 'Unknown org';
  const field = log.fieldName || log.field || 'a field';
  const ts    = log.accessedAt ? new Date(log.accessedAt).toLocaleString() : 'recently';

  chrome.notifications.create({
    type: 'basic',
    iconUrl: '../icons/icon48.png',
    title: 'Privacy Gateway — Data Accessed',
    message: `${org} accessed your ${field} at ${ts}`,
    priority: 1,
  });
}

// ── STORAGE HELPER ─────────────────────────────────────────────────
function getSettings() {
  return new Promise((resolve) => {
    chrome.storage.sync.get(['apiUrl', 'apiKey', 'patientId'], resolve);
  });
}

// ── MESSAGE LISTENER ───────────────────────────────────────────────
chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg.type === 'CONSENT_UPDATED') {
    // Immediately check if we should update badge
    checkRecentAudit().then(() => sendResponse({ ok: true }));
    return true; // async
  }
  if (msg.type === 'CLEAR_BADGE') {
    setBadge('safe');
    sendResponse({ ok: true });
  }
});
