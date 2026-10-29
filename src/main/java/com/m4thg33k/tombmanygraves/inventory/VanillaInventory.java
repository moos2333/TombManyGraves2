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

		for (int i = 0; i < graveItems.getSizeInventory(); i++) {
			ItemStack graveItem = graveItems.getStackInSlot(i);
			if (!graveItem.isEmpty()) {
				ItemStack currentStack = currentInventory.getStackInSlot(i);

				if (currentStack.isEmpty()) {
					currentInventory.setInventorySlotContents(i, graveItem);
				} else if (shouldForce) {
					ItemStack playerItem = currentStack.copy();
					currentInventory.setInventorySlotContents(i, graveItem);
					GraveInventoryHelper.dropItem(player, playerItem);
				} else {
					GraveInventoryHelper.dropItem(player, graveItem);
				}
			}
		}

	}
}