package com.go4no.ryoperformance.mixin;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.go4no.ryoperformance.BenchmarkRecorder;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MinecraftClient.class) public class MinecraftClientMixin {
 @Inject(method="render", at=@At("HEAD")) private void ryo$start(boolean tick, CallbackInfo ci){ BenchmarkRecorder.beforeRender(); }
 @Inject(method="render", at=@At("RETURN")) private void ryo$end(boolean tick, CallbackInfo ci){ BenchmarkRecorder.afterRender(); }
 @Inject(method="getFramerateLimit", at=@At("HEAD"), cancellable=true) private void ryo$uncap(CallbackInfoReturnable<Integer> cir){ cir.setReturnValue(10000); }
}
