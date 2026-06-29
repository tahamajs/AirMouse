import re

with open("app/src/main/java/com/airmouse/presentation/ui/help/HelpScreen.kt", "r") as f:
    content = f.read()

# Add new imports
imports_to_add = """import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
"""
content = content.replace("import androidx.compose.animation.*", imports_to_add + "\nimport androidx.compose.animation.*")

# Add HelpHeader
help_header = """
@Composable
fun HelpHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "HeaderIcon")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "HeaderScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Help,
            contentDescription = "Help",
            modifier = Modifier
                .size(64.dp)
                .scale(scale),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "How can we help you?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
"""
content = content + "\n" + help_header

popular_topics = """
@Composable
fun PopularTopicsRow(onTopicClick: (String) -> Unit) {
    val topics = listOf("WiFi Connection", "Calibration", "Gaming Mode", "Shortcuts", "Bluetooth")
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Popular Topics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(topics) { topic ->
                SuggestionChip(
                    onClick = { onTopicClick(topic) },
                    label = { Text(topic) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}
"""
content = content + "\n" + popular_topics

# Add HelpHeader inside HelpScreen composable right after ParticleBackground
particle_bg = "            ParticleBackground(particleCount = 15)"
new_particle_bg = particle_bg + """

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                HelpHeader()"""
original_col = """            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {"""
content = content.replace(particle_bg + "\n\n" + original_col, new_particle_bg)

# Insert PopularTopicsRow
original_spacer_after_filters = """                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.searchQuery.isNotEmpty()"""
new_spacer_after_filters = """                }

                Spacer(modifier = Modifier.height(16.dp))
                PopularTopicsRow(onTopicClick = { viewModel.updateSearchQuery(it) })
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.searchQuery.isNotEmpty()"""
content = content.replace(original_spacer_after_filters, new_spacer_after_filters)

# Update HelpSectionCard steps
old_steps_code = """                            if (step.isBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }"""

new_steps_code = """                            if (step.isBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    val stepNumberRegex = "^\\\\s*(\\\\d+)\\\\.\\\\s+(.*)".toRegex()
                                    val matchResult = stepNumberRegex.find(step)
                                    
                                    if (matchResult != null) {
                                        val (num, rest) = matchResult.destructured
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp, end = 8.dp)
                                                .size(20.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = num, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(
                                            text = rest,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.ArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp).padding(top = 2.dp, end = 4.dp)
                                        )
                                        Text(
                                            text = step.trimStart(' ', '•', '-'),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }"""
content = content.replace(old_steps_code, new_steps_code)

# Update Tips
old_tips_code = """                    if (section.tips.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                contentDescription = "Tips",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "💡 Tips",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        section.tips.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }"""

new_tips_code = """                    if (section.tips.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TipsAndUpdates,
                                        contentDescription = "Tips",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tips",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                section.tips.forEach { tip ->
                                    Row(
                                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("• ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text(
                                            text = tip,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }"""
content = content.replace(old_tips_code, new_tips_code)

# Update Related Topics
old_related_topics_code = """                    if (section.relatedTopics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "🔗 Related Topics",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        section.relatedTopics.forEach { topic ->
                            TextButton(
                                onClick = { onRelatedTopicClick(topic) },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(topic, fontSize = 13.sp)
                            }
                        }
                    }"""

new_related_topics_code = """                    @OptIn(ExperimentalLayoutApi::class)
                    if (section.relatedTopics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Related Topics",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            section.relatedTopics.forEach { topic ->
                                AssistChip(
                                    onClick = { onRelatedTopicClick(topic) },
                                    label = { Text(topic, fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Link,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }"""
content = content.replace(old_related_topics_code, new_related_topics_code)

# Update getCategoryColor
old_colors_code = """        HelpCategory.ACCESSIBILITY -> Color(0xFFE91E63)
        HelpCategory.FAQ -> Color(0xFF607D8B)
        else -> MaterialTheme.colorScheme.primary"""

new_colors_code = """        HelpCategory.ACCESSIBILITY -> Color(0xFFE91E63)
        HelpCategory.FAQ -> Color(0xFF607D8B)
        HelpCategory.GAMING -> Color(0xFF673AB7)
        HelpCategory.SCREEN_MIRRORING -> Color(0xFF009688)
        HelpCategory.FILE_TRANSFER -> Color(0xFFFF5722)
        HelpCategory.VOICE_COMMANDS -> Color(0xFFE91E63)
        HelpCategory.THEMES -> Color(0xFF9C27B0)
        HelpCategory.NOTIFICATIONS -> Color(0xFFFFC107)
        HelpCategory.BATTERY -> Color(0xFF4CAF50)
        HelpCategory.PROFILES -> Color(0xFF3F51B5)
        HelpCategory.SERVER_SETUP -> Color(0xFF607D8B)
        else -> MaterialTheme.colorScheme.primary"""
content = content.replace(old_colors_code, new_colors_code)

with open("app/src/main/java/com/airmouse/presentation/ui/help/HelpScreen.kt", "w") as f:
    f.write(content)
