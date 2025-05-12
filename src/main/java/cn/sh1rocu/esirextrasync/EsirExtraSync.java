package cn.sh1rocu.esirextrasync;

import cn.sh1rocu.esirextrasync.config.DBConfig;
import cn.sh1rocu.esirextrasync.util.DBController;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;

@Mod(EsirExtraSync.MODID)
public class EsirExtraSync {
    public static final String MODID = "esirextrasync";
    public static final Logger LOGGER = LogManager.getLogger();

    public EsirExtraSync() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DBConfig.COMMON_CONFIG);
        MinecraftForge.EVENT_BUS.register(this);
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
                        "PRIMARY KEY (uuid));"
        );
        DBController.executeUpdate(
                "CREATE TABLE IF NOT EXISTS armourers_data (" +
                        "uuid CHAR(36) NOT NULL," +
                        "nbt BLOB," +
                        "PRIMARY KEY (uuid));"
        );
        DBController.executeUpdate(
                "CREATE TABLE IF NOT EXISTS diet_data (" +
                        "uuid CHAR(36) NOT NULL," +
                        "nbt BLOB," +
                        "PRIMARY KEY (uuid));"
        );
        DBController.executeUpdate(
                "CREATE TABLE IF NOT EXISTS legendary_survival_data (" +
                        "uuid CHAR(36) NOT NULL," +
                        "temperature BLOB," +
                        "heart_modifier BLOB," +
                        "wetness BLOB," +
                        "thirst BLOB," +
                        "body_damage BLOB," +
                        "PRIMARY KEY (uuid));"
        );
        LOGGER.info("EsirExtraSync is ready!");
    }
}