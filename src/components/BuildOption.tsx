import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ViewStyle } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';

interface BuildOptionProps {
  number: string;
  title: string;
  subtitle: string;
  description: string;
  features: string[];
  actionLabel: string;
  onAction: () => void;
  isRecommended?: boolean;
  style?: ViewStyle;
}

export const BuildOption: React.FC<BuildOptionProps> = ({
  number,
  title,
  subtitle,
  description,
  features,
  actionLabel,
  onAction,
  isRecommended = false,
  style,
}) => {
  return (
    <View style={[styles.container, isRecommended && styles.containerRecommended, style]}>
      {/* Header Section */}
      <View style={styles.header}>
        <View style={styles.numberBadge}>
          <Text style={styles.numberText}>{number}</Text>
        </View>
        {isRecommended && (
          <View style={styles.recommendedBadge}>
            <Text style={styles.recommendedText}>Recommended</Text>
          </View>
        )}
      </View>

      {/* Title Section */}
      <View style={styles.titleSection}>
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.subtitle}>{subtitle}</Text>
      </View>

      {/* Description */}
      <Text style={styles.description}>{description}</Text>

      {/* Features */}
      <View style={styles.featuresSection}>
        <Text style={styles.featuresLabel}>Key Benefits</Text>
        {features.map((feature, index) => (
          <View key={index} style={styles.featureItem}>
            <View style={styles.featureDot} />
            <Text style={styles.featureText}>{feature}</Text>
          </View>
        ))}
      </View>

      {/* Action Button */}
      <TouchableOpacity 
        style={[styles.actionButton, isRecommended && styles.actionButtonRecommended]}
        onPress={onAction}
        activeOpacity={0.8}
      >
        <Text style={[styles.actionButtonText, isRecommended && styles.actionButtonTextRecommended]}>
          {actionLabel}
        </Text>
        <Ionicons 
          name="arrow-forward" 
          size={18} 
          color={isRecommended ? '#FFFFFF' : Colors.accent} 
          style={styles.actionIcon}
        />
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.surface,
    borderRadius: 24,
    padding: 28,
    marginHorizontal: 20,
    marginVertical: 12,
    borderWidth: 1,
    borderColor: Colors.surfaceLight,
  },
  containerRecommended: {
    borderColor: Colors.accent,
    borderWidth: 2,
    backgroundColor: Colors.surface,
    shadowColor: Colors.accent,
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.15,
    shadowRadius: 24,
    elevation: 8,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  numberBadge: {
    width: 44,
    height: 44,
    borderRadius: 14,
    backgroundColor: Colors.accent + '15',
    justifyContent: 'center',
    alignItems: 'center',
  },
  numberText: {
    fontSize: 18,
    fontWeight: '700',
    color: Colors.accent,
    letterSpacing: -0.5,
  },
  recommendedBadge: {
    backgroundColor: Colors.accent,
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 20,
  },
  recommendedText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#FFFFFF',
    letterSpacing: 0.3,
  },
  titleSection: {
    marginBottom: 16,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: Colors.text,
    marginBottom: 6,
    letterSpacing: -0.5,
  },
  subtitle: {
    fontSize: 15,
    color: Colors.accent,
    fontWeight: '500',
    letterSpacing: 0.3,
    textTransform: 'uppercase',
  },
  description: {
    fontSize: 15,
    color: Colors.textSecondary,
    lineHeight: 24,
    marginBottom: 24,
  },
  featuresSection: {
    marginBottom: 28,
    paddingTop: 20,
    borderTopWidth: 1,
    borderTopColor: Colors.surfaceLight,
  },
  featuresLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: Colors.textMuted,
    marginBottom: 16,
    letterSpacing: 0.5,
    textTransform: 'uppercase',
  },
  featureItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  featureDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: Colors.accent,
    marginRight: 12,
  },
  featureText: {
    fontSize: 14,
    color: Colors.text,
    fontWeight: '500',
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.accent + '12',
    paddingVertical: 16,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: Colors.accent + '30',
  },
  actionButtonRecommended: {
    backgroundColor: Colors.accent,
    borderColor: Colors.accent,
  },
  actionButtonText: {
    fontSize: 15,
    fontWeight: '600',
    color: Colors.accent,
    letterSpacing: 0.3,
  },
  actionButtonTextRecommended: {
    color: '#FFFFFF',
  },
  actionIcon: {
    marginLeft: 8,
  },
});
