package com.erp.pda.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.ui.theme.*

/**
 * iOS Settings-style grouped list item
 */
data class IosListItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val iconColor: Color = IosBlue,
    val route: String
)

@Composable
fun IosGroupedList(
    title: String? = null,
    items: List<IosListItem>,
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        if (title != null) {
            item {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
                )
            }
        }

        items(items) { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(item.route) },
                color = IosWhite,
                shape = when {
                    items.size == 1 -> MaterialTheme.shapes.medium
                    item == items.first() -> MaterialTheme.shapes.medium
                    item == items.last() -> MaterialTheme.shapes.medium
                    else -> MaterialTheme.shapes.medium
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // iOS-style icon circle
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = MaterialTheme.shapes.small,
                        color = item.iconColor.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = item.iconColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = IosLabel
                        )
                        if (item.subtitle != null) {
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = IosSecondaryLabel
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = IosGray2,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Divider between items (not after last)
            if (item != items.last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    color = IosGray5
                )
            }
        }

        // Bottom spacing
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
