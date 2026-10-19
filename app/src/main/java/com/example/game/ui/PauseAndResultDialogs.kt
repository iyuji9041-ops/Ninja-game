package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.FlameAmber
import com.example.ui.theme.GoldYellow
import com.example.ui.theme.SoulCyan

data class MoveGuideItem(
    val title: String,
    val hindiTitle: String,
    val command: String,
    val description: String
)

val MOVE_GUIDE_LIST = listOf(
    MoveGuideItem("Direct Strike", "सीधा मुक्का", "🥊 Punch Button", "Quick jab straight to the opponent's face."),
    MoveGuideItem("Uppercut Head Hit", "ऊपर का जोरदार प्रहार", "Joystick Up + 🥊 Punch", "Powerful rising punch that launches opponent backward."),
    MoveGuideItem("Weapon Slash", "तलवार / हथियार का वार", "⚔️ Slash Button", "Heavy weapon strike with distinct range & damage."),
    MoveGuideItem("Spinning Double Slash", "घूमती तलवार", "Joystick Forward + ⚔️ Slash", "Devastating double spinning slash combo."),
    MoveGuideItem("Low Leg Sweep", "नीचे की स्वीप किक", "Joystick Down + 🥋 Kick", "Trips opponent low, bypassing high guard."),
    MoveGuideItem("Roundhouse High Kick", "हाई राउंडहाउस किक", "🥋 Kick Button", "Fast overhead martial arts roundhouse strike."),
    MoveGuideItem("Flying Jump Kick", "हवा में जंप किक", "Joystick Forward + 🥋 Kick", "Acrobatic leap across screen into enemy chest."),
    MoveGuideItem("Kunai / Shuriken", "शुरिकेन फेंकना", "🌟 Kunai Button", "Deadly ranged ninja projectile."),
    MoveGuideItem("Shadow Surge / Ultimate", "शैडो शक्ति विस्फोट", "🔮 Shadow Button (Full Meter)", "Unleashes the dark demon realm shockwave!"),
    MoveGuideItem("Guard / Block", "रक्षा (ब्लॉक)", "Walk Backward (Away)", "Reduces damage by 80% and produces metallic parry sparks.")
)

// --- Moves & Combo Guide Dialog ---
@Composable
fun MoveListDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF140D22),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, SoulCyan),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(340.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🥋 SHADOW COMBAT MOVES",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = SoulCyan
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(MOVE_GUIDE_LIST) { move ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1433)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${move.title} (${move.hindiTitle})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Surface(
                                        color = BloodCrimson,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = move.command,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = move.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFFA59EB3),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Pause Dialog ---
@Composable
fun PauseDialog(
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onResume: () -> Unit,
    onQuitToDojo: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF140D22),
            border = androidx.compose.foundation.BorderStroke(2.dp, FlameAmber),
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "BATTLE PAUSED",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = FlameAmber,
                    letterSpacing = 2.sp
                )

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodCrimson),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_resume")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESUME FIGHT", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onToggleSound,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281C38)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSoundEnabled) "SOUND: ON" else "SOUND: OFF", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onQuitToDojo,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38141C)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_quit_dojo")
                ) {
                    Text("SURRENDER & QUIT TO DOJO", fontSize = 11.sp, color = Color(0xFFFF8A80))
                }
            }
        }
    }
}

// --- Victory / Defeat Match End Dialog ---
@Composable
fun MatchResultDialog(
    isVictory: Boolean,
    playerRounds: Int,
    enemyRounds: Int,
    rewardCoins: Int,
    bossName: String,
    onRematch: () -> Unit,
    onNextOrDojo: () -> Unit
) {
    Dialog(onDismissRequest = onNextOrDojo) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF140D22),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = if (isVictory) GoldYellow else Color(0xFF4A3464)
            ),
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Victory / Defeat Header
                Text(
                    text = if (isVictory) "VICTORY!" else "DEFEATED",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isVictory) GoldYellow else Color(0xFFFF5252),
                    letterSpacing = 2.sp
                )

                Text(
                    text = if (isVictory) "You conquered $bossName!" else "$bossName prevailed this time.",
                    fontSize = 13.sp,
                    color = Color(0xFFA59EB3),
                    textAlign = TextAlign.Center
                )

                // Round Score
                Surface(
                    color = Color(0xFF201333),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SHADOW", fontSize = 10.sp, color = SoulCyan, fontWeight = FontWeight.Bold)
                            Text("$playerRounds", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Text("-", fontSize = 20.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ENEMY", fontSize = 10.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                            Text("$enemyRounds", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }

                // Reward Coins
                if (rewardCoins > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+$rewardCoins COINS EARNED!", fontSize = 14.sp, fontWeight = FontWeight.Black, color = GoldYellow)
                    }
                }

                // Buttons
                Button(
                    onClick = onNextOrDojo,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isVictory) BloodCrimson else Color(0xFF4A3464)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_result_continue")
                ) {
                    Text(if (isVictory) "CONTINUE TO DOJO" else "RETURN TO DOJO", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onRematch,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF281C38)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_result_rematch")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RETRY BATTLE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
