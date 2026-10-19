package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.model.ArenaType
import com.example.game.rendering.ArenaRenderer
import com.example.game.rendering.ShadowRenderer
import com.example.game.ui.AnnouncerOverlay
import com.example.game.ui.CombatActionCluster
import com.example.game.ui.CombatTopHud
import com.example.game.ui.DojoScreen
import com.example.game.ui.FloatingCombatTextLayer
import com.example.game.ui.FloatingVirtualJoystick
import com.example.game.ui.MatchResultDialog
import com.example.game.ui.MoveListDialog
import com.example.game.ui.PauseDialog
import com.example.game.viewmodel.GameViewModel
import com.example.game.viewmodel.ScreenState
import com.example.ui.theme.MyApplicationTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Fix 3: Immersive fullscreen: hide status and navigation bars with swipe reveal
        hideSystemBars()

        setContent {
            MyApplicationTheme {
                val viewModel: GameViewModel = viewModel()
                MainGameApp(viewModel)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun MainGameApp(viewModel: GameViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // Fix 9: Auto-pause when app goes to background
                viewModel.onAppPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val weapons by viewModel.weapons.collectAsState()
    val armors by viewModel.armors.collectAsState()
    val equippedWeapon by viewModel.equippedWeapon.collectAsState()
    val equippedArmor by viewModel.equippedArmor.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF08050D)
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState.currentScreen) {
                ScreenState.DOJO -> {
                    DojoScreen(
                        coins = uiState.coins,
                        currentBossIndex = uiState.currentBossIndex,
                        bosses = viewModel.bosses,
                        weapons = weapons,
                        armors = armors,
                        equippedWeapon = equippedWeapon,
                        equippedArmor = equippedArmor,
                        isSoundEnabled = uiState.isSoundEnabled,
                        onToggleSound = { viewModel.toggleSound() },
                        onStartBattle = { index, mode -> viewModel.startBattle(index, mode) },
                        onEquipWeapon = { viewModel.equipWeapon(it) },
                        onBuyWeapon = { viewModel.buyWeapon(it) },
                        onUpgradeWeapon = { viewModel.upgradeWeapon(it) },
                        onEquipArmor = { viewModel.equipArmor(it) },
                        onBuyArmor = { viewModel.buyArmor(it) },
                        onUpgradeArmor = { viewModel.upgradeArmor(it) },
                        onShowMoveList = { viewModel.toggleMoveList(true) }
                    )
                }

                ScreenState.COMBAT -> {
                    BackHandler {
                        if (uiState.isPaused) {
                            viewModel.resumeBattle()
                        } else {
                            viewModel.pauseBattle()
                        }
                    }

                    CombatScreen(viewModel = viewModel)
                }
            }

            // --- Move List Guide Modal ---
            if (uiState.isMoveListOpen) {
                MoveListDialog(
                    onDismiss = { viewModel.toggleMoveList(false) }
                )
            }

            // --- Pause Modal ---
            if (uiState.isPaused) {
                PauseDialog(
                    isSoundEnabled = uiState.isSoundEnabled,
                    onToggleSound = { viewModel.toggleSound() },
                    onResume = { viewModel.resumeBattle() },
                    onQuitToDojo = { viewModel.quitToDojo() }
                )
            }

            // --- Match Result Modal ---
            if (uiState.isMatchResultOpen) {
                val boss = viewModel.bosses.getOrNull(uiState.selectedBossIndex)
                MatchResultDialog(
                    isVictory = uiState.isVictory,
                    playerRounds = viewModel.playerFighter.roundsWon,
                    enemyRounds = viewModel.enemyFighter.roundsWon,
                    rewardCoins = uiState.rewardCoinsEarned,
                    bossName = boss?.name ?: "Opponent",
                    onRematch = { viewModel.startBattle(uiState.selectedBossIndex, uiState.gameMode) },
                    onNextOrDojo = { viewModel.quitToDojo() }
                )
            }
        }
    }
}

@Composable
fun CombatScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val boss = viewModel.bosses.getOrNull(uiState.selectedBossIndex)
    val arena = boss?.arena ?: ArenaType.CRIMSON_PAGODA

    var gameTime by remember { mutableFloatStateOf(0f) }
    // Fix 2: frameTick updated each 60fps frame to trigger live HUD updates
    var frameTick by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val arenaWidth = constraints.maxWidth.toFloat()
        val arenaHeight = constraints.maxHeight.toFloat()
        val groundY = arenaHeight * 0.78f

        // Fix 1: World scale u = arenaHeight / 400f
        val u = (arenaHeight / 400f).coerceIn(0.5f, 3.5f)

        // 60FPS Game Loop using withFrameNanos
        LaunchedEffect(arenaWidth, arenaHeight) {
            var lastTime = 0L
            while (true) {
                withFrameNanos { now ->
                    if (lastTime != 0L) {
                        val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                        gameTime += dt
                        viewModel.onGameTick(dt, arenaWidth, groundY, arenaHeight)
                        frameTick++
                    }
                    lastTime = now
                }
            }
        }

        // Screen Shake Effect Offset reading frameTick for 60fps responsiveness
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTick
        val shake = viewModel.combatEngine.screenShake
        val shakeOffsetX = if (shake > 0f) (Random.nextFloat() - 0.5f) * shake * 2f * u else 0f
        val shakeOffsetY = if (shake > 0f) (Random.nextFloat() - 0.5f) * shake * 2f * u else 0f

        // 1. Edge-to-Edge 2D Canvas Arena & Silhouette Rendering
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = shakeOffsetX
                    translationY = shakeOffsetY
                }
        ) {
            // Camera centers around midpoint between the two fighters
            val midX = (viewModel.playerFighter.x + viewModel.enemyFighter.x) / 2f
            val cameraOffset = midX - size.width / 2f

            // 1. Draw Parallax Atmospheric Arena (with cached Path & Brush objects)
            ArenaRenderer.drawArena(this, arena, cameraOffset, gameTime)

            // 2. Draw Projectiles (Flying Shurikens scaled by u)
            ShadowRenderer.drawProjectiles(this, viewModel.combatEngine.projectiles, u)

            // 3. Draw Fighters (Articulated Silhouettes scaled by u to 35-40% screen height)
            ShadowRenderer.drawFighter(this, viewModel.playerFighter, groundY, gameTime, u)
            ShadowRenderer.drawFighter(this, viewModel.enemyFighter, groundY, gameTime, u)

            // 4. Draw Combat Particles (Sparks, blood mist, shadow smoke scaled by u)
            ShadowRenderer.drawParticles(this, viewModel.combatEngine.particles, u)
        }

        // Floating Damage Text Numbers updating live with frameTick
        FloatingCombatTextLayer(
            texts = viewModel.combatEngine.floatingTexts,
            groundY = groundY,
            frameTick = frameTick
        )

        // Fix 3: Top HUD with safeDrawing / displayCutout padding so nothing is cut by notch
        CombatTopHud(
            player = viewModel.playerFighter,
            enemy = viewModel.enemyFighter,
            boss = boss,
            roundTimer = uiState.roundTimer,
            currentRound = uiState.currentRound,
            onPauseClick = { viewModel.pauseBattle() },
            frameTick = frameTick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding()
                .displayCutoutPadding()
        )

        // Announcer Overlay ("ROUND 1", "FIGHT!", "ROUND WON", Combo counter)
        AnnouncerOverlay(
            announcementText = uiState.announcementText,
            matchPhase = uiState.matchPhase,
            comboCount = viewModel.playerFighter.comboCount,
            frameTick = frameTick,
            modifier = Modifier.align(Alignment.Center)
        )

        // Fix 3 & 4: Touch Controls with safeDrawing / displayCutout padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .displayCutoutPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Fix 4: Floating Joystick anywhere in left half of screen
            FloatingVirtualJoystick(
                onDirectionChange = { x, y -> viewModel.onJoystickChange(x, y) },
                modifier = Modifier.align(Alignment.BottomStart)
            )

            // Fix 4: Right Combat Action Buttons Cluster (transparent, icons, press feedback, glowing shadow)
            CombatActionCluster(
                shadowEnergy = viewModel.playerFighter.shadowEnergy,
                onPunch = { viewModel.onPunchClick() },
                onKick = { viewModel.onKickClick() },
                onWeapon = { viewModel.onWeaponClick() },
                onShuriken = { viewModel.onShurikenClick() },
                onShadowBlast = { viewModel.onShadowBlastClick() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-2).dp)
            )
        }
    }
}
