import React, { useEffect, useState, useRef, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  Alert,
  Modal,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import Markdown from 'react-native-markdown-display';
import useStore from '../store/useStore';
import { Colors } from '../theme/colors';
import { NoteColor } from '../types';
import { RootStackParamList } from '../navigation';
import { ColorPicker } from '../components/ColorPicker';
import { TagPicker } from '../components/TagPicker';
import { MarkdownToolbar } from '../components/MarkdownToolbar';

type EditorRouteProp = RouteProp<RootStackParamList, 'Editor'>;
type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

export const EditorScreen: React.FC = () => {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<EditorRouteProp>();
  const { noteId, templateId } = route.params || {};
  
  const {
    getNoteById,
    createNote,
    updateNote,
    tags,
    folders,
    templates,
    saveNoteVersion,
    getBacklinks,
    applyTemplate,
    setNoteTags,
    setNoteReminder,
  } = useStore();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [color, setColor] = useState<NoteColor>('#1E1E2E');
  const [isPinned, setIsPinned] = useState(false);
  const [isLocked, setIsLocked] = useState(false);
  const [isFavorite, setIsFavorite] = useState(false);
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([]);
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [showTagPicker, setShowTagPicker] = useState(false);
  const [isPreview, setIsPreview] = useState(false);
  const [showOptions, setShowOptions] = useState(false);
  const [backlinks, setBacklinks] = useState<{ id: string; title: string }[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasChanges, setHasChanges] = useState(false);
  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  
  const contentInputRef = useRef<TextInput>(null);
  const autoSaveTimer = useRef<NodeJS.Timeout | null>(null);
  const isNewNote = !noteId;

  // Load note data
  useEffect(() => {
    const loadNote = async () => {
      setIsLoading(true);
      
      if (noteId) {
        const note = getNoteById(noteId);
        if (note) {
          setTitle(note.title);
          setContent(note.content);
          setColor(note.color);
          setIsPinned(note.isPinned);
          setIsLocked(note.isLocked);
          setIsFavorite(note.isFavorite);
          setSelectedTagIds(note.tagIds);
          setLastSaved(new Date(note.updatedAt));
          
          // Load backlinks
          const links = getBacklinks(note.title, note.id);
          setBacklinks(links.map(n => ({ id: n.id, title: n.title || 'Untitled' })));
        }
      } else if (templateId) {
        const templateContent = await applyTemplate(templateId);
        setContent(templateContent);
      }
      
      setIsLoading(false);
    };
    
    loadNote();
  }, [noteId, templateId]);

  // Auto-save
  useEffect(() => {
    if (autoSaveTimer.current) {
      clearTimeout(autoSaveTimer.current);
    }
    
    if (title || content) {
      setHasChanges(true);
      autoSaveTimer.current = setTimeout(() => {
        handleSave(true);
      }, 2000);
    }
    
    return () => {
      if (autoSaveTimer.current) {
        clearTimeout(autoSaveTimer.current);
      }
    };
  }, [title, content, color, isPinned, isFavorite, selectedTagIds]);

  const handleSave = async (silent = false) => {
    if (!title.trim() && !content.trim()) {
      if (!silent) navigation.goBack();
      return;
    }

    try {
      if (noteId) {
        // Save version before updating
        await saveNoteVersion(noteId);
        await updateNote(noteId, {
          title,
          content,
          color,
          isPinned,
          isLocked,
          isFavorite,
        });
        await setNoteTags(noteId, selectedTagIds);
      } else {
        const newNote = await createNote({
          title,
          content,
          color,
          isPinned,
          isLocked,
          isFavorite,
          tagIds: selectedTagIds,
        });
        // Update navigation to use new note ID
        navigation.setParams({ noteId: newNote.id });
      }
      
      setHasChanges(false);
      setLastSaved(new Date());
      
      if (!silent) {
        navigation.goBack();
      }
    } catch (error) {
      console.error('Save error:', error);
      Alert.alert('Error', 'Failed to save note');
    }
  };

  const handleFormat = (type: string) => {
    const input = contentInputRef.current;
    if (!input) return;

    // Get current selection (simplified - in real app use native module)
    let newContent = content;
    
    switch (type) {
      case 'bold':
        newContent = content + '****';
        break;
      case 'italic':
        newContent = content + '__';
        break;
      case 'code':
        newContent = content + '``';
        break;
      case 'link':
        newContent = content + '[](url)';
        break;
      case 'list':
        newContent = content + '\n- ';
        break;
      case 'checklist':
        newContent = content + '\n- [ ] ';
        break;
      case 'h1':
        newContent = content + '\n# ';
        break;
      case 'h2':
        newContent = content + '\n## ';
        break;
    }
    
    setContent(newContent);
  };

  const handleInsert = (text: string) => {
    setContent(content + text);
  };

  const wordCount = content.trim().split(/\s+/).filter(w => w.length > 0).length;
  const charCount = content.length;

  const noteTags = tags.filter(t => selectedTagIds.includes(t.id));

  if (isLoading) {
    return (
      <SafeAreaView style={[styles.container, styles.loadingContainer]}>
        <ActivityIndicator size="large" color={Colors.accent} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => handleSave()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color={Colors.text} />
        </TouchableOpacity>
        
        <View style={styles.headerCenter}>
          <Text style={styles.headerTitle} numberOfLines={1}>
            {title || 'Untitled'}
          </Text>
          {lastSaved && (
            <Text style={styles.lastSaved}>
              {hasChanges ? 'Unsaved changes' : `Saved ${lastSaved.toLocaleTimeString()}`}
            </Text>
          )}
        </View>
        
        <View style={styles.headerActions}>
          <TouchableOpacity onPress={() => setIsPinned(!isPinned)} style={styles.headerButton}>
            <Ionicons name={isPinned ? "pin" : "pin-outline"} size={22} color={isPinned ? Colors.accent : Colors.text} />
          </TouchableOpacity>
          <TouchableOpacity onPress={() => setIsFavorite(!isFavorite)} style={styles.headerButton}>
            <Ionicons name={isFavorite ? "star" : "star-outline"} size={22} color={isFavorite ? Colors.warning : Colors.text} />
          </TouchableOpacity>
          <TouchableOpacity onPress={() => setShowOptions(true)} style={styles.headerButton}>
            <Ionicons name="ellipsis-vertical" size={22} color={Colors.text} />
          </TouchableOpacity>
        </View>
      </View>

      <KeyboardAvoidingView 
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardView}
      >
        <ScrollView 
          style={[styles.scrollView, { backgroundColor: color }]}
          contentContainerStyle={styles.scrollContent}
        >
          {/* Title Input */}
          <TextInput
            style={styles.titleInput}
            placeholder="Title"
            placeholderTextColor={Colors.textMuted}
            value={title}
            onChangeText={setTitle}
            maxLength={200}
          />
          
          {/* Content */}
          {isPreview ? (
            <Markdown style={markdownStyles}>
              {content || '*No content*'}
            </Markdown>
          ) : (
            <TextInput
              ref={contentInputRef}
              style={styles.contentInput}
              placeholder="Start writing..."
              placeholderTextColor={Colors.textMuted}
              value={content}
              onChangeText={setContent}
              multiline
              textAlignVertical="top"
              autoFocus={isNewNote}
            />
          )}
          
          {/* Backlinks */}
          {backlinks.length > 0 && (
            <View style={styles.backlinksSection}>
              <Text style={styles.backlinksTitle}>Linked from ({backlinks.length})</Text>
              {backlinks.slice(0, 5).map(link => (
                <TouchableOpacity 
                  key={link.id}
                  style={styles.backlinkItem}
                  onPress={() => navigation.navigate('Editor', { noteId: link.id })}
                >
                  <Ionicons name="link" size={14} color={Colors.accent} />
                  <Text style={styles.backlinkText}>{link.title || 'Untitled'}</Text>
                </TouchableOpacity>
              ))}
            </View>
          )}
          
          {/* Bottom padding */}
          <View style={{ height: 100 }} />
        </ScrollView>

        {/* Toolbar */}
        <MarkdownToolbar
          onInsert={handleInsert}
          onFormat={handleFormat}
          onPreview={() => setIsPreview(!isPreview)}
          isPreview={isPreview}
        />

        {/* Stats Bar */}
        <View style={styles.statsBar}>
          <Text style={styles.statsText}>{wordCount} words</Text>
          <Text style={styles.statsText}>{charCount} chars</Text>
          {noteTags.length > 0 && (
            <View style={styles.tagRow}>
              {noteTags.slice(0, 3).map(tag => (
                <View key={tag.id} style={[styles.tagChip, { backgroundColor: tag.color }]}>
                  <Text style={styles.tagText}>#{tag.name}</Text>
                </View>
              ))}
            </View>
          )}
        </View>
      </KeyboardAvoidingView>

      {/* Color Picker Modal */}
      <Modal
        visible={showColorPicker}
        transparent
        animationType="slide"
        onRequestClose={() => setShowColorPicker(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Note Color</Text>
              <TouchableOpacity onPress={() => setShowColorPicker(false)}>
                <Ionicons name="close" size={24} color={Colors.text} />
              </TouchableOpacity>
            </View>
            <ColorPicker selectedColor={color} onSelect={(c: NoteColor) => { setColor(c); setShowColorPicker(false); }} />
          </View>
        </View>
      </Modal>

      {/* Tag Picker Modal */}
      <TagPicker
        visible={showTagPicker}
        selectedTagIds={selectedTagIds}
        onSelect={(ids) => { setSelectedTagIds(ids); }}
        onClose={() => setShowTagPicker(false)}
      />

      {/* Options Modal */}
      <Modal
        visible={showOptions}
        transparent
        animationType="fade"
        onRequestClose={() => setShowOptions(false)}
      >
        <TouchableOpacity style={styles.modalOverlay} onPress={() => setShowOptions(false)}>
          <View style={[styles.modalContent, { width: 250, alignSelf: 'flex-end', marginRight: 20, marginTop: 80 }]}>
            <TouchableOpacity style={styles.optionItem} onPress={() => { setShowColorPicker(true); setShowOptions(false); }}>
              <Ionicons name="color-palette" size={20} color={Colors.text} />
              <Text style={styles.optionText}>Change Color</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.optionItem} onPress={() => { setShowTagPicker(true); setShowOptions(false); }}>
              <Ionicons name="pricetag" size={20} color={Colors.text} />
              <Text style={styles.optionText}>Add Tags</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.optionItem} onPress={() => { setIsLocked(!isLocked); setShowOptions(false); }}>
              <Ionicons name={isLocked ? "lock-open" : "lock-closed"} size={20} color={Colors.text} />
              <Text style={styles.optionText}>{isLocked ? 'Unlock' : 'Lock'} Note</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.optionItem} onPress={() => { handleSave(); setShowOptions(false); }}>
              <Ionicons name="time" size={20} color={Colors.text} />
              <Text style={styles.optionText}>Version History</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.optionItem} onPress={() => { setShowOptions(false); }}>
              <Ionicons name="share-outline" size={20} color={Colors.text} />
              <Text style={styles.optionText}>Share</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
    </SafeAreaView>
  );
};

const markdownStyles = {
  body: {
    color: Colors.text,
    fontSize: 16,
    lineHeight: 24,
  },
  heading1: {
    color: Colors.text,
    fontSize: 28,
    fontWeight: '700',
    marginVertical: 16,
  },
  heading2: {
    color: Colors.text,
    fontSize: 24,
    fontWeight: '600',
    marginVertical: 14,
  },
  heading3: {
    color: Colors.text,
    fontSize: 20,
    fontWeight: '600',
    marginVertical: 12,
  },
  paragraph: {
    color: Colors.text,
    fontSize: 16,
    lineHeight: 24,
    marginVertical: 8,
  },
  list_item: {
    color: Colors.text,
    fontSize: 16,
    lineHeight: 24,
  },
  code_inline: {
    backgroundColor: Colors.surfaceLight,
    color: Colors.accent,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
  },
  code_block: {
    backgroundColor: Colors.surfaceLight,
    padding: 12,
    borderRadius: 8,
    marginVertical: 8,
  },
  fence: {
    backgroundColor: Colors.surfaceLight,
    padding: 12,
    borderRadius: 8,
    marginVertical: 8,
    color: Colors.text,
  },
  link: {
    color: Colors.accent,
  },
  blockquote: {
    borderLeftWidth: 4,
    borderLeftColor: Colors.accent,
    paddingLeft: 12,
    marginVertical: 8,
    color: Colors.textSecondary,
  },
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  loadingContainer: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 12,
    backgroundColor: Colors.background,
  },
  backButton: {
    padding: 8,
  },
  headerCenter: {
    flex: 1,
    alignItems: 'center',
  },
  headerTitle: {
    color: Colors.text,
    fontSize: 17,
    fontWeight: '600',
    maxWidth: 200,
  },
  lastSaved: {
    color: Colors.textMuted,
    fontSize: 11,
    marginTop: 2,
  },
  headerActions: {
    flexDirection: 'row',
    gap: 4,
  },
  headerButton: {
    padding: 8,
  },
  keyboardView: {
    flex: 1,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: 20,
  },
  titleInput: {
    fontSize: 28,
    fontWeight: '700',
    color: Colors.text,
    marginBottom: 16,
  },
  contentInput: {
    fontSize: 17,
    color: Colors.text,
    lineHeight: 26,
    minHeight: 300,
  },
  backlinksSection: {
    marginTop: 32,
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: Colors.textMuted + '30',
  },
  backlinksTitle: {
    color: Colors.textSecondary,
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 8,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  backlinkItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 6,
    gap: 8,
  },
  backlinkText: {
    color: Colors.text,
    fontSize: 15,
  },
  statsBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: Colors.surface,
    borderTopWidth: 1,
    borderTopColor: Colors.surfaceLight,
  },
  statsText: {
    color: Colors.textMuted,
    fontSize: 12,
    marginRight: 16,
  },
  tagRow: {
    flexDirection: 'row',
    flex: 1,
    justifyContent: 'flex-end',
    gap: 6,
  },
  tagChip: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 10,
  },
  tagText: {
    color: Colors.text,
    fontSize: 10,
    fontWeight: '500',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: Colors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 20,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  modalTitle: {
    color: Colors.text,
    fontSize: 18,
    fontWeight: '600',
  },
  optionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    gap: 12,
  },
  optionText: {
    color: Colors.text,
    fontSize: 16,
  },
});
