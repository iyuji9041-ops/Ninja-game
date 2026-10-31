package com.example.game.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.example.game.model.ActionState
import com.example.game.model.Fighter
import com.example.game.model.FloatingText
import com.example.game.model.Particle
import com.example.game.model.ParticleType
import com.example.game.model.Projectile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object ShadowRenderer {

    fun drawFighter(
        drawScope: DrawScope,
        fighter: Fighter,
        groundY: Float,
        gameTime: Float,
        u: Float = (drawScope.size.height / 400f).coerceIn(0.5f, 3.5f)
    ) {
        with(drawScope) {
            val scaleX = if (fighter.facingRight) 1f else -1f
            val fighterX = fighter.x
            val fighterY = groundY - fighter.y // fighter.y is jump height in scaled units

            scale(scaleX = scaleX, scaleY = 1f, pivot = Offset(fighterX, groundY)) {
                // 1. Realistic ground shadow with dynamic height-based scaling & softening
                val jumpNorm = (fighter.y / (220f * u)).coerceIn(0f, 1f)
                val shadowAlpha = (1f - jumpNorm * 0.7f) * 0.65f
                val shadowWidth = (75f * u) * (1f - jumpNorm * 0.45f)
                val shadowHeight = (14f * u) * (1f - jumpNorm * 0.4f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xCC050208).copy(alpha = shadowAlpha),
                            Color(0x66050208).copy(alpha = shadowAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(fighterX, groundY - 4f * u),
                        radius = shadowWidth * 0.5f
                    ),
                    topLeft = Offset(fighterX - shadowWidth * 0.5f, groundY - shadowHeight * 0.5f - 4f * u),
                    size = Size(shadowWidth, shadowHeight)
                )

                // 2. Shadow Mode Dark Aura & Demonic Bloom
                if (fighter.isShadowMode) {
                    val pulse = sin(gameTime * 10f) * 0.15f + 0.85f
                    val auraCenter = Offset(fighterX, fighterY - 65f * u)
                    val auraColor = if (fighter.isPlayer) Color(0xFF00E5FF) else Color(0xFFD500F9)

                    // Outer soft glow bloom
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraColor.copy(alpha = 0.35f * pulse),
                                auraColor.copy(alpha = 0.12f * pulse),
                                Color.Transparent
                            ),
                            center = auraCenter,
                            radius = 110f * u * pulse
                        ),
                        center = auraCenter,
                        radius = 110f * u * pulse
                    )

                    // Inner intense flame ring
                    drawCircle(
                        color = auraColor.copy(alpha = 0.25f),
                        center = auraCenter,
                        radius = 70f * u * pulse,
                        style = Stroke(width = 3f * u)
                    )
                }

                // 3. Render Articulated High-Detail Silhouette Pose
                drawArticulatedSilhouette(fighter, fighterX, fighterY, gameTime, u)
            }
        }
    }

    private fun DrawScope.drawArticulatedSilhouette(
        fighter: Fighter,
        x: Float,
        y: Float,
        time: Float,
        u: Float
    ) {
        val silhouetteColor = Color(0xFF08050C)
        val shadowArmorColor = Color(0xFF140D1E)
        val state = fighter.actionState
        val frame = fighter.actionFrame

        // Base Anatomical Proportions
        val headRadius = 14.5f * u
        var headY = y - 110f * u
        var headX = x
        var torsoAngle = 0f
        var leftArmAngle = 20f
        var rightArmAngle = -35f
        var leftLegAngle = 15f
        var rightLegAngle = -15f
        var crouchOffset = 0f

        when (state) {
            ActionState.IDLE -> {
                val breathe = sin(time * 3f) * 2.5f * u
                headY += breathe
                torsoAngle = 4f
                leftArmAngle = 28f + breathe * 2f
                rightArmAngle = -42f - breathe * 2f
                leftLegAngle = 14f
                rightLegAngle = -14f
            }
            ActionState.WALK_FORWARD -> {
                val cycle = sin(frame * 2 * PI.toFloat())
                torsoAngle = 12f
                leftLegAngle = cycle * 36f
                rightLegAngle = -cycle * 36f
                leftArmAngle = -cycle * 32f + 20f
                rightArmAngle = cycle * 32f - 30f
                headY += abs(sin(frame * 2 * PI.toFloat())) * 3.5f * u
            }
            ActionState.WALK_BACKWARD -> {
                val cycle = sin(frame * 2 * PI.toFloat())
                torsoAngle = -8f
                leftLegAngle = -cycle * 26f
                rightLegAngle = cycle * 26f
                leftArmAngle = 55f
                rightArmAngle = 22f
            }
            ActionState.BLOCKING -> {
                torsoAngle = -14f
                leftArmAngle = 65f
                rightArmAngle = 50f
                leftLegAngle = 20f
                rightLegAngle = -18f
                // Energy Guard Shield
                drawBlockingShield(x + 22f * u, y - 65f * u, time, u)
            }
            ActionState.CROUCH -> {
                crouchOffset = 36f * u
                headY += crouchOffset
                torsoAngle = 36f
                leftLegAngle = 72f
                rightLegAngle = 42f
                leftArmAngle = 20f
                rightArmAngle = -10f
            }
            ActionState.JUMP_UP, ActionState.JUMP_FORWARD, ActionState.JUMP_BACKWARD -> {
                torsoAngle = if (state == ActionState.JUMP_FORWARD) 28f else -18f
                leftLegAngle = 48f
                rightLegAngle = 62f
                leftArmAngle = -48f
                rightArmAngle = -68f
            }
            ActionState.ROLL_FORWARD, ActionState.ROLL_BACKWARD -> {
                crouchOffset = 48f * u
                headY += crouchOffset
                torsoAngle = 85f
                leftLegAngle = 82f
                rightLegAngle = 78f
            }
            ActionState.PUNCH_JAB -> {
                val punchProgress = sin(frame * PI.toFloat())
                torsoAngle = 16f
                rightArmAngle = -40f + punchProgress * 120f
                leftArmAngle = 45f
                if (frame in 0.3f..0.7f) {
                    drawPunchStreak(x + 30f * u, y - 82f * u, punchProgress, fighter.weapon.bladeColor, u)
                }
            }
            ActionState.PUNCH_HOOK -> {
                val punchProgress = sin(frame * PI.toFloat())
                torsoAngle = 24f
                rightArmAngle = -15f + punchProgress * 130f
                leftArmAngle = 42f
                if (frame in 0.3f..0.7f) {
                    drawPunchStreak(x + 35f * u, y - 80f * u, punchProgress, fighter.weapon.bladeColor, u)
                }
            }
            ActionState.PUNCH_UPPERCUT -> {
                val punchProgress = sin(frame * PI.toFloat())
                torsoAngle = 10f - punchProgress * 22f
                rightArmAngle = 32f + punchProgress * 135f
                leftArmAngle = -22f
                drawUppercutArc(x + 28f * u, y - 78f * u, punchProgress, fighter.weapon.bladeColor, u)
            }
            ActionState.KICK_LOW -> {
                val kickProgress = sin(frame * PI.toFloat())
                torsoAngle = -22f
                rightLegAngle = -10f + kickProgress * 88f
                leftLegAngle = 26f
                leftArmAngle = 36f
                rightArmAngle = -42f
            }
            ActionState.KICK_ROUNDHOUSE -> {
                val kickProgress = sin(frame * PI.toFloat())
                torsoAngle = -16f
                rightLegAngle = -10f + kickProgress * 108f
                leftLegAngle = 12f
                leftArmAngle = -32f
                rightArmAngle = 22f
                drawKickTrail(x + 38f * u, y - 72f * u, kickProgress, u)
            }
            ActionState.KICK_FLYING -> {
                torsoAngle = -32f
                rightLegAngle = 112f
                leftLegAngle = 38f
                leftArmAngle = -46f
                rightArmAngle = -62f
                drawKickTrail(x + 42f * u, y - 65f * u, sin(frame * PI.toFloat()), u)
            }
            ActionState.WEAPON_SLASH -> {
                val slashProgress = sin(frame * PI.toFloat())
                torsoAngle = 22f
                rightArmAngle = -65f + slashProgress * 145f
                leftArmAngle = 32f
                drawSumiWeaponTrail(x + 32f * u, y - 72f * u, slashProgress, fighter.weapon.bladeColor, u)
            }
            ActionState.WEAPON_SPIN -> {
                val spinProgress = frame
                torsoAngle = sin(spinProgress * 2 * PI.toFloat()) * 32f
                rightArmAngle = spinProgress * 360f
                leftArmAngle = -spinProgress * 360f
                drawWeaponSpinAura(x, y - 58f * u, spinProgress, fighter.weapon.bladeColor, u)
            }
            ActionState.WEAPON_PIERCE -> {
                val pierceProgress = sin(frame * PI.toFloat())
                torsoAngle = 26f
                rightArmAngle = 65f * pierceProgress
                leftArmAngle = 16f
                if (frame in 0.3f..0.7f) {
                    drawPierceStreak(x + 40f * u, y - 62f * u, pierceProgress, fighter.weapon.bladeColor, u)
                }
            }
            ActionState.RANGED_THROW -> {
                torsoAngle = 16f
                rightArmAngle = if (frame < 0.45f) -85f else 65f
            }
            ActionState.SHADOW_SURGE -> {
                torsoAngle = -16f
                leftArmAngle = 82f
                rightArmAngle = 82f
                drawShadowSurgeShockwave(x, y - 58f * u, frame, u)
            }
            ActionState.HIT_LIGHT -> {
                val stagger = sin(frame * PI.toFloat())
                torsoAngle = -24f * stagger
                headX -= 14f * u * stagger
                leftArmAngle = -12f
                rightArmAngle = -22f
            }
            ActionState.HIT_HEAVY -> {
                val stagger = sin(frame * PI.toFloat())
                torsoAngle = -42f * stagger
                headX -= 28f * u * stagger
                headY += 16f * u * stagger
                leftLegAngle = 36f
                rightLegAngle = -36f
            }
            ActionState.KNOCKDOWN -> {
                val collapse = frame.coerceIn(0f, 1f)
                crouchOffset = 82f * u * collapse
                headY = y - 110f * u + crouchOffset
                torsoAngle = -86f * collapse
                leftLegAngle = -72f * collapse
                rightLegAngle = -82f * collapse
            }
            ActionState.VICTORY -> {
                val breathe = sin(time * 3f) * 2f * u
                torsoAngle = 0f
                leftArmAngle = -22f
                rightArmAngle = 70f
                headY += breathe
            }
            ActionState.DEFEAT -> {
                crouchOffset = 86f * u
                headY = y - 24f * u
                torsoAngle = -90f
                leftLegAngle = -72f
                rightLegAngle = -86f
            }
        }

        val neckY = headY + headRadius + 4f * u
        val hipY = y - 48f * u + crouchOffset * 0.7f
        val hipX = x - torsoAngle * 0.32f * u

        // --- 1. Back Leg ---
        drawSculptedLeg(silhouetteColor, hipX, hipY, leftLegAngle, isFront = false, u = u)

        // --- 2. Torso with Muscular/Armor Contour ---
        val torsoPath = Path().apply {
            moveTo(headX - 12f * u, neckY)
            // Left shoulder
            lineTo(headX - 16f * u, neckY + 12f * u)
            // Left ribcage taper
            lineTo(hipX - 11f * u, hipY)
            // Belt waist
            lineTo(hipX + 11f * u, hipY)
            // Right ribcage taper
            lineTo(headX + 16f * u, neckY + 12f * u)
            // Right shoulder
            lineTo(headX + 12f * u, neckY)
            close()
        }
        drawPath(torsoPath, color = silhouetteColor)

        // Shoulder Pauldrons / Vest Armor Accent
        val armorAccentColor = if (fighter.isShadowMode) {
            if (fighter.isPlayer) Color(0x6600E5FF) else Color(0x66D500F9)
        } else shadowArmorColor
        drawLine(
            color = armorAccentColor,
            start = Offset(headX - 15f * u, neckY + 6f * u),
            end = Offset(headX + 15f * u, neckY + 6f * u),
            strokeWidth = 4f * u,
            cap = StrokeCap.Round
        )

        // --- 3. Ninja Sash & Fluttering Belt Tails ---
        drawLine(
            color = fighter.ribbonColor,
            start = Offset(hipX - 12f * u, hipY - 6f * u),
            end = Offset(hipX + 12f * u, hipY - 6f * u),
            strokeWidth = 7f * u,
            cap = StrokeCap.Round
        )
        // Multi-point dynamic wind flutter sash
        val sashFlutter1 = sin(time * 6f + x * 0.04f) * 10f * u
        val sashFlutter2 = cos(time * 7f + x * 0.05f) * 14f * u
        val sashPath = Path().apply {
            moveTo(hipX - 6f * u, hipY - 6f * u)
            quadraticTo(
                hipX - 18f * u, hipY + 14f * u + sashFlutter1,
                hipX - 26f * u, hipY + 30f * u + sashFlutter2
            )
        }
        drawPath(sashPath, color = fighter.ribbonColor, style = Stroke(width = 4.5f * u, cap = StrokeCap.Round))

        // --- 4. Back Arm ---
        drawSculptedArm(silhouetteColor, headX - 5f * u, neckY + 6f * u, leftArmAngle, isFront = false, u = u)

        // --- 5. Front Leg ---
        drawSculptedLeg(silhouetteColor, hipX, hipY, rightLegAngle, isFront = true, u = u)

        // --- 6. Head & Cowl Mask ---
        drawCircle(
            color = silhouetteColor,
            center = Offset(headX, headY),
            radius = headRadius
        )

        // Boss Demon Horns or Shinobi Cowl crest
        if (!fighter.isPlayer) {
            val hornPath = Path().apply {
                moveTo(headX - 8f * u, headY - 10f * u)
                quadraticTo(headX - 16f * u, headY - 26f * u, headX - 10f * u, headY - 24f * u)
                lineTo(headX - 4f * u, headY - 12f * u)
                close()
            }
            drawPath(hornPath, color = silhouetteColor)
            val hornPathR = Path().apply {
                moveTo(headX + 4f * u, headY - 12f * u)
                quadraticTo(headX + 16f * u, headY - 26f * u, headX + 10f * u, headY - 24f * u)
                lineTo(headX + 8f * u, headY - 10f * u)
                close()
            }
            drawPath(hornPathR, color = silhouetteColor)
        }

        // --- 7. Flowing Ninja Scarf / Ribbon (Multi-Harmonic Wind Simulation) ---
        val wind1 = sin(time * 7.5f) * 14f * u
        val wind2 = cos(time * 9.0f) * 12f * u
        val scarfPath = Path().apply {
            moveTo(headX - 11f * u, headY + 1f * u)
            cubicTo(
                headX - 24f * u, headY + 6f * u + wind1,
                headX - 38f * u, headY + 12f * u + wind2,
                headX - 54f * u, headY + 18f * u + wind1 * 0.7f
            )
        }
        drawPath(scarfPath, color = fighter.ribbonColor, style = Stroke(width = 4.5f * u, cap = StrokeCap.Round))

        // --- 8. Glowing Slit Eyes & Motion Trail ---
        val eyeX = headX + 5f * u
        val eyeY = headY - 1f * u
        val eyeTrailLen = (fighter.vx * 0.05f * u).coerceIn(-18f * u, 18f * u)
        // Eye Glow Slit
        drawLine(
            color = fighter.eyeColor,
            start = Offset(eyeX, eyeY),
            end = Offset(eyeX + 7f * u, eyeY - 1.2f * u),
            strokeWidth = 3.5f * u,
            cap = StrokeCap.Round
        )
        // Trailing Eye Streak
        if (abs(eyeTrailLen) > 2f * u) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(fighter.eyeColor.copy(alpha = 0.8f), Color.Transparent),
                    startX = eyeX,
                    endX = eyeX - eyeTrailLen
                ),
                start = Offset(eyeX, eyeY),
                end = Offset(eyeX - eyeTrailLen, eyeY),
                strokeWidth = 2.5f * u,
                cap = StrokeCap.Round
            )
        }

        // --- 9. Front Arm & Weapon ---
        drawSculptedArmWithWeapon(
            silhouetteColor,
            headX + 5f * u,
            neckY + 6f * u,
            rightArmAngle,
            fighter,
            u
        )
    }

    private fun DrawScope.drawSculptedLeg(
        color: Color,
        hipX: Float,
        hipY: Float,
        angleDeg: Float,
        isFront: Boolean,
        u: Float
    ) {
        val rad = angleDeg * PI.toFloat() / 180f
        val thighLen = 29f * u
        val shinLen = 29f * u

        val kneeX = hipX + sin(rad) * thighLen
        val kneeY = hipY + cos(rad) * thighLen

        val shinAngle = rad - 0.22f
        val footX = kneeX + sin(shinAngle) * shinLen
        val footY = kneeY + cos(shinAngle) * shinLen

        val legColor = if (isFront) color else Color(0xFF040206)

        // Muscular Thigh
        drawLine(
            color = legColor,
            start = Offset(hipX, hipY),
            end = Offset(kneeX, kneeY),
            strokeWidth = 11.5f * u,
            cap = StrokeCap.Round
        )
        // Calves & Shin
        drawLine(
            color = legColor,
            start = Offset(kneeX, kneeY),
            end = Offset(footX, footY),
            strokeWidth = 9.5f * u,
            cap = StrokeCap.Round
        )
        // Tabi Shinobi Foot (slight toe split shape)
        drawLine(
            color = legColor,
            start = Offset(footX, footY),
            end = Offset(footX + 12f * u, footY),
            strokeWidth = 7f * u,
            cap = StrokeCap.Round
        )
    }

    private fun DrawScope.drawSculptedArm(
        color: Color,
        shoulderX: Float,
        shoulderY: Float,
        angleDeg: Float,
        isFront: Boolean,
        u: Float
    ) {
        val rad = angleDeg * PI.toFloat() / 180f
        val upperLen = 23f * u
        val lowerLen = 21f * u

        val elbowX = shoulderX + cos(rad) * upperLen
        val elbowY = shoulderY + sin(rad) * upperLen

        val wristX = elbowX + cos(rad + 0.45f) * lowerLen
        val wristY = elbowY + sin(rad + 0.45f) * lowerLen

        val armColor = if (isFront) color else Color(0xFF040206)

        drawLine(color = armColor, start = Offset(shoulderX, shoulderY), end = Offset(elbowX, elbowY), strokeWidth = 9f * u, cap = StrokeCap.Round)
        drawLine(color = armColor, start = Offset(elbowX, elbowY), end = Offset(wristX, wristY), strokeWidth = 8f * u, cap = StrokeCap.Round)
    }

    private fun DrawScope.drawSculptedArmWithWeapon(
        color: Color,
        shoulderX: Float,
        shoulderY: Float,
        angleDeg: Float,
        fighter: Fighter,
        u: Float
    ) {
        val rad = angleDeg * PI.toFloat() / 180f
        val upperLen = 23f * u
        val lowerLen = 21f * u

        val elbowX = shoulderX + cos(rad) * upperLen
        val elbowY = shoulderY + sin(rad) * upperLen

        val wristX = elbowX + cos(rad - 0.38f) * lowerLen
        val wristY = elbowY + sin(rad - 0.38f) * lowerLen

        // Upper arm (Bicep/shoulder)
        drawLine(color = color, start = Offset(shoulderX, shoulderY), end = Offset(elbowX, elbowY), strokeWidth = 9.5f * u, cap = StrokeCap.Round)
        // Forearm with forearm wrap
        drawLine(color = color, start = Offset(elbowX, elbowY), end = Offset(wristX, wristY), strokeWidth = 8f * u, cap = StrokeCap.Round)

        // Hand fist
        drawCircle(color = color, center = Offset(wristX, wristY), radius = 5.5f * u)

        // Render Upgraded Weapon
        drawWeapon(wristX, wristY, rad - 0.38f, fighter, u)
    }

    private fun DrawScope.drawWeapon(
        handX: Float,
        handY: Float,
        handAngleRad: Float,
        fighter: Fighter,
        u: Float
    ) {
        val weapon = fighter.weapon
        val bladeColor = weapon.bladeColor
        val angleDeg = handAngleRad * 180f / PI.toFloat()

        rotate(degrees = angleDeg, pivot = Offset(handX, handY)) {
            when {
                weapon.id.contains("fists") -> {
                    // Flaming Chi Aura Fist Wraps
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(bladeColor, Color.White, Color.Transparent),
                            center = Offset(handX + 3f * u, handY),
                            radius = 12f * u
                        ),
                        center = Offset(handX + 3f * u, handY),
                        radius = 12f * u
                    )
                }
                weapon.id.contains("claws") -> {
                    // Triple Serrated Razor Claws
                    for (c in -1..1) {
                        val cy = handY + c * 5f * u
                        val clawPath = Path().apply {
                            moveTo(handX - 2f * u, cy)
                            quadraticTo(handX + 24f * u, cy - 6f * u, handX + 36f * u, cy - 2f * u)
                            lineTo(handX + 22f * u, cy + 3f * u)
                            close()
                        }
                        // Core blade
                        drawPath(clawPath, color = bladeColor)
                        // Glowing edge
                        drawPath(clawPath, color = Color.White, style = Stroke(width = 1.2f * u))
                    }
                }
                weapon.id.contains("naginata") -> {
                    // Shaft
                    drawLine(
                        color = Color(0xFF1E1428),
                        start = Offset(handX - 28f * u, handY),
                        end = Offset(handX + 50f * u, handY),
                        strokeWidth = 4.5f * u,
                        cap = StrokeCap.Round
                    )
                    // Gold collars
                    drawLine(color = Color(0xFFFFD54F), start = Offset(handX + 46f * u, handY - 4f * u), end = Offset(handX + 46f * u, handY + 4f * u), strokeWidth = 3f * u)
                    // Curved sweeping polearm blade
                    val bladePath = Path().apply {
                        moveTo(handX + 48f * u, handY - 3f * u)
                        quadraticTo(handX + 75f * u, handY - 10f * u, handX + 92f * u, handY - 18f * u)
                        lineTo(handX + 72f * u, handY + 3f * u)
                        close()
                    }
                    drawPath(bladePath, color = bladeColor)
                    drawPath(bladePath, color = Color.White, style = Stroke(width = 1.5f * u))
                }
                weapon.id.contains("kusarigama") -> {
                    // Sickle handle & head
                    drawLine(color = Color(0xFF181220), start = Offset(handX - 6f * u, handY), end = Offset(handX + 16f * u, handY - 26f * u), strokeWidth = 4f * u)
                    val kamaBlade = Path().apply {
                        moveTo(handX + 14f * u, handY - 26f * u)
                        quadraticTo(handX + 44f * u, handY - 42f * u, handX + 48f * u, handY - 14f * u)
                        lineTo(handX + 20f * u, handY - 22f * u)
                        close()
                    }
                    drawPath(kamaBlade, color = bladeColor)
                    drawPath(kamaBlade, color = Color.White, style = Stroke(width = 1.5f * u))
                    // Hanging chain links
                    val chainPath = Path().apply {
                        moveTo(handX - 6f * u, handY)
                        quadraticTo(handX - 16f * u, handY + 22f * u, handX - 8f * u, handY + 38f * u)
                    }
                    drawPath(chainPath, color = Color(0xFFB0BEC5), style = Stroke(width = 2.5f * u))
                    drawCircle(color = Color(0xFF37474F), center = Offset(handX - 8f * u, handY + 38f * u), radius = 5f * u)
                }
                weapon.id.contains("cleavers") -> {
                    // Massive Heavy Demonic Cleaver
                    val cleaverPath = Path().apply {
                        moveTo(handX, handY)
                        lineTo(handX + 40f * u, handY - 24f * u)
                        lineTo(handX + 68f * u, handY - 22f * u)
                        lineTo(handX + 64f * u, handY + 18f * u)
                        lineTo(handX + 22f * u, handY + 14f * u)
                        close()
                    }
                    drawPath(cleaverPath, color = Color(0xFF1B0B2B))
                    drawPath(cleaverPath, color = bladeColor, style = Stroke(width = 2.5f * u))
                }
                else -> {
                    // Katana (Sleek sharpened blade with Hamon wave line)
                    // Tsuka Hilt
                    drawLine(
                        color = Color(0xFF140D1E),
                        start = Offset(handX - 14f * u, handY),
                        end = Offset(handX + 3f * u, handY),
                        strokeWidth = 5.5f * u,
                        cap = StrokeCap.Round
                    )
                    // Golden Tsuba Guard
                    drawLine(
                        color = Color(0xFFFFC107),
                        start = Offset(handX + 3f * u, handY - 7f * u),
                        end = Offset(handX + 3f * u, handY + 7f * u),
                        strokeWidth = 3.5f * u,
                        cap = StrokeCap.Round
                    )
                    // Curved Steel Blade
                    val bladeLen = 60f * weapon.range * u
                    val bladePath = Path().apply {
                        moveTo(handX + 3f * u, handY)
                        quadraticTo(handX + bladeLen * 0.6f, handY - 5f * u, handX + bladeLen, handY - 12f * u)
                    }
                    // Outer polished steel
                    drawPath(bladePath, color = Color(0xFFF5F5F7), style = Stroke(width = 3.8f * u, cap = StrokeCap.Round))
                    // Inner glowing spirit core
                    drawPath(bladePath, color = bladeColor, style = Stroke(width = 2.0f * u, cap = StrokeCap.Round))
                }
            }
        }
    }

    // --- Dynamic Sumi-e Ink Slash Trails & Attack FX ---

    private fun DrawScope.drawSumiWeaponTrail(x: Float, y: Float, progress: Float, bladeColor: Color, u: Float) {
        val trailAlpha = sin(progress * PI.toFloat()) * 0.85f
        val arcPath = Path().apply {
            moveTo(x - 24f * u, y + 45f * u)
            quadraticTo(x + 78f * u, y - 56f * u, x + 24f * u, y - 90f * u)
        }
        // Outer glow blade arc
        drawPath(
            arcPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    bladeColor.copy(alpha = trailAlpha),
                    Color.White.copy(alpha = trailAlpha * 0.95f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = 18f * u, cap = StrokeCap.Round)
        )
        // Inner white razor line
        drawPath(
            arcPath,
            color = Color.White.copy(alpha = trailAlpha),
            style = Stroke(width = 4f * u, cap = StrokeCap.Round)
        )
    }

    private fun DrawScope.drawUppercutArc(x: Float, y: Float, progress: Float, color: Color, u: Float) {
        val alpha = sin(progress * PI.toFloat()) * 0.85f
        val arcPath = Path().apply {
            moveTo(x - 12f * u, y + 54f * u)
            quadraticTo(x + 44f * u, y, x + 12f * u, y - 68f * u)
        }
        drawPath(
            arcPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = alpha), color.copy(alpha = alpha), Color.Transparent)
            ),
            style = Stroke(width = 14f * u, cap = StrokeCap.Round)
        )
    }

    private fun DrawScope.drawKickTrail(x: Float, y: Float, progress: Float, u: Float) {
        val alpha = sin(progress * PI.toFloat()) * 0.7f
        val arcPath = Path().apply {
            moveTo(x - 18f * u, y + 35f * u)
            quadraticTo(x + 55f * u, y, x + 30f * u, y - 48f * u)
        }
        drawPath(
            arcPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFEEEEEE).copy(alpha = alpha), Color.Transparent)
            ),
            style = Stroke(width = 10f * u, cap = StrokeCap.Round)
        )
    }

    private fun DrawScope.drawPunchStreak(x: Float, y: Float, progress: Float, color: Color, u: Float) {
        val alpha = sin(progress * PI.toFloat()) * 0.75f
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = alpha), Color.White.copy(alpha = alpha))
            ),
            start = Offset(x - 30f * u, y),
            end = Offset(x + 15f * u, y),
            strokeWidth = 6f * u,
            cap = StrokeCap.Round
        )
    }

    private fun DrawScope.drawPierceStreak(x: Float, y: Float, progress: Float, color: Color, u: Float) {
        val alpha = sin(progress * PI.toFloat()) * 0.8f
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = alpha), Color.White.copy(alpha = alpha))
            ),
            start = Offset(x - 40f * u, y),
            end = Offset(x + 35f * u, y),
            strokeWidth = 8f * u,
            cap = StrokeCap.Round
        )
    }

    private fun DrawScope.drawWeaponSpinAura(x: Float, y: Float, progress: Float, color: Color, u: Float) {
        val spinRadius = 60f * u
        val alpha = (sin(progress * PI.toFloat()) * 0.7f).coerceIn(0f, 1f)
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(color.copy(alpha = alpha), Color.White.copy(alpha = alpha), Color.Transparent),
                center = Offset(x, y)
            ),
            center = Offset(x, y),
            radius = spinRadius,
            style = Stroke(width = 14f * u)
        )
    }

    private fun DrawScope.drawBlockingShield(x: Float, y: Float, time: Float, u: Float) {
        val shieldRadius = 45f * u
        val pulse = sin(time * 12f) * 0.1f + 0.9f
        // Energy Shield Barrier
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x44FFD54F),
                    Color(0x88FFB300),
                    Color.Transparent
                ),
                center = Offset(x, y),
                radius = shieldRadius * pulse
            ),
            center = Offset(x, y),
            radius = shieldRadius * pulse
        )
        // Outer Hexagonal Rings
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.85f),
            center = Offset(x, y),
            radius = shieldRadius * pulse,
            style = Stroke(width = 3.5f * u)
        )
    }

    private fun DrawScope.drawShadowSurgeShockwave(x: Float, y: Float, frame: Float, u: Float) {
        val radius = 160f * frame * u
        val alpha = (1f - frame).coerceIn(0f, 1f)
        drawCircle(
            color = Color(0xFFD500F9).copy(alpha = alpha * 0.7f),
            center = Offset(x, y),
            radius = radius,
            style = Stroke(width = (14f - frame * 10f).coerceAtLeast(2f) * u)
        )
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = alpha * 0.9f),
            center = Offset(x, y),
            radius = (radius * 0.75f),
            style = Stroke(width = 4f * u)
        )
    }

    // --- Particles & Projectiles ---

    fun drawParticles(drawScope: DrawScope, particles: List<Particle>, groundY: Float, u: Float) {
        with(drawScope) {
            for (p in particles) {
                val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                val py = groundY - p.y
                when (p.type) {
                    ParticleType.SPARK, ParticleType.SLASH_SPARK -> {
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            center = Offset(p.x, py),
                            radius = p.size * u * (alpha * 0.5f + 0.5f)
                        )
                    }
                    ParticleType.BLOOD_MIST -> {
                        drawCircle(
                            color = Color(0xFFD32F2F).copy(alpha = alpha * 0.85f),
                            center = Offset(p.x, py),
                            radius = p.size * u
                        )
                    }
                    ParticleType.SHADOW_SMOKE -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF7B1FA2).copy(alpha = alpha * 0.6f), Color.Transparent),
                                center = Offset(p.x, py),
                                radius = p.size * 2f * u
                            ),
                            center = Offset(p.x, py),
                            radius = p.size * 2f * u
                        )
                    }
                    ParticleType.DUST_CLOUD -> {
                        drawCircle(
                            color = Color(0x779E9E9E).copy(alpha = alpha * 0.5f),
                            center = Offset(p.x, py),
                            radius = p.size * (1f + (1f - alpha)) * u
                        )
                    }
                    ParticleType.CHERRY_LEAF -> {
                        // Falling pink/red leaf with rotation
                        val leafPath = Path().apply {
                            moveTo(p.x, py - p.size * u)
                            quadraticTo(p.x + p.size * 1.5f * u, py, p.x, py + p.size * u)
                            quadraticTo(p.x - p.size * 1.5f * u, py, p.x, py - p.size * u)
                            close()
                        }
                        drawPath(leafPath, color = p.color.copy(alpha = alpha))
                    }
                    ParticleType.FIREFLY -> {
                        // Glowing pulsing firefly
                        drawCircle(
                            color = Color(0xEEFFEB3B).copy(alpha = alpha),
                            center = Offset(p.x, py),
                            radius = p.size * u
                        )
                    }
                    ParticleType.SHOCKWAVE -> {
                        val expand = (1f - alpha) * 80f * u
                        drawCircle(
                            color = p.color.copy(alpha = alpha * 0.8f),
                            center = Offset(p.x, py),
                            radius = expand,
                            style = Stroke(width = 3f * u)
                        )
                    }
                }
            }
        }
    }

    fun drawProjectiles(drawScope: DrawScope, projectiles: List<Projectile>, groundY: Float, u: Float) {
        with(drawScope) {
            for (p in projectiles) {
                if (!p.active) continue
                val py = groundY - p.y
                rotate(degrees = p.rotation, pivot = Offset(p.x, py)) {
                    val starSize = 10f * u
                    val starPath = Path().apply {
                        // 4-Pointed Ninja Shuriken
                        moveTo(p.x, py - starSize * 1.5f)
                        lineTo(p.x + starSize * 0.4f, py - starSize * 0.4f)
                        lineTo(p.x + starSize * 1.5f, py)
                        lineTo(p.x + starSize * 0.4f, py + starSize * 0.4f)
                        lineTo(p.x, py + starSize * 1.5f)
                        lineTo(p.x - starSize * 0.4f, py + starSize * 0.4f)
                        lineTo(p.x - starSize * 1.5f, py)
                        lineTo(p.x - starSize * 0.4f, py - starSize * 0.4f)
                        close()
                    }
                    drawPath(starPath, color = Color(0xFFCFD8DC))
                    drawPath(starPath, color = Color(0xFF263238), style = Stroke(width = 1.5f * u))
                    drawCircle(color = Color(0xFF101010), center = Offset(p.x, py), radius = 2.5f * u)
                }
            }
        }
    }
}
