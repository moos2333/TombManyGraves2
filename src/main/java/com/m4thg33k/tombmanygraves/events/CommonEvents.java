package com.m4thg33k.tombmanygraves.events;

import java.util.HashMap;
import java.util.UUID;

import com.m4thg33k.tombmanygraves.ModConfigs;
import com.m4thg33k.tombmanygraves.blocks.ModBlocks;
import com.m4thg33k.tombmanygraves.invman.DeathInventoryHandler;
import com.m4thg33k.tombmanygraves.invman.InventoryHolder;
import com.m4thg33k.tombmanygraves.items.ModItems;
import com.m4thg33k.tombmanygraves.tiles.TileGrave;
import com.m4thg33k.tombmanygraves.util.ChatHelper;
import com.m4thg33k.tombmanygraves.util.LogHelper;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CommonEvents {

	private static final BlockPos INVALID_POS = new BlockPos(0, -1, 0);

	private final HashMap<UUID, InventoryHolder> invs = new HashMap<>();

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerDeath(LivingDeathEvent e) {
		if (e.isCanceled() || e.getEntityLiving().getEntityWorld().isRemote || !(e.getEntityLiving() instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer) e.getEntityLiving();
		if (player.world.getGameRules().getBoolean("keepInventory")) {
			invs.remove(player.getUniqueID());
			return;
		}
		invs.remove(player.getUniqueID());
		InventoryHolder inventoryHolder = new InventoryHolder();
		inventoryHolder.grabPlayerData(player);
		if (inventoryHolder.isInventoryEmpty()) {
			if (ModConfigs.ENABLE_CHAT_MESSAGE_ON_DEATH) {
				ChatHelper.sayMessage(player, "But there were no valid items for the grave!");
			}
			if (ModConfigs.PRINT_DEATH_LOG) {
				LogHelper.info(player.getName() + " died without valid items for a grave.");
			}
			DeathInventoryHandler.clearLatest(player);
		} else {
			invs.put(player.getUniqueID(), inventoryHolder);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerDrop(PlayerDropsEvent e) {
		if (e.isCanceled()) return;
		EntityPlayer player = e.getEntityPlayer();
		if (ModConfigs.PRINT_DEATH_LOG) {
			handleDeathLog(player);
		}
		if (!player.world.getGameRules().getBoolean("keepInventory")) {
			InventoryHolder inventoryHolder = invs.remove(player.getUniqueID());
			if (inventoryHolder != null) {
				inventoryHolder.ensure(e.getDrops());
				if (!inventoryHolder.isInventoryEmpty()) {
					handleGraveLogic(player, inventoryHolder);
				}
			}

			ItemStack newList = DeathInventoryHandler.getDeathListFromFile(player.getName(), "latest");
			if (newList != null) {
				InventoryPlayer inv = player.inventory;
				int empty = inv.getFirstEmptyStack();
				if (empty >= 0) {
					inv.mainInventory.set(empty, newList.copy());
				}
			}
		}

		for (EntityItem entityItem : e.getDrops()) {
			Item item = entityItem.getItem().getItem();
			if (item == Item.getItemFromBlock(ModBlocks.blockGrave) || item == ModItems.itemDeathList) {
				entityItem.setDead();
			}
		}
	}

	private void handleDeathLog(EntityPlayer player) {
		BlockPos pos = player.getPosition();
		LogHelper.info(player.getName() + " died in dimension " + player.dimension + " at " + pos + "." + (ModConfigs.ENABLE_GRAVES ? " Their grave may be near!" : ""));
	}

	private void handleGraveLogic(EntityPlayer player, InventoryHolder inventoryHolder) {
		if (ModConfigs.ENABLE_CHAT_MESSAGE_ON_DEATH) {
			ChatHelper.sayMessage(player, "Place of death: " + player.getPosition());
		}

		if (ModConfigs.ENABLE_GRAVES) {
			World world = player.world;
			BlockPos playerPos = player.getPosition();
			IBlockState graveState = ModBlocks.blockGrave.getDefaultState();

			BlockPos posToPlace = INVALID_POS;
			if (ModConfigs.ASCEND_LIQUID) {
				posToPlace = findValidGraveLocation(world, ascendFromFluid(world, playerPos));
			}
			if (posToPlace.getY() == -1) {
				posToPlace = findValidGraveLocation(world, playerPos);
			}

			if (posToPlace.getY() == -1) {
				ChatHelper.sayMessage(player, "A suitable location for the grave wasn't found.");
				inventoryHolder.insertInventory(player);
			} else {
				world.setBlockState(posToPlace, graveState);
				TileEntity tile = world.getTileEntity(posToPlace);
				if (tile instanceof TileGrave) {
					TileGrave grave = (TileGrave) tile;
					grave.getPlayerData(player);

					IBlockState camoBlock = getBlockStateBelow(world, posToPlace);
					if (camoBlock.getMaterial() == Material.AIR) {
						camoBlock = graveState;
					} else if (camoBlock.getMaterial() == Material.GRASS) {
						camoBlock = Blocks.DIRT.getDefaultState();
					}

					grave.setCamoState(camoBlock);
					inventoryHolder.setPosition(posToPlace);
					grave.setSavedInventory(inventoryHolder);
				} else {
					LogHelper.info("Error! Grave could not be found after placement! " + posToPlace);
					ChatHelper.sayMessage(player, "ERROR! Grave formed incorrectly! Report to M4thG33k!");
				}
			}
		} else {
			inventoryHolder.insertInventory(player);
		}

		if (ModConfigs.ALLOW_INVENTORY_SAVES) {
			boolean wroteFile = DeathInventoryHandler.writeInventoryFile(inventoryHolder);
			if (!wroteFile) {
				LogHelper.error("An error occurred when writing the inventory file for " + inventoryHolder.getPlayerName() + ".");
			}
		}
	}

	private BlockPos findValidGraveLocation(World world, BlockPos pos) {
		BlockPos toCheck = pos;

		if (toCheck.getY() <= 0) {
			toCheck = toCheck.add(0, MathHelper.abs(toCheck.getY()) + (ModConfigs.START_VOID_SEARCH_AT_ONE ? 1 : ModConfigs.MAX_GRAVE_SEARCH_RADIUS), 0);
		}

		for (int r = 0; r <= ModConfigs.MAX_GRAVE_SEARCH_RADIUS; r++) {
			BlockPos result = checkLevel(world, toCheck, r, false);
			if (result.getY() != -1) {
				return result;
			}
		}
		return INVALID_POS;
	}

	private BlockPos ascendFromFluid(World world, BlockPos pos) {
		BlockPos toCheck = pos;
		int height = 0;
		while (pos.getY() + height < world.getActualHeight() - ModConfigs.MAX_GRAVE_SEARCH_RADIUS && !isValidLocation(world, toCheck, true)) {
			toCheck = checkLevel(world, pos.add(0, height, 0), 1, true);
			height += 1;
		}

		return toCheck;
	}

	private BlockPos checkLevel(World world, BlockPos pos, int radius, boolean ignoreFluidConfigs) {
		if (radius == 0) {
			return isValidLocation(world, pos, ignoreFluidConfigs) ? pos : INVALID_POS;
		}

		for (int i = -radius; i <= radius; i++) {
			for (int j = -radius; j <= radius; j++) {
				for (int k = radius; k >= -radius; k--) {
					if (MathHelper.abs(i) == radius || MathHelper.abs(j) == radius || MathHelper.abs(k) == radius) {
						if (isValidLocation(world, pos.add(i, j, k), ignoreFluidConfigs)) {
							return pos.add(i, j, k);
						}
					}
				}
			}
		}

		return INVALID_POS;
	}

	private boolean isValidLocation(World world, BlockPos pos, boolean ignoreFluidConfigs) {
		if (pos.getY() < 0 || pos.getY() >= world.getActualHeight()) {
			return false;
		}
		if (world.isAirBlock(pos)) {
			return true;
		}

		Block theBlock = world.getBlockState(pos).getBlock();
		if (ModConfigs.REPLACE_PLANTS && theBlock instanceof IPlantable) {
			return true;
		}

		if (!ignoreFluidConfigs) {
			if ((ModConfigs.REPLACE_STILL_LAVA && theBlock == Blocks.LAVA)
					|| (ModConfigs.REPLACE_FLOWING_LAVA && theBlock == Blocks.FLOWING_LAVA)
					|| (ModConfigs.REPLACE_STILL_WATER && theBlock == Blocks.WATER)
					|| (ModConfigs.REPLACE_FLOWING_WATER && theBlock == Blocks.FLOWING_WATER)) {
				return true;
			}
		}

		return false;
	}

	private IBlockState getBlockStateBelow(World world, BlockPos pos) {
		return world.getBlockState(pos.add(0, -1, 0));
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void itemToss(ItemTossEvent event) {
		Item item = event.getEntityItem().getItem().getItem();
		if (item == Item.getItemFromBlock(ModBlocks.blockGrave) || item == ModItems.itemDeathList) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerClone(PlayerEvent.Clone event) {
		if (ModConfigs.ALLOW_INVENTORY_LISTS && !event.isCanceled()
				&& !event.getEntityLiving().world.getGameRules().getBoolean("keepInventory")
				&& ModConfigs.ALLOW_INVENTORY_SAVES
				&& !event.getEntityLiving().world.isRemote
				&& event.isWasDeath()) {

			InventoryPlayer originalInv = event.getOriginal().inventory;
			InventoryPlayer newInv = event.getEntityPlayer().inventory;

			for (int i = 0; i < originalInv.mainInventory.size(); i++) {
				ItemStack item = originalInv.mainInventory.get(i);

				if (!item.isEmpty() && item.getItem() == ModItems.itemDeathList) {
					int empty = newInv.getFirstEmptyStack();
					if (empty >= 0) {
						newInv.mainInventory.set(empty, item.copy());
					}
				}
			}
		}
	}
}