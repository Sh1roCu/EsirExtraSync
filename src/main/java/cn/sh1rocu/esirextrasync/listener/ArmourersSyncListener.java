package cn.sh1rocu.esirextrasync.listener;

import cn.sh1rocu.esirextrasync.util.DBController;
import cn.sh1rocu.esirextrasync.util.NbtUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import moe.plushie.armourers_workshop.core.capability.SkinWardrobe;
import net.minecraft.entity.player.PlayerEntity;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ArmourersSyncListener {
    public static void doPlayerJoin(PlayerEntity player) throws SQLException, CommandSyntaxException {
        String uuid = player.getStringUUID();
        DBController.QueryResult queryResult = DBController.executeQuery("SELECT * FROM armourers_data WHERE uuid='" + uuid + "';");
        ResultSet resultSet = queryResult.getResultSet();
        if (!resultSet.next()) {
            saveToDB(player, true);
        } else {
            SkinWardrobe skinWardrobe = SkinWardrobe.of(player);
            if (skinWardrobe != null)
                skinWardrobe.deserializeNBT(NbtUtil.deserialize(resultSet.getString("nbt")));
        }
        resultSet.close();
        queryResult.getConnection().close();
    }

    public static void doAutoSave(PlayerEntity player) throws SQLException {
        saveToDB(player, false);
    }

    public static void doPlayerLogout(PlayerEntity player) throws SQLException {
        saveToDB(player, false);
    }

    public static void saveToDB(PlayerEntity player, boolean init) throws SQLException {
        String uuid = player.getStringUUID();
        SkinWardrobe skinWardrobe = SkinWardrobe.of(player);
        if (skinWardrobe != null) {
            String nbt = NbtUtil.serialize(skinWardrobe.serializeNBT().toString());
            if (init) {
                DBController.executeUpdate("INSERT INTO armourers_data(uuid,nbt) " +
                        "VALUES(?,?)", uuid, nbt);
            } else
                DBController.executeUpdate("UPDATE armourers_data SET nbt=? WHERE uuid=?", nbt, uuid);
        }
    }
}