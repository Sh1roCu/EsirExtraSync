package cn.sh1rocu.esirextrasync.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;

public class NbtUtil {
    public static CompoundNBT deserialize(String value) throws CommandSyntaxException {
        String nbt = value.replace("|", ",").replace("^", "\"").replace("<", "{").replace(">", "}").replace("~", "'");
        return JsonToNBT.parseTag(nbt);
    }

    public static String serialize(String value) {
        return value.replace(",", "|").replace("\"", "^").replace("{", "<").replace("}", ">").replace("'", "~");
    }
}