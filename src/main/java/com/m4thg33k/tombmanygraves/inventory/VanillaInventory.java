package com.m4thg33k.tombmanygraves.inventory;

import com.m4thg33k.tombmanygraves.api.GraveInventoryHelper;
import com.m4thg33k.tombmanygraves.api.GraveRegistry;
import com.m4thg33k.tombmanygraves.api.IGraveInventory;
import com.m4thg33k.tombmanygraves.api.TempInventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

@GraveRegistry(id = VanillaInventory.UNIQUE_IDENTIFIER, name = "Inventory")
public class VanillaInventory implements IGraveInventory {

	public static final String UNIQUE_IDENTIFIER = "vanilla";

	@Override
	public boolean pregrabLogic(EntityPlayer player) {
		return true;
	}

	@Override
	public TempInventory getItems(EntityPlayer player) {
		return GraveInventoryHelper.storeInventory(player.inventory);
	}

	@Override
	public void insertInventory(EntityPlayer player, TempInventory graveItems, boolean shouldForce) {
		IInventory currentInventory = player.inventory;

		int maxSlots = Math.min(graveItems.getSizeInventory(), currentInventory.getSizeInventory());

		for (int i = 0; i < maxSlots; i++) {
			ItemStack graveItem = graveItems.getStackInSlot(i);
			if (graveItem.isEmpty()) {
				continue;
			}
			ItemStack currentStack = currentInventory.getStackInSlot(i);

			if (currentStack == null || currentStack.isEmpty()) {
				currentInventory.setInventorySlotContents(i, graveItem);
			} else if (shouldForce) {
				if (isProtectedSlotItem(currentStack)) {
					int emptySlot = findEmptySlotExcluding(currentInventory, i);
					if (emptySlot >= 0) {
						currentInventory.setInventorySlotContents(emptySlot, currentStack);
						currentInventory.setInventorySlotContents(i, graveItem);
					} else {
						GraveInventoryHelper.dropItem(player, graveItem);
					}
				} else {
					ItemStack playerItem = currentStack.copy();
					currentInventory.setInventorySlotContents(i, graveItem);
					GraveInventoryHelper.dropItem(player, playerItem);
				}
			} else {
				GraveInventoryHelper.dropItem(player, graveItem);
			}
		}

		for (int i = maxSlots; i < graveItems.getSizeInventory(); i++) {
			ItemStack stack = graveItems.getStackInSlot(i);
			if (!stack.isEmpty()) {
				GraveInventoryHelper.dropItem(player, stack);
			}
		}
	}

	private static boolean isProtectedSlotItem(ItemStack stack) {
		return !stack.isEmpty() && !stack.isStackable();
	}

	private static int findEmptySlotExcluding(IInventory inventory, int excluded) {
		int size = inventory.getSizeInventory();
		for (int j = 0; j < size; j++) {
			if (j == excluded) {
				continue;
			}
			ItemStack stack = inventory.getStackInSlot(j);
			if (stack == null || stack.isEmpty()) {
				return j;
			}
		}
		return -1;
	}
}