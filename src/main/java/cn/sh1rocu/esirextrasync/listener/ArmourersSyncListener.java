package cn.sh1rocu.esirextrasync.listener;

import cn.sh1rocu.esirextrasync.util.DBController;
import cn.sh1rocu.esirextrasync.util.DBThreadPoolFactory;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import moe.plushie.armourers_workshop.core.capability.SkinWardrobe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod.EventBusSubscriber
public class ArmourersSyncListener {

    public static void register() {
    }

    static ExecutorService executorService = Executors.newCachedThreadPool(new DBThreadPoolFactory("ArmourersSync"));

    public static void doPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) throws SQLException, CommandSyntaxException {
        PlayerEntity player = event.getPlayer();
        String uuid = player.getUUID().toString();
        DBController.QueryResult queryResult = DBController.executeQuery("SELECT * FROM armourers_data WHERE uuid='" + uuid + "';");
        ResultSet resultSet = queryResult.getResultSet();
        if (!resultSet.next()) {
            saveToDB(event.getPlayer(), true);
            return;
        }
        SkinWardrobe skinWardrobe = SkinWardrobe.of(player);
        if (skinWardrobe != null)
            skinWardrobe.deserializeNBT(deserialize(resultSet.getString("nbt")));
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
        SkinWardrobe skinWardrobe = SkinWardrobe.of(player);
        if (skinWardrobe != null) {
            String nbt = serialize(skinWardrobe.serializeNBT().toString());
            if (init) {
                DBController.executeUpdate("INSERT INTO armourers_data(uuid,nbt) " +
                        "VALUES(?,?)", uuid, nbt);
            } else
                DBController.executeUpdate("UPDATE armourers_data SET nbt=? WHERE uuid=?", nbt, uuid);
        }
    }
}