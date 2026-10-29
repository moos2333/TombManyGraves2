package com.m4thg33k.tombmanygraves.invman;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.m4thg33k.tombmanygraves.ModConfigs;
import com.m4thg33k.tombmanygraves.items.ModItems;
import com.m4thg33k.tombmanygraves.util.ChatHelper;
import com.m4thg33k.tombmanygraves.util.LogHelper;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class DeathInventoryHandler {

    private static final String FILE_PREFIX = "./TombManyGraves/savedInventories";

    private static final Charset FALLBACK_CHARSET = resolveFallbackCharset();

    private static volatile Boolean pathReady = null;

    private static Charset resolveFallbackCharset() {
        try {
            return Charset.forName("GBK");
        } catch (Exception e) {
            try {
                return Charset.forName("GB18030");
            } catch (Exception e2) {
                return Charset.defaultCharset();
            }
        }
    }

    public static boolean checkFilePath() {
        Boolean cached = pathReady;
        if (cached != null && cached.booleanValue()) {
            return true;
        }
        File file = new File(FILE_PREFIX);
        boolean hasPath = file.exists() || file.mkdirs();
        if (hasPath) {
            pathReady = Boolean.TRUE;
        }
        return hasPath;
    }

    public static boolean writeInventoryFile(InventoryHolder inventoryHolder) {
        if (!ModConfigs.ALLOW_INVENTORY_SAVES) {
            return false;
        }
        if (!checkFilePath()) {
            return false;
        }

        String filename = "/" + inventoryHolder.getPlayerName();
        String timestamp = inventoryHolder.getTimestamp();
        String filePostfix = timestamp + ".json";

        String fullFilename = FILE_PREFIX + filename + "#" + filePostfix;

        NBTTagCompound tagCompound = inventoryHolder.writeToNBT(new NBTTagCompound());
        boolean didWork = writePortion(fullFilename, tagCompound.toString());

        if (didWork) {
            try {
                Path from = Paths.get(fullFilename);
                Path to = Paths.get(FILE_PREFIX + filename + "#latest.json");
                Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                LogHelper.error("Failed to update latest inventory file for " + inventoryHolder.getPlayerName());
                e.printStackTrace();
            }
        }

        return didWork;
    }

    private static boolean writePortion(String filename, String toWrite) {
        try {
            Files.write(Paths.get(filename), toWrite.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            pathReady = null;
            LogHelper.error("Failed to write file: " + filename);
            e.printStackTrace();
            return false;
        }
    }

    public static void clearLatest(EntityPlayer player) {
        if (!checkFilePath()) {
            return;
        }
        String filename = "/" + player.getName();
        String filePostfix = "latest" + ".json";

        String fullFilename = FILE_PREFIX + filename + "#" + filePostfix;
        writePortion(fullFilename, "{}");
    }

    public static List<String> getSavedInventories() {
        List<String> saved = new ArrayList<>();
        if (!checkFilePath()) {
            return saved;
        }
        String[] files = new File(FILE_PREFIX).list();
        if (files == null) {
            return saved;
        }
        for (String file : files) {
            if (file.endsWith(".json")) {
                saved.add(file);
            }
        }

        return saved;
    }

    public static List<String> getFilenames(String playerName) {
        List<String> saved = new ArrayList<>();
        if (!checkFilePath()) {
            return saved;
        }
        String[] files = new File(FILE_PREFIX).list();
        if (files == null) {
            return saved;
        }
        for (String file : files) {
            if (file.startsWith(playerName) && file.endsWith(".json")) {
                saved.add(file.substring(playerName.length() + 1, file.length() - 5));
            }
        }

        return saved;
    }

    public static ItemStack getDeathListFromFile(String playerName, String timestamp) {
        String filename = FILE_PREFIX + "/" + playerName + "#" + timestamp + ".json";
        NBTTagCompound allNBT = InventoryHolder.toUncompressedForm(readNBT(filename));
        if (allNBT != null && !allNBT.isEmpty()) {
            ItemStack theList = new ItemStack(ModItems.itemDeathList, 1);
            theList.setTagCompound(allNBT);
            return theList;
        }
        return null;
    }

    public static boolean getDeathList(EntityPlayer playerOld, EntityPlayer playerNew, String playerName, String timestamp, boolean didDie) {
        String filename = FILE_PREFIX + "/" + playerName + "#" + timestamp + ".json";

        NBTTagCompound allNBT = InventoryHolder.toUncompressedForm(readNBT(filename));
        if (allNBT == null) {
            return false;
        }
        if (!allNBT.isEmpty()) {
            ItemStack theList = new ItemStack(ModItems.itemDeathList, 1);
            theList.setTagCompound(allNBT);

            BlockPos pos = playerOld.getPosition();
            EntityPlayer thePlayer = playerOld;
            if (didDie) {
                thePlayer = playerNew;
                BlockPos bedPos = playerOld.getBedLocation(playerOld.world.provider.getDimension());
                if (bedPos != null) {
                    pos = bedPos;
                } else {
                    bedPos = playerOld.getBedLocation(playerOld.getSpawnDimension());
                    if (bedPos != null) {
                        pos = bedPos;
                    } else {
                        pos = playerOld.world.getSpawnPoint();
                    }
                }
            }
            EntityItem entityItem = new EntityItem(thePlayer.world, pos.getX(), pos.getY(), pos.getZ(), theList);
            thePlayer.world.spawnEntity(entityItem);
        } else {
            ChatHelper.sayMessage(playerOld.world, playerOld, playerName + " had no items upon death!");
        }
        return true;
    }

    public static NBTTagCompound getSavedInventoryAsNBT(String playerName, String timestamp) {
        String filename = FILE_PREFIX + "/" + playerName + "#" + timestamp + ".json";
        return InventoryHolder.toUncompressedForm(readNBT(filename));
    }

    private static NBTTagCompound readNBT(String filename) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filename));
            return JsonToNBT.getTagFromJson(decode(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    private static String decode(byte[] bytes) {
        boolean ascii = true;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] < 0) {
                ascii = false;
                break;
            }
        }
        if (ascii) {
            return new String(bytes, StandardCharsets.US_ASCII);
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, FALLBACK_CHARSET);
        }
    }
}