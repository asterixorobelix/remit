package com.example.myproject.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized dimension values for consistent spacing throughout the app.
 *
 * Following Material Design 3 spacing guidelines:
 * - 4dp increments for fine-grained control
 * - Standard padding: 8dp (small), 16dp (medium), 24dp (large)
 * - Touch targets: minimum 48dp for accessibility
 *
 * Usage:
 * ```kotlin
 * Modifier.padding(Dimens.paddingMedium)
 * Spacer(Modifier.height(Dimens.spacingSmall))
 * ```
 */
object Dimens {
    // ===========================================
    // Padding - Used for content insets
    // ===========================================

    /** 4.dp - Extra small padding for tight spaces */
    val paddingExtraSmall: Dp = 4.dp

    /** 8.dp - Small padding for compact layouts */
    val paddingSmall: Dp = 8.dp

    /** 12.dp - Compact padding between elements */
    val paddingCompact: Dp = 12.dp

    /** 16.dp - Standard/medium padding for most content */
    val paddingMedium: Dp = 16.dp

    /** 20.dp - Screen horizontal padding (editorial feel, NOT 16dp) */
    val screenHorizontalPadding: Dp = 20.dp

    /** 20.dp - Semi-large padding */
    val paddingSemiLarge: Dp = 20.dp

    /** 24.dp - Large padding for section separation */
    val paddingLarge: Dp = 24.dp

    /** 32.dp - Extra large padding for major sections */
    val paddingExtraLarge: Dp = 32.dp

    /** 48.dp - Screen-level padding for spacious layouts */
    val paddingScreen: Dp = 48.dp

    // ===========================================
    // Spacing - Used between elements
    // ===========================================

    /** 2.dp - Minimal spacing */
    val spacingMinimal: Dp = 2.dp

    /** 4.dp - Extra small spacing between tightly grouped items */
    val spacingExtraSmall: Dp = 4.dp

    /** 8.dp - Small spacing for related items */
    val spacingSmall: Dp = 8.dp

    /** 12.dp - Compact spacing */
    val spacingCompact: Dp = 12.dp

    /** 16.dp - Standard spacing between sections */
    val spacingMedium: Dp = 16.dp

    /** 24.dp - Large spacing for visual separation */
    val spacingLarge: Dp = 24.dp

    /** 32.dp - Extra large spacing between major sections */
    val spacingExtraLarge: Dp = 32.dp

    /** 48.dp - Section-level spacing */
    val spacingSection: Dp = 48.dp

    // ===========================================
    // Corner Radius - For rounded shapes
    // ===========================================

    /** 2.dp - Extra small corner radius */
    val cornerRadiusExtraSmall: Dp = 2.dp

    /** 4.dp - Small corner radius for subtle rounding */
    val cornerRadiusSmall: Dp = 4.dp

    /** 8.dp - Medium corner radius for cards */
    val cornerRadiusMedium: Dp = 8.dp

    /** 12.dp - Semi-large corner radius */
    val cornerRadiusSemiLarge: Dp = 12.dp

    /** 16.dp - Large corner radius for prominent elements */
    val cornerRadiusLarge: Dp = 16.dp

    /** 24.dp - Extra large corner radius */
    val cornerRadiusExtraLarge: Dp = 24.dp

    /** 28.dp - Full/pill corner radius */
    val cornerRadiusFull: Dp = 28.dp

    // ===========================================
    // Icon Sizes - Following Material guidelines
    // ===========================================

    /** 16.dp - Small inline icons */
    val iconSizeSmall: Dp = 16.dp

    /** 20.dp - Compact icon size */
    val iconSizeCompact: Dp = 20.dp

    /** 24.dp - Standard icon size (Material default) */
    val iconSizeMedium: Dp = 24.dp

    /** 32.dp - Large icons for emphasis */
    val iconSizeLarge: Dp = 32.dp

    /** 40.dp - Extra large icons for avatars/thumbnails */
    val iconSizeExtraLarge: Dp = 40.dp

    /** 48.dp - Hero icons or avatar size */
    val iconSizeHero: Dp = 48.dp

    /** 64.dp - Large display icons for empty states */
    val iconSizeDisplay: Dp = 64.dp

    /** 96.dp - Extra large display for prominent empty states */
    val iconSizeDisplayLarge: Dp = 96.dp

    // ===========================================
    // Touch Targets - Accessibility minimum sizes
    // ===========================================

    /** 44.dp - iOS minimum touch target */
    val touchTargetMinimumIos: Dp = 44.dp

    /** 48.dp - Android/Material minimum touch target */
    val touchTargetMinimum: Dp = 48.dp

    /** 56.dp - Comfortable touch target */
    val touchTargetComfortable: Dp = 56.dp

    // ===========================================
    // Component Sizes - Specific UI elements
    // ===========================================

    /** 40.dp - Compact avatar size */
    val avatarSizeSmall: Dp = 40.dp

    /** 48.dp - Standard avatar size */
    val avatarSizeMedium: Dp = 48.dp

    /** 64.dp - Large avatar size */
    val avatarSizeLarge: Dp = 64.dp

    /** 96.dp - Profile header avatar */
    val avatarSizeProfile: Dp = 96.dp

    /** 56.dp - Standard FAB size */
    val fabSize: Dp = 56.dp

    /** 40.dp - Small FAB size */
    val fabSizeSmall: Dp = 40.dp

    /** 96.dp - Extended FAB height */
    val fabSizeExtended: Dp = 96.dp

    /** 64.dp - App bar height */
    val appBarHeight: Dp = 64.dp

    /** 80.dp - Large top app bar height */
    val appBarHeightLarge: Dp = 80.dp

    /** 80.dp - Bottom navigation bar height */
    val bottomNavHeight: Dp = 80.dp

    /** 200.dp - Dropdown menu max height */
    val dropdownMaxHeight: Dp = 200.dp

    /** 52.dp - Primary button height (taller than MD3 default 40dp) */
    val buttonHeight: Dp = 52.dp

    /** 24.dp - Bottom sheet top corner radius */
    val bottomSheetCornerRadius: Dp = 24.dp

    /** 12.dp - List item vertical padding (tighter than MD3 default 16dp) */
    val listItemVerticalPadding: Dp = 12.dp

    /** 280.dp - Dialog minimum width */
    val dialogMinWidth: Dp = 280.dp

    /** 560.dp - Dialog maximum width */
    val dialogMaxWidth: Dp = 560.dp

    // ===========================================
    // Elevation - Shadow/tonal elevation values
    // ===========================================

    /** 0.dp - No elevation */
    val elevationNone: Dp = 0.dp

    /** 1.dp - Subtle elevation */
    val elevationLevel1: Dp = 1.dp

    /** 2.dp - Low elevation for cards */
    val elevationLevel2: Dp = 2.dp

    /** 3.dp - Standard elevation */
    val elevationLevel3: Dp = 3.dp

    /** 6.dp - Medium elevation for menus */
    val elevationLevel4: Dp = 6.dp

    /** 8.dp - High elevation for dialogs */
    val elevationLevel5: Dp = 8.dp

    // ===========================================
    // Divider - Line thickness
    // ===========================================

    /** 0.5.dp - Hairline divider */
    val dividerThicknessHairline: Dp = 0.5.dp

    /** 1.dp - Standard divider */
    val dividerThickness: Dp = 1.dp

    /** 2.dp - Thick divider */
    val dividerThicknessThick: Dp = 2.dp

    // ===========================================
    // Content Width - Layout constraints
    // ===========================================

    /** 600.dp - Compact width for tablet/desktop centering */
    val contentWidthCompact: Dp = 600.dp

    /** 840.dp - Medium content width */
    val contentWidthMedium: Dp = 840.dp

    /** 1200.dp - Maximum content width */
    val contentWidthExpanded: Dp = 1200.dp

    // ===========================================
    // Border - Stroke widths
    // ===========================================

    /** 1.dp - Standard border width */
    val borderWidth: Dp = 1.dp

    /** 2.dp - Thick border for emphasis */
    val borderWidthThick: Dp = 2.dp

    /** 3.dp - Extra thick border */
    val borderWidthExtraThick: Dp = 3.dp

    // ===========================================
    // Image Sizes - Standard image dimensions
    // ===========================================

    /** 120.dp - Thumbnail image size */
    val imageSizeThumbnail: Dp = 120.dp

    /** 200.dp - Medium image size */
    val imageSizeMedium: Dp = 200.dp

    /** 300.dp - Large image size */
    val imageSizeLarge: Dp = 300.dp

    // ===========================================
    // Animation Durations - Milliseconds (Int)
    // Use with tween() or delay()
    // ===========================================

    /** 150ms - Short micro-interaction (color/scale snaps) */
    const val ANIMATION_DURATION_SHORT: Int = 150

    /** 300ms - Standard transition duration */
    const val ANIMATION_DURATION_MEDIUM: Int = 300

    /** 450ms - Longer emphasis transitions */
    const val ANIMATION_DURATION_LONG: Int = 450

    /** 50ms - Stagger delay per list item entrance */
    const val STAGGER_DELAY_PER_ITEM: Int = 50

    // ===========================================
    // Skeleton / Shimmer Loading
    // ===========================================

    /** 14.dp - Small shimmer line height (body text placeholder) */
    val skeletonLineHeightSmall: Dp = 14.dp

    /** 18.dp - Medium shimmer line height (subtitle placeholder) */
    val skeletonLineHeightMedium: Dp = 18.dp

    /** 24.dp - Large shimmer line height (title placeholder) */
    val skeletonLineHeightLarge: Dp = 24.dp

    /** 28.dp - Skeleton badge height */
    val skeletonBadgeHeight: Dp = 28.dp

    /** 100.dp - Skeleton badge width */
    val skeletonBadgeWidth: Dp = 100.dp

    // ===========================================
    // Page Indicator Dots
    // ===========================================

    /** 8.dp - Unselected dot size */
    val indicatorDotSizeSmall: Dp = 8.dp

    /** 12.dp - Selected dot size */
    val indicatorDotSizeLarge: Dp = 12.dp

    /** 10.dp - Spacing between indicator dots */
    val indicatorSpacing: Dp = 10.dp

    // ===========================================
    // Card Content Layout
    // ===========================================

    /** 20.dp - Horizontal padding inside cards */
    val cardContentPaddingHorizontal: Dp = 20.dp

    /** 24.dp - Vertical padding inside cards */
    val cardContentPaddingVertical: Dp = 24.dp

    /** 4.dp - Left colour indicator bar width on item cards */
    val categoryIndicatorWidth: Dp = 4.dp

    // ===========================================
    // Map
    // ===========================================

    /** 8.dp - Radius of the user-location dot */
    val mapLocationDotRadius: Dp = 8.dp

    /** 20.dp - Radius of the translucent halo around the user-location dot */
    val mapLocationHaloRadius: Dp = 20.dp

    /** 3.dp - Stroke around the user-location dot */
    val mapLocationStrokeWidth: Dp = 3.dp
}
