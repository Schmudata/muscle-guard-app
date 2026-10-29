/*
 * mm-tracking.js – Tracking-Basis für das Kontaktformular. Auf ALLEN Seiten einbinden.
 *
 * - Speichert UTM-Parameter und Google-Ads-Klick-IDs (gclid/gbraid/wbraid) 90 Tage,
 *   damit eine Anfrage auch Tage nach dem Anzeigenklick noch zugeordnet werden kann.
 * - Auf der Danke-Seite (<body data-mm-danke>) wird "generate_lead" genau einmal gefeuert –
 *   nur nach einer echten Anfrage, nicht bei direktem Aufruf oder Neuladen.
 * - Events gehen in den dataLayer (GTM) und optional direkt an gtag (GA4 + Google Ads).
 */
(function (w, d) {
  'use strict';

  var KEY = 'mm_attribution';
  var LEAD_KEY = 'mm_pending_lead';
  var TTL = 90 * 24 * 3600 * 1000;
  var PARAMS = ['utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content',
    'gclid', 'gbraid', 'wbraid'];

  function cfg() { return w.MM_CONFIG || {}; }

  function load() {
    try {
      var v = JSON.parse(w.localStorage.getItem(KEY));
      if (v && Date.now() - v.ts < TTL) return v;
    } catch (e) { /* localStorage gesperrt oder kaputt */ }
    return null;
  }

  // Letzter Klick mit Kampagnen-Parametern gewinnt; ohne Parameter bleibt die alte Quelle.
  function capture() {
    var q = new URLSearchParams(w.location.search);
    var found = {};
    var any = false;
    PARAMS.forEach(function (p) {
      var v = q.get(p);
      if (v) { found[p] = v.slice(0, 200); any = true; }
    });
    var current = load();
    if (any || !current) {
      found.landing_page = w.location.href.split('#')[0].slice(0, 500);
      found.referrer = (d.referrer || '').slice(0, 500);
      found.ts = Date.now();
      try { w.localStorage.setItem(KEY, JSON.stringify(found)); } catch (e) { /* ignorieren */ }
      current = found;
    }
    return current;
  }

  // event: z. B. "generate_lead"; userData: optional { email, phone_number } (Enhanced Conversions)
  function track(event, params, userData) {
    params = params || {};
    var c = cfg();
    var entry = { event: event };
    Object.keys(params).forEach(function (k) {
      if (params[k] !== undefined && params[k] !== null) entry[k] = params[k];
    });
    if (userData) entry.user_data = userData;
    w.dataLayer = w.dataLayer || [];
    w.dataLayer.push(entry);

    if (c.useGtag && typeof w.gtag === 'function') {
      if (userData) w.gtag('set', 'user_data', userData);
      w.gtag('event', event, params);
      var sendTo = c.adsConversions && c.adsConversions[event];
      if (sendTo) {
        var conv = { send_to: sendTo };
        if (params.value != null) { conv.value = params.value; conv.currency = params.currency || 'EUR'; }
        if (params.transaction_id) conv.transaction_id = params.transaction_id;
        w.gtag('event', 'conversion', conv);
      }
    }
  }

  // Für Enhanced Conversions: Telefonnummer ins E.164-Format bringen (Standard DE).
  function toE164(phone) {
    var p = String(phone || '').replace(/[^\d+]/g, '');
    if (!p) return '';
    if (p.indexOf('00') === 0) return '+' + p.slice(2);
    if (p.charAt(0) === '0') return (cfg().phoneCountryCode || '+49') + p.slice(1);
    return p.charAt(0) === '+' ? p : '+' + p;
  }

  function userData(email, phone) {
    var u = {};
    if (email) u.email = String(email).trim().toLowerCase();
    var e164 = toE164(phone);
    if (e164) u.phone_number = e164;
    return Object.keys(u).length ? u : undefined;
  }

  // Vom Formular aufgerufen: Lead merken, auf der Danke-Seite feuern.
  // Gibt false zurück, wenn sessionStorage nicht verfügbar ist.
  function queueLead(params, user) {
    try {
      w.sessionStorage.setItem(LEAD_KEY, JSON.stringify({ params: params, userData: user }));
      return true;
    } catch (e) {
      return false;
    }
  }

  function firePendingLead() {
    var lead = null;
    try {
      lead = JSON.parse(w.sessionStorage.getItem(LEAD_KEY));
      w.sessionStorage.removeItem(LEAD_KEY);
    } catch (e) { /* ignorieren */ }
    if (lead && lead.params) track('generate_lead', lead.params, lead.userData);
  }

  w.MMTrack = {
    attribution: capture(),
    track: track,
    userData: userData,
    queueLead: queueLead
  };

  function onReady() {
    if (d.body && d.body.hasAttribute('data-mm-danke')) firePendingLead();
  }
  if (d.readyState === 'loading') d.addEventListener('DOMContentLoaded', onReady); else onReady();
})(window, document);
