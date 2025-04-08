package cn.sh1rocu.esirextrasync.listener;

import cn.sh1rocu.esirextrasync.util.DBController;
import cn.sh1rocu.esirextrasync.util.DBThreadPoolFactory;
import cn.sh1rocu.esirextrasync.util.NbtUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.diet.api.DietCapability;
import top.theillusivec4.diet.api.IDietGroup;
import top.theillusivec4.diet.api.IDietTracker;
import top.theillusivec4.diet.common.capability.DietTrackerCapability;
import top.theillusivec4.diet.common.group.DietGroups;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod.EventBusSubscriber
public class DietSyncListener {
    static ExecutorService executorService = Executors.newCachedThreadPool(new DBThreadPoolFactory("AoASkillSync"));

    public static void doPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) throws SQLException, CommandSyntaxException {
        PlayerEntity player = event.getPlayer();
        String uuid = player.getUUID().toString();
        DBController.QueryResult queryResult = DBController.executeQuery("SELECT * FROM diet_data WHERE uuid='" + uuid + "';");
        ResultSet resultSet = queryResult.getResultSet();
        if (!resultSet.next()) {
            saveToDB(event.getPlayer(), true);
            return;
        }
        loadDietFromNbt(
                NbtUtil.deserialize(resultSet.getString("nbt")),
                DietCapability.get(player).orElse(new DietTrackerCapability.EmptyDietTracker())
        );
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

    public static CompoundNBT loadNbtFromDiet(IDietTracker instance) {
        CompoundNBT tag = new CompoundNBT();
        Map<String, Float> values = instance.getValues();

        if (values != null) {

            for (Map.Entry<String, Float> group : values.entrySet()) {
                tag.putFloat(group.getKey(), group.getValue());
            }
        }
        ListNBT list = new ListNBT();
        Map<Attribute, Set<UUID>> modifiers = instance.getModifiers();

        if (modifiers != null) {

            for (Map.Entry<Attribute, Set<UUID>> modifier : modifiers.entrySet()) {
                CompoundNBT attributeTag = new CompoundNBT();
                attributeTag.put("AttributeName", StringNBT.valueOf(
                        Objects.requireNonNull(modifier.getKey().getRegistryName()).toString()));
                ListNBT uuids = new ListNBT();

                for (UUID uuid : modifier.getValue()) {
                    uuids.add(StringNBT.valueOf(uuid.toString()));
                }
                attributeTag.put("UUIDs", uuids);
                list.add(attributeTag);
            }
        }
        tag.put("Modifiers", list);
        list = new ListNBT();
        Set<Item> eaten = instance.getEaten();

        if (eaten != null) {

            for (Item item : eaten) {
                ResourceLocation rl = item.getRegistryName();

                if (rl != null) {
                    list.add(StringNBT.valueOf(rl.toString()));
                }
            }
        }
        tag.put("Eaten", list);
        tag.putBoolean("Active", instance.isActive());
        return tag;
    }

    public static void loadDietFromNbt(CompoundNBT tag, IDietTracker instance) {
        Map<String, Float> groups = new HashMap<>();
        for (IDietGroup group : DietGroups.get()) {
            String name = group.getName();
            float amount = tag.contains(name) ? tag.getFloat(name) : group.getDefaultValue();
            groups.put(name, MathHelper.clamp(amount, 0.0f, 1.0f));
        }
        ListNBT list = tag.getList("Modifiers", Constants.NBT.TAG_COMPOUND);
        Map<Attribute, Set<UUID>> modifiers = new HashMap<>();

        for (int i = 0; i < list.size(); i++) {
            CompoundNBT attributeTag = list.getCompound(i);
            Attribute att = ForgeRegistries.ATTRIBUTES
                    .getValue(new ResourceLocation(attributeTag.getString("AttributeName")));

            if (att != null) {
                Set<UUID> uuids = new HashSet<>();
                ListNBT uuidList = attributeTag.getList("UUIDs", Constants.NBT.TAG_STRING);

                for (int j = 0; j < uuidList.size(); j++) {
                    uuids.add(UUID.fromString(uuidList.getString(j)));
                }
                modifiers.put(att, uuids);
            }
        }
        list = tag.getList("Eaten", Constants.NBT.TAG_STRING);
        Set<Item> eaten = new HashSet<>();

        for (int i = 0; i < list.size(); i++) {
            String s = list.getString(i);
            ResourceLocation rl = new ResourceLocation(s);
            Item item = ForgeRegistries.ITEMS.getValue(rl);

            if (item != null) {
                eaten.add(item);
            }
        }
        instance.setEaten(eaten);
        instance.setModifiers(modifiers);
        instance.setValues(groups);
        instance.setActive(!tag.contains("Active") || tag.getBoolean("Active"));
    }

    public static void doPlayerSaveToFile(PlayerEvent.SaveToFile event) throws SQLException {
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
        LazyOptional<IDietTracker> iDietTrackerLazyOptional = DietCapability.get(player);
        if (iDietTrackerLazyOptional.isPresent()) {
            String nbt = NbtUtil.serialize(loadNbtFromDiet(iDietTrackerLazyOptional.orElse(new DietTrackerCapability.EmptyDietTracker())).toString());
            if (init) {
                DBController.executeUpdate("INSERT INTO diet_data(uuid,nbt) " +
                        "VALUES(?,?)", uuid, nbt);
            } else
                DBController.executeUpdate("UPDATE diet_data SET nbt=? WHERE uuid=?", nbt, uuid);
        }
    }
}
