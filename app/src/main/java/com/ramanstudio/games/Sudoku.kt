package com.ramanstudio.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class SudokuDifficulty(val label: String, val cellsToRemove: Int) {
    EASY("Easy", 30),
    MEDIUM("Medium", 45),
    HARD("Hard", 55)
}

object SudokuGenerator {
    suspend fun generate(difficulty: SudokuDifficulty): Pair<List<List<Int>>, List<List<Int>>> {
        return withContext(Dispatchers.Default) {
            val board = Array(9) { IntArray(9) }
            fillDiagonal(board)
            solveSudoku(board)
            
            val solution = board.map { it.toList() }
            
            removeDigits(board, difficulty.cellsToRemove)
            val puzzle = board.map { it.toList() }
            
            Pair(puzzle, solution)
        }
    }

    private fun fillDiagonal(board: Array<IntArray>) {
        for (i in 0 until 9 step 3) {
            fillBox(board, i, i)
        }
    }

    private fun fillBox(board: Array<IntArray>, rowStart: Int, colStart: Int) {
        var num: Int
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                do {
                    num = Random.nextInt(1, 10)
                } while (!isSafeInBox(board, rowStart, colStart, num))
                board[rowStart + i][colStart + j] = num
            }
        }
    }

    private fun isSafeInBox(board: Array<IntArray>, rowStart: Int, colStart: Int, num: Int): Boolean {
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                if (board[rowStart + i][colStart + j] == num) return false
            }
        }
        return true
    }

    private fun solveSudoku(board: Array<IntArray>): Boolean {
        var row = -1
        var col = -1
        var isEmpty = true
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (board[i][j] == 0) {
                    row = i
                    col = j
                    isEmpty = false
                    break
                }
            }
            if (!isEmpty) break
        }
        
        if (isEmpty) return true

        for (num in 1..9) {
            if (isSafe(board, row, col, num)) {
                board[row][col] = num
                if (solveSudoku(board)) return true
                board[row][col] = 0
            }
        }
        return false
    }

    private fun isSafe(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (d in 0 until 9) {
            if (board[row][d] == num) return false
        }
        for (r in 0 until 9) {
            if (board[r][col] == num) return false
        }
        val sqrt = 3
        val boxRowStart = row - row % sqrt
        val boxColStart = col - col % sqrt
        for (r in boxRowStart until boxRowStart + sqrt) {
            for (d in boxColStart until boxColStart + sqrt) {
                if (board[r][d] == num) return false
            }
        }
        return true
    }

    private fun removeDigits(board: Array<IntArray>, count: Int) {
        var c = count
        while (c != 0) {
            val cellId = Random.nextInt(0, 81)
            val i = cellId / 9
            val j = cellId % 9
            if (board[i][j] != 0) {
                c--
                board[i][j] = 0
            }
        }
    }
}

@Composable
fun SudokuScreen(onBack: () -> Unit) {
    var difficulty by remember { mutableStateOf(SudokuDifficulty.MEDIUM) }
    var showRulesDialog by remember { mutableStateOf(false) }

    var initialBoard by remember { mutableStateOf<List<List<Int>>?>(null) }
    var currentBoard by remember { mutableStateOf<List<MutableList<Int>>?>(null) }
    var solutionBoard by remember { mutableStateOf<List<List<Int>>?>(null) }
    
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    var isWon by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var mistakes by remember { mutableIntStateOf(0) }
    val maxMistakes = 3
    var moveHistory by remember { mutableStateOf(listOf<Triple<Int, Int, Int>>()) }

    val scope = rememberCoroutineScope()

    fun generateNewGame() {
        isGenerating = true
        isWon = false
        isGameOver = false
        mistakes = 0
        moveHistory = emptyList()
        selectedCell = null
        scope.launch {
            val (puzzle, solution) = SudokuGenerator.generate(difficulty)
            initialBoard = puzzle
            currentBoard = puzzle.map { it.toMutableList() }
            solutionBoard = solution
            isGenerating = false
        }
    }

    LaunchedEffect(Unit) {
        generateNewGame()
    }

    fun onNumberPadClick(number: Int) {
        if (isWon || isGameOver || isGenerating) return
        val cell = selectedCell ?: return
        val (r, c) = cell
        
        // Cannot overwrite initial given numbers
        if (initialBoard?.get(r)?.get(c) != 0) return

        val currentVal = currentBoard!![r][c]
        if (currentVal == number) return

        val newBoard = currentBoard?.map { it.toMutableList() }?.toMutableList() ?: return
        
        // Add to history
        moveHistory = moveHistory + Triple(r, c, currentVal)

        newBoard[r][c] = number
        currentBoard = newBoard

        // Check mistake
        if (number != 0 && number != solutionBoard!![r][c]) {
            mistakes++
            if (mistakes >= maxMistakes) {
                isGameOver = true
                selectedCell = null
            }
        }

        // Check Win Condition
        if (currentBoard == solutionBoard) {
            isWon = true
            selectedCell = null
        }
    }

    fun undoMove() {
        if (moveHistory.isNotEmpty() && !isWon && !isGameOver && !isGenerating) {
            val lastMove = moveHistory.last()
            moveHistory = moveHistory.dropLast(1)
            val (r, c, prevVal) = lastMove
            val newBoard = currentBoard!!.map { it.toMutableList() }.toMutableList()
            newBoard[r][c] = prevVal
            currentBoard = newBoard
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
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
                text = "Sudoku",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFC107) // Amber/Gold Theme
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showRulesDialog = true }) {
                    Text("💡", fontSize = 20.sp)
                }
                Button(
                    onClick = { generateNewGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("↺ New", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Difficulty Selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            SudokuDifficulty.entries.forEach { diff ->
                FilterChip(
                    selected = difficulty == diff,
                    onClick = {
                        difficulty = diff
                        generateNewGame()
                    },
                    label = { Text(diff.label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFC107),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E1E1E),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        if (isGenerating || currentBoard == null) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFFC107))
            }
        } else {
            // Sudoku Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFF262626), RoundedCornerShape(8.dp))
                    .border(3.dp, Color(0xFFFFC107), RoundedCornerShape(8.dp))
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    for (boxRow in 0 until 3) {
                        Row(modifier = Modifier.weight(1f)) {
                            for (boxCol in 0 until 3) {
                                // 3x3 Box
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(1.dp, Color(0xFF555555))
                                ) {
                                    Column {
                                        for (r in 0 until 3) {
                                            Row(modifier = Modifier.weight(1f)) {
                                                for (c in 0 until 3) {
                                                    val globalRow = boxRow * 3 + r
                                                    val globalCol = boxCol * 3 + c
                                                    val value = currentBoard!![globalRow][globalCol]
                                                    val isInitial = initialBoard!![globalRow][globalCol] != 0
                                                    val isSelected = selectedCell == Pair(globalRow, globalCol)
                                                    
                                                    // Highlight cells with the same number as the selected cell (if filled)
                                                    val selectedValue = selectedCell?.let { currentBoard!![it.first][it.second] } ?: 0
                                                    val isSameNumberHighlight = value != 0 && selectedValue == value && !isSelected

                                                    val bgColor = when {
                                                        isSelected -> Color(0x66FFC107)
                                                        isSameNumberHighlight -> Color(0x33FFC107)
                                                        else -> Color.Transparent
                                                    }

                                                    val textColor = when {
                                                        isInitial -> Color.White
                                                        value == 0 -> Color.Transparent
                                                        value != solutionBoard!![globalRow][globalCol] -> Color(0xFFE53935) // Red for mistake
                                                        else -> Color(0xFFFFC107)
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight()
                                                            .background(bgColor)
                                                            .border(0.5.dp, Color(0xFF333333))
                                                            .clickable {
                                                                if (!isWon) {
                                                                    selectedCell = Pair(globalRow, globalCol)
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = if (value == 0) "" else value.toString(),
                                                            color = textColor,
                                                            fontSize = 20.sp,
                                                            fontWeight = if (isInitial) FontWeight.ExtraBold else FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Status
            if (isWon) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFC107)),
                    border = BorderStroke(2.dp, Color(0xFFFFC107)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "🎉 Puzzle Solved! 🎉",
                        color = Color(0xFFFFC107),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            } else if (isGameOver) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33E53935)),
                    border = BorderStroke(2.dp, Color(0xFFE53935)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = "Game Over!",
                            color = Color(0xFFE53935),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("You made $maxMistakes mistakes.", color = Color.LightGray, fontSize = 14.sp)
                    }
                }
            } else {
                // Mistakes and Undo Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mistakes: $mistakes / $maxMistakes",
                        color = if (mistakes == maxMistakes) Color(0xFFE53935) else Color.LightGray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { undoMove() },
                        enabled = moveHistory.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A2A2A),
                            disabledContainerColor = Color(0xFF1A1A1A)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("↩ Undo", color = if (moveHistory.isNotEmpty()) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold)
                    }
                }

                // Number Pad
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 1..5) {
                            Button(
                                onClick = { onNumberPadClick(i) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(i.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 6..9) {
                            Button(
                                onClick = { onNumberPadClick(i) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(i.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        // Erase Button
                        Button(
                            onClick = { onNumberPadClick(0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("⌫", color = Color(0xFFE53935), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showRulesDialog) {
        HowToPlayDialog(
            gameTitle = "Sudoku",
            rulesText = "• Fill the 9x9 grid so every row, column, and 3x3 box contains digits 1-9 exactly once.\n• Select an empty cell and tap a number to fill it.\n• You can only make 3 mistakes per match before it's Game Over! (Wrong numbers turn Red).\n• Use the Undo button to revert your last move.",
            onDismiss = { showRulesDialog = false }
        )
    }
}
