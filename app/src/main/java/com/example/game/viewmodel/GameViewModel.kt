package com.example.game.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.SoundManager
import com.example.game.data.GameDataRepository
import com.example.game.model.ActionState
import com.example.game.model.ArenaType
import com.example.game.model.ArmorItem
import com.example.game.model.BossProfile
import com.example.game.model.Fighter
import com.example.game.model.GameMode
import com.example.game.model.MatchPhase
import com.example.game.model.Weapon
import com.example.game.physics.CombatEngine
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.FlameAmber
import com.example.ui.theme.GoldYellow
import com.example.ui.theme.SoulCyan
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class ScreenState {
    DOJO,
    COMBAT
}

data class GameUiState(
    val currentScreen: ScreenState = ScreenState.DOJO,
    val coins: Int = 300,
    val currentBossIndex: Int = 0,
    val selectedBossIndex: Int = 0,
    val gameMode: GameMode = GameMode.CAMPAIGN,
    val currentRound: Int = 1,
    val roundTimer: Int = 99,
    val matchPhase: MatchPhase = MatchPhase.ANNOUNCING_ROUND,
    val announcementText: String = "ROUND 1",
    val isPaused: Boolean = false,
    val isMoveListOpen: Boolean = false,
    val isMatchResultOpen: Boolean = false,
    val isVictory: Boolean = false,
    val rewardCoinsEarned: Int = 0,
    val isSoundEnabled: Boolean = true,
    val survivalWave: Int = 1
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val soundManager = SoundManager(application)
    val combatEngine = CombatEngine(soundManager)
    // Fix 10: DataStore repository for persistent save & load
    private val repository = GameDataRepository(application)

    // Weapons Catalog
    val defaultWeapons = listOf(
        Weapon(
            id = "fists",
            name = "Iron Fists",
            hindiName = "लौह मुक्के",
            typeName = "Unarmed",
            baseDamage = 8f,
            speed = 1.25f,
            range = 0.85f,
            critChance = 0.12f,
            bladeColor = SoulCyan,
            price = 0,
            isUnlocked = true,
            description = "Fast hand-to-hand combat martial strikes."
        ),
        Weapon(
            id = "katana",
            name = "Blood Katana",
            hindiName = "रक्त कटाना",
            typeName = "Sword",
            baseDamage = 13f,
            speed = 1.05f,
            range = 1.2f,
            critChance = 0.20f,
            bladeColor = BloodCrimson,
            price = 250,
            isUnlocked = true,
            description = "Balanced razor-sharp curved shinobi katana."
        ),
        Weapon(
            id = "claws",
            name = "Lynx Claws",
            hindiName = "लिंक्स पंजे",
            typeName = "Tekko-Kagi",
            baseDamage = 15f,
            speed = 1.3f,
            range = 0.95f,
            critChance = 0.28f,
            bladeColor = SoulCyan,
            price = 500,
            isUnlocked = false,
            description = "Triple curved razor claws with high bleed & criticals."
        ),
        Weapon(
            id = "kusarigama",
            name = "Titan Kusarigama",
            hindiName = "कुसारीगामा",
            typeName = "Chain Scythe",
            baseDamage = 18f,
            speed = 0.95f,
            range = 1.45f,
            critChance = 0.22f,
            bladeColor = FlameAmber,
            price = 850,
            isUnlocked = false,
            description = "Deadly long-range weighted chain and scythe."
        ),
        Weapon(
            id = "naginata",
            name = "Golden Naginata",
            hindiName = "स्वर्ण नागीनाता",
            typeName = "Polearm",
            baseDamage = 22f,
            speed = 0.9f,
            range = 1.6f,
            critChance = 0.25f,
            bladeColor = GoldYellow,
            price = 1200,
            isUnlocked = false,
            description = "Master polearm weapon with maximum piercing reach."
        ),
        Weapon(
            id = "cleavers",
            name = "Demon Cleavers",
            hindiName = "दैत्य कुल्हाड़ी",
            typeName = "Heavy Blades",
            baseDamage = 26f,
            speed = 0.85f,
            range = 1.15f,
            critChance = 0.35f,
            bladeColor = Color(0xFFD500F9),
            price = 1800,
            isUnlocked = false,
            description = "Massive dual butcher cleavers with devastating impact."
        )
    )

    // Armors Catalog
    val defaultArmors = listOf(
        ArmorItem(
            id = "gi",
            name = "Shadow Gi",
            hindiName = "शैडो वस्त्र",
            isHelmet = false,
            defenseBonus = 0.05f,
            healthBonus = 0f,
            price = 0,
            isUnlocked = true,
            description = "Lightweight black linen shinobi fighting gi."
        ),
        ArmorItem(
            id = "ronin",
            name = "Ronin Plate",
            hindiName = "रोनिन कवच",
            isHelmet = false,
            defenseBonus = 0.15f,
            healthBonus = 25f,
            price = 280,
            isUnlocked = false,
            description = "Hardened lacquered wooden plates for samurai duels."
        ),
        ArmorItem(
            id = "demon_visor",
            name = "Demon Visor Mask",
            hindiName = "दानव मुखौटा",
            isHelmet = true,
            defenseBonus = 0.22f,
            healthBonus = 40f,
            price = 600,
            isUnlocked = false,
            description = "Cursed mask providing high defense and critical boost."
        ),
        ArmorItem(
            id = "dragon_armor",
            name = "Dragon Scale Armor",
            hindiName = "ड्रैगन कवच",
            isHelmet = false,
            defenseBonus = 0.35f,
            healthBonus = 70f,
            price = 1400,
            isUnlocked = false,
            description = "Imperial armor forged from ancient dragon scales."
        )
    )

    // Boss Profiles (Acts 1, 2, 3)
    val bosses = listOf(
        BossProfile(
            id = "kenji",
            name = "KENJI",
            title = "Rogue Bandit Leader",
            weapon = defaultWeapons[0],
            maxHealth = 100f,
            attackMult = 1.0f,
            defenseMult = 1.0f,
            aiAggressiveness = 0.55f,
            shadowMoveName = "Bandit Rush",
            eyeColor = Color(0xFFFF5252),
            ribbonColor = Color(0xFF9E9E9E),
            arena = ArenaType.CRIMSON_PAGODA,
            rewardCoins = 150,
            quote = "Let's see if your hands are as sharp as your mouth!"
        ),
        BossProfile(
            id = "kage",
            name = "KAGE",
            title = "Swift Katana Duelist",
            weapon = defaultWeapons[1],
            maxHealth = 120f,
            attackMult = 1.15f,
            defenseMult = 1.05f,
            aiAggressiveness = 0.7f,
            shadowMoveName = "Wind Slash",
            eyeColor = SoulCyan,
            ribbonColor = SoulCyan,
            arena = ArenaType.BAMBOO_GROVE,
            rewardCoins = 250,
            quote = "My blade dances like bamboo in the storm."
        ),
        BossProfile(
            id = "lynx",
            name = "LYNX",
            title = "ACT I • Demon Assassin Lord",
            weapon = defaultWeapons[2],
            maxHealth = 150f,
            attackMult = 1.35f,
            defenseMult = 1.15f,
            aiAggressiveness = 0.85f,
            shadowMoveName = "Invisibility Claws",
            eyeColor = Color(0xFFFF1744),
            ribbonColor = BloodCrimson,
            arena = ArenaType.GATES_OF_SHADOWS,
            rewardCoins = 500,
            quote = "You are in the order of assassins now. No one escapes my claws!"
        ),
        BossProfile(
            id = "shen",
            name = "MASTER SHEN",
            title = "Mountain Hermit Sage",
            weapon = defaultWeapons[4],
            maxHealth = 175f,
            attackMult = 1.45f,
            defenseMult = 1.25f,
            aiAggressiveness = 0.8f,
            shadowMoveName = "Lightning Naginata",
            eyeColor = GoldYellow,
            ribbonColor = FlameAmber,
            arena = ArenaType.CRIMSON_PAGODA,
            rewardCoins = 750,
            quote = "Patience and distance conquer all arrogance."
        ),
        BossProfile(
            id = "butcher",
            name = "BUTCHER",
            title = "ACT II • Blood Cleaver Demon",
            weapon = defaultWeapons[5],
            maxHealth = 210f,
            attackMult = 1.7f,
            defenseMult = 1.35f,
            aiAggressiveness = 0.9f,
            shadowMoveName = "Earthquake Cleaver",
            eyeColor = Color(0xFFFF3D00),
            ribbonColor = Color(0xFFD500F9),
            arena = ArenaType.BAMBOO_GROVE,
            rewardCoins = 1000,
            quote = "I will tenderize you into meat for my pack!"
        ),
        BossProfile(
            id = "shogun",
            name = "SHOGUN",
            title = "ACT III • Supreme Shadow Emperor",
            weapon = defaultWeapons[1],
            maxHealth = 260f,
            attackMult = 2.0f,
            defenseMult = 1.5f,
            aiAggressiveness = 0.98f,
            shadowMoveName = "Imperial Shadow Gate",
            eyeColor = Color(0xFFD500F9),
            ribbonColor = Color(0xFFFFD54F),
            arena = ArenaType.GATES_OF_SHADOWS,
            rewardCoins = 2000,
            quote = "Kneel before the gates of shadows! All warriors bow to me!"
        )
    )

    private val _weapons = MutableStateFlow(defaultWeapons)
    val weapons: StateFlow<List<Weapon>> = _weapons.asStateFlow()

    private val _armors = MutableStateFlow(defaultArmors)
    val armors: StateFlow<List<ArmorItem>> = _armors.asStateFlow()

    private val _equippedWeapon = MutableStateFlow(defaultWeapons[1])
    val equippedWeapon: StateFlow<Weapon> = _equippedWeapon.asStateFlow()

    private val _equippedArmor = MutableStateFlow(defaultArmors[0])
    val equippedArmor: StateFlow<ArmorItem> = _equippedArmor.asStateFlow()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    var lastArenaWidth: Float = 800f
    var currentScaleU: Float = 1.0f

    // Runtime Fighters in match: initialized at arenaWidth * 0.3f and 0.7f
    var playerFighter = Fighter(
        isPlayer = true,
        x = 240f,
        maxHealth = 100f,
        health = 100f,
        weapon = defaultWeapons[1]
    )

    var enemyFighter = Fighter(
        isPlayer = false,
        x = 560f,
        maxHealth = 100f,
        health = 100f,
        facingRight = false,
        weapon = defaultWeapons[0],
        eyeColor = Color(0xFFFF5252)
    )

    private var matchJob: Job? = null
    private var timerJob: Job? = null
    // Fix 9: Track the delay(1800) round end coroutine as a Job so it can be cleanly cancelled
    private var roundEndJob: Job? = null

    // Joystick input states
    private var joystickX: Float = 0f
    private var joystickY: Float = 0f

    init {
        // Fix 10: Load saved DataStore preferences in GameViewModel init
        viewModelScope.launch {
            val saved = repository.savedDataFlow.first()

            val loadedWeapons = defaultWeapons.map { w ->
                val isUnlocked = saved.unlockedWeaponIds.contains(w.id) || w.id == "fists" || w.id == "katana"
                val level = saved.weaponLevels[w.id] ?: 1
                val bonusDmg = (level - 1) * 2.5f
                val bonusCrit = ((level - 1) * 0.02f).coerceAtMost(0.3f)
                w.copy(
                    isUnlocked = isUnlocked,
                    level = level,
                    baseDamage = w.baseDamage + bonusDmg,
                    critChance = (w.critChance + bonusCrit).coerceAtMost(0.5f)
                )
            }
            _weapons.value = loadedWeapons

            val loadedArmors = defaultArmors.map { a ->
                val isUnlocked = saved.unlockedArmorIds.contains(a.id) || a.id == "gi"
                val level = saved.armorLevels[a.id] ?: 1
                val bonusDef = ((level - 1) * 0.04f).coerceAtMost(0.4f)
                val bonusHp = (level - 1) * 20f
                a.copy(
                    isUnlocked = isUnlocked,
                    level = level,
                    defenseBonus = (a.defenseBonus + bonusDef).coerceAtMost(0.6f),
                    healthBonus = a.healthBonus + bonusHp
                )
            }
            _armors.value = loadedArmors

            val eqWeapon = loadedWeapons.find { it.id == saved.equippedWeaponId } ?: loadedWeapons[1]
            val eqArmor = loadedArmors.find { it.id == saved.equippedArmorId } ?: loadedArmors[0]
            _equippedWeapon.value = eqWeapon
            _equippedArmor.value = eqArmor
            playerFighter.weapon = eqWeapon

            _uiState.update {
                it.copy(
                    coins = saved.coins,
                    currentBossIndex = saved.currentBossIndex
                )
            }
        }
    }

    // Fix 10: Helper to persist state to DataStore
    private fun persistData() {
        viewModelScope.launch {
            val currentCoins = _uiState.value.coins
            val currentBoss = _uiState.value.currentBossIndex
            val eqW = _equippedWeapon.value.id
            val eqA = _equippedArmor.value.id
            val unlockedW = _weapons.value.filter { it.isUnlocked }.map { it.id }.toSet()
            val unlockedA = _armors.value.filter { it.isUnlocked }.map { it.id }.toSet()
            val wLevels = _weapons.value.associate { it.id to it.level }
            val aLevels = _armors.value.associate { it.id to it.level }

            repository.saveGameData(
                coins = currentCoins,
                currentBossIndex = currentBoss,
                equippedWeaponId = eqW,
                equippedArmorId = eqA,
                unlockedWeaponIds = unlockedW,
                weaponLevels = wLevels,
                unlockedArmorIds = unlockedA,
                armorLevels = aLevels
            )
        }
    }

    // Fix 1: Spawn player at arenaWidth*0.3 and enemy at arenaWidth*0.7
    fun startBattle(bossIndex: Int, mode: GameMode) {
        val selectedBoss = bosses.getOrElse(bossIndex) { bosses[0] }
        val maxHp = 100f + _equippedArmor.value.healthBonus

        val spawnPlayerX = lastArenaWidth * 0.3f
        val spawnEnemyX = lastArenaWidth * 0.7f

        playerFighter = Fighter(
            isPlayer = true,
            x = spawnPlayerX,
            maxHealth = maxHp,
            health = maxHp,
            weapon = _equippedWeapon.value,
            roundsWon = 0
        )

        // Fix 8: In training mode dummy has 9999f HP and cannot die
        val enemyHp = if (mode == GameMode.TRAINING) 9999f else selectedBoss.maxHealth
        enemyFighter = Fighter(
            isPlayer = false,
            x = spawnEnemyX,
            maxHealth = enemyHp,
            health = enemyHp,
            facingRight = false,
            weapon = selectedBoss.weapon,
            eyeColor = selectedBoss.eyeColor,
            ribbonColor = selectedBoss.ribbonColor,
            roundsWon = 0
        )

        // Fix 9: Reset projectiles, shadow energy, shadow mode, and combo counters at match start
        resetRoundState()

        _uiState.update {
            it.copy(
                currentScreen = ScreenState.COMBAT,
                selectedBossIndex = bossIndex,
                gameMode = mode,
                currentRound = 1,
                roundTimer = 99,
                survivalWave = 1,
                matchPhase = MatchPhase.ANNOUNCING_ROUND,
                announcementText = when (mode) {
                    GameMode.TRAINING -> "TRAINING DOJO"
                    GameMode.SURVIVAL -> "WAVE 1"
                    else -> "ROUND 1"
                },
                isPaused = false,
                isMatchResultOpen = false
            )
        }

        launchRoundSequence()
    }

    // Fix 9: Reset projectiles, shadowEnergy, isShadowMode and comboCount at start of each new round
    private fun resetRoundState() {
        combatEngine.projectiles.clear()
        combatEngine.particles.clear()
        combatEngine.floatingTexts.clear()
        playerFighter.shadowEnergy = 0f
        playerFighter.isShadowMode = false
        playerFighter.shadowModeTimer = 0f
        playerFighter.comboCount = 0
        playerFighter.comboTimer = 0f
        enemyFighter.shadowEnergy = 0f
        enemyFighter.isShadowMode = false
        enemyFighter.shadowModeTimer = 0f
        enemyFighter.comboCount = 0
        enemyFighter.comboTimer = 0f
    }

    private fun launchRoundSequence() {
        matchJob?.cancel()
        timerJob?.cancel()
        roundEndJob?.cancel()

        matchJob = viewModelScope.launch {
            soundManager.playGong()
            delay(1200)
            _uiState.update { it.copy(announcementText = "FIGHT!") }
            delay(700)
            _uiState.update {
                it.copy(
                    announcementText = "",
                    matchPhase = MatchPhase.FIGHTING
                )
            }
            startTimerCountdown()
        }
    }

    private fun startTimerCountdown() {
        timerJob?.cancel()
        // Fix 8: In training mode timer is infinite and does not count down
        if (_uiState.value.gameMode == GameMode.TRAINING) return

        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.matchPhase == MatchPhase.FIGHTING) {
                delay(1000)
                if (!_uiState.value.isPaused) {
                    val currentTimer = _uiState.value.roundTimer
                    if (currentTimer > 1) {
                        _uiState.update { it.copy(roundTimer = currentTimer - 1) }
                    } else {
                        // Time over: round winner decided by higher health
                        handleRoundEnd(timeOut = true)
                        break
                    }
                }
            }
        }
    }

    // Fix 1: Accept arenaHeight and apply world scale u = arenaHeight / 400f
    fun onGameTick(dt: Float, arenaWidth: Float, groundY: Float, arenaHeight: Float = 400f) {
        if (_uiState.value.currentScreen != ScreenState.COMBAT || _uiState.value.isPaused) return

        lastArenaWidth = arenaWidth
        val u = (arenaHeight / 400f).coerceIn(0.5f, 3.5f)
        currentScaleU = u

        val currentBoss = if (_uiState.value.gameMode == GameMode.SURVIVAL) {
            val wave = _uiState.value.survivalWave
            val baseBoss = bosses[(wave - 1) % bosses.size]
            val scale = 1f + (wave - 1) * 0.15f
            baseBoss.copy(attackMult = baseBoss.attackMult * scale, defenseMult = baseBoss.defenseMult * scale)
        } else {
            bosses.getOrNull(_uiState.value.selectedBossIndex)
        }

        // Pass equipped armor and world scale u into combat engine
        combatEngine.update(playerFighter, enemyFighter, currentBoss, _equippedArmor.value, dt, arenaWidth, groundY, u)

        // Fix 8: In training mode, keep dummy alive by resetting health
        if (_uiState.value.gameMode == GameMode.TRAINING) {
            if (enemyFighter.health < enemyFighter.maxHealth * 0.5f) {
                enemyFighter.health = enemyFighter.maxHealth
            }
        }

        // Continuous joystick movement check when in neutral states
        if (_uiState.value.matchPhase == MatchPhase.FIGHTING) {
            handleJoystickMovement(u)

            // Fix 1: Removed KNOCKDOWN / DEFEAT state conditions so KO fires as soon as health hits 0
            if (_uiState.value.gameMode != GameMode.TRAINING) {
                if (enemyFighter.health <= 0f) {
                    handleRoundEnd(playerWon = true)
                } else if (playerFighter.health <= 0f) {
                    handleRoundEnd(playerWon = false)
                }
            }
        }
    }

    private fun handleJoystickMovement(u: Float = currentScaleU) {
        val isGrounded = playerFighter.y == 0f

        // Fix 2: When joystick is neutral, set player to IDLE if in walk or crouch
        val isNeutral = abs(joystickX) <= 0.25f && abs(joystickY) <= 0.55f
        if (isNeutral) {
            if (playerFighter.actionState == ActionState.WALK_FORWARD ||
                playerFighter.actionState == ActionState.WALK_BACKWARD ||
                playerFighter.actionState == ActionState.CROUCH
            ) {
                combatEngine.setAction(playerFighter, ActionState.IDLE, 0.4f)
            }
            return
        }

        val canMove = playerFighter.actionState == ActionState.IDLE ||
                playerFighter.actionState == ActionState.WALK_FORWARD ||
                playerFighter.actionState == ActionState.WALK_BACKWARD ||
                playerFighter.actionState == ActionState.CROUCH

        if (!canMove) return

        // Fix 5: Map jump, roll, and walk by joystick X sign (screen direction), not facing direction
        when {
            // Jump (Up)
            joystickY > 0.55f && isGrounded -> {
                val jumpState = when {
                    joystickX > 0.35f -> if (playerFighter.facingRight) ActionState.JUMP_FORWARD else ActionState.JUMP_BACKWARD
                    joystickX < -0.35f -> if (playerFighter.facingRight) ActionState.JUMP_BACKWARD else ActionState.JUMP_FORWARD
                    else -> ActionState.JUMP_UP
                }
                combatEngine.jumpFighter(playerFighter, jumpState, u)
            }
            // Crouch / Roll (Down)
            joystickY < -0.55f && isGrounded -> {
                when {
                    joystickX > 0.35f -> {
                        val rollState = if (playerFighter.facingRight) ActionState.ROLL_FORWARD else ActionState.ROLL_BACKWARD
                        combatEngine.setAction(playerFighter, rollState, 0.45f)
                    }
                    joystickX < -0.35f -> {
                        val rollState = if (playerFighter.facingRight) ActionState.ROLL_BACKWARD else ActionState.ROLL_FORWARD
                        combatEngine.setAction(playerFighter, rollState, 0.45f)
                    }
                    else -> {
                        // Fix 2: Only call setAction for crouch if not already crouching
                        if (playerFighter.actionState != ActionState.CROUCH) {
                            combatEngine.setAction(playerFighter, ActionState.CROUCH, 0.4f)
                        }
                    }
                }
            }
            // Walk (Horizontal)
            isGrounded -> {
                val targetWalk = when {
                    joystickX > 0.25f -> if (playerFighter.facingRight) ActionState.WALK_FORWARD else ActionState.WALK_BACKWARD
                    joystickX < -0.25f -> if (playerFighter.facingRight) ActionState.WALK_BACKWARD else ActionState.WALK_FORWARD
                    else -> ActionState.IDLE
                }
                // Fix 2: Only call setAction for walk if not already in that walk state to avoid freezing animation
                if (targetWalk != ActionState.IDLE && playerFighter.actionState != targetWalk) {
                    combatEngine.setAction(playerFighter, targetWalk, 0.4f)
                }
            }
        }
    }

    private fun handleRoundEnd(playerWon: Boolean = true, timeOut: Boolean = false) {
        if (_uiState.value.matchPhase != MatchPhase.FIGHTING) return

        timerJob?.cancel()

        val won = if (timeOut) playerFighter.health >= enemyFighter.health else playerWon
        if (won) {
            playerFighter.roundsWon++
            combatEngine.setAction(playerFighter, ActionState.VICTORY, 1.5f)
            combatEngine.setAction(enemyFighter, ActionState.DEFEAT, 1.5f)
        } else {
            enemyFighter.roundsWon++
            combatEngine.setAction(enemyFighter, ActionState.VICTORY, 1.5f)
            combatEngine.setAction(playerFighter, ActionState.DEFEAT, 1.5f)
        }

        // Fix 8: Survival mode uses single-round elimination per wave; Campaign is best of 3
        val matchDecided = when (_uiState.value.gameMode) {
            GameMode.SURVIVAL -> true
            GameMode.TRAINING -> false
            else -> playerFighter.roundsWon >= 2 || enemyFighter.roundsWon >= 2
        }

        _uiState.update {
            it.copy(
                matchPhase = if (matchDecided) MatchPhase.MATCH_OVER else MatchPhase.ROUND_ENDED,
                announcementText = if (won) "ROUND WON!" else "ROUND LOST!"
            )
        }

        // Fix 9: Track the delay(1800) coroutine in handleRoundEnd as a Job and cancel it in quitToDojo
        roundEndJob?.cancel()
        roundEndJob = viewModelScope.launch {
            delay(1800)
            if (!isActive) return@launch

            val mode = _uiState.value.gameMode

            // Fix 8: Survival mode progression
            if (mode == GameMode.SURVIVAL) {
                if (won) {
                    val wave = _uiState.value.survivalWave
                    val waveCoins = 80 + wave * 30
                    soundManager.playVictory()

                    // Spawn next boss with scaled stats and survivalWave+1
                    val nextWave = wave + 1
                    val baseBoss = bosses[(nextWave - 1) % bosses.size]
                    val scale = 1f + (nextWave - 1) * 0.18f
                    val scaledHp = baseBoss.maxHealth * scale

                    // Fix 1: Spawn player at arenaWidth*0.3 and enemy at arenaWidth*0.7
                    enemyFighter = Fighter(
                        isPlayer = false,
                        x = lastArenaWidth * 0.7f,
                        maxHealth = scaledHp,
                        health = scaledHp,
                        facingRight = false,
                        weapon = baseBoss.weapon,
                        eyeColor = baseBoss.eyeColor,
                        ribbonColor = baseBoss.ribbonColor,
                        roundsWon = 0
                    )

                    // Partially heal player as survival reward (+35% HP)
                    playerFighter.health = (playerFighter.health + playerFighter.maxHealth * 0.35f).coerceAtMost(playerFighter.maxHealth)
                    playerFighter.x = lastArenaWidth * 0.3f
                    playerFighter.facingRight = true
                    playerFighter.roundsWon = 0
                    combatEngine.setAction(playerFighter, ActionState.IDLE, 0.4f)
                    combatEngine.setAction(enemyFighter, ActionState.IDLE, 0.4f)

                    // Fix 9: Reset round state for next survival wave
                    resetRoundState()

                    _uiState.update {
                        it.copy(
                            coins = it.coins + waveCoins,
                            survivalWave = nextWave,
                            roundTimer = 99,
                            matchPhase = MatchPhase.ANNOUNCING_ROUND,
                            announcementText = "WAVE $nextWave"
                        )
                    }
                    persistData()
                    launchRoundSequence()
                } else {
                    // Survival Run Lost: end the run
                    val earned = _uiState.value.survivalWave * 50
                    _uiState.update {
                        it.copy(
                            coins = it.coins + earned,
                            isMatchResultOpen = true,
                            isVictory = false,
                            rewardCoinsEarned = earned
                        )
                    }
                    persistData()
                }
                return@launch
            }

            // Campaign Mode match resolution
            if (matchDecided) {
                val isMatchVictory = playerFighter.roundsWon >= 2
                val currentBoss = bosses.getOrNull(_uiState.value.selectedBossIndex)
                // Fix 8: In training mode give 0 coins
                val earnedCoins = if (mode == GameMode.TRAINING) {
                    0
                } else if (isMatchVictory) {
                    currentBoss?.rewardCoins ?: 200
                } else {
                    40
                }

                if (isMatchVictory) {
                    soundManager.playVictory()
                }

                _uiState.update {
                    val newBossIndex = if (isMatchVictory && it.selectedBossIndex == it.currentBossIndex && it.currentBossIndex < bosses.size - 1) {
                        it.currentBossIndex + 1
                    } else {
                        it.currentBossIndex
                    }
                    it.copy(
                        coins = it.coins + earnedCoins,
                        currentBossIndex = newBossIndex,
                        isMatchResultOpen = true,
                        isVictory = isMatchVictory,
                        rewardCoinsEarned = earnedCoins
                    )
                }
                // Fix 10: Persist progress
                persistData()
            } else {
                // Next Round
                val nextRound = _uiState.value.currentRound + 1
                playerFighter.health = playerFighter.maxHealth
                enemyFighter.health = enemyFighter.maxHealth
                // Fix 1: Spawn player at arenaWidth*0.3 and enemy at arenaWidth*0.7 on round reset
                playerFighter.x = lastArenaWidth * 0.3f
                enemyFighter.x = lastArenaWidth * 0.7f
                playerFighter.facingRight = true
                enemyFighter.facingRight = false
                combatEngine.setAction(playerFighter, ActionState.IDLE, 0.4f)
                combatEngine.setAction(enemyFighter, ActionState.IDLE, 0.4f)

                // Fix 9: Reset projectiles, shadowEnergy, isShadowMode and comboCount at start of new round
                resetRoundState()

                _uiState.update {
                    it.copy(
                        currentRound = nextRound,
                        roundTimer = 99,
                        matchPhase = MatchPhase.ANNOUNCING_ROUND,
                        announcementText = "ROUND $nextRound"
                    )
                }
                launchRoundSequence()
            }
        }
    }

    // --- Action Inputs from Controller ---

    fun onJoystickChange(x: Float, y: Float) {
        joystickX = x
        joystickY = y
    }

    fun onPunchClick() {
        if (canExecuteAction()) {
            if (joystickY > 0.35f) {
                // Rising Uppercut
                combatEngine.setAction(playerFighter, ActionState.PUNCH_UPPERCUT, 0.48f)
            } else if (playerFighter.actionState == ActionState.PUNCH_JAB) {
                // Fix 7: PUNCH_HOOK triggered during combo window of PUNCH_JAB
                combatEngine.setAction(playerFighter, ActionState.PUNCH_HOOK, 0.42f)
            } else {
                // Standard Jab
                combatEngine.setAction(playerFighter, ActionState.PUNCH_JAB, 0.35f)
            }
        }
    }

    fun onKickClick() {
        if (canExecuteAction()) {
            if (joystickY < -0.35f) {
                // Low Sweep Kick
                combatEngine.setAction(playerFighter, ActionState.KICK_LOW, 0.44f)
            } else if (joystickX > 0.35f || joystickX < -0.35f) {
                // Fix 3 & 5: Flying Jump Kick with vertical physics and world scale
                playerFighter.vy = 360f * currentScaleU
                combatEngine.setAction(playerFighter, ActionState.KICK_FLYING, 0.55f)
            } else {
                // Roundhouse Kick
                combatEngine.setAction(playerFighter, ActionState.KICK_ROUNDHOUSE, 0.48f)
            }
        }
    }

    fun onWeaponClick() {
        if (canExecuteAction()) {
            if (joystickX > 0.35f || joystickX < -0.35f) {
                // Spinning Slash
                combatEngine.setAction(playerFighter, ActionState.WEAPON_SPIN, 0.65f)
            } else if (joystickY > 0.35f) {
                // Piercing Thrust
                combatEngine.setAction(playerFighter, ActionState.WEAPON_PIERCE, 0.55f)
            } else {
                // Standard Weapon Slash
                combatEngine.setAction(playerFighter, ActionState.WEAPON_SLASH, 0.5f)
            }
        }
    }

    fun onShurikenClick() {
        if (canExecuteAction()) {
            combatEngine.setAction(playerFighter, ActionState.RANGED_THROW, 0.45f)
            combatEngine.spawnShuriken(playerFighter, currentScaleU)
        }
    }

    fun onShadowBlastClick() {
        if (playerFighter.shadowEnergy >= 100f && canExecuteAction()) {
            playerFighter.shadowEnergy = 0f
            playerFighter.isShadowMode = true
            playerFighter.shadowModeTimer = 8f
            combatEngine.setAction(playerFighter, ActionState.SHADOW_SURGE, 0.75f)
            combatEngine.screenShake = 16f
            soundManager.playShadow()
        }
    }

    // Fix 7: Allow follow-up attack during the last 40% of a jab (actionFrame >= 0.60f)
    private fun canExecuteAction(): Boolean {
        if (_uiState.value.matchPhase != MatchPhase.FIGHTING || _uiState.value.isPaused) return false
        val state = playerFighter.actionState
        val isJabComboWindow = state == ActionState.PUNCH_JAB && playerFighter.actionFrame >= 0.60f
        return state == ActionState.IDLE || state == ActionState.WALK_FORWARD ||
                state == ActionState.WALK_BACKWARD || state == ActionState.CROUCH ||
                isJabComboWindow
    }

    // --- Shop & Armory Actions ---

    fun equipWeapon(weapon: Weapon) {
        _equippedWeapon.value = weapon
        playerFighter.weapon = weapon
        persistData()
    }

    fun buyWeapon(weapon: Weapon) {
        if (_uiState.value.coins >= weapon.price) {
            _uiState.update { it.copy(coins = it.coins - weapon.price) }
            _weapons.update { list ->
                list.map { if (it.id == weapon.id) it.copy(isUnlocked = true) else it }
            }
            equipWeapon(weapon.copy(isUnlocked = true))
            persistData()
        }
    }

    fun upgradeWeapon(weapon: Weapon) {
        val cost = weapon.level * 150
        if (_uiState.value.coins >= cost) {
            _uiState.update { it.copy(coins = it.coins - cost) }
            _weapons.update { list ->
                list.map {
                    if (it.id == weapon.id) {
                        it.copy(
                            level = it.level + 1,
                            baseDamage = it.baseDamage + 2.5f,
                            critChance = (it.critChance + 0.02f).coerceAtMost(0.5f)
                        )
                    } else it
                }
            }
            if (_equippedWeapon.value.id == weapon.id) {
                _equippedWeapon.update {
                    it.copy(
                        level = it.level + 1,
                        baseDamage = it.baseDamage + 2.5f,
                        critChance = (it.critChance + 0.02f).coerceAtMost(0.5f)
                    )
                }
                playerFighter.weapon = _equippedWeapon.value
            }
            persistData()
        }
    }

    fun equipArmor(armor: ArmorItem) {
        _equippedArmor.value = armor
        persistData()
    }

    fun buyArmor(armor: ArmorItem) {
        if (_uiState.value.coins >= armor.price) {
            _uiState.update { it.copy(coins = it.coins - armor.price) }
            _armors.update { list ->
                list.map { if (it.id == armor.id) it.copy(isUnlocked = true) else it }
            }
            equipArmor(armor.copy(isUnlocked = true))
            persistData()
        }
    }

    fun upgradeArmor(armor: ArmorItem) {
        val cost = armor.level * 120
        if (_uiState.value.coins >= cost) {
            _uiState.update { it.copy(coins = it.coins - cost) }
            _armors.update { list ->
                list.map {
                    if (it.id == armor.id) {
                        it.copy(
                            level = it.level + 1,
                            defenseBonus = (it.defenseBonus + 0.04f).coerceAtMost(0.6f),
                            healthBonus = it.healthBonus + 20f
                        )
                    } else it
                }
            }
            if (_equippedArmor.value.id == armor.id) {
                _equippedArmor.update {
                    it.copy(
                        level = it.level + 1,
                        defenseBonus = (it.defenseBonus + 0.04f).coerceAtMost(0.6f),
                        healthBonus = it.healthBonus + 20f
                    )
                }
            }
            persistData()
        }
    }

    fun toggleSound() {
        val newState = !_uiState.value.isSoundEnabled
        _uiState.update { it.copy(isSoundEnabled = newState) }
        soundManager.isSoundEnabled = newState
        soundManager.isHapticsEnabled = newState
    }

    fun pauseBattle() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeBattle() {
        _uiState.update { it.copy(isPaused = false) }
    }

    // Fix 9: Auto-pause when app goes to background (lifecycle ON_PAUSE)
    fun onAppPause() {
        if (_uiState.value.currentScreen == ScreenState.COMBAT && !_uiState.value.isPaused) {
            pauseBattle()
        }
    }

    // Fix 9: Cancel tracked roundEndJob in quitToDojo
    fun quitToDojo() {
        matchJob?.cancel()
        timerJob?.cancel()
        roundEndJob?.cancel()
        _uiState.update {
            it.copy(
                currentScreen = ScreenState.DOJO,
                isPaused = false,
                isMatchResultOpen = false
            )
        }
    }

    fun toggleMoveList(show: Boolean) {
        _uiState.update { it.copy(isMoveListOpen = show) }
    }

    // Fix 5: Release SoundPool in ViewModel onCleared
    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
