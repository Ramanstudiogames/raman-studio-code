package com.ramanstudio.games

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramanstudio.games.games.game2048.Game2048Screen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RamanStudioApp()
                }
            }
        }
    }
}

enum class AppScreen {
    Splash,
    MainMenu,
    Game2048,
    TicTacToe,
    MemoryMatch,
    Snake,
    Connect4,
    Minesweeper,
    GuessTheWord,
    Ludo,
    Chess,
    Checkers,
    FlappyLambda,
    Sudoku,
    Nonogram,
    DotsAndBoxes,
    SlidingTiles,
    BrickBreaker,
    SpaceInvaders,
    EchoDrop,
    SnakesAndLadders
}

data class GameMenuItem(
    val title: String,
    val subtitle: String,
    val screen: AppScreen,
    val cardBgGradient: List<Color>,
    val badgeContent: @Composable () -> Unit
)

@Composable
fun RamanStudioApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.Splash) }

    when (currentScreen) {
        AppScreen.Splash -> SplashScreen { currentScreen = AppScreen.MainMenu }
        AppScreen.MainMenu -> MainMenuScreen { selectedScreen -> currentScreen = selectedScreen }
        AppScreen.Game2048 -> Game2048Screen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.TicTacToe -> TicTacToeScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.MemoryMatch -> MemoryMatchScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.Snake -> SnakeScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.Connect4 -> Connect4Screen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.Minesweeper -> MinesweeperScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.GuessTheWord -> GuessTheWordScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.Ludo -> LudoScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.Chess -> ChessScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.Checkers -> CheckersScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.FlappyLambda -> FlappyLambdaScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.Sudoku -> SudokuScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.Nonogram -> NonogramScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.DotsAndBoxes -> DotsAndBoxesScreen(onBack = { currentScreen = AppScreen.MainMenu })
        AppScreen.SlidingTiles -> {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                context.startActivity(Intent(context, SlidingTilesActivity::class.java))
                currentScreen = AppScreen.MainMenu
            }
        }
        AppScreen.BrickBreaker -> {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                context.startActivity(Intent(context, BrickBreakerActivity::class.java))
                currentScreen = AppScreen.MainMenu
            }
        }
        AppScreen.SpaceInvaders -> {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                context.startActivity(Intent(context, SpaceInvadersActivity::class.java))
                currentScreen = AppScreen.MainMenu
            }
        }
        AppScreen.EchoDrop -> {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                context.startActivity(Intent(context, EchoDropActivity::class.java))
                currentScreen = AppScreen.MainMenu
            }
        }
        AppScreen.SnakesAndLadders -> {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                context.startActivity(Intent(context, SetupActivity::class.java))
                currentScreen = AppScreen.MainMenu
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startFade by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startFade = true
        delay(2200)
        onTimeout()
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startFade) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "fade"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alphaAnim)
        ) {
            Text(
                text = "Λ",
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "RΛMΛN",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp
            )
            Text(
                text = "S T U D I O S",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 8.sp
            )
        }
    }
}

@Composable
fun MainMenuScreen(onNavigate: (AppScreen) -> Unit) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }

    val gamesList = listOf(
        GameMenuItem(
            title = "Echo Drop ★",
            subtitle = "Competitive Tile Drop",
            screen = AppScreen.EchoDrop,
            cardBgGradient = listOf(Color(0xFF311B92), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFFC107), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔮", fontSize = 26.sp)
                }
            }
        ),
        GameMenuItem(
            title = "2048",
            subtitle = "Slide & Merge Tiles",
            screen = AppScreen.Game2048,
            cardBgGradient = listOf(Color(0xFF3E2723), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFEDC22E), RoundedCornerShape(12.dp))
                        .border(2.dp, Color(0xFFF2B179), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("2048", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        ),
        GameMenuItem(
            title = "Tic-Tac-Toe",
            subtitle = "3-in-a-Row Strategy",
            screen = AppScreen.TicTacToe,
            cardBgGradient = listOf(Color(0xFF1A237E), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("X", color = Color(0xFF00E5FF), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("O", color = Color(0xFFFF4081), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        ),
        GameMenuItem(
            title = "Memory Match",
            subtitle = "3D Flip & Find Pairs",
            screen = AppScreen.MemoryMatch,
            cardBgGradient = listOf(Color(0xFF4A148C), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF311B92), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFE040FB), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧠", fontSize = 26.sp)
                }
            }
        ),
        GameMenuItem(
            title = "Snake",
            subtitle = "Classic Arcade Growth",
            screen = AppScreen.Snake,
            cardBgGradient = listOf(Color(0xFF1B5E20), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2E7D32), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFFC107), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐍", fontSize = 26.sp)
                }
            }
        ),
        GameMenuItem(
            title = "Connect 4",
            subtitle = "Four-in-a-Line Drop",
            screen = AppScreen.Connect4,
            cardBgGradient = listOf(Color(0xFF0D47A1), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1565C0), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFF64B5F6), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("🔴", fontSize = 13.sp)
                        Text("🟡", fontSize = 13.sp)
                    }
                }
            }
        ),
        GameMenuItem(
            title = "Minesweeper",
            subtitle = "Uncover Safe Cells",
            screen = AppScreen.Minesweeper,
            cardBgGradient = listOf(Color(0xFFB71C1C), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFF5252), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💣", fontSize = 24.sp)
                }
            }
        ),
        GameMenuItem(
            title = "Guess Word",
            subtitle = "5-Letter Wordle Challenge",
            screen = AppScreen.GuessTheWord,
            cardBgGradient = listOf(Color(0xFFF57F17), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFFD54F), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("WORD", color = Color(0xFFFFC107), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        ),
        GameMenuItem(
            title = "Ludo",
            subtitle = "4-Player Authentic Board",
            screen = AppScreen.Ludo,
            cardBgGradient = listOf(Color(0xFF880E4F), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFF4081), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎲", fontSize = 26.sp)
                }
            }
        ),
        GameMenuItem(
            title = "Chess",
            subtitle = "Classic Staunton Match",
            screen = AppScreen.Chess,
            cardBgGradient = listOf(Color(0xFF3E2723), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♚", color = Color.White, fontSize = 26.sp)
                }
            }
        ),
        GameMenuItem(
            title = "Checkers",
            subtitle = "Diagonal Crown Jumps",
            screen = AppScreen.Checkers,
            cardBgGradient = listOf(Color(0xFFBF360C), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFF1744), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👑", fontSize = 24.sp)
                }
            }
        ),
        GameMenuItem(
            title = "Flappy Lambda",
            subtitle = "Tap to Flap!",
            screen = AppScreen.FlappyLambda,
            cardBgGradient = listOf(Color(0xFF00796B), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF1C40F), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Λ", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }
            }
        ),
        GameMenuItem(
            title = "Sudoku",
            subtitle = "Brain Puzzle",
            screen = AppScreen.Sudoku,
            cardBgGradient = listOf(Color(0xFF512DA8), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFFC107), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧩", fontSize = 26.sp)
                }
            }
        ),
        GameMenuItem(
            title = "Nonogram",
            subtitle = "Picross Logic Art",
            screen = AppScreen.Nonogram,
            cardBgGradient = listOf(Color(0xFF006064), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFFC107), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🖼️", fontSize = 26.sp)
                }
            }
        ),
        GameMenuItem(
            title = "Dots & Boxes",
            subtitle = "Classic Line Strategy",
            screen = AppScreen.DotsAndBoxes,
            cardBgGradient = listOf(Color(0xFFB71C1C), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFFC107), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🟥", fontSize = 26.sp)
                }
            }
        ),
        GameMenuItem(
            title = "15-Puzzle",
            subtitle = "Sliding Tiles Logic",
            screen = AppScreen.SlidingTiles,
            cardBgGradient = listOf(Color(0xFF004D40), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFFC107), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔢", fontSize = 26.sp)
                }
            }
        ),
        GameMenuItem(
            title = "Brick Breaker",
            subtitle = "60 FPS Arcade Action",
            screen = AppScreen.BrickBreaker,
            cardBgGradient = listOf(Color(0xFFE65100), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFFC107), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧱", fontSize = 26.sp)
                }
            }
        ),
        GameMenuItem(
            title = "Space Invaders",
            subtitle = "Alien Swarm Arcade",
            screen = AppScreen.SpaceInvaders,
            cardBgGradient = listOf(Color(0xFF311B92), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFFC107), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👾", fontSize = 26.sp)
                }
            }
        ),
        GameMenuItem(
            title = "Snakes & Ladders",
            subtitle = "10-Player Board Classic",
            screen = AppScreen.SnakesAndLadders,
            cardBgGradient = listOf(Color(0xFF1B5E20), Color(0xFF1B1B1B)),
            badgeContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121), RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFFFC107), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐍", fontSize = 26.sp)
                }
            }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Top Header
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            // Hamburger Menu Button
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("☰", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }

            // Dropdown Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF1E1E1E))
            ) {
                DropdownMenuItem(
                    text = { Text("Request A Game", color = Color.White) },
                    onClick = {
                        showMenu = false
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:ramanchapagain06@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Request for a new game in 19-in-1 App")
                        }
                        context.startActivity(intent)
                    }
                )
                DropdownMenuItem(
                    text = { Text("What's new?", color = Color.White) },
                    onClick = {
                        showMenu = false
                        showWhatsNewDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("About", color = Color.White) },
                    onClick = {
                        showMenu = false
                        showAboutDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Developer", color = Color.White) },
                    onClick = {
                        showMenu = false
                        showDeveloperDialog = true
                    }
                )
            }

            // Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = "RΛMΛN",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp
                )
                Text(
                    text = "G A M E S",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Larger, Graphical Grid of Games
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(gamesList) { game ->
                LargeGraphicalGameCard(item = game) {
                    onNavigate(game.screen)
                }
            }
        }
    }

    if (showWhatsNewDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsNewDialog = false },
            title = { Text("What's New in v1.1.0", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "• 19 Games Suite: Expanded collection with 19 total classic board and arcade games!\n\n" +
                    "• ★ EXCLUSIVE Echo Drop: Play our new flagship competitive puzzle game with 3-tier AI Memory bots!\n\n" +
                    "• 🐍 10-Player Snakes & Ladders: Play with up to 10 players on 3 difficulty maps with realistic serpentine snakes!\n\n" +
                    "• ⚡ In-Game Perks: Space Invaders now drops falling Health Boosts and Twin Lasers!\n\n" +
                    "• ✨ UI & Theme Overhaul: Sleek Gold/Amber visual themes, pre-game setup screens, and 60 FPS performance optimizations.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                TextButton(onClick = { showWhatsNewDialog = false }) {
                    Text("Awesome!", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About App", color = Color.White) },
            text = {
                Text(
                    "Raman Studio Games\n\nVersion: 1.1.0\n\nA beautiful 19-in-1 collection of classic board and arcade games, featuring 10-Player Snakes & Ladders!",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = Color(0xFFFFC107))
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    if (showDeveloperDialog) {
        AlertDialog(
            onDismissRequest = { showDeveloperDialog = false },
            title = { Text("Developer", color = Color.White) },
            text = {
                Text(
                    "Name: Raman\nFrom: Nepal\nDiscord Username: toomuchvat\n\nThank you for playing!",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeveloperDialog = false }) {
                    Text("Close", color = Color(0xFFFFC107))
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

@Composable
fun LargeGraphicalGameCard(item: GameMenuItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .clickable { onClick() }
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(item.cardBgGradient))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.size(54.dp)) {
                        item.badgeContent()
                    }

                    Text("▶", color = Color(0xFFFFC107), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Column {
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = item.subtitle,
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }
            }
        }
    }
}