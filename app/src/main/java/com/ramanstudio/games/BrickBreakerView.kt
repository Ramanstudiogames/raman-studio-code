package com.ramanstudio.games

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// --- Game Objects ---
data class Ball(
    var x: Float,
    var y: Float,
    var radius: Float,
    var velocityX: Float,
    var velocityY: Float,
    val paint: Paint
)

data class Paddle(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    val paint: Paint
)

data class Brick(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var isVisible: Boolean = true,
    val paint: Paint
)

class BrickBreakerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null

    // State
    var difficultyMode: Int = 1 // 0 = Easy, 1 = Medium, 2 = Hard, 3 = Extreme
    var isGameStarted: Boolean = false

    var score: Int = 0
        private set
    var lives: Int = 3
        private set

    var isGameOver: Boolean = false
        private set
    var isGameWon: Boolean = false
        private set
    var isBallLaunched: Boolean = false
        private set

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // Entities
    private lateinit var ball: Ball
    private lateinit var paddle: Paddle
    private val bricks = mutableListOf<Brick>()

    // Paints
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 44f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private val statusPaint = Paint().apply {
        color = Color.parseColor("#FFC107")
        textSize = 60f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    init {
        holder.addCallback(this)
        isFocusable = true
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
            isBallLaunched = false

            val rows = when (difficultyMode) {
                0 -> 3
                1 -> 5
                2 -> 7
                else -> 9
            }

            val paddleWidthPercent = when (difficultyMode) {
                0 -> 0.30f
                1 -> 0.20f
                2 -> 0.15f
                else -> 0.10f
            }

            val speedMultiplier = when (difficultyMode) {
                0 -> 0.012f
                1 -> 0.016f
                2 -> 0.020f
                else -> 0.025f
            }

            // Paddle Setup
            val pWidth = screenWidth * paddleWidthPercent
            val pHeight = 36f
            val pX = screenWidth / 2f
            val pY = screenHeight - 160f
            val paddlePaint = Paint().apply {
                color = Color.parseColor("#FFC107")
                isAntiAlias = true
            }
            paddle = Paddle(pX, pY, pWidth, pHeight, paddlePaint)

            // Ball Setup
            val bRadius = 20f
            val baseSpeed = screenHeight * speedMultiplier
            val bX = pX
            val bY = pY - bRadius - 4f
            val ballPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
            }
            ball = Ball(bX, bY, bRadius, 0f, -baseSpeed, ballPaint)

            // Bricks Setup (Positioned below the top HUD)
            bricks.clear()
            val cols = 7
            val topOffset = 270f // Shifted down so bricks never overlap HUD text
            val padding = 8f
            val totalHorizontalPadding = (cols + 1) * padding
            val brickWidth = (screenWidth - totalHorizontalPadding) / cols
            val brickHeight = 40f

            val brickColors = listOf(
                "#E57373", "#F06292", "#BA68C8", "#9575CD",
                "#7986CB", "#64B5F6", "#4FC3F7", "#4DD0E1", "#4DB6AC"
            )

            for (r in 0 until rows) {
                val colorHex = brickColors[r % brickColors.size]
                val bPaint = Paint().apply {
                    color = Color.parseColor(colorHex)
                    isAntiAlias = true
                }

                for (c in 0 until cols) {
                    val x = padding + c * (brickWidth + padding)
                    val y = topOffset + r * (brickHeight + padding)
                    bricks.add(Brick(x, y, brickWidth, brickHeight, true, bPaint))
                }
            }
        }
    }

    private fun resetBallAndPaddle() {
        isBallLaunched = false
        paddle.x = screenWidth / 2f

        val speedMultiplier = when (difficultyMode) {
            0 -> 0.012f
            1 -> 0.016f
            2 -> 0.020f
            else -> 0.025f
        }
        val baseSpeed = screenHeight * speedMultiplier

        ball.x = paddle.x
        ball.y = paddle.y - ball.radius - 4f
        ball.velocityX = 0f
        ball.velocityY = -baseSpeed
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
                    paddle.x = event.x.coerceIn(paddle.width / 2f, screenWidth - paddle.width / 2f)
                    if (!isBallLaunched) {
                        ball.x = paddle.x
                        isBallLaunched = true
                    }
                }
            }
        }
        return true
    }

    fun update() {
        synchronized(this) {
            if (!isGameStarted || isGameOver || isGameWon || !isBallLaunched) return

            // Move Ball
            ball.x += ball.velocityX
            ball.y += ball.velocityY

            // Wall Collisions
            if (ball.x - ball.radius <= 0f) {
                ball.x = ball.radius
                ball.velocityX = -ball.velocityX
            } else if (ball.x + ball.radius >= screenWidth) {
                ball.x = screenWidth - ball.radius
                ball.velocityX = -ball.velocityX
            }

            if (ball.y - ball.radius <= 0f) {
                ball.y = ball.radius
                ball.velocityY = -ball.velocityY
            } else if (ball.y + ball.radius >= screenHeight) {
                // Bottom Hit: Lose Life
                lives--
                if (lives <= 0) {
                    isGameOver = true
                } else {
                    resetBallAndPaddle()
                }
                return
            }

            // Paddle Collision (Angle Rule)
            val paddleLeft = paddle.x - paddle.width / 2f
            val paddleRight = paddle.x + paddle.width / 2f
            val paddleTop = paddle.y
            val paddleBottom = paddle.y + paddle.height

            if (ball.y + ball.radius >= paddleTop &&
                ball.y - ball.radius <= paddleBottom &&
                ball.x >= paddleLeft &&
                ball.x <= paddleRight &&
                ball.velocityY > 0
            ) {
                ball.y = paddleTop - ball.radius

                // Angle Rule Calculation
                val hitOffset = ((ball.x - paddle.x) / (paddle.width / 2f)).coerceIn(-1f, 1f)
                val currentSpeed = sqrt(ball.velocityX * ball.velocityX + ball.velocityY * ball.velocityY)
                val maxAngleRad = Math.toRadians(60.0).toFloat()
                val bounceAngle = hitOffset * maxAngleRad

                ball.velocityX = currentSpeed * sin(bounceAngle)
                ball.velocityY = -abs(currentSpeed * cos(bounceAngle))
            }

            // Brick Collisions (AABB Intersection)
            val ballLeft = ball.x - ball.radius
            val ballRight = ball.x + ball.radius
            val ballTop = ball.y - ball.radius
            val ballBottom = ball.y + ball.radius

            var activeBricksCount = 0
            for (brick in bricks) {
                if (brick.isVisible) {
                    activeBricksCount++
                    val bLeft = brick.x
                    val bRight = brick.x + brick.width
                    val bTop = brick.y
                    val bBottom = brick.y + brick.height

                    if (ballRight >= bLeft && ballLeft <= bRight && ballBottom >= bTop && ballTop <= bBottom) {
                        brick.isVisible = false
                        score += 10
                        ball.velocityY = -ball.velocityY
                        activeBricksCount--
                        break // Handle 1 brick collision per frame
                    }
                }
            }

            if (activeBricksCount == 0) {
                isGameWon = true
            }
        }
    }

    fun drawGame(canvas: Canvas) {
        synchronized(this) {
            // Background
            canvas.drawColor(Color.parseColor("#0D0D0D"))

            // Draw HUD (Clean & Positioned without overlaps)
            canvas.drawText("Score: $score", 30f, 210f, textPaint)
            canvas.drawText("Lives: ${"❤️ ".repeat(lives)}", screenWidth - 300f, 210f, textPaint)

            // Draw Bricks
            for (brick in bricks) {
                if (brick.isVisible) {
                    canvas.drawRect(
                        brick.x, brick.y,
                        brick.x + brick.width, brick.y + brick.height,
                        brick.paint
                    )
                }
            }

            // Draw Paddle
            val pLeft = paddle.x - paddle.width / 2f
            val pRight = paddle.x + paddle.width / 2f
            canvas.drawRoundRect(
                pLeft, paddle.y, pRight, paddle.y + paddle.height,
                12f, 12f, paddle.paint
            )

            // Draw Ball
            canvas.drawCircle(ball.x, ball.y, ball.radius, ball.paint)

            // Status Banners
            if (isGameOver) {
                canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 2f, statusPaint)
            } else if (isGameWon) {
                canvas.drawText("YOU WIN! 🎉", screenWidth / 2f, screenHeight / 2f, statusPaint)
            }
        }
    }

    // --- Background 60 FPS Game Loop Thread ---
    private class GameThread(
        private val surfaceHolder: SurfaceHolder,
        private val gameView: BrickBreakerView
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
