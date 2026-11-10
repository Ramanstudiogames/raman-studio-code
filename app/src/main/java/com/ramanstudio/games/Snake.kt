package com.ramanstudio.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }
data class Point(val x: Int, val y: Int)

enum class SnakeSpeed(val label: String, val delayMs: Long) {
    SLOW("Slow 🐢", 260L),
    NORMAL("Normal 🏃", 180L),
    FAST("Fast ⚡", 110L)
}

@Composable
fun SnakeScreen(onBack: () -> Unit) {
    val boardSize = 20
    var showRulesDialog by remember { mutableStateOf(false) }
    var snake by remember { mutableStateOf(listOf(Point(10, 10), Point(9, 10), Point(8, 10))) }
    var food by remember { mutableStateOf(Point(15, 10)) }
    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    var speed by remember { mutableStateOf(SnakeSpeed.NORMAL) }

    fun spawnFood(): Point {
        var p: Point
        do {
            p = Point(Random.nextInt(boardSize), Random.nextInt(boardSize))
        } while (snake.contains(p))
        return p
    }

    fun restartGame() {
        snake = listOf(Point(10, 10), Point(9, 10), Point(8, 10))
        food = spawnFood()
        direction = Direction.RIGHT
        score = 0
        isGameOver = false
        isPaused = false
    }

    // Main Game Loop
    LaunchedEffect(isGameOver, isPaused, direction, speed) {
        while (!isGameOver && !isPaused) {
            delay(speed.delayMs)
            val head = snake.first()
            val next = when (direction) {
                Direction.UP -> Point(head.x, head.y - 1)
                Direction.DOWN -> Point(head.x, head.y + 1)
                Direction.LEFT -> Point(head.x - 1, head.y)
                Direction.RIGHT -> Point(head.x + 1, head.y)
            }

            if (next.x !in 0 until boardSize || next.y !in 0 until boardSize || snake.contains(next)) {
                isGameOver = true
                if (score > highScore) highScore = score
            } else {
                val newSnake = snake.toMutableList()
                newSnake.add(0, next)

                if (next == food) {
                    score += 10
                    if (score > highScore) highScore = score
                    food = spawnFood()
                } else {
                    newSnake.removeAt(newSnake.lastIndex)
                }
                snake = newSnake
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
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
                Text("← Menu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Text(
                text = "Snake",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFC107)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showRulesDialog = true }) {
                    Text("💡", fontSize = 20.sp)
                }
                Button(
                    onClick = { restartGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("↺ New", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Scoreboard & Pause Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(8.dp)) {
                    Text("Score: $score", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(8.dp)) {
                    Text("Best: $highScore", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }

            Button(
                onClick = { isPaused = !isPaused },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(if (isPaused) "▶️ Resume" else "⏸️ Pause", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Speed Selector Chips
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SnakeSpeed.entries.forEach { spd ->
                FilterChip(
                    selected = speed == spd,
                    onClick = { speed = spd },
                    label = { Text(spd.label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFC107),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF2A2826),
                        labelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Snake Game Board Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFFFFC107), shape = RoundedCornerShape(12.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cellSize = maxWidth / boardSize

                // Draw Food (Apple)
                Box(
                    modifier = Modifier
                        .offset(x = cellSize * food.x, y = cellSize * food.y)
                        .size(cellSize)
                        .background(Color(0xFFFF1744), CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                )

                // Draw Snake Segments
                snake.forEachIndexed { index, segment ->
                    val isHead = index == 0
                    val segColor = if (isHead) Color(0xFF00E676) else Color(0xFF00B0FF)

                    Box(
                        modifier = Modifier
                            .offset(x = cellSize * segment.x, y = cellSize * segment.y)
                            .size(cellSize)
                            .background(segColor, RoundedCornerShape(if (isHead) 6.dp else 3.dp))
                            .border(0.5.dp, Color(0xFF121212), RoundedCornerShape(4.dp))
                    )
                }
            }

            // Game Over Overlay
            if (isGameOver) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE121212)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💥 Game Over! 💥", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF1744))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Final Score: $score", fontSize = 18.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { restartGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                        ) {
                            Text("Play Again", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // D-Pad Directional Arrow Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { if (direction != Direction.DOWN) direction = Direction.UP },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp)
            ) { Text("▲", color = Color.White, fontSize = 18.sp) }

            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Button(
                    onClick = { if (direction != Direction.RIGHT) direction = Direction.LEFT },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(48.dp)
                ) { Text("◄", color = Color.White, fontSize = 18.sp) }

                Button(
                    onClick = { if (direction != Direction.LEFT) direction = Direction.RIGHT },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(48.dp)
                ) { Text("►", color = Color.White, fontSize = 18.sp) }
            }

            Button(
                onClick = { if (direction != Direction.UP) direction = Direction.DOWN },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp)
            ) { Text("▼", color = Color.White, fontSize = 18.sp) }
        }
    }

    if (showRulesDialog) {
        HowToPlayDialog(
            gameTitle = "Snake",
            rulesText = "• Guide the snake using arrow controls or swipes to eat apples.\n• Each apple grows your tail and increases your score.\n• Avoid crashing into board walls or your own tail!",
            onDismiss = { showRulesDialog = false }
        )
    }
}