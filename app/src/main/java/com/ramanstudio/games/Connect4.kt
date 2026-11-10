package com.ramanstudio.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

enum class Connect4Mode { VS_PLAYER, VS_COMPUTER }

fun checkWinningCellsConnect4(b: List<List<Int>>, p: Int, rows: Int, cols: Int): List<Pair<Int, Int>>? {
    // Horizontal
    for (r in 0 until rows) {
        for (c in 0 until cols - 3) {
            if (b[r][c] == p && b[r][c + 1] == p && b[r][c + 2] == p && b[r][c + 3] == p) {
                return listOf(Pair(r, c), Pair(r, c + 1), Pair(r, c + 2), Pair(r, c + 3))
            }
        }
    }
    // Vertical
    for (r in 0 until rows - 3) {
        for (c in 0 until cols) {
            if (b[r][c] == p && b[r + 1][c] == p && b[r + 2][c] == p && b[r + 3][c] == p) {
                return listOf(Pair(r, c), Pair(r + 1, c), Pair(r + 2, c), Pair(r + 3, c))
            }
        }
    }
    // Diagonal Down-Right
    for (r in 0 until rows - 3) {
        for (c in 0 until cols - 3) {
            if (b[r][c] == p && b[r + 1][c + 1] == p && b[r + 2][c + 2] == p && b[r + 3][c + 3] == p) {
                return listOf(Pair(r, c), Pair(r + 1, c + 1), Pair(r + 2, c + 2), Pair(r + 3, c + 3))
            }
        }
    }
    // Diagonal Up-Right
    for (r in 3 until rows) {
        for (c in 0 until cols - 3) {
            if (b[r][c] == p && b[r - 1][c + 1] == p && b[r - 2][c + 2] == p && b[r - 3][c + 3] == p) {
                return listOf(Pair(r, c), Pair(r - 1, c + 1), Pair(r - 2, c + 2), Pair(r - 3, c + 3))
            }
        }
    }
    return null
}

fun calculateAiConnect4Move(board: List<List<Int>>, rows: Int, cols: Int): Int {
    fun canWin(b: List<List<Int>>, player: Int, col: Int): Boolean {
        val newBoard = b.map { it.toMutableList() }.toMutableList()
        var rowToDrop = -1
        for (r in rows - 1 downTo 0) {
            if (newBoard[r][col] == 0) {
                rowToDrop = r
                break
            }
        }
        if (rowToDrop == -1) return false
        newBoard[rowToDrop][col] = player
        return checkWinningCellsConnect4(newBoard, player, rows, cols) != null
    }

    val validCols = (0 until cols).filter { board[0][it] == 0 }
    if (validCols.isEmpty()) return -1

    // 1. AI win condition
    for (c in validCols) {
        if (canWin(board, 2, c)) return c
    }
    // 2. Block player win condition
    for (c in validCols) {
        if (canWin(board, 1, c)) return c
    }
    // 3. Prefer center columns
    val centerCols = validCols.sortedBy { abs(it - cols / 2) }
    return centerCols.firstOrNull() ?: validCols.random()
}

@Composable
fun Connect4Screen(onBack: () -> Unit) {
    val rows = 6
    val cols = 7
    var showRulesDialog by remember { mutableStateOf(false) }

    var gameMode by remember { mutableStateOf(Connect4Mode.VS_COMPUTER) }
    var board by remember { mutableStateOf(List(rows) { List(cols) { 0 } }) }
    var currentPlayer by remember { mutableIntStateOf(1) } // 1 = Red, 2 = Yellow
    var winningCells by remember { mutableStateOf<List<Pair<Int, Int>>?>(null) }
    var winner by remember { mutableStateOf<Int?>(null) }
    var isDraw by remember { mutableStateOf(false) }

    var scoreRed by remember { mutableIntStateOf(0) }
    var scoreYellow by remember { mutableIntStateOf(0) }

    fun restartGame() {
        board = List(rows) { List(cols) { 0 } }
        winner = null
        winningCells = null
        isDraw = false
        currentPlayer = 1
    }

    fun onColumnClick(col: Int) {
        if (winner != null || isDraw) return
        if (gameMode == Connect4Mode.VS_COMPUTER && currentPlayer == 2) return // Block input during AI turn

        val newBoard = board.map { it.toMutableList() }.toMutableList()
        var rowToDrop = -1
        for (r in rows - 1 downTo 0) {
            if (newBoard[r][col] == 0) {
                rowToDrop = r
                break
            }
        }

        if (rowToDrop != -1) {
            newBoard[rowToDrop][col] = currentPlayer
            board = newBoard.map { it.toList() }

            val winList = checkWinningCellsConnect4(newBoard, currentPlayer, rows, cols)
            if (winList != null) {
                winner = currentPlayer
                winningCells = winList
                if (currentPlayer == 1) scoreRed++ else scoreYellow++
            } else if (newBoard.all { row -> row.all { it != 0 } }) {
                isDraw = true
            } else {
                currentPlayer = if (currentPlayer == 1) 2 else 1
            }
        }
    }

    // AI Turn effect
    LaunchedEffect(currentPlayer, gameMode, winner, isDraw) {
        if (gameMode == Connect4Mode.VS_COMPUTER && currentPlayer == 2 && winner == null && !isDraw) {
            delay(600) // AI thinking time
            val aiMove = calculateAiConnect4Move(board, rows, cols)
            if (aiMove != -1) {
                // Manually apply AI move similar to onColumnClick but bypassing input block
                val newBoard = board.map { it.toMutableList() }.toMutableList()
                var rowToDrop = -1
                for (r in rows - 1 downTo 0) {
                    if (newBoard[r][aiMove] == 0) {
                        rowToDrop = r
                        break
                    }
                }
                if (rowToDrop != -1) {
                    newBoard[rowToDrop][aiMove] = currentPlayer
                    board = newBoard.map { it.toList() }

                    val winList = checkWinningCellsConnect4(newBoard, currentPlayer, rows, cols)
                    if (winList != null) {
                        winner = currentPlayer
                        winningCells = winList
                        if (currentPlayer == 1) scoreRed++ else scoreYellow++
                    } else if (newBoard.all { row -> row.all { it != 0 } }) {
                        isDraw = true
                    } else {
                        currentPlayer = 1
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)) // Deep dark theme
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("← Menu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Text(
                text = "Connect 4",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0D47A1)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showRulesDialog = true }) {
                    Text("💡", fontSize = 20.sp)
                }
                Button(
                    onClick = { restartGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("↺ New", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Mode Switcher
        Row(
            modifier = Modifier
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    gameMode = Connect4Mode.VS_COMPUTER
                    restartGame()
                    scoreRed = 0
                    scoreYellow = 0
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == Connect4Mode.VS_COMPUTER) Color(0xFF00E676) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("🤖 vs 👤 Computer", color = if (gameMode == Connect4Mode.VS_COMPUTER) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    gameMode = Connect4Mode.VS_PLAYER
                    restartGame()
                    scoreRed = 0
                    scoreYellow = 0
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == Connect4Mode.VS_PLAYER) Color(0xFF00E676) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("👥 Pass & Play", color = if (gameMode == Connect4Mode.VS_PLAYER) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scoreboard Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), 
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Color(0xFFD32F2F))
            ) {
                Text("🔴 Red: $scoreRed", color = Color(0xFFFF1744), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), 
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Color(0xFFFBC02D))
            ) {
                Text("🟡 Yellow: $scoreYellow", color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Banner
        val statusText = when {
            winner == 1 -> "🎉 Player Red Wins!"
            winner == 2 -> if (gameMode == Connect4Mode.VS_COMPUTER) "🤖 Computer Wins!" else "🎉 Player Yellow Wins!"
            isDraw -> "🤝 It's a Draw!"
            else -> if (currentPlayer == 1) "Player Red's Turn 🔴" else if (gameMode == Connect4Mode.VS_COMPUTER) "Computer Thinking 🤖" else "Player Yellow's Turn 🟡"
        }

        val statusColor = when (winner) {
            1 -> Color(0xFFFF1744)
            2 -> Color(0xFFFFD54F)
            else -> if (currentPlayer == 1) Color(0xFFFF1744) else Color(0xFFFFD54F)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Drop Indicators Row above Board
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (c in 0 until cols) {
                IconButton(
                    onClick = { onColumnClick(c) },
                    modifier = Modifier.size(36.dp),
                    enabled = winner == null && !isDraw && !(gameMode == Connect4Mode.VS_COMPUTER && currentPlayer == 2)
                ) {
                    Text("▼", color = statusColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Neon / Modern Arcade Connect 4 Board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D47A1), Color(0xFF1565C0))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(4.dp, Color(0xFF1976D2), shape = RoundedCornerShape(16.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (r in 0 until rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in 0 until cols) {
                            val cellState = board[r][c]
                            val isWin = winningCells?.contains(Pair(r, c)) == true

                            val tokenColor1 = when (cellState) {
                                1 -> Color(0xFFFF1744)
                                2 -> Color(0xFFFFD54F)
                                else -> Color(0xFF0A0A0A)
                            }
                            val tokenColor2 = when (cellState) {
                                1 -> Color(0xFFD32F2F)
                                2 -> Color(0xFFFBC02D)
                                else -> Color(0xFF121212)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .shadow(if (cellState != 0) 4.dp else 0.dp, CircleShape, clip = false)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(tokenColor1, tokenColor2),
                                            radius = 100f
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (isWin) 4.dp else 2.dp,
                                        color = if (isWin) Color.White else Color(0xFF000000).copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape)
                                    .clickable(enabled = winner == null && !isDraw) { onColumnClick(c) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isWin) {
                                    Text("★", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showRulesDialog) {
            HowToPlayDialog(
                gameTitle = "Connect 4",
                rulesText = "• Players take turns dropping colored discs (🔴 Red or 🟡 Yellow) into the columns.\n• Discs drop straight down to the lowest empty space in that column.\n• Connect 4 of your discs horizontally, vertically, or diagonally to win!",
                onDismiss = { showRulesDialog = false }
            )
        }
    }
}
