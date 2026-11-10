package com.ramanstudio.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

enum class GameMode { VS_HUMAN, VS_COMPUTER }
enum class AiDifficulty(val label: String) { EASY("Easy"), MEDIUM("Medium"), HARD("Hard") }

@Composable
fun TicTacToeScreen(onBack: () -> Unit) {
    var gameMode by remember { mutableStateOf(GameMode.VS_COMPUTER) }
    var difficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }
    var showRulesDialog by remember { mutableStateOf(false) }

    var board by remember { mutableStateOf(List(9) { "" }) }
    var isXTurn by remember { mutableStateOf(true) }
    var winningLine by remember { mutableStateOf<List<Int>?>(null) }
    var winner by remember { mutableStateOf<String?>(null) }
    var isDraw by remember { mutableStateOf(false) }

    var scoreX by remember { mutableIntStateOf(0) }
    var scoreO by remember { mutableIntStateOf(0) }
    var scoreDraws by remember { mutableIntStateOf(0) }

    fun checkWinningLine(b: List<String>): List<Int>? {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (line in lines) {
            if (b[line[0]] != "" && b[line[0]] == b[line[1]] && b[line[1]] == b[line[2]]) {
                return line
            }
        }
        return null
    }

    fun handleWinOrDraw(b: List<String>, line: List<Int>?) {
        if (line != null) {
            winningLine = line
            val w = b[line[0]]
            winner = w
            if (w == "X") scoreX++ else scoreO++
        } else if (!b.contains("")) {
            isDraw = true
            scoreDraws++
        }
    }

    LaunchedEffect(isXTurn, gameMode, winner, isDraw) {
        if (gameMode == GameMode.VS_COMPUTER && !isXTurn && winner == null && !isDraw) {
            delay(400L)
            val aiMoveIndex = when (difficulty) {
                AiDifficulty.EASY -> getEasyAiMove(board)
                AiDifficulty.MEDIUM -> getMediumAiMove(board)
                AiDifficulty.HARD -> getHardAiMoveMinimax(board)
            }
            if (aiMoveIndex in 0..8 && board[aiMoveIndex] == "") {
                val newBoard = board.toMutableList()
                newBoard[aiMoveIndex] = "O"
                board = newBoard
                val line = checkWinningLine(newBoard)
                if (line != null || !newBoard.contains("")) {
                    handleWinOrDraw(newBoard, line)
                } else {
                    isXTurn = true
                }
            }
        }
    }

    fun onCellClick(index: Int) {
        if (board[index] != "" || winner != null || isDraw) return
        if (gameMode == GameMode.VS_COMPUTER && !isXTurn) return

        val newBoard = board.toMutableList()
        newBoard[index] = if (isXTurn) "X" else "O"
        board = newBoard

        val line = checkWinningLine(newBoard)
        if (line != null || !newBoard.contains("")) {
            handleWinOrDraw(newBoard, line)
        } else {
            isXTurn = !isXTurn
        }
    }

    fun resetBoard() {
        board = List(9) { "" }
        isXTurn = true
        winningLine = null
        winner = null
        isDraw = false
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
                Text("← Menu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Text(
                text = "Tic-Tac-Toe",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFC107)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showRulesDialog = true }) {
                    Text("💡", fontSize = 20.sp)
                }
                Button(
                    onClick = { resetBoard() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("↺ New", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Mode Selector
        Row(
            modifier = Modifier
                .background(Color(0xFF2A2826), shape = RoundedCornerShape(20.dp))
                .padding(4.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    gameMode = GameMode.VS_COMPUTER
                    resetBoard()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == GameMode.VS_COMPUTER) Color(0xFFFFC107) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🤖 vs 👤 Computer",
                    color = if (gameMode == GameMode.VS_COMPUTER) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = {
                    gameMode = GameMode.VS_HUMAN
                    resetBoard()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == GameMode.VS_HUMAN) Color(0xFFFFC107) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "👥 Pass & Play",
                    color = if (gameMode == GameMode.VS_HUMAN) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Difficulty Selector
        AnimatedVisibility(visible = gameMode == GameMode.VS_COMPUTER, enter = fadeIn()) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiDifficulty.entries.forEach { diff ->
                    FilterChip(
                        selected = difficulty == diff,
                        onClick = {
                            difficulty = diff
                            resetBoard()
                        },
                        label = { Text(diff.label, fontWeight = FontWeight.Bold) },
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

        Spacer(modifier = Modifier.height(12.dp))

        // Scoreboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreCard(title = "Player X", score = scoreX, color = Color(0xFF00E5FF))
            ScoreCard(title = "Draws", score = scoreDraws, color = Color.Gray)
            ScoreCard(
                title = if (gameMode == GameMode.VS_COMPUTER) "AI (${difficulty.label})" else "Player O",
                score = scoreO,
                color = Color(0xFFFF4081)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Banner
        val statusText = when {
            winner != null -> if (gameMode == GameMode.VS_COMPUTER && winner == "O") "🤖 AI Won!" else "🎉 Player $winner Won!"
            isDraw -> "🤝 It's a Draw!"
            else -> if (gameMode == GameMode.VS_COMPUTER && !isXTurn) "🤖 AI is thinking..." else "Player ${if (isXTurn) "X" else "O"}'s Turn"
        }

        val statusColor = when {
            winner == "X" -> Color(0xFF00E5FF)
            winner == "O" -> Color(0xFFFF4081)
            isDraw -> Color.Yellow
            else -> Color.White
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3x3 Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF333333), shape = RoundedCornerShape(16.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                for (row in 0..2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            val cellValue = board[index]
                            val isWinningCell = winningLine?.contains(index) == true

                            val cellBgColor = when {
                                isWinningCell -> Color(0xFF37474F)
                                else -> Color(0xFF262626)
                            }

                            val cellBorderColor = when {
                                isWinningCell -> Color(0xFFFFD700)
                                else -> Color(0xFF333333)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(cellBgColor, RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isWinningCell) 3.dp else 1.dp,
                                        color = cellBorderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onCellClick(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (cellValue != "") {
                                    Text(
                                        text = cellValue,
                                        color = if (cellValue == "X") Color(0xFF00E5FF) else Color(0xFFFF4081),
                                        fontSize = 54.sp,
                                        fontWeight = FontWeight.ExtraBold
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
            gameTitle = "Tic-Tac-Toe",
            rulesText = "• Take turns placing your mark (X or O) on the 3x3 grid.\n• Get 3 of your marks in a row (horizontally, vertically, or diagonally) to win!",
            onDismiss = { showRulesDialog = false }
        )
    }
}

@Composable
fun ScoreCard(title: String, score: Int, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(text = title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(text = score.toString(), fontSize = 18.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

fun getEasyAiMove(b: List<String>): Int {
    val emptyIndices = b.indices.filter { b[it] == "" }
    return if (emptyIndices.isNotEmpty()) emptyIndices.random() else -1
}

fun getMediumAiMove(b: List<String>): Int {
    val winLines = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
        listOf(0, 4, 8), listOf(2, 4, 6)
    )

    for (i in 0 until 9) {
        if (b[i] == "") {
            val testB = b.toMutableList()
            testB[i] = "O"
            if (winLines.any { line -> testB[line[0]] != "" && testB[line[0]] == testB[line[1]] && testB[line[1]] == testB[line[2]] }) {
                return i
            }
        }
    }

    for (i in 0 until 9) {
        if (b[i] == "") {
            val testB = b.toMutableList()
            testB[i] = "X"
            if (winLines.any { line -> testB[line[0]] != "" && testB[line[0]] == testB[line[1]] && testB[line[1]] == testB[line[2]] }) {
                return i
            }
        }
    }

    if (b[4] == "") return 4

    return getEasyAiMove(b)
}

fun getHardAiMoveMinimax(b: List<String>): Int {
    var bestScore = Int.MIN_VALUE
    var bestMove = -1
    for (i in 0 until 9) {
        if (b[i] == "") {
            val nextBoard = b.toMutableList()
            nextBoard[i] = "O"
            val score = minimax(nextBoard, 0, false)
            if (score > bestScore) {
                bestScore = score
                bestMove = i
            }
        }
    }
    return if (bestMove != -1) bestMove else getEasyAiMove(b)
}

fun minimax(board: List<String>, depth: Int, isMaximizing: Boolean): Int {
    val winLines = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
        listOf(0, 4, 8), listOf(2, 4, 6)
    )

    for (line in winLines) {
        if (board[line[0]] != "" && board[line[0]] == board[line[1]] && board[line[1]] == board[line[2]]) {
            return if (board[line[0]] == "O") 10 - depth else depth - 10
        }
    }
    if (!board.contains("")) return 0

    return if (isMaximizing) {
        var maxEval = Int.MIN_VALUE
        for (i in 0 until 9) {
            if (board[i] == "") {
                val nextBoard = board.toMutableList()
                nextBoard[i] = "O"
                val eval = minimax(nextBoard, depth + 1, false)
                maxEval = maxOf(maxEval, eval)
            }
        }
        maxEval
    } else {
        var minEval = Int.MAX_VALUE
        for (i in 0 until 9) {
            if (board[i] == "") {
                val nextBoard = board.toMutableList()
                nextBoard[i] = "X"
                val eval = minimax(nextBoard, depth + 1, true)
                minEval = minOf(minEval, eval)
            }
        }
        minEval
    }
}