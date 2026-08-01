package com.go4no.ryoblocks.mixin.client;

import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin {
    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J",
            shift = At.Shift.BEFORE,
            remap = false
        )
    )
    private void ryoBlocks$hideVisualProofWindow(
        WindowEventHandler eventHandler,
        MonitorTracker monitorTracker,
        WindowSettings settings,
        String videoMode,
        String title,
        CallbackInfo ci
    ) {
        if (Boolean.getBoolean("ryoBlocks.visualProof")) {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        }
    }
}
