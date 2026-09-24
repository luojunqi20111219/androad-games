package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.WeaponEntity
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberYellow
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

@Composable
fun ArmoryScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val weapons by viewModel.weapons.collectAsState()
    val profile by viewModel.playerProfile.collectAsState()
    val equippedId = profile?.equippedWeaponId ?: "rifle_pulse"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
            .testTag("armory_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.MISSION_SELECT) },
                modifier = Modifier.testTag("armory_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = CyberCyan
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "星界特战军械库 // ARSENAL ARMORY",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "先进等离子磁聚束火控系统",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Credits & Nanites
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${profile?.credits ?: 0} 信用点",
                    color = CyberYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${profile?.nanites ?: 0} 纳米元",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Weapon Cards List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(weapons, key = { it.id }) { weapon ->
                val isEquipped = weapon.id == equippedId
                val upgradeCostCredits = weapon.upgradeLevel * 1200
                val upgradeCostNanites = weapon.upgradeLevel * 50
                val canUpgrade = (profile?.credits ?: 0) >= upgradeCostCredits && (profile?.nanites ?: 0) >= upgradeCostNanites
                val canUnlock = (profile?.credits ?: 0) >= weapon.unlockCost

                WeaponCard(
                    weapon = weapon,
                    isEquipped = isEquipped,
                    canUpgrade = canUpgrade,
                    canUnlock = canUnlock,
                    upgradeCostCredits = upgradeCostCredits,
                    upgradeCostNanites = upgradeCostNanites,
                    onEquip = { viewModel.equipWeapon(weapon.id) },
                    onUpgrade = { viewModel.upgradeWeapon(weapon.id) },
                    onUnlock = { viewModel.unlockWeapon(weapon.id) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun WeaponCard(
    weapon: WeaponEntity,
    isEquipped: Boolean,
    canUpgrade: Boolean,
    canUnlock: Boolean,
    upgradeCostCredits: Int,
    upgradeCostNanites: Int,
    onEquip: () -> Unit,
    onUpgrade: () -> Unit,
    onUnlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(
                width = if (isEquipped) 1.8.dp else 1.dp,
                color = if (isEquipped) CyberCyan else CyberCardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
            .testTag("weapon_card_${weapon.id}")
    ) {
        Column {
            // Header: Name, Type, Level, Equipped Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = weapon.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isEquipped) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberCyan)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "当前装备中",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Text(
                        text = "${weapon.typeName}  [强化等级 LV.${weapon.upgradeLevel}]",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = weapon.description,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stat Bars
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatRow(
                    label = "杀伤威力 (DMG)",
                    valueText = "${weapon.baseDamage.toInt()}",
                    progress = (weapon.baseDamage / 260f).coerceIn(0f, 1f),
                    color = CyberRed
                )
                StatRow(
                    label = "击发射速 (RPS)",
                    valueText = "${weapon.fireRateRps} 发/秒",
                    progress = (weapon.fireRateRps / 12f).coerceIn(0f, 1f),
                    color = CyberYellow
                )
                StatRow(
                    label = "弹匣容量 (MAG)",
                    valueText = "${weapon.magCapacity} 发",
                    progress = (weapon.magCapacity / 45f).coerceIn(0f, 1f),
                    color = CyberCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row: Equip, Upgrade, Unlock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!weapon.isUnlocked) {
                    Button(
                        onClick = onUnlock,
                        enabled = canUnlock,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberYellow,
                            disabledContainerColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("unlock_weapon_${weapon.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "解锁",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "解锁 (${weapon.unlockCost} 信用点)",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Upgrade Button
                    Button(
                        onClick = onUpgrade,
                        enabled = canUpgrade,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            disabledContainerColor = Color(0x331E293B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, if (canUpgrade) CyberYellow else Color(0xFF334155), RoundedCornerShape(8.dp))
                            .testTag("upgrade_weapon_${weapon.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "强化",
                            tint = if (canUpgrade) CyberYellow else Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "强化 ($upgradeCostCredits 币 / $upgradeCostNanites 纳米)",
                            color = if (canUpgrade) CyberYellow else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Equip Button
                    if (!isEquipped) {
                        Button(
                            onClick = onEquip,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("equip_weapon_${weapon.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "装备",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "装备出战",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    valueText: String,
    progress: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(110.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0x22FFFFFF)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = valueText,
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp)
        )
    }
}
