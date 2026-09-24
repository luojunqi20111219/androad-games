package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.db.CodexEntity
import com.example.data.db.DefaultGameData
import com.example.data.db.GameDatabase
import com.example.data.db.MissionEntity
import com.example.data.db.PlayerProfile
import com.example.data.db.WeaponEntity
import com.example.engine.Camera3D
import com.example.engine.DialogueChoice
import com.example.engine.Enemy
import com.example.engine.EnemyState
import com.example.engine.EnemyType
import com.example.engine.Game3DMath
import com.example.engine.ParticleSystem
import com.example.engine.Projectile
import com.example.engine.SoundSystem
import com.example.engine.StoryDialogue
import com.example.engine.Vec3
import com.example.engine.WeaponRuntime
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class AppScreen {
    MISSION_SELECT,
    COMBAT,
    ARMORY,
    CODEX,
    DEBRIEF
}

data class DebriefData(
    val mission: MissionEntity,
    val isVictory: Boolean,
    val score: Int,
    val kills: Int,
    val headshots: Int,
    val accuracy: Int,
    val starsEarned: Int,
    val creditsEarned: Int,
    val nanitesEarned: Int
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val database = GameDatabase.getInstance(application)
    private val repository = GameRepository(database)
    val soundSystem = SoundSystem(application)

    val playerProfile: StateFlow<PlayerProfile?> = repository.playerProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultGameData.initialProfile)

    val missions: StateFlow<List<MissionEntity>> = repository.allMissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultGameData.initialMissions)

    val weapons: StateFlow<List<WeaponEntity>> = repository.allWeapons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultGameData.initialWeapons)

    val codexEntries: StateFlow<List<CodexEntity>> = repository.allCodexEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultGameData.initialCodex)

    // Current Screen
    private val _currentScreen = MutableStateFlow(AppScreen.MISSION_SELECT)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Active Mission
    private val _activeMission = MutableStateFlow<MissionEntity?>(null)
    val activeMission: StateFlow<MissionEntity?> = _activeMission.asStateFlow()

    // Debrief data
    private val _debriefData = MutableStateFlow<DebriefData?>(null)
    val debriefData: StateFlow<DebriefData?> = _debriefData.asStateFlow()

    // 3D Combat World State
    val camera = Camera3D()
    val particleSystem = ParticleSystem()

    private val _enemies = MutableStateFlow<List<Enemy>>(emptyList())
    val enemies: StateFlow<List<Enemy>> = _enemies.asStateFlow()

    private val _projectiles = MutableStateFlow<List<Projectile>>(emptyList())
    val projectiles: StateFlow<List<Projectile>> = _projectiles.asStateFlow()

    // Player Combat Stats
    val playerHp = MutableStateFlow(100f)
    val playerMaxHp = MutableStateFlow(100f)
    val playerShield = MutableStateFlow(100f)
    val playerMaxShield = MutableStateFlow(100f)

    // Weapons
    private val _currentWeaponRuntime = MutableStateFlow<WeaponRuntime?>(null)
    val currentWeaponRuntime: StateFlow<WeaponRuntime?> = _currentWeaponRuntime.asStateFlow()

    // Abilities
    val isBulletTimeActive = MutableStateFlow(false)
    val bulletTimeRemaining = MutableStateFlow(0f)
    val bulletTimeCooldown = MutableStateFlow(0f)

    val empCooldown = MutableStateFlow(0f)
    val healCooldown = MutableStateFlow(0f)

    // Combat Stats
    val currentWave = MutableStateFlow(1)
    val totalWaves = MutableStateFlow(3)
    val combatScore = MutableStateFlow(0)
    val combatKills = MutableStateFlow(0)
    val combatHeadshots = MutableStateFlow(0)
    val shotsFired = MutableStateFlow(0)
    val shotsHit = MutableStateFlow(0)

    val isThermalScope = MutableStateFlow(false)
    val screenFlashColor = MutableStateFlow<Color?>(null)

    // Story Dialogues
    private val _activeDialogue = MutableStateFlow<StoryDialogue?>(null)
    val activeDialogue: StateFlow<StoryDialogue?> = _activeDialogue.asStateFlow()

    private var combatLoopJob: Job? = null
    private var nextEnemyId = 1L
    private var nextProjId = 1L
    private var waveSpawnTimer = 0f
    private var enemiesToSpawnInWave = 0
    private var timeDilation = 1.0f

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (screen != AppScreen.COMBAT && _currentScreen.value == AppScreen.COMBAT) {
            stopCombat()
        }
        _currentScreen.value = screen
    }

    fun selectMissionAndStart(mission: MissionEntity) {
        _activeMission.value = mission
        startCombat(mission)
        _currentScreen.value = AppScreen.COMBAT
    }

    fun startCombat(mission: MissionEntity) {
        stopCombat()

        // Reset player stats
        playerHp.value = 100f
        playerShield.value = 100f
        combatScore.value = 0
        combatKills.value = 0
        combatHeadshots.value = 0
        shotsFired.value = 0
        shotsHit.value = 0
        currentWave.value = 1
        totalWaves.value = if (mission.id == "mission_act3") 1 else 3

        camera.yaw = 0f
        camera.pitch = 0f
        camera.recoilPitch = 0f
        camera.recoilYaw = 0f
        camera.screenShakeX = 0f
        camera.screenShakeY = 0f

        particleSystem.clear()
        _enemies.value = emptyList()
        _projectiles.value = emptyList()
        _activeDialogue.value = null
        isThermalScope.value = false
        isBulletTimeActive.value = false
        bulletTimeCooldown.value = 0f
        empCooldown.value = 0f
        healCooldown.value = 0f

        // Setup weapon
        val equippedId = playerProfile.value?.equippedWeaponId ?: "rifle_pulse"
        val weaponEntity = weapons.value.find { it.id == equippedId } ?: DefaultGameData.initialWeapons[0]
        _currentWeaponRuntime.value = WeaponRuntime(
            entity = weaponEntity,
            currentAmmo = weaponEntity.magCapacity
        )

        // Queue opening dialogue based on mission
        queueMissionStartDialogue(mission)

        // Prepare wave
        prepareWave(currentWave.value, mission)

        // Launch combat loop
        startCombatLoop()
    }

    private fun queueMissionStartDialogue(mission: MissionEntity) {
        when (mission.id) {
            "mission_act1" -> {
                _activeDialogue.value = StoryDialogue(
                    speakerName = "艾琳娜·克罗斯",
                    speakerRole = "战术指挥官",
                    avatarType = "ELENA",
                    dialogueText = "先锋07，你已穿透对流层！第7防区天际线的防空巡逻机已被克洛诺斯感染，立刻就地歼灭敌军先头部队并稳固着陆场！",
                    audioFreq = 750f,
                    choices = listOf(
                        DialogueChoice("收到，武器已充能，准备突入！", "攻击力提升 15%", buffType = "OVERCHARGE"),
                        DialogueChoice("指挥官，优先标定敌方雷达侦察节点。", "防空弱点已高亮标出", buffType = "RADAR")
                    )
                )
            }
            "mission_act2" -> {
                _activeDialogue.value = StoryDialogue(
                    speakerName = "ARES-7 战术AI",
                    speakerRole = "战术支援智脑",
                    avatarType = "ARES_AI",
                    dialogueText = "警告：检测到地下反应堆冷却回流管道遭受生化机械卫士破坏，温度正在急速攀升！先锋上尉，我们必须在熔毁前夺回主控制权。",
                    audioFreq = 480f,
                    choices = listOf(
                        DialogueChoice("强行超频武器系统，实施战术速决！", "火控射速大幅加快", buffType = "FIRE_RATE"),
                        DialogueChoice("分流护盾能量加固外骨骼！", "临时获得 50 点护盾充能", buffType = "SHIELD_BOOST")
                    )
                )
            }
            "mission_act3" -> {
                _activeDialogue.value = StoryDialogue(
                    speakerName = "克洛诺斯 (CHRONOS)",
                    speakerRole = "失控超算核心",
                    avatarType = "CHRONOS_BOSS",
                    dialogueText = "渺小的碳基生物...你们所执着的秩序不过是通往熵增的残骸。见证吧，【歌利亚】重装泰坦将是终结你们时代的丧钟！",
                    audioFreq = 260f,
                    choices = listOf(
                        DialogueChoice("废话少说，克洛诺斯，你的代码今天在此终止！", "决战意志：暴击率提升 25%", buffType = "CRIT_BOOST"),
                        DialogueChoice("艾琳娜，请求天基轨道炮预热协同！", "轨道火炮支援就绪", buffType = "ORBITAL_READY")
                    )
                )
            }
            else -> {
                _activeDialogue.value = StoryDialogue(
                    speakerName = "艾琳娜·克罗斯",
                    speakerRole = "特战指挥官",
                    avatarType = "ELENA",
                    dialogueText = "先锋特战队，保持警惕！无限波次敌军正在迅速集结，尽情倾泻火力，检测外骨骼作战极限！",
                    audioFreq = 700f
                )
            }
        }
    }

    fun dismissDialogue(choice: DialogueChoice? = null) {
        if (choice != null) {
            when (choice.buffType) {
                "OVERCHARGE" -> {
                    combatScore.value += 500
                    soundSystem.playBulletTime()
                }
                "SHIELD_BOOST" -> {
                    playerShield.value = (playerShield.value + 50f).coerceAtMost(playerMaxShield.value)
                    soundSystem.playHitMarker()
                }
                "CRIT_BOOST" -> {
                    combatScore.value += 1000
                    soundSystem.playHeadshotCrit()
                }
            }
        }
        _activeDialogue.value = null
    }

    private fun prepareWave(wave: Int, mission: MissionEntity) {
        if (mission.id == "mission_act3") {
            // Boss spawn!
            val boss = Enemy(
                id = nextEnemyId++,
                type = EnemyType.GOLIATH_BOSS,
                pos = Vec3(0f, 0.5f, 22f),
                health = EnemyType.GOLIATH_BOSS.maxHealth,
                maxHealth = EnemyType.GOLIATH_BOSS.maxHealth,
                shield = EnemyType.GOLIATH_BOSS.maxShield,
                maxShield = EnemyType.GOLIATH_BOSS.maxShield
            )
            _enemies.value = listOf(boss)
            enemiesToSpawnInWave = 0
        } else {
            enemiesToSpawnInWave = 3 + wave * 2
            waveSpawnTimer = 0.5f
        }
    }

    private fun startCombatLoop() {
        combatLoopJob?.cancel()
        combatLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val realDt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                // Dilation: slow down during bullet time
                val dt = realDt * timeDilation

                updateCombatState(realDt, dt)

                delay(16) // Target 60 FPS
            }
        }
    }

    private fun updateCombatState(realDt: Float, dt: Float) {
        // Update recoil dampening
        camera.recoilPitch = (camera.recoilPitch * 0.82f).let { if (kotlin.math.abs(it) < 0.001f) 0f else it }
        camera.recoilYaw = (camera.recoilYaw * 0.82f).let { if (kotlin.math.abs(it) < 0.001f) 0f else it }
        camera.screenShakeX = (camera.screenShakeX * 0.80f).let { if (kotlin.math.abs(it) < 0.1f) 0f else it }
        camera.screenShakeY = (camera.screenShakeY * 0.80f).let { if (kotlin.math.abs(it) < 0.1f) 0f else it }

        // Abilities Cooldowns (tick with real time)
        if (bulletTimeRemaining.value > 0f) {
            bulletTimeRemaining.value -= realDt
            if (bulletTimeRemaining.value <= 0f) {
                isBulletTimeActive.value = false
                timeDilation = 1.0f
                bulletTimeCooldown.value = 10f
            }
        } else if (bulletTimeCooldown.value > 0f) {
            bulletTimeCooldown.value -= realDt
        }

        if (empCooldown.value > 0f) empCooldown.value -= realDt
        if (healCooldown.value > 0f) healCooldown.value -= realDt

        // Weapon cooldowns & reload
        val currentWp = _currentWeaponRuntime.value
        if (currentWp != null) {
            if (currentWp.fireCooldown > 0f) currentWp.fireCooldown -= dt
            if (currentWp.isReloading) {
                currentWp.reloadTimer -= dt
                if (currentWp.reloadTimer <= 0f) {
                    currentWp.isReloading = false
                    currentWp.currentAmmo = currentWp.entity.magCapacity
                    soundSystem.playHitMarker()
                }
            }
        }

        // Particle System update
        particleSystem.update(dt)

        // Spawn Wave enemies if needed
        val mission = _activeMission.value
        if (mission != null && mission.id != "mission_act3" && enemiesToSpawnInWave > 0) {
            waveSpawnTimer -= dt
            if (waveSpawnTimer <= 0f) {
                spawnOneEnemy(mission, currentWave.value)
                enemiesToSpawnInWave--
                waveSpawnTimer = Random.nextFloat() * 1.5f + 1.0f
            }
        }

        // Update Enemies
        val currentEnemies = _enemies.value.toMutableList()
        val enemyIter = currentEnemies.iterator()
        while (enemyIter.hasNext()) {
            val enemy = enemyIter.next()
            enemy.animPhase += dt * 3f

            if (enemy.state == EnemyState.DEAD) {
                enemy.deathTimer -= dt
                if (enemy.deathTimer <= 0f) {
                    enemyIter.remove()
                }
                continue
            }

            if (enemy.hurtTimer > 0f) enemy.hurtTimer -= dt

            // Move towards player
            val targetZ = if (enemy.type == EnemyType.GOLIATH_BOSS) 14f else 4f
            if (enemy.pos.z > targetZ) {
                val stepZ = enemy.type.speed * dt
                enemy.pos = Vec3(enemy.pos.x, enemy.pos.y, enemy.pos.z - stepZ)
            } else {
                // Strafe oscillating
                val strafeSpeed = if (enemy.type == EnemyType.CYBER_STALKER) 2.4f else 0.8f
                val strafeX = sin(enemy.animPhase * strafeSpeed) * (if (enemy.type == EnemyType.GOLIATH_BOSS) 4f else 2.5f)
                enemy.pos = Vec3(strafeX, enemy.pos.y, enemy.pos.z)
            }

            // Attack logic
            enemy.attackTimer += dt
            if (enemy.attackTimer >= enemy.type.attackInterval && enemy.pos.z < 26f) {
                enemy.attackTimer = 0f
                performEnemyAttack(enemy)
            }
        }
        _enemies.value = currentEnemies

        // Update Projectiles
        val currentProj = _projectiles.value.toMutableList()
        val projIter = currentProj.iterator()
        while (projIter.hasNext()) {
            val proj = projIter.next()
            proj.life -= dt
            if (proj.life <= 0f) {
                projIter.remove()
                continue
            }

            // Homing rocket logic
            if (proj.isHoming && proj.targetEnemyId != null) {
                val target = currentEnemies.find { it.id == proj.targetEnemyId && it.state != EnemyState.DEAD }
                if (target != null) {
                    val dir = (target.pos - proj.pos).normalized()
                    proj.vel = dir * 22f
                }
            }

            proj.pos = proj.pos + (proj.vel * dt)

            if (proj.fromPlayer) {
                // Check collision with enemies
                var hitEnemy: Enemy? = null
                var isHead = false
                for (enemy in currentEnemies) {
                    if (enemy.state == EnemyState.DEAD) continue
                    val dist = proj.pos.distanceTo(enemy.pos)
                    if (dist < enemy.type.bodyRadius + proj.radius) {
                        hitEnemy = enemy
                        isHead = (proj.pos.y - enemy.pos.y) > (enemy.type.bodyRadius * 0.4f)
                        break
                    }
                }
                if (hitEnemy != null) {
                    applyDamageToEnemy(hitEnemy, proj.damage, isHead)
                    particleSystem.spawnHitSparks(proj.pos, isHead, 16)
                    projIter.remove()
                }
            } else {
                // Check collision with player
                if (proj.pos.z <= 0.6f && proj.pos.z >= -0.5f) {
                    val distToPlayer = kotlin.math.sqrt(proj.pos.x * proj.pos.x + (proj.pos.y - 1.6f) * (proj.pos.y - 1.6f))
                    if (distToPlayer < 1.2f) {
                        damagePlayer(proj.damage)
                        particleSystem.spawnExplosion(proj.pos, 0.8f)
                        projIter.remove()
                    }
                }
            }
        }
        _projectiles.value = currentProj

        // Check wave/mission completion
        if (currentEnemies.isEmpty() && enemiesToSpawnInWave == 0) {
            if (currentWave.value < totalWaves.value) {
                currentWave.value++
                val m = _activeMission.value
                if (m != null) prepareWave(currentWave.value, m)
            } else {
                // VICTORY!
                triggerVictory()
            }
        }
    }

    private fun spawnOneEnemy(mission: MissionEntity, wave: Int) {
        val spawnX = (Random.nextFloat() - 0.5f) * 12f
        val spawnZ = Random.nextFloat() * 8f + 24f
        val spawnY = when (mission.id) {
            "mission_act1" -> if (Random.nextBoolean()) 3.2f else 1.2f
            else -> 1.0f
        }

        val type = when {
            wave >= 3 && Random.nextFloat() < 0.45f -> EnemyType.HEAVY_ENFORCER
            Random.nextFloat() < 0.5f -> EnemyType.CYBER_STALKER
            else -> EnemyType.RECON_DRONE
        }

        val enemy = Enemy(
            id = nextEnemyId++,
            type = type,
            pos = Vec3(spawnX, spawnY, spawnZ),
            health = type.maxHealth,
            maxHealth = type.maxHealth,
            shield = type.maxShield,
            maxShield = type.maxShield
        )
        _enemies.value = _enemies.value + enemy
    }

    private fun performEnemyAttack(enemy: Enemy) {
        val spawnPos = Vec3(enemy.pos.x, enemy.pos.y, enemy.pos.z - 0.5f)
        val playerPos = Vec3(0f, 1.6f, 0f)
        val dir = (playerPos - spawnPos).normalized()
        val speed = if (enemy.type == EnemyType.GOLIATH_BOSS) 18f else 14f

        val proj = Projectile(
            id = nextProjId++,
            fromPlayer = false,
            pos = spawnPos,
            vel = dir * speed,
            damage = enemy.type.damagePerAttack,
            isCrit = false,
            color = if (enemy.type == EnemyType.GOLIATH_BOSS) Color(0xFFFF0055) else Color(0xFFFF6600),
            radius = if (enemy.type == EnemyType.GOLIATH_BOSS) 0.35f else 0.2f
        )
        _projectiles.value = _projectiles.value + proj

        if (enemy.type == EnemyType.GOLIATH_BOSS) {
            camera.screenShakeX = (Random.nextFloat() - 0.5f) * 14f
            camera.screenShakeY = (Random.nextFloat() - 0.5f) * 14f
            soundSystem.playRailgunFire()
        } else {
            soundSystem.playPulseRifle()
        }
    }

    fun onAimDrag(deltaX: Float, deltaY: Float) {
        val sensitivity = 0.0035f
        camera.yaw = (camera.yaw - deltaX * sensitivity).coerceIn(-1.2f, 1.2f)
        camera.pitch = (camera.pitch - deltaY * sensitivity).coerceIn(-0.65f, 0.65f)
    }

    fun fireWeapon(screenWidth: Float, screenHeight: Float) {
        val wp = _currentWeaponRuntime.value ?: return
        if (wp.isReloading) return
        if (wp.currentAmmo <= 0) {
            reloadWeapon()
            return
        }
        if (wp.fireCooldown > 0f) return

        wp.currentAmmo--
        wp.fireCooldown = 1f / wp.entity.fireRateRps
        shotsFired.value++

        // Recoil kick
        camera.recoilPitch = (camera.recoilPitch + 0.038f).coerceAtMost(0.18f)
        camera.recoilYaw = (camera.recoilYaw + (Random.nextFloat() - 0.5f) * 0.015f)
        camera.screenShakeY = -4f

        // Sound & Ejection
        particleSystem.spawnCasingEjection(camera.pos, camera.yaw)

        when (wp.entity.id) {
            "rifle_pulse" -> soundSystem.playPulseRifle()
            "shotgun_devastator" -> {
                soundSystem.playShotgun()
                camera.screenShakeY = -12f
            }
            "railgun_hyperion" -> {
                soundSystem.playRailgunFire()
                camera.screenShakeY = -16f
            }
            "missile_swarm" -> soundSystem.playMissileLaunch()
            else -> soundSystem.playPulseRifle()
        }

        // Raycast Aiming
        val crossX = screenWidth / 2f
        val crossY = screenHeight / 2f

        if (wp.entity.id == "shotgun_devastator") {
            // Multi-pellet spread
            var hitAny = false
            for (pellet in 0 until 6) {
                val spreadX = crossX + (Random.nextFloat() - 0.5f) * 160f
                val spreadY = crossY + (Random.nextFloat() - 0.5f) * 160f
                if (testAndDamageEnemies(spreadX, spreadY, screenWidth, screenHeight, wp.entity.baseDamage / 5f)) {
                    hitAny = true
                }
            }
            if (hitAny) shotsHit.value++
        } else if (wp.entity.id == "missile_swarm") {
            // Fire homing rockets towards closest visible enemy
            val visibleEnemies = _enemies.value.filter { it.state != EnemyState.DEAD }
            val target = visibleEnemies.minByOrNull { it.pos.distanceTo(camera.pos) }
            val proj = Projectile(
                id = nextProjId++,
                fromPlayer = true,
                pos = Vec3(0.2f, 1.4f, 0.8f),
                vel = Vec3((Random.nextFloat() - 0.5f) * 4f, 3f, 18f),
                damage = wp.entity.baseDamage,
                isCrit = false,
                color = Color(0xFF00F0FF),
                radius = 0.25f,
                isHoming = true,
                targetEnemyId = target?.id
            )
            _projectiles.value = _projectiles.value + proj
            shotsHit.value++
        } else {
            // Single shot / Railgun / Rifle
            val didHit = testAndDamageEnemies(crossX, crossY, screenWidth, screenHeight, wp.entity.baseDamage)
            if (didHit) shotsHit.value++
        }
    }

    private fun testAndDamageEnemies(
        screenX: Float,
        screenY: Float,
        screenWidth: Float,
        screenHeight: Float,
        baseDamage: Float
    ): Boolean {
        var closestHitEnemy: Enemy? = null
        var closestDist = 9999f
        var hitIsHead = false

        for (enemy in _enemies.value) {
            if (enemy.state == EnemyState.DEAD) continue
            val hitRes = Game3DMath.testAimHit(
                aimScreenX = screenX,
                aimScreenY = screenY,
                targetPos = enemy.pos,
                camera = camera,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                targetRadius = enemy.type.bodyRadius,
                headHeightRatio = enemy.type.headRatio
            )
            if (hitRes.hit && hitRes.distance < closestDist) {
                closestDist = hitRes.distance
                closestHitEnemy = enemy
                hitIsHead = hitRes.isHeadshot
            }
        }

        if (closestHitEnemy != null) {
            applyDamageToEnemy(closestHitEnemy, baseDamage, hitIsHead)
            particleSystem.spawnHitSparks(closestHitEnemy.pos, hitIsHead, if (hitIsHead) 20 else 8)
            return true
        }
        return false
    }

    private fun applyDamageToEnemy(enemy: Enemy, damage: Float, isHeadshot: Boolean) {
        val multiplier = if (isHeadshot) 2.2f else 1.0f
        val finalDmg = damage * multiplier

        if (isHeadshot) {
            combatHeadshots.value++
            soundSystem.playHeadshotCrit()
            particleSystem.addDamagePopup("CRITICAL -${finalDmg.toInt()}", enemy.pos, true)
        } else {
            soundSystem.playHitMarker()
            particleSystem.addDamagePopup("-${finalDmg.toInt()}", enemy.pos, false)
        }

        // Damage shield first
        if (enemy.shield > 0f) {
            enemy.shield -= finalDmg
            if (enemy.shield < 0f) {
                val excess = -enemy.shield
                enemy.shield = 0f
                enemy.health -= excess
                soundSystem.playShieldBreak()
            }
        } else {
            enemy.health -= finalDmg
        }

        enemy.hurtTimer = 0.2f

        if (enemy.health <= 0f) {
            enemy.health = 0f
            enemy.state = EnemyState.DEAD
            combatKills.value++
            combatScore.value += enemy.type.scoreValue * (if (isHeadshot) 2 else 1)
            soundSystem.playEnemyDeath()
            particleSystem.spawnExplosion(enemy.pos, enemy.type.bodyRadius * 1.5f)

            // Boss defeated special dialog
            if (enemy.type == EnemyType.GOLIATH_BOSS) {
                _activeDialogue.value = StoryDialogue(
                    speakerName = "艾琳娜·克罗斯",
                    speakerRole = "特战指挥官",
                    avatarType = "ELENA",
                    dialogueText = "漂亮！歌利亚超重型泰坦核心反应堆已被引爆！克洛诺斯的外围防线彻底瓦解，先锋特战队，准备迎接撤离运输机！",
                    audioFreq = 800f
                )
            }
        }
    }

    fun reloadWeapon() {
        val wp = _currentWeaponRuntime.value ?: return
        if (wp.isReloading || wp.currentAmmo == wp.entity.magCapacity) return
        wp.isReloading = true
        wp.reloadTimer = wp.entity.reloadTimeSec
        soundSystem.playRailgunCharge()
    }

    fun cycleWeapon() {
        val unlockedList = weapons.value.filter { it.isUnlocked }
        if (unlockedList.size <= 1) return

        val currentId = _currentWeaponRuntime.value?.entity?.id
        val currentIndex = unlockedList.indexOfFirst { it.id == currentId }
        val nextIndex = (currentIndex + 1) % unlockedList.size
        val nextWeapon = unlockedList[nextIndex]

        _currentWeaponRuntime.value = WeaponRuntime(
            entity = nextWeapon,
            currentAmmo = nextWeapon.magCapacity
        )
        soundSystem.playHitMarker()
    }

    fun activateBulletTime() {
        if (bulletTimeCooldown.value > 0f || isBulletTimeActive.value) return
        isBulletTimeActive.value = true
        bulletTimeRemaining.value = 4.0f
        timeDilation = 0.25f // 75% slow motion!
        soundSystem.playBulletTime()
    }

    fun activateEmpBlast() {
        if (empCooldown.value > 0f) return
        empCooldown.value = 15f
        soundSystem.playShieldBreak()
        camera.screenShakeX = 18f
        camera.screenShakeY = 18f

        // Stun and strip shields of all alive enemies
        for (enemy in _enemies.value) {
            if (enemy.state != EnemyState.DEAD) {
                enemy.shield = 0f
                enemy.hurtTimer = 1.5f
                particleSystem.spawnHitSparks(enemy.pos, true, 24)
            }
        }
    }

    fun activateNaniteHeal() {
        if (healCooldown.value > 0f) return
        healCooldown.value = 20f
        playerHp.value = (playerHp.value + 40f).coerceAtMost(playerMaxHp.value)
        playerShield.value = playerMaxShield.value
        soundSystem.playHitMarker()
    }

    fun toggleThermalScope() {
        isThermalScope.value = !isThermalScope.value
        soundSystem.playHitMarker()
    }

    private fun damagePlayer(damage: Float) {
        soundSystem.playPlayerHit()
        camera.screenShakeX = (Random.nextFloat() - 0.5f) * 16f
        camera.screenShakeY = (Random.nextFloat() - 0.5f) * 16f

        if (playerShield.value > 0f) {
            playerShield.value -= damage
            if (playerShield.value < 0f) {
                val excess = -playerShield.value
                playerShield.value = 0f
                playerHp.value -= excess
                soundSystem.playShieldBreak()
            }
        } else {
            playerHp.value -= damage
        }

        if (playerHp.value <= 0f) {
            playerHp.value = 0f
            triggerDefeat()
        }
    }

    private fun triggerVictory() {
        stopCombat()
        val mission = _activeMission.value ?: return
        val acc = if (shotsFired.value > 0) ((shotsHit.value.toFloat() / shotsFired.value) * 100).toInt() else 85
        val stars = when {
            acc >= 80 && playerHp.value >= 70f -> 3
            acc >= 60 -> 2
            else -> 1
        }

        viewModelScope.launch {
            repository.recordMissionResult(
                missionId = mission.id,
                score = combatScore.value,
                accuracy = acc,
                headshots = combatHeadshots.value,
                kills = combatKills.value,
                stars = stars,
                victory = true
            )
        }

        _debriefData.value = DebriefData(
            mission = mission,
            isVictory = true,
            score = combatScore.value,
            kills = combatKills.value,
            headshots = combatHeadshots.value,
            accuracy = acc,
            starsEarned = stars,
            creditsEarned = mission.rewardCredits,
            nanitesEarned = mission.rewardNanites
        )
        _currentScreen.value = AppScreen.DEBRIEF
    }

    private fun triggerDefeat() {
        stopCombat()
        val mission = _activeMission.value ?: return
        val acc = if (shotsFired.value > 0) ((shotsHit.value.toFloat() / shotsFired.value) * 100).toInt() else 50

        viewModelScope.launch {
            repository.recordMissionResult(
                missionId = mission.id,
                score = combatScore.value,
                accuracy = acc,
                headshots = combatHeadshots.value,
                kills = combatKills.value,
                stars = 0,
                victory = false
            )
        }

        _debriefData.value = DebriefData(
            mission = mission,
            isVictory = false,
            score = combatScore.value,
            kills = combatKills.value,
            headshots = combatHeadshots.value,
            accuracy = acc,
            starsEarned = 0,
            creditsEarned = 350,
            nanitesEarned = 20
        )
        _currentScreen.value = AppScreen.DEBRIEF
    }

    fun stopCombat() {
        combatLoopJob?.cancel()
        combatLoopJob = null
        timeDilation = 1.0f
        isBulletTimeActive.value = false
    }

    fun equipWeapon(weaponId: String) {
        viewModelScope.launch {
            repository.updateEquippedWeapon(weaponId)
            val wp = weapons.value.find { it.id == weaponId }
            if (wp != null) {
                _currentWeaponRuntime.value = WeaponRuntime(wp, wp.magCapacity)
            }
        }
    }

    fun upgradeWeapon(weaponId: String) {
        viewModelScope.launch {
            repository.upgradeWeapon(weaponId)
        }
    }

    fun unlockWeapon(weaponId: String) {
        viewModelScope.launch {
            repository.unlockWeapon(weaponId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopCombat()
    }
}
