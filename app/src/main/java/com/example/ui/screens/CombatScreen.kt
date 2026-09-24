package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FiberSmartRecord
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.Camera3D
import com.example.engine.Enemy
import com.example.engine.EnemyState
import com.example.engine.EnemyType
import com.example.engine.Game3DMath
import com.example.engine.Particle
import com.example.engine.ParticleType
import com.example.engine.Projectile
import com.example.engine.Vec3
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.HealthRed
import com.example.ui.theme.ShieldBlue
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CombatScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val hp by viewModel.playerHp.collectAsState()
    val maxHp by viewModel.playerMaxHp.collectAsState()
    val shield by viewModel.playerShield.collectAsState()
    val maxShield by viewModel.playerMaxShield.collectAsState()
    val weaponRuntime by viewModel.currentWeaponRuntime.collectAsState()
    val enemies by viewModel.enemies.collectAsState()
    val projectiles by viewModel.projectiles.collectAsState()
    val isBulletTime by viewModel.isBulletTimeActive.collectAsState()
    val btRemaining by viewModel.bulletTimeRemaining.collectAsState()
    val btCooldown by viewModel.bulletTimeCooldown.collectAsState()
    val empCd by viewModel.empCooldown.collectAsState()
    val healCd by viewModel.healCooldown.collectAsState()
    val isThermal by viewModel.isThermalScope.collectAsState()
    val activeDialogue by viewModel.activeDialogue.collectAsState()
    val activeMission by viewModel.activeMission.collectAsState()
    val currentWave by viewModel.currentWave.collectAsState()
    val totalWaves by viewModel.totalWaves.collectAsState()
    val score by viewModel.combatScore.collectAsState()

    // Pulse animation for critical alerts & low hp
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alert"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    viewModel.onAimDrag(dragAmount.x, dragAmount.y)
                }
            }
            .testTag("combat_screen_root")
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // 3D Canvas Rendering
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCyberpunkScene(
                camera = viewModel.camera,
                enemies = enemies,
                projectiles = projectiles,
                particles = viewModel.particleSystem.getParticles(),
                isThermal = isThermal,
                isBulletTime = isBulletTime,
                alertAlpha = alertAlpha,
                weaponEntityId = weaponRuntime?.entity?.id ?: "rifle_pulse"
            )

            // Render damage numbers
            drawDamagePopups(viewModel.particleSystem.getPopups(), viewModel.camera)

            // Render 3D Weapon in bottom right
            drawTacticalWeapon(
                weapon = weaponRuntime?.entity,
                isReloading = weaponRuntime?.isReloading == true,
                reloadProgress = if (weaponRuntime?.isReloading == true) {
                    1f - (weaponRuntime!!.reloadTimer / weaponRuntime!!.entity.reloadTimeSec)
                } else 0f,
                camera = viewModel.camera
            )
        }

        // Low HP Red Vignette warning
        if (hp < 35f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 8.dp,
                        color = CyberRed.copy(alpha = alertAlpha * 0.6f)
                    )
            )
        }

        // Bullet Time Blue Warp Edge Effect
        if (isBulletTime) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 6.dp,
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, CyberCyan.copy(alpha = 0.5f))
                        ),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
            )
        }

        // Top Tactical Status Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mission title & wave
                Column {
                    Text(
                        text = activeMission?.title ?: "行动展开中",
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "波次 // WAVE $currentWave / $totalWaves   战术积分: $score",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Top right control buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Thermal toggle
                    IconButton(
                        onClick = { viewModel.toggleThermalScope() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isThermal) CyberCyan.copy(alpha = 0.25f) else Color(0x660F172A))
                            .border(1.dp, if (isThermal) CyberCyan else Color(0xFF334155), CircleShape)
                            .testTag("thermal_toggle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = "热成像战术夜视",
                            tint = if (isThermal) CyberCyan else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Exit combat button
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.MISSION_SELECT) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x660F172A))
                            .border(1.dp, Color(0xFF334155), CircleShape)
                            .testTag("exit_combat_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "撤出任务",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Player Shield & Health HUD Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Shield Gauge
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SHIELD 动能护盾",
                            color = ShieldBlue,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${shield.toInt()} / ${maxShield.toInt()}",
                            color = ShieldBlue,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (shield / maxShield).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ShieldBlue,
                        trackColor = Color(0x3338BDF8)
                    )
                }

                // Health Gauge
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "HEALTH 装甲结构",
                            color = HealthRed,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${hp.toInt()} / ${maxHp.toInt()}",
                            color = HealthRed,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (hp / maxHp).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = HealthRed,
                        trackColor = Color(0x33EF4444)
                    )
                }
            }
        }

        // Center Tactical Reticle (Crosshair)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(80.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val reticleColor = if (isThermal) Color(0xFF00FF88) else CyberCyan

                // Outer brackets
                drawArc(
                    color = reticleColor.copy(alpha = 0.8f),
                    startAngle = 45f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(4f, 4f),
                    size = Size(size.width - 8f, size.height - 8f),
                    style = Stroke(width = 2f)
                )
                drawArc(
                    color = reticleColor.copy(alpha = 0.8f),
                    startAngle = 135f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(4f, 4f),
                    size = Size(size.width - 8f, size.height - 8f),
                    style = Stroke(width = 2f)
                )
                drawArc(
                    color = reticleColor.copy(alpha = 0.8f),
                    startAngle = 225f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(4f, 4f),
                    size = Size(size.width - 8f, size.height - 8f),
                    style = Stroke(width = 2f)
                )
                drawArc(
                    color = reticleColor.copy(alpha = 0.8f),
                    startAngle = 315f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(4f, 4f),
                    size = Size(size.width - 8f, size.height - 8f),
                    style = Stroke(width = 2f)
                )

                // Crosshair tick marks
                drawLine(
                    color = Color.White,
                    start = Offset(cx - 18f, cy),
                    end = Offset(cx - 6f, cy),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cx + 6f, cy),
                    end = Offset(cx + 18f, cy),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cx, cy - 18f),
                    end = Offset(cx, cy - 6f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cx, cy + 6f),
                    end = Offset(cx, cy + 18f),
                    strokeWidth = 2f
                )

                // Center laser point
                drawCircle(
                    color = CyberRed,
                    radius = 2.5f,
                    center = Offset(cx, cy)
                )
            }
        }

        // Left Side: Tactical Cyberware Abilities
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ability 1: Bullet Time / Chrono Shift
            AbilityButton(
                icon = Icons.Default.SlowMotionVideo,
                label = if (isBulletTime) "${btRemaining.toInt()}s" else "缓速",
                isActive = isBulletTime,
                cooldown = btCooldown,
                accentColor = CyberCyan,
                testTag = "ability_bullet_time",
                onClick = { viewModel.activateBulletTime() }
            )

            // Ability 2: EMP Overload Blast
            AbilityButton(
                icon = Icons.Default.Bolt,
                label = if (empCd > 0f) "${empCd.toInt()}s" else "脉冲",
                isActive = false,
                cooldown = empCd,
                accentColor = CyberYellow,
                testTag = "ability_emp",
                onClick = { viewModel.activateEmpBlast() }
            )

            // Ability 3: Nanite Armor Repair
            AbilityButton(
                icon = Icons.Default.Shield,
                label = if (healCd > 0f) "${healCd.toInt()}s" else "重构",
                isActive = false,
                cooldown = healCd,
                accentColor = Color(0xFF00FF88),
                testTag = "ability_heal",
                onClick = { viewModel.activateNaniteHeal() }
            )
        }

        // Bottom Controls Bar (Fire, Reload, Weapon Switch, Ammo HUD)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Weapon Info & Switch
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC0F172A))
                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { viewModel.cycleWeapon() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("switch_weapon_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "切换武器",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = weaponRuntime?.entity?.name ?: "武器",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Reload Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC0F172A))
                            .border(1.dp, Color(0xFF334155), CircleShape)
                            .clickable { viewModel.reloadWeapon() }
                            .testTag("reload_weapon_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "换弹",
                            tint = if (weaponRuntime?.isReloading == true) CyberYellow else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Ammo numbers
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (weaponRuntime?.isReloading == true) "RELOADING" else "${weaponRuntime?.currentAmmo ?: 0}",
                        color = if (weaponRuntime?.isReloading == true) CyberYellow else CyberCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = " / ${weaponRuntime?.entity?.magCapacity ?: 0}",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                    )
                }
            }

            // PRIMARY FIRE BUTTON (Large tactical round trigger)
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyberRed.copy(alpha = 0.95f),
                                Color(0xFF880022)
                            )
                        )
                    )
                    .border(2.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    .clickable { viewModel.fireWeapon(screenWidth, screenHeight) }
                    .testTag("primary_fire_btn"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "发射",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "FIRE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Story Dialogue Overlay
        StoryDialogOverlay(
            dialogue = activeDialogue,
            onDismissChoice = { choice -> viewModel.dismissDialogue(choice) },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun AbilityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    cooldown: Float,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val isReady = cooldown <= 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = isReady, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (isActive) accentColor.copy(alpha = 0.35f) else Color(0xCC0F172A))
                .border(
                    width = 1.5.dp,
                    color = if (isActive) accentColor else if (isReady) accentColor.copy(alpha = 0.7f) else Color(0xFF334155),
                    shape = CircleShape
                )
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isReady || isActive) accentColor else Color(0xFF64748B),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isReady || isActive) accentColor else Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// 3D SCENE RENDERING PIPELINE IN DRAW SCOPE
private fun DrawScope.drawCyberpunkScene(
    camera: Camera3D,
    enemies: List<Enemy>,
    projectiles: List<Projectile>,
    particles: List<Particle>,
    isThermal: Boolean,
    isBulletTime: Boolean,
    alertAlpha: Float,
    weaponEntityId: String
) {
    val sw = size.width
    val sh = size.height
    val horizonY = sh * 0.52f + camera.pitch * 350f

    // 1. Sky Gradient & Atmospheric Cyberpunk Cityscape
    val skyTop = if (isThermal) Color(0xFF001108) else Color(0xFF04060E)
    val skyBottom = if (isThermal) Color(0xFF002A14) else Color(0xFF0A1026)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(skyTop, skyBottom),
            startY = 0f,
            endY = horizonY
        ),
        size = Size(sw, horizonY)
    )

    // Distant Neon Skyscrapers
    val buildingBaseColor = if (isThermal) Color(0xFF00381B) else Color(0xFF0A1324)
    val buildingCount = 9
    for (i in 0 until buildingCount) {
        val bw = sw / buildingCount * 1.3f
        val bx = (i - 1) * (sw / (buildingCount - 2)) - camera.yaw * 140f
        val bh = 140f + (i % 4) * 60f
        val by = horizonY - bh

        drawRect(
            color = buildingBaseColor,
            topLeft = Offset(bx, by),
            size = Size(bw, bh)
        )

        // Neon windows / antenna beacons
        val neonColor = when (i % 3) {
            0 -> if (isThermal) Color(0xFF00FF88) else CyberCyan.copy(alpha = 0.6f)
            1 -> if (isThermal) Color(0xFF00CC66) else CyberRed.copy(alpha = 0.5f)
            else -> if (isThermal) Color(0xFF009944) else CyberYellow.copy(alpha = 0.5f)
        }
        for (w in 0 until 4) {
            drawRect(
                color = neonColor,
                topLeft = Offset(bx + 12f + w * 18f, by + 20f + (w % 2) * 30f),
                size = Size(8f, 16f)
            )
        }
    }

    // 2. Perspective Battle Platform / Floor Grid
    val floorColorTop = if (isThermal) Color(0xFF001A0C) else Color(0xFF050B17)
    val floorColorBottom = if (isThermal) Color(0xFF00331A) else Color(0xFF0F1B33)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(floorColorTop, floorColorBottom),
            startY = horizonY,
            endY = sh
        ),
        topLeft = Offset(0f, horizonY),
        size = Size(sw, sh - horizonY)
    )

    // Floor 3D Grid Lines
    val gridColor = if (isThermal) Color(0x3300FF88) else Color(0x2A00F0FF)
    val gridCount = 14
    for (i in 0 until gridCount) {
        val t = i.toFloat() / (gridCount - 1)
        val groundX = (t - 0.5f) * sw * 3.5f - camera.yaw * 400f
        val projBottom = Offset(sw / 2f + groundX, sh)
        val projVanishing = Offset(sw / 2f - camera.yaw * 120f, horizonY)

        drawLine(
            color = gridColor,
            start = projVanishing,
            end = projBottom,
            strokeWidth = 1.2f
        )
    }

    // Horizontal Scanning Floor Lines
    for (j in 1..7) {
        val factor = j.toFloat() / 7f
        val lineY = horizonY + (sh - horizonY) * (factor * factor)
        drawLine(
            color = gridColor,
            start = Offset(0f, lineY),
            end = Offset(sw, lineY),
            strokeWidth = 1.2f
        )
    }

    // 3. Draw 3D Enemies (Sorted by Depth Z)
    val sortedEnemies = enemies.sortedByDescending { it.pos.z }
    for (enemy in sortedEnemies) {
        drawEnemy3D(enemy, camera, sw, sh, isThermal, alertAlpha)
    }

    // 4. Draw Projectiles
    for (proj in projectiles) {
        val p = Game3DMath.project(proj.pos, camera, sw, sh)
        if (p.inFront) {
            val r = (proj.radius * p.scale).coerceAtLeast(3f)
            drawCircle(
                color = if (isThermal) Color(0xFF00FF88) else proj.color,
                radius = r,
                center = Offset(p.screenX, p.screenY)
            )
            // Outer plasma glow
            drawCircle(
                color = (if (isThermal) Color(0xFF00FF88) else proj.color).copy(alpha = 0.35f),
                radius = r * 2.2f,
                center = Offset(p.screenX, p.screenY)
            )
        }
    }

    // 5. Draw 3D Particles
    for (particle in particles) {
        val p = Game3DMath.project(particle.pos, camera, sw, sh)
        if (p.inFront) {
            val alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
            when (particle.type) {
                ParticleType.SHOCKWAVE -> {
                    val radius = particle.size * p.scale * (1f - particle.life / particle.maxLife) * 1.5f
                    drawCircle(
                        color = particle.color.copy(alpha = alpha * 0.8f),
                        radius = radius,
                        center = Offset(p.screenX, p.screenY),
                        style = Stroke(width = 3f * p.scale)
                    )
                }
                ParticleType.SHELL_CASING -> {
                    val cw = 6f * p.scale
                    val ch = 3f * p.scale
                    drawRect(
                        color = Color(0xFFFFB800),
                        topLeft = Offset(p.screenX - cw / 2f, p.screenY - ch / 2f),
                        size = Size(cw, ch)
                    )
                }
                else -> {
                    val r = (particle.size * p.scale * 0.5f).coerceIn(2f, 40f)
                    drawCircle(
                        color = particle.color.copy(alpha = alpha),
                        radius = r,
                        center = Offset(p.screenX, p.screenY)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawEnemy3D(
    enemy: Enemy,
    camera: Camera3D,
    sw: Float,
    sh: Float,
    isThermal: Boolean,
    alertAlpha: Float
) {
    val proj = Game3DMath.project(enemy.pos, camera, sw, sh)
    if (!proj.inFront) return

    val scale = proj.scale
    val ex = proj.screenX
    val ey = proj.screenY

    val isHurt = enemy.hurtTimer > 0f
    val baseColor = when {
        isHurt -> Color(0xFFFF2200)
        isThermal -> Color(0xFF00FF88)
        enemy.type == EnemyType.GOLIATH_BOSS -> Color(0xFFDC2626)
        enemy.type == EnemyType.HEAVY_ENFORCER -> Color(0xFFE11D48)
        enemy.type == EnemyType.CYBER_STALKER -> Color(0xFF00F0FF)
        else -> Color(0xFFFFB800)
    }

    when (enemy.type) {
        EnemyType.RECON_DRONE -> {
            val droneR = 30f * scale
            // Quad-rotor arms
            drawLine(
                color = Color(0xFF475569),
                start = Offset(ex - droneR * 1.4f, ey - droneR * 0.7f),
                end = Offset(ex + droneR * 1.4f, ey + droneR * 0.7f),
                strokeWidth = 3f * scale
            )
            drawLine(
                color = Color(0xFF475569),
                start = Offset(ex - droneR * 1.4f, ey + droneR * 0.7f),
                end = Offset(ex + droneR * 1.4f, ey - droneR * 0.7f),
                strokeWidth = 3f * scale
            )
            // Drone fuselage
            drawCircle(
                color = baseColor,
                radius = droneR,
                center = Offset(ex, ey)
            )
            // Glowing scanning red eye
            drawCircle(
                color = if (isThermal) Color.White else Color(0xFFFF0033),
                radius = droneR * 0.45f,
                center = Offset(ex, ey)
            )
        }

        EnemyType.CYBER_STALKER -> {
            val h = 80f * scale
            val w = 34f * scale
            // Torso
            drawRect(
                color = baseColor,
                topLeft = Offset(ex - w / 2f, ey - h / 2f),
                size = Size(w, h * 0.6f)
            )
            // Head / Visor
            drawCircle(
                color = if (isThermal) Color.White else Color(0xFF00F0FF),
                radius = 12f * scale,
                center = Offset(ex, ey - h * 0.65f)
            )
            // Mechanical limbs
            drawLine(
                color = Color(0xFF334155),
                start = Offset(ex - w * 0.3f, ey + h * 0.1f),
                end = Offset(ex - w * 0.7f, ey + h * 0.7f),
                strokeWidth = 4f * scale
            )
            drawLine(
                color = Color(0xFF334155),
                start = Offset(ex + w * 0.3f, ey + h * 0.1f),
                end = Offset(ex + w * 0.7f, ey + h * 0.7f),
                strokeWidth = 4f * scale
            )
        }

        EnemyType.HEAVY_ENFORCER -> {
            val h = 110f * scale
            val w = 55f * scale
            // Heavy armor chest
            drawRoundRect(
                color = baseColor,
                topLeft = Offset(ex - w / 2f, ey - h / 2f),
                size = Size(w, h * 0.7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale)
            )
            // Shoulder weapon pod
            drawRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(ex + w * 0.4f, ey - h * 0.4f),
                size = Size(20f * scale, 30f * scale)
            )
            // Armor visor
            drawLine(
                color = CyberYellow,
                start = Offset(ex - 12f * scale, ey - h * 0.55f),
                end = Offset(ex + 12f * scale, ey - h * 0.55f),
                strokeWidth = 4f * scale
            )
        }

        EnemyType.GOLIATH_BOSS -> {
            // Colossal Boss Mech!
            val bw = 160f * scale
            val bh = 140f * scale

            // Massive mechanical legs
            drawLine(
                color = Color(0xFF334155),
                start = Offset(ex - bw * 0.45f, ey),
                end = Offset(ex - bw * 0.8f, ey + bh * 0.8f),
                strokeWidth = 10f * scale
            )
            drawLine(
                color = Color(0xFF334155),
                start = Offset(ex + bw * 0.45f, ey),
                end = Offset(ex + bw * 0.8f, ey + bh * 0.8f),
                strokeWidth = 10f * scale
            )

            // Heavy Torso Chassis
            drawRoundRect(
                color = baseColor,
                topLeft = Offset(ex - bw / 2f, ey - bh / 2f),
                size = Size(bw, bh * 0.75f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f * scale)
            )

            // Missile Pods on Left & Right
            drawRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(ex - bw * 0.75f, ey - bh * 0.6f),
                size = Size(35f * scale, 45f * scale)
            )
            drawRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(ex + bw * 0.4f, ey - bh * 0.6f),
                size = Size(35f * scale, 45f * scale)
            )

            // Core Glowing Eye / Reactor
            drawCircle(
                color = if (isHurt) Color.White else CyberYellow,
                radius = 24f * scale,
                center = Offset(ex, ey - bh * 0.1f)
            )
            drawCircle(
                color = CyberRed,
                radius = 12f * scale,
                center = Offset(ex, ey - bh * 0.1f)
            )

            // Kinetic Shield Arc Bubble
            if (enemy.shield > 0f) {
                drawArc(
                    color = CyberCyan.copy(alpha = 0.5f + alertAlpha * 0.3f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(ex - bw * 0.85f, ey - bh * 0.9f),
                    size = Size(bw * 1.7f, bh * 1.6f),
                    style = Stroke(width = 4f * scale)
                )
            }
        }
    }

    // Overhead Health & Shield Bar
    val barW = (enemy.type.bodyRadius * scale * 70f).coerceIn(40f, 180f)
    val barH = 5f * scale
    val barY = ey - (enemy.type.bodyRadius * scale * 60f) - 18f

    // Background track
    drawRect(
        color = Color(0x99000000),
        topLeft = Offset(ex - barW / 2f, barY),
        size = Size(barW, barH)
    )

    // Shield portion
    if (enemy.maxShield > 0f && enemy.shield > 0f) {
        val sRatio = (enemy.shield / enemy.maxShield).coerceIn(0f, 1f)
        drawRect(
            color = ShieldBlue,
            topLeft = Offset(ex - barW / 2f, barY - barH - 2f),
            size = Size(barW * sRatio, barH)
        )
    }

    // Health portion
    val hRatio = (enemy.health / enemy.maxHealth).coerceIn(0f, 1f)
    drawRect(
        color = if (enemy.type == EnemyType.GOLIATH_BOSS) CyberRed else CyberOrange,
        topLeft = Offset(ex - barW / 2f, barY),
        size = Size(barW * hRatio, barH)
    )
}

private fun DrawScope.drawDamagePopups(
    popups: List<com.example.engine.DamagePopup>,
    camera: Camera3D
) {
    for (popup in popups) {
        val worldP = Vec3(popup.worldPos.x, popup.worldPos.y + popup.offsetY, popup.worldPos.z)
        val p = Game3DMath.project(worldP, camera, size.width, size.height)
        if (p.inFront) {
            val alpha = (popup.life).coerceIn(0f, 1f)
            val textSize = (if (popup.isCritical) 20f else 14f) * p.scale.coerceIn(0.6f, 1.8f)

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = popup.color.copy(alpha = alpha).hashCode()
                    isFakeBoldText = true
                    this.textSize = textSize
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(popup.text, p.screenX, p.screenY, paint)
            }
        }
    }
}

private fun DrawScope.drawTacticalWeapon(
    weapon: com.example.data.db.WeaponEntity?,
    isReloading: Boolean,
    reloadProgress: Float,
    camera: Camera3D
) {
    val sw = size.width
    val sh = size.height

    val baseGunX = sw * 0.72f + camera.recoilYaw * 60f
    val baseGunY = sh * 0.78f + camera.recoilPitch * 120f + (if (isReloading) 50f * sin(reloadProgress * Math.PI.toFloat()) else 0f)

    // Weapon receiver body (Metallic angled polygon)
    val gunPath = Path().apply {
        moveTo(baseGunX - 40f, sh)
        lineTo(baseGunX, baseGunY)
        lineTo(baseGunX + 110f, baseGunY + 20f)
        lineTo(baseGunX + 140f, sh)
        close()
    }

    drawPath(
        path = gunPath,
        color = Color(0xFF1E293B)
    )
    drawPath(
        path = gunPath,
        color = CyberCyan.copy(alpha = 0.5f),
        style = Stroke(width = 2f)
    )

    // Barrel Extension
    drawRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(baseGunX + 10f, baseGunY - 24f),
        size = Size(40f, 28f)
    )

    // Glowing weapon energy conduit
    drawLine(
        color = CyberCyan,
        start = Offset(baseGunX + 15f, baseGunY + 8f),
        end = Offset(baseGunX + 85f, baseGunY + 18f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    // Muzzle Flash when firing
    if (camera.recoilPitch > 0.05f) {
        val muzzleX = baseGunX + 30f
        val muzzleY = baseGunY - 26f
        drawCircle(
            color = Color(0xFFFFB800),
            radius = 32f,
            center = Offset(muzzleX, muzzleY)
        )
        drawCircle(
            color = Color.White,
            radius = 16f,
            center = Offset(muzzleX, muzzleY)
        )
    }
}
