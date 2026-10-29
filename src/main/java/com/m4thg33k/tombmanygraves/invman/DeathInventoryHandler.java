package com.m4thg33k.tombmanygraves.invman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.m4thg33k.tombmanygraves.ModConfigs;
import com.m4thg33k.tombmanygraves.items.ModItems;
import com.m4thg33k.tombmanygraves.util.ChatHelper;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class DeathInventoryHandler {

    private static final String FILE_PREFIX = "./TombManyGraves/savedInventories";

    public static boolean checkFilePath()
    {
        File file = new File(FILE_PREFIX);
        boolean hasPath = true;
        if (!file.exists())
        {
            hasPath = file.mkdirs();
        }

        return hasPath;
    }

    public static boolean writeInventoryFile(InventoryHolder inventoryHolder)
    {
        boolean hasPath = checkFilePath();

        if (!ModConfigs.ALLOW_INVENTORY_SAVES || !hasPath)
        {
            return false;
        }

        String filename = "/" + inventoryHolder.getPlayerName();
        String timestamp = inventoryHolder.getTimestamp();
        String filePostfix = timestamp + ".json";

        String fullFilename = FILE_PREFIX + filename + "#" + filePostfix;

        NBTTagCompound tagCompound = inventoryHolder.writeToNBT(new NBTTagCompound());
        boolean didWork = writePortion(fullFilename, tagCompound.toString());

        if (didWork)
        {
            try {
                Path from = Paths.get(fullFilename);
                Path to = Paths.get(FILE_PREFIX + filename + "#latest.json");
                Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return didWork;
    }

    private static boolean writePortion(String filename, String toWrite)
    {
        try (FileWriter file = new FileWriter(filename))
        {
            file.write(toWrite);
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public static void clearLatest(EntityPlayer player)
    {
        String filename = "/" + player.getName();
        String filePostfix = "latest" + ".json";

        String fullFilename = FILE_PREFIX + filename + "#" + filePostfix;
        writePortion(fullFilename, "{}");
    }

    public static List<String> getSavedInventories()
    {
        List<String> saved = new ArrayList<>();
        boolean doesPathExist = checkFilePath();
        if (!doesPathExist)
        {
            return saved;
        }
        String[] files = new File(FILE_PREFIX).list();
        if (files == null)
        {
            return saved;
        }
        for (String file : files)
        {
            if (file.endsWith(".json"))
            {
                saved.add(file);
            }
        }

        return saved;
    }

    public static List<String> getFilenames(String playerName)
    {
        List<String> saved = new ArrayList<>();
        boolean doesPathExist = checkFilePath();
        if (!doesPathExist)
        {
            return saved;
        }
        String[] files = new File(FILE_PREFIX).list();
        if (files == null)
        {
            return saved;
        }
        for (String file : files)
        {
            if (file.startsWith(playerName) && file.endsWith(".json"))
            {
                saved.add(file.substring(playerName.length() + 1, file.length() - 5));
            }
        }

        return saved;
    }

    public static ItemStack getDeathListFromFile(String playerName, String timestamp){
        String filename = FILE_PREFIX + "/" + playerName + "#" + timestamp + ".json";

        try (BufferedReader reader = new BufferedReader(new FileReader(filename)))
        {
            String fileData = reader.readLine();
            NBTTagCompound allNBT = JsonToNBT.getTagFromJson(fileData);
            if (!allNBT.isEmpty()){
                ItemStack theList = new ItemStack(ModItems.itemDeathList, 1);
                theList.setTagCompound(allNBT);
                return theList;
            }

            return null;
        } catch (Exception e){
            return null;
        }
    }

    public static boolean getDeathList(EntityPlayer playerOld, EntityPlayer playerNew, String playerName, String timestamp, boolean didDie)
    {
        boolean didWork = true;

        String filename = FILE_PREFIX + "/" + playerName + "#" + timestamp + ".json";

        try (BufferedReader reader = new BufferedReader(new FileReader(filename)))
        {
            String fileData = reader.readLine();
            NBTTagCompound allNBT = JsonToNBT.getTagFromJson(fileData);
            if (!allNBT.isEmpty())
            {
                ItemStack theList = new ItemStack(ModItems.itemDeathList, 1);
                theList.setTagCompound(allNBT);

                BlockPos pos = playerOld.getPosition();
                EntityPlayer thePlayer = playerOld;
                if (didDie)
                {
                    thePlayer = playerNew;
                    BlockPos bedPos = playerOld.getBedLocation(playerOld.world.provider.getDimension());
                    if (bedPos != null)
                    {
                        pos = bedPos;
                    }
                    else
                    {
                        bedPos = playerOld.getBedLocation(playerOld.getSpawnDimension());
                        if (bedPos != null){
                            pos = bedPos;
                        } else {
                            pos = playerOld.world.getSpawnPoint();
                        }
                    }
                }
                EntityItem entityItem = new EntityItem(thePlayer.world, pos.getX(), pos.getY(), pos.getZ(), theList);
                thePlayer.world.spawnEntity(entityItem);
            }
            else
            {
                ChatHelper.sayMessage(playerOld.world, playerOld, playerName + " had no items upon death!");
            }
        }
        catch (Exception e)
        {
            didWork = false;
        }
        return didWork;
    }

    public static NBTTagCompound getSavedInventoryAsNBT(String playerName, String timestamp)
    {
        String filename = FILE_PREFIX + "/" + playerName + "#" + timestamp + ".json";

        try (BufferedReader reader = new BufferedReader(new FileReader(filename)))
        {
            String fileData = reader.readLine();
            NBTTagCompound allNBT = JsonToNBT.getTagFromJson(fileData);
            return allNBT;
        } catch (Exception e)
        {
            return null;
        }
    }
}