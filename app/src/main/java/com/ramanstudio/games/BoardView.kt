package com.ramanstudio.games

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var numPlayers: Int = 2
    val playerPositions = IntArray(10) { 1 } // All start at Square 1

    // Snakes & Ladders Map
    var laddersMap: Map<Int, Int> = emptyMap()
    var snakesMap: Map<Int, Int> = emptyMap()

    // Smooth Animation State per player
    private val playerAnimCoords = Array(10) { PointF() }
    private var isAnimating = false

    // Grid Dimensions
    private var cellSize = 0f
    private var startX = 0f
    private var startY = 0f

    // Colors
    private val playerColorsHex = listOf(
        "#E57373", "#64B5F6", "#81C784", "#FFD54F",
        "#FF8A65", "#BA68C8", "#4DD0E1", "#F06292",
        "#AED581", "#E0E0E0"
    )

    // Vibrant & Colorful Classic Board Tile Palette (Bold Saturated Colors)
    private val tileColors = listOf(
        Color.parseColor("#D32F2F"), // Vibrant Red
        Color.parseColor("#F57C00"), // Vibrant Orange
        Color.parseColor("#FBC02D"), // Vibrant Yellow
        Color.parseColor("#388E3C"), // Vibrant Green
        Color.parseColor("#1976D2"), // Vibrant Blue
        Color.parseColor("#7B1FA2")  // Vibrant Purple
    )

    private val outerFramePaint = Paint().apply {
        color = Color.parseColor("#3E2723") // Rich Mahogany Wood
        style = Paint.Style.STROKE
        strokeWidth = 14f
        isAntiAlias = true
    }

    private val gridBorderPaint = Paint().apply {
        color = Color.parseColor("#1A1A1A")
        strokeWidth = 2.5f
        isAntiAlias = true
    }

    // Crisp White Numbers with Drop Shadow for Maximum Legibility
    private val numberPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        setShadowLayer(4f, 2f, 2f, Color.parseColor("#CC000000"))
    }

    // Wooden Ladder Paints
    private val ladderRailOuter = Paint().apply {
        color = Color.parseColor("#3E2723")
        strokeWidth = 14f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val ladderRailInner = Paint().apply {
        color = Color.parseColor("#8D6E63")
        strokeWidth = 8f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val ladderRungPaint = Paint().apply {
        color = Color.parseColor("#4E342E")
        strokeWidth = 8f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    // Reusable Paints for Token Drawing
    private val tokenBorderPaint = Paint().apply { color = Color.BLACK; isAntiAlias = true }
    private val tokenMainPaint = Paint().apply { isAntiAlias = true }
    private val tokenHighlightPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }

    init {
        for (i in 0 until 10) {
            val pt = getSquareCenterWithOffset(1, i)
            playerAnimCoords[i].set(pt)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val boardSize = min(w, h) * 0.96f
        cellSize = boardSize / 10f
        startX = (w - boardSize) / 2f
        startY = (h - boardSize) / 2f

        for (i in 0 until 10) {
            val pt = getSquareCenterWithOffset(playerPositions[i], i)
            playerAnimCoords[i].set(pt)
        }
    }

    // --- 4. Zig-Zag Grid & Overlap Mathematics ---

    fun getSquareCenter(square: Int): PointF {
        val clampedSq = square.coerceIn(1, 100)
        val rowFromBottom = (clampedSq - 1) / 10
        val col = if (rowFromBottom % 2 == 0) {
            (clampedSq - 1) % 10
        } else {
            9 - ((clampedSq - 1) % 10)
        }

        val cx = startX + (col + 0.5f) * cellSize
        val cy = startY + (9 - rowFromBottom + 0.5f) * cellSize
        return PointF(cx, cy)
    }

    fun getSquareCenterWithOffset(square: Int, playerIndex: Int): PointF {
        val baseCenter = getSquareCenter(square)
        val offsetX = ((playerIndex % 3) - 1) * (cellSize * 0.22f)
        val offsetY = ((playerIndex / 3) - 1) * (cellSize * 0.22f)
        return PointF(baseCenter.x + offsetX, baseCenter.y + offsetY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (cellSize <= 0f) return

        // Outer Wooden Frame
        val boardWidth = cellSize * 10f
        canvas.drawRect(startX - 4f, startY - 4f, startX + boardWidth + 4f, startY + boardWidth + 4f, outerFramePaint)

        // 1. Draw 10x10 Colorful Grid & White Tile Numbers
        for (sq in 1..100) {
            val rowFromBottom = (sq - 1) / 10
            val col = if (rowFromBottom % 2 == 0) (sq - 1) % 10 else 9 - ((sq - 1) % 10)

            val left = startX + col * cellSize
            val top = startY + (9 - rowFromBottom) * cellSize
            val right = left + cellSize
            val bottom = top + cellSize

            // Vibrant colorful tile pattern
            val tileColor = tileColors[(sq + col) % tileColors.size]
            val tilePaint = Paint().apply { color = tileColor; isAntiAlias = true }

            canvas.drawRect(left, top, right, bottom, tilePaint)
            canvas.drawRect(left, top, right, bottom, gridBorderPaint)

            // Crisp White Number with Shadow
            canvas.drawText(sq.toString(), left + 6f, top + 24f, numberPaint)
        }

        // 2. Draw Ladders
        for ((startSq, endSq) in laddersMap) {
            drawLadder(canvas, startSq, endSq)
        }

        // 3. Draw Realistic Snakes
        for ((headSq, tailSq) in snakesMap) {
            drawRealisticSnake(canvas, headSq, tailSq)
        }

        // 4. Draw Player Tokens
        for (i in 0 until numPlayers) {
            val coord = playerAnimCoords[i]
            val tokenColor = Color.parseColor(playerColorsHex[i % playerColorsHex.size])

            // Token Border
            canvas.drawCircle(coord.x, coord.y, cellSize * 0.20f, tokenBorderPaint)

            // Main Circle
            tokenMainPaint.color = tokenColor
            canvas.drawCircle(coord.x, coord.y, cellSize * 0.17f, tokenMainPaint)

            // Inner Highlight
            canvas.drawCircle(coord.x - 2f, coord.y - 2f, cellSize * 0.05f, tokenHighlightPaint)
        }
    }

    private fun drawLadder(canvas: Canvas, startSq: Int, endSq: Int) {
        val startPt = getSquareCenter(startSq)
        val endPt = getSquareCenter(endSq)

        val dx = endPt.x - startPt.x
        val dy = endPt.y - startPt.y
        val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

        if (dist <= 0f) return

        val perpX = -dy / dist * (cellSize * 0.18f)
        val perpY = dx / dist * (cellSize * 0.18f)

        // Outer Rails
        val lX1 = startPt.x + perpX
        val lY1 = startPt.y + perpY
        val lX2 = endPt.x + perpX
        val lY2 = endPt.y + perpY
        canvas.drawLine(lX1, lY1, lX2, lY2, ladderRailOuter)

        val rX1 = startPt.x - perpX
        val rY1 = startPt.y - perpY
        val rX2 = endPt.x - perpX
        val rY2 = endPt.y - perpY
        canvas.drawLine(rX1, rY1, rX2, rY2, ladderRailOuter)

        // Inner Rails
        canvas.drawLine(lX1, lY1, lX2, lY2, ladderRailInner)
        canvas.drawLine(rX1, rY1, rX2, rY2, ladderRailInner)

        // Rungs
        val rungsCount = (dist / (cellSize * 0.35f)).toInt().coerceAtLeast(2)
        for (i in 1..rungsCount) {
            val t = i.toFloat() / (rungsCount + 1)
            val rx1 = lX1 + t * (lX2 - lX1)
            val ry1 = lY1 + t * (lY2 - lY1)
            val rx2 = rX1 + t * (rX2 - rX1)
            val ry2 = rY1 + t * (rY2 - rY1)
            canvas.drawLine(rx1, ry1, rx2, ry2, ladderRungPaint)
        }
    }

    // Realistic Serpent Snake Drawing with S-Curve, Scaled Body, Slit Pupil Eyes, and Red Forked Tongue
    private fun drawRealisticSnake(canvas: Canvas, headSq: Int, tailSq: Int) {
        val headPt = getSquareCenter(headSq)
        val tailPt = getSquareCenter(tailSq)

        val dx = tailPt.x - headPt.x
        val dy = tailPt.y - headPt.y
        val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (dist <= 0f) return

        val angle = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()

        // Serpentine S-Curve Path
        val path = Path()
        path.moveTo(headPt.x, headPt.y)

        val perpX = -dy / dist * (cellSize * 0.65f)
        val perpY = dx / dist * (cellSize * 0.65f)

        val cp1X = headPt.x + dx * 0.25f + perpX
        val cp1Y = headPt.y + dy * 0.25f + perpY
        val cp2X = headPt.x + dx * 0.75f - perpX
        val cp2Y = headPt.y + dy * 0.75f - perpY

        path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, tailPt.x, tailPt.y)

        // 1. Dark Shadow under Snake
        val shadowPaint = Paint().apply {
            color = Color.parseColor("#70000000")
            strokeWidth = 22f
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(path, shadowPaint)

        // 2. Snake Outer Scaly Skin (Dark Anacondas/Viper Green)
        val skinOuterPaint = Paint().apply {
            color = Color.parseColor("#1B431B")
            strokeWidth = 18f
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(path, skinOuterPaint)

        // 3. Snake Inner Body (Emerald Green)
        val skinInnerPaint = Paint().apply {
            color = Color.parseColor("#2E7D32")
            strokeWidth = 12f
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(path, skinInnerPaint)

        // 4. Snake Diamond Spots / Belly Stripe
        val patternPaint = Paint().apply {
            color = Color.parseColor("#FFD54F") // Gold spots
            strokeWidth = 4f
            isAntiAlias = true
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(path, patternPaint)

        // 5. Realistic Viper Head
        canvas.save()
        canvas.translate(headPt.x, headPt.y)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat() + 180f)

        // Triangular Head Base
        val headPath = Path().apply {
            moveTo(cellSize * 0.32f, 0f) // Snout tip
            lineTo(-cellSize * 0.15f, -cellSize * 0.22f) // Left cheek
            lineTo(-cellSize * 0.22f, 0f) // Neck
            lineTo(-cellSize * 0.15f, cellSize * 0.22f) // Right cheek
            close()
        }
        val headPaint = Paint().apply {
            color = Color.parseColor("#1B431B")
            isAntiAlias = true
        }
        canvas.drawPath(headPath, headPaint)

        // Head Scale Pattern
        val headScalePath = Path().apply {
            moveTo(cellSize * 0.22f, 0f)
            lineTo(-cellSize * 0.10f, -cellSize * 0.14f)
            lineTo(-cellSize * 0.15f, 0f)
            lineTo(-cellSize * 0.10f, cellSize * 0.14f)
            close()
        }
        val headScalePaint = Paint().apply {
            color = Color.parseColor("#2E7D32")
            isAntiAlias = true
        }
        canvas.drawPath(headScalePath, headScalePaint)

        // Golden Eyes with Slit Pupil
        val eyePaint = Paint().apply { color = Color.parseColor("#FFD54F"); isAntiAlias = true }
        val pupilPaint = Paint().apply { color = Color.BLACK; isAntiAlias = true; strokeWidth = 2f }

        canvas.drawCircle(cellSize * 0.08f, -cellSize * 0.12f, 4.5f, eyePaint)
        canvas.drawCircle(cellSize * 0.08f, cellSize * 0.12f, 4.5f, eyePaint)
        canvas.drawLine(cellSize * 0.08f, -cellSize * 0.15f, cellSize * 0.08f, -cellSize * 0.09f, pupilPaint)
        canvas.drawLine(cellSize * 0.08f, cellSize * 0.09f, cellSize * 0.08f, cellSize * 0.15f, pupilPaint)

        // Red Forked Tongue
        val tonguePath = Path().apply {
            moveTo(cellSize * 0.32f, 0f)
            lineTo(cellSize * 0.48f, 0f)
            lineTo(cellSize * 0.55f, -cellSize * 0.07f)
            moveTo(cellSize * 0.48f, 0f)
            lineTo(cellSize * 0.55f, cellSize * 0.07f)
        }
        val tonguePaint = Paint().apply {
            color = Color.parseColor("#D50000")
            strokeWidth = 3f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        canvas.drawPath(tonguePath, tonguePaint)

        canvas.restore()
    }

    // --- 4. Smooth ValueAnimator Token Movement ---

    fun animatePlayerMovement(
        playerIndex: Int,
        pathSquares: List<Int>,
        finalSquare: Int,
        onComplete: () -> Unit
    ) {
        if (pathSquares.isEmpty()) {
            onComplete()
            return
        }

        isAnimating = true
        animateStepRecursive(playerIndex, pathSquares, 0) {
            val lastPathSq = pathSquares.last()
            if (lastPathSq != finalSquare) {
                val startPt = playerAnimCoords[playerIndex]
                val endPt = getSquareCenterWithOffset(finalSquare, playerIndex)

                val slideAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 600L
                    addUpdateListener { anim ->
                        val t = anim.animatedValue as Float
                        playerAnimCoords[playerIndex].x = startPt.x + t * (endPt.x - startPt.x)
                        playerAnimCoords[playerIndex].y = startPt.y + t * (endPt.y - startPt.y)
                        invalidate()
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            playerPositions[playerIndex] = finalSquare
                            isAnimating = false
                            invalidate()
                            onComplete()
                        }
                    })
                }
                slideAnim.start()
            } else {
                playerPositions[playerIndex] = finalSquare
                isAnimating = false
                invalidate()
                onComplete()
            }
        }
    }

    private fun animateStepRecursive(
        playerIndex: Int,
        pathSquares: List<Int>,
        stepIdx: Int,
        onAllStepsFinished: () -> Unit
    ) {
        if (stepIdx >= pathSquares.size) {
            onAllStepsFinished()
            return
        }

        val targetSq = pathSquares[stepIdx]
        val startPt = PointF(playerAnimCoords[playerIndex].x, playerAnimCoords[playerIndex].y)
        val endPt = getSquareCenterWithOffset(targetSq, playerIndex)

        val stepAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180L
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                playerAnimCoords[playerIndex].x = startPt.x + t * (endPt.x - startPt.x)
                playerAnimCoords[playerIndex].y = startPt.y + t * (endPt.y - startPt.y)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    playerPositions[playerIndex] = targetSq
                    animateStepRecursive(playerIndex, pathSquares, stepIdx + 1, onAllStepsFinished)
                }
            })
        }
        stepAnim.start()
    }
}
