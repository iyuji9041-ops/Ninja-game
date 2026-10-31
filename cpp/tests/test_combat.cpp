#include <cassert>
#include <iostream>
#include "CombatEngine.hpp"

using namespace ShadowCombat;

void testVectorMath() {
    Vec2 a(3.0f, 4.0f);
    assert(std::abs(a.length() - 5.0f) < 0.001f);
    Vec2 n = a.normalized();
    assert(std::abs(n.length() - 1.0f) < 0.001f);
    std::cout << "[PASS] Vector Math\n";
}

void testFighterStateTransitions() {
    Fighter f("TestShinobi", true, WeaponType::KATANA);
    assert(f.actionState == ActionState::IDLE);
    assert(f.canPerformAction());

    f.startAction(ActionState::WEAPON_SLASH, 0.4f);
    assert(f.actionState == ActionState::WEAPON_SLASH);
    assert(f.isAttacking());

    f.takeDamage(20.0f, HitType::NORMAL, 15.0f);
    assert(f.health == 80.0f);
    assert(f.shadowEnergy > 0.0f); // Gained energy from being struck

    std::cout << "[PASS] Fighter State Transitions\n";
}

void testHitboxIntersection() {
    Fighter p1("P1", true, WeaponType::KATANA);
    Fighter p2("P2", false, WeaponType::KATANA);

    p1.x = 200.0f;
    p2.x = 240.0f; // In melee range
    p1.facingRight = true;
    p2.facingRight = false;

    p1.startAction(ActionState::WEAPON_SLASH, 0.4f);
    p1.actionFrame = 0.5f; // Middle of strike

    Rect hit = p1.getHitbox();
    Rect hurt = p2.getHurtbox();

    assert(hit.width() > 0);
    assert(hurt.width() > 0);
    assert(hit.intersects(hurt));

    std::cout << "[PASS] Hitbox vs Hurtbox Intersection\n";
}

void testCombatEngineSimulation() {
    CombatEngine engine;
    assert(engine.player.health == 100.0f);
    assert(engine.enemy.health > 0.0f);

    // Simulate 60 frames (1 second of physics)
    float dt = 1.0f / 60.0f;
    for (int i = 0; i < 60; ++i) {
        engine.update(dt);
    }

    assert(engine.player.x >= 50.0f && engine.player.x <= engine.arenaWidth - 50.0f);
    assert(engine.enemy.x >= 50.0f && engine.enemy.x <= engine.arenaWidth - 50.0f);

    std::cout << "[PASS] Combat Engine 60-Frame Simulation\n";
}

int main() {
    std::cout << ">>> RUNNING SHADOW COMBAT C++ TEST SUITE <<<\n";
    testVectorMath();
    testFighterStateTransitions();
    testHitboxIntersection();
    testCombatEngineSimulation();
    std::cout << ">>> ALL C++ TESTS PASSED SUCCESSFULLY! <<<\n";
    return 0;
}
