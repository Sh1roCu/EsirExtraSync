package cn.sh1rocu.esirextrasync.listener;

import cn.sh1rocu.esirextrasync.util.DBController;
import cn.sh1rocu.esirextrasync.util.NbtUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import sfiomn.legendarysurvivaloverhaul.util.CapabilityUtil;

import java.sql.ResultSet;
import java.sql.SQLException;

public class LegendarySurvivalSyncListener {
    public static void doPlayerJoin(PlayerEntity player) throws SQLException, CommandSyntaxException {
        String uuid = player.getStringUUID();
        DBController.QueryResult queryResult = DBController.executeQuery("SELECT * FROM legendary_survival_data WHERE uuid='" + uuid + "';");
        ResultSet resultSet = queryResult.getResultSet();
        if (!resultSet.next()) {
            saveToDB(player, true);
        } else {
            CapabilityUtil.getTempCapability(player).readNBT(NbtUtil.deserialize(resultSet.getString("temperature")));
            CapabilityUtil.getHeartModCapability(player).readNBT(NbtUtil.deserialize(resultSet.getString("heart_modifier")));
            CapabilityUtil.getWetnessCapability(player).readNBT(NbtUtil.deserialize(resultSet.getString("wetness")));
            CapabilityUtil.getThirstCapability(player).readNBT(NbtUtil.deserialize(resultSet.getString("thirst")));
            CapabilityUtil.getBodyDamageCapability(player).readNBT(NbtUtil.deserialize(resultSet.getString("body_damage")));
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
