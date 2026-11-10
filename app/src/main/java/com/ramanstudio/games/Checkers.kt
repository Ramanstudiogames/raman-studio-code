package com.ramanstudio.games

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

enum class CheckersMode { VS_COMPUTER, VS_HUMAN }
enum class CheckersDifficulty(val label: String) { EASY("Easy"), MEDIUM("Medium"), HARD("Hard") }

data class CheckersPosition(val row: Int, val col: Int)

@Composable
fun CheckersScreen(onBack: () -> Unit) {
    var gameMode by remember { mutableStateOf(CheckersMode.VS_COMPUTER) }
    var difficulty by remember { mutableStateOf(CheckersDifficulty.MEDIUM) }
    var showRulesDialog by remember { mutableStateOf(false) }

    // 0 = empty, 1 = Red, 2 = Black, 3 = Red King, 4 = Black King
    var board by remember { mutableStateOf(initialCheckersBoard()) }
    var currentPlayer by remember { mutableIntStateOf(1) } // 1 = Red, 2 = Black / AI
    var selectedSquare by remember { mutableStateOf<CheckersPosition?>(null) }
    var validTargetMoves by remember { mutableStateOf<List<CheckersPosition>>(emptyList()) }

    var statusMessage by remember { mutableStateOf("Player Red's Turn") }

    fun getValidMovesForCell(from: CheckersPosition): List<CheckersPosition> {
        val targets = mutableListOf<CheckersPosition>()
        for (r in 0..7) {
            for (c in 0..7) {
                val to = CheckersPosition(r, c)
                if ((r + c) % 2 != 0 && isValidCheckersMove(board, currentPlayer, from, to)) {
                    targets.add(to)
                }
            }
        }
        return targets
    }

    fun executeCheckersMove(from: CheckersPosition, to: CheckersPosition) {
        val newBoard = board.map { it.toMutableList() }.toMutableList()
        val piece = newBoard[from.row][from.col]

        newBoard[to.row][to.col] = piece
        newBoard[from.row][from.col] = 0

        // Capture jump
        if (abs(from.row - to.row) == 2) {
            val midRow = (from.row + to.row) / 2
            val midCol = (from.col + to.col) / 2
            newBoard[midRow][midCol] = 0
        }

        // Kinging
        if (piece == 1 && to.row == 7) newBoard[to.row][to.col] = 3
        if (piece == 2 && to.row == 0) newBoard[to.row][to.col] = 4

        board = newBoard.map { it.toList() }
        selectedSquare = null
        validTargetMoves = emptyList()

        // Check Win Condition
        val redRemaining = board.sumOf { row -> row.count { it == 1 || it == 3 } }
        val blackRemaining = board.sumOf { row -> row.count { it == 2 || it == 4 } }

        if (blackRemaining == 0) {
            statusMessage = "🎉 Player Red Wins!"
            return
        } else if (redRemaining == 0) {
            statusMessage = if (gameMode == CheckersMode.VS_COMPUTER) "🤖 Computer Wins!" else "🎉 Player Black Wins!"
            return
        }

        currentPlayer = if (currentPlayer == 1) 2 else 1
        statusMessage = if (gameMode == CheckersMode.VS_COMPUTER && currentPlayer == 2) "🤖 Computer thinking..." else "Player ${if (currentPlayer == 1) "Red" else "Black"}'s Turn"
    }

    // AI Move Effect
    LaunchedEffect(currentPlayer, gameMode, board) {
        if (gameMode == CheckersMode.VS_COMPUTER && currentPlayer == 2 && !statusMessage.contains("Wins")) {
            delay(500L)
            val aiMove = calculateAiCheckersMove(board, difficulty)
            if (aiMove != null) {
                executeCheckersMove(aiMove.first, aiMove.second)
            }
        }
    }

    fun onSquareClick(row: Int, col: Int) {
        if (gameMode == CheckersMode.VS_COMPUTER && currentPlayer == 2) return
        val clickedPos = CheckersPosition(row, col)
        val piece = board[row][col]

        if (selectedSquare == null) {
            val isOwnPiece = if (currentPlayer == 1) (piece == 1 || piece == 3) else (piece == 2 || piece == 4)
            if (isOwnPiece) {
                selectedSquare = clickedPos
                validTargetMoves = getValidMovesForCell(clickedPos)
            }
        } else {
            if (selectedSquare == clickedPos) {
                selectedSquare = null
                validTargetMoves = emptyList()
            } else if (clickedPos in validTargetMoves) {
                executeCheckersMove(selectedSquare!!, clickedPos)
            } else {
                val isOwnPiece = if (currentPlayer == 1) (piece == 1 || piece == 3) else (piece == 2 || piece == 4)
                if (isOwnPiece) {
                    selectedSquare = clickedPos
                    validTargetMoves = getValidMovesForCell(clickedPos)
                }
            }
        }
    }

    fun resetGame() {
        board = initialCheckersBoard()
        currentPlayer = 1
        selectedSquare = null
        validTargetMoves = emptyList()
        statusMessage = "Player Red's Turn"
    }

    val redCount = board.sumOf { row -> row.count { it == 1 || it == 3 } }
    val blackCount = board.sumOf { row -> row.count { it == 2 || it == 4 } }

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

            Text("Checkers", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E676))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showRulesDialog = true }) {
                    Text("💡", fontSize = 20.sp)
                }
                Button(
                    onClick = { resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) { Text("↺ New", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }

        // Mode Switcher
        Row(
            modifier = Modifier
                .background(Color(0xFF2A2826), shape = RoundedCornerShape(20.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { gameMode = CheckersMode.VS_COMPUTER; resetGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == CheckersMode.VS_COMPUTER) Color(0xFF00E676) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("🤖 vs 👤 Computer", color = if (gameMode == CheckersMode.VS_COMPUTER) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }

            Button(
                onClick = { gameMode = CheckersMode.VS_HUMAN; resetGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == CheckersMode.VS_HUMAN) Color(0xFF00E676) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("👥 Pass & Play", color = if (gameMode == CheckersMode.VS_HUMAN) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }

        // Difficulty Chips (if VS_COMPUTER)
        AnimatedVisibility(visible = gameMode == CheckersMode.VS_COMPUTER) {
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CheckersDifficulty.entries.forEach { diff ->
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

        Spacer(modifier = Modifier.height(8.dp))

        // Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = statusMessage,
                color = if (currentPlayer == 1) Color(0xFFFF5252) else Color(0xFFFFD54F),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scoreboard Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(8.dp)) {
                Text("🔴 Red: $redCount", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(8.dp)) {
                Text(if (gameMode == CheckersMode.VS_COMPUTER) "🤖 AI: $blackCount" else "⚫ Black: $blackCount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 8x8 Wooden Checkers Board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF2E1C0C), shape = RoundedCornerShape(12.dp))
                .border(3.dp, Color(0xFF5D4037), shape = RoundedCornerShape(12.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column {
                for (row in 0..7) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        for (col in 0..7) {
                            val isDarkSquare = (row + col) % 2 != 0
                            val piece = board[row][col]
                            val isSelected = selectedSquare?.row == row && selectedSquare?.col == col
                            val isValidTarget = CheckersPosition(row, col) in validTargetMoves

                            val squareBg = when {
                                isSelected -> Color(0xFF80DEEA)
                                isValidTarget -> Color(0xFFA5D6A7)
                                isDarkSquare -> Color(0xFF4E342E)
                                else -> Color(0xFFD7CCC8)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(squareBg)
                                    .border(
                                        width = if (isSelected || isValidTarget) 2.dp else 0.dp,
                                        color = if (isSelected) Color(0xFF00E5FF) else if (isValidTarget) Color(0xFF2E7D32) else Color.Transparent
                                    )
                                    .clickable(enabled = isDarkSquare) { onSquareClick(row, col) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece != 0) {
                                    val isRedPiece = piece == 1 || piece == 3
                                    val isKing = piece == 3 || piece == 4

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.8f)
                                            .clip(CircleShape)
                                            .background(if (isRedPiece) Color(0xFFFF1744) else Color(0xFF212121))
                                            .border(1.5.dp, if (isRedPiece) Color.White else Color(0xFFFFD700), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isKing) {
                                            Text("👑", fontSize = 16.sp)
                                        }
                                    }
                                } else if (isValidTarget) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(Color(0x882E7D32), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRulesDialog) {
        HowToPlayDialog(
            gameTitle = "Checkers",
            rulesText = "• Move pieces diagonally forward onto dark squares.\n• Jump over adjacent enemy pieces to capture them.\n• Reaching the opposite end promotes your piece to a King (👑), allowing backward diagonal movement!",
            onDismiss = { showRulesDialog = false }
        )
    }
}

fun isValidCheckersMove(b: List<List<Int>>, player: Int, from: CheckersPosition, to: CheckersPosition): Boolean {
    val piece = b[from.row][from.col]
    val target = b[to.row][to.col]
    if (target != 0) return false

    val rowDiff = to.row - from.row
    val colDiff = abs(to.col - from.col)

    if (colDiff == 1) {
        if (piece == 1 && rowDiff == 1) return true
        if (piece == 2 && rowDiff == -1) return true
        if ((piece == 3 || piece == 4) && abs(rowDiff) == 1) return true
    }

    if (colDiff == 2) {
        val midRow = (from.row + to.row) / 2
        val midCol = (from.col + to.col) / 2
        val midPiece = b[midRow][midCol]
        val isOpponent = if (player == 1) (midPiece == 2 || midPiece == 4) else (midPiece == 1 || midPiece == 3)

        if (isOpponent) {
            if (piece == 1 && rowDiff == 2) return true
            if (piece == 2 && rowDiff == -2) return true
            if ((piece == 3 || piece == 4) && abs(rowDiff) == 2) return true
        }
    }
    return false
}

fun calculateAiCheckersMove(b: List<List<Int>>, difficulty: CheckersDifficulty): Pair<CheckersPosition, CheckersPosition>? {
    val legalMoves = mutableListOf<Pair<CheckersPosition, CheckersPosition>>()

    for (r in 0..7) {
        for (c in 0..7) {
            val piece = b[r][c]
            if (piece == 2 || piece == 4) { // Black AI pieces
                val from = CheckersPosition(r, c)
                for (tr in 0..7) {
                    for (tc in 0..7) {
                        val to = CheckersPosition(tr, tc)
                        if ((tr + tc) % 2 != 0 && isValidCheckersMove(b, 2, from, to)) {
                            legalMoves.add(Pair(from, to))
                        }
                    }
                }
            }
        }
    }

    if (legalMoves.isEmpty()) return null

    val jumps = legalMoves.filter { abs(it.first.row - it.second.row) == 2 }
    if (jumps.isNotEmpty()) return jumps.random()

    return legalMoves.random()
}

fun initialCheckersBoard(): List<List<Int>> {
    val b = MutableList(8) { MutableList(8) { 0 } }
    for (row in 0..2) {
        for (col in 0..7) {
            if ((row + col) % 2 != 0) b[row][col] = 1
        }
    }
    for (row in 5..7) {
        for (col in 0..7) {
            if ((row + col) % 2 != 0) b[row][col] = 2
        }
    }
    return b.map { it.toList() }
}