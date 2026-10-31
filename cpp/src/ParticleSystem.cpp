#include "ParticleSystem.hpp"
#include <cstdlib>

namespace ShadowCombat {

static float randomRange(float minVal, float maxVal) {
    float r = static_cast<float>(rand()) / static_cast<float>(RAND_MAX);
    return minVal + r * (maxVal - minVal);
}

void ParticleSystem::update(float dt) {
    // Update Particles
    for (size_t i = 0; i < particles.size();) {
        auto& p = particles[i];
        p.life -= dt;
        if (p.isDead()) {
            particles[i] = particles.back();
            particles.pop_back();
        } else {
            p.pos += p.vel * dt;
            // Gravity for blood and sparks
            if (p.type == ParticleType::SPARK || p.type == ParticleType::BLOOD_SPLITTER) {
                p.vel.y -= 150.0f * dt;
            }
            ++i;
        }
    }

    // Update Floating Combat Texts
    for (size_t i = 0; i < floatingTexts.size();) {
        auto& t = floatingTexts[i];
        t.life -= dt;
        if (t.isDead()) {
            floatingTexts[i] = floatingTexts.back();
            floatingTexts.pop_back();
        } else {
            t.pos += t.vel * dt;
            t.vel.y += 15.0f * dt; // Rise upwards
            ++i;
        }
    }
}

void ParticleSystem::clear() {
    particles.clear();
    floatingTexts.clear();
}

void ParticleSystem::spawnSparks(Vec2 pos, int count, uint32_t color) {
    for (int i = 0; i < count; ++i) {
        Particle p;
        p.pos = pos;
        p.vel = Vec2(randomRange(-180.0f, 180.0f), randomRange(-50.0f, 220.0f));
        p.color = color;
        p.size = randomRange(2.0f, 5.0f);
        p.life = randomRange(0.2f, 0.45f);
        p.maxLife = p.life;
        p.type = ParticleType::SPARK;
        particles.push_back(p);
    }
}

void ParticleSystem::spawnBloodSplatter(Vec2 pos, int count, bool facingRight) {
    float baseDir = facingRight ? 1.0f : -1.0f;
    for (int i = 0; i < count; ++i) {
        Particle p;
        p.pos = pos;
        p.vel = Vec2(baseDir * randomRange(40.0f, 240.0f), randomRange(-40.0f, 180.0f));
        p.color = 0xFFD32F2F; // Crimson blood shadow
        p.size = randomRange(3.0f, 6.0f);
        p.life = randomRange(0.3f, 0.6f);
        p.maxLife = p.life;
        p.type = ParticleType::BLOOD_SPLITTER;
        particles.push_back(p);
    }
}

void ParticleSystem::spawnDust(Vec2 pos, int count) {
    for (int i = 0; i < count; ++i) {
        Particle p;
        p.pos = pos;
        p.vel = Vec2(randomRange(-50.0f, 50.0f), randomRange(10.0f, 50.0f));
        p.color = 0x889E9E9E;
        p.size = randomRange(3.0f, 8.0f);
        p.life = randomRange(0.3f, 0.5f);
        p.maxLife = p.life;
        p.type = ParticleType::DUST_CLOUD;
        particles.push_back(p);
    }
}

void ParticleSystem::spawnShadowFlame(Vec2 pos, int count) {
    for (int i = 0; i < count; ++i) {
        Particle p;
        p.pos = pos + Vec2(randomRange(-15.0f, 15.0f), randomRange(-15.0f, 15.0f));
        p.vel = Vec2(randomRange(-25.0f, 25.0f), randomRange(60.0f, 160.0f));
        p.color = (rand() % 2 == 0) ? 0xDD7B1FA2 : 0xDD00E5FF;
        p.size = randomRange(4.0f, 10.0f);
        p.life = randomRange(0.3f, 0.6f);
        p.maxLife = p.life;
        p.type = ParticleType::SHADOW_FLAME;
        particles.push_back(p);
    }
}

void ParticleSystem::spawnShockwave(Vec2 pos, uint32_t color) {
    Particle p;
    p.pos = pos;
    p.vel = Vec2(0.0f, 0.0f);
    p.color = color;
    p.size = 12.0f;
    p.life = 0.4f;
    p.maxLife = 0.4f;
    p.type = ParticleType::SHOCKWAVE;
    particles.push_back(p);
}

void ParticleSystem::addFloatingText(const std::string& text, Vec2 pos, uint32_t color, float scale) {
    FloatingCombatText t;
    t.text = text;
    t.pos = pos;
    t.vel = Vec2(randomRange(-15.0f, 15.0f), 45.0f);
    t.color = color;
    t.life = 0.9f;
    t.maxLife = 0.9f;
    t.scale = scale;
    floatingTexts.push_back(t);
}

} // namespace ShadowCombat
