package com.ramanstudio.games

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.random.Random

class SnakesLaddersActivity : ComponentActivity() {

    private var numPlayers = 2
    private var difficulty = 1 // 0 = Easy, 1 = Medium, 2 = Hard
    private var playerIsBot = BooleanArray(10) { idx -> idx > 0 }

    private var currentPlayerIndex = 0
    private var isRolling = false
    private var isGameOver = false

    private val playerPositions = IntArray(10) { 1 }

    // Maps for Snakes and Ladders
    private val laddersMap = mutableMapOf<Int, Int>()
    private val snakesMap = mutableMapOf<Int, Int>()

    private lateinit var boardView: BoardView
    private lateinit var tvTurnStatus: TextView
    private lateinit var tvDiceDisplay: TextView
    private lateinit var btnRollDice: Button
    private lateinit var btnBack: Button
    private lateinit var btnSetup: Button
    private lateinit var btnRestart: Button

    private val handler = Handler(Looper.getMainLooper())

    private val playerColorsHex = listOf(
        "#E57373", "#64B5F6", "#81C784", "#FFD54F",
        "#FF8A65", "#BA68C8", "#4DD0E1", "#F06292",
        "#AED581", "#E0E0E0"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snakes_ladders)

        parseIntentExtras()

        boardView = findViewById(R.id.boardView)
        tvTurnStatus = findViewById(R.id.tvTurnStatus)
        tvDiceDisplay = findViewById(R.id.tvDiceDisplay)
        btnRollDice = findViewById(R.id.btnRollDice)

        btnBack = findViewById(R.id.btnBack)
        btnSetup = findViewById(R.id.btnSetup)
        btnRestart = findViewById(R.id.btnRestart)

        btnBack.setOnClickListener { finish() }
        btnSetup.setOnClickListener {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
        }
        btnRestart.setOnClickListener { resetGame() }

        btnRollDice.setOnClickListener {
            if (!isRolling && !isGameOver && !playerIsBot[currentPlayerIndex]) {
                handleDiceRoll()
            }
        }

        generateMapByDifficulty()
        setupBoardView()
        updateTurnStatusUI()
    }

    private fun parseIntentExtras() {
        numPlayers = intent.getIntExtra("num_players", 2).coerceIn(1, 10)
        difficulty = intent.getIntExtra("difficulty", 1).coerceIn(0, 2)
        val botArray = intent.getBooleanArrayExtra("player_is_bot")
        if (botArray != null && botArray.size >= numPlayers) {
            for (i in 0 until numPlayers) {
                playerIsBot[i] = botArray[i]
            }
        }
    }

    // --- 2. Board Difficulties (Map Generation) ---
    private fun generateMapByDifficulty() {
        laddersMap.clear()
        snakesMap.clear()

        when (difficulty) {
            0 -> {
                // Easy: ~8 long ladders, only ~3 short snakes
                laddersMap[4] = 25
                laddersMap[9] = 31
                laddersMap[21] = 52
                laddersMap[28] = 84
                laddersMap[36] = 66
                laddersMap[51] = 77
                laddersMap[71] = 91
                laddersMap[80] = 99

                snakesMap[27] = 15
                snakesMap[54] = 34
                snakesMap[62] = 43
            }
            1 -> {
                // Medium / Classic: ~6 ladders, ~6 snakes
                laddersMap[4] = 14
                laddersMap[12] = 31
                laddersMap[28] = 55
                laddersMap[36] = 44
                laddersMap[51] = 67
                laddersMap[71] = 91

                snakesMap[16] = 6
                snakesMap[48] = 26
                snakesMap[64] = 60
                snakesMap[79] = 19
                snakesMap[93] = 73
                snakesMap[98] = 58
            }
            else -> {
                // Hard: ~3 short ladders, ~8 brutal snakes
                laddersMap[4] = 14
                laddersMap[36] = 44
                laddersMap[51] = 67

                snakesMap[16] = 6
                snakesMap[48] = 26
                snakesMap[64] = 24
                snakesMap[79] = 19
                snakesMap[93] = 37
                snakesMap[95] = 13
                snakesMap[98] = 8
                snakesMap[99] = 4 // Brutal down to row 1!
            }
        }
    }

    private fun setupBoardView() {
        boardView.numPlayers = numPlayers
        boardView.laddersMap = laddersMap
        boardView.snakesMap = snakesMap
        for (i in 0 until 10) {
            playerPositions[i] = 1
            boardView.playerPositions[i] = 1
        }
    }

    private fun resetGame() {
        handler.removeCallbacksAndMessages(null)
        isRolling = false
        isGameOver = false
        currentPlayerIndex = 0
        for (i in 0 until 10) {
            playerPositions[i] = 1
            boardView.playerPositions[i] = 1
        }
        tvDiceDisplay.text = "🎲"
        generateMapByDifficulty()
        setupBoardView()
        boardView.invalidate()
        updateTurnStatusUI()
    }

    private fun updateTurnStatusUI() {
        val pNum = currentPlayerIndex + 1
        val isBot = playerIsBot[currentPlayerIndex]
        val pColorHex = playerColorsHex[currentPlayerIndex % playerColorsHex.size]

        val typeStr = if (isBot) "🤖 Bot" else "👤 Human"
        tvTurnStatus.text = "Player $pNum ($typeStr)'s Turn"
        tvTurnStatus.setBackgroundColor(Color.parseColor(pColorHex))
        tvTurnStatus.setTextColor(Color.BLACK)

        btnRollDice.isEnabled = !isRolling && !isGameOver && !isBot

        // Bot Logic trigger
        if (isBot && !isRolling && !isGameOver) {
            handler.postDelayed({
                if (!isGameOver) {
                    handleDiceRoll()
                }
            }, 1000L) // 1.0 second delay for bot move
        }
    }

    private fun handleDiceRoll() {
        isRolling = true
        btnRollDice.isEnabled = false

        // Dice rolling animation
        val diceRolls = 6
        var rollCount = 0
        val diceAnimHandler = Handler(Looper.getMainLooper())
        
        val diceRunnable = object : Runnable {
            override fun run() {
                val rolledVal = Random.nextInt(1, 7)
                tvDiceDisplay.text = getDiceSymbol(rolledVal)
                rollCount++
                if (rollCount < diceRolls) {
                    diceAnimHandler.postDelayed(this, 60L)
                } else {
                    val finalDice = Random.nextInt(1, 7)
                    tvDiceDisplay.text = getDiceSymbol(finalDice)
                    processMove(finalDice)
                }
            }
        }
        diceAnimHandler.post(diceRunnable)
    }

    // --- 3. Classic Rules & Bounce Back Logic ---
    private fun processMove(dice: Int) {
        val currentSq = playerPositions[currentPlayerIndex]

        // Build step-by-step path squares (with Bounce Back Rule if > 100)
        val pathSquares = mutableListOf<Int>()
        var targetSq: Int

        if (currentSq + dice <= 100) {
            targetSq = currentSq + dice
            for (s in (currentSq + 1)..targetSq) {
                pathSquares.add(s)
            }
        } else {
            // Bounce back rule: e.g. on 98 and roll 5 -> 99, 100, 99, 98, 97
            val overshoot = (currentSq + dice) - 100
            targetSq = 100 - overshoot
            for (s in (currentSq + 1)..100) {
                pathSquares.add(s)
            }
            for (s in 99 downTo targetSq) {
                pathSquares.add(s)
            }
        }

        // Check if destination has Snake or Ladder
        val finalSq = snakesMap[targetSq] ?: laddersMap[targetSq] ?: targetSq

        // Animate movement smooth across squares
        boardView.animatePlayerMovement(currentPlayerIndex, pathSquares, finalSq) {
            playerPositions[currentPlayerIndex] = finalSq
            isRolling = false

            // Win Detection
            if (finalSq == 100) {
                isGameOver = true
                showWinDialog()
            } else if (dice == 6) {
                // Rule of 6: Extra turn
                tvTurnStatus.text = "Player ${currentPlayerIndex + 1} rolled a 6! Roll again!"
                if (playerIsBot[currentPlayerIndex]) {
                    handler.postDelayed({
                        if (!isGameOver) handleDiceRoll()
                    }, 1000L)
                } else {
                    btnRollDice.isEnabled = true
                }
            } else {
                // Next player's turn
                currentPlayerIndex = (currentPlayerIndex + 1) % numPlayers
                updateTurnStatusUI()
            }
        }
    }

    private fun getDiceSymbol(value: Int): String {
        return when (value) {
            1 -> "⚀"
            2 -> "⚁"
            3 -> "⚂"
            4 -> "⚃"
            5 -> "⚄"
            6 -> "⚅"
            else -> "🎲"
        }
    }

    // --- 5. Game Over Dialog ---
    private fun showWinDialog() {
        val pNum = currentPlayerIndex + 1
        val isBot = playerIsBot[currentPlayerIndex]
        val winnerName = if (isBot) "🤖 Bot (Player $pNum)" else "👤 Player $pNum"

        AlertDialog.Builder(this)
            .setTitle("Game Over! 🎉")
            .setMessage("$winnerName has reached Square 100 and WON the match!")
            .setPositiveButton("Play Again") { _, _ -> resetGame() }
            .setNegativeButton("Back to Menu") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}
