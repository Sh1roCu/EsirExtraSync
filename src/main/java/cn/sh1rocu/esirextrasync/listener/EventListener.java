package cn.sh1rocu.esirextrasync.listener;

import cn.sh1rocu.esirextrasync.EsirExtraSync;
import cn.sh1rocu.esirextrasync.util.DBController;
import cn.sh1rocu.esirextrasync.util.DBThreadPoolFactory;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod.EventBusSubscriber
public class EventListener {
    static ExecutorService executorService = Executors.newCachedThreadPool(new DBThreadPoolFactory("EsirExtraSync"));

    public static void doPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) throws SQLException, CommandSyntaxException, InterruptedException {
        PlayerEntity player = event.getPlayer();
        String uuid = player.getStringUUID();
        DBController.QueryResult queryResult = DBController.executeQuery("SELECT * FROM sync_status WHERE uuid='" + uuid + "';");
        ResultSet resultSet = queryResult.getResultSet();
        if (!resultSet.next()) {
            DBController.executeUpdate("INSERT INTO sync_status(uuid,is_syncing) " +
                    "VALUES(?,0)", uuid);
        } else {
            boolean isSyncing = resultSet.getBoolean("is_syncing");
            EsirExtraSync.LOGGER.info("检测玩家{}{{}}的数据库数据同步状态...", player.getName().getString(), uuid);
            EsirExtraSync.LOGGER.info("{}{{}} isSyncing: {}", player.getName().getString(), uuid, isSyncing);
            while (isSyncing) {
                EsirExtraSync.LOGGER.info("{}{{}} isSyncing: {}", player.getName().getString(), uuid, isSyncing);
                resultSet.close();
                queryResult.getConnection().close();
                queryResult = DBController.executeQuery("SELECT * FROM sync_status WHERE uuid='" + uuid + "';");
                resultSet = queryResult.getResultSet();
                isSyncing = resultSet.next() && resultSet.getBoolean("is_syncing");
                Thread.sleep((long) (0.05 * 1000));
            }
            EsirExtraSync.LOGGER.info("玩家{}{{}}的数据库数据同步完成", player.getName().getString(), uuid);
        }
        resultSet.close();
        queryResult.getConnection().close();

        player.sendMessage(new StringTextComponent("[EsirExtraSync]开始同步数据...").withStyle(TextFormatting.GOLD), Util.NIL_UUID);
        AoASkillSyncListener.doPlayerJoin(player);
        ArmourersSyncListener.doPlayerJoin(player);
        DietSyncListener.doPlayerJoin(player);
        LegendarySurvivalSyncListener.doPlayerJoin(player);
        player.sendMessage(new StringTextComponent("[EsirExtraSync]数据同步完成").withStyle(TextFormatting.GREEN), Util.NIL_UUID);
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

    public static void doAutoSave(PlayerEntity player) throws SQLException {
        AoASkillSyncListener.doAutoSave(player);
        ArmourersSyncListener.doAutoSave(player);
        DietSyncListener.doAutoSave(player);
        LegendarySurvivalSyncListener.doAutoSave(player);
    }

    private static int serverTicks = 0;
    private static final int AUTO_SAVE_TICKS = 20 * 60 * 3;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ++serverTicks >= AUTO_SAVE_TICKS) {
            serverTicks = 0;
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
                    executorService.submit(() -> {
                        try {
                            long startMillis = System.currentTimeMillis();
                            player.sendMessage(new StringTextComponent("[EsirExtraSync]开始保存数据...").withStyle(TextFormatting.GOLD), Util.NIL_UUID);
                            EsirExtraSync.LOGGER.info("开始保存玩家{}{{}}的数据库数据...", player.getName().getString(), player.getStringUUID());

                            doAutoSave(player);

                            long endMillis = System.currentTimeMillis();
                            player.sendMessage(new StringTextComponent("[EsirExtraSync]数据保存完成").withStyle(TextFormatting.GREEN), Util.NIL_UUID);
                            EsirExtraSync.LOGGER.info("玩家{}{{}}的数据库数据保存完成，耗时{}ms", player.getName().getString(), player.getStringUUID(), endMillis - startMillis);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
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
        try {
            PlayerEntity player = event.getPlayer();
            DBController.executeUpdate("UPDATE sync_status SET is_syncing=1 WHERE uuid=?", player.getStringUUID());
            executorService.submit(() -> {
                try {
                    long startMillis = System.currentTimeMillis();
                    EsirExtraSync.LOGGER.info("开始同步玩家{}{{}}的数据库数据...", player.getName().getString(), player.getStringUUID());

                    doPlayerLogout(event);

                    DBController.executeUpdate("UPDATE sync_status SET is_syncing=0 WHERE uuid=?", player.getStringUUID());
                    long endMillis = System.currentTimeMillis();
                    EsirExtraSync.LOGGER.info("玩家{}{{}}的数据库数据同步完成，耗时{}ms", player.getName().getString(), player.getStringUUID(), endMillis - startMillis);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
