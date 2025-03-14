package cn.sh1rocu.aoaskillsync.listener;

import cn.sh1rocu.aoaskillsync.util.DBController;
import cn.sh1rocu.aoaskillsync.util.DBThreadPoolFactory;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tslat.aoa3.common.registration.custom.AoASkills;
import net.tslat.aoa3.util.PlayerUtil;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod.EventBusSubscriber
public class SyncListener {

    public static void register() {
    }

    static ExecutorService executorService = Executors.newCachedThreadPool(new DBThreadPoolFactory("AoASkillSync"));

    public static void doPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) throws SQLException, CommandSyntaxException {
        PlayerEntity player = event.getPlayer();
        String uuid = player.getUUID().toString();
        DBController.QueryResult queryResult = DBController.executeQuery("SELECT * FROM skill_data WHERE uuid='" + uuid + "';");
        ResultSet resultSet = queryResult.getResultSet();
        if (!resultSet.next()) {
            saveToDB(event.getPlayer(), true);
            return;
        }
        PlayerUtil.getSkill(player, AoASkills.DEXTERITY.get()).loadFromNbt(deserialize(resultSet.getString("dexterity")));
        PlayerUtil.getSkill(player, AoASkills.EXTRACTION.get()).loadFromNbt(deserialize(resultSet.getString("extraction")));
        PlayerUtil.getSkill(player, AoASkills.FARMING.get()).loadFromNbt(deserialize(resultSet.getString("farming")));
        PlayerUtil.getSkill(player, AoASkills.HAULING.get()).loadFromNbt(deserialize(resultSet.getString("hauling")));
        PlayerUtil.getSkill(player, AoASkills.INNERVATION.get()).loadFromNbt(deserialize(resultSet.getString("innervation")));
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

    public static CompoundNBT deserialize(String value) throws CommandSyntaxException {
        String nbt = value.replace("|", ",").replace("^", "\"").replace("<", "{").replace(">", "}").replace("~", "'");
        return JsonToNBT.parseTag(nbt);
    }

    public static String serialize(String value) {
        return value.replace(",", "|").replace("\"", "^").replace("{", "<").replace("}", ">").replace("'", "~");
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
        String dexterity = serialize(PlayerUtil.getSkill(player, AoASkills.DEXTERITY.get()).saveToNbt().toString());
        String extraction = serialize(PlayerUtil.getSkill(player, AoASkills.EXTRACTION.get()).saveToNbt().toString());
        String farming = serialize(PlayerUtil.getSkill(player, AoASkills.FARMING.get()).saveToNbt().toString());
        String hauling = serialize(PlayerUtil.getSkill(player, AoASkills.HAULING.get()).saveToNbt().toString());
        String innervation = serialize(PlayerUtil.getSkill(player, AoASkills.INNERVATION.get()).saveToNbt().toString());
        if (init) {
            DBController.executeUpdate("INSERT INTO skill_data(uuid,dexterity,extraction,farming,hauling,innervation) " +
                    "VALUES(?,?,?,?,?,?)", uuid, dexterity, extraction, farming, hauling, innervation);
        } else
            DBController.executeUpdate("UPDATE skill_data SET dexterity=?,extraction=?,farming=?,hauling=?,innervation=? WHERE uuid=?)", dexterity, extraction, farming, hauling, innervation, uuid);
    }
}