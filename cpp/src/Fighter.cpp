#include "Fighter.hpp"
#include <algorithm>

namespace ShadowCombat {

bool Fighter::isAttacking() const {
    switch (actionState) {
        case ActionState::PUNCH_JAB:
        case ActionState::PUNCH_HOOK:
        case ActionState::PUNCH_UPPERCUT:
        case ActionState::KICK_LOW:
        case ActionState::KICK_ROUNDHOUSE:
        case ActionState::KICK_FLYING:
        case ActionState::WEAPON_SLASH:
        case ActionState::WEAPON_SPIN:
        case ActionState::WEAPON_PIERCE:
        case ActionState::RANGED_THROW:
        case ActionState::SHADOW_SURGE:
            return true;
        default:
            return false;
    }
}

bool Fighter::isStunned() const {
    return actionState == ActionState::HIT_LIGHT ||
           actionState == ActionState::HIT_HEAVY ||
           actionState == ActionState::KNOCKDOWN ||
           actionState == ActionState::DEFEAT;
}

bool Fighter::canPerformAction() const {
    if (health <= 0.0f) return false;
    if (isStunned()) return false;
    if (actionState == ActionState::ROLL_FORWARD || actionState == ActionState::ROLL_BACKWARD) return false;
    if (isAttacking()) return false; // Must finish attack active/recovery or combo cancel
    return true;
}

void Fighter::startAction(ActionState state, float duration) {
    actionState = state;
    actionTime = 0.0f;
    actionDuration = std::max(0.05f, duration);
    actionFrame = 0.0f;
    attackHasHit = false;
}

void Fighter::takeDamage(float amount, HitType hitType, float knockback) {
    if (health <= 0.0f) return;

    float effectiveDamage = amount;
    if (actionState == ActionState::BLOCKING) {
        effectiveDamage *= 0.25f; // Block reduces 75% damage
    } else {
        effectiveDamage *= defenseMultiplier;
    }

    health = std::max(0.0f, health - effectiveDamage);

    // Apply knockback direction based on facing
    float kbDir = facingRight ? -1.0f : 1.0f;
    vx = kbDir * knockback;

    // Gain shadow energy on taking damage
    shadowEnergy = std::min(maxShadowEnergy, shadowEnergy + effectiveDamage * 0.4f);

    if (health <= 0.0f) {
        startAction(ActionState::DEFEAT, 1.5f);
        vy = 2.0f;
    } else if (actionState != ActionState::BLOCKING) {
        if (knockback > 20.0f || hitType == HitType::CRITICAL || hitType == HitType::SHADOW_BLAST) {
            startAction(ActionState::KNOCKDOWN, 0.7f);
            vy = 3.5f;
        } else if (knockback > 10.0f) {
            startAction(ActionState::HIT_HEAVY, 0.45f);
        } else {
            startAction(ActionState::HIT_LIGHT, 0.28f);
        }
    }
}

void Fighter::activateShadowMode() {
    if (shadowEnergy >= 50.0f && !isShadowMode && canPerformAction()) {
        isShadowMode = true;
        shadowModeTimer = 10.0f;
        shadowEnergy = 0.0f;
        defenseMultiplier = 0.75f; // 25% defense bonus in shadow form
    }
}

Rect Fighter::getHurtbox(float u) const {
    float width = 36.0f * u;
    float height = (actionState == ActionState::CROUCH || actionState == ActionState::ROLL_FORWARD || actionState == ActionState::ROLL_BACKWARD)
                   ? 50.0f * u : 100.0f * u;

    float left = x - width * 0.5f;
    float right = x + width * 0.5f;
    float bottom = y;
    float top = y + height;
    return Rect(left, top, right, bottom);
}

Rect Fighter::getHitbox(float u) const {
    if (!isAttacking() || attackHasHit) {
        return Rect(0, 0, 0, 0);
    }

    // Only active during the strike phase (30% to 75% of animation)
    if (actionFrame < 0.25f || actionFrame > 0.75f) {
        return Rect(0, 0, 0, 0);
    }

    float forwardOffset = 15.0f * u;
    float hitWidth = 50.0f * u * weapon.rangeMultiplier;
    float hitHeight = 40.0f * u;
    float hitY = y + 50.0f * u;

    switch (actionState) {
        case ActionState::PUNCH_JAB:
        case ActionState::PUNCH_HOOK:
            hitWidth = 45.0f * u;
            hitHeight = 30.0f * u;
            hitY = y + 65.0f * u;
            break;
        case ActionState::PUNCH_UPPERCUT:
            hitWidth = 45.0f * u;
            hitHeight = 55.0f * u;
            hitY = y + 70.0f * u;
            break;
        case ActionState::KICK_LOW:
            hitWidth = 55.0f * u;
            hitHeight = 25.0f * u;
            hitY = y + 15.0f * u;
            break;
        case ActionState::KICK_ROUNDHOUSE:
        case ActionState::KICK_FLYING:
            hitWidth = 65.0f * u;
            hitHeight = 40.0f * u;
            hitY = y + 60.0f * u;
            break;
        case ActionState::WEAPON_SLASH:
            hitWidth = 75.0f * u * weapon.rangeMultiplier;
            hitHeight = 55.0f * u;
            hitY = y + 55.0f * u;
            break;
        case ActionState::WEAPON_SPIN:
            hitWidth = 95.0f * u * weapon.rangeMultiplier;
            hitHeight = 70.0f * u;
            hitY = y + 50.0f * u;
            forwardOffset = -15.0f * u;
            break;
        case ActionState::WEAPON_PIERCE:
            hitWidth = 90.0f * u * weapon.rangeMultiplier;
            hitHeight = 25.0f * u;
            hitY = y + 50.0f * u;
            break;
        case ActionState::SHADOW_SURGE:
            hitWidth = 140.0f * u;
            hitHeight = 90.0f * u;
            hitY = y + 50.0f * u;
            forwardOffset = -20.0f * u;
            break;
        default:
            break;
    }

    float left = facingRight ? (x + forwardOffset) : (x - forwardOffset - hitWidth);
    float right = left + hitWidth;
    float bottom = hitY - hitHeight * 0.5f;
    float top = hitY + hitHeight * 0.5f;
    return Rect(left, top, right, bottom);
}

} // namespace ShadowCombat
