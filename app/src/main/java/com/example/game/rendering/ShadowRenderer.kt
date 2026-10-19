package com.example.game.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.cos
import kotlin.math.sin

object ShadowRenderer {

    // Fix 1: World scale u = size.height / 400f applied to fighter body parts, limbs, weapons, trails
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
                // Ground shadow beneath fighter scaled by u
                val shadowAlpha = (1f - (fighter.y / (200f * u)).coerceIn(0f, 0.7f)) * 0.6f
                val shadowWidth = 70f * u * (1f - (fighter.y / (350f * u)).coerceIn(0f, 0.5f))
                drawOval(
                    color = Color(0x99050308).copy(alpha = shadowAlpha),
                    topLeft = Offset(fighterX - shadowWidth / 2, groundY - 6f * u),
                    size = Size(shadowWidth, 12f * u)
                )

                // Shadow Mode dark aura scaled by u
                if (fighter.isShadowMode) {
                    val pulse = (sin(gameTime * 12f) * 0.2f + 0.8f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x889C27B0),
                                Color(0x444A148C),
                                Color(0x00000000)
                            ),
                            center = Offset(fighterX, fighterY - 65f * u),
                            radius = 95f * u * pulse
                        ),
                        center = Offset(fighterX, fighterY - 65f * u),
                        radius = 95f * u * pulse
                    )
                }

                // Render Silhouette Pose based on ActionState scaled by u
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
        val silhouetteColor = Color(0xFF07040A)
        val state = fighter.actionState
        val frame = fighter.actionFrame // 0.0 to 1.0 progress in current action

        // Joint offsets and radii scaled by u (Fighters appear ~35-40% of screen height)
        val headRadius = 14f * u
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
                leftArmAngle = 30f + breathe * 2f
                rightArmAngle = -45f - breathe * 2f
                leftLegAngle = 12f
                rightLegAngle = -12f
            }
            ActionState.WALK_FORWARD -> {
                val cycle = sin(frame * 2 * PI.toFloat())
                torsoAngle = 12f
                leftLegAngle = cycle * 35f
                rightLegAngle = -cycle * 35f
                leftArmAngle = -cycle * 30f + 20f
                rightArmAngle = cycle * 30f - 30f
                headY += kotlin.math.abs(sin(frame * 2 * PI.toFloat())) * 3f * u
            }
            ActionState.WALK_BACKWARD -> {
                val cycle = sin(frame * 2 * PI.toFloat())
                torsoAngle = -8f
                leftLegAngle = -cycle * 25f
                rightLegAngle = cycle * 25f
                leftArmAngle = 55f // Guard stance arms high
                rightArmAngle = 20f
            }
            ActionState.BLOCKING -> {
                torsoAngle = -12f
                leftArmAngle = 65f // Crossed guard
                rightArmAngle = 45f
                leftLegAngle = 18f
                rightLegAngle = -15f
            }
            ActionState.CROUCH -> {
                crouchOffset = 35f * u
                headY += crouchOffset
                torsoAngle = 35f
                leftLegAngle = 70f
                rightLegAngle = 40f
                leftArmAngle = 20f
                rightArmAngle = -10f
            }
            ActionState.JUMP_UP, ActionState.JUMP_FORWARD, ActionState.JUMP_BACKWARD -> {
                torsoAngle = if (state == ActionState.JUMP_FORWARD) 25f else -15f
                leftLegAngle = 45f
                rightLegAngle = 60f
                leftArmAngle = -50f
                rightArmAngle = -70f
            }
            ActionState.ROLL_FORWARD, ActionState.ROLL_BACKWARD -> {
                crouchOffset = 45f * u
                headY += crouchOffset
                torsoAngle = 80f
                leftLegAngle = 80f
                rightLegAngle = 75f
            }
            ActionState.PUNCH_JAB -> {
                val punchProgress = sin(frame * PI.toFloat())
                torsoAngle = 15f
                rightArmAngle = -45f + punchProgress * 115f // Punch lunging forward
                leftArmAngle = 45f // Guarding face
            }
            ActionState.PUNCH_HOOK -> {
                val punchProgress = sin(frame * PI.toFloat())
                torsoAngle = 22f
                rightArmAngle = -20f + punchProgress * 125f
                leftArmAngle = 40f
            }
            ActionState.PUNCH_UPPERCUT -> {
                val punchProgress = sin(frame * PI.toFloat())
                torsoAngle = 10f - punchProgress * 20f
                rightArmAngle = 30f + punchProgress * 130f // Rising uppercut
                leftArmAngle = -20f
                drawUppercutArc(x + 25f * u, y - 80f * u, punchProgress, fighter.weapon.bladeColor, u)
            }
            ActionState.KICK_LOW -> {
                val kickProgress = sin(frame * PI.toFloat())
                torsoAngle = -20f
                rightLegAngle = -10f + kickProgress * 85f // Sweeping low
                leftLegAngle = 25f
                leftArmAngle = 35f
                rightArmAngle = -40f
            }
            ActionState.KICK_ROUNDHOUSE -> {
                val kickProgress = sin(frame * PI.toFloat())
                torsoAngle = -15f
                rightLegAngle = -10f + kickProgress * 105f // High horizontal kick
                leftLegAngle = 10f
                leftArmAngle = -30f
                rightArmAngle = 20f
                drawKickTrail(x + 35f * u, y - 75f * u, kickProgress, u)
            }
            ActionState.KICK_FLYING -> {
                torsoAngle = -30f
                rightLegAngle = 110f // Thrust straight out
                leftLegAngle = 35f
                leftArmAngle = -45f
                rightArmAngle = -60f
            }
            ActionState.WEAPON_SLASH -> {
                val slashProgress = sin(frame * PI.toFloat())
                torsoAngle = 20f
                rightArmAngle = -60f + slashProgress * 140f
                leftArmAngle = 30f
                drawWeaponTrail(x + 30f * u, y - 75f * u, slashProgress, fighter.weapon.bladeColor, u)
            }
            ActionState.WEAPON_SPIN -> {
                val spinProgress = frame
                torsoAngle = sin(spinProgress * 2 * PI.toFloat()) * 30f
                rightArmAngle = spinProgress * 360f
                leftArmAngle = -spinProgress * 360f
                drawWeaponSpinAura(x, y - 60f * u, spinProgress, fighter.weapon.bladeColor, u)
            }
            ActionState.WEAPON_PIERCE -> {
                val pierceProgress = sin(frame * PI.toFloat())
                torsoAngle = 25f
                rightArmAngle = 60f * pierceProgress
                leftArmAngle = 15f
            }
            ActionState.RANGED_THROW -> {
                val throwProgress = frame
                torsoAngle = 15f
                rightArmAngle = if (throwProgress < 0.5f) -80f else 60f
            }
            ActionState.SHADOW_SURGE -> {
                torsoAngle = -15f
                leftArmAngle = 80f
                rightArmAngle = 80f
                drawShadowSurgeShockwave(x, y - 60f * u, frame, u)
            }
            ActionState.HIT_LIGHT -> {
                val stagger = sin(frame * PI.toFloat())
                torsoAngle = -22f * stagger
                headX -= 12f * u * stagger
                leftArmAngle = -10f
                rightArmAngle = -20f
            }
            ActionState.HIT_HEAVY -> {
                val stagger = sin(frame * PI.toFloat())
                torsoAngle = -40f * stagger
                headX -= 25f * u * stagger
                headY += 15f * u * stagger
                leftLegAngle = 35f
                rightLegAngle = -35f
            }
            ActionState.KNOCKDOWN -> {
                val collapse = frame.coerceIn(0f, 1f)
                crouchOffset = 80f * u * collapse
                headY = y - 110f * u + crouchOffset
                torsoAngle = -85f * collapse
                leftLegAngle = -70f * collapse
                rightLegAngle = -80f * collapse
            }
            ActionState.VICTORY -> {
                val breathe = sin(time * 3f) * 2f * u
                torsoAngle = 0f
                leftArmAngle = -20f
                rightArmAngle = 65f
                headY += breathe
            }
            ActionState.DEFEAT -> {
                crouchOffset = 85f * u
                headY = y - 25f * u
                torsoAngle = -90f
                leftLegAngle = -70f
                rightLegAngle = -85f
            }
        }

        val neckY = headY + headRadius + 4f * u
        val hipY = y - 48f * u + crouchOffset * 0.7f
        val hipX = x - torsoAngle * 0.3f * u

        // --- Back Leg ---
        drawLeg(silhouetteColor, hipX, hipY, leftLegAngle, isFront = false, u = u)

        // --- Torso ---
        val torsoPath = Path().apply {
            moveTo(headX - 10f * u, neckY)
            lineTo(headX + 12f * u, neckY)
            lineTo(hipX + 11f * u, hipY)
            lineTo(hipX - 11f * u, hipY)
            close()
        }
        drawPath(torsoPath, color = silhouetteColor)

        // Ninja Sash / Belt
        drawLine(
            color = fighter.ribbonColor,
            start = Offset(hipX - 11f * u, hipY - 6f * u),
            end = Offset(hipX + 11f * u, hipY - 6f * u),
            strokeWidth = 6f * u
        )
        // Belt tails fluttering behind
        val sashFlutter = sin(time * 5f + x * 0.05f) * 8f * u
        val sashPath = Path().apply {
            moveTo(hipX - 6f * u, hipY - 6f * u)
            quadraticTo(hipX - 18f * u, hipY + 12f * u + sashFlutter, hipX - 24f * u, hipY + 28f * u + sashFlutter)
        }
        drawPath(sashPath, color = fighter.ribbonColor, style = Stroke(width = 4f * u, cap = StrokeCap.Round))

        // --- Back Arm ---
        drawArm(silhouetteColor, headX - 4f * u, neckY + 6f * u, leftArmAngle, isFront = false, u = u)

        // --- Front Leg ---
        drawLeg(silhouetteColor, hipX, hipY, rightLegAngle, isFront = true, u = u)

        // --- Head & Cowl ---
        drawCircle(
            color = silhouetteColor,
            center = Offset(headX, headY),
            radius = headRadius
        )

        // Ninja Headband / Scarf Ribbon (Wind flutter)
        val scarfFlutter1 = sin(time * 7f) * 12f * u
        val scarfFlutter2 = cos(time * 8f) * 10f * u
        val scarfPath = Path().apply {
            moveTo(headX - 10f * u, headY)
            quadraticTo(headX - 25f * u, headY + 5f * u + scarfFlutter1, headX - 42f * u, headY + 14f * u + scarfFlutter2)
        }
        drawPath(scarfPath, color = fighter.ribbonColor, style = Stroke(width = 4f * u, cap = StrokeCap.Round))

        // Glowing Ninja Eyes (Shadow Fight signature glowing slits)
        val eyeX = headX + 5f * u
        val eyeY = headY - 1f * u
        drawLine(
            color = fighter.eyeColor,
            start = Offset(eyeX, eyeY),
            end = Offset(eyeX + 6f * u, eyeY - 1f * u),
            strokeWidth = 3f * u,
            cap = StrokeCap.Round
        )

        // --- Front Arm & Weapon ---
        drawArmWithWeapon(
            silhouetteColor,
            headX + 4f * u,
            neckY + 6f * u,
            rightArmAngle,
            fighter,
            u
        )
    }

    private fun DrawScope.drawLeg(
        color: Color,
        hipX: Float,
        hipY: Float,
        angleDeg: Float,
        isFront: Boolean,
        u: Float
    ) {
        val rad = angleDeg * PI.toFloat() / 180f
        val thighLen = 28f * u
        val shinLen = 28f * u

        val kneeX = hipX + sin(rad) * thighLen
        val kneeY = hipY + cos(rad) * thighLen

        val shinAngle = rad - 0.2f
        val footX = kneeX + sin(shinAngle) * shinLen
        val footY = kneeY + cos(shinAngle) * shinLen

        val legColor = if (isFront) color else Color(0xFF030205)

        // Thigh
        drawLine(
            color = legColor,
            start = Offset(hipX, hipY),
            end = Offset(kneeX, kneeY),
            strokeWidth = 10f * u,
            cap = StrokeCap.Round
        )
        // Shin
        drawLine(
            color = legColor,
            start = Offset(kneeX, kneeY),
            end = Offset(footX, footY),
            strokeWidth = 8f * u,
            cap = StrokeCap.Round
        )
        // Tabi Foot
        drawLine(
            color = legColor,
            start = Offset(footX, footY),
            end = Offset(footX + 10f * u, footY),
            strokeWidth = 6f * u,
            cap = StrokeCap.Round
        )
    }

    private fun DrawScope.drawArm(
        color: Color,
        shoulderX: Float,
        shoulderY: Float,
        angleDeg: Float,
        isFront: Boolean,
        u: Float
    ) {
        val rad = angleDeg * PI.toFloat() / 180f
        val upperLen = 22f * u
        val lowerLen = 20f * u

        val elbowX = shoulderX + cos(rad) * upperLen
        val elbowY = shoulderY + sin(rad) * upperLen

        val wristX = elbowX + cos(rad + 0.5f) * lowerLen
        val wristY = elbowY + sin(rad + 0.5f) * lowerLen

        val armColor = if (isFront) color else Color(0xFF030205)

        drawLine(color = armColor, start = Offset(shoulderX, shoulderY), end = Offset(elbowX, elbowY), strokeWidth = 8f * u, cap = StrokeCap.Round)
        drawLine(color = armColor, start = Offset(elbowX, elbowY), end = Offset(wristX, wristY), strokeWidth = 7f * u, cap = StrokeCap.Round)
    }

    private fun DrawScope.drawArmWithWeapon(
        color: Color,
        shoulderX: Float,
        shoulderY: Float,
        angleDeg: Float,
        fighter: Fighter,
        u: Float
    ) {
        val rad = angleDeg * PI.toFloat() / 180f
        val upperLen = 22f * u
        val lowerLen = 20f * u

        val elbowX = shoulderX + cos(rad) * upperLen
        val elbowY = shoulderY + sin(rad) * upperLen

        val wristX = elbowX + cos(rad - 0.4f) * lowerLen
        val wristY = elbowY + sin(rad - 0.4f) * lowerLen

        drawLine(color = color, start = Offset(shoulderX, shoulderY), end = Offset(elbowX, elbowY), strokeWidth = 8f * u, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(elbowX, elbowY), end = Offset(wristX, wristY), strokeWidth = 7f * u, cap = StrokeCap.Round)

        // Draw Weapon attached to hand scaled by u
        drawWeapon(wristX, wristY, rad - 0.4f, fighter, u)
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
                    // Glowing hand wraps
                    drawCircle(color = bladeColor, center = Offset(handX + 3f * u, handY), radius = 5f * u)
                }
                weapon.id.contains("claws") -> {
                    // Triple Wolverine/Lynx style sharp claws
                    for (c in -1..1) {
                        val cy = handY + c * 4f * u
                        val clawPath = Path().apply {
                            moveTo(handX, cy)
                            quadraticTo(handX + 22f * u, cy - 4f * u, handX + 32f * u, cy)
                            lineTo(handX + 20f * u, cy + 2f * u)
                            close()
                        }
                        drawPath(clawPath, color = bladeColor)
                    }
                }
                weapon.id.contains("naginata") -> {
                    // Long shaft + curved blade
                    drawLine(
                        color = Color(0xFF1E1428),
                        start = Offset(handX - 25f * u, handY),
                        end = Offset(handX + 45f * u, handY),
                        strokeWidth = 4f * u
                    )
                    // Naginata Blade
                    val bladePath = Path().apply {
                        moveTo(handX + 45f * u, handY - 3f * u)
                        quadraticTo(handX + 70f * u, handY - 8f * u, handX + 85f * u, handY - 14f * u)
                        lineTo(handX + 68f * u, handY + 2f * u)
                        close()
                    }
                    drawPath(bladePath, color = bladeColor)
                }
                weapon.id.contains("kusarigama") -> {
                    // Scythe blade and hanging chain
                    val scythePath = Path().apply {
                        moveTo(handX, handY)
                        lineTo(handX + 15f * u, handY - 25f * u)
                        quadraticTo(handX + 38f * u, handY - 35f * u, handX + 42f * u, handY - 12f * u)
                        lineTo(handX + 18f * u, handY - 20f * u)
                        close()
                    }
                    drawPath(scythePath, color = bladeColor)
                }
                else -> {
                    // Default Katana (Blood Edge / Shadow Katana)
                    // Hilt (Tsuka)
                    drawLine(
                        color = Color(0xFF181220),
                        start = Offset(handX - 12f * u, handY),
                        end = Offset(handX + 3f * u, handY),
                        strokeWidth = 5f * u
                    )
                    // Guard (Tsuba)
                    drawLine(
                        color = Color(0xFFFFB300),
                        start = Offset(handX + 3f * u, handY - 6f * u),
                        end = Offset(handX + 3f * u, handY + 6f * u),
                        strokeWidth = 3f * u
                    )
                    // Curved Katana Blade with glowing edge
                    val bladeLen = 55f * weapon.range * u
                    val bladePath = Path().apply {
                        moveTo(handX + 3f * u, handY)
                        quadraticTo(handX + bladeLen * 0.6f, handY - 4f * u, handX + bladeLen, handY - 10f * u)
                    }
                    drawPath(bladePath, color = Color(0xFFEEEEF0), style = Stroke(width = 3.5f * u))
                    drawPath(bladePath, color = bladeColor, style = Stroke(width = 2f * u))
                }
            }
        }
    }

    // --- Dynamic Attack Trails ---

    private fun DrawScope.drawWeaponTrail(x: Float, y: Float, progress: Float, bladeColor: Color, u: Float) {
        val trailAlpha = sin(progress * PI.toFloat()) * 0.7f
        val arcPath = Path().apply {
            moveTo(x - 20f * u, y + 40f * u)
            quadraticTo(x + 70f * u, y - 50f * u, x + 20f * u, y - 80f * u)
        }
        drawPath(
            arcPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    bladeColor.copy(alpha = trailAlpha),
                    Color.White.copy(alpha = trailAlpha * 0.9f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = 16f * u, cap = StrokeCap.Round)
        )
    }

    private fun DrawScope.drawUppercutArc(x: Float, y: Float, progress: Float, color: Color, u: Float) {
        val alpha = sin(progress * PI.toFloat()) * 0.8f
        val arcPath = Path().apply {
            moveTo(x - 10f * u, y + 50f * u)
            quadraticTo(x + 40f * u, y, x + 10f * u, y - 60f * u)
        }
        drawPath(
            arcPath,
            color = color.copy(alpha = alpha),
            style = Stroke(width = 12f * u, cap = StrokeCap.Round)
        )
    }

    private fun DrawScope.drawKickTrail(x: Float, y: Float, progress: Float, u: Float) {
        val alpha = sin(progress * PI.toFloat()) * 0.6f
        val arcPath = Path().apply {
            moveTo(x - 15f * u, y + 30f * u)
            quadraticTo(x + 55f * u, y, x + 40f * u, y - 40f * u)
        }
        drawPath(
            arcPath,
            color = Color(0xFFFF9800).copy(alpha = alpha),
            style = Stroke(width = 10f * u, cap = StrokeCap.Round)
        )
    }

    private fun DrawScope.drawWeaponSpinAura(x: Float, y: Float, progress: Float, color: Color, u: Float) {
        val radius = 65f * u
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.5f),
                    Color.White.copy(alpha = 0.7f),
                    Color.Transparent
                ),
                center = Offset(x, y),
                radius = radius
            ),
            center = Offset(x, y),
            radius = radius,
            style = Stroke(width = 14f * u)
        )
    }

    private fun DrawScope.drawShadowSurgeShockwave(x: Float, y: Float, progress: Float, u: Float) {
        val radius = progress * 140f * u
        val alpha = (1f - progress).coerceIn(0f, 1f)
        drawCircle(
            color = Color(0xFFD500F9).copy(alpha = alpha * 0.8f),
            center = Offset(x, y),
            radius = radius,
            style = Stroke(width = 18f * (1f - progress) * u)
        )
    }

    // --- Projectiles & Particles ---

    fun drawProjectiles(drawScope: DrawScope, projectiles: List<Projectile>, u: Float = (drawScope.size.height / 400f).coerceIn(0.5f, 3.5f)) {
        with(drawScope) {
            for (p in projectiles) {
                if (!p.active) continue
                rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                    // Shuriken (4-point ninja star) scaled by u
                    val starPath = Path().apply {
                        moveTo(p.x, p.y - 14f * u)
                        lineTo(p.x + 4f * u, p.y - 4f * u)
                        lineTo(p.x + 14f * u, p.y)
                        lineTo(p.x + 4f * u, p.y + 4f * u)
                        lineTo(p.x, p.y + 14f * u)
                        lineTo(p.x - 4f * u, p.y + 4f * u)
                        lineTo(p.x - 14f * u, p.y)
                        lineTo(p.x - 4f * u, p.y - 4f * u)
                        close()
                    }
                    drawPath(starPath, color = Color(0xFFECEFF1))
                    drawCircle(color = Color(0xFF00E5FF), center = Offset(p.x, p.y), radius = 3.5f * u)
                }
            }
        }
    }

    fun drawParticles(drawScope: DrawScope, particles: List<Particle>, u: Float = (drawScope.size.height / 400f).coerceIn(0.5f, 3.5f)) {
        with(drawScope) {
            for (p in particles) {
                val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                when (p.type) {
                    ParticleType.SPARK -> {
                        drawLine(
                            color = p.color.copy(alpha = alpha),
                            start = Offset(p.x, p.y),
                            end = Offset(p.x - p.vx * 2f, p.y - p.vy * 2f),
                            strokeWidth = p.size * u,
                            cap = StrokeCap.Round
                        )
                    }
                    ParticleType.BLOOD_MIST, ParticleType.SHADOW_SMOKE -> {
                        drawCircle(
                            color = p.color.copy(alpha = alpha * 0.7f),
                            center = Offset(p.x, p.y),
                            radius = p.size * u * (2f - alpha)
                        )
                    }
                    ParticleType.CHERRY_LEAF -> {
                        drawOval(
                            color = p.color.copy(alpha = alpha * 0.85f),
                            topLeft = Offset(p.x, p.y),
                            size = Size(p.size * 2f * u, p.size * u)
                        )
                    }
                    ParticleType.FIREFLY -> {
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            center = Offset(p.x, p.y),
                            radius = p.size * u
                        )
                    }
                    ParticleType.SHOCKWAVE -> {
                        drawCircle(
                            color = p.color.copy(alpha = alpha * 0.6f),
                            center = Offset(p.x, p.y),
                            radius = p.size * u * (1f - alpha),
                            style = Stroke(width = 4f * u)
                        )
                    }
                }
            }
        }
    }

    fun drawFloatingTexts(drawScope: DrawScope, texts: List<FloatingText>) {
        // Floating texts are drawn in FloatingCombatTextLayer with live frameTick
    }
}
