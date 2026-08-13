package com.apptimemachine.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Single source of truth for how a category name maps to an icon + color.
 * Backed by [com.apptimemachine.core.monitoring.PackageInfoReader.categoryName],
 * which is itself a direct mapping of Android's ApplicationInfo.category
 * constants — so every category that can ever land in the database has an
 * entry here. Anything unmapped (there shouldn't be any) falls back to a
 * neutral Apps glyph via [categoryStyle].
 *
 * Shared by the Dashboard "Top Categories" panel and the Apps tab's
 * category filter so both always agree on how a category looks.
 */
val categoryIcons: Map<String, Pair<ImageVector, Color>> = mapOf(
    "Game" to (Icons.Default.SportsEsports to Color(0xFFAD1457)),
    "Audio" to (Icons.Default.MusicNote to Color(0xFF6A1B9A)),
    "Video" to (Icons.Default.Videocam to Color(0xFFE65100)),
    "Image" to (Icons.Default.Image to Color(0xFF00838F)),
    "Social" to (Icons.Default.Groups to Color(0xFF6A1B9A)),
    "News" to (Icons.Default.Article to Color(0xFF1565C0)),
    "Maps" to (Icons.Default.Map to Color(0xFF2E7D32)),
    "Productivity" to (Icons.Default.Work to Color(0xFF1565C0)),
    "Others" to (Icons.Default.MoreHoriz to Color(0xFF757575)),
    "Uncategorized" to (Icons.Default.Apps to Color(0xFF757575))
)

/** Convenience accessor with a sensible fallback for any category not in [categoryIcons]. */
fun categoryStyle(category: String): Pair<ImageVector, Color> =
    categoryIcons[category] ?: (Icons.Default.Apps to Color(0xFF757575))

/** Sentinel label used across the Apps tab to mean "show every category". */
const val CATEGORY_ALL = "All"
