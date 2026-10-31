#pragma once

#include <vector>
#include <memory>
#include "Math.hpp"
#include "CombatTypes.hpp"
#include "Fighter.hpp"
#include "ParticleSystem.hpp"
#include "Bosses.hpp"
#include "AIController.hpp"

namespace ShadowCombat {

class CombatEngine {
public:
    Fighter player;
    Fighter enemy;
    ParticleSystem particles;
    std::vector<Projectile> projectiles;
    AIController aiController;

    float arenaWidth = 800.0f;
    float groundY = 320.0f;
    float screenShake = 0.0f;
    ArenaType arena = ArenaType::CRIMSON_PAGODA;
    MatchPhase matchPhase = MatchPhase::FIGHTING;
    float phaseTimer = 0.0f;
    int playerRoundWins = 0;
    int enemyRoundWins = 0;
    int currentRound = 1;
    BossProfile currentBoss;

    int64_t nextProjectileId = 1;

    CombatEngine();

    void startMatch(const std::string& bossId = "lynx", WeaponType playerWeapon = WeaponType::KATANA, ArenaType arenaType = ArenaType::CRIMSON_PAGODA);
    void resetRound();
    void update(float dt);

    // Player Commands
    void playerWalk(float direction); // -1.0 to 1.0
    void playerJump(float forwardDir = 0.0f);
    void playerCrouch();
    void playerBlock(bool blocking);
    void playerRoll(bool forward);
    void playerAttack(ActionState attackType);
    void playerThrowRanged();
    void playerShadowSurge();

    bool isMatchOver() const { return matchPhase == MatchPhase::MATCH_OVER; }

private:
    void updateFighterPhysics(Fighter& f, const Fighter& opponent, float dt);
    void checkAttackHit(Fighter& attacker, Fighter& defender);
    void updateProjectiles(float dt);
    void handleRoundTransitions(float dt);
};

} // namespace ShadowCombat
