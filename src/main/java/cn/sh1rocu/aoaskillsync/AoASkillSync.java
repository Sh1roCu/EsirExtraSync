package cn.sh1rocu.aoaskillsync;

import cn.sh1rocu.aoaskillsync.config.DBConfig;
import cn.sh1rocu.aoaskillsync.listener.AoASkillSyncListener;
import cn.sh1rocu.aoaskillsync.util.DBController;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;

@Mod(AoASkillSync.MODID)
public class AoASkillSync {
    public static final String MODID = "esirextrasync";
    public static final Logger LOGGER = LogManager.getLogger();

    public AoASkillSync() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DBConfig.COMMON_CONFIG);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        AoASkillSyncListener.register();
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) throws SQLException {
        DBController.executeCreateDB("CREATE DATABASE IF NOT EXISTS " + DBConfig.DATABASE_NAME.get());

        DBController.executeUpdate(
                "CREATE TABLE IF NOT EXISTS skill_data (" +
                        "uuid CHAR(36) NOT NULL," +
                        "dexterity BLOB," +
                        "extraction BLOB," +
                        "farming BLOB," +
                        "hauling BLOB," +
                        "innervation BLOB," +
                        "PRIMARY KEY (uuid));" +
                        "CREATE TABLE IF NOT EXISTS armourers_data (" +
                        "uuid CHAR(36) NOT NULL," +
                        "nbt BLOB," +
                        "PRIMARY KEY (uuid))"
        );
        LOGGER.info("AoASkillSync is ready!");
    }

}