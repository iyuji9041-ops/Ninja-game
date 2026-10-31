#pragma once

#include <string>
#include <vector>
#include "Weapons.hpp"

namespace ShadowCombat {

struct BossProfile {
    std::string id = "lynx";
    std::string name = "Lynx";
    std::string title = "The Silent Assassin";
    WeaponType weaponType = WeaponType::DEMON_CLAWS;
    float health = 150.0f;
    float attackMultiplier = 1.15f;
    float speedMultiplier = 1.1f;
    float aggression = 0.75f;
    float blockChance = 0.45f;
    bool hasInvisibility = true;
    bool hasShadowBlast = true;
    std::string introQuote = "You dare step into my territory, shadow? Prepare to bleed.";
    std::string defeatQuote = "Impossible... the shadows bend to you...";
};

class BossDatabase {
public:
    static std::vector<BossProfile> getAllBosses() {
        return {
            { "lynx", "Kagetsu Lynx", "Demon of the Claws", WeaponType::DEMON_CLAWS,
              160.0f, 1.2f, 1.15f, 0.75f, 0.40f, true, true,
              "Your flesh will tear before your eyes perceive my strike!",
              "My claws... shattered?!" },
            { "hermit", "Hermit", "Master of Wind & Lightning", WeaponType::STEEL_STAFF,
              190.0f, 1.3f, 0.95f, 0.65f, 0.60f, false, true,
              "Knowledge is power, young ninja. Taste true mastery!",
              "The lightning has chosen a new vessel..." },
            { "butcher", "Butcher", "The Cleaver Demon", WeaponType::BLOOD_SICKLES,
              230.0f, 1.45f, 0.85f, 0.85f, 0.30f, false, true,
              "Fresh meat for my blades! Slice! Hack! Chop!",
              "Ugh... too fast... you rat..." },
            { "wasp", "Wasp", "Empress of the Seas", WeaponType::KATANA,
              210.0f, 1.35f, 1.25f, 0.80f, 0.50f, true, true,
              "The tide rises to drown your insolence!",
              "Swept away... by a mere shadow..." },
            { "shogun", "Shogun", "Warlord of the Iron Gate", WeaponType::KATANA,
              270.0f, 1.55f, 1.05f, 0.85f, 0.65f, false, true,
              "Bow before the true ruler! My honor demands your demise!",
              "A warrior without equal... I yield..." },
            { "titan", "Titan", "Ruler of the Gates of Shadows", WeaponType::STEEL_STAFF,
              350.0f, 1.8f, 0.90f, 0.90f, 0.70f, true, true,
              "I am eternity! Bow before the sovereign of darkness!",
              "No! The Gates... are closing forever..." }
        };
    }

    static BossProfile get(const std::string& id) {
        for (const auto& b : getAllBosses()) {
            if (b.id == id) return b;
        }
        return getAllBosses()[0];
    }
};

} // namespace ShadowCombat
