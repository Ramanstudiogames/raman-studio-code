package com.ramanstudio.games

import androidx.compose.foundation.BorderStroke
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class NonogramDifficulty(val label: String, val size: Int) {
    EASY("Easy", 5),
    MEDIUM("Medium", 7),
    HARD("Hard", 10),
    EXTREME("Extreme", 12)
}

enum class NonogramTool { FILL, CROSS, ERASE }

object NonogramGenerator {
    suspend fun generate(difficulty: NonogramDifficulty): NonogramPuzzle {
        return withContext(Dispatchers.Default) {
            val size = difficulty.size
            val solution = Array(size) { IntArray(size) }

            for (r in 0 until size) {
                for (c in 0 until size) {
                    solution[r][c] = if (Random.nextFloat() < 0.52f) 1 else 0
                }
            }

            val rowClues = Array(size) { r ->
                calculateClues(solution[r].toList())
            }.toList()

            val colClues = Array(size) { c ->
                val colList = (0 until size).map { r -> solution[r][c] }
                calculateClues(colList)
            }.toList()

            NonogramPuzzle(
                size = size,
                solution = solution.map { it.toList() },
                rowClues = rowClues,
                colClues = colClues
            )
        }
    }

    private fun calculateClues(line: List<Int>): List<Int> {
        val clues = mutableListOf<Int>()
        var count = 0
        for (cell in line) {
            if (cell == 1) {
                count++
            } else {
                if (count > 0) {
                    clues.add(count)
                    count = 0
                }
            }
        }
        if (count > 0) {
            clues.add(count)
        }
        return if (clues.isEmpty()) listOf(0) else clues
    }
}

data class NonogramPuzzle(
    val size: Int,
    val solution: List<List<Int>>,
    val rowClues: List<List<Int>>,
    val colClues: List<List<Int>>
)

@Composable
fun NonogramScreen(onBack: () -> Unit) {
    var difficulty by remember { mutableStateOf(NonogramDifficulty.MEDIUM) }
    var showRulesDialog by remember { mutableStateOf(false) }

    var puzzle by remember { mutableStateOf<NonogramPuzzle?>(null) }
    var userBoard by remember { mutableStateOf<List<MutableList<Int>>?>(null) }
    var selectedTool by remember { mutableStateOf(NonogramTool.FILL) }
    
    var isGenerating by remember { mutableStateOf(true) }
    var isWon by remember { mutableStateOf(false) }
    var moveHistory by remember { mutableStateOf(listOf<Triple<Int, Int, Int>>()) }

    val scope = rememberCoroutineScope()

    fun generateNewGame() {
        isGenerating = true
        isWon = false
        moveHistory = emptyList()
        scope.launch {
            val newPuzzle = NonogramGenerator.generate(difficulty)
            puzzle = newPuzzle
            userBoard = List(newPuzzle.size) { MutableList(newPuzzle.size) { 0 } }
            isGenerating = false
        }
    }

    LaunchedEffect(difficulty) {
        generateNewGame()
    }

    fun onCellClick(r: Int, c: Int) {
        if (isWon || isGenerating || puzzle == null || userBoard == null) return
        val currentVal = userBoard!![r][c]
        
        val newVal = when (selectedTool) {
            NonogramTool.FILL -> if (currentVal == 1) 0 else 1
            NonogramTool.CROSS -> if (currentVal == -1) 0 else -1
            NonogramTool.ERASE -> 0
        }

        if (currentVal == newVal) return

        val newBoard = userBoard!!.map { it.toMutableList() }.toMutableList()
        moveHistory = moveHistory + Triple(r, c, currentVal)
        newBoard[r][c] = newVal
        userBoard = newBoard

        // Check Win
        val currentFilled = newBoard.map { row -> row.map { if (it == 1) 1 else 0 } }
        if (currentFilled == puzzle!!.solution) {
            isWon = true
        }
    }

    fun undoMove() {
        if (moveHistory.isNotEmpty() && !isWon && !isGenerating && userBoard != null) {
            val lastMove = moveHistory.last()
            moveHistory = moveHistory.dropLast(1)
            val (r, c, prevVal) = lastMove
            val newBoard = userBoard!!.map { it.toMutableList() }.toMutableList()
            newBoard[r][c] = prevVal
            userBoard = newBoard
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
                text = "Nonogram",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFC107)
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            NonogramDifficulty.entries.forEach { diff ->
                FilterChip(
                    selected = difficulty == diff,
                    onClick = { difficulty = diff },
                    label = { Text(diff.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFC107),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E1E1E),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }

        if (isGenerating || puzzle == null || userBoard == null) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFFC107))
            }
        } else {
            val size = puzzle!!.size
            val rowClues = puzzle!!.rowClues
            val colClues = puzzle!!.colClues

            // Tool Selector & Undo Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { selectedTool = NonogramTool.FILL },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTool == NonogramTool.FILL) Color(0xFFFFC107) else Color(0xFF1E1E1E)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("■ Fill", color = if (selectedTool == NonogramTool.FILL) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { selectedTool = NonogramTool.CROSS },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTool == NonogramTool.CROSS) Color(0xFFFFC107) else Color(0xFF1E1E1E)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("❌ Cross", color = if (selectedTool == NonogramTool.CROSS) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { selectedTool = NonogramTool.ERASE },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTool == NonogramTool.ERASE) Color(0xFFFFC107) else Color(0xFF1E1E1E)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("🧹 Erase", color = if (selectedTool == NonogramTool.ERASE) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { undoMove() },
                    enabled = moveHistory.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A2A),
                        disabledContainerColor = Color(0xFF151515)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("↩ Undo", color = if (moveHistory.isNotEmpty()) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold)
                }
            }

            // Picross Board Display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF181818), RoundedCornerShape(12.dp))
                    .border(2.dp, Color(0xFFFFC107), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val maxColClueSize = colClues.maxOf { it.size }
                    val maxRowClueSize = rowClues.maxOf { it.size }

                    // Top header for column clues
                    for (clueRowIdx in 0 until maxColClueSize) {
                        Row {
                            Spacer(modifier = Modifier.width((maxRowClueSize * 16).dp))
                            for (c in 0 until size) {
                                val cluesForCol = colClues[c]
                                val offset = maxColClueSize - cluesForCol.size
                                val clueText = if (clueRowIdx >= offset) cluesForCol[clueRowIdx - offset].toString() else ""
                                
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(1.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Text(
                                        text = clueText,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rows with row clues + cells
                    for (r in 0 until size) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val cluesForRow = rowClues[r]
                            val paddedClues = List(maxRowClueSize - cluesForRow.size) { "" } + cluesForRow.map { it.toString() }
                            
                            Row(
                                modifier = Modifier.width((maxRowClueSize * 16).dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                paddedClues.forEach { clueStr ->
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp, 24.dp)
                                            .padding(end = 2.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = clueStr,
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Cells in row
                            for (c in 0 until size) {
                                val cellState = userBoard!![r][c]
                                val cellBg = when (cellState) {
                                    1 -> Color(0xFFFFC107)
                                    -1 -> Color(0xFF333333)
                                    else -> Color(0xFF222222)
                                }
                                val cellText = if (cellState == -1) "✕" else ""

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(0.5.dp)
                                        .background(cellBg, RoundedCornerShape(2.dp))
                                        .border(0.5.dp, Color(0xFF444444), RoundedCornerShape(2.dp))
                                        .clickable { onCellClick(r, c) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cellText,
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isWon) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFC107)),
                    border = BorderStroke(2.dp, Color(0xFFFFC107)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "🎉 Picture Solved! 🎉",
                        color = Color(0xFFFFC107),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                    )
                }
            }
        }
    }

    if (showRulesDialog) {
        HowToPlayDialog(
            gameTitle = "Nonogram (Picross)",
            rulesText = "• Fill in grid cells based on the number clues given on the top and left sides.\n• Each number represents consecutive colored blocks in that row or column.\n• Multiple numbers mean there is at least one blank space between the blocks.\n• Use the Fill (■) tool, Cross (❌) tool, and Undo button if needed!",
            onDismiss = { showRulesDialog = false }
        )
    }
}
