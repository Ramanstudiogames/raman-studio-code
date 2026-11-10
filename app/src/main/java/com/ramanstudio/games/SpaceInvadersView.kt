package com.ramanstudio.games

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.sin
import kotlin.random.Random

// --- Game Objects ---
data class Player(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    val paint: Paint
)

data class Invader(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var speedY: Float,
    var phase: Float = 0f,
    var isVisible: Boolean = true,
    val paint: Paint
)

data class Laser(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var velocityY: Float,
    val paint: Paint
)

enum class PerkType { HEALTH_BOOST, DOUBLE_SHOOTER }

data class Perk(
    var x: Float,
    var y: Float,
    var radius: Float,
    var type: PerkType,
    var velocityY: Float
)

class SpaceInvadersView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private val prefs: SharedPreferences = context.getSharedPreferences("space_invaders_prefs", Context.MODE_PRIVATE)

    // State
    var difficultyMode: Int = 1 // 0 = Easy, 1 = Med, 2 = Hard, 3 = Extreme, 4 = Impossible
    var isGameStarted: Boolean = false

    var score: Int = 0
        private set
    var highScore: Int = 0
        private set
    var lives: Int = 3
        private set

    var isGameOver: Boolean = false
        private set
    var isGameWon: Boolean = false
        private set

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // Sequential Spawning State
    private var totalInvadersToDefeat: Int = 70
    private var invadersSpawnedCount: Int = 0
    private var invadersDefeatedCount: Int = 0

    private var maxSimultaneousInvaders: Int = 2
    private var lastSpawnTime: Long = 0L

    // Perks State
    private var doubleShooterEndTime: Long = 0L
    private val perks = mutableListOf<Perk>()

    // Entities
    private lateinit var player: Player
    private val activeInvaders = mutableListOf<Invader>()
    private val playerLasers = mutableListOf<Laser>()
    private val enemyLasers = mutableListOf<Laser>()

    private var enemyFireProbability: Float = 0.02f
    private var lastPlayerShotTime: Long = 0L
    private var playerShotIntervalMs: Long = 320L

    // Paints
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 38f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private val statusPaint = Paint().apply {
        color = Color.parseColor("#FFC107")
        textSize = 58f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val playerShipPaint = Paint().apply {
        color = Color.parseColor("#00E5FF")
        isAntiAlias = true
    }

    private val invaderPaintPrimary = Paint().apply {
        color = Color.parseColor("#FF5252")
        isAntiAlias = true
    }

    private val perkHealthPaint = Paint().apply {
        color = Color.parseColor("#FF1744")
        isAntiAlias = true
    }

    private val perkDoublePaint = Paint().apply {
        color = Color.parseColor("#FFD54F")
        isAntiAlias = true
    }

    init {
        holder.addCallback(this)
        isFocusable = true
        highScore = prefs.getInt("high_score", 0)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (screenWidth > 0 && screenHeight > 0) {
            initGame()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        initGame()
        startGameLoop()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopGameLoop()
    }

    fun setDifficulty(mode: Int) {
        difficultyMode = mode
        if (screenWidth > 0 && screenHeight > 0) {
            initGame()
        }
    }

    fun restartGame() {
        initGame()
    }

    fun startGame() {
        isGameStarted = true
    }

    private fun startGameLoop() {
        stopGameLoop()
        gameThread = GameThread(holder, this).apply {
            running = true
            start()
        }
    }

    private fun stopGameLoop() {
        gameThread?.let {
            it.running = false
            var retry = true
            while (retry) {
                try {
                    it.join()
                    retry = false
                } catch (e: InterruptedException) {
                    // Retry
                }
            }
        }
        gameThread = null
    }

    private fun initGame() {
        synchronized(this) {
            score = 0
            lives = 3
            isGameOver = false
            isGameWon = false
            doubleShooterEndTime = 0L

            playerLasers.clear()
            enemyLasers.clear()
            activeInvaders.clear()
            perks.clear()

            highScore = prefs.getInt("high_score", 0)

            totalInvadersToDefeat = when (difficultyMode) {
                0 -> 20
                1 -> 70
                2 -> 100
                3 -> 150
                else -> 500 // Impossible Mode
            }

            invadersSpawnedCount = 0
            invadersDefeatedCount = 0
            maxSimultaneousInvaders = 2 // Coming 1 or 2 at a time max

            enemyFireProbability = when (difficultyMode) {
                0 -> 0.012f
                1 -> 0.022f
                2 -> 0.035f
                3 -> 0.050f
                else -> 0.080f
            }

            // Player Ship Setup
            val pWidth = 110f
            val pHeight = 60f
            val pX = screenWidth / 2f
            val pY = screenHeight - 160f
            player = Player(pX, pY, pWidth, pHeight, playerShipPaint)
        }
    }

    private fun updateHighScoreIfNeeded() {
        if (score > highScore) {
            highScore = score
            prefs.edit().putInt("high_score", highScore).apply()
        }
    }

    private fun spawnInvaderIfNeeded() {
        if (invadersSpawnedCount < totalInvadersToDefeat && activeInvaders.size < maxSimultaneousInvaders) {
            val now = System.currentTimeMillis()
            if (now - lastSpawnTime > 700L) {
                lastSpawnTime = now
                invadersSpawnedCount++

                val invaderWidth = 90f
                val invaderHeight = 60f
                val spawnX = Random.nextFloat() * (screenWidth - invaderWidth - 100f) + 50f
                val speedY = when (difficultyMode) {
                    0 -> screenHeight * 0.0035f
                    1 -> screenHeight * 0.0050f
                    2 -> screenHeight * 0.0065f
                    3 -> screenHeight * 0.0080f
                    else -> screenHeight * 0.0100f
                }

                activeInvaders.add(
                    Invader(
                        x = spawnX,
                        y = -invaderHeight,
                        width = invaderWidth,
                        height = invaderHeight,
                        speedY = speedY,
                        phase = Random.nextFloat() * 100f,
                        paint = invaderPaintPrimary
                    )
                )
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                performClick()
                if (isGameStarted && !isGameOver && !isGameWon) {
                    player.x = event.x.coerceIn(player.width / 2f, screenWidth - player.width / 2f)
                }
            }
        }
        return true
    }

    fun update() {
        synchronized(this) {
            if (!isGameStarted || isGameOver || isGameWon) return

            val now = System.currentTimeMillis()
            val isDoubleShooterActive = now < doubleShooterEndTime

            // 1. Sequential Wave Invader Spawner
            spawnInvaderIfNeeded()

            // 2. Player Auto-Firing (Single or Double Shooter Perk)
            if (now - lastPlayerShotTime >= playerShotIntervalMs) {
                lastPlayerShotTime = now
                val laserPaint = Paint().apply {
                    color = Color.parseColor("#00E5FF")
                    isAntiAlias = true
                }

                if (isDoubleShooterActive) {
                    // Twin parallel lasers
                    playerLasers.add(Laser(player.x - 22f, player.y - 12f, 8f, 28f, -screenHeight * 0.024f, laserPaint))
                    playerLasers.add(Laser(player.x + 22f, player.y - 12f, 8f, 28f, -screenHeight * 0.024f, laserPaint))
                } else {
                    // Single center laser
                    playerLasers.add(Laser(player.x, player.y - 12f, 8f, 28f, -screenHeight * 0.024f, laserPaint))
                }
            }

            // 3. Move Active Invaders (Weaving path downward)
            val invaderIter = activeInvaders.iterator()
            while (invaderIter.hasNext()) {
                val invader = invaderIter.next()
                invader.y += invader.speedY
                invader.phase += 0.05f
                invader.x += (sin(invader.phase.toDouble()) * 3f).toFloat()
                invader.x = invader.x.coerceIn(20f, screenWidth - invader.width - 20f)

                // Random enemy shooting from active invaders
                if (Random.nextFloat() < enemyFireProbability) {
                    val laserPaint = Paint().apply {
                        color = Color.parseColor("#FF5252")
                        isAntiAlias = true
                    }
                    enemyLasers.add(
                        Laser(
                            x = invader.x + invader.width / 2f,
                            y = invader.y + invader.height,
                            width = 8f,
                            height = 26f,
                            velocityY = screenHeight * 0.015f,
                            paint = laserPaint
                        )
                    )
                }

                // If invader reaches bottom past player -> deduct life & remove
                if (invader.y + invader.height >= player.y) {
                    lives--
                    invaderIter.remove()
                    invadersDefeatedCount++
                    if (lives <= 0) {
                        isGameOver = true
                        updateHighScoreIfNeeded()
                        return
                    }
                }
            }

            // 4. Update Perks Movement & Collection
            val perkIter = perks.iterator()
            while (perkIter.hasNext()) {
                val perk = perkIter.next()
                perk.y += perk.velocityY

                // Off-screen cleanup
                if (perk.y > screenHeight) {
                    perkIter.remove()
                    continue
                }

                // Player Catch Perk
                val pLeft = player.x - player.width / 2f
                val pRight = player.x + player.width / 2f
                if (perk.y + perk.radius >= player.y && perk.y - perk.radius <= player.y + player.height &&
                    perk.x >= pLeft && perk.x <= pRight) {
                    
                    if (perk.type == PerkType.HEALTH_BOOST) {
                        lives = (lives + 1).coerceAtMost(5)
                    } else if (perk.type == PerkType.DOUBLE_SHOOTER) {
                        doubleShooterEndTime = now + 8000L // 8s duration
                    }
                    perkIter.remove()
                }
            }

            // 5. Update Player Lasers & Collisions
            val pLaserIter = playerLasers.iterator()
            while (pLaserIter.hasNext()) {
                val laser = pLaserIter.next()
                laser.y += laser.velocityY

                if (laser.y < 0f) {
                    pLaserIter.remove()
                    continue
                }

                var hit = false
                val invaderCollisionIter = activeInvaders.iterator()
                while (invaderCollisionIter.hasNext()) {
                    val invader = invaderCollisionIter.next()
                    if (checkAABB(laser.x - laser.width / 2f, laser.y, laser.width, laser.height,
                                  invader.x, invader.y, invader.width, invader.height)) {
                        
                        // Destroy Invader
                        invaderCollisionIter.remove()
                        invadersDefeatedCount++
                        hit = true
                        score += 10
                        updateHighScoreIfNeeded()

                        // 18% Chance to Drop Perk
                        if (Random.nextFloat() < 0.18f) {
                            val perkType = if (Random.nextBoolean()) PerkType.DOUBLE_SHOOTER else PerkType.HEALTH_BOOST
                            perks.add(Perk(invader.x + invader.width / 2f, invader.y, 22f, perkType, screenHeight * 0.007f))
                        }
                        break
                    }
                }

                if (hit) {
                    pLaserIter.remove()
                }
            }

            // 6. Update Enemy Lasers & Collisions
            val eLaserIter = enemyLasers.iterator()
            while (eLaserIter.hasNext()) {
                val laser = eLaserIter.next()
                laser.y += laser.velocityY

                if (laser.y > screenHeight) {
                    eLaserIter.remove()
                    continue
                }

                val playerLeft = player.x - player.width / 2f
                if (checkAABB(laser.x - laser.width / 2f, laser.y, laser.width, laser.height,
                              playerLeft, player.y, player.width, player.height)) {
                    lives--
                    eLaserIter.remove()

                    if (lives <= 0) {
                        isGameOver = true
                        updateHighScoreIfNeeded()
                    }
                }
            }

            // 7. Check Win Condition
            if (invadersDefeatedCount >= totalInvadersToDefeat && activeInvaders.isEmpty()) {
                isGameWon = true
                updateHighScoreIfNeeded()
            }
        }
    }

    private fun checkAABB(
        x1: Float, y1: Float, w1: Float, h1: Float,
        x2: Float, y2: Float, w2: Float, h2: Float
    ): Boolean {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2
    }

    // --- Textured Drawing Functions ---
    fun drawGame(canvas: Canvas) {
        synchronized(this) {
            // Background
            canvas.drawColor(Color.parseColor("#0D0D0D"))

            // HUD
            val remaining = (totalInvadersToDefeat - invadersDefeatedCount).coerceAtLeast(0)
            canvas.drawText("Score: $score", 30f, 210f, textPaint)
            canvas.drawText("Fleet: $remaining", screenWidth / 2f - 70f, 210f, textPaint)
            canvas.drawText("Lives: ${"❤️ ".repeat(lives)}", screenWidth - 280f, 210f, textPaint)

            // Double Shooter Perk Active Indicator
            if (System.currentTimeMillis() < doubleShooterEndTime) {
                canvas.drawText("⚡ DOUBLE LASERS ACTIVE!", 30f, 260f, perkDoublePaint.apply { textSize = 28f; typeface = Typeface.DEFAULT_BOLD })
            }

            // Draw Active Invaders (Textured Alien Ships)
            for (invader in activeInvaders) {
                drawTexturedInvader(canvas, invader)
            }

            // Draw Player Ship (Textured Fighter)
            drawTexturedPlayerShip(canvas, player)

            // Draw Player Lasers
            for (laser in playerLasers) {
                canvas.drawRoundRect(
                    laser.x - laser.width / 2f, laser.y,
                    laser.x + laser.width / 2f, laser.y + laser.height,
                    4f, 4f, laser.paint
                )
            }

            // Draw Enemy Lasers
            for (laser in enemyLasers) {
                canvas.drawRoundRect(
                    laser.x - laser.width / 2f, laser.y,
                    laser.x + laser.width / 2f, laser.y + laser.height,
                    4f, 4f, laser.paint
                )
            }

            // Draw Perks
            for (perk in perks) {
                if (perk.type == PerkType.HEALTH_BOOST) {
                    canvas.drawCircle(perk.x, perk.y, perk.radius, perkHealthPaint)
                    canvas.drawText("❤️", perk.x - 14f, perk.y + 10f, textPaint.apply { textSize = 24f })
                } else {
                    canvas.drawCircle(perk.x, perk.y, perk.radius, perkDoublePaint)
                    canvas.drawText("⚡", perk.x - 12f, perk.y + 10f, textPaint.apply { textSize = 24f })
                }
            }

            // Status Banners
            if (isGameOver) {
                canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 2f, statusPaint)
            } else if (isGameWon) {
                canvas.drawText("FLEET DESTROYED! 🎉", screenWidth / 2f, screenHeight / 2f, statusPaint)
            }
        }
    }

    // Detailed Textured Player Ship Drawing
    private fun drawTexturedPlayerShip(canvas: Canvas, p: Player) {
        val cx = p.x
        val cy = p.y + p.height / 2f
        val w = p.width
        val h = p.height

        // Main Body Hull
        val bodyPath = Path().apply {
            moveTo(cx, cy - h / 2f - 12f) // Nose tip
            lineTo(cx + w / 4f, cy - h / 4f)
            lineTo(cx + w / 2f, cy + h / 2f) // Right Wing tip
            lineTo(cx + w / 3f, cy + h / 2f)
            lineTo(cx, cy + h / 3f) // Engine center
            lineTo(cx - w / 3f, cy + h / 2f)
            lineTo(cx - w / 2f, cy + h / 2f) // Left Wing tip
            lineTo(cx - w / 4f, cy - h / 4f)
            close()
        }
        canvas.drawPath(bodyPath, p.paint)

        // Cockpit Glass
        val cockpitPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        canvas.drawOval(cx - 10f, cy - h / 4f, cx + 10f, cy + 4f, cockpitPaint)

        // Dual Cannons
        val cannonPaint = Paint().apply { color = Color.parseColor("#FFC107"); isAntiAlias = true }
        canvas.drawRect(cx - w / 3f - 3f, cy - h / 3f, cx - w / 3f + 3f, cy + 4f, cannonPaint)
        canvas.drawRect(cx + w / 3f - 3f, cy - h / 3f, cx + w / 3f + 3f, cy + 4f, cannonPaint)
    }

    // Detailed Textured Invader Alien Drawing
    private fun drawTexturedInvader(canvas: Canvas, inv: Invader) {
        val x = inv.x
        val y = inv.y
        val w = inv.width
        val h = inv.height

        // Alien Core Body
        val alienPath = Path().apply {
            moveTo(x + w * 0.2f, y)
            lineTo(x + w * 0.8f, y)
            lineTo(x + w, y + h * 0.4f)
            lineTo(x + w * 0.85f, y + h)
            lineTo(x + w * 0.65f, y + h * 0.7f)
            lineTo(x + w * 0.35f, y + h * 0.7f)
            lineTo(x + w * 0.15f, y + h)
            lineTo(x, y + h * 0.4f)
            close()
        }
        canvas.drawPath(alienPath, inv.paint)

        // Glowing Eyes
        val eyePaint = Paint().apply { color = Color.YELLOW; isAntiAlias = true }
        canvas.drawCircle(x + w * 0.35f, y + h * 0.35f, 6f, eyePaint)
        canvas.drawCircle(x + w * 0.65f, y + h * 0.35f, 6f, eyePaint)

        // Antennae
        val antennaPaint = Paint().apply { color = inv.paint.color; strokeWidth = 5f; isAntiAlias = true }
        canvas.drawLine(x + w * 0.3f, y, x + w * 0.15f, y - 10f, antennaPaint)
        canvas.drawLine(x + w * 0.7f, y, x + w * 0.85f, y - 10f, antennaPaint)
    }

    // --- Background 60 FPS Game Loop Thread ---
    private class GameThread(
        private val surfaceHolder: SurfaceHolder,
        private val gameView: SpaceInvadersView
    ) : Thread() {

        var running = false
        private val targetFPS = 60
        private val targetTime = 1000L / targetFPS

        override fun run() {
            var startTime: Long
            var timeMillis: Long
            var waitTime: Long

            while (running) {
                startTime = System.currentTimeMillis()
                var canvas: Canvas? = null

                try {
                    canvas = surfaceHolder.lockCanvas()
                    synchronized(surfaceHolder) {
                        if (canvas != null) {
                            gameView.update()
                            gameView.drawGame(canvas)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (canvas != null) {
                        try {
                            surfaceHolder.unlockCanvasAndPost(canvas)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                timeMillis = System.currentTimeMillis() - startTime
                waitTime = targetTime - timeMillis

                if (waitTime > 0) {
                    try {
                        sleep(waitTime)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
