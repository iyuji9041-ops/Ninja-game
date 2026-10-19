package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.BossProfile
import com.example.game.model.Fighter
import com.example.game.model.FloatingText
import com.example.game.model.MatchPhase
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.EnergyBlue
import com.example.ui.theme.EnergyPurple
import com.example.ui.theme.FlameAmber
import com.example.ui.theme.GoldYellow
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.HealthRed
import com.example.ui.theme.ShadowPurple
import com.example.ui.theme.SoulCyan
import kotlin.math.roundToInt

// --- Top HUD: Health Bars, Damage Trail, Round Timer, Pause Button ---
@Composable
fun CombatTopHud(
    player: Fighter,
    enemy: Fighter,
    boss: BossProfile?,
    roundTimer: Int,
    currentRound: Int,
    onPauseClick: () -> Unit,
    frameTick: Int,
    modifier: Modifier = Modifier
) {
    // Reading frameTick triggers live recomposition every 60fps frame
    @Suppress("UNUSED_VARIABLE")
    val tick = frameTick

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // --- Left: Player Health Card ---
        FighterHealthCard(
            fighter = player,
            name = "SHADOW",
            subtitle = player.weapon.name,
            isPlayer = true,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(14.dp))

        // --- Center: Round & Timer ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Surface(
                color = Color(0xCC181024),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A3866)),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text = "ROUND $currentRound",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlameAmber,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }

            Text(
                text = "$roundTimer",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (roundTimer <= 15) HealthRed else GoldYellow,
                modifier = Modifier.testTag("round_timer")
            )

            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("pause_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color(0xFFC4B8D8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // --- Right: Enemy Health Card ---
        FighterHealthCard(
            fighter = enemy,
            name = boss?.name ?: "NINJA",
            subtitle = boss?.title ?: "Dojo Challenger",
            isPlayer = false,
            modifier = Modifier.weight(1f)
        )
    }
}

// Fix 2: Health bar with smooth animation and delayed damage trail bar behind it
@Composable
private fun FighterHealthCard(
    fighter: Fighter,
    name: String,
    subtitle: String,
    isPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    val hpFraction = (fighter.health / fighter.maxHealth).coerceIn(0f, 1f)
    val shadowFraction = (fighter.shadowEnergy / 100f).coerceIn(0f, 1f)

    // Animated fast bar
    val animatedHp by animateFloatAsState(
        targetValue = hpFraction,
        animationSpec = tween(durationMillis = 80),
        label = "hp_fast"
    )

    // Delayed Damage Trail bar (amber/yellow trail staying behind, then catching up smoothly)
    val damageTrailHp by animateFloatAsState(
        targetValue = hpFraction,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "hp_trail"
    )

    // Animated shadow energy bar
    val animatedShadow by animateFloatAsState(
        targetValue = shadowFraction,
        animationSpec = tween(durationMillis = 150),
        label = "shadow_energy"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = if (isPlayer) Alignment.Start else Alignment.End
    ) {
        // Name & Round Win Gems
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isPlayer) Arrangement.Start else Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isPlayer) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                RoundGems(fighter.roundsWon)
            } else {
                RoundGems(fighter.roundsWon)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }

        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = Color(0xFFAAA0C0),
            maxLines = 1,
            modifier = Modifier.padding(bottom = 3.dp)
        )

        // Health Bar with Delayed Damage Trail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(15.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF22162B))
                .border(1.dp, Color(0xFF4A3459), RoundedCornerShape(4.dp))
        ) {
            // Layer 1 (Behind): Damage Trail (Gold/Amber)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = damageTrailHp)
                    .fillMaxHeight()
                    .align(if (isPlayer) Alignment.CenterStart else Alignment.CenterEnd)
                    .background(Color(0xFFFFB300))
            )

            // Layer 2 (Front): Current HP (Green-Amber or Crimson-Red gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedHp)
                    .fillMaxHeight()
                    .align(if (isPlayer) Alignment.CenterStart else Alignment.CenterEnd)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (hpFraction > 0.35f) {
                                listOf(HealthGreen, Color(0xFFFFD54F))
                            } else {
                                listOf(HealthRed, Color(0xFFFF5252))
                            }
                        )
                    )
            )
        }

        // Shadow Energy Bar (Blue/Purple)
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF140D22))
                .border(0.5.dp, Color(0xFF3B2859), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedShadow)
                    .fillMaxHeight()
                    .align(if (isPlayer) Alignment.CenterStart else Alignment.CenterEnd)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(EnergyBlue, EnergyPurple)
                        )
                    )
            )
        }
    }
}

@Composable
private fun RoundGems(roundsWon: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..2) {
            val won = roundsWon >= i
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (won) GoldYellow else Color(0xFF382D4A))
                    .border(1.dp, if (won) Color(0xFFFFE082) else Color(0xFF4D3E64), CircleShape)
            )
        }
    }
}

// --- Dynamic Announcer Banner & Combo Counter ---
@Composable
fun AnnouncerOverlay(
    announcementText: String,
    matchPhase: MatchPhase,
    comboCount: Int,
    frameTick: Int,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_VARIABLE")
    val tick = frameTick

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Main Cinematic Announcement Text
        AnimatedVisibility(
            visible = announcementText.isNotEmpty(),
            enter = scaleIn(initialScale = 0.6f) + fadeIn(),
            exit = scaleOut(targetScale = 1.3f) + fadeOut()
        ) {
            Surface(
                color = Color(0xE60D0714),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, FlameAmber),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = announcementText,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = when {
                        announcementText.contains("FIGHT") -> Color(0xFFFF5252)
                        announcementText.contains("VICTORY") -> GoldYellow
                        announcementText.contains("DEFEAT") -> Color(0xFFCFD8DC)
                        else -> SoulCyan
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                )
            }
        }

        // Live Combo Counter on Left
        if (comboCount > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 36.dp, y = (-36).dp)
            ) {
                Column {
                    Text(
                        text = "COMBO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA59EB3),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "x$comboCount",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = FlameAmber
                    )
                }
            }
        }
    }
}

// --- Floating Combat Text Layer ---
@Composable
fun FloatingCombatTextLayer(
    texts: List<FloatingText>,
    groundY: Float,
    frameTick: Int
) {
    @Suppress("UNUSED_VARIABLE")
    val tick = frameTick

    Box(modifier = Modifier.fillMaxSize()) {
        for (ft in texts) {
            val screenY = groundY - ft.y
            Text(
                text = ft.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = ft.color.copy(alpha = ft.alpha),
                modifier = Modifier.offset {
                    IntOffset(ft.x.roundToInt() - 40, screenY.roundToInt())
                }
            )
        }
    }
}

// Fix 4: Floating Joystick that appears where the thumb touches in the left half of the screen
@Composable
fun FloatingVirtualJoystick(
    onDirectionChange: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val baseRadius = 56.dp
    val knobRadius = 24.dp
    val maxRadiusPx = with(density) { (baseRadius - 16.dp).toPx() }

    var isTouchActive by remember { mutableStateOf(false) }
    var baseCenter by remember { mutableStateOf(Offset.Zero) }
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isTouchActive = true
                    baseCenter = down.position
                    knobOffset = Offset.Zero
                    down.consume()

                    do {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (pointer.pressed) {
                            val drag = pointer.position - baseCenter
                            val dist = drag.getDistance()
                            val clamped = if (dist > maxRadiusPx) drag * (maxRadiusPx / dist) else drag
                            knobOffset = clamped
                            val normX = (clamped.x / maxRadiusPx).coerceIn(-1f, 1f)
                            val normY = (-clamped.y / maxRadiusPx).coerceIn(-1f, 1f)
                            onDirectionChange(normX, normY)
                            pointer.consume()
                        }
                    } while (event.changes.any { it.id == down.id && it.pressed })

                    // Gesture ended
                    isTouchActive = false
                    knobOffset = Offset.Zero
                    onDirectionChange(0f, 0f)
                }
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        // Default resting center if not actively touching
        val restingCenter = Offset(widthPx * 0.35f, heightPx * 0.72f)
        val activeCenter = if (isTouchActive) baseCenter else restingCenter
        val alpha = if (isTouchActive) 0.85f else 0.35f

        // Joystick Base Ring
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (activeCenter.x - with(density) { baseRadius.toPx() }).roundToInt(),
                        (activeCenter.y - with(density) { baseRadius.toPx() }).roundToInt()
                    )
                }
                .size(baseRadius * 2)
                .clip(CircleShape)
                .background(Color(0x33140C20).copy(alpha = alpha * 0.4f))
                .border(2.dp, Color(0xFF6B4A8C).copy(alpha = alpha), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Knob
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt())
                    }
                    .size(knobRadius * 2)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF7E57C2).copy(alpha = alpha),
                                Color(0xFF281C38).copy(alpha = alpha)
                            )
                        )
                    )
                    .border(1.5.dp, SoulCyan.copy(alpha = alpha), CircleShape)
            )
        }
    }
}

// Fix 4: Action cluster with larger transparent action buttons, icons, press feedback, haptics,
// and SHADOW button always visible (greyed out when < 100, glowing when 100).
@Composable
fun CombatActionCluster(
    shadowEnergy: Float,
    onPunch: () -> Unit,
    onKick: () -> Unit,
    onWeapon: () -> Unit,
    onShuriken: () -> Unit,
    onShadowBlast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canShadow = shadowEnergy >= 100f
    val infiniteTransition = rememberInfiniteTransition(label = "shadow_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier.size(205.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center: Primary Weapon Slash Button (Large 70dp)
        CombatActionButton(
            label = "SLASH",
            icon = Icons.Default.FlashOn,
            accentColor = BloodCrimson,
            size = 68.dp,
            onClick = onWeapon,
            modifier = Modifier
                .align(Alignment.Center)
                .testTag("action_weapon")
        )

        // Top: Strike / Punch (56dp)
        CombatActionButton(
            label = "STRIKE",
            icon = Icons.Default.SportsMartialArts,
            accentColor = Color(0xFFFF5252),
            size = 54.dp,
            onClick = onPunch,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .testTag("action_punch")
        )

        // Right: Kick (56dp)
        CombatActionButton(
            label = "KICK",
            icon = Icons.AutoMirrored.Filled.Send,
            accentColor = FlameAmber,
            size = 54.dp,
            onClick = onKick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .testTag("action_kick")
        )

        // Bottom: Kunai / Shuriken (50dp)
        CombatActionButton(
            label = "KUNAI",
            icon = Icons.Default.AutoAwesome,
            accentColor = SoulCyan,
            size = 48.dp,
            onClick = onShuriken,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag("action_shuriken")
        )

        // Left: SHADOW Power Button (ALWAYS visible: greyed out or glowing)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .scale(if (canShadow) glowScale else 1f)
        ) {
            CombatActionButton(
                label = if (canShadow) "SHADOW" else "${shadowEnergy.toInt()}%",
                icon = Icons.Default.Whatshot,
                accentColor = if (canShadow) ShadowPurple else Color(0xFF555555),
                isGlowing = canShadow,
                isEnabled = canShadow,
                size = 54.dp,
                onClick = onShadowBlast,
                modifier = Modifier.testTag("action_shadow")
            )
        }
    }
}

// Fix 4: Transparent action button with vector icon, label, press feedback, and haptics
@Composable
private fun CombatActionButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    size: androidx.compose.ui.unit.Dp,
    isGlowing: Boolean = false,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale = if (isPressed) 0.90f else 1f
    val bgColor = when {
        isPressed -> accentColor.copy(alpha = 0.35f)
        isGlowing -> Color(0x667E57C2)
        !isEnabled -> Color(0x33100B18)
        else -> Color(0x44181024)
    }
    val borderColor = when {
        isGlowing -> Color(0xFFE040FB)
        !isEnabled -> Color(0x44555555)
        else -> accentColor
    }
    val contentColor = when {
        isGlowing -> Color.White
        !isEnabled -> Color(0xFF777777)
        else -> Color.White
    }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (isEnabled) onClick()
        },
        interactionSource = interactionSource,
        color = bgColor,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(if (isGlowing) 2.5.dp else 1.5.dp, borderColor),
        modifier = modifier
            .size(size)
            .scale(scale)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isGlowing) SoulCyan else if (!isEnabled) Color(0xFF666666) else accentColor,
                modifier = Modifier.size((size.value * 0.44f).dp)
            )
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                letterSpacing = 0.5.sp
            )
        }
    }
}
