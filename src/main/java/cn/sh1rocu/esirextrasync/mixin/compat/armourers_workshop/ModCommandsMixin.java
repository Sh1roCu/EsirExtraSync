package cn.sh1rocu.esirextrasync.mixin.compat.armourers_workshop;

import cn.sh1rocu.esirextrasync.listener.EventListener;
import com.mojang.brigadier.context.CommandContext;
import moe.plushie.armourers_workshop.core.capability.SkinWardrobe;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(targets = "moe.plushie.armourers_workshop.init.ModCommands$Executor")
public class ModCommandsMixin {
    @Inject(remap = false, locals = LocalCapture.CAPTURE_FAILHARD, method = "openWardrobe", at = @At(value = "INVOKE", target = "Lmoe/plushie/armourers_workshop/init/platform/MenuManager;openMenu(Lmoe/plushie/armourers_workshop/api/registry/IRegistryKey;Lnet/minecraft/entity/player/PlayerEntity;Ljava/lang/Object;)Z"), cancellable = true)
    private static void esir$openWardrobe(CommandContext<CommandSource> context, CallbackInfoReturnable<Integer> cir, PlayerEntity player, Iterator<Entity> var2, Entity entity, SkinWardrobe wardrobe) {
        if (EventListener.IS_SYNCING.contains(player.getStringUUID())) {
            player.sendMessage(new StringTextComponent("正在同步数据，请稍后再打开时装GUI").withStyle(TextFormatting.RED), Util.NIL_UUID);
            cir.setReturnValue(1);
        } else if (entity instanceof PlayerEntity && EventListener.IS_SYNCING.contains(entity.getStringUUID())) {
            player.sendMessage(new StringTextComponent("目标正在同步数据，请稍后再打开时装GUI").withStyle(TextFormatting.RED), Util.NIL_UUID);
            cir.setReturnValue(1);
        }
    }
}
