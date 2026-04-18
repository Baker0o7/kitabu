import React, { useEffect, useCallback, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  RefreshControl,
  Alert,
  Modal,
  ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import useStore from '../store/useStore';
import { Colors } from '../theme/colors';
import { Note, Tag, Template, Folder } from '../types';
import { NoteCard } from '../components/NoteCard';
import { SearchBar } from '../components/SearchBar';
import { EmptyState } from '../components/EmptyState';
import { RootStackParamList } from '../navigation';

type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

export const HomeScreen: React.FC = () => {
  const navigation = useNavigation<NavigationProp>();
  const [refreshing, setRefreshing] = useState(false);
  const [showFabMenu, setShowFabMenu] = useState(false);
  const [showTemplates, setShowTemplates] = useState(false);
  const [showSortMenu, setShowSortMenu] = useState(false);
  
  const {
    notes,
    tags,
    templates,
    folders,
    searchQuery,
    viewMode,
    settings,
    showingArchived,
    showingTrashed,
    showingFavorites,
    showingDaily,
    selectedFolderId,
    selectedTagId,
    isLoading,
    initialize,
    setSearchQuery,
    setViewMode,
    trashNote,
    restoreNote,
    deleteNote,
    togglePin,
    toggleFavorite,
    toggleArchive,
    setShowingArchived,
    setShowingTrashed,
    setShowingFavorites,
    setShowingDaily,
    setSelectedFolder,
    setSelectedTag,
    getFilteredNotes,
    getOrCreateDailyNote,
    updateSettings,
  } = useStore();

  useEffect(() => {
    initialize();
  }, []);

  const filteredNotes = getFilteredNotes();

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await initialize();
    setRefreshing(false);
  }, []);

  const handleNotePress = (note: Note) => {
    navigation.navigate('Editor', { noteId: note.id });
  };

  const handleNoteLongPress = (note: Note) => {
    type AlertButton = { text: string; onPress?: () => void | Promise<void>; style?: 'default' | 'cancel' | 'destructive' };
    const options: AlertButton[] = [
      { text: note.isPinned ? 'Unpin' : 'Pin', onPress: () => togglePin(note.id) },
      { text: note.isFavorite ? 'Unfavorite' : 'Favorite', onPress: () => toggleFavorite(note.id) },
    ];
    
    if (!showingTrashed) {
      options.push({ text: note.isArchived ? 'Unarchive' : 'Archive', onPress: () => toggleArchive(note.id) });
    }
    
    if (showingTrashed) {
      options.push({ text: 'Restore', onPress: () => restoreNote(note.id) });
      options.push({ text: 'Delete Permanently', onPress: () => confirmDelete(note), style: 'destructive' });
    } else {
      options.push({ text: 'Move to Trash', onPress: () => trashNote(note.id), style: 'destructive' });
    }
    
    options.push({ text: 'Cancel', style: 'cancel' });

    Alert.alert(
      note.title || 'Untitled',
      'Choose an action',
      options
    );
  };

  const confirmDelete = (note: Note) => {
    Alert.alert(
      'Delete Permanently?',
      'This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Delete', style: 'destructive', onPress: () => deleteNote(note.id) },
      ]
    );
  };

  const handleCreateNote = async (templateId?: string) => {
    setShowFabMenu(false);
    setShowTemplates(false);
    navigation.navigate('Editor', { templateId });
  };

  const handleCreateDailyNote = async () => {
    setShowFabMenu(false);
    const note = await getOrCreateDailyNote();
    navigation.navigate('Editor', { noteId: note.id });
  };

  const handleSortChange = (sortOrder: 'newest' | 'oldest' | 'alphabetical' | 'updated') => {
    updateSettings({ sortOrder });
    setShowSortMenu(false);
  };

  const toggleViewMode = () => {
    const modes: ('grid' | 'compact' | 'list')[] = ['grid', 'list', 'compact'];
    const currentIndex = modes.indexOf(viewMode);
    const nextMode = modes[(currentIndex + 1) % modes.length];
    setViewMode(nextMode);
  };

  const getScreenTitle = () => {
    if (showingTrashed) return 'Trash';
    if (showingArchived) return 'Archive';
    if (showingFavorites) return 'Favorites';
    if (showingDaily) return 'Daily Notes';
    if (selectedFolderId) {
      const folder = folders.find(f => f.id === selectedFolderId);
      return folder?.name || 'Folder';
    }
    if (selectedTagId) {
      const tag = tags.find(t => t.id === selectedTagId);
      return tag ? `#${tag.name}` : 'Tagged Notes';
    }
    return 'All Notes';
  };

  const getEmptyState = () => {
    if (showingTrashed) return { icon: '🗑️', title: 'Trash is empty', subtitle: 'Deleted notes appear here for 30 days' };
    if (showingArchived) return { icon: '📤', title: 'No archived notes', subtitle: 'Long-press a note and choose Archive to move it here' };
    if (showingFavorites) return { icon: '⭐', title: 'No favorites yet', subtitle: 'Long-press a note and choose Favorite' };
    if (showingDaily) return { icon: '📅', title: 'No daily notes', subtitle: 'Create your first daily journal entry' };
    return { icon: '📝', title: 'No notes yet', subtitle: 'Tap + to start writing' };
  };

  const renderHeader = () => (
    <View style={styles.header}>
      <View style={styles.headerTop}>
        <Text style={styles.title}>{getScreenTitle()}</Text>
        <View style={styles.headerActions}>
          <TouchableOpacity style={styles.iconButton} onPress={() => setShowSortMenu(true)}>
            <Ionicons name="funnel-outline" size={22} color={Colors.text} />
          </TouchableOpacity>
          <TouchableOpacity style={styles.iconButton} onPress={toggleViewMode}>
            <Ionicons 
              name={viewMode === 'grid' ? 'grid' : viewMode === 'list' ? 'list' : 'reorder-three'} 
              size={22} 
              color={Colors.text} 
            />
          </TouchableOpacity>
          <TouchableOpacity style={styles.iconButton} onPress={() => navigation.navigate('Settings')}>
            <Ionicons name="settings-outline" size={22} color={Colors.text} />
          </TouchableOpacity>
        </View>
      </View>
      
      <SearchBar value={searchQuery} onChangeText={setSearchQuery} />
      
      {/* Filter chips */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterChips}>
        <TouchableOpacity 
          style={[styles.filterChip, !showingArchived && !showingTrashed && !showingFavorites && !showingDaily && !selectedFolderId && !selectedTagId && styles.filterChipActive]}
          onPress={() => { setShowingArchived(false); setShowingTrashed(false); setShowingFavorites(false); setShowingDaily(false); setSelectedFolder(null); setSelectedTag(null); }}
        >
          <Text style={[styles.filterChipText, !showingArchived && !showingTrashed && !showingFavorites && !showingDaily && !selectedFolderId && !selectedTagId && styles.filterChipTextActive]}>All</Text>
        </TouchableOpacity>
        <TouchableOpacity 
          style={[styles.filterChip, showingFavorites && styles.filterChipActive]}
          onPress={() => { setShowingFavorites(!showingFavorites); setShowingArchived(false); setShowingTrashed(false); setShowingDaily(false); }}
        >
          <Text style={[styles.filterChipText, showingFavorites && styles.filterChipTextActive]}>⭐ Favorites</Text>
        </TouchableOpacity>
        <TouchableOpacity 
          style={[styles.filterChip, showingDaily && styles.filterChipActive]}
          onPress={() => { setShowingDaily(!showingDaily); setShowingArchived(false); setShowingTrashed(false); setShowingFavorites(false); }}
        >
          <Text style={[styles.filterChipText, showingDaily && styles.filterChipTextActive]}>📅 Daily</Text>
        </TouchableOpacity>
        <TouchableOpacity 
          style={[styles.filterChip, showingArchived && styles.filterChipActive]}
          onPress={() => { setShowingArchived(!showingArchived); setShowingTrashed(false); setShowingFavorites(false); setShowingDaily(false); }}
        >
          <Text style={[styles.filterChipText, showingArchived && styles.filterChipTextActive]}>📤 Archive</Text>
        </TouchableOpacity>
        <TouchableOpacity 
          style={[styles.filterChip, showingTrashed && styles.filterChipActive]}
          onPress={() => { setShowingTrashed(!showingTrashed); setShowingArchived(false); setShowingFavorites(false); setShowingDaily(false); }}
        >
          <Text style={[styles.filterChipText, showingTrashed && styles.filterChipTextActive]}>🗑️ Trash</Text>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );

  const numColumns = viewMode === 'grid' ? 2 : 1;

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {renderHeader()}
      
      <FlatList
        data={filteredNotes}
        keyExtractor={(item) => item.id}
        numColumns={numColumns}
        key={viewMode}
        contentContainerStyle={[
          styles.listContent,
          filteredNotes.length === 0 && styles.emptyList,
        ]}
        renderItem={({ item }) => (
          <NoteCard
            note={item}
            tags={tags}
            onPress={() => handleNotePress(item)}
            onLongPress={() => handleNoteLongPress(item)}
            onDelete={() => showingTrashed ? deleteNote(item.id) : trashNote(item.id)}
            viewMode={viewMode}
            isTrash={showingTrashed}
          />
        )}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={Colors.accent} />
        }
        ListEmptyComponent={
          <EmptyState {...getEmptyState()} />
        }
      />

      {/* FAB */}
      <View style={styles.fabContainer}>
        {showFabMenu && (
          <View style={styles.fabMenu}>
            <TouchableOpacity style={styles.fabMenuItem} onPress={() => handleCreateNote()}>
              <Text style={styles.fabMenuIcon}>📝</Text>
              <Text style={styles.fabMenuText}>Blank Note</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.fabMenuItem} onPress={handleCreateDailyNote}>
              <Text style={styles.fabMenuIcon}>📅</Text>
              <Text style={styles.fabMenuText}>Daily Note</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.fabMenuItem} onPress={() => { setShowFabMenu(false); setShowTemplates(true); }}>
              <Text style={styles.fabMenuIcon}>📄</Text>
              <Text style={styles.fabMenuText}>From Template</Text>
            </TouchableOpacity>
          </View>
        )}
        <TouchableOpacity 
          style={[styles.fab, showFabMenu && styles.fabActive]} 
          onPress={() => setShowFabMenu(!showFabMenu)}
        >
          <Ionicons name={showFabMenu ? "close" : "add"} size={28} color={Colors.text} />
        </TouchableOpacity>
      </View>

      {/* Templates Modal */}
      <Modal
        visible={showTemplates}
        transparent
        animationType="fade"
        onRequestClose={() => setShowTemplates(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Choose Template</Text>
              <TouchableOpacity onPress={() => setShowTemplates(false)}>
                <Ionicons name="close" size={24} color={Colors.text} />
              </TouchableOpacity>
            </View>
            <FlatList
              data={templates}
              keyExtractor={(item) => item.id}
              renderItem={({ item }) => (
                <TouchableOpacity 
                  style={styles.templateItem}
                  onPress={() => handleCreateNote(item.id)}
                >
                  <Text style={styles.templateIcon}>{item.icon}</Text>
                  <View style={styles.templateInfo}>
                    <Text style={styles.templateName}>{item.name}</Text>
                    {item.isBuiltIn && <Text style={styles.templateBuiltIn}>Built-in</Text>}
                  </View>
                  <Ionicons name="chevron-forward" size={20} color={Colors.textMuted} />
                </TouchableOpacity>
              )}
            />
          </View>
        </View>
      </Modal>

      {/* Sort Menu Modal */}
      <Modal
        visible={showSortMenu}
        transparent
        animationType="fade"
        onRequestClose={() => setShowSortMenu(false)}
      >
        <TouchableOpacity style={styles.modalOverlay} onPress={() => setShowSortMenu(false)}>
          <View style={[styles.modalContent, { maxHeight: 300 }]}>
            <Text style={styles.modalTitle}>Sort By</Text>
            {[
              { key: 'newest', label: 'Newest First', icon: 'time' },
              { key: 'oldest', label: 'Oldest First', icon: 'time-outline' },
              { key: 'updated', label: 'Recently Updated', icon: 'refresh' },
              { key: 'alphabetical', label: 'Alphabetical', icon: 'text' },
            ].map((option) => (
              <TouchableOpacity
                key={option.key}
                style={[styles.sortOption, settings.sortOrder === option.key && styles.sortOptionActive]}
                onPress={() => handleSortChange(option.key as any)}
              >
                <Ionicons 
                  name={option.icon as any} 
                  size={20} 
                  color={settings.sortOrder === option.key ? Colors.accent : Colors.text} 
                />
                <Text style={[styles.sortOptionText, settings.sortOrder === option.key && styles.sortOptionTextActive]}>
                  {option.label}
                </Text>
                {settings.sortOrder === option.key && (
                  <Ionicons name="checkmark" size={20} color={Colors.accent} />
                )}
              </TouchableOpacity>
            ))}
          </View>
        </TouchableOpacity>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    paddingTop: 8,
  },
  headerTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    color: Colors.text,
  },
  headerActions: {
    flexDirection: 'row',
    gap: 8,
  },
  iconButton: {
    padding: 8,
    borderRadius: 10,
    backgroundColor: Colors.surfaceLight,
  },
  filterChips: {
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  filterChip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: Colors.surfaceLight,
    marginRight: 8,
  },
  filterChipActive: {
    backgroundColor: Colors.accent,
  },
  filterChipText: {
    color: Colors.textSecondary,
    fontSize: 14,
    fontWeight: '500',
  },
  filterChipTextActive: {
    color: Colors.text,
  },
  listContent: {
    padding: 6,
    paddingBottom: 100,
  },
  emptyList: {
    flexGrow: 1,
    justifyContent: 'center',
  },
  fabContainer: {
    position: 'absolute',
    right: 20,
    bottom: 20,
    alignItems: 'flex-end',
  },
  fabMenu: {
    backgroundColor: Colors.surface,
    borderRadius: 16,
    padding: 12,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  fabMenuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 12,
  },
  fabMenuIcon: {
    fontSize: 20,
    marginRight: 12,
  },
  fabMenuText: {
    color: Colors.text,
    fontSize: 15,
    fontWeight: '500',
  },
  fab: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: Colors.accent,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: Colors.accent,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 8,
    elevation: 8,
  },
  fabActive: {
    backgroundColor: Colors.error,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'center',
    padding: 20,
  },
  modalContent: {
    backgroundColor: Colors.surface,
    borderRadius: 20,
    maxHeight: '80%',
    overflow: 'hidden',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: Colors.surfaceLight,
  },
  modalTitle: {
    color: Colors.text,
    fontSize: 18,
    fontWeight: '600',
    padding: 20,
  },
  templateItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: Colors.surfaceLight,
  },
  templateIcon: {
    fontSize: 24,
    marginRight: 12,
  },
  templateInfo: {
    flex: 1,
  },
  templateName: {
    color: Colors.text,
    fontSize: 16,
    fontWeight: '500',
  },
  templateBuiltIn: {
    color: Colors.textMuted,
    fontSize: 12,
    marginTop: 2,
  },
  sortOption: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    gap: 12,
  },
  sortOptionActive: {
    backgroundColor: Colors.accent + '15',
  },
  sortOptionText: {
    color: Colors.text,
    fontSize: 16,
    flex: 1,
  },
  sortOptionTextActive: {
    color: Colors.accent,
    fontWeight: '600',
  },
});
