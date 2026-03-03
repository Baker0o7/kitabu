const STORAGE_KEY = "kitabu.notes.v1";
const THEME_KEY = "kitabu.theme.v1";

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

function nowIso() {
  return new Date().toISOString();
}

function createBlankNote() {
  const timestamp = nowIso();
  return {
    id: crypto.randomUUID(),
    title: "",
    body: "",
    tags: [],
    pinned: false,
    archived: false,
    createdAt: timestamp,
    updatedAt: timestamp,
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
    "- Click New note to create entries",
    "- Use commas in tags field",
    "- Toggle Preview for lightweight markdown",
    "- Export JSON backups any time",
  ].join("\n");
  note.tags = ["welcome", "guide"];
  note.updatedAt = nowIso();
  return [note];
}

function safeParse(value) {
  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}

function normalizeIso(value, fallback) {
  if (typeof value !== "string") {
    return fallback;
  }
  return Number.isNaN(Date.parse(value)) ? fallback : value;
}

function normalizeNote(note) {
  if (!note || typeof note !== "object") {
    return null;
  }

  const timestamp = nowIso();
  const createdAt = normalizeIso(note.createdAt, timestamp);
  const updatedAt = normalizeIso(note.updatedAt, createdAt);

  const tags = Array.isArray(note.tags)
    ? [...new Set(note.tags.filter((tag) => typeof tag === "string").map((tag) => tag.trim().toLowerCase()).filter(Boolean))]
    : [];

  return {
    id: typeof note.id === "string" && note.id.trim() ? note.id : crypto.randomUUID(),
    title: typeof note.title === "string" ? note.title : "",
    body: typeof note.body === "string" ? note.body : "",
    tags,
    pinned: Boolean(note.pinned),
    archived: Boolean(note.archived),
    createdAt,
    updatedAt,
  };
}

function loadNotes() {
  const raw = localStorage.getItem(STORAGE_KEY);
  const parsed = raw ? safeParse(raw) : null;

  const incoming = Array.isArray(parsed)
    ? parsed
    : parsed && Array.isArray(parsed.notes)
      ? parsed.notes
      : [];

  const normalized = incoming.map(normalizeNote).filter(Boolean);

  if (normalized.length === 0) {
    state.notes = defaultNotes();
    state.activeId = state.notes[0].id;
    saveNotes();
    return;
  }

  state.notes = normalized;
  const firstActive = sortNotes(state.notes).find((note) => !note.archived);
  state.activeId = firstActive ? firstActive.id : sortNotes(state.notes)[0].id;
}

function saveNotes() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state.notes));
}

function loadTheme() {
  const stored = localStorage.getItem(THEME_KEY);
  state.theme = stored === "midnight" ? "midnight" : "sunrise";
  applyTheme();
}

function applyTheme() {
  document.documentElement.setAttribute("data-theme", state.theme);
  elements.themeBtn.textContent = state.theme === "midnight" ? "Sun" : "Night";
}

function toggleTheme() {
  state.theme = state.theme === "midnight" ? "sunrise" : "midnight";
  localStorage.setItem(THEME_KEY, state.theme);
  applyTheme();
}

function getActiveNote() {
  return state.notes.find((note) => note.id === state.activeId) || null;
}

function sortNotes(notes) {
  return [...notes].sort((a, b) => {
    if (a.pinned !== b.pinned) {
      return a.pinned ? -1 : 1;
    }
    return Date.parse(b.updatedAt) - Date.parse(a.updatedAt);
  });
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
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
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

function inlineMarkdown(value) {
  return value
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/\*([^*]+)\*/g, "<em>$1</em>");
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
  if (view === "archived") {
    return note.archived;
  }
  return !note.archived;
}

function ensureActiveInView(preferredId = null) {
  if (preferredId) {
    const preferred = state.notes.find((note) => note.id === preferredId);
    if (preferred && noteMatchesView(preferred)) {
      state.activeId = preferredId;
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
            <span>${labels}</span>
          </div>
          <div class="note-tags">${escapeHtml(tags)}</div>
        </article>
      `;
    })
    .join("");
}

function renderEditor() {
  const note = getActiveNote();

  if (!note) {
    elements.emptyState.hidden = false;
    elements.editor.hidden = true;
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
}

function renderEditorMeta(note) {
  const updatedText = `Updated ${formatRelativeDate(note.updatedAt)}`;
  elements.metaText.textContent = note.archived ? `Archived · ${updatedText}` : updatedText;
  elements.statsText.textContent = `${wordCount(note.body)} words · ${note.body.length} chars`;
  if (state.previewMode) {
    elements.previewPane.innerHTML = markdownToHtml(note.body);
  }
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
  }, 1700);
}

function setActive(id) {
  state.activeId = id;
  renderAll();
}

function setView(view) {
  if (state.view === view) {
    return;
  }
  state.view = view;
  renderAll();
}

function createNote() {
  const note = createBlankNote();
  state.notes.push(note);
  state.view = "active";
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
  }

  saveNotes();
  renderAll();
  showToast("Note deleted");
}

function updateActive(fields, { fullRender = false } = {}) {
  const note = getActiveNote();
  if (!note) {
    return;
  }

  Object.assign(note, fields, { updatedAt: nowIso() });
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
    version: 2,
    exportedAt: nowIso(),
    notes: state.notes,
  };

  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  const stamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, "-");
  anchor.href = url;
  anchor.download = `kitabu-notes-${stamp}.json`;
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
  showToast("Notes exported");
}

function importNotesFromFile(file) {
  if (!file) {
    return;
  }

  file
    .text()
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
      if (normalized.length === 0) {
        showToast("No notes found in import");
        return;
      }

      const byId = new Map(state.notes.map((note) => [note.id, note]));
      for (const note of normalized) {
        byId.set(note.id, note);
      }

      state.notes = Array.from(byId.values());
      state.view = "active";
      const firstImported = normalized.find((note) => !note.archived) || normalized[0];
      state.activeId = firstImported.id;

      saveNotes();
      renderAll();
      showToast(`Imported ${normalized.length} notes`);
    })
    .catch(() => {
      showToast("Import failed");
    });
}

function bindEvents() {
  elements.newNoteBtn.addEventListener("click", createNote);
  elements.deleteNoteBtn.addEventListener("click", deleteActiveNote);

  elements.themeBtn.addEventListener("click", () => {
    toggleTheme();
  });

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
    const tags = event.target.value
      .split(",")
      .map((tag) => tag.trim().toLowerCase())
      .filter(Boolean)
      .slice(0, 12);
    updateActive({ tags: [...new Set(tags)] });
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
    if (!note) {
      return;
    }
    updateActive({ pinned: !note.pinned }, { fullRender: true });
  });

  document.addEventListener("keydown", (event) => {
    const meta = event.metaKey || event.ctrlKey;
    if (meta && event.key.toLowerCase() === "n") {
      event.preventDefault();
      createNote();
      return;
    }
    if (meta && event.key.toLowerCase() === "d") {
      event.preventDefault();
      deleteActiveNote();
      return;
    }
    if (meta && event.shiftKey && event.key.toLowerCase() === "a") {
      event.preventDefault();
      toggleArchiveActive();
    }
  });
}

function init() {
  loadTheme();
  loadNotes();
  bindEvents();
  renderAll();
}

init();
