package cn.sh1rocu.esirextrasync.listener;

import cn.sh1rocu.esirextrasync.util.DBController;
import cn.sh1rocu.esirextrasync.util.NbtUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.tslat.aoa3.common.registration.custom.AoASkills;
import net.tslat.aoa3.util.PlayerUtil;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AoASkillSyncListener {
    public static void doPlayerJoin(PlayerEntity player) throws SQLException, CommandSyntaxException {
        String uuid = player.getStringUUID();
        DBController.QueryResult queryResult = DBController.executeQuery("SELECT * FROM skill_data WHERE uuid='" + uuid + "';");
        ResultSet resultSet = queryResult.getResultSet();
        if (!resultSet.next()) {
            saveToDB(player, true);
        } else {
            PlayerUtil.getSkill(player, AoASkills.DEXTERITY.get()).loadFromNbt(NbtUtil.deserialize(resultSet.getString("dexterity")));
            PlayerUtil.getSkill(player, AoASkills.EXTRACTION.get()).loadFromNbt(NbtUtil.deserialize(resultSet.getString("extraction")));
            PlayerUtil.getSkill(player, AoASkills.FARMING.get()).loadFromNbt(NbtUtil.deserialize(resultSet.getString("farming")));
            PlayerUtil.getSkill(player, AoASkills.HAULING.get()).loadFromNbt(NbtUtil.deserialize(resultSet.getString("hauling")));
            PlayerUtil.getSkill(player, AoASkills.INNERVATION.get()).loadFromNbt(NbtUtil.deserialize(resultSet.getString("innervation")));
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
        String dexterity = NbtUtil.serialize(PlayerUtil.getSkill(player, AoASkills.DEXTERITY.get()).saveToNbt().toString());
        String extraction = NbtUtil.serialize(PlayerUtil.getSkill(player, AoASkills.EXTRACTION.get()).saveToNbt().toString());
        String farming = NbtUtil.serialize(PlayerUtil.getSkill(player, AoASkills.FARMING.get()).saveToNbt().toString());
        String hauling = NbtUtil.serialize(PlayerUtil.getSkill(player, AoASkills.HAULING.get()).saveToNbt().toString());
        String innervation = NbtUtil.serialize(PlayerUtil.getSkill(player, AoASkills.INNERVATION.get()).saveToNbt().toString());
        if (init) {
            DBController.executeUpdate("INSERT INTO skill_data(uuid,dexterity,extraction,farming,hauling,innervation) " +
                    "VALUES(?,?,?,?,?,?)", uuid, dexterity, extraction, farming, hauling, innervation);
        } else
            DBController.executeUpdate("UPDATE skill_data SET dexterity=?,extraction=?,farming=?,hauling=?,innervation=? WHERE uuid=?", dexterity, extraction, farming, hauling, innervation, uuid);
    }
}