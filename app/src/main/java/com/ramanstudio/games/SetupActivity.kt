package com.ramanstudio.games

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class SetupActivity : ComponentActivity() {

    private var selectedDifficulty = 1 // 0 = Easy, 1 = Medium, 2 = Hard
    private var numPlayers = 2 // 1 to 10
    private val isBotList = BooleanArray(10) { idx -> idx > 0 } // Player 1 human, others bot by default

    private lateinit var btnBack: Button
    private lateinit var btnEasy: Button
    private lateinit var btnMedium: Button
    private lateinit var btnHard: Button

    private lateinit var btnMinusPlayer: Button
    private lateinit var btnPlusPlayer: Button
    private lateinit var tvPlayerCount: TextView

    private lateinit var layoutPlayerConfigs: LinearLayout
    private lateinit var btnStartGame: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        btnBack = findViewById(R.id.btnBack)
        btnEasy = findViewById(R.id.btnEasy)
        btnMedium = findViewById(R.id.btnMedium)
        btnHard = findViewById(R.id.btnHard)

        btnMinusPlayer = findViewById(R.id.btnMinusPlayer)
        btnPlusPlayer = findViewById(R.id.btnPlusPlayer)
        tvPlayerCount = findViewById(R.id.tvPlayerCount)

        layoutPlayerConfigs = findViewById(R.id.layoutPlayerConfigs)
        btnStartGame = findViewById(R.id.btnStartGame)

        btnBack.setOnClickListener { finish() }

        btnEasy.setOnClickListener { updateDifficulty(0) }
        btnMedium.setOnClickListener { updateDifficulty(1) }
        btnHard.setOnClickListener { updateDifficulty(2) }

        btnMinusPlayer.setOnClickListener {
            if (numPlayers > 1) {
                numPlayers--
                updatePlayerCountUI()
            }
        }

        btnPlusPlayer.setOnClickListener {
            if (numPlayers < 10) {
                numPlayers++
                updatePlayerCountUI()
            }
        }

        btnStartGame.setOnClickListener {
            val intent = Intent(this, SnakesLaddersActivity::class.java).apply {
                putExtra("num_players", numPlayers)
                putExtra("difficulty", selectedDifficulty)
                putExtra("player_is_bot", isBotList.copyOf(numPlayers))
            }
            startActivity(intent)
        }

        updateDifficulty(1)
        updatePlayerCountUI()
    }

    private fun updateDifficulty(diff: Int) {
        selectedDifficulty = diff
        val activeBg = ColorStateList.valueOf(Color.parseColor("#FFC107"))
        val inactiveBg = ColorStateList.valueOf(Color.parseColor("#2A2A2A"))

        btnEasy.backgroundTintList = if (diff == 0) activeBg else inactiveBg
        btnEasy.setTextColor(if (diff == 0) Color.BLACK else Color.WHITE)

        btnMedium.backgroundTintList = if (diff == 1) activeBg else inactiveBg
        btnMedium.setTextColor(if (diff == 1) Color.BLACK else Color.WHITE)

        btnHard.backgroundTintList = if (diff == 2) activeBg else inactiveBg
        btnHard.setTextColor(if (diff == 2) Color.BLACK else Color.WHITE)
    }

    private fun updatePlayerCountUI() {
        tvPlayerCount.text = numPlayers.toString()
        rebuildPlayerConfigList()
    }

    private fun rebuildPlayerConfigList() {
        layoutPlayerConfigs.removeAllViews()

        val playerColorsHex = listOf(
            "#E57373", "#64B5F6", "#81C784", "#FFD54F",
            "#FF8A65", "#BA68C8", "#4DD0E1", "#F06292",
            "#AED581", "#E0E0E0"
        )

        for (i in 0 until numPlayers) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 8, 8, 8)
            }

            val colorIndicator = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(28, 28).apply { setMargins(0, 0, 16, 0) }
                setBackgroundColor(Color.parseColor(playerColorsHex[i % playerColorsHex.size]))
            }

            val pLabel = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "Player ${i + 1}"
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }

            val toggleBtn = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(220, 80)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 0)
                
                fun updateBtnState() {
                    val isBot = isBotList[i]
                    text = if (isBot) "🤖 Bot" else "👤 Human"
                    backgroundTintList = ColorStateList.valueOf(
                        if (isBot) Color.parseColor("#333333") else Color.parseColor("#FFC107")
                    )
                    setTextColor(if (isBot) Color.WHITE else Color.BLACK)
                }

                updateBtnState()

                setOnClickListener {
                    isBotList[i] = !isBotList[i]
                    updateBtnState()
                }
            }

            row.addView(colorIndicator)
            row.addView(pLabel)
            row.addView(toggleBtn)

            layoutPlayerConfigs.addView(row)
        }
    }
}
