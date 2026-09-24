package com.example.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 {
        val l = length()
        return if (l > 0.0001f) Vec3(x / l, y / l, z / l) else Vec3(0f, 0f, 1f)
    }
    fun distanceTo(other: Vec3): Float = (this - other).length()
}

data class Camera3D(
    var pos: Vec3 = Vec3(0f, 1.6f, 0f),
    var yaw: Float = 0f, // radians, horizontal turn
    var pitch: Float = 0f, // radians, vertical look
    var roll: Float = 0f,
    var fov: Float = 65f, // degrees
    var recoilPitch: Float = 0f,
    var recoilYaw: Float = 0f,
    var screenShakeX: Float = 0f,
    var screenShakeY: Float = 0f
)

data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val scale: Float,
    val depth: Float,
    val inFront: Boolean
)

object Game3DMath {
    /**
     * Projects a 3D world coordinate into 2D screen coordinate using camera yaw and pitch.
     */
    fun project(
        worldPos: Vec3,
        camera: Camera3D,
        screenWidth: Float,
        screenHeight: Float
    ): ProjectedPoint {
        // Translate relative to camera
        val rx = worldPos.x - camera.pos.x
        val ry = worldPos.y - camera.pos.y
        val rz = worldPos.z - camera.pos.z

        // Effective angles with recoil and shake
        val effYaw = camera.yaw + camera.recoilYaw
        val effPitch = camera.pitch + camera.recoilPitch

        val cy = cos(-effYaw)
        val sy = sin(-effYaw)
        val cp = cos(-effPitch)
        val sp = sin(-effPitch)

        // Rotate around Y axis (Yaw)
        val x1 = rx * cy - rz * sy
        val z1 = rx * sy + rz * cy
        val y1 = ry

        // Rotate around X axis (Pitch)
        val y2 = y1 * cp - z1 * sp
        val z2 = y1 * sp + z1 * cp
        val x2 = x1

        val inFront = z2 > 0.3f
        if (!inFront) {
            return ProjectedPoint(0f, 0f, 0f, z2, false)
        }

        // Perspective divide
        val fovFactor = (screenWidth / 2f) / kotlin.math.tan(Math.toRadians(camera.fov.toDouble() / 2.0)).toFloat()
        val scale = fovFactor / z2

        val cx = screenWidth / 2f + camera.screenShakeX
        val cyScreen = screenHeight / 2f + camera.screenShakeY

        val screenX = cx + x2 * scale
        val screenY = cyScreen - y2 * scale

        return ProjectedPoint(screenX, screenY, scale, z2, true)
    }

    /**
     * Test raycast intersection between crosshair center (cx, cy) and enemy screen bounds.
     */
    fun testAimHit(
        aimScreenX: Float,
        aimScreenY: Float,
        targetPos: Vec3,
        camera: Camera3D,
        screenWidth: Float,
        screenHeight: Float,
        targetRadius: Float = 0.9f,
        headHeightRatio: Float = 0.75f
    ): HitResult {
        val proj = project(targetPos, camera, screenWidth, screenHeight)
        if (!proj.inFront) return HitResult(false, false, 999f)

        val screenRadius = targetRadius * proj.scale
        val dx = aimScreenX - proj.screenX
        val dy = aimScreenY - proj.screenY
        val distSq = dx * dx + dy * dy

        if (distSq <= screenRadius * screenRadius) {
            // Check if headshot: top 25% of target body
            val isHeadshot = dy < -screenRadius * (headHeightRatio - 0.5f)
            return HitResult(true, isHeadshot, proj.depth)
        }
        return HitResult(false, false, proj.depth)
    }
}

data class HitResult(
    val hit: Boolean,
    val isHeadshot: Boolean,
    val distance: Float
)
