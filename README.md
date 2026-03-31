# Kitabu v3.0

A full-featured, dark-themed **second brain** notes app for Android — built in Kotlin with Room, MVVM, Markwon, Gemini AI, and Hilt.

---

## Feature Set

### Core
| Feature | Details |
|---|---|
| Rich Markdown editing | Bold, italic, strikethrough, code, code blocks, H1-H3, lists, task lists, tables, horizontal rules |
| Live preview toggle | One tap switches between edit and rendered Markdown; split-pane in landscape |
| Wikilink `[[note title]]` | Cross-link notes with autocomplete; backlink panel with tappable items |
| 8 note colour themes | Per-note background colour picker (Default, Rose, Ocean, Forest, Amber, Lavender, Teal, Charcoal) |
| Staggered grid list | Pinterest-style card layout with tag chips and word count badge |
| Pin, lock & favorite | Pinned notes float to top; locked notes encrypted at rest; favorites quick-access list |
| Archive | Move notes to archive; swipe to restore from archive view |
| Trash | Soft-delete with 30-day auto-purge; restore from trash view |

### Organisation
| Feature | Details |
|---|---|
| Nested tags | `#work/meetings`, `#personal/health` — tag autocomplete in editor; rename support |
| Filter by tag | Tap any tag in the drawer to filter the note list |
| Sort | Last edited, Date created, Title A-Z, Title Z-A, Word count |
| Full-text search | Debounced real-time search across title + body |
| Daily notes | Auto-creates a dated note per day; separate list in drawer |
| Templates | 8 built-in + custom templates: Meeting, Journal, Book Summary, Project Plan, Quick, Weekly Review, Habit Tracker, Reading List |
| Version history | Auto-snapshot on save, browse & restore up to 30 versions per note; preview before restore |

### Data & Security
| Feature | Details |
|---|---|
| Export/Import | Full JSON backup of all notes, tags, versions, templates; export individual notes as .md |
| Note encryption | Locked notes encrypted with AES-256-GCM via Android Keystore |
| Encrypted preferences | API key stored in EncryptedSharedPreferences |
| Trash auto-purge | WorkManager daily task purges trashed notes older than 30 days |

### AI (requires Google AI Studio key — free)
| Feature | Details |
|---|---|
| AI Assistant | Full chat interface with note context injected as system prompt |
| Quick actions | Summarise, Expand, Rewrite, Flashcards, Suggest tags, Outline |
| Voice-to-text | Tap microphone in the Markdown toolbar to dictate |
| Multi-turn conversation | Maintains conversation context across messages |

### Visual & Widgets
| Feature | Details |
|---|---|
| Knowledge Graph | Force-directed graph of all notes; edges from `[[wikilinks]]`; background computation; tap node to open; pinch to zoom; pan; connection count |
| 9 dark themes | Noir, Midnight, Forest, Crimson, Dusk, Nord, Rose, Slate, Aurora |
| 9 light themes | Light variants for each dark theme, toggleable from theme picker |
| Smart text stats | Words, characters, reading time estimate in editor subtitle |
| Relative dates | "Just now / 5m ago / Yesterday / Mar 3" |
| Home screen widgets | Quick Note widget (tap to create) and Recent Notes widget (shows 5 latest) |

### Editor
| Feature | Details |
|---|---|
| Split-pane preview | Side-by-side edit and preview in landscape mode |
| Keyboard shortcuts | Ctrl+B bold, Ctrl+I italic, Ctrl+S save, Ctrl+K wikilink, Ctrl+P preview, Ctrl+N table |
| Share notes | Share any note via Android share sheet (email, messaging, etc.) |
| Reminders | Set date/time reminders on notes with notification alerts |
| Table insertion | Visual grid dialog with editable headers and cells, generates Markdown tables |
| Backlinks panel | Tappable list of notes linking to the current note, shown below tags |

---

## Architecture

```
kitabu/
└── app/src/main/
    ├── kotlin/com/kitabu/app/
    │   ├── KitabuApplication.kt    <- @HiltAndroidApp, Configuration.Provider, TrashWorker init
    │   ├── data/
    │   │   ├── Note.kt, Tag.kt, NoteTag.kt        <- Entities
    │   │   ├── NoteWithTags.kt                    <- @Relation result
    │   │   ├── NoteVersion.kt, Template.kt        <- History & templates
    │   │   ├── NoteDao, TagDao, NoteVersionDao,
    │   │   │   TemplateDao                        <- Room DAOs
    │   │   ├── KitabuDatabase.kt                  <- DB + migration v1->v2->v3->v4
    │   │   └── NoteRepository, TagRepository,
    │   │       TemplateRepository                 <- Single sources of truth
    │   ├── ui/
    │   │   ├── notes/   MainActivity, NoteAdapter, NoteViewModel
    │   │   ├── editor/  EditorActivity (Markdown toolbar, voice, code block, autosave,
    │   │   │              split-pane, keyboard shortcuts, backlinks, sharing, reminders, encryption)
    │   │   ├── ai/      AiAssistantActivity (Gemini API, encrypted prefs)
    │   │   ├── graph/   GraphActivity + GraphView (background computation)
    │   │   ├── history/ VersionHistoryActivity (preview + restore)
    │   │   ├── tags/    TagManagerActivity (create, rename, delete)
    │   │   ├── templates/ TemplatesActivity (built-in + custom)
    │   │   ├── settings/ SettingsActivity (encrypted API key + version info)
    │   │   ├── theme/   ThemePickerActivity (9 dark + 9 light themes)
    │   │   ├── reminder/ ReminderReceiver (notification channel + alarm)
    │   │   └── widget/   NoteWidgetProvider, NoteQuickNoteWidget
    │   └── util/
    │       ├── MarkdownHelper.kt   <- Markwon + wikilink utils + template vars
    │       ├── CryptoHelper.kt     <- EncryptedSharedPreferences + AES-256-GCM encryption
    │       ├── BiometricHelper.kt  <- Biometric/PIN auth
    │       ├── ReminderHelper.kt   <- AlarmManager reminder scheduling
    │       ├── TrashWorker.kt      <- WorkManager daily trash purge
    │       ├── Extensions.kt       <- View, date, string, markdown extensions
    │       ├── SortOrder.kt        <- 5 sort modes
    │       ├── TextStats.kt        <- Words, chars, sentences, reading time
    │       └── ThemeManager.kt     <- 9 dark + 9 light themes with persistence
    └── res/
        ├── layout/   16 layouts (14 + 2 widget layouts)
        ├── drawable/  27 vector icons + backgrounds
        ├── menu/     3 menus (drawer, main, editor)
        ├── xml/      2 widget metadata files
        ├── font/     Inter Regular / Medium / SemiBold
        └── values/   colors, strings, themes, dimens, attrs
```

**Stack:** MVVM · Room 2.6.1 · Hilt · Kotlin Coroutines + Flow · Markwon 4.6.2 · OkHttp 4.12 · Biometric 1.2 · Material 3 · WorkManager · Coil

---

## Setup

### 1. Clone & open in Android Studio Ladybug+

### 2. Add Inter fonts
Download from [fonts.google.com/specimen/Inter](https://fonts.google.com/specimen/Inter) and put in `app/src/main/res/font/`:
```
inter_regular.ttf   (weight 400)
inter_medium.ttf    (weight 500)
inter_semibold.ttf  (weight 600)
```

### 3. Enable AI features (optional)
Open the app > Settings > paste your free Google AI Studio key.
Get one at [aistudio.google.com](https://aistudio.google.com).

### 4. Build
```bash
./gradlew assembleDebug
```
Min SDK: **26** · Target SDK: **35** · Java: **17** · Kotlin: **2.1.0**

---

## What's New in v3.0

- **Full JSON Export/Import** — backup all notes, tags, versions, templates to JSON file
- **Note Encryption** — locked notes encrypted with AES-256-GCM via Android Keystore
- **Encrypted API Key Storage** — Gemini key stored in EncryptedSharedPreferences
- **Trash System** — soft-delete with 30-day auto-purge via WorkManager
- **Favorites** — bookmark notes for quick access via dedicated drawer section
- **Reminder System** — set date/time reminders on notes with notification alerts
- **Home Screen Widgets** — Quick Note widget + Recent Notes widget
- **Split-Pane Editor** — side-by-side edit/preview in landscape mode
- **Backlinks Panel** — tappable list of linking notes below tags in editor
- **Keyboard Shortcuts** — Ctrl+B/I/S/K/P/N for power users
- **Note Sharing** — share any note via Android share sheet
- **Light Themes** — 9 light theme variants toggleable from theme picker
- **Graph Performance** — layout computation moved to background thread
- **Hilt DI** — dependency injection throughout the app
- **GlobalScope Removed** — replaced with lifecycleScope for proper lifecycle management
