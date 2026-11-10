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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

enum class ChessMode { VS_HUMAN, VS_COMPUTER }
enum class ChessDifficulty(val label: String) { EASY("Easy"), MEDIUM("Medium"), HARD("Hard") }

enum class PieceColor { WHITE, BLACK }
enum class PieceType(val symbol: String, val value: Int) {
    PAWN("♟", 10),
    KNIGHT("♞", 30),
    BISHOP("♝", 30),
    ROOK("♜", 50),
    QUEEN("♛", 90),
    KING("♚", 1000)
}

data class ChessPiece(val type: PieceType, val color: PieceColor)

data class MoveRecord(
    val from: Pair<Int, Int>,
    val to: Pair<Int, Int>,
    val piece: ChessPiece,
    val captured: ChessPiece? = null,
    val isPawnDoubleStep: Boolean = false
)

@Composable
fun ChessScreen(onBack: () -> Unit) {
    var gameMode by remember { mutableStateOf(ChessMode.VS_COMPUTER) }
    var difficulty by remember { mutableStateOf(ChessDifficulty.MEDIUM) }
    var showRulesDialog by remember { mutableStateOf(false) }

    var board by remember { mutableStateOf(createInitialChessBoard()) }
    var turn by remember { mutableStateOf(PieceColor.WHITE) }
    var selectedPos by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var validMoves by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }

    var capturedWhite by remember { mutableStateOf<List<ChessPiece>>(emptyList()) }
    var capturedBlack by remember { mutableStateOf<List<ChessPiece>>(emptyList()) }
    var statusMessage by remember { mutableStateOf("White's Turn") }

    // Advanced Chess State
    var lastMove by remember { mutableStateOf<MoveRecord?>(null) }
    var whiteKingMoved by remember { mutableStateOf(false) }
    var blackKingMoved by remember { mutableStateOf(false) }
    var whiteRookKingsideMoved by remember { mutableStateOf(false) }
    var whiteRookQueensideMoved by remember { mutableStateOf(false) }
    var blackRookKingsideMoved by remember { mutableStateOf(false) }
    var blackRookQueensideMoved by remember { mutableStateOf(false) }

    fun executeMove(selR: Int, selC: Int, targetR: Int, targetC: Int) {
        val movingPiece = board[selR][selC]!!
        val targetPiece = board[targetR][targetC]
        val newBoard = board.map { row -> row.toMutableList() }.toMutableList()

        var isEnPassantCapture = false

        // Check En Passant
        if (movingPiece.type == PieceType.PAWN && targetC != selC && targetPiece == null) {
            val epRow = if (movingPiece.color == PieceColor.WHITE) targetR + 1 else targetR - 1
            val epPiece = newBoard[epRow][targetC]
            if (epPiece != null && epPiece.type == PieceType.PAWN && epPiece.color != movingPiece.color) {
                newBoard[epRow][targetC] = null
                isEnPassantCapture = true
                if (epPiece.color == PieceColor.WHITE) capturedWhite += epPiece else capturedBlack += epPiece
            }
        }

        // Check Castling
        if (movingPiece.type == PieceType.KING && abs(targetC - selC) == 2) {
            if (targetC == 6) { // Kingside
                val rook = newBoard[selR][7]
                newBoard[selR][5] = rook
                newBoard[selR][7] = null
            } else if (targetC == 2) { // Queenside
                val rook = newBoard[selR][0]
                newBoard[selR][3] = rook
                newBoard[selR][0] = null
            }
        }

        // Standard move
        newBoard[targetR][targetC] = movingPiece
        newBoard[selR][selC] = null

        // Pawn Promotion (Queen)
        if (movingPiece.type == PieceType.PAWN) {
            if ((movingPiece.color == PieceColor.WHITE && targetR == 0) || (movingPiece.color == PieceColor.BLACK && targetR == 7)) {
                newBoard[targetR][targetC] = ChessPiece(PieceType.QUEEN, movingPiece.color)
            }
        }

        // Track Captures
        if (targetPiece != null && !isEnPassantCapture) {
            if (targetPiece.color == PieceColor.WHITE) capturedWhite += targetPiece else capturedBlack += targetPiece
            if (targetPiece.type == PieceType.KING) {
                statusMessage = "${if (turn == PieceColor.WHITE) "White" else "Black"} Wins by King Capture!"
            }
        }

        // Update Movement Flags
        if (movingPiece.type == PieceType.KING) {
            if (movingPiece.color == PieceColor.WHITE) whiteKingMoved = true else blackKingMoved = true
        }
        if (movingPiece.type == PieceType.ROOK) {
            if (selR == 7 && selC == 7) whiteRookKingsideMoved = true
            if (selR == 7 && selC == 0) whiteRookQueensideMoved = true
            if (selR == 0 && selC == 7) blackRookKingsideMoved = true
            if (selR == 0 && selC == 0) blackRookQueensideMoved = true
        }

        val isPawn2Step = movingPiece.type == PieceType.PAWN && abs(targetR - selR) == 2
        lastMove = MoveRecord(Pair(selR, selC), Pair(targetR, targetC), movingPiece, targetPiece, isPawn2Step)

        board = newBoard.map { it.toList() }
        selectedPos = null
        validMoves = emptyList()

        if (!statusMessage.contains("Wins")) {
            turn = if (turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
            val isCheck = isKingInCheck(board, turn)
            statusMessage = if (isCheck) "${if (turn == PieceColor.WHITE) "White" else "Black"} is in CHECK!" else "${if (turn == PieceColor.WHITE) "White" else "Black"}'s Turn"
        }
    }

    // Computer AI Move Effect
    LaunchedEffect(turn, gameMode, statusMessage) {
        if (gameMode == ChessMode.VS_COMPUTER && turn == PieceColor.BLACK && !statusMessage.contains("Wins")) {
            delay(500L) // AI thinking time
            val aiMove = calculateAiChessMove(board, PieceColor.BLACK, difficulty, lastMove)
            if (aiMove != null) {
                executeMove(aiMove.first.first, aiMove.first.second, aiMove.second.first, aiMove.second.second)
            }
        }
    }

    fun selectCell(r: Int, c: Int) {
        if (gameMode == ChessMode.VS_COMPUTER && turn == PieceColor.BLACK) return
        val piece = board[r][c]
        val sel = selectedPos

        if (sel != null) {
            val (selR, selC) = sel
            if (Pair(r, c) in validMoves) {
                executeMove(selR, selC, r, c)
                return
            }
        }

        if (piece != null && piece.color == turn) {
            selectedPos = Pair(r, c)
            validMoves = calculateChessValidMoves(
                board = board,
                r = r,
                c = c,
                lastMove = lastMove,
                whiteKingMoved = whiteKingMoved,
                blackKingMoved = blackKingMoved,
                whiteRookKingsideMoved = whiteRookKingsideMoved,
                whiteRookQueensideMoved = whiteRookQueensideMoved,
                blackRookKingsideMoved = blackRookKingsideMoved,
                blackRookQueensideMoved = blackRookQueensideMoved,
                allowCastling = true
            )
        } else {
            selectedPos = null
            validMoves = emptyList()
        }
    }

    fun resetGame() {
        board = createInitialChessBoard()
        turn = PieceColor.WHITE
        selectedPos = null
        validMoves = emptyList()
        capturedWhite = emptyList()
        capturedBlack = emptyList()
        statusMessage = "White's Turn"
        lastMove = null
        whiteKingMoved = false
        blackKingMoved = false
        whiteRookKingsideMoved = false
        whiteRookQueensideMoved = false
        blackRookKingsideMoved = false
        blackRookQueensideMoved = false
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
                text = "Chess",
                fontSize = 32.sp,
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
                    Text("↺ New", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Mode Switcher
        Row(
            modifier = Modifier
                .background(Color(0xFF2A2826), shape = RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    gameMode = ChessMode.VS_COMPUTER
                    resetGame()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == ChessMode.VS_COMPUTER) Color(0xFFFFC107) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("🤖 vs 👤 Computer", color = if (gameMode == ChessMode.VS_COMPUTER) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    gameMode = ChessMode.VS_HUMAN
                    resetGame()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameMode == ChessMode.VS_HUMAN) Color(0xFFFFC107) else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("👥 Pass & Play", color = if (gameMode == ChessMode.VS_HUMAN) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Difficulty Chips (if VS_COMPUTER)
        AnimatedVisibility(visible = gameMode == ChessMode.VS_COMPUTER) {
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChessDifficulty.entries.forEach { diff ->
                    FilterChip(
                        selected = difficulty == diff,
                        onClick = {
                            difficulty = diff
                            resetGame()
                        },
                        label = { Text(diff.label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
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

        // Status Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = statusMessage,
                color = if (statusMessage.contains("CHECK")) Color(0xFFFF1744) else if (turn == PieceColor.WHITE) Color.White else Color(0xFFFFB74D),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }

        // Captured Black Bar
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Captured: ", fontSize = 11.sp, color = Color.Gray)
            capturedBlack.forEach { piece ->
                RealisticChessPieceView(piece = piece, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 8x8 Classic Wooden Chessboard Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF2E1C0C), shape = RoundedCornerShape(8.dp))
                .border(3.dp, Color(0xFF5D4037), RoundedCornerShape(8.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column {
                for (r in 0 until 8) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        for (c in 0 until 8) {
                            val isLightSquare = (r + c) % 2 == 0
                            val isLastMoveFrom = lastMove?.from == Pair(r, c)
                            val isLastMoveTo = lastMove?.to == Pair(r, c)

                            val squareColor = when {
                                selectedPos == Pair(r, c) -> Color(0xFF80DEEA) // Cyan selection
                                Pair(r, c) in validMoves -> Color(0xFFA5D6A7) // Soft Green target
                                isLastMoveFrom || isLastMoveTo -> Color(0xFFFFF59D) // Soft Yellow last move
                                isLightSquare -> Color(0xFFF0D9B5) // Staunton Light Wood
                                else -> Color(0xFFB58863) // Staunton Dark Walnut Wood
                            }

                            val piece = board[r][c]

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(squareColor)
                                    .border(
                                        width = if (Pair(r, c) in validMoves) 1.5.dp else 0.dp,
                                        color = if (Pair(r, c) in validMoves) Color(0xFF2E7D32) else Color.Transparent
                                    )
                                    .clickable { selectCell(r, c) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece != null) {
                                    RealisticChessPieceView(piece = piece)
                                } else if (Pair(r, c) in validMoves) {
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

        Spacer(modifier = Modifier.height(2.dp))

        // Captured White Bar
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Captured: ", fontSize = 11.sp, color = Color.Gray)
            capturedWhite.forEach { piece ->
                RealisticChessPieceView(piece = piece, modifier = Modifier.size(24.dp))
            }
        }

        if (showRulesDialog) {
            HowToPlayDialog(
                gameTitle = "Chess",
                rulesText = "• Move pieces according to chess rules to capture the opponent's pieces and checkmate the King.\n• Castling: Move King 2 squares towards Rook if neither has moved.\n• En Passant: Capture an enemy pawn diagonally if it moved 2 squares past yours!",
                onDismiss = { showRulesDialog = false }
            )
        }
    }
}

@Composable
fun RealisticChessPieceView(piece: ChessPiece, modifier: Modifier = Modifier.fillMaxSize(0.85f)) {
    val isWhite = piece.color == PieceColor.WHITE

    val badgeBgColor = if (isWhite) Color(0xFFFAFAFA) else Color(0xFF212121)
    val badgeBorderColor = if (isWhite) Color(0xFFBDBDBD) else Color(0xFFFFD700)
    val pieceSymbolColor = if (isWhite) Color(0xFF212121) else Color(0xFFFAFAFA)

    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = badgeBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .padding(2.dp)
            .border(1.5.dp, badgeBorderColor, CircleShape)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = piece.type.symbol,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = pieceSymbolColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Initial Chessboard Setup
fun createInitialChessBoard(): List<List<ChessPiece?>> {
    val b = List(8) { MutableList<ChessPiece?>(8) { null } }

    b[0][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
    b[0][1] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
    b[0][2] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
    b[0][3] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
    b[0][4] = ChessPiece(PieceType.KING, PieceColor.BLACK)
    b[0][5] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
    b[0][6] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
    b[0][7] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
    for (c in 0 until 8) b[1][c] = ChessPiece(PieceType.PAWN, PieceColor.BLACK)

    for (c in 0 until 8) b[6][c] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
    b[7][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
    b[7][1] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
    b[7][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
    b[7][3] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
    b[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE)
    b[7][5] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
    b[7][6] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
    b[7][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)

    return b.map { it.toList() }
}

fun calculateChessValidMoves(
    board: List<List<ChessPiece?>>,
    r: Int,
    c: Int,
    lastMove: MoveRecord?,
    whiteKingMoved: Boolean,
    blackKingMoved: Boolean,
    whiteRookKingsideMoved: Boolean,
    whiteRookQueensideMoved: Boolean,
    blackRookKingsideMoved: Boolean,
    blackRookQueensideMoved: Boolean,
    allowCastling: Boolean = false
): List<Pair<Int, Int>> {
    val piece = board[r][c] ?: return emptyList()
    val moves = mutableListOf<Pair<Int, Int>>()

    fun addMoveIfValid(nr: Int, nc: Int): Boolean {
        if (nr !in 0 until 8 || nc !in 0 until 8) return false
        val target = board[nr][nc]
        if (target == null) {
            moves.add(Pair(nr, nc))
            return true
        } else if (target.color != piece.color) {
            moves.add(Pair(nr, nc))
            return false
        }
        return false
    }

    when (piece.type) {
        PieceType.PAWN -> {
            val dir = if (piece.color == PieceColor.WHITE) -1 else 1
            val startRow = if (piece.color == PieceColor.WHITE) 6 else 1

            if (r + dir in 0 until 8 && board[r + dir][c] == null) {
                moves.add(Pair(r + dir, c))
                if (r == startRow && board[r + 2 * dir][c] == null) {
                    moves.add(Pair(r + 2 * dir, c))
                }
            }
            for (dc in listOf(-1, 1)) {
                val nr = r + dir
                val nc = c + dc
                if (nr in 0 until 8 && nc in 0 until 8) {
                    val target = board[nr][nc]
                    if (target != null && target.color != piece.color) {
                        moves.add(Pair(nr, nc))
                    }
                    // En Passant Rule
                    if (target == null && lastMove != null && lastMove.isPawnDoubleStep && lastMove.to == Pair(r, nc)) {
                        moves.add(Pair(nr, nc))
                    }
                }
            }
        }
        PieceType.KNIGHT -> {
            val offsets = listOf(Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2), Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1))
            for ((dr, dc) in offsets) addMoveIfValid(r + dr, c + dc)
        }
        PieceType.ROOK -> {
            val dirs = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
            for ((dr, dc) in dirs) {
                var nr = r + dr
                var nc = c + dc
                while (addMoveIfValid(nr, nc)) { nr += dr; nc += dc }
            }
        }
        PieceType.BISHOP -> {
            val dirs = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
            for ((dr, dc) in dirs) {
                var nr = r + dr
                var nc = c + dc
                while (addMoveIfValid(nr, nc)) { nr += dr; nc += dc }
            }
        }
        PieceType.QUEEN -> {
            val dirs = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1), Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
            for ((dr, dc) in dirs) {
                var nr = r + dr
                var nc = c + dc
                while (addMoveIfValid(nr, nc)) { nr += dr; nc += dc }
            }
        }
        PieceType.KING -> {
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr != 0 || dc != 0) addMoveIfValid(r + dr, c + dc)
                }
            }

            // Castling Rule (Only evaluated when allowCastling = true)
            if (allowCastling) {
                val isKingMoved = if (piece.color == PieceColor.WHITE) whiteKingMoved else blackKingMoved
                val inCheck = isKingInCheck(board, piece.color)

                if (!isKingMoved && !inCheck) {
                    val row = if (piece.color == PieceColor.WHITE) 7 else 0
                    val ksRookMoved = if (piece.color == PieceColor.WHITE) whiteRookKingsideMoved else blackRookKingsideMoved
                    val qsRookMoved = if (piece.color == PieceColor.WHITE) whiteRookQueensideMoved else blackRookQueensideMoved

                    // Kingside Castling
                    if (!ksRookMoved && board[row][5] == null && board[row][6] == null && board[row][7]?.type == PieceType.ROOK) {
                        moves.add(Pair(row, 6))
                    }
                    // Queenside Castling
                    if (!qsRookMoved && board[row][1] == null && board[row][2] == null && board[row][3] == null && board[row][0]?.type == PieceType.ROOK) {
                        moves.add(Pair(row, 2))
                    }
                }
            }
        }
    }

    return moves
}

fun isKingInCheck(board: List<List<ChessPiece?>>, color: PieceColor): Boolean {
    var kingPos: Pair<Int, Int>? = null
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            val p = board[r][c]
            if (p != null && p.type == PieceType.KING && p.color == color) {
                kingPos = Pair(r, c)
                break
            }
        }
    }
    if (kingPos == null) return false

    val enemyColor = if (color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            val p = board[r][c]
            if (p != null && p.color == enemyColor) {
                val enemyMoves = calculateChessValidMoves(
                    board, r, c, null,
                    whiteKingMoved = true, blackKingMoved = true,
                    whiteRookKingsideMoved = true, whiteRookQueensideMoved = true,
                    blackRookKingsideMoved = true, blackRookQueensideMoved = true,
                    allowCastling = false
                )
                if (kingPos in enemyMoves) return true
            }
        }
    }
    return false
}

// AI Move Generator
fun calculateAiChessMove(
    board: List<List<ChessPiece?>>,
    color: PieceColor,
    difficulty: ChessDifficulty,
    lastMove: MoveRecord?
): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
    val allLegalMoves = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()

    for (r in 0 until 8) {
        for (c in 0 until 8) {
            val p = board[r][c]
            if (p != null && p.color == color) {
                val targets = calculateChessValidMoves(
                    board, r, c, lastMove,
                    whiteKingMoved = false, blackKingMoved = false,
                    whiteRookKingsideMoved = false, whiteRookQueensideMoved = false,
                    blackRookKingsideMoved = false, blackRookQueensideMoved = false
                )
                for (target in targets) {
                    allLegalMoves.add(Pair(Pair(r, c), target))
                }
            }
        }
    }

    if (allLegalMoves.isEmpty()) return null

    when (difficulty) {
        ChessDifficulty.EASY -> {
            val captures = allLegalMoves.filter { board[it.second.first][it.second.second] != null }
            return if (captures.isNotEmpty() && Math.random() < 0.6) captures.random() else allLegalMoves.random()
        }
        ChessDifficulty.MEDIUM, ChessDifficulty.HARD -> {
            var bestMove = allLegalMoves.first()
            var bestScore = Int.MIN_VALUE

            for (move in allLegalMoves) {
                val (_, to) = move
                val targetPiece = board[to.first][to.second]
                var score = targetPiece?.type?.value ?: 0

                if (to.first in 3..4 && to.second in 3..4) score += 5

                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
            }
            return bestMove
        }
    }
}