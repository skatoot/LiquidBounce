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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.common.OutlineFlag;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.render.engine.Color4b;
import net.ccbluex.liquidbounce.render.engine.RenderingFlags;
import net.ccbluex.liquidbounce.render.shader.shaders.OutlineShader;
import net.ccbluex.liquidbounce.utils.client.ClientUtilsKt;
import net.ccbluex.liquidbounce.utils.combat.CombatExtensionsKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow
    @Nullable
    public Framebuffer entityOutlinesFramebuffer;

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;

    @Shadow
    public abstract @Nullable Framebuffer getEntityOutlinesFramebuffer();

    @Shadow
    protected abstract boolean canDrawEntityOutlines();

    @Inject(method = "loadEntityOutlinePostProcessor", at = @At("RETURN"))
    private void onLoadEntityOutlineShader(CallbackInfo info) {
        try {
            OutlineShader.INSTANCE.load();
        } catch (Throwable e) {
            ClientUtilsKt.getLogger().error("Failed to load outline shader", e);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix, CallbackInfo ci) {
        try {
            OutlineShader outlineShader = OutlineShader.INSTANCE;

            outlineShader.begin(2.0F);

            outlineShader.getFramebuffer().beginWrite(false);

            var event = new DrawOutlinesEvent(matrices, camera, tickDelta, DrawOutlinesEvent.OutlineType.INBUILT_OUTLINE);

            EventManager.INSTANCE.callEvent(event);

            if (event.getDirtyFlag()) {
                outlineShader.setDirty();
            }

            MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
        } catch (Throwable e) {
            ClientUtilsKt.getLogger().error("Failed to begin outline shader", e);
        }
    }

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void injectOutlineESP(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        if (RenderingFlags.isCurrentlyRenderingEntityOutline().get()) {
            return;
        }

        Color4b color;

        // Prevent stack overflow
        if (ModuleESP.INSTANCE.getEnabled() && ModuleESP.OutlineMode.INSTANCE.isActive() && entity instanceof LivingEntity && CombatExtensionsKt.shouldBeShown(entity)) {
            color = ModuleESP.INSTANCE.getColor((LivingEntity) entity);
        } else if (ModuleItemESP.INSTANCE.getEnabled() && ModuleItemESP.OutlineMode.INSTANCE.isActive() && ModuleItemESP.INSTANCE.shouldRender(entity)) {
            color = ModuleItemESP.INSTANCE.getColor();
        } else {
            return;
        }

        OutlineShader outlineShader = OutlineShader.INSTANCE;
        Framebuffer originalBuffer = this.entityOutlinesFramebuffer;

        this.entityOutlinesFramebuffer = outlineShader.getFramebuffer();

        outlineShader.setColor(color);
        outlineShader.setDirty();

        RenderingFlags.isCurrentlyRenderingEntityOutline().set(true);

        try {
            renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, outlineShader.getVertexConsumerProvider());
        } finally {
            RenderingFlags.isCurrentlyRenderingEntityOutline().set(false);
        }

        this.entityOutlinesFramebuffer = originalBuffer;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void onDrawOutlines(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo info) {
//        if (!(ModuleESP.INSTANCE.getEnabled() && ModuleESP.OutlineMode.INSTANCE.isActive()) && !(ModuleItemESP.INSTANCE.getEnabled() && ModuleItemESP.OutlineMode.INSTANCE.isActive())) {
//            return;
//        }

        OutlineShader.INSTANCE.end(tickDelta);
    }

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(IIZ)V"))
    private void onDrawEntityOutlinesFramebuffer(CallbackInfo info) {
//        if (!(ModuleESP.INSTANCE.getEnabled() && ModuleESP.OutlineMode.INSTANCE.isActive()) && !(ModuleItemESP.INSTANCE.getEnabled() && ModuleItemESP.OutlineMode.INSTANCE.isActive())) {
//            return;
//        }

        if (OutlineShader.INSTANCE.isDirty()) {
            OutlineShader.INSTANCE.drawFramebuffer();
        }
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void onResized(int w, int h, CallbackInfo info) {
        OutlineShader.INSTANCE.onResized(w, h);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isSleeping()Z"))
    private boolean hookFreeCamRenderPlayerFromAllPerspectives(LivingEntity instance) {
        return ModuleFreeCam.INSTANCE.renderPlayerFromAllPerspectives(instance);
    }

    /**
     * Enables an outline glow when ESP is enabled and glow mode is active
     *
     * @author 1zuna
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    private boolean injectHasOutline(MinecraftClient instance, Entity entity) {
        if (ModuleItemESP.INSTANCE.getEnabled() && ModuleItemESP.GlowMode.INSTANCE.isActive() && ModuleItemESP.INSTANCE.shouldRender(entity)) {
            return true;
        }
        if (ModuleESP.INSTANCE.getEnabled() && ModuleESP.GlowMode.INSTANCE.isActive() && CombatExtensionsKt.shouldBeShown(entity)) {
            return true;
        }

        if (ModuleStorageESP.INSTANCE.getEnabled() && ModuleStorageESP.Glow.INSTANCE.isActive() &&
                ModuleStorageESP.INSTANCE.categorizeEntity(entity) != null) {
            return true;
        }

        return instance.hasOutline(entity);
    }

    /**
     * Inject ESP color as glow color
     *
     * @author 1zuna
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
    private int injectTeamColor(Entity instance) {
        if (ModuleItemESP.INSTANCE.getEnabled() && ModuleItemESP.GlowMode.INSTANCE.isActive() && ModuleItemESP.INSTANCE.shouldRender(instance)) {
            return ModuleItemESP.INSTANCE.getColor().toRGBA();
        }
        if (ModuleStorageESP.INSTANCE.getEnabled() && ModuleStorageESP.Glow.INSTANCE.isActive()) {
            var categorizedEntity = ModuleStorageESP.INSTANCE.categorizeEntity(instance);
            if (categorizedEntity != null) {
                return categorizedEntity.getColor().invoke().toRGBA();
            }
        }

        if (instance instanceof LivingEntity && ModuleESP.INSTANCE.getEnabled()
                && ModuleESP.GlowMode.INSTANCE.isActive()) {
            final Color4b color = ModuleESP.INSTANCE.getColor((LivingEntity) instance);
            return color.toRGBA();
        }

        return instance.getTeamColorValue();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V", shift = At.Shift.BEFORE))
    private void onRenderOutline(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (!this.canDrawEntityOutlines()) {
            return;
        }

        this.getEntityOutlinesFramebuffer().beginWrite(false);

        var event = new DrawOutlinesEvent(matrices, camera, tickDelta, DrawOutlinesEvent.OutlineType.MINECRAFT_GLOW);

        EventManager.INSTANCE.callEvent(event);

        OutlineFlag.drawOutline |= event.getDirtyFlag();

        MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
    }

    @ModifyVariable(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilderStorage;getOutlineVertexConsumers()Lnet/minecraft/client/render/OutlineVertexConsumerProvider;",
                    ordinal = 1),
            ordinal = 3,
            name = "bl3",
            require = 1
    )
    private boolean hookOutlineFlag(boolean bl3) {
        if (OutlineFlag.drawOutline) {
            OutlineFlag.drawOutline = false;
            return true;
        }

        return bl3;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"), index = 3)
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        return ModuleFreeCam.INSTANCE.getEnabled() || spectator;
    }

    @ModifyVariable(method = "renderWeather", at = @At(value = "STORE"), name = "precipitation", ordinal = 0, require = 1, allow = 1)
    private Biome.Precipitation modifyBiomePrecipitation(Biome.Precipitation precipitation) {
        var moduleOverrideWeather = ModuleOverrideWeather.INSTANCE;
        if (moduleOverrideWeather.getEnabled() && moduleOverrideWeather.getWeather().get() == ModuleOverrideWeather.WeatherType.SNOWY) {
            return Biome.Precipitation.SNOW;
        }
        return precipitation;
    }

    @ModifyVariable(method = "tickRainSplashing", at = @At("STORE"), ordinal = 0, require = 1, allow = 1)
    private float removeRainSplashing(float f) {
        var moduleOverrideWeather = ModuleOverrideWeather.INSTANCE;
        if (moduleOverrideWeather.getEnabled() && moduleOverrideWeather.getWeather().get() == ModuleOverrideWeather.WeatherType.SNOWY) {
            return 0f;
        }
        return f;
    }
}
