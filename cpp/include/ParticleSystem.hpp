#pragma once

#include <cstdint>
#include <vector>
#include <string>
#include "Math.hpp"

namespace ShadowCombat {

enum class ParticleType {
    SPARK,
    BLOOD_SPLITTER,
    DUST_CLOUD,
    SLASH_TRAIL,
    SHADOW_FLAME,
    SHOCKWAVE
};

struct Particle {
    Vec2 pos;
    Vec2 vel;
    uint32_t color = 0xFFFFFFFF;
    float size = 4.0f;
    float life = 0.5f;
    float maxLife = 0.5f;
    ParticleType type = ParticleType::SPARK;

    bool isDead() const { return life <= 0.0f; }
};

struct FloatingCombatText {
    std::string text;
    Vec2 pos;
    Vec2 vel;
    uint32_t color = 0xFFFFFFFF;
    float life = 0.8f;
    float maxLife = 0.8f;
    float scale = 1.0f;

    bool isDead() const { return life <= 0.0f; }
};

class ParticleSystem {
private:
    std::vector<Particle> particles;
    std::vector<FloatingCombatText> floatingTexts;

public:
    ParticleSystem() {
        particles.reserve(256);
        floatingTexts.reserve(64);
    }

    void update(float dt);
    void clear();

    void spawnSparks(Vec2 pos, int count, uint32_t color = 0xFFFFD54F);
    void spawnBloodSplatter(Vec2 pos, int count, bool facingRight);
    void spawnDust(Vec2 pos, int count);
    void spawnShadowFlame(Vec2 pos, int count);
    void spawnShockwave(Vec2 pos, uint32_t color = 0xAA9C27B0);
    void addFloatingText(const std::string& text, Vec2 pos, uint32_t color = 0xFFFFFFFF, float scale = 1.0f);

    const std::vector<Particle>& getParticles() const { return particles; }
    const std::vector<FloatingCombatText>& getFloatingTexts() const { return floatingTexts; }
};

} // namespace ShadowCombat
