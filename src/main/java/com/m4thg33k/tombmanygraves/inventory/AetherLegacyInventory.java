package com.m4thg33k.tombmanygraves.inventory;

import com.gildedgames.the_aether.api.AetherAPI;
import com.gildedgames.the_aether.api.player.IPlayerAether;
import com.m4thg33k.tombmanygraves.api.GraveInventoryHelper;
import com.m4thg33k.tombmanygraves.api.GraveRegistry;
import com.m4thg33k.tombmanygraves.api.IGraveInventory;
import com.m4thg33k.tombmanygraves.api.TempInventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

@GraveRegistry(id = AetherLegacyInventory.UNIQUE_IDENTIFIER, name = "Aether Accessories", reqMod = "aether_legacy")
public class AetherLegacyInventory implements IGraveInventory {

	public static final String UNIQUE_IDENTIFIER = "aether";

	@Override
	public boolean pregrabLogic(EntityPlayer player) {
		return true;
	}

	@Override
	public TempInventory getItems(EntityPlayer player) {
		IPlayerAether aether = AetherAPI.getInstance().get(player);
		if (aether != null && aether.getAccessoryInventory() != null) {
			return GraveInventoryHelper.storeInventory((IInventory) aether.getAccessoryInventory());
		}
		return null;
	}

	@Override
	public void insertInventory(EntityPlayer player, TempInventory graveItems, boolean shouldForce) {
		IPlayerAether aether = AetherAPI.getInstance().get(player);
		if (aether == null || aether.getAccessoryInventory() == null) {
			return;
		}

		IInventory currentInventory = (IInventory) aether.getAccessoryInventory();

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