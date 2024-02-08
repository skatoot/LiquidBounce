/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import baritone.api.BaritoneAPI
import baritone.api.pathing.goals.GoalNear
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleChestAura
import net.ccbluex.liquidbounce.utils.baritone.baritoneInstalled
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

/**
 * A fight bot, fights for you, probably better than you. Lol.
 */
object Baritone : ToggleableConfigurable(ModuleKillAura, "Baritone", false) {

    fun followTargets(target: Set<LivingEntity>) {
        if (!enabled) return

        if (!baritoneInstalled) {
            chat("§cBaritone is not installed.")
            return
        }

        val baritone = BaritoneAPI.getProvider().primaryBaritone

        if (mc.currentScreen is HandledScreen<*>) {
            baritone.pathingBehavior.cancelEverything()
            return
        }

        baritone.followProcess.follow { it in target }
    }

    fun isNearTarget(target: Entity) {
        // Calculate location behind the target
        val targetRotation = target.rotation
        val targetPosition = target.pos

        val x = targetPosition.x + sin(Math.toRadians(targetRotation.yaw.toDouble())) * 2
        val z = targetPosition.z - cos(Math.toRadians(targetRotation.yaw.toDouble())) * 2

        val bestPosition = Vec3d(x, targetPosition.y, z).toBlockPos()

        if (!enabled) return

        if (!baritoneInstalled) {
            chat("§cBaritone is not installed.")
            return
        }

        val baritone = BaritoneAPI.getProvider().primaryBaritone

        baritone.customGoalProcess.setGoalAndPath(GoalNear(bestPosition, 3))

    }

}
