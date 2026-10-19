package com.example.game.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.FlameAmber
import com.example.ui.theme.SoulCyan

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
}

enum class HitType {
    NORMAL,
    HEAD_HIT,
    LOW_SWEEP,
    CRITICAL,
    BLOCKED,
    SHADOW_BLAST
}

enum class ArenaType {
    CRIMSON_PAGODA,
    BAMBOO_GROVE,
    GATES_OF_SHADOWS
}

enum class GameMode {
    CAMPAIGN,
    SURVIVAL,
    TRAINING
}

enum class MatchPhase {
    ANNOUNCING_ROUND,
    FIGHTING,
    ROUND_ENDED,
    MATCH_OVER
}

data class Weapon(
    val id: String,
    val name: String,
    val hindiName: String,
    val typeName: String,
    val baseDamage: Float,
    val speed: Float, // 1.0 = normal, 1.2 = fast
    val range: Float, // melee range multiplier
    val critChance: Float, // 0.15 = 15%
    val bladeColor: Color,
    val price: Int,
    val level: Int = 1,
    val isUnlocked: Boolean = false,
    val description: String
)

data class ArmorItem(
    val id: String,
    val name: String,
    val hindiName: String,
    val isHelmet: Boolean,
    val defenseBonus: Float, // percentage reduction
    val healthBonus: Float,
    val price: Int,
    val level: Int = 1,
    val isUnlocked: Boolean = false,
    val description: String
)

data class BossProfile(
    val id: String,
    val name: String,
    val title: String,
    val weapon: Weapon,
    val maxHealth: Float,
    val attackMult: Float,
    val defenseMult: Float,
    val aiAggressiveness: Float, // 0.0 to 1.0
    val shadowMoveName: String,
    val eyeColor: Color,
    val ribbonColor: Color,
    val arena: ArenaType,
    val rewardCoins: Int,
    val quote: String
)

data class Projectile(
    val id: Long,
    var x: Float,
    var y: Float,
    val vx: Float,
    val fromPlayer: Boolean,
    val damage: Float,
    var rotation: Float = 0f,
    var active: Boolean = true
)

enum class ParticleType {
    SPARK,
    BLOOD_MIST,
    SHADOW_SMOKE,
    CHERRY_LEAF,
    FIREFLY,
    SHOCKWAVE
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color,
    val size: Float,
    val type: ParticleType
)

data class FloatingText(
    val text: String,
    var x: Float,
    var y: Float,
    val color: Color,
    var alpha: Float = 1f,
    var scale: Float = 1f,
    var life: Float = 1f
)

data class Fighter(
    val isPlayer: Boolean,
    var x: Float,
    var y: Float = 0f, // 0 is ground level, positive is jumping
    var vy: Float = 0f,
    var facingRight: Boolean = true,
    var health: Float = 100f,
    var maxHealth: Float = 100f,
    var shadowEnergy: Float = 0f, // 0 to 100
    var actionState: ActionState = ActionState.IDLE,
    var actionFrame: Float = 0f,
    var actionDuration: Float = 1f,
    var isInvulnerable: Boolean = false,
    var weapon: Weapon,
    var roundsWon: Int = 0,
    var comboCount: Int = 0,
    var comboTimer: Float = 0f,
    var eyeColor: Color = SoulCyan,
    var ribbonColor: Color = BloodCrimson,
    var isShadowMode: Boolean = false,
    var shadowModeTimer: Float = 0f,
    var hitCooldown: Float = 0f
)
