/*
 * mm-tracking.js – gemeinsame Tracking-Basis für Kontaktformular & Chatbot.
 *
 * - Speichert UTM-Parameter und Google-Ads-Klick-IDs (gclid/gbraid/wbraid) 90 Tage,
 *   damit sie auch bei einer Anfrage Tage nach dem Klick mitgeschickt werden.
 * - track() pusht jedes Event in den dataLayer (für GTM) und optional direkt an gtag
 *   (GA4-Event + Google-Ads-Conversion inkl. Enhanced Conversions).
 * - Erkennt erfolgreiche Calendly-Buchungen auf jeder Seite (event "book_appointment").
 *
 * Muss VOR mm-contact.js und mm-chat.js geladen werden.
 */
(function (w, d) {
  'use strict';

  var KEY = 'mm_attribution';
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

  function hasGtm() { return !!w.google_tag_manager; }

  /*
   * event:    Event-Name, z. B. "generate_lead"
   * params:   Event-Parameter (form_id, value, currency, transaction_id, …)
   * userData: optional { email, phone_number } für Enhanced Conversions
   * Gibt ein Promise zurück, das auflöst, sobald die Tags gefeuert haben (max. 1,5 s) –
   * wichtig, wenn danach auf eine Danke-Seite weitergeleitet wird.
   */
  function track(event, params, userData) {
    params = params || {};
    var c = cfg();
    return new Promise(function (resolve) {
      var done = false;
      function finish() { if (!done) { done = true; resolve(); } }

      w.dataLayer = w.dataLayer || [];
      var entry = { event: event };
      Object.keys(params).forEach(function (k) {
        if (params[k] !== undefined && params[k] !== null) entry[k] = params[k];
      });
      if (userData) entry.user_data = userData;
      if (hasGtm()) {
        entry.eventCallback = finish;
        entry.eventTimeout = 1500;
      }
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

      setTimeout(finish, hasGtm() ? 1500 : 300);
    });
  }

  // Für gtag/Enhanced Conversions: Telefonnummer ins E.164-Format bringen (Standard DE).
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

  // Calendly meldet Buchungen per postMessage – egal ob Popup aus dem Chat oder Inline-Embed.
  w.addEventListener('message', function (e) {
    if (e.origin !== 'https://calendly.com' || !e.data || e.data.event !== 'calendly.event_scheduled') return;
    var uri = (e.data.payload && e.data.payload.event && e.data.payload.event.uri) || '';
    track('book_appointment', {
      booking_tool: 'calendly',
      transaction_id: uri.split('/').pop() || undefined,
      value: cfg().bookingValue,
      currency: 'EUR'
    });
  });

  w.MMTrack = {
    attribution: capture(),
    track: track,
    userData: userData
  };
})(window, document);
