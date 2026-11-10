package com.ramanstudio.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

val EXPANDED_WORD_LIST = listOf(
    "GAMES", "RAMAN", "SMART", "PHONE", "BOARD", "WORLD", "TIGER", "WATER",
    "LUCKY", "ALIEN", "BRAIN", "POWER", "MAGIC", "LIGHT", "SUPER", "FLASH",
    "CLOUD", "STORM", "PIXEL", "CYBER", "PULSE", "FOCUS", "DREAM", "SHINE",
    "OCEAN", "FLAME", "CROWN", "SOLAR", "SPACE", "GROWN", "PLANT", "SMILE"
)

@Composable
fun GuessTheWordScreen(onBack: () -> Unit) {
    var targetWord by remember { mutableStateOf(EXPANDED_WORD_LIST.random()) }
    var guesses by remember { mutableStateOf(List(6) { "" }) }
    var currentAttempt by remember { mutableIntStateOf(0) }
    var currentWord by remember { mutableStateOf("") }
    var isGameOver by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showRulesDialog by remember { mutableStateOf(false) }

    var streak by remember { mutableIntStateOf(0) }
    var totalWins by remember { mutableIntStateOf(0) }

    fun onKeyPress(key: String) {
        if (isGameOver) return

        if (key == "ENTER") {
            if (currentWord.length == 5) {
                val newGuesses = guesses.toMutableList()
                newGuesses[currentAttempt] = currentWord
                guesses = newGuesses

                if (currentWord == targetWord) {
                    isGameOver = true
                    streak++
                    totalWins++
                    message = "🎉 You Got It! 🎉"
                } else if (currentAttempt == 5) {
                    isGameOver = true
                    streak = 0
                    message = "💥 Game Over! Word was $targetWord"
                } else {
                    currentAttempt++
                    currentWord = ""
                    message = ""
                }
            } else {
                message = "Word must be 5 letters!"
            }
        } else if (key == "DEL" || key == "⌫") {
            if (currentWord.isNotEmpty()) {
                currentWord = currentWord.dropLast(1)
                message = ""
            }
        } else {
            if (currentWord.length < 5) {
                currentWord += key
                message = ""
            }
        }
    }

    fun getLetterColor(letter: Char, index: Int, attemptIndex: Int): Color {
        if (attemptIndex >= currentAttempt) return Color(0xFF1E1E1E)

        val guessWord = guesses[attemptIndex]
        return when {
            guessWord[index] == targetWord[index] -> Color(0xFF00E676) // Green
            targetWord.contains(guessWord[index]) -> Color(0xFFFFD54F) // Yellow
            else -> Color(0xFF424242) // Dark Gray
        }
    }

    fun getKeyColor(key: String): Color {
        if (key == "ENTER" || key == "⌫") return Color(0xFF2A2826)

        val charKey = key.firstOrNull() ?: return Color(0xFF2A2826)
        var bestColor = Color(0xFF2A2826)

        for (r in 0 until currentAttempt) {
            val word = guesses[r]
            for (i in word.indices) {
                if (word[i] == charKey) {
                    if (targetWord[i] == charKey) return Color(0xFF00E676) // Green
                    else if (targetWord.contains(charKey)) bestColor = Color(0xFFFFD54F) // Yellow
                    else if (bestColor == Color(0xFF2A2826)) bestColor = Color(0xFF333333) // Gray
                }
            }
        }
        return bestColor
    }

    fun restartGame() {
        targetWord = EXPANDED_WORD_LIST.random()
        guesses = List(6) { "" }
        currentAttempt = 0
        currentWord = ""
        isGameOver = false
        message = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) { Text("← Menu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }

                Text("Guess Word", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFC107))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showRulesDialog = true }) { Text("💡", fontSize = 20.sp) }
                    Button(
                        onClick = { restartGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2826)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text("↺ New", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
            }

            // Score Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(8.dp)) {
                    Text("🔥 Streak: $streak", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(8.dp)) {
                    Text("🏆 Wins: $totalWins", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Status Banner
            if (message.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        color = if (message.contains("Won") || message.contains("Got")) Color(0xFF00E676) else Color(0xFFFF5252),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6x5 Wordle Letter Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0..5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (col in 0..4) {
                            val letter = if (row == currentAttempt) {
                                currentWord.getOrNull(col) ?: ' '
                            } else {
                                guesses[row].getOrNull(col) ?: ' '
                            }

                            val boxColor = if (row < currentAttempt) getLetterColor(letter, col, row) else Color(0xFF1E1E1E)
                            val textColor = if (row < currentAttempt && boxColor == Color(0xFFFFD54F)) Color.Black else Color.White

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(boxColor, RoundedCornerShape(8.dp))
                                    .border(
                                        width = 1.5.dp,
                                        color = if (row == currentAttempt && letter != ' ') Color(0xFF00E676) else Color(0xFF333333),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter.toString(),
                                    color = textColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Color-coded On-Screen Keyboard
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            val keyboardRows = listOf(
                listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
                listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
                listOf("ENTER", "Z", "X", "C", "V", "B", "N", "M", "⌫")
            )

            for (row in keyboardRows) {
                Row(
                    modifier = Modifier.padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (key in row) {
                        val keyWidth = if (key == "ENTER" || key == "⌫") 62.dp else 32.dp
                        val bgColor = getKeyColor(key)
                        val txtColor = if (bgColor == Color(0xFFFFD54F)) Color.Black else Color.White

                        Box(
                            modifier = Modifier
                                .width(keyWidth)
                                .height(46.dp)
                                .background(bgColor, RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF444444), RoundedCornerShape(6.dp))
                                .clickable { onKeyPress(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(key, color = txtColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showRulesDialog) {
        HowToPlayDialog(
            gameTitle = "Guess the Word",
            rulesText = "• Guess the secret 5-letter word in 6 tries.\n• Green tile = Correct letter in the correct spot.\n• Yellow tile = Correct letter in the wrong spot.\n• Gray tile = Letter not in the secret word.",
            onDismiss = { showRulesDialog = false }
        )
    }
}