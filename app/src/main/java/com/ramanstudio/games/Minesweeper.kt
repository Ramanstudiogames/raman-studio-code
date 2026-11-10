package com.ramanstudio.games

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import kotlin.math.abs

enum class Difficulty(val label: String, val rows: Int, val cols: Int, val mines: Int) {
    EASY("Easy", 8, 8, 10),
    MEDIUM("Medium", 10, 10, 18),
    HARD("Hard", 12, 12, 30)
}

data class CellState(
    val row: Int,
    val col: Int,
    val isMine: Boolean = false,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val neighborMines: Int = 0,
    val isExplodedMine: Boolean = false
)

enum class InteractionMode {
    REVEAL,
    FLAG
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinesweeperScreen(onBack: () -> Unit) {
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var mode by remember { mutableStateOf(InteractionMode.REVEAL) }
    var showRulesDialog by remember { mutableStateOf(false) }

    var grid by remember(difficulty) { mutableStateOf(createEmptyGrid(difficulty.rows, difficulty.cols)) }
    var isFirstTap by remember(difficulty) { mutableStateOf(true) }
    var isGameOver by remember(difficulty) { mutableStateOf(false) }
    var isGameWon by remember(difficulty) { mutableStateOf(false) }
    var timerSeconds by remember(difficulty) { mutableIntStateOf(0) }
    var isTimerRunning by remember(difficulty) { mutableStateOf(false) }

    // Timer effect
    LaunchedEffect(isTimerRunning, isGameOver, isGameWon) {
        if (isTimerRunning && !isGameOver && !isGameWon) {
            while (true) {
                delay(1000L)
                timerSeconds++
            }
        }
    }

    fun resetGame() {
        grid = createEmptyGrid(difficulty.rows, difficulty.cols)
        isFirstTap = true
        isGameOver = false
        isGameWon = false
        timerSeconds = 0
        isTimerRunning = false
    }

    fun handleCellClick(r: Int, c: Int) {
        if (isGameOver || isGameWon) return

        val cell = grid[r][c]

        if (mode == InteractionMode.FLAG) {
            val cellToFlag = grid[r][c]
            if (!cellToFlag.isRevealed) {
                val newGrid = grid.map { rowList -> rowList.toMutableList() }
                newGrid[r][c] = cellToFlag.copy(isFlagged = !cellToFlag.isFlagged)
                grid = newGrid.map { it.toList() }
            }
            return
        }

        // REVEAL MODE
        if (cell.isFlagged || cell.isRevealed) return

        if (isFirstTap) {
            isFirstTap = false
            grid = generateMinesAndNumbers(grid, difficulty.rows, difficulty.cols, difficulty.mines, safeRow = r, safeCol = c)
            isTimerRunning = true
        }

        val currentCell = grid[r][c]
        if (currentCell.isMine) {
            // Hit a mine! Game Over
            isGameOver = true
            isTimerRunning = false
            grid = revealAllMines(grid, explodedRow = r, explodedCol = c)
        } else {
            // Reveal cell & flood fill if 0
            grid = revealCellAndNeighbors(grid, r, c, difficulty.rows, difficulty.cols)
            // Check win condition
            if (checkWinCondition(grid, difficulty.mines)) {
                isGameWon = true
                isTimerRunning = false
                grid = flagAllMines(grid)
            }
        }
    }

    fun handleCellLongClick(r: Int, c: Int) {
        if (isGameOver || isGameWon) return
        val cell = grid[r][c]
        if (cell.isRevealed) return

        val newGrid = grid.map { rowList -> rowList.toMutableList() }
        newGrid[r][c] = cell.copy(isFlagged = !cell.isFlagged)
        grid = newGrid.map { it.toList() }
    }

    val flaggedCount = grid.sumOf { rowList -> rowList.count { it.isFlagged } }
    val remainingMines = maxOf(0, difficulty.mines - flaggedCount)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Bar
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

            Text(
                text = "Minesweeper",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFC107)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showRulesDialog = true }) {
                    Text("💡", fontSize = 20.sp)
                }
                Button(
                    onClick = { resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isGameOver) "😵" else if (isGameWon) "😎" else "🙂",
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Difficulty Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Difficulty.entries.forEach { diff ->
                Button(
                    onClick = {
                        difficulty = diff
                        resetGame()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (difficulty == diff) Color(0xFFFFC107) else Color(0xFF2A2826)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = diff.label,
                        color = if (difficulty == diff) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Status Bar (Mines Left, Timer, Mode Switch)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mines Counter
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "🚩 $remainingMines",
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Mode Selector Toggle
            Row(
                modifier = Modifier
                    .background(Color(0xFF2A2826), shape = RoundedCornerShape(20.dp))
                    .padding(2.dp)
            ) {
                Button(
                    onClick = { mode = InteractionMode.REVEAL },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mode == InteractionMode.REVEAL) Color(0xFFFFC107) else Color.Transparent
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("⛏️ Dig", color = if (mode == InteractionMode.REVEAL) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { mode = InteractionMode.FLAG },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mode == InteractionMode.FLAG) Color(0xFFFF5252) else Color.Transparent
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("🚩 Flag", color = if (mode == InteractionMode.FLAG) Color.White else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Timer Counter
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "⏱️ ${timerSeconds}s",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Minesweeper Grid Board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (r in 0 until difficulty.rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in 0 until difficulty.cols) {
                            val cell = grid[r][c]
                            MinesweeperCell(
                                cell = cell,
                                onClick = { handleCellClick(r, c) },
                                onLongClick = { handleCellLongClick(r, c) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }

            // Game Won Overlay
            if (isGameWon) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE121212)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉 You Won! 🎉", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFC107))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Time: ${timerSeconds}s", fontSize = 16.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { resetGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                        ) {
                            Text("Play Again", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
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
                        Text("💥 Game Over! 💥", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("You hit a mine!", fontSize = 16.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { resetGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                        ) {
                            Text("Try Again", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tip: Tap to dig, toggle Flag mode or long-press to flag mines!",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }

    if (showRulesDialog) {
        HowToPlayDialog(
            gameTitle = "Minesweeper",
            rulesText = "• Tap unrevealed tiles to dig them up without exploding a bomb.\n• Numbers tell you how many hidden mines are in adjacent surrounding cells.\n• Use Flag mode or long-press to mark suspected mine locations!",
            onDismiss = { showRulesDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinesweeperCell(
    cell: CellState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        cell.isExplodedMine -> Color(0xFFFF1744)
        cell.isRevealed -> if (cell.isMine) Color(0xFFD32F2F) else Color(0xFF333333)
        else -> Color(0xFF424242)
    }

    Box(
        modifier = modifier
            .background(backgroundColor, shape = RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = if (cell.isRevealed) Color(0xFF222222) else Color(0xFF555555),
                shape = RoundedCornerShape(4.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            cell.isFlagged -> {
                Text(text = "🚩", fontSize = 14.sp)
            }
            cell.isRevealed -> {
                if (cell.isMine) {
                    Text(text = "💣", fontSize = 14.sp)
                } else if (cell.neighborMines > 0) {
                    val numColor = getNumberColor(cell.neighborMines)
                    Text(
                        text = cell.neighborMines.toString(),
                        color = numColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

fun getNumberColor(count: Int): Color {
    return when (count) {
        1 -> Color(0xFF2196F3)
        2 -> Color(0xFF4CAF50)
        3 -> Color(0xFFF44336)
        4 -> Color(0xFF9C27B0)
        5 -> Color(0xFFFF9800)
        6 -> Color(0xFF00BCD4)
        7 -> Color(0xFFE91E63)
        else -> Color(0xFFFFFFFF)
    }
}

fun createEmptyGrid(rows: Int, cols: Int): List<List<CellState>> {
    return List(rows) { r ->
        List(cols) { c ->
            CellState(row = r, col = c)
        }
    }
}

fun generateMinesAndNumbers(
    emptyGrid: List<List<CellState>>,
    rows: Int,
    cols: Int,
    mineCount: Int,
    safeRow: Int,
    safeCol: Int
): List<List<CellState>> {
    val newGrid = emptyGrid.map { rowList -> rowList.toMutableList() }

    val validPositions = mutableListOf<Pair<Int, Int>>()
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (abs(r - safeRow) <= 1 && abs(c - safeCol) <= 1) {
                continue
            }
            validPositions.add(Pair(r, c))
        }
    }

    if (validPositions.size < mineCount) {
        validPositions.clear()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (r == safeRow && c == safeCol) continue
                validPositions.add(Pair(r, c))
            }
        }
    }

    validPositions.shuffle()
    val minePositions = validPositions.take(mineCount).toSet()

    for ((r, c) in minePositions) {
        newGrid[r][c] = newGrid[r][c].copy(isMine = true)
    }

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (!newGrid[r][c].isMine) {
                var count = 0
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0 until rows && nc in 0 until cols && newGrid[nr][nc].isMine) {
                            count++
                        }
                    }
                }
                newGrid[r][c] = newGrid[r][c].copy(neighborMines = count)
            }
        }
    }

    return newGrid.map { it.toList() }
}

fun revealCellAndNeighbors(
    grid: List<List<CellState>>,
    startR: Int,
    startC: Int,
    rows: Int,
    cols: Int
): List<List<CellState>> {
    val mutableGrid = grid.map { rowList -> rowList.toMutableList() }
    val queue = ArrayDeque<Pair<Int, Int>>()
    queue.add(Pair(startR, startC))

    while (queue.isNotEmpty()) {
        val (r, c) = queue.removeFirst()
        val cell = mutableGrid[r][c]

        if (cell.isRevealed || cell.isFlagged) continue

        mutableGrid[r][c] = cell.copy(isRevealed = true)

        if (cell.neighborMines == 0 && !cell.isMine) {
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0 until rows && nc in 0 until cols) {
                        val neighbor = mutableGrid[nr][nc]
                        if (!neighbor.isRevealed && !neighbor.isFlagged && !neighbor.isMine) {
                            queue.add(Pair(nr, nc))
                        }
                    }
                }
            }
        }
    }

    return mutableGrid.map { it.toList() }
}

fun revealAllMines(grid: List<List<CellState>>, explodedRow: Int, explodedCol: Int): List<List<CellState>> {
    return grid.mapIndexed { r, rowList ->
        rowList.mapIndexed { c, cell ->
            if (cell.isMine) {
                cell.copy(isRevealed = true, isExplodedMine = (r == explodedRow && c == explodedCol))
            } else {
                cell
            }
        }
    }
}

fun flagAllMines(grid: List<List<CellState>>): List<List<CellState>> {
    return grid.map { rowList ->
        rowList.map { cell ->
            if (cell.isMine) cell.copy(isFlagged = true) else cell
        }
    }
}

fun checkWinCondition(grid: List<List<CellState>>, mineCount: Int): Boolean {
    var unrevealedNonMines = 0
    for (row in grid) {
        for (cell in row) {
            if (!cell.isMine && !cell.isRevealed) {
                unrevealedNonMines++
            }
        }
    }
    return unrevealedNonMines == 0
}