// Fala Já – Browser-Teil: Spracherkennung, WebSocket, geordnete Wiedergabe, Unterbrechen.
(() => {
  "use strict";

  const $ = (s) => document.querySelector(s);
  const els = {
    chat: $("#chat"), empty: $("#empty"), interim: $("#interim"),
    mic: $("#mic"), stop: $("#stop"), form: $("#text-form"), text: $("#text"),
    status: $("#status"), statusText: $("#status-text"), latency: $("#latency"),
    mode: $("#mode"), level: $("#level"), accent: $("#accent"),
    slow: $("#slow"), subtitles: $("#subtitles"), bargein: $("#bargein"),
  };

  const state = {
    ws: null,
    ttsMode: "browser",   // "elevenlabs" | "browser" (vom Server)
    turn: 0,              // zählt gesendete Runden (gleich wie der Server)
    activeTurn: 0,        // Runde, deren Nachrichten wir annehmen (0 = keine)
    queue: new Map(),     // idx -> {url} | {text}
    sentences: new Map(), // idx -> {text, span}
    nextIdx: 0,
    playing: false,
    botBubble: null,
    spokenText: "",       // was die Tutorin in dieser Runde sagt (Echo-Filter)
    tSent: 0,
    latencyPending: false,
    listening: false,
    started: false,
    lastAudioEnd: 0,
  };

  const audio = new Audio();
  audio.preservesPitch = true;

  // ---------- Einstellungen (pro Browser gemerkt) ----------
  const SETTINGS_KEY = "fala-ja-settings";
  function loadSettings() {
    try {
      const s = JSON.parse(localStorage.getItem(SETTINGS_KEY) || "{}");
      for (const k of ["mode", "level", "accent"]) if (s[k]) els[k].value = s[k];
      for (const k of ["slow", "subtitles", "bargein"]) if (k in s) els[k].checked = !!s[k];
    } catch { /* ohne Speicher geht es auch */ }
  }
  function saveSettings() {
    try {
      localStorage.setItem(SETTINGS_KEY, JSON.stringify({
        mode: els.mode.value, level: els.level.value, accent: els.accent.value,
        slow: els.slow.checked, subtitles: els.subtitles.checked, bargein: els.bargein.checked,
      }));
    } catch { /* ignorieren */ }
  }
  const settings = () => ({ mode: els.mode.value, level: els.level.value, accent: els.accent.value });

  // ---------- Verbindung ----------
  function setStatus(text, kind) {
    els.statusText.textContent = text;
    els.status.className = "status " + (kind || "");
  }

  function connect() {
    const proto = location.protocol === "https:" ? "wss" : "ws";
    const ws = new WebSocket(`${proto}://${location.host}/ws`);
    state.ws = ws;
    ws.onopen = () => {
      state.turn = 0; state.activeTurn = 0;
      setStatus(state.ttsMode === "elevenlabs" ? "Bereit · Stimme: ElevenLabs" : "Bereit · Stimme: Browser", "ok");
    };
    ws.onclose = () => { setStatus("Getrennt – verbinde neu…", "err"); setTimeout(connect, 1000); };
    ws.onmessage = (ev) => handle(JSON.parse(ev.data));
  }

  function send(obj) {
    if (state.ws && state.ws.readyState === WebSocket.OPEN) {
      state.ws.send(JSON.stringify(obj));
      return true;
    }
    return false;
  }

  // ---------- Chat-Darstellung ----------
  function scrollDown() { els.chat.scrollTop = els.chat.scrollHeight; }

  function addMsg(cls, text) {
    els.empty?.remove();
    const div = document.createElement("div");
    div.className = "msg " + cls;
    if (text) div.textContent = text;
    els.chat.appendChild(div);
    scrollDown();
    return div;
  }

  function newBotBubble() {
    const div = addMsg("bot thinking");
    const t = document.createElement("span");
    t.className = "text";
    div.appendChild(t);
    div.addEventListener("click", () => div.classList.toggle("reveal"));
    state.botBubble = div;
    return div;
  }

  function addNote(text) {
    const note = document.createElement("div");
    note.className = "note";
    note.textContent = text;
    if (state.botBubble) els.chat.insertBefore(note, state.botBubble);
    else els.chat.appendChild(note);
    scrollDown();
  }

  // ---------- Wiedergabe (in Reihenfolge der Sätze) ----------
  function stopPlayback() {
    audio.onended = null;
    audio.pause();
    audio.removeAttribute("src");
    try { speechSynthesis.cancel(); } catch { /* nicht vorhanden */ }
    for (const item of state.queue.values()) if (item.url) URL.revokeObjectURL(item.url);
    state.queue.clear();
    state.sentences.forEach((s) => s.span?.classList.remove("speaking"));
    state.playing = false;
  }

  function botActive() {
    return state.activeTurn !== 0 && (state.playing || state.queue.size > 0 ||
      state.botBubble?.classList.contains("thinking"));
  }

  function pump() {
    if (state.playing) return;
    const idx = state.nextIdx;
    const item = state.queue.get(idx);
    if (!item) return;
    state.queue.delete(idx);
    state.nextIdx++;
    state.playing = true;

    if (state.latencyPending) {
      state.latencyPending = false;
      const ms = Math.round(performance.now() - state.tSent);
      els.latency.textContent = `⚡ ${ms} ms`;
    }
    const span = state.sentences.get(idx)?.span;
    span?.classList.add("speaking");

    const finish = () => {
      if (!state.playing) return;
      span?.classList.remove("speaking");
      if (item.url) URL.revokeObjectURL(item.url);
      state.playing = false;
      state.lastAudioEnd = performance.now();
      pump();
    };

    if (item.url) {
      audio.src = item.url;
      audio.playbackRate = els.slow.checked ? 0.85 : 1;
      audio.onended = finish;
      audio.play().catch(finish);
    } else {
      speakBrowser(item.text, finish);
    }
  }

  let ptVoice = null;
  function pickVoice() {
    try {
      const voices = speechSynthesis.getVoices();
      ptVoice = voices.find((v) => v.lang.replace("_", "-") === "pt-BR") ||
                voices.find((v) => v.lang.startsWith("pt")) || null;
    } catch { ptVoice = null; }
  }
  if ("speechSynthesis" in window) {
    pickVoice();
    speechSynthesis.onvoiceschanged = pickVoice;
  }

  function speakBrowser(text, done) {
    if (!("speechSynthesis" in window)) { done(); return; }
    const u = new SpeechSynthesisUtterance(text);
    u.lang = "pt-BR";
    if (ptVoice) u.voice = ptVoice;
    u.rate = els.slow.checked ? 0.8 : 1;
    // Sicherheitsnetz, falls der Browser onend nie meldet
    const guard = setTimeout(done, 2500 + text.length * 90);
    u.onend = u.onerror = () => { clearTimeout(guard); done(); };
    speechSynthesis.speak(u);
  }

  function b64ToUrl(b64) {
    const bin = atob(b64);
    const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    return URL.createObjectURL(new Blob([bytes], { type: "audio/mpeg" }));
  }

  // ---------- Nachrichten vom Server ----------
  function handle(msg) {
    if (msg.turn !== undefined && msg.turn !== state.activeTurn) return; // veraltete Runde
    const bubble = state.botBubble;
    switch (msg.type) {
      case "sentence": {
        bubble?.classList.remove("thinking");
        const span = document.createElement("span");
        span.className = "s";
        span.textContent = (bubble.querySelector(".text").childElementCount ? " " : "") + msg.text;
        bubble.querySelector(".text").appendChild(span);
        state.sentences.set(msg.idx, { text: msg.text, span });
        state.spokenText += " " + msg.text;
        if (state.ttsMode === "browser") { state.queue.set(msg.idx, { text: msg.text }); pump(); }
        scrollDown();
        break;
      }
      case "audio":
        state.queue.set(msg.idx, { url: b64ToUrl(msg.data) });
        pump();
        break;
      case "tts_error":
        state.queue.set(msg.idx, { text: state.sentences.get(msg.idx)?.text || "" });
        pump();
        break;
      case "correction":
        addNote(msg.text);
        break;
      case "done":
        bubble?.classList.remove("thinking");
        break;
      case "error":
        bubble?.classList.remove("thinking");
        if (bubble && !bubble.querySelector(".s")) bubble.remove();
        addMsg("sys", msg.message);
        state.activeTurn = 0;
        break;
    }
  }

  // ---------- Senden / Unterbrechen ----------
  function beginTurn() {
    stopPlayback();
    state.sentences.clear();
    state.nextIdx = 0;
    state.spokenText = "";
    state.activeTurn = ++state.turn;
    state.tSent = performance.now();
    state.latencyPending = true;
    newBotBubble();
  }

  function sendUser(text) {
    text = text.trim();
    if (!text) return;
    if (!state.ws || state.ws.readyState !== WebSocket.OPEN) { addMsg("sys", "Keine Verbindung zum Server."); return; }
    addMsg("user", text);
    beginTurn();
    send({ type: "user_text", text, ...settings() });
  }

  function startConversation() {
    if (state.started) return;
    if (!state.ws || state.ws.readyState !== WebSocket.OPEN) return;
    state.started = true;
    beginTurn();
    send({ type: "start", ...settings() });
  }

  function interrupt() {
    if (!botActive()) return;
    stopPlayback();
    state.botBubble?.classList.remove("thinking");
    state.botBubble?.classList.add("cut");
    state.activeTurn = 0;
    send({ type: "interrupt" });
  }

  // Ist das Erkannte nur das Echo der Tutorin aus dem Lautsprecher?
  function norm(s) {
    return s.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "")
      .replace(/[^a-z0-9\s]/g, " ").split(/\s+/).filter(Boolean);
  }
  // Echo nur prüfen, solange die Tutorin spricht (plus kurze Nachlaufzeit)
  function echoWindow() { return botActive() || performance.now() - state.lastAudioEnd < 1500; }
  function looksLikeEcho(heard) {
    if (!echoWindow()) return false;
    const words = norm(heard);
    if (!words.length) return true;
    const bot = new Set(norm(state.spokenText));
    const hits = words.filter((w) => bot.has(w)).length;
    return hits / words.length >= 0.6;
  }

  // ---------- Spracherkennung (Web Speech API) ----------
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
  let rec = null;

  function recLang() { return els.mode.value === "pergunta" ? "de-DE" : "pt-BR"; }

  function makeRecognizer() {
    const r = new SR();
    r.lang = recLang();
    r.continuous = true;
    r.interimResults = true;
    r.maxAlternatives = 1;

    r.onresult = (e) => {
      let interim = "", final = "";
      for (let i = e.resultIndex; i < e.results.length; i++) {
        const res = e.results[i];
        if (res.isFinal) final += res[0].transcript; else interim += res[0].transcript;
      }
      const active = botActive();
      if (interim) {
        if (active && (!els.bargein.checked || looksLikeEcho(interim))) return;
        els.interim.textContent = interim;
        // Dazwischensprechen: ab zwei eigenen Wörtern sofort stoppen
        if (active && norm(interim).length >= 2) interrupt();
      }
      if (final.trim()) {
        els.interim.textContent = "";
        if (active && (!els.bargein.checked || looksLikeEcho(final))) return;
        if (looksLikeEcho(final)) return; // spätes Echo kurz nach dem Ende
        sendUser(final);
      }
    };
    r.onerror = (e) => {
      if (e.error === "not-allowed" || e.error === "service-not-allowed") {
        addMsg("sys", "Mikrofon nicht erlaubt. Bitte im Browser freigeben.");
        setListening(false);
      }
    };
    // Chrome beendet die Erkennung regelmäßig – dann neu starten
    r.onend = () => { if (state.listening) { try { r.start(); } catch { /* läuft schon */ } } };
    return r;
  }

  function setListening(on) {
    state.listening = on;
    els.mic.setAttribute("aria-pressed", String(on));
    els.mic.setAttribute("aria-label", on ? "Mikrofon stoppen" : "Mikrofon starten");
    if (on) {
      if (!rec) rec = makeRecognizer();
      try { rec.start(); } catch { /* läuft schon */ }
    } else if (rec) {
      try { rec.stop(); } catch { /* egal */ }
      els.interim.textContent = "";
    }
  }

  // ---------- Bedienelemente ----------
  els.mic.addEventListener("click", () => {
    if (!SR) {
      addMsg("sys", "Dein Browser hat keine Spracherkennung. Nutze Chrome oder Edge – oder tippe unten.");
      startConversation();
      els.text.focus();
      return;
    }
    const on = !state.listening;
    setListening(on);
    if (on) startConversation();
  });

  els.stop.addEventListener("click", interrupt);
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") interrupt();
  });

  els.form.addEventListener("submit", (e) => {
    e.preventDefault();
    const t = els.text.value;
    els.text.value = "";
    if (botActive()) interrupt();
    sendUser(t);
  });

  for (const k of ["mode", "level", "accent", "slow", "subtitles", "bargein"]) {
    els[k].addEventListener("change", () => {
      saveSettings();
      document.body.classList.toggle("no-subs", !els.subtitles.checked);
      if (k === "mode" && rec) {
        rec.lang = recLang();
        if (state.listening) { try { rec.stop(); } catch { /* startet in onend neu */ } }
      }
    });
  }

  // ---------- Start ----------
  loadSettings();
  document.body.classList.toggle("no-subs", !els.subtitles.checked);
  fetch("/api/config").then((r) => r.json()).then((cfg) => {
    state.ttsMode = cfg.tts;
  }).catch(() => {}).finally(connect);
})();
