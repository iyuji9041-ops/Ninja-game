package com.example.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "shadow_combat_prefs")

data class SavedGameData(
    val coins: Int,
    val currentBossIndex: Int,
    val equippedWeaponId: String,
    val equippedArmorId: String,
    val unlockedWeaponIds: Set<String>,
    val weaponLevels: Map<String, Int>,
    val unlockedArmorIds: Set<String>,
    val armorLevels: Map<String, Int>
)

class GameDataRepository(private val context: Context) {

    private object PreferencesKeys {
        val COINS = intPreferencesKey("coins")
        val CURRENT_BOSS_INDEX = intPreferencesKey("current_boss_index")
        val EQUIPPED_WEAPON = stringPreferencesKey("equipped_weapon")
        val EQUIPPED_ARMOR = stringPreferencesKey("equipped_armor")
        val UNLOCKED_WEAPONS = stringSetPreferencesKey("unlocked_weapons")
        val UNLOCKED_ARMORS = stringSetPreferencesKey("unlocked_armors")
        val WEAPON_LEVELS = stringPreferencesKey("weapon_levels")
        val ARMOR_LEVELS = stringPreferencesKey("armor_levels")
    }

    val savedDataFlow: Flow<SavedGameData> = context.gameDataStore.data.map { prefs ->
        val coins = prefs[PreferencesKeys.COINS] ?: 300
        val bossIndex = prefs[PreferencesKeys.CURRENT_BOSS_INDEX] ?: 0
        val weaponId = prefs[PreferencesKeys.EQUIPPED_WEAPON] ?: "katana"
        val armorId = prefs[PreferencesKeys.EQUIPPED_ARMOR] ?: "gi"
        val unlockedWeapons = prefs[PreferencesKeys.UNLOCKED_WEAPONS] ?: setOf("fists", "katana")
        val unlockedArmors = prefs[PreferencesKeys.UNLOCKED_ARMORS] ?: setOf("gi")

        val weaponLevelsStr = prefs[PreferencesKeys.WEAPON_LEVELS] ?: ""
        val weaponLevels = parseLevels(weaponLevelsStr)

        val armorLevelsStr = prefs[PreferencesKeys.ARMOR_LEVELS] ?: ""
        val armorLevels = parseLevels(armorLevelsStr)

        SavedGameData(
            coins = coins,
            currentBossIndex = bossIndex,
            equippedWeaponId = weaponId,
            equippedArmorId = armorId,
            unlockedWeaponIds = unlockedWeapons,
            weaponLevels = weaponLevels,
            unlockedArmorIds = unlockedArmors,
            armorLevels = armorLevels
        )
    }

    suspend fun saveGameData(
        coins: Int,
        currentBossIndex: Int,
        equippedWeaponId: String,
        equippedArmorId: String,
        unlockedWeaponIds: Set<String>,
        weaponLevels: Map<String, Int>,
        unlockedArmorIds: Set<String>,
        armorLevels: Map<String, Int>
    ) {
        context.gameDataStore.edit { prefs ->
            prefs[PreferencesKeys.COINS] = coins
            prefs[PreferencesKeys.CURRENT_BOSS_INDEX] = currentBossIndex
            prefs[PreferencesKeys.EQUIPPED_WEAPON] = equippedWeaponId
            prefs[PreferencesKeys.EQUIPPED_ARMOR] = equippedArmorId
            prefs[PreferencesKeys.UNLOCKED_WEAPONS] = unlockedWeaponIds
            prefs[PreferencesKeys.UNLOCKED_ARMORS] = unlockedArmorIds
            prefs[PreferencesKeys.WEAPON_LEVELS] = encodeLevels(weaponLevels)
            prefs[PreferencesKeys.ARMOR_LEVELS] = encodeLevels(armorLevels)
        }
    }

    private fun parseLevels(encoded: String): Map<String, Int> {
        if (encoded.isBlank()) return emptyMap()
        return encoded.split(",")
            .mapNotNull {
                val parts = it.split(":")
                if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 1) else null
            }.toMap()
    }

    private fun encodeLevels(levels: Map<String, Int>): String {
        return levels.entries.joinToString(",") { "${it.key}:${it.value}" }
    }
}
