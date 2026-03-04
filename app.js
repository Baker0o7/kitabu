const STORAGE_KEY = "kitabu.notes.v1";
const THEME_KEY = "kitabu.theme.v1";
const VIEW_KEY = "kitabu.view.v1";
const MAX_IMPORT_SIZE_BYTES = 5 * 1024 * 1024;

const elements = {
  newNoteBtn: document.getElementById("new-note-btn"),
  deleteNoteBtn: document.getElementById("delete-note-btn"),
  themeBtn: document.getElementById("theme-btn"),
  importBtn: document.getElementById("import-btn"),
  exportBtn: document.getElementById("export-btn"),
  importFileInput: document.getElementById("import-file-input"),
  noteCount: document.getElementById("note-count"),
  searchInput: document.getElementById("search-input"),
  viewActiveBtn: document.getElementById("view-active-btn"),
  viewArchivedBtn: document.getElementById("view-archived-btn"),
  noteList: document.getElementById("note-list"),
  emptyState: document.getElementById("empty-state"),
  editor: document.getElementById("editor"),
  titleInput: document.getElementById("title-input"),
  tagsInput: document.getElementById("tags-input"),
  bodyInput: document.getElementById("body-input"),
  previewPane: document.getElementById("preview-pane"),
  previewToggle: document.getElementById("preview-toggle"),
  archiveBtn: document.getElementById("archive-btn"),
  pinBtn: document.getElementById("pin-btn"),
  metaText: document.getElementById("meta-text"),
  statsText: document.getElementById("stats-text"),
  toast: document.getElementById("toast"),
};

const state = {
  notes: [],
  activeId: null,
  query: "",
  previewMode: false,
  view: "active",
  theme: "sunrise",
};

let toastTimer = null;
let storageWarningShown = false;

function nowIso() {
  return new Date().toISOString();
}

function safeParse(value) {
  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}

function storageRead(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function storageWrite(key, value) {
  try {
    localStorage.setItem(key, value);
    return true;
  } catch {
    return false;
  }
}

function showStorageWarning() {
  if (storageWarningShown) {
    return;
  }
  storageWarningShown = true;
  showToast("Storage access failed. Changes may not persist.");
}

function generateId() {
  if (globalThis.crypto && typeof globalThis.crypto.randomUUID === "function") {
    return globalThis.crypto.randomUUID();
  }
  return `note_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`;
}

function normalizeIso(value, fallback) {
  if (typeof value !== "string") {
    return fallback;
  }
  return Number.isNaN(Date.parse(value)) ? fallback : value;
}

function normalizeTags(tags) {
  if (!Array.isArray(tags)) {
    return [];
  }
  return [...new Set(tags.filter((tag) => typeof tag === "string").map((tag) => tag.trim().toLowerCase()).filter(Boolean))];
}

function parseTagsInput(raw) {
  return [...new Set(raw.split(",").map((tag) => tag.trim().toLowerCase()).filter(Boolean).slice(0, 12))];
}

function parseDateMs(value) {
  const ms = Date.parse(value);
  return Number.isNaN(ms) ? 0 : ms;
}

function createBlankNote() {
  const timestamp = nowIso();
  return {
    id: generateId(),
    title: "",
    body: "",
    tags: [],
    pinned: false,
    archived: false,
    createdAt: timestamp,
    updatedAt: timestamp,
  };
}

function normalizeNote(note) {
  if (!note || typeof note !== "object") {
    return null;
  }

  const timestamp = nowIso();
  const createdAt = normalizeIso(note.createdAt, timestamp);
  const updatedAt = normalizeIso(note.updatedAt, createdAt);

  return {
    id: typeof note.id === "string" && note.id.trim() ? note.id : generateId(),
    title: typeof note.title === "string" ? note.title : "",
    body: typeof note.body === "string" ? note.body : "",
    tags: normalizeTags(note.tags),
    pinned: Boolean(note.pinned),
    archived: Boolean(note.archived),
    createdAt,
    updatedAt,
  };
}

function defaultNotes() {
  const note = createBlankNote();
  note.title = "Welcome to Kitabu";
  note.body = [
    "# Start here",
    "",
    "Kitabu keeps your notes saved on this device.",
    "",
    "- Create, pin, archive, and search notes",
    "- Use Preview for lightweight markdown",
    "- Export JSON backups any time",
  ].join("\n");
  note.tags = ["welcome", "guide"];
  note.updatedAt = nowIso();
  return [note];
}

function sortNotes(notes) {
  return [...notes].sort((a, b) => {
    if (a.pinned !== b.pinned) {
      return a.pinned ? -1 : 1;
    }
    return parseDateMs(b.updatedAt) - parseDateMs(a.updatedAt);
  });
}

function getActiveNote() {
  return state.notes.find((note) => note.id === state.activeId) || null;
}

function saveNotes() {
  const ok = storageWrite(STORAGE_KEY, JSON.stringify(state.notes));
  if (!ok) {
    showStorageWarning();
  }
}

function loadNotes() {
  const raw = storageRead(STORAGE_KEY);
  const parsed = raw ? safeParse(raw) : null;

  const incoming = Array.isArray(parsed)
    ? parsed
    : parsed && Array.isArray(parsed.notes)
      ? parsed.notes
      : [];

  const normalized = incoming.map(normalizeNote).filter(Boolean);
  state.notes = normalized.length ? normalized : defaultNotes();

  const firstActive = sortNotes(state.notes).find((note) => !note.archived);
  const firstAny = sortNotes(state.notes)[0];
  state.activeId = firstActive ? firstActive.id : firstAny ? firstAny.id : null;

  if (!normalized.length) {
    saveNotes();
  }
}

function loadTheme() {
  const stored = storageRead(THEME_KEY);
  state.theme = stored === "midnight" ? "midnight" : "sunrise";
  applyTheme();
}

function applyTheme() {
  document.documentElement.setAttribute("data-theme", state.theme);
  elements.themeBtn.textContent = state.theme === "midnight" ? "Sun" : "Night";
}

function toggleTheme() {
  state.theme = state.theme === "midnight" ? "sunrise" : "midnight";
  const ok = storageWrite(THEME_KEY, state.theme);
  if (!ok) {
    showStorageWarning();
  }
  applyTheme();
}

function loadView() {
  const stored = storageRead(VIEW_KEY);
  state.view = stored === "archived" ? "archived" : "active";
}

function saveView() {
  const ok = storageWrite(VIEW_KEY, state.view);
  if (!ok) {
    showStorageWarning();
  }
}

function formatRelativeDate(iso) {
  const value = Date.parse(iso);
  if (Number.isNaN(value)) {
    return "Unknown time";
  }

  const deltaMs = Date.now() - value;
  const minute = 60_000;
  const hour = 60 * minute;
  const day = 24 * hour;

  if (deltaMs < minute) return "just now";
  if (deltaMs < hour) return `${Math.floor(deltaMs / minute)}m ago`;
  if (deltaMs < day) return `${Math.floor(deltaMs / hour)}h ago`;

  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(value);
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function inlineMarkdown(value) {
  return value
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/\*([^*]+)\*/g, "<em>$1</em>");
}

function markdownToHtml(markdown) {
  const escaped = escapeHtml(markdown);
  const lines = escaped.split("\n");
  const html = [];

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i].trimEnd();

    if (!line.trim()) {
      continue;
    }

    if (line.startsWith("### ")) {
      html.push(`<h3>${inlineMarkdown(line.slice(4))}</h3>`);
      continue;
    }
    if (line.startsWith("## ")) {
      html.push(`<h2>${inlineMarkdown(line.slice(3))}</h2>`);
      continue;
    }
    if (line.startsWith("# ")) {
      html.push(`<h1>${inlineMarkdown(line.slice(2))}</h1>`);
      continue;
    }

    if (line.startsWith("- ")) {
      const items = [line.slice(2)];
      while (i + 1 < lines.length && lines[i + 1].trimStart().startsWith("- ")) {
        i += 1;
        items.push(lines[i].trimStart().slice(2));
      }
      html.push(`<ul>${items.map((item) => `<li>${inlineMarkdown(item)}</li>`).join("")}</ul>`);
      continue;
    }

    html.push(`<p>${inlineMarkdown(line)}</p>`);
  }

  return html.join("") || "<p><em>Nothing to preview yet.</em></p>";
}

function noteTitle(note) {
  if (note.title.trim()) {
    return note.title.trim();
  }

  const firstLine = note.body.split("\n").find((line) => line.trim());
  if (firstLine) {
    return firstLine.trim().slice(0, 70);
  }

  return "Untitled note";
}

function noteSnippet(note) {
  const source = note.body.replace(/\s+/g, " ").trim();
  return source ? source.slice(0, 120) : "No content yet";
}

function filterNotes(notes, query) {
  if (!query.trim()) {
    return notes;
  }
  const q = query.trim().toLowerCase();
  return notes.filter((note) => {
    const hay = `${noteTitle(note)} ${note.body} ${note.tags.join(" ")}`.toLowerCase();
    return hay.includes(q);
  });
}

function wordCount(text) {
  const clean = text.trim();
  if (!clean) {
    return 0;
  }
  return clean.split(/\s+/).length;
}

function noteMatchesView(note, view = state.view) {
  return view === "archived" ? note.archived : !note.archived;
}

function ensureActiveInView(preferredId = null) {
  if (preferredId) {
    const preferred = state.notes.find((note) => note.id === preferredId);
    if (preferred && noteMatchesView(preferred)) {
      state.activeId = preferred.id;
      return;
    }
  }

  const active = getActiveNote();
  if (active && noteMatchesView(active)) {
    return;
  }

  const first = sortNotes(state.notes).find((note) => noteMatchesView(note));
  state.activeId = first ? first.id : null;
}

function renderViewControls() {
  elements.viewActiveBtn.classList.toggle("active", state.view === "active");
  elements.viewArchivedBtn.classList.toggle("active", state.view === "archived");
}

function renderActionStates() {
  const active = getActiveNote();
  const hasActive = Boolean(active);

  elements.deleteNoteBtn.disabled = !hasActive;
  elements.previewToggle.disabled = !hasActive;
  elements.archiveBtn.disabled = !hasActive;
  elements.pinBtn.disabled = !hasActive || Boolean(active && active.archived);
}

function renderNoteList() {
  const activeCount = state.notes.filter((note) => !note.archived).length;
  const archivedCount = state.notes.length - activeCount;
  elements.noteCount.textContent = `Active ${activeCount} · Archived ${archivedCount}`;

  const inView = sortNotes(state.notes).filter((note) => noteMatchesView(note));
  const filtered = filterNotes(inView, state.query);

  if (filtered.length === 0) {
    const message = state.query.trim()
      ? "No notes match your search."
      : state.view === "archived"
        ? "No archived notes yet."
        : "No active notes yet.";
    elements.noteList.innerHTML = `<p class="muted">${message}</p>`;
    return;
  }

  elements.noteList.innerHTML = filtered
    .map((note, index) => {
      const tags = note.tags.length ? `#${note.tags.join(" #")}` : "";
      const labels = [note.pinned ? "Pinned" : "", note.archived ? "Archived" : ""]
        .filter(Boolean)
        .join(" • ");

      return `
        <article class="note-card ${note.id === state.activeId ? "active" : ""}" data-note-id="${note.id}" style="--i:${index}">
          <h3 class="note-title">${escapeHtml(noteTitle(note))}</h3>
          <p class="note-snippet">${escapeHtml(noteSnippet(note))}</p>
          <div class="note-row">
            <span>${formatRelativeDate(note.updatedAt)}</span>
            <span>${escapeHtml(labels)}</span>
          </div>
          <div class="note-tags">${escapeHtml(tags)}</div>
        </article>
      `;
    })
    .join("");
}

function renderEditorMeta(note) {
  const updatedText = `Updated ${formatRelativeDate(note.updatedAt)}`;
  elements.metaText.textContent = note.archived ? `Archived · ${updatedText}` : updatedText;
  elements.statsText.textContent = `${wordCount(note.body)} words · ${note.body.length} chars`;

  if (state.previewMode) {
    elements.previewPane.innerHTML = markdownToHtml(note.body);
  }
}

function renderEditor() {
  const note = getActiveNote();

  if (!note) {
    elements.emptyState.hidden = false;
    elements.editor.hidden = true;
    renderActionStates();
    return;
  }

  elements.emptyState.hidden = true;
  elements.editor.hidden = false;

  elements.titleInput.value = note.title;
  elements.tagsInput.value = note.tags.join(", ");
  elements.bodyInput.value = note.body;

  const readOnly = note.archived;
  elements.titleInput.readOnly = readOnly;
  elements.tagsInput.readOnly = readOnly;
  elements.bodyInput.readOnly = readOnly;

  renderEditorMeta(note);

  elements.archiveBtn.textContent = note.archived ? "Restore" : "Archive";
  elements.archiveBtn.classList.toggle("active", note.archived);

  elements.pinBtn.textContent = note.pinned ? "Unpin" : "Pin";
  elements.pinBtn.classList.toggle("active", note.pinned);

  elements.previewToggle.classList.toggle("active", state.previewMode);
  elements.previewToggle.textContent = state.previewMode ? "Edit" : "Preview";

  if (state.previewMode) {
    elements.bodyInput.hidden = true;
    elements.previewPane.hidden = false;
    elements.previewPane.innerHTML = markdownToHtml(note.body);
  } else {
    elements.bodyInput.hidden = false;
    elements.previewPane.hidden = true;
  }

  renderActionStates();
}

function renderAll() {
  ensureActiveInView();
  renderViewControls();
  renderNoteList();
  renderEditor();
}

function showToast(message) {
  elements.toast.textContent = message;
  elements.toast.classList.add("show");

  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    elements.toast.classList.remove("show");
  }, 1800);
}

function setActive(id) {
  state.activeId = id;
  renderAll();
}

function setView(view) {
  if (view !== "active" && view !== "archived") {
    return;
  }
  if (state.view === view) {
    return;
  }
  state.view = view;
  saveView();
  renderAll();
}

function createNote() {
  const note = createBlankNote();
  state.notes.push(note);
  state.view = "active";
  saveView();
  state.activeId = note.id;
  saveNotes();
  renderAll();
  elements.titleInput.focus();
  showToast("Note created");
}

function deleteActiveNote() {
  const active = getActiveNote();
  if (!active) {
    return;
  }

  const shouldDelete = window.confirm(`Delete "${noteTitle(active)}"?`);
  if (!shouldDelete) {
    return;
  }

  state.notes = state.notes.filter((note) => note.id !== active.id);

  if (state.notes.length === 0) {
    state.notes = [createBlankNote()];
    state.view = "active";
    saveView();
  }

  saveNotes();
  renderAll();
  showToast("Note deleted");
}

function updateActive(fields, { fullRender = false, touchUpdatedAt = true } = {}) {
  const note = getActiveNote();
  if (!note) {
    return;
  }

  const patch = touchUpdatedAt ? { ...fields, updatedAt: nowIso() } : { ...fields };
  Object.assign(note, patch);
  saveNotes();

  if (fullRender) {
    renderAll();
    return;
  }

  renderNoteList();
  renderEditorMeta(note);
}

function toggleArchiveActive() {
  const note = getActiveNote();
  if (!note) {
    return;
  }

  const archived = !note.archived;
  updateActive({ archived }, { fullRender: true });
  showToast(archived ? "Note archived" : "Note restored");
}

function exportNotes() {
  const payload = {
    app: "kitabu",
    version: 2,
    exportedAt: nowIso(),
    total: state.notes.length,
    notes: state.notes,
  };

  try {
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    const stamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, "-");

    anchor.href = url;
    anchor.download = `kitabu-notes-${stamp}.json`;
    document.body.append(anchor);
    anchor.click();
    anchor.remove();

    setTimeout(() => {
      URL.revokeObjectURL(url);
    }, 1000);

    showToast("Notes exported");
  } catch {
    showToast("Export failed");
  }
}

function readFileText(file) {
  if (typeof file.text === "function") {
    return file.text();
  }

  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ""));
    reader.onerror = () => reject(reader.error || new Error("File read failed"));
    reader.readAsText(file);
  });
}

function mergeImportedNotes(normalized) {
  const byId = new Map(state.notes.map((note) => [note.id, note]));
  let added = 0;
  let updated = 0;
  let skippedOlder = 0;
  let preferredId = null;

  for (const incoming of normalized) {
    const existing = byId.get(incoming.id);
    if (!existing) {
      byId.set(incoming.id, incoming);
      added += 1;
      preferredId = preferredId || incoming.id;
      continue;
    }

    if (parseDateMs(incoming.updatedAt) >= parseDateMs(existing.updatedAt)) {
      byId.set(incoming.id, incoming);
      updated += 1;
      preferredId = preferredId || incoming.id;
    } else {
      skippedOlder += 1;
    }
  }

  state.notes = Array.from(byId.values());

  if (preferredId) {
    state.activeId = preferredId;
  }

  return { added, updated, skippedOlder };
}

function importNotesFromFile(file) {
  if (!file) {
    return;
  }

  if (file.size > MAX_IMPORT_SIZE_BYTES) {
    showToast("Import file is too large");
    return;
  }

  readFileText(file)
    .then((text) => {
      const parsed = safeParse(text);
      const incoming = Array.isArray(parsed)
        ? parsed
        : parsed && Array.isArray(parsed.notes)
          ? parsed.notes
          : null;

      if (!incoming) {
        showToast("Invalid import file");
        return;
      }

      const normalized = incoming.map(normalizeNote).filter(Boolean);
      if (!normalized.length) {
        showToast("No notes found in import");
        return;
      }

      const shouldImport = window.confirm(
        `Import ${normalized.length} notes and merge with ${state.notes.length} existing notes?`
      );
      if (!shouldImport) {
        return;
      }

      const stats = mergeImportedNotes(normalized);
      saveNotes();

      if (state.notes.some((note) => !note.archived)) {
        state.view = "active";
        saveView();
      }

      renderAll();

      if (!stats.added && !stats.updated) {
        showToast("Import finished (no newer notes)");
        return;
      }

      showToast(`Import complete: +${stats.added} new, ${stats.updated} updated`);
    })
    .catch(() => {
      showToast("Import failed");
    });
}

function isMacLike() {
  return /Mac|iPhone|iPad|iPod/i.test(navigator.platform);
}

function hasPrimaryModifier(event) {
  return isMacLike() ? event.metaKey : event.ctrlKey;
}

function isEditableTarget(target) {
  if (!target || typeof target.closest !== "function") {
    return false;
  }
  return Boolean(target.closest("input, textarea, [contenteditable='true']"));
}

function bindEvents() {
  elements.newNoteBtn.addEventListener("click", createNote);
  elements.deleteNoteBtn.addEventListener("click", deleteActiveNote);

  elements.themeBtn.addEventListener("click", toggleTheme);
  elements.exportBtn.addEventListener("click", exportNotes);

  elements.importBtn.addEventListener("click", () => {
    elements.importFileInput.click();
  });

  elements.importFileInput.addEventListener("change", (event) => {
    const [file] = event.target.files || [];
    importNotesFromFile(file);
    event.target.value = "";
  });

  elements.viewActiveBtn.addEventListener("click", () => setView("active"));
  elements.viewArchivedBtn.addEventListener("click", () => setView("archived"));

  elements.searchInput.addEventListener("input", (event) => {
    state.query = event.target.value;
    renderNoteList();
  });

  elements.noteList.addEventListener("click", (event) => {
    const card = event.target.closest("[data-note-id]");
    if (!card) {
      return;
    }
    setActive(card.dataset.noteId);
  });

  elements.titleInput.addEventListener("input", (event) => {
    updateActive({ title: event.target.value });
  });

  elements.tagsInput.addEventListener("input", (event) => {
    updateActive({ tags: parseTagsInput(event.target.value) });
  });

  elements.bodyInput.addEventListener("input", (event) => {
    updateActive({ body: event.target.value });
  });

  elements.previewToggle.addEventListener("click", () => {
    state.previewMode = !state.previewMode;
    renderEditor();
  });

  elements.archiveBtn.addEventListener("click", toggleArchiveActive);

  elements.pinBtn.addEventListener("click", () => {
    const note = getActiveNote();
    if (!note || note.archived) {
      return;
    }
    updateActive({ pinned: !note.pinned }, { fullRender: true });
  });

  document.addEventListener("keydown", (event) => {
    const primary = hasPrimaryModifier(event);
    if (!primary) {
      return;
    }

    const key = event.key.toLowerCase();
    const editable = isEditableTarget(event.target);

    if (key === "n") {
      event.preventDefault();
      createNote();
      return;
    }

    if (editable) {
      return;
    }

    if (key === "d") {
      event.preventDefault();
      deleteActiveNote();
      return;
    }

    if (event.shiftKey && key === "a") {
      event.preventDefault();
      toggleArchiveActive();
    }
  });
}

function init() {
  loadTheme();
  loadView();
  loadNotes();
  bindEvents();
  renderAll();
}

init();
