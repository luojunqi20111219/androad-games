package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.data.db.WeaponEntity

enum class EnemyType(
    val displayName: String,
    val maxHealth: Float,
    val maxShield: Float,
    val speed: Float,
    val damagePerAttack: Float,
    val attackInterval: Float,
    val scoreValue: Int,
    val bodyRadius: Float,
    val headRatio: Float
) {
    RECON_DRONE(
        displayName = "猎隼-III 空侦机",
        maxHealth = 60f,
        maxShield = 0f,
        speed = 2.2f,
        damagePerAttack = 12f,
        attackInterval = 1.8f,
        scoreValue = 250,
        bodyRadius = 0.75f,
        headRatio = 0.5f
    ),
    CYBER_STALKER(
        displayName = "暗影-7 仿生突击体",
        maxHealth = 110f,
        maxShield = 30f,
        speed = 1.6f,
        damagePerAttack = 18f,
        attackInterval = 1.5f,
        scoreValue = 420,
        bodyRadius = 0.85f,
        headRatio = 0.78f
    ),
    HEAVY_ENFORCER(
        displayName = "铁卫-X 重装执法者",
        maxHealth = 260f,
        maxShield = 80f,
        speed = 0.9f,
        damagePerAttack = 26f,
        attackInterval = 2.2f,
        scoreValue = 850,
        bodyRadius = 1.15f,
        headRatio = 0.82f
    ),
    GOLIATH_BOSS(
        displayName = "歌利亚-MK9 毁灭泰坦 [BOSS]",
        maxHealth = 1800f,
        maxShield = 500f,
        speed = 0.55f,
        damagePerAttack = 38f,
        attackInterval = 1.6f,
        scoreValue = 5000,
        bodyRadius = 2.4f,
        headRatio = 0.72f
    )
}

enum class EnemyState {
    SPAWNING,
    ADVANCING,
    AIMING,
    ATTACKING,
    HURT,
    DEAD
}

data class Enemy(
    val id: Long,
    val type: EnemyType,
    var pos: Vec3,
    var health: Float,
    var maxHealth: Float,
    var shield: Float,
    var maxShield: Float,
    var state: EnemyState = EnemyState.SPAWNING,
    var attackTimer: Float = 0f,
    var hurtTimer: Float = 0f,
    var deathTimer: Float = 0.5f,
    var animPhase: Float = 0f,
    var dodgeOffsetX: Float = 0f,
    var isShieldActive: Boolean = true
)

data class Projectile(
    val id: Long,
    val fromPlayer: Boolean,
    var pos: Vec3,
    var vel: Vec3,
    val damage: Float,
    val isCrit: Boolean,
    val color: Color,
    var life: Float = 2.0f,
    val radius: Float = 0.15f,
    val isHoming: Boolean = false,
    val targetEnemyId: Long? = null
)

data class DialogueChoice(
    val text: String,
    val outcomeNote: String,
    val grantCredits: Int = 0,
    val grantNanites: Int = 0,
    val buffType: String? = null // e.g. "OVERCHARGE", "AIR_STRIKE", "REPAIR"
)

data class StoryDialogue(
    val speakerName: String,
    val speakerRole: String,
    val avatarType: String, // "ELENA", "ARES_AI", "CHRONOS_BOSS"
    val dialogueText: String,
    val audioFreq: Float = 600f,
    val choices: List<DialogueChoice> = emptyList(),
    val onDismiss: (() -> Unit)? = null
)

data class WeaponRuntime(
    val entity: WeaponEntity,
    var currentAmmo: Int,
    var isReloading: Boolean = false,
    var reloadTimer: Float = 0f,
    var fireCooldown: Float = 0f,
    var chargeRatio: Float = 0f
)
