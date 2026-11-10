package com.ramanstudio.games

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.min
import kotlin.random.Random

// --- CUSTOM ANDROID VIEW FOR DOTS & BOXES ---
class DotsAndBoxesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var gridSize = 4 // 3x3, 4x4, 5x5, 6x6 boxes
        private set
    var gameMode = 0 // 0 = Pass&Play, 1 = Easy AI, 2 = Medium AI, 3 = Hard AI
    
    var isPlayer1Turn = true
        private set
    var player1Score = 0
        private set
    var player2Score = 0
        private set

    // Grid data arrays: 0 = empty, 1 = player 1, 2 = player 2
    private var hLines = Array(gridSize + 1) { IntArray(gridSize) }
    private var vLines = Array(gridSize) { IntArray(gridSize + 1) }
    private var boxes = Array(gridSize) { IntArray(gridSize) }

    // UI & Drawing Settings
    private var dotRadius = 14f
    private var spacing = 0f
    private var startX = 0f
    private var startY = 0f
    private var isAiThinking = false

    var onGameStateChanged: ((Int, Int, Boolean) -> Unit)? = null

    private val dotPaint = Paint().apply { color = AndroidColor.parseColor("#AAAAAA"); isAntiAlias = true }
    private val p1LinePaint = Paint().apply { color = AndroidColor.parseColor("#E57373"); strokeWidth = 16f; isAntiAlias = true; strokeCap = Paint.Cap.ROUND }
    private val p2LinePaint = Paint().apply { color = AndroidColor.parseColor("#64B5F6"); strokeWidth = 16f; isAntiAlias = true; strokeCap = Paint.Cap.ROUND }
    
    private val boxP1Paint = Paint().apply { color = AndroidColor.parseColor("#55E57373") }
    private val boxP2Paint = Paint().apply { color = AndroidColor.parseColor("#5564B5F6") }

    fun setGridSize(newSize: Int) {
        gridSize = newSize
        hLines = Array(gridSize + 1) { IntArray(gridSize) }
        vLines = Array(gridSize) { IntArray(gridSize + 1) }
        boxes = Array(gridSize) { IntArray(gridSize) }
        isPlayer1Turn = true
        player1Score = 0
        player2Score = 0
        isAiThinking = false
        requestLayout()
        invalidate()
        onGameStateChanged?.invoke(player1Score, player2Score, isPlayer1Turn)
    }

    fun resetGame() {
        setGridSize(gridSize)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val boardSize = min(w, h) * 0.85f
        spacing = boardSize / gridSize
        startX = (w - boardSize) / 2
        startY = (h - boardSize) / 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw completed boxes
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (boxes[r][c] != 0) {
                    val paint = if (boxes[r][c] == 1) boxP1Paint else boxP2Paint
                    canvas.drawRect(
                        startX + c * spacing + 6f, startY + r * spacing + 6f,
                        startX + (c + 1) * spacing - 6f, startY + (r + 1) * spacing - 6f, paint
                    )
                }
            }
        }

        // 2. Draw Horizontal Lines
        for (r in 0..gridSize) {
            for (c in 0 until gridSize) {
                val owner = hLines[r][c]
                if (owner != 0) {
                    val paint = if (owner == 1) p1LinePaint else p2LinePaint
                    canvas.drawLine(
                        startX + c * spacing, startY + r * spacing,
                        startX + (c + 1) * spacing, startY + r * spacing, paint
                    )
                }
            }
        }

        // 3. Draw Vertical Lines
        for (r in 0 until gridSize) {
            for (c in 0..gridSize) {
                val owner = vLines[r][c]
                if (owner != 0) {
                    val paint = if (owner == 1) p1LinePaint else p2LinePaint
                    canvas.drawLine(
                        startX + c * spacing, startY + r * spacing,
                        startX + c * spacing, startY + (r + 1) * spacing, paint
                    )
                }
            }
        }

        // 4. Draw Dots on top
        for (r in 0..gridSize) {
            for (c in 0..gridSize) {
                canvas.drawCircle(startX + c * spacing, startY + r * spacing, dotRadius, dotPaint)
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN || isAiThinking) return true
        performClick()

        val x = event.x
        val y = event.y

        val boardWidth = gridSize * spacing
        val boardHeight = gridSize * spacing
        if (x < startX - spacing/2 || x > startX + boardWidth + spacing/2 ||
            y < startY - spacing/2 || y > startY + boardHeight + spacing/2) {
            return true
        }

        // Precise Distance-to-Segment Edge Detection
        var minDistance = Float.MAX_VALUE
        var bestIsHorizontal = false
        var bestR = -1
        var bestC = -1

        // Find closest undrawn horizontal line
        for (r in 0..gridSize) {
            for (c in 0 until gridSize) {
                if (hLines[r][c] != 0) continue
                val lineX1 = startX + c * spacing
                val lineX2 = startX + (c + 1) * spacing
                val lineY = startY + r * spacing

                val clampedX = x.coerceIn(lineX1, lineX2)
                val dist = distSq(x, y, clampedX, lineY)
                if (dist < minDistance) {
                    minDistance = dist
                    bestIsHorizontal = true
                    bestR = r
                    bestC = c
                }
            }
        }

        // Find closest undrawn vertical line
        for (r in 0 until gridSize) {
            for (c in 0..gridSize) {
                if (vLines[r][c] != 0) continue
                val lineY1 = startY + r * spacing
                val lineY2 = startY + (r + 1) * spacing
                val lineX = startX + c * spacing

                val clampedY = y.coerceIn(lineY1, lineY2)
                val dist = distSq(x, y, lineX, clampedY)
                if (dist < minDistance) {
                    minDistance = dist
                    bestIsHorizontal = false
                    bestR = r
                    bestC = c
                }
            }
        }

        if (bestR != -1 && bestC != -1) {
            if (bestIsHorizontal) {
                hLines[bestR][bestC] = if (isPlayer1Turn) 1 else 2
            } else {
                vLines[bestR][bestC] = if (isPlayer1Turn) 1 else 2
            }
            processTurn()
        }
        return true
    }

    private fun distSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return dx * dx + dy * dy
    }

    private fun processTurn() {
        val scored = checkCompletedBoxes()
        invalidate()
        
        onGameStateChanged?.invoke(player1Score, player2Score, isPlayer1Turn)

        if (!scored) {
            isPlayer1Turn = !isPlayer1Turn
        }

        val totalBoxes = gridSize * gridSize
        if (player1Score + player2Score == totalBoxes) {
            return
        }

        if (!isPlayer1Turn && gameMode != 0) {
            isAiThinking = true
            Handler(Looper.getMainLooper()).postDelayed({
                makeAIMove()
                isAiThinking = false
            }, 450)
        }
    }

    private fun checkCompletedBoxes(): Boolean {
        var scoredThisTurn = false
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (boxes[r][c] == 0 && hLines[r][c] != 0 && hLines[r + 1][c] != 0 && vLines[r][c] != 0 && vLines[r][c + 1] != 0) {
                    boxes[r][c] = if (isPlayer1Turn) 1 else 2
                    if (isPlayer1Turn) player1Score++ else player2Score++
                    scoredThisTurn = true
                }
            }
        }
        return scoredThisTurn
    }

    private fun makeAIMove() {
        val safeMoves = mutableListOf<Pair<Boolean, Pair<Int, Int>>>()
        val scoringMoves = mutableListOf<Pair<Boolean, Pair<Int, Int>>>()
        val dangerousMoves = mutableListOf<Pair<Boolean, Pair<Int, Int>>>()

        for (r in 0..gridSize) {
            for (c in 0 until gridSize) {
                if (hLines[r][c] == 0) {
                    hLines[r][c] = 2
                    if (canScoreMove()) {
                        scoringMoves.add(Pair(true, Pair(r, c)))
                    } else if (createsThreeLineBox()) {
                        dangerousMoves.add(Pair(true, Pair(r, c)))
                    } else {
                        safeMoves.add(Pair(true, Pair(r, c)))
                    }
                    hLines[r][c] = 0
                }
            }
        }

        for (r in 0 until gridSize) {
            for (c in 0..gridSize) {
                if (vLines[r][c] == 0) {
                    vLines[r][c] = 2
                    if (canScoreMove()) {
                        scoringMoves.add(Pair(false, Pair(r, c)))
                    } else if (createsThreeLineBox()) {
                        dangerousMoves.add(Pair(false, Pair(r, c)))
                    } else {
                        safeMoves.add(Pair(false, Pair(r, c)))
                    }
                    vLines[r][c] = 0
                }
            }
        }

        val chosenMove = when (gameMode) {
            1 -> {
                val allMoves = scoringMoves + safeMoves + dangerousMoves
                if (allMoves.isNotEmpty()) allMoves.random() else return
            }
            2 -> {
                if (Random.nextFloat() < 0.5f) {
                    val allMoves = scoringMoves + safeMoves + dangerousMoves
                    if (allMoves.isNotEmpty()) allMoves.random() else return
                } else {
                    if (scoringMoves.isNotEmpty()) scoringMoves.random()
                    else if (safeMoves.isNotEmpty()) safeMoves.random()
                    else dangerousMoves.random()
                }
            }
            else -> {
                if (scoringMoves.isNotEmpty()) scoringMoves.random()
                else if (safeMoves.isNotEmpty()) safeMoves.random()
                else dangerousMoves.random()
            }
        }

        if (chosenMove.first) {
            hLines[chosenMove.second.first][chosenMove.second.second] = 2
        } else {
            vLines[chosenMove.second.first][chosenMove.second.second] = 2
        }

        processTurn()
    }

    private fun canScoreMove(): Boolean {
        var scoreCount = 0
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                if (boxes[i][j] == 0 && hLines[i][j] != 0 && hLines[i + 1][j] != 0 && vLines[i][j] != 0 && vLines[i][j + 1] != 0) {
                    scoreCount++
                }
            }
        }
        return scoreCount > 0
    }

    private fun createsThreeLineBox(): Boolean {
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                if (boxes[i][j] == 0) {
                    var lineCount = 0
                    if (hLines[i][j] != 0) lineCount++
                    if (hLines[i + 1][j] != 0) lineCount++
                    if (vLines[i][j] != 0) lineCount++
                    if (vLines[i][j + 1] != 0) lineCount++
                    
                    if (lineCount == 3) return true
                }
            }
        }
        return false
    }
}

// --- COMPOSE SCREEN WRAPPER WITH PRE-GAME SETUP ---
@Composable
fun DotsAndBoxesScreen(onBack: () -> Unit) {
    var gameMode by remember { mutableIntStateOf(0) } // 0 = Pass&Play, 1 = Easy, 2 = Medium, 3 = Hard
    var gridSize by remember { mutableIntStateOf(4) } // 3x3, 4x4, 5x5, 6x6
    var isGameStarted by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    
    var p1Score by remember { mutableIntStateOf(0) }
    var p2Score by remember { mutableIntStateOf(0) }
    var isP1Turn by remember { mutableStateOf(true) }

    var viewRef by remember { mutableStateOf<DotsAndBoxesView?>(null) }

    val totalBoxes = gridSize * gridSize
    val isGameOver = (p1Score + p2Score) >= totalBoxes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
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
                text = "Dots & Boxes",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFC107)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showRulesDialog = true }) {
                    Text("💡", fontSize = 20.sp)
                }
                if (isGameStarted) {
                    Button(
                        onClick = { viewRef?.resetGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("↺ Reset", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { isGameStarted = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("⚙ Setup", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        if (!isGameStarted) {
            // --- PRE-GAME SETUP SCREEN ---
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(0xFFFFC107)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GAME SETUP",
                        color = Color(0xFFFFC107),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Grid Size Selector
                    Text("Select Board Size:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val sizes = listOf("3x3" to 3, "4x4" to 4, "5x5" to 5, "6x6" to 6)
                        sizes.forEach { (label, sz) ->
                            FilterChip(
                                selected = gridSize == sz,
                                onClick = { gridSize = sz },
                                label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFC107),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF2A2A2A),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mode Selector
                    Text("Select Game Mode:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val modes = listOf("👥 Pass & Play" to 0, "🤖 Easy Bot" to 1, "🤖 Medium Bot" to 2, "🤖 Hard Bot" to 3)
                        modes.forEach { (label, modeVal) ->
                            FilterChip(
                                selected = gameMode == modeVal,
                                onClick = { gameMode = modeVal },
                                label = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFC107),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF2A2A2A),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Start Game Button
                    Button(
                        onClick = {
                            isGameStarted = true
                            viewRef?.thisGameSetup(gridSize, gameMode)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("▶ START GAME", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        } else {
            // --- IN-GAME PLAY SCREEN ---

            // Scoreboard Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, if (isP1Turn && !isGameOver) Color(0xFFE57373) else Color.Transparent)
                ) {
                    Text(
                        "🔴 Red: $p1Score",
                        color = Color(0xFFE57373),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, if (!isP1Turn && !isGameOver) Color(0xFF64B5F6) else Color.Transparent)
                ) {
                    val p2Label = if (gameMode == 0) "🔵 Blue: $p2Score" else "🤖 Bot: $p2Score"
                    Text(
                        p2Label,
                        color = Color(0xFF64B5F6),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Status Card / Game Over
            if (isGameOver) {
                val winnerText = when {
                    p1Score > p2Score -> "🎉 Red Wins! 🎉"
                    p2Score > p1Score -> if (gameMode == 0) "🎉 Blue Wins! 🎉" else "🤖 Bot Wins! 🤖"
                    else -> "🤝 It's a Tie! 🤝"
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFC107)),
                    border = BorderStroke(2.dp, Color(0xFFFFC107)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = winnerText,
                        color = Color(0xFFFFC107),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                }
            } else {
                val turnText = if (isP1Turn) "Red's Turn 🔴" else (if (gameMode == 0) "Blue's Turn 🔵" else "Bot's Turn 🤖")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = turnText,
                        color = if (isP1Turn) Color(0xFFE57373) else Color(0xFF64B5F6),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    )
                }
            }

            // Custom Android View embedded in Compose
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF181818), RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFFFFC107), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        DotsAndBoxesView(ctx).apply {
                            thisGameSetup(gridSize, gameMode)
                            onGameStateChanged = { s1, s2, p1 ->
                                p1Score = s1
                                p2Score = s2
                                isP1Turn = p1
                            }
                            viewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { view ->
                    viewRef = view
                }
            }
        }
    }

    if (showRulesDialog) {
        HowToPlayDialog(
            gameTitle = "Dots & Boxes",
            rulesText = "• Players take turns connecting two adjacent dots with a horizontal or vertical line.\n• Tapping anywhere between two dots/boxes places the line on that edge.\n• Lines are color-coded to the player who drew them (🔴 Red vs 🔵 Blue/Bot).\n• Completing the fourth side of a box claims that box, earns a point, and grants an extra turn!",
            onDismiss = { showRulesDialog = false }
        )
    }
}

// Helper extension on DotsAndBoxesView
private fun DotsAndBoxesView.thisGameSetup(sz: Int, mode: Int) {
    this.gameMode = mode
    this.setGridSize(sz)
}
