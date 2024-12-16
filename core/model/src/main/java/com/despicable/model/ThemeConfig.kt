package com.despicable.model

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable


// Data class to hold theme-related state
@Stable
data class ThemePreferences(
    val isDarkTheme: Boolean,
    val colorScheme: ColorScheme,
    override val theme: Theme,
    override val dynamicColor: Boolean
) : ThemeConfig

// Sealed interface for better type safety and extensibility
sealed interface ThemeConfig {
    val theme: Theme
    val dynamicColor: Boolean
}

// Grid layout enum
enum class GridLayout {
    OneColumn, TwoColumns, ThreeColumns
}

// Main settings data class
data class Settings(
    override val theme: Theme,
    override val dynamicColor: Boolean = true,
    val gridLayout: GridLayout = GridLayout.TwoColumns,
) : ThemeConfig

// Theme enum with string resources
enum class Theme {
    System,
    Light,
    Dark,
}