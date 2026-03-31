# Kitabu v2.1

A full-featured, dark-themed **second brain** notes app for Android — built in Kotlin with Room, MVVM, Markwon, and Gemini AI.

---

## Feature Set

### Core
| Feature | Details |
|---|---|
| Rich Markdown editing | Bold, italic, strikethrough, code, code blocks, H1-H3, lists, task lists, tables, horizontal rules |
| Live preview toggle | One tap switches between edit and rendered Markdown |
| Wikilink `[[note title]]` | Cross-link notes with autocomplete; backlink count shown in editor |
| 8 note colour themes | Per-note background colour picker (Default, Rose, Ocean, Forest, Amber, Lavender, Teal, Charcoal) |
| Staggered grid list | Pinterest-style card layout with tag chips and word count badge |
| Pin & lock notes | Pinned notes float to top; locked notes require biometric auth |
| Archive | Move notes to archive; swipe to restore from archive view |

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

### AI (requires Google AI Studio key — free)
| Feature | Details |
|---|---|
| AI Assistant | Full chat interface with note context injected as system prompt |
| Quick actions | Summarise, Expand, Rewrite, Flashcards, Suggest tags, Outline |
| Voice-to-text | Tap microphone in the Markdown toolbar to dictate |
| Multi-turn conversation | Maintains conversation context across messages |

### Visual
| Feature | Details |
|---|---|
| Knowledge Graph | Force-directed graph of all notes; edges from `[[wikilinks]]`; tap node to open; pinch to zoom; pan; connection count |
| 9 themes | Noir, Midnight, Forest, Crimson, Dusk, Nord, Rose, Slate, Aurora |
| Smart text stats | Words, characters, reading time estimate in editor subtitle |
| Relative dates | "Just now / 5m ago / Yesterday / Mar 3" |

---

## Architecture

```
kitabu/
└── app/src/main/
    ├── kotlin/com/kitabu/app/
    │   ├── data/
    │   │   ├── Note.kt, Tag.kt, NoteTag.kt        <- Entities
    │   │   ├── NoteWithTags.kt                    <- @Relation result
    │   │   ├── NoteVersion.kt, Template.kt        <- History & templates
    │   │   ├── NoteDao, TagDao, NoteVersionDao,
    │   │   │   TemplateDao                        <- Room DAOs
    │   │   ├── KitabuDatabase.kt                  <- DB + migration v1->v2->v3
    │   │   └── NoteRepository, TagRepository,
    │   │       TemplateRepository                 <- Single sources of truth
    │   ├── ui/
    │   │   ├── notes/   MainActivity, NoteAdapter, NoteViewModel
    │   │   ├── editor/  EditorActivity (Markdown toolbar, voice, code block, autosave)
    │   │   ├── ai/      AiAssistantActivity (Gemini API)
    │   │   ├── graph/   GraphActivity + GraphView (custom canvas)
    │   │   ├── history/ VersionHistoryActivity (preview + restore)
    │   │   ├── tags/    TagManagerActivity (create, rename, delete)
    │   │   ├── templates/ TemplatesActivity (built-in + custom)
    │   │   ├── settings/ SettingsActivity (AI key + version info)
    │   │   └── theme/   ThemePickerActivity (9 themes with previews)
    │   └── util/
    │       ├── MarkdownHelper.kt   <- Markwon + wikilink utils + template vars
    │       ├── BiometricHelper.kt  <- Biometric/PIN auth
    │       ├── Extensions.kt       <- View, date, string, markdown extensions
    │       ├── SortOrder.kt        <- 5 sort modes
    │       ├── TextStats.kt        <- Words, chars, sentences, reading time
    │       └── ThemeManager.kt     <- 9 themes with persistence
    └── res/
        ├── layout/   14 layouts
        ├── drawable/ 23 vector icons + backgrounds
        ├── menu/     3 menus (drawer, main, editor)
        ├── font/     Inter Regular / Medium / SemiBold
        └── values/   colors, strings, themes, dimens, attrs
```

**Stack:** MVVM · Room 2.6.1 · Kotlin Coroutines + Flow · Markwon 4.6.2 · OkHttp 4.12 · Biometric 1.1 · Material 3

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

## What's New in v2.1

- **Archive system** — move notes to archive, swipe to restore
- **Aurora theme** — 9th theme with deep indigo + violet accent
- **2 new note colours** — Teal and Charcoal
- **3 new templates** — Weekly Review, Habit Tracker, Reading List
- **Code block insertion** — ``` button in Markdown toolbar
- **Tag rename** — tap a tag to rename it
- **Custom templates** — create your own templates with FAB
- **Smart autosave** — hash-based dedup, 1.5s debounce
- **Text stats** — words, chars, reading time in editor
- **Improved graph** — better layout algorithm with edge attraction
- **Version preview** — tap to preview before restoring
- **5 sort modes** — including Title Z-A and Word Count
- **Markdown stripping** — cleaner note previews in card view
- **SDK 35** — target latest Android API level
