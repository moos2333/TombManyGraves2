package com.m4thg33k.tombmanygraves.inventory;

import com.m4thg33k.tombmanygraves.api.GraveInventoryHelper;
import com.m4thg33k.tombmanygraves.api.GraveRegistry;
import com.m4thg33k.tombmanygraves.api.IGraveInventory;
import com.m4thg33k.tombmanygraves.api.TempInventory;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

@GraveRegistry(id = "bauble", reqMod = "baubles", color = 0x5E8FFF, name = "Baubles")
public class BaublesInventory implements IGraveInventory {

    public static final String SLOT = "Slot";
    public static final String INVENTORY = "Inventory";

    @Override
    public boolean pregrabLogic(EntityPlayer player) {
        return true;
    }

    @Override
    public TempInventory getItems(EntityPlayer player) {
        IBaublesItemHandler container = BaublesApi.getBaublesHandler(player);
        if (container == null) {
            return new TempInventory(0);
        }
        int slots = container.getSlots();
        TempInventory inv = new TempInventory(slots);
        for (int i = 0; i < slots; i++) {
            ItemStack stack = container.getStackInSlot(i);
            if (stack != null && !stack.isEmpty()) {
                inv.setInventorySlotContents(i, stack.copy());
            }
        }
        return inv;
    }

    @Override
    public void insertInventory(EntityPlayer player, TempInventory inventory, boolean shouldForce) {
        IBaublesItemHandler container = BaublesApi.getBaublesHandler(player);
        if (container == null) {
            dropAll(player, inventory, 0);
            return;
        }

        int maxSlots = Math.min(inventory.getSizeInventory(), container.getSlots());

        for (int i = 0; i < maxSlots; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack current = container.getStackInSlot(i);

            if (current == null || current.isEmpty()) {
                container.setStackInSlot(i, stack);
            } else if (shouldForce) {
                ItemStack original = current.copy();
                container.setStackInSlot(i, stack);
                GraveInventoryHelper.dropItem(player, original);
            } else {
                GraveInventoryHelper.dropItem(player, stack);
            }
        }

        dropAll(player, inventory, maxSlots);
    }

    private static void dropAll(EntityPlayer player, TempInventory inventory, int fromIndex) {
        for (int i = fromIndex; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                GraveInventoryHelper.dropItem(player, stack);
            }
        }
    }
}