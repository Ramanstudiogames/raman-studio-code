package com.ramanstudio.games

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.ComponentActivity

class BrickBreakerActivity : ComponentActivity() {

    private lateinit var brickBreakerView: BrickBreakerView
    private lateinit var btnBack: Button
    private lateinit var btnSetup: Button
    private lateinit var btnRestart: Button

    private lateinit var layoutSetup: LinearLayout
    private lateinit var btnEasy: Button
    private lateinit var btnMedium: Button
    private lateinit var btnHard: Button
    private lateinit var btnExtreme: Button
    private lateinit var btnStartGame: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brick_breaker)

        brickBreakerView = findViewById(R.id.brickBreakerView)
        btnBack = findViewById(R.id.btnBack)
        btnSetup = findViewById(R.id.btnSetup)
        btnRestart = findViewById(R.id.btnRestart)

        layoutSetup = findViewById(R.id.layoutSetup)
        btnEasy = findViewById(R.id.btnEasy)
        btnMedium = findViewById(R.id.btnMedium)
        btnHard = findViewById(R.id.btnHard)
        btnExtreme = findViewById(R.id.btnExtreme)
        btnStartGame = findViewById(R.id.btnStartGame)

        btnBack.setOnClickListener {
            finish()
        }

        btnSetup.setOnClickListener {
            layoutSetup.visibility = View.VISIBLE
        }

        btnRestart.setOnClickListener {
            brickBreakerView.restartGame()
        }

        btnStartGame.setOnClickListener {
            layoutSetup.visibility = View.GONE
            brickBreakerView.startGame()
        }

        btnEasy.setOnClickListener {
            updateDifficulty(0)
        }

        btnMedium.setOnClickListener {
            updateDifficulty(1)
        }

        btnHard.setOnClickListener {
            updateDifficulty(2)
        }

        btnExtreme.setOnClickListener {
            updateDifficulty(3)
        }

        updateDifficulty(1) // Default to Medium
    }

    private fun updateDifficulty(mode: Int) {
        brickBreakerView.setDifficulty(mode)

        val activeBg = ColorStateList.valueOf(Color.parseColor("#FFC107"))
        val inactiveBg = ColorStateList.valueOf(Color.parseColor("#2A2A2A"))

        btnEasy.backgroundTintList = if (mode == 0) activeBg else inactiveBg
        btnEasy.setTextColor(if (mode == 0) Color.BLACK else Color.WHITE)

        btnMedium.backgroundTintList = if (mode == 1) activeBg else inactiveBg
        btnMedium.setTextColor(if (mode == 1) Color.BLACK else Color.WHITE)

        btnHard.backgroundTintList = if (mode == 2) activeBg else inactiveBg
        btnHard.setTextColor(if (mode == 2) Color.BLACK else Color.WHITE)

        btnExtreme.backgroundTintList = if (mode == 3) activeBg else inactiveBg
        btnExtreme.setTextColor(if (mode == 3) Color.BLACK else Color.WHITE)
    }
}
