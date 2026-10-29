/*
 * mm-contact.js – Terminanfrage-Formular (ersetzt Calendly).
 *
 * Ablauf: Formular absenden → Server bestätigt → Weiterleitung auf die Danke-Seite,
 * dort feuert mm-tracking.js "generate_lead" (genau einmal).
 *
 * Events: form_start (erste Eingabe), form_error (Senden fehlgeschlagen).
 * Erwartet ein <form data-mm-contact> (siehe kontakt.html).
 */
(function (w, d) {
  'use strict';

  var T = w.MMTrack;
  if (!T) { console.error('mm-contact.js: mm-tracking.js muss vorher geladen werden.'); return; }

  function isoDate(dt) {
    return dt.getFullYear() + '-' + String(dt.getMonth() + 1).padStart(2, '0') + '-' +
      String(dt.getDate()).padStart(2, '0');
  }

  // Wunschtermin: frühestens morgen, höchstens maxDays im Voraus, nur an erlaubten Wochentagen.
  function setupDate(input, c) {
    if (!input) return;
    var days = c.bookingDays || [1, 2, 3, 4, 5]; // 0 = Sonntag … 6 = Samstag
    var min = new Date();
    min.setDate(min.getDate() + 1);
    var max = new Date();
    max.setDate(max.getDate() + (c.maxDays || 60));
    input.min = isoDate(min);
    input.max = isoDate(max);
    input.addEventListener('input', function () {
      var v = input.value ? new Date(input.value + 'T12:00:00') : null;
      input.setCustomValidity(v && days.indexOf(v.getDay()) === -1
        ? 'An diesem Wochentag sind leider keine Termine möglich.' : '');
    });
  }

  function init(form) {
    var c = w.MM_CONFIG || {};
    var formId = form.getAttribute('data-mm-contact') || 'kontakt';
    var status = form.querySelector('.mm-status');
    var button = form.querySelector('button[type="submit"]');
    var started = false;

    setupDate(form.querySelector('input[name="date"]'), c);

    form.addEventListener('focusin', function () {
      if (started) return;
      started = true;
      T.track('form_start', { form_id: formId });
    });

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      if (!form.checkValidity()) { form.reportValidity(); return; }

      var fd = new FormData(form);
      var data = {
        name: (fd.get('name') || '').trim(),
        email: (fd.get('email') || '').trim(),
        phone: (fd.get('phone') || '').trim(),
        date: fd.get('date') || '',
        slot: fd.get('slot') || '',
        message: (fd.get('message') || '').trim(),
        consent: !!fd.get('consent'),
        website: fd.get('website') || '', // Honeypot
        page: w.location.href.split('#')[0],
        attribution: T.attribution
      };

      button.disabled = true;
      setStatus('Wird gesendet …', '');

      fetch((c.apiBase || '') + '/api/contact', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      })
        .then(function (r) {
          return r.json().catch(function () { return {}; }).then(function (j) {
            if (!r.ok) throw new Error(typeof j.detail === 'string' ? j.detail : 'Bitte prüfe deine Eingaben.');
            return j;
          });
        })
        .then(function (j) {
          var params = {
            form_id: formId,
            lead_type: 'erstgespraech',
            transaction_id: j.lead_id,
            value: c.leadValue,
            currency: 'EUR'
          };
          var user = T.userData(data.email, data.phone);
          var next = c.thankYouUrl || '/danke.html';
          if (T.queueLead(params, user)) {
            w.location.href = next;
          } else {
            // Ohne sessionStorage direkt hier tracken und den Tags kurz Zeit geben.
            T.track('generate_lead', params, user);
            setTimeout(function () { w.location.href = next; }, 800);
          }
        })
        .catch(function (err) {
          button.disabled = false;
          setStatus(err.message && err.message !== 'Failed to fetch'
            ? err.message
            : 'Senden hat nicht geklappt. Bitte versuche es erneut oder schreib mir direkt per E-Mail.', 'mm-error');
          T.track('form_error', { form_id: formId });
        });
    });

    function setStatus(text, cls) {
      if (!status) return;
      status.textContent = text;
      status.className = 'mm-status ' + cls;
    }
  }

  function boot() {
    Array.prototype.forEach.call(d.querySelectorAll('form[data-mm-contact]'), init);
  }
  if (d.readyState === 'loading') d.addEventListener('DOMContentLoaded', boot); else boot();
})(window, document);
