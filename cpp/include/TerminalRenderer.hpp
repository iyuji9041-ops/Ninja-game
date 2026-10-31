#pragma once

#include <string>
#include <vector>
#include "CombatEngine.hpp"

namespace ShadowCombat {

class TerminalRenderer {
public:
    static constexpr int SCREEN_W = 80;
    static constexpr int SCREEN_H = 24;

private:
    char buffer[SCREEN_H][SCREEN_W];
    std::string colorBuffer[SCREEN_H][SCREEN_W];

public:
    TerminalRenderer();

    void clearBuffer();
    void render(const CombatEngine& engine);
    void present() const;

private:
    void drawHud(const CombatEngine& engine);
    void drawArena(ArenaType arena);
    void drawFighter(const Fighter& f, bool isPlayer);
    void drawProjectiles(const std::vector<Projectile>& projectiles);
    void drawParticles(const ParticleSystem& particles);
    void setChar(int x, int y, char c, const std::string& color);
    void drawString(int x, int y, const std::string& str, const std::string& color);
};

} // namespace ShadowCombat
