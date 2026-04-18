import React from 'react';
import { View, TouchableOpacity, StyleSheet, ScrollView, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';

interface MarkdownToolbarProps {
  onInsert: (markdown: string) => void;
  onFormat: (type: 'bold' | 'italic' | 'code' | 'link' | 'list' | 'checklist' | 'h1' | 'h2' | 'h3') => void;
  onPreview?: () => void;
  isPreview?: boolean;
}

export const MarkdownToolbar: React.FC<MarkdownToolbarProps> = ({
  onInsert,
  onFormat,
  onPreview,
  isPreview = false,
}) => {
  const buttons = [
    { icon: 'bold', action: () => onFormat('bold'), label: 'B' },
    { icon: 'italic', action: () => onFormat('italic'), label: 'I' },
    { icon: 'code-slash', action: () => onFormat('code'), label: '</>' },
    { icon: 'link', action: () => onFormat('link'), label: '🔗' },
    { icon: 'list', action: () => onFormat('list'), label: '•' },
    { icon: 'checkbox', action: () => onFormat('checklist'), label: '☐' },
    { icon: 'text', action: () => onFormat('h1'), label: 'H1' },
    { icon: 'text', action: () => onFormat('h2'), label: 'H2' },
    { icon: 'remove', action: () => onInsert('---'), label: '—' },
    { icon: 'grid', action: () => onInsert('\n| Col 1 | Col 2 |\n| ----- | ----- |\n|       |       |\n'), label: '▦' },
    { icon: 'mic', action: () => {}, label: '🎙️' },
  ];

  return (
    <View style={styles.container}>
      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <View style={styles.buttons}>
          {buttons.map((btn, index) => (
            <TouchableOpacity
              key={index}
              style={styles.button}
              onPress={btn.action}
            >
              <Text style={styles.buttonText}>{btn.label}</Text>
            </TouchableOpacity>
          ))}
          {onPreview && (
            <TouchableOpacity
              style={[styles.button, isPreview && styles.activeButton]}
              onPress={onPreview}
            >
              <Ionicons name="eye" size={18} color={isPreview ? Colors.accent : Colors.text} />
            </TouchableOpacity>
          )}
        </View>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.surface,
    borderTopWidth: 1,
    borderTopColor: Colors.surfaceLight,
    paddingVertical: 8,
  },
  buttons: {
    flexDirection: 'row',
    paddingHorizontal: 12,
    gap: 8,
  },
  button: {
    minWidth: 40,
    height: 36,
    backgroundColor: Colors.surfaceLight,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 12,
  },
  activeButton: {
    backgroundColor: Colors.accent + '30',
  },
  buttonText: {
    color: Colors.text,
    fontSize: 14,
    fontWeight: '600',
  },
});
