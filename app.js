const STORAGE_KEY = "kitabu.notes.v1";

const elements = {
  newNoteBtn: document.getElementById("new-note-btn"),
  deleteNoteBtn: document.getElementById("delete-note-btn"),
  noteCount: document.getElementById("note-count"),
  searchInput: document.getElementById("search-input"),
  noteList: document.getElementById("note-list"),
  emptyState: document.getElementById("empty-state"),
  editor: document.getElementById("editor"),
  titleInput: document.getElementById("title-input"),
  tagsInput: document.getElementById("tags-input"),
  bodyInput: document.getElementById("body-input"),
  previewPane: document.getElementById("preview-pane"),
  previewToggle: document.getElementById("preview-toggle"),
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
  ].join("\n");
  note.tags = ["welcome", "guide"];
  note.updatedAt = nowIso();
  return [note];
}

function safeParse(value) {
  try {
    const parsed = JSON.parse(value);
    if (!Array.isArray(parsed)) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

function loadNotes() {
  const raw = localStorage.getItem(STORAGE_KEY);
  const parsed = raw ? safeParse(raw) : null;

  if (!parsed || parsed.length === 0) {
    state.notes = defaultNotes();
    state.activeId = state.notes[0].id;
    saveNotes();
    return;
  }

  state.notes = parsed
    .filter((note) => note && typeof note.id === "string")
    .map((note) => ({
      id: note.id,
      title: typeof note.title === "string" ? note.title : "",
      body: typeof note.body === "string" ? note.body : "",
      tags: Array.isArray(note.tags)
        ? note.tags.filter((tag) => typeof tag === "string")
        : [],
      pinned: Boolean(note.pinned),
      createdAt: typeof note.createdAt === "string" ? note.createdAt : nowIso(),
      updatedAt: typeof note.updatedAt === "string" ? note.updatedAt : nowIso(),
    }));

  state.activeId = state.notes[0] ? state.notes[0].id : null;
}

function saveNotes() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state.notes));
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

function renderNoteList() {
  const sorted = sortNotes(state.notes);
  const filtered = filterNotes(sorted, state.query);

  elements.noteCount.textContent = `${state.notes.length} ${state.notes.length === 1 ? "note" : "notes"}`;

  if (filtered.length === 0) {
    elements.noteList.innerHTML = '<p class="muted">No notes match your search.</p>';
    return;
  }

  elements.noteList.innerHTML = filtered
    .map((note, index) => {
      const tags = note.tags.length ? `#${note.tags.join(" #")}` : "";
      const pinLabel = note.pinned ? "Pinned" : "";
      return `
        <article class="note-card ${note.id === state.activeId ? "active" : ""}" data-note-id="${note.id}" style="--i:${index}">
          <h3 class="note-title">${escapeHtml(noteTitle(note))}</h3>
          <p class="note-snippet">${escapeHtml(noteSnippet(note))}</p>
          <div class="note-row">
            <span>${formatRelativeDate(note.updatedAt)}</span>
            <span>${pinLabel}</span>
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
  renderEditorMeta(note);

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
  elements.metaText.textContent = `Updated ${formatRelativeDate(note.updatedAt)}`;
  elements.statsText.textContent = `${wordCount(note.body)} words`;
  if (state.previewMode) {
    elements.previewPane.innerHTML = markdownToHtml(note.body);
  }
}

function renderAll() {
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

function createNote() {
  const note = createBlankNote();
  state.notes.push(note);
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
    const starter = createBlankNote();
    state.notes = [starter];
  }

  state.activeId = sortNotes(state.notes)[0].id;
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

function bindEvents() {
  elements.newNoteBtn.addEventListener("click", createNote);
  elements.deleteNoteBtn.addEventListener("click", deleteActiveNote);

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
    }
  });
}

function init() {
  loadNotes();
  bindEvents();
  renderAll();
}

init();
