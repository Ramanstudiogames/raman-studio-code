package com.ramanstudio.games.games.game2048

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramanstudio.games.HowToPlayDialog
import kotlin.math.abs

enum class MoveDirection { LEFT, RIGHT, UP, DOWN }

data class Game2048State(
    val grid: List<List<Int>> = List(4) { List(4) { 0 } },
    val score: Int = 0,
    val highScore: Int = 0,
    val isGameOver: Boolean = false,
    val isGameWon: Boolean = false,
    val keepPlaying: Boolean = false,
    val previousState: Game2048State? = null
)

class Game2048Logic {
    companion object {
        fun createInitialState(highScore: Int = 0): Game2048State {
            val emptyGrid = List(4) { MutableList(4) { 0 } }
            spawnRandomTile(emptyGrid)
            spawnRandomTile(emptyGrid)
            val immutableGrid = emptyGrid.map { it.toList() }
            return Game2048State(
                grid = immutableGrid,
                score = 0,
                highScore = highScore,
                isGameOver = false,
                isGameWon = false,
                keepPlaying = false,
                previousState = null
            )
        }

        private fun spawnRandomTile(grid: List<MutableList<Int>>) {
            val emptyCells = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until 4) {
                for (c in 0 until 4) {
                    if (grid[r][c] == 0) {
                        emptyCells.add(Pair(r, c))
                    }
                }
            }
            if (emptyCells.isNotEmpty()) {
                val (r, c) = emptyCells.random()
                grid[r][c] = if (Math.random() < 0.9) 2 else 4
            }
        }

        fun move(currentState: Game2048State, direction: MoveDirection): Game2048State {
            if (currentState.isGameOver) return currentState

            val currentGrid = currentState.grid
            val newGrid = List(4) { MutableList(4) { 0 } }
            var pointsGained = 0

            when (direction) {
                MoveDirection.LEFT -> {
                    for (r in 0 until 4) {
                        val line = currentGrid[r]
                        val (merged, pts) = slideAndMerge(line)
                        pointsGained += pts
                        for (c in 0 until 4) {
                            newGrid[r][c] = merged[c]
                        }
                    }
                }
                MoveDirection.RIGHT -> {
                    for (r in 0 until 4) {
                        val line = currentGrid[r].reversed()
                        val (merged, pts) = slideAndMerge(line)
                        pointsGained += pts
                        val reversedBack = merged.reversed()
                        for (c in 0 until 4) {
                            newGrid[r][c] = reversedBack[c]
                        }
                    }
                }
                MoveDirection.UP -> {
                    for (c in 0 until 4) {
                        val line = listOf(currentGrid[0][c], currentGrid[1][c], currentGrid[2][c], currentGrid[3][c])
                        val (merged, pts) = slideAndMerge(line)
                        pointsGained += pts
                        for (r in 0 until 4) {
                            newGrid[r][c] = merged[r]
                        }
                    }
                }
                MoveDirection.DOWN -> {
                    for (c in 0 until 4) {
                        val line = listOf(currentGrid[3][c], currentGrid[2][c], currentGrid[1][c], currentGrid[0][c])
                        val (merged, pts) = slideAndMerge(line)
                        pointsGained += pts
                        val reversedBack = merged.reversed()
                        for (r in 0 until 4) {
                            newGrid[r][c] = reversedBack[r]
                        }
                    }
                }
            }

            val immutableNewGrid = newGrid.map { it.toList() }
            if (immutableNewGrid == currentGrid) {
                return currentState
            }

            spawnRandomTile(newGrid)
            val finalGrid = newGrid.map { it.toList() }

            val newScore = currentState.score + pointsGained
            val newHighScore = maxOf(newScore, currentState.highScore)

            val has2048 = finalGrid.any { row -> row.any { it == 2048 } }
            val isWon = has2048 && !currentState.keepPlaying && !currentState.isGameWon

            val isGameOver = checkGameOver(finalGrid)

            return Game2048State(
                grid = finalGrid,
                score = newScore,
                highScore = newHighScore,
                isGameOver = isGameOver,
                isGameWon = isWon,
                keepPlaying = currentState.keepPlaying,
                previousState = currentState.copy(previousState = null)
            )
        }

        private fun slideAndMerge(line: List<Int>): Pair<List<Int>, Int> {
            val nonZeros = line.filter { it != 0 }
            val result = mutableListOf<Int>()
            var points = 0
            var i = 0
            while (i < nonZeros.size) {
                if (i + 1 < nonZeros.size && nonZeros[i] == nonZeros[i + 1]) {
                    val mergedValue = nonZeros[i] * 2
                    result.add(mergedValue)
                    points += mergedValue
                    i += 2
                } else {
                    result.add(nonZeros[i])
                    i++
                }
            }
            while (result.size < 4) {
                result.add(0)
            }
            return Pair(result, points)
        }

        private fun checkGameOver(grid: List<List<Int>>): Boolean {
            for (r in 0 until 4) {
                for (c in 0 until 4) {
                    if (grid[r][c] == 0) return false
                }
            }
            for (r in 0 until 4) {
                for (c in 0 until 3) {
                    if (grid[r][c] == grid[r][c + 1]) return false
                }
            }
            for (r in 0 until 3) {
                for (c in 0 until 4) {
                    if (grid[r][c] == grid[r + 1][c]) return false
                }
            }
            return true
        }
    }
}

@Composable
fun Game2048Screen(onBack: () -> Unit) {
    var gameState by remember { mutableStateOf(Game2048Logic.createInitialState()) }
    var showRulesDialog by remember { mutableStateOf(false) }

    fun handleMove(direction: MoveDirection) {
        gameState = Game2048Logic.move(gameState, direction)
    }

    fun handleReset() {
        gameState = Game2048Logic.createInitialState(highScore = gameState.highScore)
    }

    fun handleUndo() {
        gameState.previousState?.let { prev ->
            gameState = prev
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "← Menu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "2048",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFEDC22E)
                )
                IconButton(onClick = { showRulesDialog = true }) {
                    Text("💡", fontSize = 20.sp)
                }
            }

            // Score Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScoreBadge(title = "SCORE", value = gameState.score)
                ScoreBadge(title = "BEST", value = gameState.highScore)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Swipe or tap arrows to play",
                fontSize = 13.sp,
                color = Color.Gray
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { handleUndo() },
                    enabled = gameState.previousState != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8F7A66),
                        disabledContainerColor = Color(0xFF3E3B38)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("↶ Undo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { handleReset() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F7A66)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("↺ New", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game Board with Swipe Detection
        var totalDragX by remember { mutableFloatStateOf(0f) }
        var totalDragY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFBBADA0), shape = RoundedCornerShape(12.dp))
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                            totalDragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                        },
                        onDragEnd = {
                            val minSwipeDistance = 40f
                            if (abs(totalDragX) > abs(totalDragY)) {
                                if (abs(totalDragX) > minSwipeDistance) {
                                    if (totalDragX > 0) handleMove(MoveDirection.RIGHT)
                                    else handleMove(MoveDirection.LEFT)
                                }
                            } else {
                                if (abs(totalDragY) > minSwipeDistance) {
                                    if (totalDragY > 0) handleMove(MoveDirection.DOWN)
                                    else handleMove(MoveDirection.UP)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (r in 0 until 4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (c in 0 until 4) {
                            val value = gameState.grid[r][c]
                            TileCell(
                                value = value,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }

            // Game Won Overlay
            if (gameState.isGameWon) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCCEDC22E), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "You Win!",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    gameState = gameState.copy(isGameWon = false, keepPlaying = true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F7A66))
                            ) {
                                Text("Keep Going", color = Color.White)
                            }
                            Button(
                                onClick = { handleReset() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F7A66))
                            ) {
                                Text("Try Again", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Game Over Overlay
            if (gameState.isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC3C3A32), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Game Over!",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { handleReset() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8F7A66))
                        ) {
                            Text("Try Again", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // On-screen Directional Arrow Pad
        DirectionalPad(onMove = { handleMove(it) })
    }

    if (showRulesDialog) {
        HowToPlayDialog(
            gameTitle = "2048",
            rulesText = "• Swipe or tap arrows to slide tiles.\n• When two tiles with the same number touch, they merge into one (2+2=4, 4+4=8, 8+8=16...).\n• Reach the 2048 tile to win!",
            onDismiss = { showRulesDialog = false }
        )
    }
}

@Composable
fun ScoreBadge(title: String, value: Int) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFBBADA0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEEE4DA)
            )
            Text(
                text = value.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun TileCell(value: Int, modifier: Modifier = Modifier) {
    val backgroundColor = when (value) {
        0 -> Color(0xFFCDC1B4)
        2 -> Color(0xFFEEE4DA)
        4 -> Color(0xFFEDE0C8)
        8 -> Color(0xFFF2B179)
        16 -> Color(0xFFF59563)
        32 -> Color(0xFFF67C5F)
        64 -> Color(0xFFF65E3B)
        128 -> Color(0xFFEDCF72)
        256 -> Color(0xFFEDCC61)
        512 -> Color(0xFFEDC850)
        1024 -> Color(0xFFEDC53F)
        2048 -> Color(0xFFEDC22E)
        else -> Color(0xFF3C3A32)
    }

    val textColor = when (value) {
        2, 4 -> Color(0xFF776E65)
        else -> Color(0xFFF9F6F2)
    }

    val fontSize = when {
        value < 100 -> 26.sp
        value < 1000 -> 22.sp
        else -> 18.sp
    }

    Box(
        modifier = modifier
            .background(backgroundColor, shape = RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (value > 0) {
            Text(
                text = value.toString(),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DirectionalPad(onMove: (MoveDirection) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Button(
            onClick = { onMove(MoveDirection.UP) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(48.dp)
        ) {
            Text("▲", color = Color.White, fontSize = 18.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Button(
                onClick = { onMove(MoveDirection.LEFT) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Text("◄", color = Color.White, fontSize = 18.sp)
            }

            Button(
                onClick = { onMove(MoveDirection.RIGHT) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Text("►", color = Color.White, fontSize = 18.sp)
            }
        }

        Button(
            onClick = { onMove(MoveDirection.DOWN) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(48.dp)
        ) {
            Text("▼", color = Color.White, fontSize = 18.sp)
        }
    }
}