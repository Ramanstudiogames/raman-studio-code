package com.ramanstudio.games

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

enum class TileDifficulty(val label: String, val steps: Int) {
    EASY("Easy", 15),
    MEDIUM("Medium", 45),
    HARD("Hard", 100),
    EXTREME("Extreme", 200)
}

class SlidingTilesActivity : ComponentActivity() {

    // --- State Management ---
    private val board = IntArray(16)
    private val winState = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0)
    
    private var moveCount = 0
    private var currentDifficulty = TileDifficulty.EASY
    private val buttons = ArrayList<Button>()

    private lateinit var tvMoves: TextView
    private lateinit var boardGrid: GridLayout
    private lateinit var btnRestart: Button
    private lateinit var btnBack: Button

    private lateinit var btnEasy: Button
    private lateinit var btnMedium: Button
    private lateinit var btnHard: Button
    private lateinit var btnExtreme: Button

    private lateinit var btnGoalPreview: Button
    private lateinit var layoutGoalPreview: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sliding_tiles)

        tvMoves = findViewById(R.id.tvMoves)
        boardGrid = findViewById(R.id.boardGrid)
        btnRestart = findViewById(R.id.btnRestart)
        btnBack = findViewById(R.id.btnBack)

        btnEasy = findViewById(R.id.btnEasy)
        btnMedium = findViewById(R.id.btnMedium)
        btnHard = findViewById(R.id.btnHard)
        btnExtreme = findViewById(R.id.btnExtreme)

        btnGoalPreview = findViewById(R.id.btnGoalPreview)
        layoutGoalPreview = findViewById(R.id.layoutGoalPreview)

        btnBack.setOnClickListener {
            finish()
        }

        btnRestart.setOnClickListener {
            setupNewGame()
        }

        btnGoalPreview.setOnClickListener {
            layoutGoalPreview.visibility = if (layoutGoalPreview.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Difficulty selection handlers
        btnEasy.setOnClickListener {
            currentDifficulty = TileDifficulty.EASY
            updateDifficultyUI()
            setupNewGame()
        }

        btnMedium.setOnClickListener {
            currentDifficulty = TileDifficulty.MEDIUM
            updateDifficultyUI()
            setupNewGame()
        }

        btnHard.setOnClickListener {
            currentDifficulty = TileDifficulty.HARD
            updateDifficultyUI()
            setupNewGame()
        }

        btnExtreme.setOnClickListener {
            currentDifficulty = TileDifficulty.EXTREME
            updateDifficultyUI()
            setupNewGame()
        }

        initGrid()
        updateDifficultyUI()
        setupNewGame()
    }

    private fun updateDifficultyUI() {
        val activeBg = ColorStateList.valueOf(Color.parseColor("#FFC107"))
        val inactiveBg = ColorStateList.valueOf(Color.parseColor("#1E1E1E"))

        btnEasy.backgroundTintList = if (currentDifficulty == TileDifficulty.EASY) activeBg else inactiveBg
        btnEasy.setTextColor(if (currentDifficulty == TileDifficulty.EASY) Color.BLACK else Color.WHITE)

        btnMedium.backgroundTintList = if (currentDifficulty == TileDifficulty.MEDIUM) activeBg else inactiveBg
        btnMedium.setTextColor(if (currentDifficulty == TileDifficulty.MEDIUM) Color.BLACK else Color.WHITE)

        btnHard.backgroundTintList = if (currentDifficulty == TileDifficulty.HARD) activeBg else inactiveBg
        btnHard.setTextColor(if (currentDifficulty == TileDifficulty.HARD) Color.BLACK else Color.WHITE)

        btnExtreme.backgroundTintList = if (currentDifficulty == TileDifficulty.EXTREME) activeBg else inactiveBg
        btnExtreme.setTextColor(if (currentDifficulty == TileDifficulty.EXTREME) Color.BLACK else Color.WHITE)
    }

    /**
     * Dynamically generates 16 Button views and adds them to the GridLayout
     */
    private fun initGrid() {
        boardGrid.removeAllViews()
        buttons.clear()

        for (i in 0 until 16) {
            val btn = Button(this).apply {
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    rowSpec = GridLayout.spec(i / 4, 1f)
                    columnSpec = GridLayout.spec(i % 4, 1f)
                    setMargins(10, 10, 10, 10)
                }
                layoutParams = params
                textSize = 24f
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.parseColor("#FFC107")) // Amber/Gold tiles
                setOnClickListener { onTileClicked(i) }
            }
            buttons.add(btn)
            boardGrid.addView(btn)
        }
    }

    /**
     * Solvability Rule: Uses the "Random Walk" (Reverse Shuffle) method based on selected Difficulty steps.
     */
    private fun setupNewGame() {
        // 1. Initialize winning state
        for (i in 0 until 16) {
            board[i] = winState[i]
        }

        // 2. Perform random valid neighbor swaps based on difficulty
        val shuffleSteps = currentDifficulty.steps
        for (step in 0 until shuffleSteps) {
            val emptyIdx = findEmptyIndex()
            val validNeighbors = getValidNeighbors(emptyIdx)
            val randomNeighbor = validNeighbors.random()

            // Swap emptyIdx and randomNeighbor
            val temp = board[emptyIdx]
            board[emptyIdx] = board[randomNeighbor]
            board[randomNeighbor] = temp
        }

        moveCount = 0
        updateUI()
    }

    private fun findEmptyIndex(): Int {
        return board.indexOf(0)
    }

    private fun getValidNeighbors(e: Int): List<Int> {
        val neighbors = mutableListOf<Int>()
        // Up
        if (e >= 4) neighbors.add(e - 4)
        // Down
        if (e < 12) neighbors.add(e + 4)
        // Left (prevent wrapping)
        if (e % 4 != 0) neighbors.add(e - 1)
        // Right (prevent wrapping)
        if (e % 4 != 3) neighbors.add(e + 1)
        return neighbors
    }

    private fun onTileClicked(clickedIdx: Int) {
        val emptyIdx = findEmptyIndex()
        if (isValidMove(clickedIdx, emptyIdx)) {
            // Swap values
            val temp = board[clickedIdx]
            board[clickedIdx] = board[emptyIdx]
            board[emptyIdx] = temp

            moveCount++
            updateUI()

            checkWin()
        }
    }

    private fun isValidMove(i: Int, e: Int): Boolean {
        return (i == e - 4) ||                           // Up
               (i == e + 4) ||                           // Down
               (i == e - 1 && i % 4 != 3) ||             // Left (prevent wrapping)
               (i == e + 1 && i % 4 != 0)                // Right (prevent wrapping)
    }

    private fun updateUI() {
        tvMoves.text = "Moves: $moveCount"
        for (i in 0 until 16) {
            val value = board[i]
            val btn = buttons[i]
            if (value == 0) {
                btn.visibility = View.INVISIBLE
            } else {
                btn.visibility = View.VISIBLE
                btn.text = value.toString()
            }
        }
    }

    private fun checkWin() {
        if (board.contentEquals(winState) && moveCount > 0) {
            AlertDialog.Builder(this)
                .setTitle("Congratulations! 🎉")
                .setMessage("You solved the 15-Puzzle (${currentDifficulty.label}) in $moveCount moves!")
                .setPositiveButton("Play Again") { _, _ -> setupNewGame() }
                .setCancelable(false)
                .show()
        }
    }
}
