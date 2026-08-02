package com.go4no.ryoperformance.mixin;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.go4no.ryoperformance.BenchmarkRecorder;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.MonitorTracker;
import org.lwjgl.glfw.GLFW;
@Mixin(Window.class) public class WindowMixin {
 @Inject(method="<init>", at=@At(value="INVOKE", target="Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", shift=At.Shift.BEFORE, remap=false))
 private void ryo$hideBenchmarkWindow(WindowEventHandler handler, MonitorTracker monitors, WindowSettings settings, String videoMode, String title, CallbackInfo ci){ GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); }
 @Inject(method="swapBuffers", at=@At("HEAD")) private void ryo$swap(CallbackInfo ci){ BenchmarkRecorder.swap(); }
}
