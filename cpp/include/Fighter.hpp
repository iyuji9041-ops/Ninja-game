#pragma once

#include <string>
#include "Math.hpp"
#include "CombatTypes.hpp"
#include "Weapons.hpp"

namespace ShadowCombat {

struct Projectile {
    int64_t id = 0;
    float x = 0.0f;
    float y = 0.0f;
    float vx = 0.0f;
    float vy = 0.0f;
    bool isPlayer = true;
    float damage = 15.0f;
    float rotation = 0.0f;
    bool isAlive = true;
};

class Fighter {
public:
    std::string name = "Shadow";
    bool isPlayer = true;
    float x = 200.0f;
    float y = 0.0f; // Height above ground (jump elevation)
    float vx = 0.0f;
    float vy = 0.0f;
    bool facingRight = true;

    // Health & Energy
    float health = 100.0f;
    float maxHealth = 100.0f;
    float shadowEnergy = 0.0f;
    float maxShadowEnergy = 100.0f;
    bool isShadowMode = false;
    float shadowModeTimer = 0.0f;

    // Combat State
    ActionState actionState = ActionState::IDLE;
    float actionTime = 0.0f;
    float actionDuration = 0.35f;
    float actionFrame = 0.0f; // 0.0 to 1.0 progress in current action
    bool attackHasHit = false;

    // Defense & Armor
    float defenseMultiplier = 1.0f;
    float armorRating = 0.0f;
    int comboCount = 0;
    float comboResetTimer = 0.0f;

    // Cooldowns
    float rangedCooldown = 0.0f;
    float dodgeCooldown = 0.0f;

    // Weapon & Cosmetics
    WeaponDef weapon;
    uint32_t eyeColor = 0xFF4FC3F7;    // Glowing cyan eyes
    uint32_t ribbonColor = 0xFFD32F2F; // Crimson ninja headband

    Fighter(const std::string& fighterName = "Shadow", bool player = true, WeaponType weaponType = WeaponType::KATANA)
        : name(fighterName), isPlayer(player) {
        weapon = WeaponDatabase::get(weaponType);
        if (!player) {
            eyeColor = 0xFFE53935;    // Demon red eyes
            ribbonColor = 0xFF7B1FA2; // Purple sash
        }
    }

    void reset(float startX, bool faceRight) {
        x = startX;
        y = 0.0f;
        vx = 0.0f;
        vy = 0.0f;
        facingRight = faceRight;
        health = maxHealth;
        shadowEnergy = 0.0f;
        isShadowMode = false;
        shadowModeTimer = 0.0f;
        actionState = ActionState::IDLE;
        actionTime = 0.0f;
        actionDuration = 0.35f;
        actionFrame = 0.0f;
        attackHasHit = false;
        comboCount = 0;
        comboResetTimer = 0.0f;
        rangedCooldown = 0.0f;
    }

    bool isGrounded() const { return y <= 0.001f; }
    bool isAttacking() const;
    bool isDefending() const { return actionState == ActionState::BLOCKING; }
    bool isStunned() const;

    Rect getHurtbox(float u = 1.0f) const;
    Rect getHitbox(float u = 1.0f) const;

    bool canPerformAction() const;
    void startAction(ActionState state, float duration = 0.35f);
    void takeDamage(float amount, HitType hitType, float knockback);
    void activateShadowMode();
};

} // namespace ShadowCombat
