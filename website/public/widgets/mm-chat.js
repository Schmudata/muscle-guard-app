/*
 * mm-chat.js – Chat-Widget (unten rechts) mit KI-Antworten und Calendly-Terminbuchung.
 *
 * Events (dataLayer / gtag):
 *   chat_open         – Chat wurde geöffnet (einmal pro Sitzung)
 *   chat_engaged      – erste eigene Nachricht des Besuchers (einmal pro Sitzung)
 *   booking_click     – Terminbuchung geöffnet
 *   book_appointment  – Termin tatsächlich gebucht (kommt aus mm-tracking.js)
 */
(function (w, d) {
  'use strict';

  var T = w.MMTrack;
  if (!T) { console.error('mm-chat.js: mm-tracking.js muss vorher geladen werden.'); return; }

  var c = w.MM_CONFIG || {};
  var chat = c.chat || {};
  var STORE = 'mm_chat';
  var MAX_HISTORY = 12;

  var state = loadState();
  var el = {};
  var busy = false;

  function loadState() {
    try {
      var s = JSON.parse(w.sessionStorage.getItem(STORE));
      if (s && Array.isArray(s.messages)) return s;
    } catch (e) { /* ignorieren */ }
    return { messages: [], opened: false, engaged: false };
  }

  function saveState() {
    try { w.sessionStorage.setItem(STORE, JSON.stringify(state)); } catch (e) { /* ignorieren */ }
  }

  function h(tag, cls, text) {
    var n = d.createElement(tag);
    if (cls) n.className = cls;
    if (text) n.textContent = text;
    return n;
  }

  function build() {
    el.launcher = h('button', 'mm-chat-launcher');
    el.launcher.type = 'button';
    el.launcher.setAttribute('aria-label', 'Chat öffnen');
    el.launcher.innerHTML = '<svg viewBox="0 0 24 24" width="28" height="28" aria-hidden="true">' +
      '<path fill="currentColor" d="M4 4h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H8l-4 4V6a2 2 0 0 1 2-2z"/></svg>';

    el.panel = h('section', 'mm-chat-panel');
    el.panel.setAttribute('role', 'dialog');
    el.panel.setAttribute('aria-label', chat.title || 'Chat');
    el.panel.hidden = true;

    var head = h('header', 'mm-chat-head');
    var titles = h('div');
    titles.appendChild(h('strong', '', chat.title || 'Fragen? Schreib mir!'));
    titles.appendChild(h('span', '', chat.subtitle || 'Antwortet sofort – rund um die Uhr'));
    var close = h('button', 'mm-chat-close', '×');
    close.type = 'button';
    close.setAttribute('aria-label', 'Chat schließen');
    close.addEventListener('click', toggle);
    head.appendChild(titles);
    head.appendChild(close);

    el.log = h('div', 'mm-chat-log');
    el.log.setAttribute('aria-live', 'polite');

    el.quick = h('div', 'mm-chat-quick');
    (chat.quickReplies || ['Was bietest du an?', 'Was kostet das?']).forEach(function (q) {
      var b = h('button', '', q);
      b.type = 'button';
      b.addEventListener('click', function () { send(q); });
      el.quick.appendChild(b);
    });
    if (c.calendlyUrl) {
      var book = h('button', 'mm-chat-book-quick', '📅 Termin buchen');
      book.type = 'button';
      book.addEventListener('click', function () { openBooking('quick_reply'); });
      el.quick.appendChild(book);
    }

    el.form = h('form', 'mm-chat-form');
    el.input = h('input');
    el.input.type = 'text';
    el.input.maxLength = 1000;
    el.input.placeholder = 'Nachricht schreiben …';
    el.input.setAttribute('aria-label', 'Nachricht');
    var sendBtn = h('button', '', 'Senden');
    sendBtn.type = 'submit';
    el.form.appendChild(el.input);
    el.form.appendChild(sendBtn);
    el.form.addEventListener('submit', function (e) {
      e.preventDefault();
      var text = el.input.value.trim();
      if (text) send(text);
    });

    el.panel.appendChild(head);
    el.panel.appendChild(el.log);
    el.panel.appendChild(el.quick);
    el.panel.appendChild(el.form);
    if (c.privacyUrl) {
      var note = h('p', 'mm-chat-note');
      note.innerHTML = 'KI-Assistent. Keine sensiblen Daten eingeben. <a href="' +
        encodeURI(c.privacyUrl) + '">Datenschutz</a>';
      el.panel.appendChild(note);
    }

    el.launcher.addEventListener('click', toggle);
    d.body.appendChild(el.panel);
    d.body.appendChild(el.launcher);

    if (state.messages.length) {
      state.messages.forEach(function (m) { render(m.role, m.text, m.booking); });
    } else {
      render('model', chat.greeting || 'Hi! 👋 Ich beantworte gern deine Fragen – oder buch dir direkt einen Termin.');
    }
  }

  function toggle() {
    var open = el.panel.hidden;
    el.panel.hidden = !open;
    el.launcher.classList.toggle('mm-open', open);
    el.launcher.setAttribute('aria-label', open ? 'Chat schließen' : 'Chat öffnen');
    if (open) {
      el.input.focus();
      if (!state.opened) {
        state.opened = true;
        saveState();
        T.track('chat_open', { chat_location: w.location.pathname });
      }
    }
  }

  function render(role, text, booking) {
    var bubble = h('div', 'mm-msg mm-msg-' + (role === 'user' ? 'user' : 'bot'), text);
    if (booking && c.calendlyUrl) {
      var b = h('button', 'mm-chat-book', '📅 Jetzt Termin auswählen');
      b.type = 'button';
      b.addEventListener('click', function () { openBooking('bot_suggestion'); });
      bubble.appendChild(b);
    }
    el.log.appendChild(bubble);
    el.log.scrollTop = el.log.scrollHeight;
    return bubble;
  }

  function send(text) {
    if (busy) return;
    busy = true;
    el.input.value = '';
    el.quick.hidden = true;

    state.messages.push({ role: 'user', text: text });
    render('user', text);
    if (!state.engaged) {
      state.engaged = true;
      T.track('chat_engaged', { chat_location: w.location.pathname });
    }
    saveState();

    var typing = render('model', '…');
    typing.classList.add('mm-typing');

    var history = state.messages.slice(-MAX_HISTORY).map(function (m) {
      return { role: m.role, text: m.text };
    });

    fetch((c.apiBase || '') + '/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ messages: history, page: w.location.pathname })
    })
      .then(function (r) {
        if (!r.ok) throw new Error(r.status === 429 ? 'rate' : 'http');
        return r.json();
      })
      .then(function (j) {
        typing.remove();
        var msg = { role: 'model', text: j.reply, booking: !!j.show_booking };
        state.messages.push(msg);
        saveState();
        render(msg.role, msg.text, msg.booking);
      })
      .catch(function (err) {
        typing.remove();
        render('model', err.message === 'rate'
          ? 'Puh, das waren viele Nachrichten. Bitte warte kurz einen Moment.'
          : 'Da ist gerade etwas schiefgelaufen. Nutze gern das Kontaktformular oder buch direkt einen Termin.',
        true);
        // Fehlgeschlagene Frage nicht im Verlauf lassen, sonst stehen zwei User-Nachrichten hintereinander.
        state.messages.pop();
        saveState();
      })
      .then(function () {
        busy = false;
        el.input.focus();
      });
  }

  // Calendly-Popup; Script wird erst beim ersten Klick nachgeladen.
  function openBooking(source) {
    T.track('booking_click', { booking_source: 'chat_' + source });
    var a = T.attribution || {};
    var opts = {
      url: c.calendlyUrl,
      utm: {
        utmSource: a.utm_source || (a.gclid ? 'google' : 'website'),
        utmMedium: a.utm_medium || (a.gclid ? 'cpc' : 'chatbot'),
        utmCampaign: a.utm_campaign,
        utmContent: a.utm_content,
        utmTerm: a.utm_term
      }
    };
    loadCalendly(function (ok) {
      if (ok) w.Calendly.initPopupWidget(opts);
      else w.open(c.calendlyUrl, '_blank', 'noopener');
    });
  }

  function loadCalendly(cb) {
    if (w.Calendly) { cb(true); return; }
    var css = d.createElement('link');
    css.rel = 'stylesheet';
    css.href = 'https://assets.calendly.com/assets/external/widget.css';
    d.head.appendChild(css);
    var s = d.createElement('script');
    s.src = 'https://assets.calendly.com/assets/external/widget.js';
    s.async = true;
    s.onload = function () { cb(!!w.Calendly); };
    s.onerror = function () { cb(false); };
    d.head.appendChild(s);
  }

  if (d.readyState === 'loading') d.addEventListener('DOMContentLoaded', build); else build();
})(window, document);
