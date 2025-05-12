package cn.sh1rocu.esirextrasync.listener;

import cn.sh1rocu.esirextrasync.util.DBController;
import cn.sh1rocu.esirextrasync.util.DBThreadPoolFactory;
import cn.sh1rocu.esirextrasync.util.NbtUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import sfiomn.legendarysurvivaloverhaul.util.CapabilityUtil;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod.EventBusSubscriber
public class LegendarySurvivalSyncListener {
    static ExecutorService executorService = Executors.newCachedThreadPool(new DBThreadPoolFactory("LegendarySurvivalSync"));

    public static void doPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) throws SQLException, CommandSyntaxException {
        PlayerEntity player = event.getPlayer();
        String uuid = player.getUUID().toString();
        DBController.QueryResult queryResult = DBController.executeQuery("SELECT * FROM legendary_survival_data WHERE uuid='" + uuid + "';");
        ResultSet resultSet = queryResult.getResultSet();
        if (!resultSet.next()) {
            saveToDB(event.getPlayer(), true);
            return;
        }
        CapabilityUtil.getTempCapability(player).readNBT(NbtUtil.deserialize(resultSet.getString("temperature")));
        CapabilityUtil.getHeartModCapability(player).readNBT(NbtUtil.deserialize(resultSet.getString("heart_modifier")));
        CapabilityUtil.getWetnessCapability(player).readNBT(NbtUtil.deserialize(resultSet.getString("wetness")));
        CapabilityUtil.getThirstCapability(player).readNBT(NbtUtil.deserialize(resultSet.getString("thirst")));
        CapabilityUtil.getBodyDamageCapability(player).readNBT(NbtUtil.deserialize(resultSet.getString("body_damage")));
        resultSet.close();
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

    public static void doPlayerSaveToFile(PlayerEvent.SaveToFile event) throws SQLException, IOException {
        saveToDB(event.getPlayer(), false);
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
        saveToDB(event.getPlayer(), false);
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

    public static void saveToDB(PlayerEntity player, boolean init) throws SQLException {
        String uuid = player.getUUID().toString();
        String temperature = NbtUtil.serialize(CapabilityUtil.getTempCapability(player).writeNBT().toString());
        String heart_modifier = NbtUtil.serialize(CapabilityUtil.getHeartModCapability(player).writeNBT().toString());
        String wetness = NbtUtil.serialize(CapabilityUtil.getWetnessCapability(player).writeNBT().toString());
        String thirst = NbtUtil.serialize(CapabilityUtil.getThirstCapability(player).writeNBT().toString());
        String body_damage = NbtUtil.serialize(CapabilityUtil.getBodyDamageCapability(player).writeNBT().toString());
        if (init) {
            DBController.executeUpdate("INSERT INTO legendary_survival_data(uuid,temperature,heart_modifier,wetness,thirst,body_damage) " +
                    "VALUES(?,?,?,?,?,?)", uuid, temperature, heart_modifier, wetness, thirst, body_damage);
        } else
            DBController.executeUpdate("UPDATE legendary_survival_data SET temperature=?,heart_modifier=?,wetness=?,thirst=?,body_damage=? WHERE uuid=?", temperature, heart_modifier, wetness, thirst, body_damage, uuid);
    }
}
