import React from 'react';
import { View, TouchableOpacity, StyleSheet, Text } from 'react-native';
import { NoteColors } from '../types';
import { Colors } from '../theme/colors';

interface ColorPickerProps {
  selectedColor: string;
  onSelect: (color: string) => void;
  showLabel?: boolean;
}

export const ColorPicker: React.FC<ColorPickerProps> = ({
  selectedColor,
  onSelect,
  showLabel = false,
}) => {
  const colorNames: Record<string, string> = {
    '#1E1E2E': 'Default',
    '#2D1B2E': 'Rose',
    '#0D2137': 'Ocean',
    '#0D2818': 'Forest',
    '#2D1F00': 'Amber',
    '#1A1A2E': 'Lavender',
    '#0D2225': 'Teal',
    '#1A1A1A': 'Charcoal',
  };

  return (
    <View style={styles.container}>
      {showLabel && <Text style={styles.label}>Note Color</Text>}
      <View style={styles.grid}>
        {NoteColors.map((color) => (
          <TouchableOpacity
            key={color}
            style={[
              styles.colorButton,
              { backgroundColor: color },
              selectedColor === color && styles.selected,
            ]}
            onPress={() => onSelect(color)}
            activeOpacity={0.8}
          >
            {selectedColor === color && (
              <View style={styles.checkmark}>
                <Text style={styles.checkmarkText}>✓</Text>
              </View>
            )}
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginVertical: 12,
  },
  label: {
    color: Colors.textSecondary,
    fontSize: 14,
    marginBottom: 12,
    fontWeight: '500',
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  colorButton: {
    width: 48,
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: 'transparent',
  },
  selected: {
    borderColor: Colors.accent,
    transform: [{ scale: 1.1 }],
  },
  checkmark: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: Colors.accent,
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkmarkText: {
    color: Colors.text,
    fontSize: 14,
    fontWeight: 'bold',
  },
});
