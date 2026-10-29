package com.m4thg33k.tombmanygraves.invman;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.m4thg33k.tombmanygraves.api.TempInventory;
import com.m4thg33k.tombmanygraves.blocks.ModBlocks;
import com.m4thg33k.tombmanygraves.items.ItemDeathList;
import com.m4thg33k.tombmanygraves.util.LogHelper;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class InventoryHolder {
    public static final String TAG_NAME = "InventoryHolder";
    public static final String EMPTY = "IsEmpty";
    public static final String TIMESTAMP = "Timestamp";
    public static final String PLAYER_NAME = "PlayerName";
    public static final String X = "Xcoord";
    public static final String Y = "Ycoord";
    public static final String Z = "Zcoord";

    private static final String TAG_COMPRESSED = TAG_NAME + "_z";
    private static final int COMPRESSION_THRESHOLD = 512;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("MM_dd_yyyy_HH_mm_ss");

    private NBTTagCompound compound = new NBTTagCompound();
    private boolean isEmpty = true;
    private String timestamp = "";
    private int xcoord = -1;
    private int ycoord = -1;
    private int zcoord = -1;
    private String playerName = "unknown";

    public InventoryHolder() {
    }

    public boolean isInventoryEmpty() {
        return isEmpty;
    }

    public void grabPlayerData(EntityPlayer player) {
        HashMap<String, TempInventory> map = GraveInventoryManager.getInstance().grabItemsFromPlayer(player);
        isEmpty = map.isEmpty();
        compound = new NBTTagCompound();
        for (Entry<String, TempInventory> entry : map.entrySet()) {
            NBTTagList list = new NBTTagList();
            entry.getValue().writeToTagList(list);
            compound.setTag(entry.getKey(), list);
        }

        playerName = player.getName();
        compound.setString(PLAYER_NAME, playerName);

        setTimestamp(TIMESTAMP_FORMATTER.format(LocalDateTime.now()));
    }

    public List<ItemStack> getAllItems() {
        List<ItemStack> list = new ArrayList<>();
        for (String key : compound.getKeySet()) {
            TempInventory inv = new TempInventory(compound.getTagList(key, 10));
            list.addAll(inv.getListOfNonEmptyItemStacks());
        }
        return list;
    }

    public void ensure(List<EntityItem> drops) {
        int nonEmpty = 0;
        for (String key : compound.getKeySet()) {
            TempInventory inv = new TempInventory(compound.getTagList(key, 10));
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                boolean matched = false;
                Iterator<EntityItem> it = drops.iterator();
                while (it.hasNext()) {
                    if (ItemStack.areItemStacksEqual(stack, it.next().getItem())) {
                        it.remove();
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    inv.setInventorySlotContents(i, ItemStack.EMPTY);
                } else {
                    nonEmpty++;
                }
            }
            NBTTagList list = new NBTTagList();
            inv.writeToTagList(list);
            compound.setTag(key, list);
        }
        isEmpty = nonEmpty == 0;
    }

    public void grabPlayerData(EntityPlayer player, BlockPos pos) {
        grabPlayerData(player);
        setPosition(pos);
    }

    public void setPosition(BlockPos pos) {
        xcoord = pos.getX();
        ycoord = pos.getY();
        zcoord = pos.getZ();

        compound.setInteger(X, xcoord);
        compound.setInteger(Y, ycoord);
        compound.setInteger(Z, zcoord);
    }

    public String getPlayerName() {
        return playerName;
    }

    public BlockPos getPosition() {
        return new BlockPos(xcoord, ycoord, zcoord);
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        compound.setString(TIMESTAMP, timestamp);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public static boolean isItemValidForGrave(ItemStack stack) {
        if (stack.isEmpty()
                || stack.getItem() instanceof ItemDeathList
                || stack.getItem() == Item.getItemFromBlock(ModBlocks.blockGrave)) {
            return false;
        }
        return true;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound inCompound) {
        byte[] compressed = compressIfNeeded();
        if (compressed != null) {
            inCompound.setByteArray(TAG_COMPRESSED, compressed);
        } else {
            inCompound.setTag(TAG_NAME, this.compound);
        }
        inCompound.setBoolean(EMPTY, this.isEmpty);
        return inCompound;
    }

    public void readFromNBT(NBTTagCompound inCompound) {
        if (inCompound.hasKey(TAG_COMPRESSED)) {
            byte[] compressed = inCompound.getByteArray(TAG_COMPRESSED);
            NBTTagCompound restored = decompressOrNull(compressed);
            if (restored == null) {
                LogHelper.error("Failed to decompress saved inventory; data may be corrupted.");
                this.compound = new NBTTagCompound();
                this.isEmpty = true;
                this.xcoord = -1;
                this.ycoord = -1;
                this.zcoord = -1;
                this.timestamp = "";
                this.playerName = "unknown";
                return;
            }
            this.compound = restored;
            this.isEmpty = inCompound.getBoolean(EMPTY);
        } else if (inCompound.hasKey(TAG_NAME)) {
            this.compound = inCompound.getCompoundTag(TAG_NAME);
            this.isEmpty = inCompound.getBoolean(EMPTY);
        } else {
            this.compound = new NBTTagCompound();
            this.isEmpty = true;
            this.xcoord = -1;
            this.ycoord = -1;
            this.zcoord = -1;
            this.timestamp = "";
            this.playerName = "unknown";
            return;
        }

        this.xcoord = compound.getInteger(X);
        this.ycoord = compound.getInteger(Y);
        this.zcoord = compound.getInteger(Z);

        this.timestamp = compound.getString(TIMESTAMP);
        this.playerName = compound.getString(PLAYER_NAME);
    }

    private byte[] compressIfNeeded() {
        if (this.compound.isEmpty()) {
            return null;
        }
        byte[] raw;
        try {
            NBTTagCompound wrapper = new NBTTagCompound();
            wrapper.setTag("i", this.compound);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            CompressedStreamTools.write(wrapper, dos);
            dos.flush();
            raw = baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
        if (raw.length <= COMPRESSION_THRESHOLD) {
            return null;
        }
        byte[] compressed = deflate(raw);
        if (compressed == null || compressed.length >= raw.length) {
            return null;
        }
        return compressed;
    }

    private static byte[] deflate(byte[] input) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater);
            dos.write(input);
            dos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            deflater.end();
        }
    }

    private static NBTTagCompound decompressOrNull(byte[] input) {
        if (input == null || input.length == 0) {
            return null;
        }
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(input);
            InflaterInputStream iis = new InflaterInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = iis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            iis.close();

            ByteArrayInputStream rawIn = new ByteArrayInputStream(baos.toByteArray());
            DataInputStream dis = new DataInputStream(rawIn);
            NBTTagCompound wrapper = CompressedStreamTools.read(dis);
            if (wrapper == null || !wrapper.hasKey("i")) {
                return null;
            }
            net.minecraft.nbt.NBTBase base = wrapper.getTag("i");
            return base instanceof NBTTagCompound ? (NBTTagCompound) base : null;
        } catch (IOException e) {
            return null;
        }
    }

    public void insertInventory(EntityPlayer player) {
        if (isEmpty) {
            return;
        }

        GraveInventoryManager.getInstance().insertInventory(player, compound, false);
    }

    public void forceInventory(EntityPlayer player) {
        if (isEmpty) {
            return;
        }

        GraveInventoryManager.getInstance().insertInventory(player, compound, true);
    }

    public void dropInventory(EntityPlayer player) {
        dropInventory(player.getEntityWorld(), player.getPosition());
    }

    public void dropInventory(World world, BlockPos pos) {
        for (ItemStack itemStack : GraveInventoryManager.getInstance().generateDrops(compound)) {
            InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), itemStack);
        }
    }

    public static void dropItem(EntityPlayer player, ItemStack stack) {
        InventoryHelper.spawnItemStack(player.getEntityWorld(), player.posX, player.posY, player.posZ, stack);
    }

    public static void dropItem(World world, BlockPos pos, ItemStack stack) {
        InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
    }

    public Map<String, Tuple<String, List<String>>> getItemStackStringsForGui() {
        return GraveInventoryManager.getInstance().createItemListForGui(compound);
    }

    public static NBTTagCompound toUncompressedForm(NBTTagCompound raw) {
        if (raw == null) {
            return null;
        }
        if (raw.hasKey(TAG_NAME) || !raw.hasKey(TAG_COMPRESSED)) {
            return raw;
        }
        InventoryHolder holder = new InventoryHolder();
        holder.readFromNBT(raw);
        NBTTagCompound flat = new NBTTagCompound();
        flat.setTag(TAG_NAME, holder.compound);
        flat.setBoolean(EMPTY, holder.isEmpty);
        return flat;
    }
}