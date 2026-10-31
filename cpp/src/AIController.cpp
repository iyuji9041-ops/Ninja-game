#include "AIController.hpp"
#include <cstdlib>
#include <cmath>
#include <algorithm>

namespace ShadowCombat {

void AIController::reset() {
    decisionTimer = 0.0f;
    aggressionCooldown = 0.0f;
    blockTimer = 0.0f;
}

void AIController::update(Fighter& ai, const Fighter& player, const BossProfile* boss, float dt, float arenaWidth) {
    (void)arenaWidth;
    if (ai.health <= 0.0f || !ai.canPerformAction()) return;

    decisionTimer -= dt;
    aggressionCooldown -= dt;
    if (blockTimer > 0.0f) {
        blockTimer -= dt;
        if (blockTimer <= 0.0f && ai.actionState == ActionState::BLOCKING) {
            ai.startAction(ActionState::IDLE, 0.1f);
        }
    }

    float dist = std::abs(player.x - ai.x);

    // Reactive defense: If player is actively attacking and in range, chance to block
    float blockChance = boss ? boss->blockChance : 0.45f;
    if (player.isAttacking() && dist < 120.0f && (static_cast<float>(rand()) / RAND_MAX) < blockChance) {
        if (ai.actionState != ActionState::BLOCKING) {
            ai.startAction(ActionState::BLOCKING, 0.45f);
            blockTimer = 0.45f;
            return;
        }
    }

    // Shadow Mode activation for Boss
    if (ai.shadowEnergy >= 50.0f && !ai.isShadowMode) {
        ai.activateShadowMode();
        return;
    }

    if (decisionTimer <= 0.0f) {
        decideNextMove(ai, player, boss, dist);
        // Faster decisions for high-aggression bosses (0.15s - 0.4s)
        float baseDecision = boss ? (0.45f - boss->aggression * 0.25f) : 0.28f;
        decisionTimer = baseDecision + (static_cast<float>(rand()) / RAND_MAX) * 0.15f;
    }
}

void AIController::decideNextMove(Fighter& ai, const Fighter& player, const BossProfile* boss, float dist) {
    (void)boss;
    float optimalRange = 75.0f * ai.weapon.rangeMultiplier;
    float r = static_cast<float>(rand()) / RAND_MAX;

    // Boss Shadow Blast / Special
    if (ai.isShadowMode && dist < 140.0f && r < 0.35f) {
        ai.startAction(ActionState::SHADOW_SURGE, 0.65f);
        return;
    }

    // Far distance: Close the gap or throw ranged shuriken
    if (dist > 180.0f) {
        if (r < 0.25f && ai.rangedCooldown <= 0.0f) {
            ai.startAction(ActionState::RANGED_THROW, 0.4f);
            ai.rangedCooldown = 4.0f;
        } else if (r < 0.65f) {
            ai.startAction(ActionState::WALK_FORWARD, 0.35f);
            float dir = (player.x > ai.x) ? 1.0f : -1.0f;
            ai.vx = dir * 140.0f;
        } else {
            ai.startAction(ActionState::ROLL_FORWARD, 0.4f);
            float dir = (player.x > ai.x) ? 1.0f : -1.0f;
            ai.vx = dir * 210.0f;
        }
        return;
    }

    // Medium distance: Space carefully or leap strike
    if (dist > optimalRange + 30.0f) {
        if (r < 0.45f) {
            ai.startAction(ActionState::WALK_FORWARD, 0.25f);
            float dir = (player.x > ai.x) ? 1.0f : -1.0f;
            ai.vx = dir * 130.0f;
        } else if (r < 0.70f) {
            ai.startAction(ActionState::KICK_FLYING, 0.45f);
            float dir = (player.x > ai.x) ? 1.0f : -1.0f;
            ai.vx = dir * 180.0f;
            ai.vy = 2.5f;
        } else {
            ai.startAction(ActionState::WEAPON_SLASH, 0.38f);
        }
        return;
    }

    // Close distance / Melee Combat Zone: Unleash deadly combos!
    if (dist <= optimalRange + 30.0f) {
        if (r < 0.22f) {
            ai.startAction(ActionState::WEAPON_SLASH, 0.35f);
        } else if (r < 0.42f) {
            ai.startAction(ActionState::WEAPON_SPIN, 0.45f);
        } else if (r < 0.58f) {
            ai.startAction(ActionState::WEAPON_PIERCE, 0.32f);
        } else if (r < 0.72f) {
            ai.startAction(ActionState::PUNCH_UPPERCUT, 0.35f);
        } else if (r < 0.84f) {
            ai.startAction(ActionState::KICK_ROUNDHOUSE, 0.38f);
        } else if (r < 0.93f) {
            ai.startAction(ActionState::KICK_LOW, 0.28f);
        } else {
            // Tactical retreat or roll backward
            ai.startAction(ActionState::ROLL_BACKWARD, 0.35f);
            float dir = (player.x > ai.x) ? -1.0f : 1.0f;
            ai.vx = dir * 180.0f;
        }
    }
}

} // namespace ShadowCombat
