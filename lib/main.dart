import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:intl/intl.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const KitabuApp());
}

class KitabuApp extends StatefulWidget {
  const KitabuApp({super.key});

  @override
  State<KitabuApp> createState() => _KitabuAppState();
}

class _KitabuAppState extends State<KitabuApp> {
  bool _darkMode = false;

  void _setDarkMode(bool value) {
    setState(() {
      _darkMode = value;
    });
  }

  @override
  Widget build(BuildContext context) {
    final light = ThemeData(
      colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFFD06B2E)),
      useMaterial3: true,
      scaffoldBackgroundColor: const Color(0xFFF3E8D5),
      cardColor: const Color(0xFFFFFAF2),
    );

    final dark = ThemeData(
      colorScheme: ColorScheme.fromSeed(
        seedColor: const Color(0xFFFFA564),
        brightness: Brightness.dark,
      ),
      useMaterial3: true,
    );

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Kitabu',
      theme: light,
      darkTheme: dark,
      themeMode: _darkMode ? ThemeMode.dark : ThemeMode.light,
      home: KitabuHome(
        darkMode: _darkMode,
        onToggleTheme: () => _setDarkMode(!_darkMode),
        onThemeLoaded: _setDarkMode,
      ),
    );
  }
}

class Note {
  Note({
    required this.id,
    required this.title,
    required this.body,
    required this.tags,
    required this.pinned,
    required this.favorite,
    required this.archived,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String title;
  final String body;
  final List<String> tags;
  final bool pinned;
  final bool favorite;
  final bool archived;
  final DateTime createdAt;
  final DateTime updatedAt;

  Note copyWith({
    String? title,
    String? body,
    List<String>? tags,
    bool? pinned,
    bool? favorite,
    bool? archived,
    DateTime? updatedAt,
  }) {
    return Note(
      id: id,
      title: title ?? this.title,
      body: body ?? this.body,
      tags: tags ?? this.tags,
      pinned: pinned ?? this.pinned,
      favorite: favorite ?? this.favorite,
      archived: archived ?? this.archived,
      createdAt: createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'body': body,
      'tags': tags,
      'pinned': pinned,
      'favorite': favorite,
      'archived': archived,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }

  static Note? fromJson(dynamic json) {
    if (json is! Map<String, dynamic>) {
      return null;
    }

    final now = DateTime.now();
    final createdAt = DateTime.tryParse('${json['createdAt']}') ?? now;
    final updatedAt = DateTime.tryParse('${json['updatedAt']}') ?? createdAt;

    final tagsRaw = json['tags'];
    final tags = tagsRaw is List
        ? tagsRaw
            .whereType<String>()
            .map((e) => e.trim().toLowerCase())
            .where((e) => e.isNotEmpty)
            .toSet()
            .toList()
        : <String>[];

    return Note(
      id: (json['id'] as String?)?.trim().isNotEmpty == true
          ? (json['id'] as String)
          : const Uuid().v4(),
      title: (json['title'] as String?) ?? '',
      body: (json['body'] as String?) ?? '',
      tags: tags,
      pinned: json['pinned'] == true,
      favorite: json['favorite'] == true,
      archived: json['archived'] == true,
      createdAt: createdAt,
      updatedAt: updatedAt,
    );
  }
}

class KitabuHome extends StatefulWidget {
  const KitabuHome({
    super.key,
    required this.darkMode,
    required this.onToggleTheme,
    required this.onThemeLoaded,
  });

  final bool darkMode;
  final VoidCallback onToggleTheme;
  final ValueChanged<bool> onThemeLoaded;

  @override
  State<KitabuHome> createState() => _KitabuHomeState();
}

enum NoteListFilter { all, pinned, favorites }

class _KitabuHomeState extends State<KitabuHome> {
  static const _notesKey = 'kitabu.notes.v1';
  static const _themeKey = 'kitabu.theme.v1';
  static const _viewKey = 'kitabu.view.v1';
  static const _maxImportBytes = 5 * 1024 * 1024;

  final _uuid = const Uuid();
  final _titleController = TextEditingController();
  final _tagsController = TextEditingController();
  final _bodyController = TextEditingController();
  final _searchController = TextEditingController();

  final _titleFocus = FocusNode();

  final List<Note> _notes = [];
  String? _activeId;
  bool _previewMode = false;
  bool _showArchived = false;
  NoteListFilter _listFilter = NoteListFilter.all;
  bool _hydratingEditors = false;
  bool _loading = true;

  Timer? _saveDebounce;
  SharedPreferences? _prefs;

  @override
  void initState() {
    super.initState();
    _titleController.addListener(_onEditorChanged);
    _tagsController.addListener(_onEditorChanged);
    _bodyController.addListener(_onEditorChanged);
    _searchController.addListener(() => setState(() {}));
    _init();
  }

  @override
  void dispose() {
    _saveDebounce?.cancel();
    _persistNotes();
    _titleController.dispose();
    _tagsController.dispose();
    _bodyController.dispose();
    _searchController.dispose();
    _titleFocus.dispose();
    super.dispose();
  }

  Future<void> _init() async {
    _prefs = await SharedPreferences.getInstance();

    final theme = _prefs?.getString(_themeKey);
    widget.onThemeLoaded(theme == 'midnight');

    _showArchived = _prefs?.getString(_viewKey) == 'archived';

    final raw = _prefs?.getString(_notesKey);
    final parsed = _decodeJsonSafely(raw);

    final listRaw = parsed is List
        ? parsed
        : parsed is Map<String, dynamic> && parsed['notes'] is List
            ? parsed['notes'] as List
            : null;

    if (listRaw != null) {
      for (final item in listRaw) {
        final note = Note.fromJson(item);
        if (note != null) {
          _notes.add(note);
        }
      }
    }

    if (_notes.isEmpty) {
      _notes.add(_welcomeNote());
    }

    _activeId = _preferredInitialNote()?.id;
    _hydrateEditors();

    if (mounted) {
      setState(() {
        _loading = false;
      });
    }
  }

  Note _welcomeNote() {
    final now = DateTime.now();
    return Note(
      id: _uuid.v4(),
      title: 'Welcome to Kitabu',
      body: '# Start here\n\n- Create notes\n- Pin and archive\n- Import and export backups',
      tags: const ['welcome', 'guide'],
      pinned: false,
      favorite: false,
      archived: false,
      createdAt: now,
      updatedAt: now,
    );
  }

  Note? _preferredInitialNote() {
    final sorted = _sortedNotes(_notes);
    return sorted.firstWhere(
      (n) => !n.archived,
      orElse: () => sorted.isNotEmpty ? sorted.first : _welcomeNote(),
    );
  }

  List<Note> _sortedNotes(List<Note> notes) {
    final list = [...notes];
    list.sort((a, b) {
      if (a.pinned != b.pinned) {
        return a.pinned ? -1 : 1;
      }
      return b.updatedAt.compareTo(a.updatedAt);
    });
    return list;
  }

  List<Note> get _visibleNotes {
    final q = _searchController.text.trim().toLowerCase();
    final inView = _sortedNotes(_notes)
        .where((n) => _showArchived ? n.archived : !n.archived)
        .toList();
    final filtered = inView.where((note) {
      switch (_listFilter) {
        case NoteListFilter.all:
          return true;
        case NoteListFilter.pinned:
          return note.pinned;
        case NoteListFilter.favorites:
          return note.favorite;
      }
    }).toList();

    if (q.isEmpty) {
      return filtered;
    }

    return filtered.where((n) {
      final hay = '${_noteTitle(n)} ${n.body} ${n.tags.join(' ')}'.toLowerCase();
      return hay.contains(q);
    }).toList();
  }

  int get _activeCount => _notes.where((n) => !n.archived).length;
  int get _archivedCount => _notes.length - _activeCount;

  Note? get _activeNote {
    if (_activeId == null) return null;
    for (final n in _notes) {
      if (n.id == _activeId) return n;
    }
    return null;
  }

  String _noteTitle(Note note) {
    final t = note.title.trim();
    if (t.isNotEmpty) return t;
    final lines = note.body.split('\n');
    for (final line in lines) {
      final s = line.trim();
      if (s.isNotEmpty) return s;
    }
    return 'Untitled note';
  }

  String _noteSnippet(Note note) {
    final text = note.body.replaceAll(RegExp(r'\s+'), ' ').trim();
    if (text.isEmpty) return 'No content yet';
    return text.length > 120 ? '${text.substring(0, 120)}…' : text;
  }

  String _formatRelative(DateTime value) {
    final delta = DateTime.now().difference(value);
    if (delta.inMinutes < 1) return 'just now';
    if (delta.inHours < 1) return '${delta.inMinutes}m ago';
    if (delta.inDays < 1) return '${delta.inHours}h ago';
    return DateFormat('MMM d, yyyy').format(value);
  }

  List<String> _parseTags(String raw) {
    return raw
        .split(',')
        .map((e) => e.trim().toLowerCase())
        .where((e) => e.isNotEmpty)
        .toSet()
        .take(12)
        .toList();
  }

  void _selectNote(String id) {
    setState(() {
      _activeId = id;
      _previewMode = false;
    });
    _hydrateEditors();
  }

  void _createNote() {
    final now = DateTime.now();
    final note = Note(
      id: _uuid.v4(),
      title: '',
      body: '',
      tags: const [],
      pinned: false,
      favorite: false,
      archived: false,
      createdAt: now,
      updatedAt: now,
    );

    setState(() {
      _showArchived = false;
      _activeId = note.id;
      _notes.add(note);
      _previewMode = false;
    });

    _saveView();
    _scheduleSave();
    _hydrateEditors();
    _titleFocus.requestFocus();
    _toast('Note created');
  }

  void _deleteActive() {
    final note = _activeNote;
    if (note == null) return;

    final title = _noteTitle(note);
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete note?'),
        content: Text('Delete "$title" permanently?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () {
              Navigator.of(context).pop();
              setState(() {
                _notes.removeWhere((n) => n.id == note.id);
                if (_notes.isEmpty) {
                  _notes.add(_welcomeNote());
                  _showArchived = false;
                  _saveView();
                }
                _activeId = _visibleAfterDelete()?.id;
                _previewMode = false;
              });
              _scheduleSave();
              _hydrateEditors();
              _toast('Note deleted');
            },
            child: const Text('Delete'),
          ),
        ],
      ),
    );
  }

  Note? _visibleAfterDelete() {
    final visible = _visibleNotes;
    if (visible.isNotEmpty) return visible.first;

    final fallback = _sortedNotes(_notes);
    if (fallback.isNotEmpty) return fallback.first;
    return null;
  }

  void _togglePinned() {
    final note = _activeNote;
    if (note == null || note.archived) return;
    _replaceActive(note.copyWith(pinned: !note.pinned, updatedAt: DateTime.now()));
  }

  void _toggleFavorite() {
    final note = _activeNote;
    if (note == null || note.archived) return;
    _replaceActive(note.copyWith(favorite: !note.favorite, updatedAt: DateTime.now()));
  }

  void _toggleArchived() {
    final note = _activeNote;
    if (note == null) return;

    final archived = !note.archived;
    _replaceActive(note.copyWith(archived: archived, updatedAt: DateTime.now()));

    if (archived && !_showArchived) {
      setState(() {
        _activeId = _visibleAfterDelete()?.id;
      });
      _hydrateEditors();
    }

    _toast(archived ? 'Note archived' : 'Note restored');
  }

  void _replaceActive(Note updated) {
    final idx = _notes.indexWhere((n) => n.id == updated.id);
    if (idx < 0) return;

    setState(() {
      _notes[idx] = updated;
    });

    _scheduleSave();
  }

  void _onEditorChanged() {
    if (_hydratingEditors) return;

    final note = _activeNote;
    if (note == null || note.archived) return;

    final updated = note.copyWith(
      title: _titleController.text,
      body: _bodyController.text,
      tags: _parseTags(_tagsController.text),
      updatedAt: DateTime.now(),
    );

    _replaceActive(updated);
  }

  void _hydrateEditors() {
    final note = _activeNote;
    _hydratingEditors = true;
    _titleController.text = note?.title ?? '';
    _tagsController.text = note?.tags.join(', ') ?? '';
    _bodyController.text = note?.body ?? '';
    _hydratingEditors = false;
  }

  void _toggleTheme() {
    widget.onToggleTheme();
    _prefs?.setString(_themeKey, widget.darkMode ? 'sunrise' : 'midnight');
  }

  void _toggleView(bool archived) {
    setState(() {
      _showArchived = archived;
      _activeId = _visibleNotes.isNotEmpty ? _visibleNotes.first.id : null;
      _previewMode = false;
    });
    _saveView();
    _hydrateEditors();
  }

  void _toggleListFilter(NoteListFilter filter) {
    setState(() {
      _listFilter = filter;
      _activeId = _visibleNotes.isNotEmpty ? _visibleNotes.first.id : null;
      _previewMode = false;
    });
    _hydrateEditors();
  }

  void _saveView() {
    _prefs?.setString(_viewKey, _showArchived ? 'archived' : 'active');
  }

  dynamic _decodeJsonSafely(String? raw) {
    if (raw == null || raw.trim().isEmpty) return null;
    try {
      return jsonDecode(raw);
    } catch (_) {
      return null;
    }
  }

  void _scheduleSave() {
    _saveDebounce?.cancel();
    _saveDebounce = Timer(const Duration(milliseconds: 250), _persistNotes);
  }

  void _persistNotes() {
    if (_prefs == null) return;
    final payload = jsonEncode(_notes.map((e) => e.toJson()).toList());
    _prefs!.setString(_notesKey, payload);
  }

  Future<void> _exportNotes() async {
    try {
      final payload = jsonEncode({
        'app': 'kitabu',
        'version': 2,
        'exportedAt': DateTime.now().toIso8601String(),
        'notes': _notes.map((e) => e.toJson()).toList(),
      });

      final dir = await getTemporaryDirectory();
      final stamp = DateFormat('yyyy-MM-dd-HH-mm-ss').format(DateTime.now());
      final file = File('${dir.path}/kitabu-notes-$stamp.json');
      await file.writeAsString(payload);

      await Share.shareXFiles([XFile(file.path)], text: 'Kitabu notes backup');
      _toast('Notes exported');
    } catch (_) {
      _toast('Export failed');
    }
  }

  Future<void> _importNotes() async {
    try {
      final picked = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: const ['json'],
        withData: true,
      );

      if (picked == null || picked.files.isEmpty) return;
      final file = picked.files.first;

      if (file.size > _maxImportBytes) {
        _toast('Import file is too large');
        return;
      }

      String text;
      if (file.bytes != null) {
        text = utf8.decode(file.bytes!);
      } else if (file.path != null) {
        text = await File(file.path!).readAsString();
      } else {
        _toast('Import failed');
        return;
      }

      final parsed = _decodeJsonSafely(text);
      final incoming = parsed is List
          ? parsed
          : parsed is Map<String, dynamic> && parsed['notes'] is List
              ? parsed['notes'] as List
              : null;

      if (incoming == null) {
        _toast('Invalid import file');
        return;
      }

      final normalized = incoming.map(Note.fromJson).whereType<Note>().toList();
      if (normalized.isEmpty) {
        _toast('No notes found in import');
        return;
      }

      int added = 0;
      int updated = 0;
      final map = {for (final n in _notes) n.id: n};

      for (final note in normalized) {
        final existing = map[note.id];
        if (existing == null) {
          map[note.id] = note;
          added += 1;
          continue;
        }

        if (!note.updatedAt.isBefore(existing.updatedAt)) {
          map[note.id] = note;
          updated += 1;
        }
      }

      setState(() {
        _notes
          ..clear()
          ..addAll(map.values);
        _showArchived = false;
        _activeId = _visibleNotes.isNotEmpty ? _visibleNotes.first.id : null;
      });

      _saveView();
      _scheduleSave();
      _hydrateEditors();

      _toast('Import complete: +$added new, $updated updated');
    } catch (_) {
      _toast('Import failed');
    }
  }

  void _toast(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..clearSnackBars()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  Widget _buildLibraryPanel() {
    final visible = _visibleNotes;

    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Row(
              children: [
                const Text('Library', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
                const Spacer(),
                Text('A $_activeCount · R $_archivedCount'),
              ],
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _searchController,
              decoration: const InputDecoration(
                prefixIcon: Icon(Icons.search),
                hintText: 'Search title, tags, content',
                border: OutlineInputBorder(),
                isDense: true,
              ),
            ),
            const SizedBox(height: 8),
            SegmentedButton<bool>(
              segments: const [
                ButtonSegment<bool>(value: false, label: Text('Active')),
                ButtonSegment<bool>(value: true, label: Text('Archived')),
              ],
              selected: {_showArchived},
              onSelectionChanged: (value) => _toggleView(value.first),
            ),
            const SizedBox(height: 10),
            SegmentedButton<NoteListFilter>(
              segments: const [
                ButtonSegment<NoteListFilter>(value: NoteListFilter.all, label: Text('All')),
                ButtonSegment<NoteListFilter>(value: NoteListFilter.pinned, label: Text('Pinned')),
                ButtonSegment<NoteListFilter>(value: NoteListFilter.favorites, label: Text('Favorites')),
              ],
              selected: {_listFilter},
              onSelectionChanged: (value) => _toggleListFilter(value.first),
            ),
            const SizedBox(height: 10),
            Expanded(
              child: visible.isEmpty
                  ? Center(
                      child: Text(
                        _searchController.text.trim().isNotEmpty
                            ? 'No notes match your search.'
                            : _showArchived
                                ? 'No archived notes yet.'
                                : 'No active notes yet.',
                      ),
                    )
                  : ListView.builder(
                      itemCount: visible.length,
                      itemBuilder: (context, index) {
                        final note = visible[index];
                        final selected = note.id == _activeId;
                        return Card(
                          color: selected ? Theme.of(context).colorScheme.secondaryContainer : null,
                          child: ListTile(
                            onTap: () => _selectNote(note.id),
                            title: Text(_noteTitle(note), maxLines: 1, overflow: TextOverflow.ellipsis),
                            subtitle: Text(_noteSnippet(note), maxLines: 2, overflow: TextOverflow.ellipsis),
                            trailing: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Text(_formatRelative(note.updatedAt), style: Theme.of(context).textTheme.labelSmall),
                                if (note.pinned || note.favorite || note.archived)
                                  Text(
                                    _statusText(note),
                                    style: Theme.of(context).textTheme.labelSmall,
                                  ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEditorPanel() {
    final note = _activeNote;

    if (note == null) {
      return const Card(
        margin: EdgeInsets.zero,
        child: Center(child: Text('No note selected')),
      );
    }

    final readOnly = note.archived;

    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(
                  child: TextField(
                    focusNode: _titleFocus,
                    controller: _titleController,
                    readOnly: readOnly,
                    maxLength: 140,
                    decoration: const InputDecoration(
                      labelText: 'Title',
                      border: OutlineInputBorder(),
                      counterText: '',
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    OutlinedButton(
                      onPressed: note.archived ? null : _togglePinned,
                      child: Text(note.pinned ? 'Unpin' : 'Pin'),
                    ),
                    OutlinedButton(
                      onPressed: note.archived ? null : _toggleFavorite,
                      child: Text(note.favorite ? 'Unfavorite' : 'Favorite'),
                    ),
                    OutlinedButton(
                      onPressed: _toggleArchived,
                      child: Text(note.archived ? 'Restore' : 'Archive'),
                    ),
                    OutlinedButton(
                      onPressed: () => setState(() => _previewMode = !_previewMode),
                      child: Text(_previewMode ? 'Edit' : 'Preview'),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _tagsController,
              readOnly: readOnly,
              decoration: const InputDecoration(
                labelText: 'Tags (comma separated)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 8),
            Expanded(
              child: _previewMode
                  ? Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Markdown(
                        data: _bodyController.text,
                        shrinkWrap: true,
                      ),
                    )
                  : TextField(
                      controller: _bodyController,
                      readOnly: readOnly,
                      maxLines: null,
                      expands: true,
                      textAlignVertical: TextAlignVertical.top,
                      decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        hintText: 'Write your note...',
                      ),
                    ),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Text(note.archived ? 'Archived · Updated ${_formatRelative(note.updatedAt)}' : 'Updated ${_formatRelative(note.updatedAt)}'),
                const Spacer(),
                Text('${_wordCount(_bodyController.text)} words · ${_bodyController.text.length} chars'),
              ],
            ),
          ],
        ),
      ),
    );
  }

  int _wordCount(String text) {
    final value = text.trim();
    if (value.isEmpty) return 0;
    return value.split(RegExp(r'\s+')).length;
  }

  String _statusText(Note note) {
    final tags = <String>[
      if (note.pinned) 'Pinned',
      if (note.favorite) 'Favorite',
      if (note.archived) 'Archived',
    ];
    return tags.join(' • ');
  }

  @override
  Widget build(BuildContext context) {
    final note = _activeNote;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Kitabu'),
        actions: [
          IconButton(
            tooltip: widget.darkMode ? 'Switch to light mode' : 'Switch to dark mode',
            onPressed: _toggleTheme,
            icon: Icon(widget.darkMode ? Icons.wb_sunny_outlined : Icons.nightlight_outlined),
          ),
          IconButton(
            tooltip: 'Import JSON',
            onPressed: _importNotes,
            icon: const Icon(Icons.file_upload_outlined),
          ),
          IconButton(
            tooltip: 'Export JSON',
            onPressed: _notes.isEmpty ? null : _exportNotes,
            icon: const Icon(Icons.file_download_outlined),
          ),
          IconButton(
            tooltip: 'New note',
            onPressed: _createNote,
            icon: const Icon(Icons.add),
          ),
          IconButton(
            tooltip: 'Delete active note',
            onPressed: note == null ? null : _deleteActive,
            icon: const Icon(Icons.delete_outline),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : Padding(
              padding: const EdgeInsets.all(12),
              child: LayoutBuilder(
                builder: (context, constraints) {
                  if (constraints.maxWidth >= 940) {
                    return Row(
                      children: [
                        SizedBox(width: 360, child: _buildLibraryPanel()),
                        const SizedBox(width: 12),
                        Expanded(child: _buildEditorPanel()),
                      ],
                    );
                  }

                  return Column(
                    children: [
                      SizedBox(height: 300, child: _buildLibraryPanel()),
                      const SizedBox(height: 12),
                      Expanded(child: _buildEditorPanel()),
                    ],
                  );
                },
              ),
            ),
    );
  }
}
