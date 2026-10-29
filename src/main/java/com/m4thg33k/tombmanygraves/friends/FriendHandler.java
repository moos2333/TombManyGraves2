package com.m4thg33k.tombmanygraves.friends;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.m4thg33k.tombmanygraves.util.LogHelper;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FriendHandler {
    private static final String PREFIX = "./TombManyGraves";
    private static final String FILE = "./TombManyGraves/friends.fdat";
    private static final String TEMP_FILE = "./TombManyGraves/friends.fdat.tmp";

    private static HashMap<UUID, HashSet<UUID>> friendList;
    private static volatile Boolean pathReady = null;

    @SuppressWarnings("unchecked")
    public static void importFriendsList()
    {
        friendList = new HashMap<>();
        if (!checkFilePath())
        {
            LogHelper.error("Could not create TombManyGraves directory; friend list is unavailable.");
            return;
        }

        File file = new File(FILE);
        if (!file.exists())
        {
            LogHelper.info("Friend list file not found. It will be created on the first friend change.");
            return;
        }

        try (FileInputStream fileIn = new FileInputStream(file);
             ObjectInputStream in = new ObjectInputStream(fileIn))
        {
            Object read = in.readObject();
            if (read instanceof HashMap)
            {
                friendList = (HashMap<UUID, HashSet<UUID>>) read;
                LogHelper.info("Successfully loaded the friend list.");
            }
            else
            {
                LogHelper.warn("Friend list file has unexpected format; starting with an empty list.");
            }
        }
        catch (Exception e)
        {
            LogHelper.error("Could not load friend list file; original file will be preserved as .bak. " + e);
            try
            {
                Files.move(Paths.get(FILE), Paths.get(FILE + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (Exception backupFailure)
            {
                LogHelper.error("Failed to back up corrupted friend list file: " + backupFailure);
            }
        }
    }

    public static boolean checkFilePath()
    {
        Boolean cached = pathReady;
        if (cached != null && cached.booleanValue())
        {
            return true;
        }
        File file = new File(PREFIX);
        boolean ok = file.exists() || file.mkdirs();
        if (ok)
        {
            pathReady = Boolean.TRUE;
        }
        return ok;
    }

    public static boolean hasAsFriend(UUID player1, UUID player2)
    {
        HashSet<UUID> set = friendList == null ? null : friendList.get(player1);
        return set != null && set.contains(player2);
    }

    public static synchronized void writeFriends()
    {
        if (friendList == null || !checkFilePath())
        {
            return;
        }

        Path temp = Paths.get(TEMP_FILE);
        Path target = Paths.get(FILE);
        try
        {
            try (FileOutputStream fileOut = new FileOutputStream(TEMP_FILE);
                 ObjectOutputStream out = new ObjectOutputStream(fileOut))
            {
                out.writeObject(friendList);
                out.flush();
            }
            try
            {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (Exception atomicFailure)
            {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Exception e)
        {
            LogHelper.error("Failed to save friend list: " + e);
        }
    }

    public static boolean hasFriendList(UUID playerID)
    {
        return friendList != null && friendList.containsKey(playerID);
    }

    public static void createFriendListFor(UUID playerID)
    {
        if (friendList == null)
        {
            friendList = new HashMap<>();
        }
        if (friendList.containsKey(playerID))
        {
            return;
        }
        friendList.put(playerID, new HashSet<>());
        writeFriends();
    }

    public static HashSet<UUID> getFriendSetFor(UUID playerID)
    {
        if (friendList == null)
        {
            return null;
        }
        return friendList.get(playerID);
    }

    public static List<String> getFriendStringListFor(MinecraftServer server, UUID playerID)
    {
        ArrayList<String> names = new ArrayList<>();

        HashSet<UUID> friends = getFriendSetFor(playerID);
        if (friends == null)
        {
            return names;
        }

        for (UUID id : friends)
        {
            if (server == null || server.getPlayerProfileCache() == null)
            {
                continue;
            }
            GameProfile profile = server.getPlayerProfileCache().getProfileByUUID(id);
            if (profile != null)
            {
                names.add(profile.getName());
            }
            else
            {
                LogHelper.warn("Could not resolve name for friend UUID: " + id);
            }
        }
        return names;
    }

    @SubscribeEvent
    public void onPlayerJoin(EntityJoinWorldEvent event)
    {
        if (event.getEntity() instanceof EntityPlayer)
        {
            FriendHandler.createFriendListFor(event.getEntity().getUniqueID());
        }
    }

    public static boolean addFriendToList(UUID owner, UUID friend)
    {
        if (friendList == null)
        {
            return false;
        }
        if (!friendList.containsKey(owner))
        {
            friendList.put(owner, new HashSet<>());
        }
        boolean answer = friendList.get(owner).add(friend);
        if (answer)
        {
            writeFriends();
        }
        return answer;
    }

    public static void printFriendList(MinecraftServer server, EntityPlayer player)
    {
        UUID playerID = player.getUniqueID();

        List<String> friends = getFriendStringListFor(server, playerID);

        if (friends.isEmpty())
        {
            player.sendMessage(new TextComponentString("You have not added any friends yet."));
            return;
        }

        for (String name : friends)
        {
            player.sendMessage(new TextComponentString(name));
        }
    }

    public static boolean removeFriend(UUID owner, UUID friend)
    {
        if (friendList == null)
        {
            return false;
        }
        HashSet<UUID> set = friendList.get(owner);
        if (set == null)
        {
            return false;
        }
        boolean answer = set.remove(friend);
        if (answer)
        {
            writeFriends();
        }
        return answer;
    }

    public static void clearFriends(UUID owner)
    {
        if (friendList == null)
        {
            return;
        }
        HashSet<UUID> set = friendList.get(owner);
        if (set == null || set.isEmpty())
        {
            return;
        }
        set.clear();
        writeFriends();
    }

    public static boolean isFriendOf(UUID owner, UUID friend)
    {
        return hasAsFriend(owner, friend);
    }
}