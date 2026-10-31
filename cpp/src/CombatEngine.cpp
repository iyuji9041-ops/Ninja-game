#include "CombatEngine.hpp"
#include <cmath>
#include <cstdlib>
#include <algorithm>

namespace ShadowCombat {

CombatEngine::CombatEngine()
    : player("Shinobi", true, WeaponType::KATANA),
      enemy("Lynx", false, WeaponType::DEMON_CLAWS) {
    currentBoss = BossDatabase::get("lynx");
    startMatch("lynx", WeaponType::KATANA, ArenaType::CRIMSON_PAGODA);
}

void CombatEngine::startMatch(const std::string& bossId, WeaponType playerWeapon, ArenaType arenaType) {
    currentBoss = BossDatabase::get(bossId);
    arena = arenaType;

    player.weapon = WeaponDatabase::get(playerWeapon);
    enemy.weapon = WeaponDatabase::get(currentBoss.weaponType);
    enemy.name = currentBoss.name;
    enemy.maxHealth = currentBoss.health;
    enemy.health = currentBoss.health;

    playerRoundWins = 0;
    enemyRoundWins = 0;
    currentRound = 1;

    resetRound();
}

void CombatEngine::resetRound() {
    float startP1 = 220.0f;
    float startP2 = arenaWidth - 220.0f;

    player.reset(startP1, true);
    enemy.reset(startP2, false);

    particles.clear();
    projectiles.clear();
    aiController.reset();

    matchPhase = MatchPhase::FIGHTING;
    phaseTimer = 0.0f;
}

void CombatEngine::update(float dt) {
    dt = clamp(dt, 0.001f, 0.05f);

    // Screen Shake decay
    if (screenShake > 0.0f) {
        screenShake = std::max(0.0f, screenShake - dt * 25.0f);
    }

    // Update Particles
    particles.update(dt);

    if (matchPhase == MatchPhase::FIGHTING) {
        // AI Update
        if (enemy.health > 0.0f && player.health > 0.0f) {
            aiController.update(enemy, player, &currentBoss, dt, arenaWidth);
        }

        // Update Physics
        updateFighterPhysics(player, enemy, dt);
        updateFighterPhysics(enemy, player, dt);

        // Fighter Spacing Pushback (fighters cannot walk through each other unless rolling)
        bool playerInvuln = (player.actionState == ActionState::ROLL_FORWARD || player.actionState == ActionState::ROLL_BACKWARD);
        bool enemyInvuln = (enemy.actionState == ActionState::ROLL_FORWARD || enemy.actionState == ActionState::ROLL_BACKWARD);

        if (!playerInvuln && !enemyInvuln) {
            float dist = enemy.x - player.x;
            float minDist = 38.0f;
            if (std::abs(dist) < minDist) {
                float push = (minDist - std::abs(dist)) * 0.5f;
                if (player.x < enemy.x) {
                    player.x = std::max(60.0f, player.x - push);
                    enemy.x = std::min(arenaWidth - 60.0f, enemy.x + push);
                } else {
                    player.x = std::min(arenaWidth - 60.0f, player.x + push);
                    enemy.x = std::max(60.0f, enemy.x - push);
                }
            }
        }

        // Auto facing
        if (player.canPerformAction() && !playerInvuln) {
            player.facingRight = player.x <= enemy.x;
        }
        if (enemy.canPerformAction() && !enemyInvuln) {
            enemy.facingRight = enemy.x <= player.x;
        }

        // Hit Detection
        checkAttackHit(player, enemy);
        checkAttackHit(enemy, player);

        // Projectiles Update
        updateProjectiles(dt);

        // Check Round Over
        if (player.health <= 0.0f || enemy.health <= 0.0f) {
            matchPhase = MatchPhase::ROUND_OVER;
            phaseTimer = 2.0f;

            if (player.health > 0.0f) {
                playerRoundWins++;
                player.startAction(ActionState::VICTORY, 2.5f);
            } else {
                enemyRoundWins++;
                enemy.startAction(ActionState::VICTORY, 2.5f);
            }
        }
    } else {
        handleRoundTransitions(dt);
    }
}

void CombatEngine::updateFighterPhysics(Fighter& f, const Fighter& opponent, float dt) {
    // Action Animation Frame Progression
    if (f.actionState != ActionState::IDLE) {
        f.actionTime += dt;
        f.actionFrame = clamp(f.actionTime / f.actionDuration, 0.0f, 1.0f);

        if (f.actionTime >= f.actionDuration) {
            if (f.actionState != ActionState::DEFEAT && f.actionState != ActionState::VICTORY) {
                f.startAction(ActionState::IDLE, 0.1f);
            }
        }
    }

    // Shadow Mode Timer
    if (f.isShadowMode) {
        f.shadowModeTimer -= dt;
        if (rand() % 4 == 0) {
            particles.spawnShadowFlame(Vec2(f.x, f.y + 40.0f), 1);
        }
        if (f.shadowModeTimer <= 0.0f) {
            f.isShadowMode = false;
            f.defenseMultiplier = 1.0f;
        }
    }

    // Cooldowns
    if (f.rangedCooldown > 0.0f) f.rangedCooldown -= dt;
    if (f.dodgeCooldown > 0.0f) f.dodgeCooldown -= dt;
    if (f.comboResetTimer > 0.0f) {
        f.comboResetTimer -= dt;
        if (f.comboResetTimer <= 0.0f) f.comboCount = 0;
    }

    // Gravity & Ground Physics
    if (!f.isGrounded()) {
        f.vy -= 9.8f * 45.0f * dt; // Gravity
        f.y += f.vy * dt * 30.0f;
        if (f.y <= 0.0f) {
            f.y = 0.0f;
            f.vy = 0.0f;
            particles.spawnDust(Vec2(f.x, 0.0f), 5);
        }
    }

    // Velocity X & Friction
    f.x += f.vx * dt;
    float friction = f.isGrounded() ? 12.0f : 4.0f;
    if (std::abs(f.vx) > 0.1f) {
        f.vx -= f.vx * friction * dt;
    } else {
        f.vx = 0.0f;
    }

    // Arena Boundaries
    f.x = clamp(f.x, 50.0f, arenaWidth - 50.0f);
}

void CombatEngine::checkAttackHit(Fighter& attacker, Fighter& defender) {
    if (!attacker.isAttacking() || attacker.attackHasHit) return;

    // Check invulnerability during dodge rolls
    if (defender.actionState == ActionState::ROLL_FORWARD || defender.actionState == ActionState::ROLL_BACKWARD) {
        return;
    }

    Rect hit = attacker.getHitbox();
    Rect hurt = defender.getHurtbox();

    if (hit.width() > 0 && hurt.width() > 0 && hit.intersects(hurt)) {
        attacker.attackHasHit = true;

        // Base damage calculation
        float baseDmg = attacker.weapon.baseDamage;
        float speedMod = attacker.weapon.attackSpeedMultiplier;
        float knockback = 14.0f;
        HitType hitType = HitType::NORMAL;

        switch (attacker.actionState) {
            case ActionState::PUNCH_JAB:
                baseDmg = 12.0f; knockback = 10.0f; break;
            case ActionState::PUNCH_HOOK:
                baseDmg = 16.0f; knockback = 16.0f; break;
            case ActionState::PUNCH_UPPERCUT:
                baseDmg = 22.0f; knockback = 28.0f; hitType = HitType::HEAD_HIT; break;
            case ActionState::KICK_LOW:
                baseDmg = 14.0f; knockback = 12.0f; hitType = HitType::LOW_SWEEP; break;
            case ActionState::KICK_ROUNDHOUSE:
                baseDmg = 20.0f; knockback = 22.0f; break;
            case ActionState::KICK_FLYING:
                baseDmg = 26.0f; knockback = 32.0f; break;
            case ActionState::WEAPON_SLASH:
                baseDmg = attacker.weapon.baseDamage * 1.15f; knockback = 18.0f; break;
            case ActionState::WEAPON_SPIN:
                baseDmg = attacker.weapon.baseDamage * 1.35f; knockback = 24.0f; break;
            case ActionState::WEAPON_PIERCE:
                baseDmg = attacker.weapon.baseDamage * 1.5f; knockback = 26.0f; break;
            case ActionState::SHADOW_SURGE:
                baseDmg = 45.0f; knockback = 45.0f; hitType = HitType::SHADOW_BLAST; break;
            default:
                break;
        }

        // Shadow Mode damage bonus (40% increased damage)
        if (attacker.isShadowMode) {
            baseDmg *= 1.4f;
        }

        // Critical Hit Check
        float critRoll = static_cast<float>(rand()) / RAND_MAX;
        if (critRoll < attacker.weapon.critChance && hitType != HitType::SHADOW_BLAST) {
            baseDmg *= 1.75f;
            hitType = HitType::CRITICAL;
        }

        // Blocking Check
        bool isBlocked = (defender.actionState == ActionState::BLOCKING);
        if (isBlocked && hitType != HitType::SHADOW_BLAST) {
            hitType = HitType::BLOCKED;
            knockback *= 0.35f;
        }

        // Apply Damage
        defender.takeDamage(baseDmg, hitType, knockback);

        // Screen Shake
        screenShake = (hitType == HitType::CRITICAL || hitType == HitType::SHADOW_BLAST) ? 14.0f : 6.0f;

        // Visual Particles & Floating Text
        Vec2 contactPoint((hit.left + hit.right) * 0.5f, (hit.bottom + hit.top) * 0.5f);
        if (hitType == HitType::BLOCKED) {
            particles.spawnSparks(contactPoint, 15, 0xFFFFD54F);
            particles.addFloatingText("BLOCKED", contactPoint + Vec2(0, 30), 0xFFB0BEC5, 0.9f);
        } else {
            particles.spawnSparks(contactPoint, 20, attacker.weapon.bladeColor);
            particles.spawnBloodSplatter(contactPoint, 12, attacker.facingRight);

            // Combo counter
            attacker.comboCount++;
            attacker.comboResetTimer = 2.0f;

            if (hitType == HitType::CRITICAL) {
                particles.addFloatingText("CRITICAL! " + std::to_string(static_cast<int>(baseDmg)), contactPoint + Vec2(0, 40), 0xFFFF1744, 1.4f);
            } else if (hitType == HitType::SHADOW_BLAST) {
                particles.addFloatingText("SHADOW BLAST!", contactPoint + Vec2(0, 45), 0xFFD500F9, 1.5f);
                particles.spawnShockwave(contactPoint, 0xEE7B1FA2);
            } else {
                particles.addFloatingText(std::to_string(static_cast<int>(baseDmg)), contactPoint + Vec2(0, 30), 0xFFFFFFFF, 1.0f);
            }

            if (attacker.comboCount >= 3) {
                std::string comboMsg = "COMBO x" + std::to_string(attacker.comboCount);
                particles.addFloatingText(comboMsg, Vec2(attacker.x, attacker.y + 110.0f), 0xFFFFAB00, 1.2f);
            }
        }
    }
}

void CombatEngine::updateProjectiles(float dt) {
    for (size_t i = 0; i < projectiles.size();) {
        auto& p = projectiles[i];
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.rotation += 720.0f * dt;

        Fighter& target = p.isPlayer ? enemy : player;
        Rect hurt = target.getHurtbox();

        // Check Hit
        if (p.x >= hurt.left && p.x <= hurt.right && p.y >= hurt.bottom && p.y <= hurt.top) {
            target.takeDamage(p.damage, HitType::NORMAL, 14.0f);
            particles.spawnSparks(Vec2(p.x, p.y), 15, 0xFFFFD54F);
            particles.spawnBloodSplatter(Vec2(p.x, p.y), 8, p.vx > 0);
            particles.addFloatingText(std::to_string(static_cast<int>(p.damage)), Vec2(p.x, p.y + 20), 0xFFFF9800);
            p.isAlive = false;
        }

        // Out of screen
        if (p.x < 0 || p.x > arenaWidth) {
            p.isAlive = false;
        }

        if (!p.isAlive) {
            projectiles[i] = projectiles.back();
            projectiles.pop_back();
        } else {
            ++i;
        }
    }
}

void CombatEngine::handleRoundTransitions(float dt) {
    phaseTimer -= dt;
    if (phaseTimer <= 0.0f) {
        if (playerRoundWins >= 2 || enemyRoundWins >= 2) {
            matchPhase = MatchPhase::MATCH_OVER;
        } else {
            currentRound++;
            resetRound();
        }
    }
}

// Player Commands
void CombatEngine::playerWalk(float direction) {
    if (!player.canPerformAction()) return;
    if (std::abs(direction) < 0.15f) return;

    bool forward = (player.facingRight && direction > 0.0f) || (!player.facingRight && direction < 0.0f);
    player.startAction(forward ? ActionState::WALK_FORWARD : ActionState::WALK_BACKWARD, 0.3f);
    player.vx = direction * 150.0f;
}

void CombatEngine::playerJump(float forwardDir) {
    if (!player.canPerformAction() || !player.isGrounded()) return;

    ActionState jumpState = ActionState::JUMP_UP;
    if (forwardDir > 0.2f) jumpState = ActionState::JUMP_FORWARD;
    else if (forwardDir < -0.2f) jumpState = ActionState::JUMP_BACKWARD;

    player.startAction(jumpState, 0.65f);
    player.vy = 8.5f;
    player.vx = forwardDir * 180.0f;
}

void CombatEngine::playerCrouch() {
    if (!player.canPerformAction() || !player.isGrounded()) return;
    player.startAction(ActionState::CROUCH, 0.4f);
}

void CombatEngine::playerBlock(bool blocking) {
    if (blocking && player.canPerformAction()) {
        player.startAction(ActionState::BLOCKING, 0.5f);
    } else if (!blocking && player.actionState == ActionState::BLOCKING) {
        player.startAction(ActionState::IDLE, 0.1f);
    }
}

void CombatEngine::playerRoll(bool forward) {
    if (!player.canPerformAction() || !player.isGrounded()) return;
    float dir = player.facingRight ? (forward ? 1.0f : -1.0f) : (forward ? -1.0f : 1.0f);
    player.startAction(forward ? ActionState::ROLL_FORWARD : ActionState::ROLL_BACKWARD, 0.38f);
    player.vx = dir * 260.0f;
}

void CombatEngine::playerAttack(ActionState attackType) {
    if (!player.canPerformAction()) return;
    float duration = 0.35f;
    switch (attackType) {
        case ActionState::PUNCH_JAB: duration = 0.25f; break;
        case ActionState::PUNCH_HOOK: duration = 0.32f; break;
        case ActionState::PUNCH_UPPERCUT: duration = 0.40f; break;
        case ActionState::KICK_LOW: duration = 0.28f; break;
        case ActionState::KICK_ROUNDHOUSE: duration = 0.36f; break;
        case ActionState::KICK_FLYING: duration = 0.45f; break;
        case ActionState::WEAPON_SLASH: duration = 0.35f / player.weapon.attackSpeedMultiplier; break;
        case ActionState::WEAPON_SPIN: duration = 0.48f / player.weapon.attackSpeedMultiplier; break;
        case ActionState::WEAPON_PIERCE: duration = 0.38f / player.weapon.attackSpeedMultiplier; break;
        default: break;
    }
    player.startAction(attackType, duration);
}

void CombatEngine::playerThrowRanged() {
    if (!player.canPerformAction() || player.rangedCooldown > 0.0f) return;

    player.startAction(ActionState::RANGED_THROW, 0.4f);
    player.rangedCooldown = 3.5f;

    Projectile p;
    p.id = nextProjectileId++;
    p.x = player.facingRight ? player.x + 30.0f : player.x - 30.0f;
    p.y = player.y + 60.0f;
    p.vx = player.facingRight ? 450.0f : -450.0f;
    p.vy = 0.0f;
    p.isPlayer = true;
    p.damage = 18.0f;
    projectiles.push_back(p);
}

void CombatEngine::playerShadowSurge() {
    if (!player.canPerformAction() || !player.isShadowMode) {
        // Can also activate shadow mode if energy is full
        if (player.shadowEnergy >= 50.0f) {
            player.activateShadowMode();
            particles.spawnShockwave(Vec2(player.x, player.y + 50.0f), 0xFF00E5FF);
        }
        return;
    }

    player.startAction(ActionState::SHADOW_SURGE, 0.65f);
}

} // namespace ShadowCombat
