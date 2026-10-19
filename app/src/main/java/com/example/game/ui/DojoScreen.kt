package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.ArmorItem
import com.example.game.model.BossProfile
import com.example.game.model.GameMode
import com.example.game.model.Weapon
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.FlameAmber
import com.example.ui.theme.GoldYellow
import com.example.ui.theme.ShadowDarkBg
import com.example.ui.theme.ShadowSurface
import com.example.ui.theme.ShadowSurfaceVariant
import com.example.ui.theme.SoulCyan

@Composable
fun DojoScreen(
    coins: Int,
    currentBossIndex: Int,
    bosses: List<BossProfile>,
    weapons: List<Weapon>,
    armors: List<ArmorItem>,
    equippedWeapon: Weapon,
    equippedArmor: ArmorItem,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onStartBattle: (bossIndex: Int, mode: GameMode) -> Unit,
    onEquipWeapon: (Weapon) -> Unit,
    onUpgradeWeapon: (Weapon) -> Unit,
    onBuyWeapon: (Weapon) -> Unit,
    onEquipArmor: (ArmorItem) -> Unit,
    onBuyArmor: (ArmorItem) -> Unit,
    onUpgradeArmor: (ArmorItem) -> Unit,
    onShowMoveList: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Battle/Campaign, 1: Weapons, 2: Armor, 3: Modes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF140D1E),
                        Color(0xFF0A0610),
                        Color(0xFF050308)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // --- Top Header Bar ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SHADOW COMBAT",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = BloodCrimson,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "SHINOBI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Currency & Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Coins Pill
                    Surface(
                        color = Color(0xFF241A33),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldYellow)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = GoldYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$coins",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldYellow
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Move List Info Button
                    IconButton(
                        onClick = onShowMoveList,
                        modifier = Modifier.testTag("btn_moves_guide")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Move List",
                            tint = SoulCyan
                        )
                    }

                    // Sound Toggle
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier.testTag("btn_toggle_sound")
                    ) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound",
                            tint = Color.White
                        )
                    }
                }
            }

            // --- Navigation Tabs ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = SoulCyan,
                divider = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("TOURNAMENT", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("WEAPONS", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("ARMOR", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("MODES", fontWeight = FontWeight.Bold) }
                )
            }

            // --- Content Views ---
            when (selectedTab) {
                0 -> BossCampaignView(
                    bosses = bosses,
                    currentBossIndex = currentBossIndex,
                    onFightBoss = { index -> onStartBattle(index, GameMode.CAMPAIGN) }
                )
                1 -> WeaponShopView(
                    weapons = weapons,
                    equippedWeapon = equippedWeapon,
                    coins = coins,
                    onEquip = onEquipWeapon,
                    onBuy = onBuyWeapon,
                    onUpgrade = onUpgradeWeapon
                )
                2 -> ArmorShopView(
                    armors = armors,
                    equippedArmor = equippedArmor,
                    coins = coins,
                    onEquip = onEquipArmor,
                    onBuy = onBuyArmor,
                    onUpgrade = onUpgradeArmor
                )
                3 -> ExtraModesView(
                    onStartSurvival = { onStartBattle(0, GameMode.SURVIVAL) },
                    onStartTraining = { onStartBattle(0, GameMode.TRAINING) },
                    onShowMoveList = onShowMoveList
                )
            }
        }
    }
}

// --- Tab 0: Boss / Act Tournament Campaign ---
@Composable
private fun BossCampaignView(
    bosses: List<BossProfile>,
    currentBossIndex: Int,
    onFightBoss: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(bosses.size) { index ->
            val boss = bosses[index]
            val isUnlocked = index <= currentBossIndex
            val isDefeated = index < currentBossIndex
            val isCurrent = index == currentBossIndex

            BossDuelCard(
                boss = boss,
                stageNumber = index + 1,
                isUnlocked = isUnlocked,
                isDefeated = isDefeated,
                isCurrent = isCurrent,
                onFight = { onFightBoss(index) }
            )
        }
    }
}

@Composable
private fun BossDuelCard(
    boss: BossProfile,
    stageNumber: Int,
    isUnlocked: Boolean,
    isDefeated: Boolean,
    isCurrent: Boolean,
    onFight: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = if (isCurrent) FlameAmber else if (isUnlocked) Color(0xFF4A3464) else Color(0xFF281E36),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFF201330) else Color(0xFF140D22)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stage Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isDefeated) Color(0xFF2E7D32) else if (isCurrent) BloodCrimson else Color(0xFF382A4D),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isDefeated) "DEFEATED" else "STAGE $stageNumber",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "${boss.rewardCoins} 🪙",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldYellow
                )
            }

            // Boss Silhouette Emblem
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0C0714))
                    .border(2.dp, boss.eyeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⚔️",
                            fontSize = 32.sp
                        )
                        // Glowing Eyes Indicator
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(6.dp, 3.dp).background(boss.eyeColor))
                            Box(modifier = Modifier.size(6.dp, 3.dp).background(boss.eyeColor))
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF6B5885),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Name & Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = boss.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = boss.title,
                    fontSize = 11.sp,
                    color = FlameAmber,
                    maxLines = 1
                )
                Text(
                    text = "\"${boss.quote}\"",
                    fontSize = 10.sp,
                    color = Color(0xFFA59EB3),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Fight Button
            Button(
                onClick = onFight,
                enabled = isUnlocked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrent) BloodCrimson else Color(0xFF4A3464),
                    disabledContainerColor = Color(0xFF1F172B)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_fight_stage_$stageNumber")
            ) {
                Text(
                    text = if (isDefeated) "REMATCH" else if (isUnlocked) "FIGHT" else "LOCKED",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// --- Tab 1: Weapon Shop ---
@Composable
private fun WeaponShopView(
    weapons: List<Weapon>,
    equippedWeapon: Weapon,
    coins: Int,
    onEquip: (Weapon) -> Unit,
    onBuy: (Weapon) -> Unit,
    onUpgrade: (Weapon) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(weapons) { weapon ->
            val isEquipped = weapon.id == equippedWeapon.id
            val upgradeCost = weapon.level * 150

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isEquipped) Color(0xFF241535) else Color(0xFF140D22)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isEquipped) SoulCyan else Color(0xFF3B2A52)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Weapon Info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = weapon.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF38234D),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LVL ${weapon.level}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoulCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "${weapon.hindiName} • ${weapon.typeName}",
                            fontSize = 11.sp,
                            color = FlameAmber
                        )
                        Text(
                            text = weapon.description,
                            fontSize = 11.sp,
                            color = Color(0xFFA59EB3)
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("⚔️ DMG: ${(weapon.baseDamage * 10).toInt()}", fontSize = 11.sp, color = Color.White)
                            Text("⚡ SPD: ${weapon.speed}x", fontSize = 11.sp, color = Color.White)
                            Text("💥 CRIT: ${(weapon.critChance * 100).toInt()}%", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    // Action Buttons
                    Column(horizontalAlignment = Alignment.End) {
                        if (weapon.isUnlocked) {
                            if (isEquipped) {
                                Surface(
                                    color = Color(0xFF1E3A2F),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = SoulCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("EQUIPPED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SoulCyan)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onEquip(weapon) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("EQUIP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Upgrade button
                            Button(
                                onClick = { onUpgrade(weapon) },
                                enabled = coins >= upgradeCost,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38234D)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("UPGRADE ($upgradeCost 🪙)", fontSize = 10.sp, color = GoldYellow)
                            }
                        } else {
                            Button(
                                onClick = { onBuy(weapon) },
                                enabled = coins >= weapon.price,
                                colors = ButtonDefaults.buttonColors(containerColor = FlameAmber),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("BUY ${weapon.price} 🪙", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 2: Armor Shop ---
@Composable
private fun ArmorShopView(
    armors: List<ArmorItem>,
    equippedArmor: ArmorItem,
    coins: Int,
    onEquip: (ArmorItem) -> Unit,
    onBuy: (ArmorItem) -> Unit,
    onUpgrade: (ArmorItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(armors) { armor ->
            val isEquipped = armor.id == equippedArmor.id
            val upgradeCost = armor.level * 120

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isEquipped) Color(0xFF241535) else Color(0xFF140D22)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isEquipped) SoulCyan else Color(0xFF3B2A52)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = armor.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF38234D),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LVL ${armor.level}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoulCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = armor.hindiName,
                            fontSize = 11.sp,
                            color = FlameAmber
                        )
                        Text(
                            text = armor.description,
                            fontSize = 11.sp,
                            color = Color(0xFFA59EB3)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🛡️ DEF: ${(armor.defenseBonus * 100).toInt()}%", fontSize = 11.sp, color = Color.White)
                            Text("❤️ HP: +${armor.healthBonus.toInt()}", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (armor.isUnlocked) {
                            if (isEquipped) {
                                Surface(
                                    color = Color(0xFF1E3A2F),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = SoulCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("EQUIPPED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SoulCyan)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onEquip(armor) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("EQUIP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = { onUpgrade(armor) },
                                enabled = coins >= upgradeCost,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38234D)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("UPGRADE ($upgradeCost 🪙)", fontSize = 10.sp, color = GoldYellow)
                            }
                        } else {
                            Button(
                                onClick = { onBuy(armor) },
                                enabled = coins >= armor.price,
                                colors = ButtonDefaults.buttonColors(containerColor = FlameAmber),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("BUY ${armor.price} 🪙", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 3: Extra Modes (Survival & Training Dummy) ---
@Composable
private fun ExtraModesView(
    onStartSurvival: () -> Unit,
    onStartTraining: () -> Unit,
    onShowMoveList: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Survival Mode Card
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1128)),
            border = androidx.compose.foundation.BorderStroke(1.dp, FlameAmber)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SURVIVAL MODE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = FlameAmber
                )
                Text(
                    text = "Endless ninja waves! Defeat consecutive shadow fighters to earn massive gold and test your martial skill.",
                    fontSize = 12.sp,
                    color = Color(0xFFA59EB3),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onStartSurvival,
                    colors = ButtonDefaults.buttonColors(containerColor = FlameAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_mode_survival")
                ) {
                    Text("START SURVIVAL", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        // Training Dojo Card
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141926)),
            border = androidx.compose.foundation.BorderStroke(1.dp, SoulCyan)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TRAINING DOJO",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = SoulCyan
                )
                Text(
                    text = "Spar against an invulnerable wooden dummy. Test your weapon ranges, practice uppercuts, kicks, and combos.",
                    fontSize = 12.sp,
                    color = Color(0xFFA59EB3),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onStartTraining,
                    colors = ButtonDefaults.buttonColors(containerColor = SoulCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_mode_training")
                ) {
                    Text("ENTER DOJO", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}
