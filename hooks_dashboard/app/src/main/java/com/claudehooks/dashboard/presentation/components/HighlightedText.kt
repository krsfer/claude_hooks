package com.claudehooks.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.util.*

/**
 * Text component that highlights search matches
 */
@Composable
fun HighlightedText(
    text: String,
    searchQuery: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    highlightBackground: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    modifier: Modifier = Modifier
) {
    if (searchQuery.isBlank()) {
        Text(
            text = text,
            style = style,
            modifier = modifier
        )
    } else {
        Text(
            text = buildHighlightedString(text, searchQuery, highlightColor, highlightBackground),
            style = style,
            modifier = modifier
        )
    }
}

/**
 * Build annotated string with highlighted search matches
 */
private fun buildHighlightedString(
    text: String,
    searchQuery: String,
    highlightColor: Color,
    highlightBackground: Color
): AnnotatedString {
    return buildAnnotatedString {
        val searchTerms = searchQuery.split(" ").filter { it.isNotBlank() }
        var currentText = text
        var currentIndex = 0
        
        // Find all matches for all search terms
        val matches = mutableListOf<Triple<Int, Int, String>>() // start, end, term
        
        searchTerms.forEach { term ->
            var startIndex = 0
            while (startIndex < currentText.length) {
                val index = currentText.indexOf(term, startIndex, ignoreCase = true)
                if (index != -1) {
                    matches.add(Triple(index, index + term.length, term))
                    startIndex = index + term.length
                } else {
                    break
                }
            }
        }
        
        // Sort matches by start position
        matches.sortBy { it.first }
        
        // Merge overlapping matches
        val mergedMatches = mutableListOf<Pair<Int, Int>>()
        matches.forEach { (start, end, _) ->
            if (mergedMatches.isEmpty()) {
                mergedMatches.add(start to end)
            } else {
                val last = mergedMatches.last()
                if (start <= last.second) {
                    // Overlapping - merge
                    mergedMatches[mergedMatches.size - 1] = last.first to maxOf(last.second, end)
                } else {
                    mergedMatches.add(start to end)
                }
            }
        }
        
        // Build the annotated string
        var lastEnd = 0
        mergedMatches.forEach { (start, end) ->
            // Add text before match
            if (start > lastEnd) {
                append(currentText.substring(lastEnd, start))
            }
            
            // Add highlighted match
            withStyle(
                SpanStyle(
                    color = highlightColor,
                    background = highlightBackground,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(currentText.substring(start, end))
            }
            
            lastEnd = end
        }
        
        // Add remaining text
        if (lastEnd < currentText.length) {
            append(currentText.substring(lastEnd))
        }
    }
}

/**
 * Check if text contains search query (case-insensitive)
 */
fun matchesSearchQuery(text: String, searchQuery: String): Boolean {
    if (searchQuery.isBlank()) return true
    
    val searchTerms = searchQuery.split(" ").filter { it.isNotBlank() }
    return searchTerms.all { term ->
        text.contains(term, ignoreCase = true)
    }
}

/**
 * Calculate search relevance score (0-1)
 */
fun calculateSearchRelevance(text: String, searchQuery: String): Float {
    if (searchQuery.isBlank()) return 0f
    
    val searchTerms = searchQuery.split(" ").filter { it.isNotBlank() }
    val textLower = text.lowercase()
    var score = 0f
    var maxScore = 0f
    
    searchTerms.forEach { term ->
        val termLower = term.lowercase()
        maxScore += 1f
        
        when {
            // Exact phrase match gets highest score
            textLower.contains(searchQuery.lowercase()) -> score += 1f
            // Individual term matches
            textLower.contains(termLower) -> {
                // Bonus for word boundary matches
                val wordBoundaryRegex = "\\b${Regex.escape(termLower)}\\b".toRegex()
                if (wordBoundaryRegex.containsMatchIn(textLower)) {
                    score += 0.8f
                } else {
                    score += 0.5f
                }
            }
            // Partial matches
            termLower.length > 2 && textLower.contains(termLower.substring(0, termLower.length - 1)) -> {
                score += 0.3f
            }
        }
    }
    
    return if (maxScore > 0) score / maxScore else 0f
}