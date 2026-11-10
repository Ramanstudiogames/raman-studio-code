package com.ramanstudio.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

data class Pipe(var x: Float, val gapY: Float, var passed: Boolean = false)

@Composable
fun FlappyLambdaScreen(onBack: () -> Unit) {
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var highScore by remember { mutableStateOf(0) }

    // Screen dimensions (initialized to 1000f, will be updated)
    var screenWidth by remember { mutableStateOf(1000f) }
    var screenHeight by remember { mutableStateOf(1000f) }
    
    // Dynamic scaling based on screen size
    val density = LocalDensity.current
    val lambdaRadiusPx = with(density) { 20.dp.toPx() } 
    val pipeWidthPx = with(density) { 70.dp.toPx() }
    val pipeSpacingPx = with(density) { 320.dp.toPx() } // Increased spacing between pipes
    val gapSizePx = with(density) { 210.dp.toPx() } // Increased gap size
    
    val gravity = with(density) { 0.35f.dp.toPx() } // Decreased gravity for floatier feel
    val jumpVelocity = with(density) { -7f.dp.toPx() } // Adjusted jump velocity
    val pipeSpeed = with(density) { 2.8f.dp.toPx() } // Slower pipes

    // Game state
    var lambdaY by remember { mutableStateOf(500f) }
    var velocity by remember { mutableStateOf(0f) }
    var pipes by remember { mutableStateOf(listOf<Pipe>()) }

    fun resetGame() {
        lambdaY = screenHeight / 2f
        velocity = 0f
        
        val firstGapY = Random.nextFloat() * (screenHeight - gapSizePx - 200f) + 100f + gapSizePx / 2
        val secondGapY = Random.nextFloat() * (screenHeight - gapSizePx - 200f) + 100f + gapSizePx / 2
        
        pipes = listOf(
            Pipe(screenWidth, firstGapY),
            Pipe(screenWidth + pipeSpacingPx, secondGapY)
        )
        score = 0
        isGameOver = false
        isPlaying = true
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying && isActive) {
            delay(16) // ~60fps

            // Physics
            velocity += gravity
            lambdaY += velocity

            // Pipe movement and collision
            val newPipes = pipes.map { it.copy(x = it.x - pipeSpeed) }.toMutableList()
            
            // Add new pipe
            if (newPipes.last().x < screenWidth - pipeSpacingPx) {
                val newGapY = Random.nextFloat() * (screenHeight - gapSizePx - 200f) + 100f + gapSizePx / 2
                newPipes.add(Pipe(newPipes.last().x + pipeSpacingPx, newGapY))
            }
            
            // Remove off-screen pipes
            if (newPipes.first().x < -pipeWidthPx) {
                newPipes.removeAt(0)
            }

            val lambdaX = screenWidth / 3f

            // Score check
            newPipes.forEach { pipe ->
                if (!pipe.passed && pipe.x + pipeWidthPx < lambdaX - lambdaRadiusPx) {
                    pipe.passed = true
                    score++
                    if (score > highScore) highScore = score
                }
            }

            pipes = newPipes

            // Collision detection
            val lambdaRect = Rect(lambdaX - lambdaRadiusPx + 10f, lambdaY - lambdaRadiusPx + 10f, lambdaX + lambdaRadiusPx - 10f, lambdaY + lambdaRadiusPx - 10f)
            
            // Floor/Ceiling
            if (lambdaY + lambdaRadiusPx > screenHeight - 40f || lambdaY - lambdaRadiusPx < 0) {
                isPlaying = false
                isGameOver = true
            }

            // Pipes
            for (pipe in pipes) {
                val topPipe = Rect(pipe.x, 0f, pipe.x + pipeWidthPx, pipe.gapY - gapSizePx / 2)
                val bottomPipe = Rect(pipe.x, pipe.gapY + gapSizePx / 2, pipe.x + pipeWidthPx, screenHeight)

                if (lambdaRect.overlaps(topPipe) || lambdaRect.overlaps(bottomPipe)) {
                    isPlaying = false
                    isGameOver = true
                    break
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF70C5CE)) // Flappy sky color
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isPlaying) {
                    velocity = jumpVelocity
                } else if (!isGameOver) {
                    resetGame()
                    velocity = jumpVelocity
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            screenWidth = size.width
            screenHeight = size.height

            if (!isPlaying && !isGameOver && pipes.isEmpty()) {
                 lambdaY = size.height / 2f
            }

            // Draw Pipes
            for (pipe in pipes) {
                // Top pipe
                drawRect(
                    color = Color(0xFF73BF2E), // Flappy green
                    topLeft = Offset(pipe.x, 0f),
                    size = Size(pipeWidthPx, pipe.gapY - gapSizePx / 2)
                )
                // Top pipe cap
                drawRect(
                    color = Color(0xFF73BF2E),
                    topLeft = Offset(pipe.x - 10f, pipe.gapY - gapSizePx / 2 - 40f),
                    size = Size(pipeWidthPx + 20f, 40f)
                )

                // Bottom pipe
                drawRect(
                    color = Color(0xFF73BF2E),
                    topLeft = Offset(pipe.x, pipe.gapY + gapSizePx / 2),
                    size = Size(pipeWidthPx, size.height - (pipe.gapY + gapSizePx / 2))
                )
                // Bottom pipe cap
                drawRect(
                    color = Color(0xFF73BF2E),
                    topLeft = Offset(pipe.x - 10f, pipe.gapY + gapSizePx / 2),
                    size = Size(pipeWidthPx + 20f, 40f)
                )
                
                // Pipe Outlines
                // Top pipe body outline
                drawRect(color = Color(0xFF543847), topLeft = Offset(pipe.x, 0f), size = Size(pipeWidthPx, pipe.gapY - gapSizePx / 2), style = Stroke(width = 4f))
                // Top cap outline
                drawRect(color = Color(0xFF543847), topLeft = Offset(pipe.x - 10f, pipe.gapY - gapSizePx / 2 - 40f), size = Size(pipeWidthPx + 20f, 40f), style = Stroke(width = 4f))
                
                // Bottom pipe body outline
                drawRect(color = Color(0xFF543847), topLeft = Offset(pipe.x, pipe.gapY + gapSizePx / 2), size = Size(pipeWidthPx, size.height - (pipe.gapY + gapSizePx / 2)), style = Stroke(width = 4f))
                // Bottom cap outline
                drawRect(color = Color(0xFF543847), topLeft = Offset(pipe.x - 10f, pipe.gapY + gapSizePx / 2), size = Size(pipeWidthPx + 20f, 40f), style = Stroke(width = 4f))
            }

            // Ground base
            drawRect(
                color = Color(0xFFDED895),
                topLeft = Offset(0f, size.height - 40f),
                size = Size(size.width, 40f)
            )
            drawRect(
                color = Color(0xFF73BF2E),
                topLeft = Offset(0f, size.height - 40f),
                size = Size(size.width, 10f)
            )
            drawRect(
                color = Color(0xFF543847),
                topLeft = Offset(0f, size.height - 40f),
                size = Size(size.width, 40f),
                style = Stroke(width = 4f)
            )
        }

        // Draw Lambda using Compose
        val lambdaRotation = (velocity * 3).coerceIn(-45f, 90f)
        val lambdaXDp = with(density) { (screenWidth / 3f).toDp() }
        val lambdaYDp = with(density) { lambdaY.toDp() }
        val lambdaRadiusDp = with(density) { lambdaRadiusPx.toDp() }

        Box(
            modifier = Modifier
                .offset(
                    x = lambdaXDp - lambdaRadiusDp,
                    y = lambdaYDp - lambdaRadiusDp
                )
                .size(lambdaRadiusDp * 2)
                .rotate(lambdaRotation)
                .background(Color(0xFFF1C40F), shape = RoundedCornerShape(50))
                .border(2.dp, Color.Black, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Λ",
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // UI Layer
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xAA000000)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("← Menu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Text(
                    text = "$score",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(4f, 4f),
                            blurRadius = 8f
                        )
                    )
                )

                Text(
                    text = "High: $highScore",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }

            if (!isPlaying && !isGameOver) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "TAP TO START",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(4f, 4f),
                                blurRadius = 8f
                            )
                        )
                    )
                }
            }

            if (isGameOver) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDED895)),
                        border = BorderStroke(4.dp, Color(0xFF543847)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "GAME OVER",
                                color = Color(0xFFE74C3C),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black,
                                        offset = Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Score: $score", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF543847))
                            Text(text = "Best: $highScore", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color(0xFF543847))
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { resetGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE67E22)),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black)
                            ) {
                                Text("PLAY AGAIN", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}