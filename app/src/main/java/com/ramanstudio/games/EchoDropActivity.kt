package com.ramanstudio.games

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.random.Random

class EchoDropActivity : ComponentActivity() {

    private val rows = 7
    private val cols = 5

    // --- 1. Game State & Turn Management ---
    private val board = Array(rows) { IntArray(cols) { 0 } }
    private val isHidden = Array(rows) { BooleanArray(cols) { false } }

    private var player1Score = 0
    private var player2Score = 0
    private var isPlayer1Turn = true

    // 2. Game Mode Selection (0 = Pass & Play, 1 = Easy AI, 2 = Medium AI, 3 = Hard AI)
    private var gameMode = 1
    private var nextTileValue = 2
    private var isProcessingMove = false

    // UI Elements
    private lateinit var tvP1Score: TextView
    private lateinit var tvP2Score: TextView
    private lateinit var tvNextTile: TextView
    private lateinit var tvTurnStatus: TextView
    private lateinit var gridBoard: GridLayout
    private lateinit var layoutSetup: LinearLayout

    private lateinit var btnBack: Button
    private lateinit var btnRestart: Button
    private lateinit var btnSetupBack: Button
    private lateinit var btnStartGame: Button

    private lateinit var btnModePassPlay: Button
    private lateinit var btnModeEasyAI: Button
    private lateinit var btnModeMediumAI: Button
    private lateinit var btnModeHardAI: Button

    private val dropButtons = ArrayList<Button>()
    private val cellViews = Array(rows) { arrayOfNulls<TextView>(cols) }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_echo_drop)

        tvP1Score = findViewById(R.id.tvP1Score)
        tvP2Score = findViewById(R.id.tvP2Score)
        tvNextTile = findViewById(R.id.tvNextTile)
        tvTurnStatus = findViewById(R.id.tvTurnStatus)
        gridBoard = findViewById(R.id.gridBoard)
        layoutSetup = findViewById(R.id.layoutSetup)

        btnBack = findViewById(R.id.btnBack)
        btnRestart = findViewById(R.id.btnRestart)
        btnSetupBack = findViewById(R.id.btnSetupBack)
        btnStartGame = findViewById(R.id.btnStartGame)

        btnModePassPlay = findViewById(R.id.btnModePassPlay)
        btnModeEasyAI = findViewById(R.id.btnModeEasyAI)
        btnModeMediumAI = findViewById(R.id.btnModeMediumAI)
        btnModeHardAI = findViewById(R.id.btnModeHardAI)

        dropButtons.add(findViewById(R.id.btnDrop0))
        dropButtons.add(findViewById(R.id.btnDrop1))
        dropButtons.add(findViewById(R.id.btnDrop2))
        dropButtons.add(findViewById(R.id.btnDrop3))
        dropButtons.add(findViewById(R.id.btnDrop4))

        for (c in 0 until cols) {
            dropButtons[c].setOnClickListener {
                if (!isProcessingMove && (isPlayer1Turn || gameMode == 0)) {
                    dropTile(c)
                }
            }
        }

        btnBack.setOnClickListener { finish() }
        btnSetupBack.setOnClickListener { finish() }
        btnRestart.setOnClickListener { resetGame() }

        btnModePassPlay.setOnClickListener { updateModeSelection(0) }
        btnModeEasyAI.setOnClickListener { updateModeSelection(1) }
        btnModeMediumAI.setOnClickListener { updateModeSelection(2) }
        btnModeHardAI.setOnClickListener { updateModeSelection(3) }

        btnStartGame.setOnClickListener {
            layoutSetup.visibility = View.GONE
            resetGame()
        }

        initBoardGrid()
        updateModeSelection(1) // Default Easy AI
        resetGame()
    }

    private fun updateModeSelection(mode: Int) {
        gameMode = mode
        val activeBg = ColorStateList.valueOf(Color.parseColor("#FFC107"))
        val inactiveBg = ColorStateList.valueOf(Color.parseColor("#2A2A2A"))

        btnModePassPlay.backgroundTintList = if (mode == 0) activeBg else inactiveBg
        btnModePassPlay.setTextColor(if (mode == 0) Color.BLACK else Color.WHITE)

        btnModeEasyAI.backgroundTintList = if (mode == 1) activeBg else inactiveBg
        btnModeEasyAI.setTextColor(if (mode == 1) Color.BLACK else Color.WHITE)

        btnModeMediumAI.backgroundTintList = if (mode == 2) activeBg else inactiveBg
        btnModeMediumAI.setTextColor(if (mode == 2) Color.BLACK else Color.WHITE)

        btnModeHardAI.backgroundTintList = if (mode == 3) activeBg else inactiveBg
        btnModeHardAI.setTextColor(if (mode == 3) Color.BLACK else Color.WHITE)
    }

    private fun initBoardGrid() {
        gridBoard.removeAllViews()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val tv = TextView(this).apply {
                    val params = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        rowSpec = GridLayout.spec(r, 1f)
                        columnSpec = GridLayout.spec(c, 1f)
                        setMargins(4, 4, 4, 4)
                    }
                    layoutParams = params
                    gravity = Gravity.CENTER
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                    setBackgroundColor(Color.parseColor("#222222"))
                }
                cellViews[r][c] = tv
                gridBoard.addView(tv)
            }
        }
    }

    private fun generateNextTileValue() {
        val options = listOf(2, 2, 4, 4, 8)
        nextTileValue = options.random()
        tvNextTile.text = nextTileValue.toString()
        tvNextTile.setBackgroundColor(getTileColor(nextTileValue))
    }

    private fun resetGame() {
        handler.removeCallbacksAndMessages(null)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                board[r][c] = 0
                isHidden[r][c] = false
            }
        }
        player1Score = 0
        player2Score = 0
        isPlayer1Turn = true
        isProcessingMove = false

        generateNextTileValue()
        updateUI()
    }

    private fun dropTile(col: Int) {
        if (isProcessingMove) return

        // Check if column is full
        if (board[0][col] != 0) {
            triggerGameOver("Column $col is full!")
            return
        }

        isProcessingMove = true

        // Find lowest available row in column
        var targetRow = -1
        for (r in rows - 1 downTo 0) {
            if (board[r][col] == 0) {
                targetRow = r
                break
            }
        }

        if (targetRow != -1) {
            board[targetRow][col] = nextTileValue
            isHidden[targetRow][col] = false

            // Process cascades & merges
            processCascadesAndMerges()

            generateNextTileValue()
            updateUI()

            // 1.5-second UI reveal, then hide tiles & switch turn
            handler.postDelayed({
                hideBoardTiles()
                switchTurn()
            }, 1500)
        }
    }

    private fun hideBoardTiles() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (board[r][c] != 0) {
                    isHidden[r][c] = true
                }
            }
        }
        updateUI()
    }

    private fun switchTurn() {
        isPlayer1Turn = !isPlayer1Turn
        isProcessingMove = false
        updateUI()

        // Check Game Over (Board full)
        if (board.all { row -> row.all { it != 0 } }) {
            triggerGameOver("Board is completely full!")
            return
        }

        // Trigger AI Turn if Player 2 is AI
        if (!isPlayer1Turn && gameMode > 0) {
            handler.postDelayed({
                val aiCol = calculateAIMove()
                dropTile(aiCol)
            }, 600)
        }
    }

    private fun processCascadesAndMerges() {
        var mergedInPass: Boolean
        do {
            mergedInPass = false

            // 1. Gravity Fall
            for (c in 0 until cols) {
                for (r in rows - 1 downTo 0) {
                    if (board[r][c] == 0) {
                        for (aboveR in r - 1 downTo 0) {
                            if (board[aboveR][c] != 0) {
                                board[r][c] = board[aboveR][c]
                                isHidden[r][c] = isHidden[aboveR][c]
                                board[aboveR][c] = 0
                                isHidden[aboveR][c] = false
                                break
                            }
                        }
                    }
                }
            }

            // 2. Merges (Adjacent matching tiles)
            val mergedFlags = Array(rows) { BooleanArray(cols) { false } }
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val val1 = board[r][c]
                    if (val1 != 0 && !mergedFlags[r][c]) {
                        // Check Down
                        if (r + 1 < rows && board[r + 1][c] == val1 && !mergedFlags[r + 1][c]) {
                            val newMergedVal = val1 * 2
                            board[r][c] = newMergedVal
                            board[r + 1][c] = 0
                            mergedFlags[r][c] = true
                            mergedInPass = true
                            addPointsToCurrentPlayer(newMergedVal)
                        }
                        // Check Right
                        else if (c + 1 < cols && board[r][c + 1] == val1 && !mergedFlags[r][c + 1]) {
                            val newMergedVal = val1 * 2
                            board[r][c] = newMergedVal
                            board[r][c + 1] = 0
                            mergedFlags[r][c] = true
                            mergedInPass = true
                            addPointsToCurrentPlayer(newMergedVal)
                        }
                    }
                }
            }
        } while (mergedInPass)
    }

    private fun addPointsToCurrentPlayer(points: Int) {
        if (isPlayer1Turn) {
            player1Score += points
        } else {
            player2Score += points
        }
    }

    // --- 3. AI Bot Logic (The "Memory" System) ---
    private fun calculateAIMove(): Int {
        val availableCols = (0 until cols).filter { board[0][it] == 0 }
        if (availableCols.isEmpty()) return 0

        return when (gameMode) {
            1 -> calculateEasyAIMove(availableCols)
            2 -> calculateMediumAIMove(availableCols)
            else -> calculateHardAIMove(availableCols)
        }
    }

    /**
     * EASY AI (Zero Memory):
     * Reads board ONLY where isHidden == false. Treats hidden tiles as 0.
     * Picks column that merges with a visible tile if possible, otherwise random.
     */
    private fun calculateEasyAIMove(availableCols: List<Int>): Int {
        val visibleBoard = Array(rows) { r ->
            IntArray(cols) { c ->
                if (!isHidden[r][c]) board[r][c] else 0
            }
        }

        for (c in availableCols) {
            var targetRow = -1
            for (r in rows - 1 downTo 0) {
                if (visibleBoard[r][c] == 0) {
                    targetRow = r
                    break
                }
            }
            if (targetRow != -1) {
                // Check if adjacent visible cells match nextTileValue
                if (targetRow + 1 < rows && visibleBoard[targetRow + 1][c] == nextTileValue) return c
                if (c - 1 >= 0 && visibleBoard[targetRow][c - 1] == nextTileValue) return c
                if (c + 1 < cols && visibleBoard[targetRow][c + 1] == nextTileValue) return c
            }
        }
        return availableCols.random()
    }

    /**
     * MEDIUM AI (Short-Term Memory):
     * Remembers ONLY the top-most tile of every column, regardless of isHidden.
     * Calculates best column to drop "Next Tile" based on highest immediate merge value with top tiles.
     */
    private fun calculateMediumAIMove(availableCols: List<Int>): Int {
        var bestCol = availableCols.random()
        var maxScorePrediction = -1

        for (c in availableCols) {
            var topRow = -1
            for (r in 0 until rows) {
                if (board[r][c] != 0) {
                    topRow = r
                    break
                }
            }

            if (topRow != -1) {
                val topValue = board[topRow][c]
                if (topValue == nextTileValue) {
                    val scorePred = nextTileValue * 2
                    if (scorePred > maxScorePrediction) {
                        maxScorePrediction = scorePred
                        bestCol = c
                    }
                }
            }
        }
        return bestCol
    }

    /**
     * HARD AI (Perfect Memory):
     * Reads actual board array completely, ignoring isHidden.
     * Loops through all 5 columns, simulates dropping nextTileValue, calculates highest score (including recursive combos).
     */
    private fun calculateHardAIMove(availableCols: List<Int>): Int {
        var bestCol = availableCols.random()
        var maxSimulatedScore = -1

        for (c in availableCols) {
            val simBoard = Array(rows) { r -> board[r].clone() }
            var targetRow = -1
            for (r in rows - 1 downTo 0) {
                if (simBoard[r][c] == 0) {
                    targetRow = r
                    break
                }
            }

            if (targetRow != -1) {
                simBoard[targetRow][c] = nextTileValue
                val simScore = simulateCascadesAndScore(simBoard)
                if (simScore > maxSimulatedScore) {
                    maxSimulatedScore = simScore
                    bestCol = c
                }
            }
        }
        return bestCol
    }

    private fun simulateCascadesAndScore(simBoard: Array<IntArray>): Int {
        var totalPoints = 0
        var mergedInPass: Boolean
        do {
            mergedInPass = false
            // 1. Gravity
            for (c in 0 until cols) {
                for (r in rows - 1 downTo 0) {
                    if (simBoard[r][c] == 0) {
                        for (aboveR in r - 1 downTo 0) {
                            if (simBoard[aboveR][c] != 0) {
                                simBoard[r][c] = simBoard[aboveR][c]
                                simBoard[aboveR][c] = 0
                                break
                            }
                        }
                    }
                }
            }

            // 2. Merges
            val mergedFlags = Array(rows) { BooleanArray(cols) { false } }
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val val1 = simBoard[r][c]
                    if (val1 != 0 && !mergedFlags[r][c]) {
                        if (r + 1 < rows && simBoard[r + 1][c] == val1 && !mergedFlags[r + 1][c]) {
                            val newMergedVal = val1 * 2
                            simBoard[r][c] = newMergedVal
                            simBoard[r + 1][c] = 0
                            mergedFlags[r][c] = true
                            mergedInPass = true
                            totalPoints += newMergedVal
                        } else if (c + 1 < cols && simBoard[r][c + 1] == val1 && !mergedFlags[r][c + 1]) {
                            val newMergedVal = val1 * 2
                            simBoard[r][c] = newMergedVal
                            simBoard[r][c + 1] = 0
                            mergedFlags[r][c] = true
                            mergedInPass = true
                            totalPoints += newMergedVal
                        }
                    }
                }
            }
        } while (mergedInPass)
        return totalPoints
    }

    // --- UI Rendering ---
    private fun updateUI() {
        tvP1Score.text = "🔴 P1: $player1Score"
        val p2Label = if (gameMode == 0) "🔵 P2: $player2Score" else "🤖 AI: $player2Score"
        tvP2Score.text = p2Label

        val turnLabel = when {
            isPlayer1Turn -> "Player 1's Turn 🔴 (Drop Tile)"
            gameMode == 0 -> "Player 2's Turn 🔵 (Drop Tile)"
            else -> "AI Bot Thinking... 🤖"
        }
        tvTurnStatus.text = turnLabel
        tvTurnStatus.setTextColor(if (isPlayer1Turn) Color.parseColor("#FF1744") else Color.parseColor("#00E5FF"))

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val value = board[r][c]
                val hidden = isHidden[r][c]
                val tv = cellViews[r][c]!!

                if (value == 0) {
                    tv.text = ""
                    tv.setBackgroundColor(Color.parseColor("#1A1A1A"))
                } else if (hidden) {
                    tv.text = "❓"
                    tv.setBackgroundColor(Color.parseColor("#333333"))
                    tv.setTextColor(Color.WHITE)
                } else {
                    tv.text = value.toString()
                    tv.setBackgroundColor(getTileColor(value))
                    tv.setTextColor(Color.BLACK)
                }
            }
        }
    }

    private fun getTileColor(value: Int): Int {
        return when (value) {
            2 -> Color.parseColor("#EEE4DA")
            4 -> Color.parseColor("#EDE0C8")
            8 -> Color.parseColor("#F2B179")
            16 -> Color.parseColor("#F59563")
            32 -> Color.parseColor("#F67C5F")
            64 -> Color.parseColor("#F65E3B")
            128 -> Color.parseColor("#EDCF72")
            256 -> Color.parseColor("#EDCC61")
            512 -> Color.parseColor("#EDC850")
            else -> Color.parseColor("#FFC107")
        }
    }

    // --- 4. Game Over Condition ---
    private fun triggerGameOver(reason: String) {
        isProcessingMove = true
        val p2Name = if (gameMode == 0) "Player 2" else "AI Bot"
        val winnerText = when {
            player1Score > player2Score -> "🎉 Player 1 Wins! 🎉"
            player2Score > player1Score -> "🎉 $p2Name Wins! 🎉"
            else -> "🤝 It's a Tie! 🤝"
        }

        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("$reason\n\nFinal Scores:\n🔴 Player 1: $player1Score\n🔵 $p2Name: $player2Score\n\n$winnerText")
            .setPositiveButton("Play Again") { _, _ -> resetGame() }
            .setCancelable(false)
            .show()
    }
}
