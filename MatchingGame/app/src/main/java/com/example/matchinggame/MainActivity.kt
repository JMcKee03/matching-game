package com.example.matchinggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MatchingGameApp() }
    }
}

@Composable
fun MatchingGameApp() {
    val game = remember { MatchingGameState() }
    game.FlipBackEffect()

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Matching Game") },
                    actions = {
                        TextButton(onClick = { game.reset() }) {
                            Text("Restart")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Moves: ${game.moves}", fontWeight = FontWeight.Bold)
                    Text(
                        "Matches: ${game.matchesFound}/${game.totalPairs}",
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(game.cards, key = { it.id }) { card ->
                        MemoryCard(card) { game.onCardTapped(card.id) }
                    }
                }
            }

            if (game.isWin) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("You Win!") },
                    text = { Text("You matched all pairs in ${game.moves} moves.") },
                    confirmButton = {
                        Button(onClick = { game.reset() }) {
                            Text("Play Again")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MemoryCard(card: CardModel, onTap: () -> Unit) {
    val color = when {
        card.isMatched -> Color(0xFFB9F6CA)
        card.isFaceUp -> Color(0xFFBBDEFB)
        else -> Color(0xFFE0E0E0)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(color, RoundedCornerShape(14.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(14.dp))
            .clickable(enabled = !card.isMatched) { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (card.isFaceUp || card.isMatched) card.symbol else "?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ---------------- GAME LOGIC ---------------- */

data class CardModel(
    val id: Int,
    val symbol: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

class MatchingGameState {

    var cards by mutableStateOf(emptyList<CardModel>())
    var moves by mutableIntStateOf(0)
    var matchesFound by mutableIntStateOf(0)
    var lockBoard by mutableStateOf(false)

    val totalPairs = 8
    val isWin get() = matchesFound == totalPairs

    private var firstId: Int? = null
    private var secondId: Int? = null
    private var pendingFlipBack by mutableStateOf<Pair<Int, Int>?>(null)

    init {
        reset()
    }

    fun reset() {
        cards = buildDeck()
        moves = 0
        matchesFound = 0
        lockBoard = false
        firstId = null
        secondId = null
    }

    fun onCardTapped(id: Int) {
        if (lockBoard) return

        val card = cards.first { it.id == id }
        if (card.isFaceUp || card.isMatched) return

        cards = cards.map { if (it.id == id) it.copy(isFaceUp = true) else it }

        if (firstId == null) {
            firstId = id
            return
        }

        secondId = id
        moves++

        val first = cards.first { it.id == firstId }
        val second = cards.first { it.id == secondId }

        if (first.symbol == second.symbol) {
            cards = cards.map {
                if (it.id == first.id || it.id == second.id)
                    it.copy(isMatched = true)
                else it
            }
            matchesFound++
            clearSelection()
        } else {
            lockBoard = true
            pendingFlipBack = first.id to second.id
        }
    }

    private fun clearSelection() {
        firstId = null
        secondId = null
    }

    @Composable
    fun FlipBackEffect() {
        val pair = pendingFlipBack
        LaunchedEffect(pair) {
            if (pair != null) {
                delay(700)
                cards = cards.map {
                    if (it.id == pair.first || it.id == pair.second)
                        it.copy(isFaceUp = false)
                    else it
                }
                pendingFlipBack = null
                lockBoard = false
                clearSelection()
            }
        }
    }
}

fun buildDeck(): List<CardModel> {
    val symbols = listOf("🍎", "🚀", "🎮", "🐶", "⚽", "🎵", "🌙", "🔥")
    val deck = (symbols + symbols).shuffled()
    return deck.mapIndexed { index, symbol ->
        CardModel(index, symbol)
    }
}
