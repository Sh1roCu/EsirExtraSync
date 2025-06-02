package cn.sh1rocu.esirextrasync.mixin.compat.armourers_workshop;

import cn.sh1rocu.esirextrasync.listener.EventListener;
import moe.plushie.armourers_workshop.core.item.SkinItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkinItem.class)
public class SkinItemMixin {
    @Inject(method = "use", at = @At(remap = false, value = "INVOKE", target = "Lmoe/plushie/armourers_workshop/core/capability/SkinWardrobe;setItem(Lmoe/plushie/armourers_workshop/core/data/slot/SkinSlotType;ILnet/minecraft/item/ItemStack;)V"), cancellable = true)
    private void esir$use(World level, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        if (EventListener.IS_SYNCING.contains(player.getStringUUID())) {
            player.sendMessage(new StringTextComponent("正在同步数据，请稍后再尝试穿戴时装").withStyle(TextFormatting.RED), Util.NIL_UUID);
            cir.setReturnValue(ActionResult.pass(player.getItemInHand(hand)));
        }
    }
}
