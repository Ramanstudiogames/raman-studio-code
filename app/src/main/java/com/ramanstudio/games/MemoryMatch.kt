package com.ramanstudio.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class MemoryMode { VS_COMPUTER, VS_HUMAN }
enum class MemoryDifficulty(val label: String) { EASY("Easy"), MEDIUM("Medium"), HARD("Hard") }

enum class GridSize(val label: String, val rows: Int, val cols: Int, val pairs: Int) {
    SIZE_4X4("4x4 (16 Cards)", 4, 4, 8),
    SIZE_4X5("4x5 (20 Cards)", 5, 4, 10),
    SIZE_4X6("4x6 (24 Cards)", 6, 4, 12)
}

data class MemoryCard(
    val id: Int,
    val symbol: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

val SYMBOL_POOL = listOf(
    "🚀", "🛸", "🤖", "👾", "⭐", "🌙", "🔥", "⚡",
    "🍕", "🍔", "⚽", "🏀", "🎨", "🎸", "🦁", "🦄",
    "🌈", "💎", "🎮", "👑", "🍕", "🚗", "🏆", "🎯"
)

@Composable
fun MemoryMatchScreen(onBack: () -> Unit) {
    var gameMode by remember { mutableStateOf(MemoryMode.VS_COMPUTER) }
    var difficulty by remember { mutableStateOf(MemoryDifficulty.MEDIUM) }
    var gridSize by remember { mutableStateOf(GridSize.SIZE_4X4) }

    var cards by remember(gridSize) { mutableStateOf(createShuffledDeck(gridSize.pairs)) }
    var isChecking by remember { mutableStateOf(false) }

    var currentPlayer by remember { mutableIntStateOf(1) } // 1 = Human/P1, 2 = Computer/P2
    var scoreP1 by remember { mutableIntStateOf(0) }
    var scoreP2 by remember { mutableIntStateOf(0) }

    // Memory AI Knowledge Base: maps card index to symbol
    val aiMemory = remember { mutableStateMapOf<Int, String>() }

    fun resetGame() {
        cards = createShuffledDeck(gridSize.pairs)
        isChecking = false
        currentPlayer = 1
        scoreP1 = 0
        scoreP2 = 0
        aiMemory.clear()
    }

    // Match / Flip Back Logic Handler
    LaunchedEffect(cards) {
        val faceUpUnmatched = cards.mapIndexedNotNull { index, card ->
            if (card.isFaceUp && !card.isMatched) Pair(index, card) else null
        }

        if (faceUpUnmatched.size == 2) {
            isChecking = true

            // Record observed cards in AI memory
            faceUpUnmatched.forEach { (idx, card) ->
                aiMemory[idx] = card.symbol
            }

            delay(750L) // Wait to observe cards

            val (idx1, card1) = faceUpUnmatched[0]
            val (idx2, card2) = faceUpUnmatched[1]

            val newCards = cards.toMutableList()
            if (card1.symbol == card2.symbol) {
                // Match Found!
                newCards[idx1] = card1.copy(isMatched = true)
                newCards[idx2] = card2.copy(isMatched = true)
                if (currentPlayer == 1) scoreP1++ else scoreP2++
                // Current player gets ANOTHER turn for matching!
            } else {
                // No match -> flip back and switch turn
                newCards[idx1] = card1.copy(isFaceUp = false)
                newCards[idx2] = card2.copy(isFaceUp = false)
                currentPlayer = if (currentPlayer == 1) 2 else 1
            }

            cards = newCards.toList()
            isChecking = false
        }
    }

    // Computer AI Turn Effect
    LaunchedEffect(currentPlayer, isChecking, gameMode) {
        if (gameMode == MemoryMode.VS_COMPUTER && currentPlayer == 2 && !isChecking) {
            val allMatched = cards.all { it.isMatched }
            if (allMatched) return@LaunchedEffect

            delay(600L) // AI thinking time

            val unrevealed = cards.mapIndexedNotNull { index, card ->
                if (!card.isFaceUp && !card.isMatched) index else null
            }

            if (unrevealed.isEmpty()) return@LaunchedEffect

            // AI Decision Making
            var firstPick: Int? = null
            var secondPick: Int? = null

            if (difficulty != MemoryDifficulty.EASY) {
                // Check if AI remembers any matching pair among unrevealed cards
                val rememberedUnrevealed = aiMemory.filter { (idx, _) ->
                    !cards[idx].isFaceUp && !cards[idx].isMatched
                }

                val symbolGroups = rememberedUnrevealed.entries.groupBy { it.value }
                val knownPair = symbolGroups.values.firstOrNull { it.size >= 2 }

                if (knownPair != null && (difficulty == MemoryDifficulty.HARD || Math.random() < 0.65)) {
                    firstPick = knownPair[0].key
                    secondPick = knownPair[1].key
                }
            }

            if (firstPick == null) {
                firstPick = unrevealed.random()
            }

            // Flip 1st card
            val newCards1 = cards.toMutableList()
            newCards1[firstPick] = newCards1[firstPick].copy(isFaceUp = true)
            cards = newCards1.toList()
            aiMemory[firstPick] = cards[firstPick].symbol

            delay(600L)

            // Pick 2nd card
            if (secondPick == null) {
                val symbol1 = cards[firstPick].symbol
                val knownMatchIdx = aiMemory.entries.firstOrNull { (idx, sym) ->
                    idx != firstPick && sym == symbol1 && !cards[idx].isMatched && !cards[idx].isFaceUp
                }?.key

                if (knownMatchIdx != null && (difficulty == MemoryDifficulty.HARD || Math.random() < 0.7)) {
                    secondPick = knownMatchIdx
                } else {
                    val remainingUnrevealed = unrevealed.filter { it != firstPick }
                    if (remainingUnrevealed.isNotEmpty()) {
                        secondPick = remainingUnrevealed.random()
                    }
                }
            }

            if (secondPick != null) {
                val newCards2 = cards.toMutableList()
                newCards2[secondPick] = newCards2[secondPick].copy(isFaceUp = true)
                cards = newCards2.toList()
                aiMemory[secondPick] = cards[secondPick].symbol
            }
        }
    }

    fun onCardClick(index: Int) {
        if (isChecking || cards[index].isFaceUp || cards[index].isMatched) return
        if (gameMode == MemoryMode.VS_COMPUTER && currentPlayer == 2) return // Block during AI turn

        val newCards = cards.toMutableList()
        newCards[index] = newCards[index].copy(isFaceUp = true)
        cards = newCards.toList()
    }

    val isWon = cards.all { it.isMatched }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("← Menu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }

            Text("Memory Match", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFC107))

            Button(
                onClick = { resetGame() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("↺ New", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }

        // Mode Switcher (VS Computer / Pass & Play)
        Row(
            modifier = Modifier
                .background(Color(0xFF2A2826), shape = RoundedCornerShape(20.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { gameMode = MemoryMode.VS_COMPUTER; resetGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == MemoryMode.VS_COMPUTER) Color(0xFFFFC107) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("🤖 vs 👤 Computer", color = if (gameMode == MemoryMode.VS_COMPUTER) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }

            Button(
                onClick = { gameMode = MemoryMode.VS_HUMAN; resetGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == MemoryMode.VS_HUMAN) Color(0xFFFFC107) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("👥 Pass & Play", color = if (gameMode == MemoryMode.VS_HUMAN) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }

        // Options Row (Grid Size & AI Difficulty)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grid Size Chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                GridSize.entries.forEach { sz ->
                    FilterChip(
                        selected = gridSize == sz,
                        onClick = { gridSize = sz; resetGame() },
                        label = { Text("${sz.rows}x${sz.cols}", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFC107),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF2A2826),
                            labelColor = Color.White
                        )
                    )
                }
            }

            // AI Difficulty Chips
            AnimatedVisibility(visible = gameMode == MemoryMode.VS_COMPUTER) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MemoryDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff; resetGame() },
                            label = { Text(diff.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E5FF),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF2A2826),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scoreboard Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreBadgeCard(
                title = "Player 1 (You)",
                score = scoreP1,
                isActive = currentPlayer == 1,
                color = Color(0xFF00E5FF)
            )
            ScoreBadgeCard(
                title = if (gameMode == MemoryMode.VS_COMPUTER) "AI (${difficulty.label})" else "Player 2",
                score = scoreP2,
                isActive = currentPlayer == 2,
                color = Color(0xFFFF4081)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Message Banner
        val statusText = when {
            isWon -> if (scoreP1 > scoreP2) "🎉 Player 1 Wins!" else if (scoreP2 > scoreP1) "🤖 Computer Wins!" else "🤝 It's a Tie!"
            else -> if (gameMode == MemoryMode.VS_COMPUTER && currentPlayer == 2) "🤖 Computer is thinking..." else "Player $currentPlayer's Turn"
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = statusText,
                color = if (isWon) Color(0xFFFFD700) else if (currentPlayer == 1) Color(0xFF00E5FF) else Color(0xFFFF4081),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card Grid Board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1A1A1A), shape = RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF333333), shape = RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridSize.cols),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(cards.size) { index ->
                    val card = cards[index]
                    Flip3DMemoryCard(
                        card = card,
                        onClick = { onCardClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun Flip3DMemoryCard(card: MemoryCard, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFaceUp || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 380),
        label = "CardFlip"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (card.isMatched) Color(0xFF2E7D32)
            else if (card.isFaceUp) Color(0xFF333333)
            else Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .border(
                width = if (card.isMatched) 2.dp else 1.dp,
                color = if (card.isMatched) Color(0xFFFFC107) else Color(0xFF333333),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (rotation > 90f) {
                // Card Face (Icon / Photo Symbol)
                Text(
                    text = card.symbol,
                    fontSize = 32.sp,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                )
            } else {
                // Card Back Pattern
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF262626), shape = RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFFC107), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎴",
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreBadgeCard(title: String, score: Int, isActive: Boolean, color: Color) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) color.copy(alpha = 0.2f) else Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(
            width = if (isActive) 1.5.dp else 0.dp,
            color = if (isActive) color else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(text = title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(text = "$score Pairs", fontSize = 15.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

fun createShuffledDeck(pairCount: Int): List<MemoryCard> {
    val selectedSymbols = SYMBOL_POOL.shuffled().take(pairCount)
    val deck = (selectedSymbols + selectedSymbols).shuffled()
    return deck.mapIndexed { index, symbol ->
        MemoryCard(id = index, symbol = symbol)
    }
}