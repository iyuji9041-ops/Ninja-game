#pragma once

#include <string>
#include <cstdint>

namespace ShadowCombat {

enum class ActionState {
    IDLE,
    WALK_FORWARD,
    WALK_BACKWARD,
    JUMP_UP,
    JUMP_FORWARD,
    JUMP_BACKWARD,
    CROUCH,
    ROLL_FORWARD,
    ROLL_BACKWARD,
    PUNCH_JAB,
    PUNCH_HOOK,
    PUNCH_UPPERCUT,
    KICK_LOW,
    KICK_ROUNDHOUSE,
    KICK_FLYING,
    WEAPON_SLASH,
    WEAPON_SPIN,
    WEAPON_PIERCE,
    RANGED_THROW,
    SHADOW_SURGE,
    BLOCKING,
    HIT_LIGHT,
    HIT_HEAVY,
    KNOCKDOWN,
    VICTORY,
    DEFEAT
};

enum class HitType {
    NORMAL,
    HEAD_HIT,
    LOW_SWEEP,
    CRITICAL,
    BLOCKED,
    SHADOW_BLAST
};

enum class ArenaType {
    CRIMSON_PAGODA,
    BAMBOO_GROVE,
    GATES_OF_SHADOWS,
    MOONLIT_TEMPLE
};

enum class GameMode {
    CAMPAIGN,
    SURVIVAL,
    TRAINING
};

enum class MatchPhase {
    ANNOUNCING_ROUND,
    FIGHTING,
    ROUND_OVER,
    MATCH_OVER
};

struct AttackInfo {
    float damage = 10.0f;
    float range = 50.0f;
    float startupTime = 0.1f;
    float activeTime = 0.2f;
    float recoveryTime = 0.15f;
    float knockback = 15.0f;
    bool isLow = false;
    bool isOverhead = false;
    bool isUnblockable = false;
    float shadowEnergyGain = 12.0f;
    std::string name = "Attack";
};

} // namespace ShadowCombat
