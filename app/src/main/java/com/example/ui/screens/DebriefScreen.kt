package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberYellow
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

@Composable
fun DebriefScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val debrief by viewModel.debriefData.collectAsState()
    val isVictory = debrief?.isVictory == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(20.dp)
            .testTag("debrief_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurface)
                .border(
                    width = 2.dp,
                    color = if (isVictory) CyberCyan else CyberRed,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isVictory) CyberCyan.copy(alpha = 0.2f) else CyberRed.copy(alpha = 0.2f))
                    .border(2.dp, if (isVictory) CyberCyan else CyberRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVictory) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isVictory) "任务胜利" else "行动受挫",
                    tint = if (isVictory) CyberCyan else CyberRed,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = if (isVictory) "MISSION ACCOMPLISHED // 战区肃清" else "CRITICAL RETREAT // 紧急折跃",
                color = if (isVictory) CyberCyan else CyberRed,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = debrief?.mission?.title ?: "行动报告",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Star Rating (if victory)
            if (isVictory) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val stars = debrief?.starsEarned ?: 1
                    for (i in 1..3) {
                        Icon(
                            imageVector = if (stars >= i) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "评级",
                            tint = if (stars >= i) CyberYellow else Color(0xFF475569),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Combat Stats Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF090E1A))
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DebriefStatRow("战术总分 (SCORE)", "${debrief?.score ?: 0}", CyberYellow)
                    DebriefStatRow("击灭敌军 (KILLS)", "${debrief?.kills ?: 0}", Color.White)
                    DebriefStatRow("精准爆头 (HEADSHOTS)", "${debrief?.headshots ?: 0}", CyberCyan)
                    DebriefStatRow("射击命中率 (ACCURACY)", "${debrief?.accuracy ?: 0}%", Color(0xFF00FF88))
                    DebriefStatRow("获得信用点 (CREDITS)", "+${debrief?.creditsEarned ?: 0}", CyberYellow)
                    DebriefStatRow("获得纳米元 (NANITES)", "+${debrief?.nanitesEarned ?: 0}", CyberCyan)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(AppScreen.MISSION_SELECT) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                        .testTag("debrief_home_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "基地",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("返回基地", color = Color.White, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val m = debrief?.mission
                        if (m != null) viewModel.selectMissionAndStart(m)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVictory) CyberCyan else CyberRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("debrief_retry_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重新部署",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isVictory) "再次出击" else "重新部署",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DebriefStatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
