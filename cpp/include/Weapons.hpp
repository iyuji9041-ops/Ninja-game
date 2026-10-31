#pragma once

#include <string>
#include <vector>
#include "CombatTypes.hpp"

namespace ShadowCombat {

enum class WeaponType {
    UNARMED,
    KATANA,
    KUSARIGAMA,
    TWIN_SAI,
    STEEL_STAFF,
    DEMON_CLAWS,
    BLOOD_SICKLES,
    NUNCHAKU
};

struct WeaponDef {
    WeaponType type = WeaponType::KATANA;
    std::string id = "katana";
    std::string name = "Shadow Katana";
    std::string description = "Razor-sharp folded steel forged in midnight shadow.";
    float baseDamage = 18.0f;
    float attackSpeedMultiplier = 1.0f;
    float rangeMultiplier = 1.0f;
    float critChance = 0.15f;
    float shadowGainMultiplier = 1.0f;
    uint32_t bladeColor = 0xFF58C4DC; // Cyan steel
    uint32_t trailColor = 0x884BA3E3;
};

class WeaponDatabase {
public:
    static WeaponDef get(WeaponType type) {
        switch (type) {
            case WeaponType::KATANA:
                return { WeaponType::KATANA, "katana", "Shadow Katana",
                         "Traditional folded steel with fast lethal slashing arcs.",
                         20.0f, 1.1f, 1.15f, 0.18f, 1.1f, 0xFF64B5F6, 0x992196F3 };
            case WeaponType::KUSARIGAMA:
                return { WeaponType::KUSARIGAMA, "kusarigama", "Whirlwind Kusarigama",
                         "Sickle and weighted chain for formidable mid-to-long range zone control.",
                         17.0f, 0.95f, 1.45f, 0.14f, 1.25f, 0xFFFFB74D, 0x99FF9800 };
            case WeaponType::TWIN_SAI:
                return { WeaponType::TWIN_SAI, "twin_sai", "Twin Serpent Sai",
                         "Rapid dual prongs delivering blazing high-frequency punctures.",
                         14.0f, 1.4f, 0.9f, 0.25f, 1.0f, 0xFF81C784, 0x994CAF50 };
            case WeaponType::STEEL_STAFF:
                return { WeaponType::STEEL_STAFF, "steel_staff", "Iron Dragon Staff",
                         "Heavy blunt reach that breaks guards and staggers opponents.",
                         24.0f, 0.85f, 1.35f, 0.12f, 1.15f, 0xFFBA68C8, 0x999C27B0 };
            case WeaponType::DEMON_CLAWS:
                return { WeaponType::DEMON_CLAWS, "demon_claws", "Kagetsu Claws",
                         "Curved razor talons infused with demonic blood fire.",
                         28.0f, 1.25f, 0.95f, 0.30f, 1.4f, 0xFFE57373, 0x99F44336 };
            case WeaponType::BLOOD_SICKLES:
                return { WeaponType::BLOOD_SICKLES, "blood_sickles", "Blood Moon Sickles",
                         "Paired curved crescents that rip through armor.",
                         22.0f, 1.2f, 1.05f, 0.22f, 1.2f, 0xFFFF5252, 0xAAFF1744 };
            case WeaponType::NUNCHAKU:
                return { WeaponType::NUNCHAKU, "nunchaku", "Thunder Nunchaku",
                         "Relentless whirling momentum granting unstoppable combo chains.",
                         16.0f, 1.35f, 1.0f, 0.20f, 1.3f, 0xFFFFD700, 0x99FFC107 };
            case WeaponType::UNARMED:
            default:
                return { WeaponType::UNARMED, "unarmed", "Bare Fist Shinobi",
                         "Pure martial discipline relying on raw speed and hand-to-hand precision.",
                         12.0f, 1.2f, 0.8f, 0.10f, 1.0f, 0xFFE0E0E0, 0x889E9E9E };
        }
    }
};

} // namespace ShadowCombat
