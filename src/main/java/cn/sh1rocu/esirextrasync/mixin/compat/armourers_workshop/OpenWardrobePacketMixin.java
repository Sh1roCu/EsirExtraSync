package cn.sh1rocu.esirextrasync.mixin.compat.armourers_workshop;

import cn.sh1rocu.esirextrasync.listener.EventListener;
import moe.plushie.armourers_workshop.api.network.IServerPacketHandler;
import moe.plushie.armourers_workshop.core.network.OpenWardrobePacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OpenWardrobePacket.class)
public class OpenWardrobePacketMixin {
    @Inject(remap = false, method = "accept", at = @At("HEAD"), cancellable = true)
    private void esir$accept(IServerPacketHandler packetHandler, ServerPlayerEntity player, CallbackInfo ci) {
        if (EventListener.IS_SYNCING.contains(player.getStringUUID())) {
            player.sendMessage(new StringTextComponent("正在同步数据，请稍后再打开时装GUI").withStyle(TextFormatting.RED), Util.NIL_UUID);
            ci.cancel();
        }
    }
}
