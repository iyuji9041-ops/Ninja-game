package com.example.game.physics

import androidx.compose.ui.graphics.Color
import com.example.game.audio.SoundManager
import com.example.game.model.ActionState
import com.example.game.model.ArmorItem
import com.example.game.model.BossProfile
import com.example.game.model.Fighter
import com.example.game.model.FloatingText
import com.example.game.model.Particle
import com.example.game.model.ParticleType
import com.example.game.model.Projectile
import kotlin.math.abs
import kotlin.random.Random

class CombatEngine(
    private val soundManager: SoundManager
) {
    var screenShake: Float = 0f
    val particles = mutableListOf<Particle>()
    val projectiles = mutableListOf<Projectile>()
    val floatingTexts = mutableListOf<FloatingText>()

    private var nextProjectileId = 1L

    // Fix 1: Accept world scale u = size.height / 400f to scale physics, speeds, ranges, gravity, and bounds
    fun update(
        player: Fighter,
        enemy: Fighter,
        currentBoss: BossProfile?,
        playerArmor: ArmorItem?,
        dt: Float,
        arenaWidth: Float,
        groundY: Float,
        u: Float = 1.0f
    ) {
        // Decay screen shake
        if (screenShake > 0f) {
            screenShake = (screenShake - dt * 25f).coerceAtLeast(0f)
        }

        // Update Fighters physics & actions with world scale u
        updateFighter(player, enemy, dt, arenaWidth, u)
        updateFighter(enemy, player, dt, arenaWidth, u)

        // Enforce spacing & body collisions scaled by u
        val minDist = 40f * u
        val dist = enemy.x - player.x
        val minBoundary = 60f * u
        val maxBoundary = arenaWidth - 60f * u

        if (abs(dist) < minDist) {
            val overlap = minDist - abs(dist)
            val push = overlap * 0.5f
            if (player.x < enemy.x) {
                player.x = (player.x - push).coerceAtLeast(minBoundary)
                enemy.x = (enemy.x + push).coerceAtMost(maxBoundary)
            } else {
                player.x = (player.x + push).coerceAtMost(maxBoundary)
                enemy.x = (enemy.x - push).coerceAtLeast(minBoundary)
            }
        }

        // Always face each other unless rolling/jumping past
        if (!isAcrobaticAction(player.actionState)) {
            player.facingRight = player.x <= enemy.x
        }
        if (!isAcrobaticAction(enemy.actionState)) {
            enemy.facingRight = enemy.x <= player.x
        }

        // AI Logic for Enemy scaled by u
        if (enemy.health > 0f && player.health > 0f) {
            updateEnemyAI(enemy, player, currentBoss, dt, u)
        }

        // Hit Detection with armor defenseBonus, boss multipliers, and world scale ranges
        checkAttackHit(player, enemy, currentBoss, playerArmor, u)
        checkAttackHit(enemy, player, currentBoss, playerArmor, u)

        // Projectiles update with dynamic dt, blocking check, roll invulnerability, and world scale
        updateProjectiles(player, enemy, currentBoss, playerArmor, arenaWidth, dt, u)

        // Particles & Floating Text Updates
        updateParticles(dt, u)
        updateFloatingTexts(dt, u)
    }

    private fun updateFighter(fighter: Fighter, opponent: Fighter, dt: Float, arenaWidth: Float, u: Float) {
        // Hit cooldown
        if (fighter.hitCooldown > 0f) {
            fighter.hitCooldown -= dt
        }

        // Combo timer decay
        if (fighter.comboTimer > 0f) {
            fighter.comboTimer -= dt
            if (fighter.comboTimer <= 0f) {
                fighter.comboCount = 0
            }
        }

        // Shadow mode duration
        if (fighter.isShadowMode) {
            fighter.shadowModeTimer -= dt
            if (fighter.shadowModeTimer <= 0f) {
                fighter.isShadowMode = false
            }
        }

        // Fix 3: Global vertical physics applied whenever in air (y > 0 or vy != 0), scaled by gravity * u
        if (fighter.y > 0f || fighter.vy != 0f) {
            fighter.y += fighter.vy * dt
            fighter.vy -= 850f * u * dt // Scaled gravity
            if (fighter.y <= 0f) {
                fighter.y = 0f
                fighter.vy = 0f
                if (fighter.actionState == ActionState.JUMP_UP ||
                    fighter.actionState == ActionState.JUMP_FORWARD ||
                    fighter.actionState == ActionState.JUMP_BACKWARD ||
                    fighter.actionState == ActionState.KICK_FLYING
                ) {
                    setAction(fighter, ActionState.IDLE, 0.4f)
                }
            }
        }

        // Fix 6: Set isInvulnerable during first half of a roll (actionFrame in 0f..0.5f)
        if (fighter.actionState == ActionState.ROLL_FORWARD || fighter.actionState == ActionState.ROLL_BACKWARD) {
            fighter.isInvulnerable = fighter.actionFrame in 0f..0.5f
        } else if (fighter.actionState != ActionState.KNOCKDOWN && fighter.actionState != ActionState.DEFEAT) {
            fighter.isInvulnerable = false
        }

        // Action State Machine
        fighter.actionFrame += (dt / fighter.actionDuration) * fighter.weapon.speed

        val minBoundary = 60f * u
        val maxBoundary = arenaWidth - 60f * u

        when (fighter.actionState) {
            ActionState.WALK_FORWARD -> {
                val speed = 140f * u * dt
                val dir = if (fighter.facingRight) 1f else -1f
                fighter.x = (fighter.x + dir * speed).coerceIn(minBoundary, maxBoundary)
                if (fighter.actionFrame >= 1f) {
                    // Fix 2: Enemy walk returns to IDLE after its duration (~0.5s) so AI attacks again
                    if (fighter.isPlayer) {
                        fighter.actionFrame = 0f
                    } else {
                        setAction(fighter, ActionState.IDLE, 0.4f)
                    }
                }
            }
            ActionState.WALK_BACKWARD -> {
                val speed = 100f * u * dt
                val dir = if (fighter.facingRight) -1f else 1f
                fighter.x = (fighter.x + dir * speed).coerceIn(minBoundary, maxBoundary)
                if (fighter.actionFrame >= 1f) {
                    // Fix 2: Enemy walk returns to IDLE after its duration (~0.5s) so AI attacks again
                    if (fighter.isPlayer) {
                        fighter.actionFrame = 0f
                    } else {
                        setAction(fighter, ActionState.IDLE, 0.4f)
                    }
                }
            }
            ActionState.JUMP_UP, ActionState.JUMP_FORWARD, ActionState.JUMP_BACKWARD -> {
                // Horizontal momentum in forward / backward jumps scaled by u
                if (fighter.actionState == ActionState.JUMP_FORWARD) {
                    val dir = if (fighter.facingRight) 1f else -1f
                    fighter.x = (fighter.x + dir * 180f * u * dt).coerceIn(minBoundary, maxBoundary)
                } else if (fighter.actionState == ActionState.JUMP_BACKWARD) {
                    val dir = if (fighter.facingRight) -1f else 1f
                    fighter.x = (fighter.x + dir * 150f * u * dt).coerceIn(minBoundary, maxBoundary)
                }
            }
            ActionState.ROLL_FORWARD -> {
                val dir = if (fighter.facingRight) 1f else -1f
                fighter.x = (fighter.x + dir * 260f * u * dt).coerceIn(minBoundary, maxBoundary)
                if (fighter.actionFrame >= 1f) {
                    fighter.isInvulnerable = false
                    setAction(fighter, ActionState.IDLE, 0.4f)
                }
            }
            ActionState.ROLL_BACKWARD -> {
                val dir = if (fighter.facingRight) -1f else 1f
                fighter.x = (fighter.x + dir * 220f * u * dt).coerceIn(minBoundary, maxBoundary)
                if (fighter.actionFrame >= 1f) {
                    fighter.isInvulnerable = false
                    setAction(fighter, ActionState.IDLE, 0.4f)
                }
            }
            ActionState.PUNCH_JAB, ActionState.PUNCH_HOOK, ActionState.PUNCH_UPPERCUT,
            ActionState.KICK_LOW, ActionState.KICK_ROUNDHOUSE, ActionState.KICK_FLYING,
            ActionState.WEAPON_SLASH, ActionState.WEAPON_SPIN, ActionState.WEAPON_PIERCE,
            ActionState.RANGED_THROW, ActionState.SHADOW_SURGE -> {
                // Slight forward momentum during lunging attacks scaled by u
                if (fighter.actionFrame in 0.2f..0.5f) {
                    val dir = if (fighter.facingRight) 1f else -1f
                    val lungeSpeed = when (fighter.actionState) {
                        ActionState.WEAPON_SLASH -> 90f * u
                        ActionState.KICK_FLYING -> 240f * u
                        ActionState.PUNCH_UPPERCUT -> 60f * u
                        else -> 30f * u
                    }
                    fighter.x = (fighter.x + dir * lungeSpeed * dt).coerceIn(minBoundary, maxBoundary)
                }
                if (fighter.actionFrame >= 1f) {
                    // If grounded, return to IDLE; if still in air during KICK_FLYING, global vertical physics handles landing
                    if (fighter.y <= 0f) {
                        setAction(fighter, ActionState.IDLE, 0.4f)
                    }
                }
            }
            ActionState.HIT_LIGHT, ActionState.HIT_HEAVY -> {
                if (fighter.actionFrame >= 1f) {
                    setAction(fighter, ActionState.IDLE, 0.4f)
                }
            }
            ActionState.KNOCKDOWN -> {
                if (fighter.health <= 0f) {
                    setAction(fighter, ActionState.DEFEAT, 1f)
                } else if (fighter.actionFrame >= 1f) {
                    setAction(fighter, ActionState.IDLE, 0.4f)
                }
            }
            ActionState.BLOCKING -> {
                if (fighter.actionFrame >= 1f) {
                    setAction(fighter, ActionState.IDLE, 0.4f)
                }
            }
            else -> {}
        }
    }

    private fun isAcrobaticAction(state: ActionState): Boolean {
        return state == ActionState.ROLL_FORWARD || state == ActionState.ROLL_BACKWARD ||
                state == ActionState.JUMP_FORWARD || state == ActionState.JUMP_BACKWARD
    }

    // Fix 1 & 4: Hit Detection with scaled ranges, armor defense, and boss multipliers
    private fun checkAttackHit(
        attacker: Fighter,
        defender: Fighter,
        currentBoss: BossProfile?,
        playerArmor: ArmorItem?,
        u: Float
    ) {
        if (defender.health <= 0f || defender.isInvulnerable) return
        if (attacker.hitCooldown > 0f) return

        val state = attacker.actionState
        val isAttacking = isAttackState(state)
        if (!isAttacking) return

        // Active hit frames window
        if (attacker.actionFrame !in 0.35f..0.75f) return

        val dist = abs(attacker.x - defender.x)
        val inFront = if (attacker.facingRight) defender.x >= attacker.x else defender.x <= attacker.x
        if (!inFront) return

        // Range calculation based on attack type and weapon, scaled by world scale u
        val baseRange = when (state) {
            ActionState.PUNCH_JAB -> 75f * u
            ActionState.PUNCH_HOOK -> 85f * u
            ActionState.PUNCH_UPPERCUT -> 90f * u
            ActionState.KICK_LOW -> 95f * u
            ActionState.KICK_ROUNDHOUSE -> 110f * u
            ActionState.KICK_FLYING -> 135f * u
            ActionState.WEAPON_SLASH -> 125f * attacker.weapon.range * u
            ActionState.WEAPON_SPIN -> 115f * attacker.weapon.range * u
            ActionState.WEAPON_PIERCE -> 140f * attacker.weapon.range * u
            ActionState.SHADOW_SURGE -> 180f * u
            else -> 80f * u
        }

        if (dist <= baseRange) {
            // Check if defender is blocking
            val isDefending = defender.actionState == ActionState.BLOCKING ||
                    (defender.actionState == ActionState.WALK_BACKWARD && defender.facingRight != attacker.facingRight)

            attacker.hitCooldown = 0.38f // prevent multi-hits in same strike

            if (isDefending && state != ActionState.SHADOW_SURGE && state != ActionState.KICK_LOW) {
                // Blocked!
                var blockedDamage = 4f
                if (defender.isPlayer) {
                    val def = (playerArmor?.defenseBonus ?: 0f).coerceIn(0f, 0.85f)
                    blockedDamage *= (1f - def) * (currentBoss?.attackMult ?: 1.0f)
                } else {
                    val bossDef = (currentBoss?.defenseMult ?: 1.0f).coerceAtLeast(0.5f)
                    blockedDamage /= bossDef
                }
                blockedDamage = blockedDamage.coerceAtLeast(1f)

                defender.health = (defender.health - blockedDamage).coerceAtLeast(0f)
                spawnBlockSparks(defender.x, defender.y, u)
                soundManager.playClang()
                screenShake = 3f
                addFloatingText("BLOCKED -${blockedDamage.toInt()}", defender.x, defender.y + 70f * u, Color(0xFFB0BEC5))
            } else {
                // Clean Hit!
                val isCrit = Random.nextFloat() < attacker.weapon.critChance
                var damage = attacker.weapon.baseDamage * when (state) {
                    ActionState.PUNCH_JAB -> 0.7f
                    ActionState.PUNCH_HOOK -> 0.9f
                    ActionState.PUNCH_UPPERCUT -> 1.3f
                    ActionState.KICK_LOW -> 0.85f
                    ActionState.KICK_ROUNDHOUSE -> 1.2f
                    ActionState.KICK_FLYING -> 1.4f
                    ActionState.WEAPON_SLASH -> 1.35f
                    ActionState.WEAPON_SPIN -> 1.5f
                    ActionState.WEAPON_PIERCE -> 1.6f
                    ActionState.SHADOW_SURGE -> 2.4f
                    else -> 1.0f
                }

                if (attacker.isShadowMode) damage *= 1.4f
                if (isCrit) damage *= 1.75f

                // If attacker is enemy, apply boss attackMult
                if (!attacker.isPlayer) {
                    damage *= (currentBoss?.attackMult ?: 1.0f)
                }

                // If defender is player, reduce damage by equipped armor defenseBonus
                if (defender.isPlayer) {
                    val armorDef = (playerArmor?.defenseBonus ?: 0f).coerceIn(0f, 0.85f)
                    damage *= (1f - armorDef)
                } else {
                    val bossDef = (currentBoss?.defenseMult ?: 1.0f).coerceAtLeast(0.5f)
                    damage /= bossDef
                }
                damage = damage.coerceAtLeast(1f)

                defender.health = (defender.health - damage).coerceAtLeast(0f)

                // Fill shadow energy on hits
                attacker.shadowEnergy = (attacker.shadowEnergy + (if (attacker.isShadowMode) 0f else 18f)).coerceAtMost(100f)
                defender.shadowEnergy = (defender.shadowEnergy + 8f).coerceAtMost(100f)

                // Hit reaction & Knockback scaled by u
                val knockDir = if (attacker.facingRight) 1f else -1f
                val pushDist = (if (state == ActionState.SHADOW_SURGE || state == ActionState.PUNCH_UPPERCUT || isCrit) 65f else 30f) * u
                defender.x += knockDir * pushDist

                if (state == ActionState.PUNCH_UPPERCUT || state == ActionState.SHADOW_SURGE || isCrit || defender.health <= 0f) {
                    setAction(defender, ActionState.KNOCKDOWN, 0.7f)
                    screenShake = 12f
                    soundManager.playHit()
                } else if (state == ActionState.KICK_LOW) {
                    setAction(defender, ActionState.HIT_HEAVY, 0.45f)
                    screenShake = 6f
                    soundManager.playKick()
                } else if (state == ActionState.WEAPON_SLASH || state == ActionState.WEAPON_SPIN) {
                    setAction(defender, ActionState.HIT_HEAVY, 0.45f)
                    screenShake = 8f
                    soundManager.playSlash()
                } else {
                    setAction(defender, ActionState.HIT_LIGHT, 0.35f)
                    screenShake = 5f
                    soundManager.playHit()
                }

                // Combo Tracking
                attacker.comboCount++
                attacker.comboTimer = 1.8f

                // Sparks & Floating Numbers
                spawnHitSparks(defender.x, defender.y, isCrit, attacker.weapon.bladeColor, u)
                val label = when {
                    isCrit -> "CRITICAL! -${damage.toInt()}"
                    state == ActionState.PUNCH_UPPERCUT -> "HEAD HIT! -${damage.toInt()}"
                    state == ActionState.KICK_LOW -> "SWEEP! -${damage.toInt()}"
                    state == ActionState.SHADOW_SURGE -> "SHADOW BURST! -${damage.toInt()}"
                    attacker.comboCount > 1 -> "COMBO x${attacker.comboCount}! -${damage.toInt()}"
                    else -> "-${damage.toInt()}"
                }
                val textColor = if (isCrit) Color(0xFFFF5252) else if (state == ActionState.SHADOW_SURGE) Color(0xFFE040FB) else Color(0xFFFFD54F)
                addFloatingText(label, defender.x, defender.y + 80f * u, textColor)
            }
        }
    }

    private fun isAttackState(state: ActionState): Boolean {
        return state == ActionState.PUNCH_JAB || state == ActionState.PUNCH_HOOK ||
                state == ActionState.PUNCH_UPPERCUT || state == ActionState.KICK_LOW ||
                state == ActionState.KICK_ROUNDHOUSE || state == ActionState.KICK_FLYING ||
                state == ActionState.WEAPON_SLASH || state == ActionState.WEAPON_SPIN ||
                state == ActionState.WEAPON_PIERCE || state == ActionState.SHADOW_SURGE
    }

    private fun updateEnemyAI(enemy: Fighter, player: Fighter, boss: BossProfile?, dt: Float, u: Float) {
        if (enemy.actionState != ActionState.IDLE) return

        val dist = abs(enemy.x - player.x)
        val aggro = boss?.aiAggressiveness ?: 0.6f

        // If player is attacking and close, chance to block or roll back
        if (isAttackState(player.actionState) && dist < 120f * u) {
            val blockRoll = Random.nextFloat()
            if (blockRoll < 0.45f * aggro) {
                setAction(enemy, ActionState.BLOCKING, 0.5f)
                return
            } else if (blockRoll < 0.65f * aggro) {
                setAction(enemy, ActionState.ROLL_BACKWARD, 0.45f)
                return
            }
        }

        // Unleash Shadow Surge if charged
        if (enemy.shadowEnergy >= 100f && dist < 170f * u) {
            enemy.shadowEnergy = 0f
            enemy.isShadowMode = true
            enemy.shadowModeTimer = 7f
            setAction(enemy, ActionState.SHADOW_SURGE, 0.75f)
            soundManager.playShadow()
            return
        }

        // Ranged Shuriken Throw if at long distance
        if (dist > 220f * u && Random.nextFloat() < 0.03f * aggro) {
            setAction(enemy, ActionState.RANGED_THROW, 0.5f)
            spawnShuriken(enemy, u)
            return
        }

        // Distance & Movement Choices
        if (dist > 130f * u) {
            // Approach player
            val roll = Random.nextFloat()
            if (roll < 0.6f) {
                // Fix 2: Give walk a duration of 0.5s after which enemy returns to IDLE to attack
                setAction(enemy, ActionState.WALK_FORWARD, 0.5f)
            } else if (roll < 0.85f) {
                setAction(enemy, ActionState.ROLL_FORWARD, 0.4f)
            } else {
                jumpFighter(enemy, ActionState.JUMP_FORWARD, u)
            }
        } else if (dist < 60f * u) {
            // Too close: backstep or quick jab
            if (Random.nextFloat() < 0.4f) {
                setAction(enemy, ActionState.WALK_BACKWARD, 0.5f)
            } else {
                setAction(enemy, ActionState.PUNCH_JAB, 0.35f)
            }
        } else {
            // Optimal strike range: select combat combo
            val attackChoice = Random.nextInt(6)
            when (attackChoice) {
                0 -> setAction(enemy, ActionState.PUNCH_JAB, 0.35f)
                1 -> setAction(enemy, ActionState.PUNCH_UPPERCUT, 0.48f)
                2 -> setAction(enemy, ActionState.KICK_LOW, 0.42f)
                3 -> setAction(enemy, ActionState.KICK_ROUNDHOUSE, 0.5f)
                4 -> setAction(enemy, ActionState.WEAPON_SLASH, 0.55f)
                5 -> setAction(enemy, ActionState.WEAPON_SPIN, 0.65f)
            }
        }
    }

    fun spawnShuriken(fighter: Fighter, u: Float = 1.0f) {
        val dir = if (fighter.facingRight) 1f else -1f
        val proj = Projectile(
            id = nextProjectileId++,
            x = fighter.x + dir * 30f * u,
            y = fighter.y + 60f * u,
            vx = dir * 650f * u,
            fromPlayer = fighter.isPlayer,
            damage = 18f
        )
        projectiles.add(proj)
        soundManager.playShuriken()
    }

    // Fix 6: Projectiles accept dt, check roll invulnerability, and respect BLOCKING
    private fun updateProjectiles(
        player: Fighter,
        enemy: Fighter,
        currentBoss: BossProfile?,
        playerArmor: ArmorItem?,
        arenaWidth: Float,
        dt: Float,
        u: Float
    ) {
        val iterator = projectiles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * dt
            p.rotation += 35f

            // Target check scaled by u
            val target = if (p.fromPlayer) enemy else player
            if (abs(p.x - target.x) < 30f * u && abs(p.y - (target.y + 50f * u)) < 40f * u) {
                if (target.isInvulnerable) {
                    continue
                }

                val isDefending = target.actionState == ActionState.BLOCKING ||
                        (target.actionState == ActionState.WALK_BACKWARD && target.facingRight != p.fromPlayer)

                if (isDefending) {
                    var blockedDmg = p.damage * 0.2f
                    if (target.isPlayer) {
                        val def = (playerArmor?.defenseBonus ?: 0f).coerceIn(0f, 0.85f)
                        blockedDmg *= (1f - def)
                    } else {
                        val bossDef = (currentBoss?.defenseMult ?: 1.0f).coerceAtLeast(0.5f)
                        blockedDmg /= bossDef
                    }
                    blockedDmg = blockedDmg.coerceAtLeast(1f)

                    target.health = (target.health - blockedDmg).coerceAtLeast(0f)
                    spawnBlockSparks(target.x, target.y + 50f * u, u)
                    soundManager.playClang()
                    addFloatingText("BLOCKED -${blockedDmg.toInt()}", target.x, target.y + 70f * u, Color(0xFFB0BEC5))
                } else {
                    var finalDmg = p.damage
                    if (target.isPlayer) {
                        val def = (playerArmor?.defenseBonus ?: 0f).coerceIn(0f, 0.85f)
                        finalDmg *= (1f - def) * (currentBoss?.attackMult ?: 1.0f)
                    } else {
                        val bossDef = (currentBoss?.defenseMult ?: 1.0f).coerceAtLeast(0.5f)
                        finalDmg /= bossDef
                    }
                    finalDmg = finalDmg.coerceAtLeast(1f)

                    target.health = (target.health - finalDmg).coerceAtLeast(0f)
                    setAction(target, ActionState.HIT_LIGHT, 0.3f)
                    spawnHitSparks(target.x, target.y + 50f * u, false, Color(0xFF00E5FF), u)
                    soundManager.playHit()
                    addFloatingText("-${finalDmg.toInt()}", target.x, target.y + 70f * u, Color(0xFF00E5FF))
                }

                iterator.remove()
                continue
            }

            // Screen boundary check
            if (p.x < 0f || p.x > arenaWidth) {
                iterator.remove()
            }
        }
    }

    private fun spawnHitSparks(x: Float, y: Float, isCrit: Boolean, color: Color, u: Float) {
        val count = if (isCrit) 18 else 10
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = (Random.nextFloat() * (if (isCrit) 320f else 180f) + 60f) * u
            particles.add(
                Particle(
                    x = x,
                    y = y + 50f * u,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    life = 0.35f,
                    maxLife = 0.35f,
                    color = if (i % 2 == 0) color else Color.White,
                    size = (if (isCrit) 5f else 3.5f),
                    type = ParticleType.SPARK
                )
            )
        }
        // Shadow mist puff
        for (i in 0 until 4) {
            particles.add(
                Particle(
                    x = x + (Random.nextFloat() - 0.5f) * 20f * u,
                    y = y + 50f * u + (Random.nextFloat() - 0.5f) * 20f * u,
                    vx = (Random.nextFloat() - 0.5f) * 40f * u,
                    vy = -Random.nextFloat() * 50f * u,
                    life = 0.5f,
                    maxLife = 0.5f,
                    color = Color(0xAA1A0D28),
                    size = 12f,
                    type = ParticleType.SHADOW_SMOKE
                )
            )
        }
    }

    private fun spawnBlockSparks(x: Float, y: Float, u: Float) {
        for (i in 0 until 6) {
            val angle = Random.nextFloat() * Math.PI.toFloat() - Math.PI.toFloat() / 2f
            val speed = (Random.nextFloat() * 140f + 50f) * u
            particles.add(
                Particle(
                    x = x,
                    y = y + 60f * u,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    life = 0.25f,
                    maxLife = 0.25f,
                    color = Color(0xFFCFD8DC),
                    size = 3f,
                    type = ParticleType.SPARK
                )
            )
        }
    }

    private fun updateParticles(dt: Float, u: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            if (p.type == ParticleType.SPARK) {
                p.vy += 300f * u * dt // Gravity on sparks
            }
            if (p.life <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun updateFloatingTexts(dt: Float, u: Float) {
        val iterator = floatingTexts.iterator()
        while (iterator.hasNext()) {
            val ft = iterator.next()
            ft.life -= dt
            ft.y += 45f * u * dt // Float upward
            ft.alpha = (ft.life / 0.8f).coerceIn(0f, 1f)
            if (ft.life <= 0f) {
                iterator.remove()
            }
        }
    }

    fun addFloatingText(text: String, x: Float, y: Float, color: Color) {
        floatingTexts.add(FloatingText(text, x, y, color, life = 0.85f))
    }

    fun setAction(fighter: Fighter, newState: ActionState, duration: Float) {
        fighter.actionState = newState
        fighter.actionFrame = 0f
        fighter.actionDuration = duration
    }

    // Fix 1 & 5: Accepts explicit ActionState and scales jump impulse by world scale u
    fun jumpFighter(fighter: Fighter, state: ActionState = ActionState.JUMP_UP, u: Float = 1.0f) {
        if (fighter.y == 0f) {
            fighter.vy = 480f * u
            setAction(fighter, state, 0.7f)
        }
    }
}
