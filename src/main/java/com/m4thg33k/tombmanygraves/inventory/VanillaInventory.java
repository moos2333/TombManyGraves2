package com.m4thg33k.tombmanygraves.inventory;

import com.m4thg33k.tombmanygraves.api.GraveInventoryHelper;
import com.m4thg33k.tombmanygraves.api.GraveRegistry;
import com.m4thg33k.tombmanygraves.api.IGraveInventory;
import com.m4thg33k.tombmanygraves.api.TempInventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

@GraveRegistry(id = VanillaInventory.UNIQUE_IDENTIFIER, name = "Inventory")
public class VanillaInventory implements IGraveInventory {

	public static final String UNIQUE_IDENTIFIER = "vanilla";

	private static final int MAIN_INV_SIZE = 36;
	private static final int ARMOR_SLOT_BOOTS = 36;
	private static final int ARMOR_SLOT_LEGGINGS = 37;
	private static final int ARMOR_SLOT_CHESTPLATE = 38;
	private static final int ARMOR_SLOT_HELMET = 39;
	private static final int OFFHAND_SLOT = 40;

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
					int emptySlot = findSlotFor(currentInventory, i, currentStack);
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

	private static int findSlotFor(IInventory inventory, int excluded, ItemStack toPlace) {
		int size = inventory.getSizeInventory();
		for (int j = 0; j < size; j++) {
			if (j == excluded) {
				continue;
			}
			ItemStack stack = inventory.getStackInSlot(j);
			if (stack != null && !stack.isEmpty()) {
				continue;
			}
			if (j < MAIN_INV_SIZE || j == OFFHAND_SLOT) {
				return j;
			}
			if (canPlaceInArmorSlot(toPlace, j)) {
				return j;
			}
		}
		return -1;
	}

	private static boolean canPlaceInArmorSlot(ItemStack stack, int slotIndex) {
		EntityEquipmentSlot required = armorSlotFor(slotIndex);
		if (required == null) {
			return false;
		}
		EntityEquipmentSlot itemSlot = stack.getItem().getEquipmentSlot(stack);
		return itemSlot != null && itemSlot == required;
	}

	private static EntityEquipmentSlot armorSlotFor(int slotIndex) {
		switch (slotIndex) {
			case ARMOR_SLOT_BOOTS:
				return EntityEquipmentSlot.FEET;
			case ARMOR_SLOT_LEGGINGS:
				return EntityEquipmentSlot.LEGS;
			case ARMOR_SLOT_CHESTPLATE:
				return EntityEquipmentSlot.CHEST;
			case ARMOR_SLOT_HELMET:
				return EntityEquipmentSlot.HEAD;
			default:
				return null;
		}
	}
}