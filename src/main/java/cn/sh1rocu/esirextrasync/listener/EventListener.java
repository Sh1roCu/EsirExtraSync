package cn.sh1rocu.esirextrasync.listener;

import cn.sh1rocu.esirextrasync.util.DBThreadPoolFactory;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod.EventBusSubscriber
public class EventListener {
    static ExecutorService executorService = Executors.newCachedThreadPool(new DBThreadPoolFactory("EsirExtraSync"));

    public static void doPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) throws SQLException, CommandSyntaxException {
        PlayerEntity player = event.getPlayer();
        AoASkillSyncListener.doPlayerJoin(player);
        ArmourersSyncListener.doPlayerJoin(player);
        DietSyncListener.doPlayerJoin(player);
        LegendarySurvivalSyncListener.doPlayerJoin(player);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        executorService.submit(() -> {
            try {
                doPlayerJoin(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    public static void doPlayerSaveToFile(PlayerEvent.SaveToFile event) throws SQLException {
        PlayerEntity player = event.getPlayer();
        AoASkillSyncListener.doPlayerSaveToFile(player);
        ArmourersSyncListener.doPlayerSaveToFile(player);
        DietSyncListener.doPlayerSaveToFile(player);
        LegendarySurvivalSyncListener.doPlayerSaveToFile(player);
    }

    @SubscribeEvent
    public static void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        executorService.submit(() -> {
            try {
                doPlayerSaveToFile(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void doPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) throws SQLException {
        PlayerEntity player = event.getPlayer();
        AoASkillSyncListener.doPlayerLogout(player);
        ArmourersSyncListener.doPlayerLogout(player);
        DietSyncListener.doPlayerLogout(player);
        LegendarySurvivalSyncListener.doPlayerLogout(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        executorService.submit(() -> {
            try {
                doPlayerLogout(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}
