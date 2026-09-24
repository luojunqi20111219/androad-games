package com.example.engine

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class ParticleType {
    SPARK,
    SMOKE,
    PLASMA,
    SHOCKWAVE,
    SHELL_CASING
}

data class Particle(
    var pos: Vec3,
    var vel: Vec3,
    var color: Color,
    var size: Float,
    var life: Float,
    var maxLife: Float,
    val type: ParticleType,
    var rotAngle: Float = 0f,
    var rotSpeed: Float = 0f
)

data class DamagePopup(
    val id: Long,
    val text: String,
    val worldPos: Vec3,
    val color: Color,
    val isCritical: Boolean,
    var life: Float = 1.0f,
    var offsetY: Float = 0f
)

class ParticleSystem {
    private val particles = mutableListOf<Particle>()
    private val popups = mutableListOf<DamagePopup>()
    private var nextPopupId = 1L

    fun getParticles(): List<Particle> = particles
    fun getPopups(): List<DamagePopup> = popups

    fun update(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life -= dt
            if (p.life <= 0f) {
                iter.remove()
                continue
            }
            p.pos = p.pos + (p.vel * dt)
            p.rotAngle += p.rotSpeed * dt

            // Gravity on sparks and shell casings
            if (p.type == ParticleType.SPARK || p.type == ParticleType.SHELL_CASING) {
                p.vel = Vec3(p.vel.x, p.vel.y - 9.8f * dt, p.vel.z)
            } else if (p.type == ParticleType.SMOKE) {
                p.vel = Vec3(p.vel.x * 0.95f, p.vel.y + 0.5f * dt, p.vel.z * 0.95f)
            }
        }

        val popupIter = popups.iterator()
        while (popupIter.hasNext()) {
            val popup = popupIter.next()
            popup.life -= dt * 1.2f
            popup.offsetY += dt * 0.8f
            if (popup.life <= 0f) {
                popupIter.remove()
            }
        }
    }

    fun spawnHitSparks(worldPos: Vec3, isCrit: Boolean, count: Int = 12) {
        val baseColor = if (isCrit) Color(0xFFFFB800) else Color(0xFF00F0FF)
        for (i in 0 until count) {
            val vx = (Random.nextFloat() - 0.5f) * 6f
            val vy = Random.nextFloat() * 5f + 1f
            val vz = (Random.nextFloat() - 0.5f) * 6f
            particles.add(
                Particle(
                    pos = worldPos,
                    vel = Vec3(vx, vy, vz),
                    color = baseColor,
                    size = if (isCrit) 5f else 3.5f,
                    life = Random.nextFloat() * 0.35f + 0.15f,
                    maxLife = 0.5f,
                    type = ParticleType.SPARK
                )
            )
        }
    }

    fun spawnExplosion(worldPos: Vec3, radius: Float = 1.5f) {
        // Shockwave ring
        particles.add(
            Particle(
                pos = worldPos,
                vel = Vec3(0f, 0f, 0f),
                color = Color(0xFFFF4400),
                size = radius,
                life = 0.4f,
                maxLife = 0.4f,
                type = ParticleType.SHOCKWAVE
            )
        )

        // Fiery sparks
        for (i in 0 until 24) {
            val vx = (Random.nextFloat() - 0.5f) * 12f
            val vy = (Random.nextFloat() - 0.2f) * 10f
            val vz = (Random.nextFloat() - 0.5f) * 12f
            particles.add(
                Particle(
                    pos = worldPos,
                    vel = Vec3(vx, vy, vz),
                    color = if (Random.nextBoolean()) Color(0xFFFF3300) else Color(0xFFFFCC00),
                    size = Random.nextFloat() * 6f + 3f,
                    life = Random.nextFloat() * 0.5f + 0.2f,
                    maxLife = 0.7f,
                    type = ParticleType.SPARK
                )
            )
        }

        // Smoke billows
        for (i in 0 until 8) {
            val vx = (Random.nextFloat() - 0.5f) * 2f
            val vy = Random.nextFloat() * 2f + 0.5f
            val vz = (Random.nextFloat() - 0.5f) * 2f
            particles.add(
                Particle(
                    pos = worldPos,
                    vel = Vec3(vx, vy, vz),
                    color = Color(0x88334155),
                    size = Random.nextFloat() * 8f + 6f,
                    life = Random.nextFloat() * 0.8f + 0.4f,
                    maxLife = 1.2f,
                    type = ParticleType.SMOKE
                )
            )
        }
    }

    fun spawnCasingEjection(cameraPos: Vec3, cameraYaw: Float) {
        val rightX = kotlin.math.cos(cameraYaw) * 0.35f
        val rightZ = kotlin.math.sin(cameraYaw) * 0.35f
        val casingPos = Vec3(cameraPos.x + rightX, cameraPos.y - 0.25f, cameraPos.z + rightZ + 0.5f)
        val vx = rightX * 3f + (Random.nextFloat() - 0.5f) * 0.5f
        val vy = 2.2f + Random.nextFloat() * 0.8f
        val vz = rightZ * 3f - 1.0f
        particles.add(
            Particle(
                pos = casingPos,
                vel = Vec3(vx, vy, vz),
                color = Color(0xFFFFCC00),
                size = 4f,
                life = 0.6f,
                maxLife = 0.6f,
                type = ParticleType.SHELL_CASING,
                rotAngle = Random.nextFloat() * 360f,
                rotSpeed = 720f
            )
        )
    }

    fun addDamagePopup(text: String, worldPos: Vec3, isCrit: Boolean) {
        popups.add(
            DamagePopup(
                id = nextPopupId++,
                text = text,
                worldPos = worldPos,
                color = if (isCrit) Color(0xFFFF0055) else Color(0xFF00F0FF),
                isCritical = isCrit
            )
        )
    }

    fun clear() {
        particles.clear()
        popups.clear()
    }
}
