/*
 * mm-contact.js – schlankes Kontaktformular mit Conversion Tracking.
 *
 * Events (dataLayer / gtag):
 *   form_start     – erste Interaktion mit dem Formular (einmal pro Seitenaufruf)
 *   generate_lead  – Anfrage wurde vom Server angenommen (NICHT schon beim Klick)
 *   form_error     – Senden fehlgeschlagen
 *
 * Erwartet ein <form data-mm-contact> (siehe beispiel.html).
 */
(function (w, d) {
  'use strict';

  var T = w.MMTrack;
  if (!T) { console.error('mm-contact.js: mm-tracking.js muss vorher geladen werden.'); return; }

  function init(form) {
    var c = w.MM_CONFIG || {};
    var formId = form.getAttribute('data-mm-contact') || 'kontakt';
    var status = form.querySelector('.mm-status');
    var button = form.querySelector('button[type="submit"]');
    var started = false;

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
          return T.track('generate_lead', {
            form_id: formId,
            lead_source: 'kontaktformular',
            transaction_id: j.lead_id,
            value: c.leadValue,
            currency: 'EUR'
          }, T.userData(data.email, data.phone));
        })
        .then(function () {
          if (c.thankYouUrl) { w.location.href = c.thankYouUrl; return; }
          form.innerHTML = '<p class="mm-success" role="status">Danke für deine Nachricht! ' +
            'Ich melde mich in der Regel innerhalb von 24 Stunden bei dir.</p>';
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
