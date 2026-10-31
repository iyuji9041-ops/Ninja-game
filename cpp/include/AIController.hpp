#pragma once

#include "Fighter.hpp"
#include "Bosses.hpp"

namespace ShadowCombat {

class AIController {
private:
    float decisionTimer = 0.0f;
    float aggressionCooldown = 0.0f;
    float blockTimer = 0.0f;

public:
    AIController() = default;

    void update(Fighter& ai, const Fighter& player, const BossProfile* boss, float dt, float arenaWidth);
    void reset();

private:
    void decideNextMove(Fighter& ai, const Fighter& player, const BossProfile* boss, float dist);
};

} // namespace ShadowCombat
