import React, { useState } from 'react';
import { 
  View, Text, TouchableOpacity, StyleSheet, 
  Modal, TextInput, ScrollView, Dimensions 
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Tag } from '../types';
import { Colors, TagColors } from '../theme/colors';
import useStore from '../store/useStore';

interface TagPickerProps {
  visible: boolean;
  selectedTagIds: string[];
  onSelect: (tagIds: string[]) => void;
  onClose: () => void;
}

const { height } = Dimensions.get('window');

export const TagPicker: React.FC<TagPickerProps> = ({
  visible,
  selectedTagIds,
  onSelect,
  onClose,
}) => {
  const { tags, createTag } = useStore();
  const [newTagName, setNewTagName] = useState('');
  const [selectedColor, setSelectedColor] = useState(TagColors[0]);
  const [localSelected, setLocalSelected] = useState<string[]>(selectedTagIds);

  const toggleTag = (tagId: string) => {
    setLocalSelected(prev => 
      prev.includes(tagId) 
        ? prev.filter(id => id !== tagId)
        : [...prev, tagId]
    );
  };

  const handleCreateTag = async () => {
    if (!newTagName.trim()) return;
    const tag = await createTag(newTagName.trim(), selectedColor);
    setLocalSelected([...localSelected, tag.id]);
    setNewTagName('');
  };

  const handleSave = () => {
    onSelect(localSelected);
    onClose();
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}
    >
      <View style={styles.overlay}>
        <View style={styles.container}>
          <View style={styles.header}>
            <Text style={styles.title}>Select Tags</Text>
            <TouchableOpacity onPress={onClose}>
              <Ionicons name="close" size={24} color={Colors.text} />
            </TouchableOpacity>
          </View>

          <View style={styles.createSection}>
            <TextInput
              style={styles.input}
              placeholder="New tag name..."
              placeholderTextColor={Colors.textMuted}
              value={newTagName}
              onChangeText={setNewTagName}
            />
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.colorRow}>
              {TagColors.map(color => (
                <TouchableOpacity
                  key={color}
                  style={[
                    styles.colorDot,
                    { backgroundColor: color },
                    selectedColor === color && styles.colorDotSelected,
                  ]}
                  onPress={() => setSelectedColor(color)}
                />
              ))}
            </ScrollView>
            <TouchableOpacity style={styles.createButton} onPress={handleCreateTag}>
              <Text style={styles.createButtonText}>+ Create Tag</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.tagList}>
            {tags.length === 0 ? (
              <Text style={styles.emptyText}>No tags yet</Text>
            ) : (
              tags.map(tag => (
                <TouchableOpacity
                  key={tag.id}
                  style={[
                    styles.tagItem,
                    localSelected.includes(tag.id) && styles.tagItemSelected,
                  ]}
                  onPress={() => toggleTag(tag.id)}
                >
                  <View style={[styles.tagColor, { backgroundColor: tag.color }]} />
                  <Text style={styles.tagName}>#{tag.name}</Text>
                  {localSelected.includes(tag.id) && (
                    <Ionicons name="checkmark" size={18} color={Colors.accent} />
                  )}
                </TouchableOpacity>
              ))
            )}
          </ScrollView>

          <TouchableOpacity style={styles.saveButton} onPress={handleSave}>
            <Text style={styles.saveButtonText}>Done</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'flex-end',
  },
  container: {
    backgroundColor: Colors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    maxHeight: height * 0.8,
    paddingBottom: 20,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: Colors.surfaceLight,
  },
  title: {
    color: Colors.text,
    fontSize: 18,
    fontWeight: '600',
  },
  createSection: {
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: Colors.surfaceLight,
  },
  input: {
    backgroundColor: Colors.surfaceLight,
    borderRadius: 12,
    padding: 12,
    color: Colors.text,
    fontSize: 16,
  },
  colorRow: {
    marginTop: 12,
  },
  colorDot: {
    width: 32,
    height: 32,
    borderRadius: 16,
    marginRight: 8,
  },
  colorDotSelected: {
    borderWidth: 3,
    borderColor: Colors.text,
  },
  createButton: {
    backgroundColor: Colors.accent,
    borderRadius: 12,
    padding: 12,
    alignItems: 'center',
    marginTop: 12,
  },
  createButtonText: {
    color: Colors.text,
    fontSize: 14,
    fontWeight: '600',
  },
  tagList: {
    maxHeight: 300,
    padding: 12,
  },
  tagItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderRadius: 12,
    marginBottom: 8,
    backgroundColor: Colors.surfaceLight,
  },
  tagItemSelected: {
    backgroundColor: Colors.accent + '20',
  },
  tagColor: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 10,
  },
  tagName: {
    color: Colors.text,
    fontSize: 15,
    flex: 1,
  },
  emptyText: {
    color: Colors.textMuted,
    textAlign: 'center',
    padding: 20,
  },
  saveButton: {
    backgroundColor: Colors.accent,
    margin: 20,
    padding: 16,
    borderRadius: 14,
    alignItems: 'center',
  },
  saveButtonText: {
    color: Colors.text,
    fontSize: 16,
    fontWeight: '600',
  },
});
