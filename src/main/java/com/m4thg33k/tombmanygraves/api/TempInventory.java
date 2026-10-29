package com.m4thg33k.tombmanygraves.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class TempInventory extends InventoryBasic {

    public TempInventory(int slotCount) {
        super("Temp", false, slotCount);
    }

    public TempInventory(NBTTagList tagList) {
        this(getMaxSlotInTagList(tagList));

        this.readFromTagList(tagList);
    }

    private static int getMaxSlotInTagList(NBTTagList tagList) {
        int max = -1;
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound compound = tagList.getCompoundTagAt(i);
            int val = compound.getByte("Slot") & 255;
            if (val > max) {
                max = val;
            }
        }

        return max + 1;
    }

    public NBTTagList writeToTagList(NBTTagList list) {
        for (int i = 0; i < this.getSizeInventory(); i++) {
            ItemStack stack = this.getStackInSlot(i);
            if (!stack.isEmpty()) {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setByte("Slot", (byte) i);
                stack.writeToNBT(compound);
                list.appendTag(compound);
            }
        }

        return list;
    }

    public void readFromTagList(NBTTagList list) {
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound compound = list.getCompoundTagAt(i);
            ItemStack stack = new ItemStack(compound);
            this.setInventorySlotContents(compound.getByte("Slot") & 255, stack);
        }
    }

    public List<ItemStack> getListOfNonEmptyItemStacks() {
        List<ItemStack> ret = new ArrayList<>(this.getSizeInventory());
        for (int i = 0; i < this.getSizeInventory(); i++) {
            ItemStack inSlot = this.getStackInSlot(i);
            if (!inSlot.isEmpty()) {
                ret.add(inSlot);
            }
        }

        return ret;
    }

    public static List<String> getGuiStringsForItemStack(int number, ItemStack stack) {
        List<String> ret = new ArrayList<>();

        if (!stack.isEmpty()) {
            String name = stack.getDisplayName();
            if (name.length() > 28) {
                name = name.substring(0, 25) + "...";
            }

            ret.add(number + ") " + name + (stack.getCount() > 1 ? " x" + stack.getCount() : ""));

            for (java.util.Map.Entry<net.minecraft.enchantment.Enchantment, Integer> entry
                    : EnchantmentHelper.getEnchantments(stack).entrySet()) {
                ret.add("    " + entry.getKey().getTranslatedName(entry.getValue()));
            }
        }

        return ret;
    }

    public static List<String> getGuiStringsForItemStackList(List<ItemStack> items) {
        List<String> ret = new ArrayList<>();
        int number = 1;

        for (ItemStack stack : items) {
            ret.addAll(getGuiStringsForItemStack(number++, stack));
        }

        return ret;
    }

    public ArrayList<String> getListOfItemsInInventoryAsStrings() {
        ArrayList<String> ret = new ArrayList<>();
        int number = 1;

        for (int i = 0; i < this.getSizeInventory(); i++) {
            ItemStack inSlot = this.getStackInSlot(i);
            if (!inSlot.isEmpty()) {
                ret.addAll(getGuiStringsForItemStack(number++, inSlot));
            }
        }

        return ret;
    }
}