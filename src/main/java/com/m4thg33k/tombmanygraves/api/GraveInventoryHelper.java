package com.m4thg33k.tombmanygraves.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GraveInventoryHelper {

	public static TempInventory storeInventory(IInventory inventory) {
		int size = inventory.getSizeInventory();
		TempInventory copy = new TempInventory(size);

		for (int i = 0; i < size; i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (!stack.isEmpty()) {
				copy.setInventorySlotContents(i, stack.copy());
			}
		}
		return copy;
	}

	public static void dropItem(EntityPlayer player, ItemStack stack) {
		dropItem(player.getEntityWorld(), player.getPosition(), stack);
	}

	public static void dropItem(World world, BlockPos pos, ItemStack stack) {
		InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
	}
}