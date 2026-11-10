package com.ramanstudio.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class LudoColor(val label: String, val bgHex: Color, val tokenHex: Color) {
    RED("Red", Color(0xFFFF1744), Color(0xFFD50000)),
    GREEN("Green", Color(0xFF00E676), Color(0xFF00C853)),
    YELLOW("Yellow", Color(0xFFFFD600), Color(0xFFFFAB00)),
    BLUE("Blue", Color(0xFF2979FF), Color(0xFF2962FF)) // Adjusted to Yellow/Blue for opposite layout
}

// In standard Ludo order is typically: Red -> Green -> Yellow -> Blue (clockwise)
// Or Red(TopLeft), Green(TopRight), Yellow(BottomRight), Blue(BottomLeft).

enum class PlayerType { HUMAN, COMPUTER }
enum class LudoDifficulty { EASY, MEDIUM }
enum class LudoMode { VS_COMPUTER, PASS_AND_PLAY }

data class LudoPlayerConfig(
    val color: LudoColor,
    var type: PlayerType = PlayerType.HUMAN,
    var isEnabled: Boolean = true
)

data class LudoPieceState(
    val id: Int,
    val color: LudoColor,
    val step: Int = -1 // -1 = Base, 0..50 = Common Track, 51..55 = Home Path, 56 = Finished
)

@Composable
fun LudoScreen(onBack: () -> Unit) {
    var numPlayers by remember { mutableIntStateOf(4) }
    var gameMode by remember { mutableStateOf(LudoMode.VS_COMPUTER) }
    var difficulty by remember { mutableStateOf(LudoDifficulty.MEDIUM) }
    var showRulesDialog by remember { mutableStateOf(false) }

    var playerConfigs by remember {
        mutableStateOf(
            listOf(
                LudoPlayerConfig(LudoColor.RED, PlayerType.HUMAN, true),
                LudoPlayerConfig(LudoColor.GREEN, PlayerType.COMPUTER, true),
                LudoPlayerConfig(LudoColor.YELLOW, PlayerType.COMPUTER, true),
                LudoPlayerConfig(LudoColor.BLUE, PlayerType.COMPUTER, true)
            )
        )
    }

    fun updateConfigsForModeAndCount(mode: LudoMode, count: Int) {
        playerConfigs = playerConfigs.mapIndexed { idx, conf ->
            val isEn = idx < count
            val type = if (mode == LudoMode.PASS_AND_PLAY) {
                PlayerType.HUMAN
            } else {
                if (idx == 0) PlayerType.HUMAN else PlayerType.COMPUTER
            }
            conf.copy(isEnabled = isEn, type = type)
        }
    }

    val activeConfigs = playerConfigs.filter { it.isEnabled }
    var turnIndex by remember { mutableIntStateOf(0) }
    val safeActiveIndex = if (activeConfigs.isNotEmpty()) turnIndex % activeConfigs.size else 0
    val currentConfig = activeConfigs.getOrElse(safeActiveIndex) { playerConfigs[0] }

    var diceValue by remember { mutableIntStateOf(1) }
    var isDiceRolled by remember { mutableStateOf(false) }
    var isRollingAnimation by remember { mutableStateOf(false) }
    
    // Sixes tracking
    var consecutiveSixes by remember { mutableIntStateOf(0) }

    var pieces by remember {
        mutableStateOf(
            LudoColor.entries.flatMap { color ->
                List(4) { id -> LudoPieceState(id = id, color = color) }
            }
        )
    }

    var statusMessage by remember { mutableStateOf("${currentConfig.color.label}'s Turn. Roll the Dice!") }

    fun nextTurn(rolledSix: Boolean, capturedEnemy: Boolean) {
        if (rolledSix && consecutiveSixes < 2) {
            consecutiveSixes++
            statusMessage = "${currentConfig.color.label} rolled a 6! Roll again!"
            isDiceRolled = false
        } else if (capturedEnemy) {
            statusMessage = "${currentConfig.color.label} captured a token! Roll again!"
            isDiceRolled = false
        } else {
            consecutiveSixes = 0
            isDiceRolled = false
            if (activeConfigs.isNotEmpty()) {
                turnIndex = (turnIndex + 1) % activeConfigs.size
            }
            val nextConf = activeConfigs.getOrElse(turnIndex % activeConfigs.size.coerceAtLeast(1)) { playerConfigs[0] }
            statusMessage = "${nextConf.color.label}'s (${if (nextConf.type == PlayerType.HUMAN) "Human" else "AI"}) Turn. Roll!"
        }
    }

    fun handlePieceClick(p: LudoPieceState) {
        if (!isDiceRolled || p.color != currentConfig.color || isRollingAnimation) return

        if (p.step == -1) {
            if (diceValue == 6) {
                pieces = pieces.map { if (it.color == p.color && it.id == p.id) it.copy(step = 0) else it }
                nextTurn(rolledSix = true, capturedEnemy = false) // 6 gives extra roll, but we handle consecutive inside nextTurn
                return
            } else return
        }

        val targetStep = p.step + diceValue
        if (targetStep <= 56) {
            var capturedEnemy = false
            val updated = pieces.map { if (it.color == p.color && it.id == p.id) it.copy(step = targetStep) else it }
            
            // Check for captures
            val myTrackPos = getGridPositionForStep(p.color, targetStep)
            
            if (targetStep < 51 && myTrackPos != null && !isSafeStarSquare(myTrackPos)) {
                pieces = updated.map { enemy ->
                    if (enemy.color != p.color && enemy.step in 0..50) {
                        val enemyPos = getGridPositionForStep(enemy.color, enemy.step)
                        if (enemyPos == myTrackPos) {
                            capturedEnemy = true
                            enemy.copy(step = -1) // Send back to base
                        } else enemy
                    } else enemy
                }
            } else {
                pieces = updated
            }

            // Check Win Condition
            val hasWon = pieces.filter { it.color == p.color }.all { it.step == 56 }
            if (hasWon) {
                statusMessage = "🎉 ${p.color.label} Wins the Ludo Match! 🎉"
                return
            }

            nextTurn(rolledSix = (diceValue == 6), capturedEnemy = capturedEnemy)
        }
    }

    val scope = rememberCoroutineScope()

    suspend fun animateDiceRoll() {
        if (isDiceRolled || isRollingAnimation) return
        isRollingAnimation = true
        for (i in 0..8) {
            diceValue = Random.nextInt(1, 7)
            delay(50L)
        }
        isRollingAnimation = false
        isDiceRolled = true

        val myPieces = pieces.filter { it.color == currentConfig.color }
        val validPieces = myPieces.filter { p ->
            if (p.step == -1) diceValue == 6
            else p.step + diceValue <= 56
        }

        if (validPieces.isEmpty()) {
            statusMessage = "${currentConfig.color.label} rolled $diceValue. No moves available."
            delay(800L)
            nextTurn(rolledSix = false, capturedEnemy = false)
        } else {
            statusMessage = "${currentConfig.color.label} rolled $diceValue! Tap token to move."
        }
    }

    // AI Turn logic
    LaunchedEffect(turnIndex, isDiceRolled, currentConfig, statusMessage) {
        if (currentConfig.type == PlayerType.COMPUTER && !statusMessage.contains("Wins")) {
            if (!isDiceRolled && !isRollingAnimation) {
                delay(400L)
                animateDiceRoll()
            } else if (isDiceRolled && !isRollingAnimation) {
                delay(600L)
                val myPieces = pieces.filter { it.color == currentConfig.color }
                val validPieces = myPieces.filter { p ->
                    if (p.step == -1) diceValue == 6 else p.step + diceValue <= 56
                }
                
                if (validPieces.isNotEmpty()) {
                    // Basic AI logic:
                    // 1. If we can capture an enemy, do it.
                    // 2. If we can spawn a piece, do it.
                    // 3. Otherwise move the piece that is furthest along.
                    var chosenPiece: LudoPieceState? = null
                    
                    // Look for captures
                    for (vp in validPieces) {
                        if (vp.step != -1 && vp.step + diceValue <= 50) {
                            val targetPos = getGridPositionForStep(vp.color, vp.step + diceValue)
                            if (targetPos != null && !isSafeStarSquare(targetPos)) {
                                val enemyThere = pieces.any { enemy ->
                                    enemy.color != vp.color && enemy.step in 0..50 && getGridPositionForStep(enemy.color, enemy.step) == targetPos
                                }
                                if (enemyThere) {
                                    chosenPiece = vp
                                    break
                                }
                            }
                        }
                    }
                    
                    if (chosenPiece == null) {
                        chosenPiece = validPieces.firstOrNull { it.step == -1 && diceValue == 6 }
                    }
                    if (chosenPiece == null) {
                        chosenPiece = validPieces.maxByOrNull { it.step }
                    }
                    
                    chosenPiece?.let { handlePieceClick(it) }
                }
            }
        }
    }

    fun resetGame() {
        pieces = LudoColor.entries.flatMap { color -> List(4) { id -> LudoPieceState(id = id, color = color) } }
        diceValue = 1
        isDiceRolled = false
        isRollingAnimation = false
        consecutiveSixes = 0
        turnIndex = 0
        val c = activeConfigs.getOrElse(0) { playerConfigs[0] }
        statusMessage = "${c.color.label}'s Turn. Roll the Dice!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F1F)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("← Menu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }

            Text("LUDO", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 2.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showRulesDialog = true }) { Text("💡", fontSize = 20.sp) }
                Button(
                    onClick = { resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F1F)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) { Text("↺ New", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }

        // Setup Controls (Players Count)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            listOf(2, 3, 4).forEach { count ->
                FilterChip(
                    selected = numPlayers == count,
                    onClick = {
                        numPlayers = count
                        updateConfigsForModeAndCount(gameMode, count)
                        resetGame()
                    },
                    label = { Text("${count} Players", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFC107),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1F1F1F),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Mode Switcher
        Row(
            modifier = Modifier
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(20.dp))
                .padding(4.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    gameMode = LudoMode.VS_COMPUTER
                    updateConfigsForModeAndCount(LudoMode.VS_COMPUTER, numPlayers)
                    resetGame()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == LudoMode.VS_COMPUTER) Color(0xFFFFC107) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("🤖 vs 👤", color = if (gameMode == LudoMode.VS_COMPUTER) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    gameMode = LudoMode.PASS_AND_PLAY
                    updateConfigsForModeAndCount(LudoMode.PASS_AND_PLAY, numPlayers)
                    resetGame()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == LudoMode.PASS_AND_PLAY) Color(0xFFFFC107) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("👥 Pass & Play", color = if (gameMode == LudoMode.PASS_AND_PLAY) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = currentConfig.color.tokenHex),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = statusMessage,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Redesigned Ludo Board Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF2A2A2A), shape = RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF444444), shape = RoundedCornerShape(12.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column {
                for (r in 0 until 15) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        for (c in 0 until 15) {
                            val cellColor = getLudoCellBgColor(r, c)
                            val isStar = isSafeStarSquare(Pair(r, c))

                            // Find all tokens in this square
                            val tokensInCell = pieces.filter { piece ->
                                if (piece.step == -1) getBaseGridPos(piece.color, piece.id) == Pair(r, c)
                                else getGridPositionForStep(piece.color, piece.step) == Pair(r, c)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(cellColor)
                                    .border(0.5.dp, Color(0xFF111111))
                                    .clickable {
                                        // Prioritize moving the current player's token
                                        tokensInCell.firstOrNull { it.color == currentConfig.color }?.let {
                                            handlePieceClick(it)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isStar && tokensInCell.isEmpty()) {
                                    Text("★", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                                }

                                if (tokensInCell.isNotEmpty()) {
                                    // Stacking rendering
                                    val tokenCount = tokensInCell.size
                                    val topToken = tokensInCell.last()
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(if (tokenCount > 1) 0.9f else 0.75f)
                                            .clip(CircleShape)
                                            .background(topToken.color.tokenHex)
                                            .border(1.5.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (tokenCount > 1) {
                                            Text(
                                                text = "$tokenCount",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        } else {
                                            // Highlight if it's movable
                                            val isMovable = topToken.color == currentConfig.color && isDiceRolled
                                            if (isMovable) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
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

        // Animated 3D Dice Roll Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val diceRotation by animateFloatAsState(
                targetValue = if (isRollingAnimation) 720f else 0f,
                animationSpec = tween(durationMillis = 500),
                label = "DiceRoll"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer { rotationZ = diceRotation }
                    .border(2.dp, currentConfig.color.tokenHex, RoundedCornerShape(12.dp))
                    .clickable(enabled = !isDiceRolled && !isRollingAnimation && currentConfig.type == PlayerType.HUMAN) {
                        scope.launch { animateDiceRoll() }
                    }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = getLudoDiceSymbol(diceValue), fontSize = 48.sp, color = Color.Black)
                }
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Button(
                onClick = {
                    scope.launch { animateDiceRoll() }
                },
                enabled = !isDiceRolled && !isRollingAnimation && currentConfig.type == PlayerType.HUMAN,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    disabledContainerColor = Color(0xFF333333)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Text(
                    text = if (isRollingAnimation) "Rolling..." else if (isDiceRolled) "Tap token to move" else "🎲 Roll Dice",
                    color = if (isDiceRolled) Color.Gray else Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }
    }

    if (showRulesDialog) {
        HowToPlayDialog(
            gameTitle = "Ludo",
            rulesText = "• Roll a 6 on the 🎲 dice to bring a token out of your home base onto the start space.\n• Land on an opponent's token to send them back to base (Star spaces are safe).\n• Navigate all 4 tokens around the board into your colored home path to win!",
            onDismiss = { showRulesDialog = false }
        )
    }
}

// ---------------------------------------------------------
// REWRITTEN LUDO ENGINE LOGIC
// ---------------------------------------------------------

fun isSafeStarSquare(pos: Pair<Int, Int>): Boolean {
    return pos in listOf(
        Pair(6, 1), Pair(1, 8), Pair(8, 13), Pair(13, 6), // Start Squares
        Pair(2, 6), Pair(6, 12), Pair(12, 8), Pair(8, 2)  // Common Safe Stars
    )
}

fun getLudoCellBgColor(r: Int, c: Int): Color {
    // Bases (6x6)
    if (r in 0..5 && c in 0..5) return if (r in 1..4 && c in 1..4) Color(0xFFF5F5F5) else LudoColor.RED.bgHex
    if (r in 0..5 && c in 9..14) return if (r in 1..4 && c in 10..13) Color(0xFFF5F5F5) else LudoColor.GREEN.bgHex
    if (r in 9..14 && c in 9..14) return if (r in 10..13 && c in 10..13) Color(0xFFF5F5F5) else LudoColor.YELLOW.bgHex
    if (r in 9..14 && c in 0..5) return if (r in 10..13 && c in 1..4) Color(0xFFF5F5F5) else LudoColor.BLUE.bgHex

    // Center Goal
    if (r in 6..8 && c in 6..8) return Color(0xFF222222)

    // Home Paths
    if (r == 7 && c in 1..5) return LudoColor.RED.bgHex
    if (c == 7 && r in 1..5) return LudoColor.GREEN.bgHex
    if (r == 7 && c in 9..13) return LudoColor.YELLOW.bgHex
    if (c == 7 && r in 9..13) return LudoColor.BLUE.bgHex

    // Start Cells
    if (r == 6 && c == 1) return LudoColor.RED.bgHex
    if (r == 1 && c == 8) return LudoColor.GREEN.bgHex
    if (r == 8 && c == 13) return LudoColor.YELLOW.bgHex
    if (r == 13 && c == 6) return LudoColor.BLUE.bgHex
    
    // Star cells
    if (isSafeStarSquare(Pair(r, c))) return Color(0xFFDDDDDD)

    return Color.White // Common track
}

fun getBaseGridPos(color: LudoColor, id: Int): Pair<Int, Int> {
    return when (color) {
        LudoColor.RED -> when (id) { 0 -> Pair(2, 2); 1 -> Pair(2, 3); 2 -> Pair(3, 2); else -> Pair(3, 3) }
        LudoColor.GREEN -> when (id) { 0 -> Pair(2, 11); 1 -> Pair(2, 12); 2 -> Pair(3, 11); else -> Pair(3, 12) }
        LudoColor.YELLOW -> when (id) { 0 -> Pair(11, 11); 1 -> Pair(11, 12); 2 -> Pair(12, 11); else -> Pair(12, 12) }
        LudoColor.BLUE -> when (id) { 0 -> Pair(11, 2); 1 -> Pair(11, 3); 2 -> Pair(12, 2); else -> Pair(12, 3) }
    }
}

// Fixed track mapping (0..51 for common path, perfectly matching classic Ludo)
fun getGridPositionForStep(color: LudoColor, step: Int): Pair<Int, Int>? {
    if (step < 0) return null

    // Exact clockwise path starting from RED start (6,1)
    val commonTrack = listOf(
        Pair(6, 1), Pair(6, 2), Pair(6, 3), Pair(6, 4), Pair(6, 5), // Red track approaching green
        Pair(5, 6), Pair(4, 6), Pair(3, 6), Pair(2, 6), Pair(1, 6), Pair(0, 6), // Green approach
        Pair(0, 7), Pair(0, 8), // Top turn
        Pair(1, 8), Pair(2, 8), Pair(3, 8), Pair(4, 8), Pair(5, 8), // Green track descending
        Pair(6, 9), Pair(6, 10), Pair(6, 11), Pair(6, 12), Pair(6, 13), Pair(6, 14), // Yellow approach
        Pair(7, 14), Pair(8, 14), // Right turn
        Pair(8, 13), Pair(8, 12), Pair(8, 11), Pair(8, 10), Pair(8, 9), // Yellow track moving left
        Pair(9, 8), Pair(10, 8), Pair(11, 8), Pair(12, 8), Pair(13, 8), Pair(14, 8), // Blue approach
        Pair(14, 7), Pair(14, 6), // Bottom turn
        Pair(13, 6), Pair(12, 6), Pair(11, 6), Pair(10, 6), Pair(9, 6), // Blue track going up
        Pair(8, 5), Pair(8, 4), Pair(8, 3), Pair(8, 2), Pair(8, 1), Pair(8, 0), // Red approach
        Pair(7, 0), Pair(6, 0) // Left turn (back to start - 1)
    )

    // Offset based on starting position in the commonTrack list
    val offset = when (color) {
        LudoColor.RED -> 0        // Starts at index 0
        LudoColor.GREEN -> 13     // Starts at index 13 (1, 8)
        LudoColor.YELLOW -> 26    // Starts at index 26 (8, 13)
        LudoColor.BLUE -> 39      // Starts at index 39 (13, 6)
    }

    if (step <= 50) {
        val idx = (offset + step) % 52
        return commonTrack[idx]
    }

    // Home path (51..55)
    if (step in 51..55) {
        val pathIdx = step - 51
        return when (color) {
            LudoColor.RED -> listOf(Pair(7, 1), Pair(7, 2), Pair(7, 3), Pair(7, 4), Pair(7, 5))[pathIdx]
            LudoColor.GREEN -> listOf(Pair(1, 7), Pair(2, 7), Pair(3, 7), Pair(4, 7), Pair(5, 7))[pathIdx]
            LudoColor.YELLOW -> listOf(Pair(7, 13), Pair(7, 12), Pair(7, 11), Pair(7, 10), Pair(7, 9))[pathIdx]
            LudoColor.BLUE -> listOf(Pair(13, 7), Pair(12, 7), Pair(11, 7), Pair(10, 7), Pair(9, 7))[pathIdx]
        }
    }

    return Pair(7, 7) // Finished (Step 56)
}

fun getLudoDiceSymbol(v: Int): String {
    return when (v) {
        1 -> "⚀"
        2 -> "⚁"
        3 -> "⚂"
        4 -> "⚃"
        5 -> "⚄"
        6 -> "⚅"
        else -> "🎲"
    }
}