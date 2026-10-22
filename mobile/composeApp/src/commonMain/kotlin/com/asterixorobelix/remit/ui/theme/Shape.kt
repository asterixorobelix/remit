package com.asterixorobelix.remit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Shape system — overrides MD3 defaults with larger corner radii for a modern look.
 *
 * Key differences from stock MD3:
 * - Small (cards): 16dp not 8dp — matches Airbnb/Spotify card style
 * - Medium (dialogs): 20dp not 12dp — floating card feel
 * - Large (sheets): 24dp not 16dp — modern bottom sheet corners
 */
val AppShapes = Shapes(
    // 4dp — chips, tags, small badges
    extraSmall = RoundedCornerShape(Dimens.cornerRadiusSmall),

    // 16dp — cards (NOT 8dp — bigger radius looks more polished)
    small = RoundedCornerShape(Dimens.cornerRadiusLarge),

    // 20dp — floating cards, modals, dialogs
    medium = RoundedCornerShape(Dimens.paddingSemiLarge),

    // 24dp — bottom sheets, navigation drawers
    large = RoundedCornerShape(Dimens.cornerRadiusExtraLarge),

    // 28dp — full-screen sheets, large containers
    extraLarge = RoundedCornerShape(Dimens.cornerRadiusFull),
)
