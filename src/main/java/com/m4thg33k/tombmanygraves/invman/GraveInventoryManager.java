package com.m4thg33k.tombmanygraves.invman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.m4thg33k.tombmanygraves.api.GraveRegistry;
import com.m4thg33k.tombmanygraves.api.IGraveInventory;
import com.m4thg33k.tombmanygraves.api.TempInventory;
import com.m4thg33k.tombmanygraves.inventory.VanillaInventory;
import com.m4thg33k.tombmanygraves.util.LogHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Tuple;

public class GraveInventoryManager {

	private static final GraveInventoryManager INSTANCE = new GraveInventoryManager();

	private final Map<String, IGraveInventory> listenerMap = new HashMap<>();
	private final Map<String, GraveRegistry> registryMap = new HashMap<>();
	private List<Map.Entry<String, IGraveInventory>> sortedListeners = new ArrayList<>();
	private List<String> sortedGuiNames = new ArrayList<>();

	private GraveInventoryManager() {
	}

	public static GraveInventoryManager getInstance() {
		return INSTANCE;
	}

	public List<Map.Entry<String, IGraveInventory>> getSortedListeners() {
		return sortedListeners;
	}

	public static int getGuiColorForInventory(String key) {
		GraveRegistry registry = getInstance().registryMap.get(key);
		return registry == null ? 0 : registry.color();
	}

	private static int clampPriority(int priority) {
		return priority < -10 ? -10 : (priority > 10 ? 10 : priority);
	}

	@ParametersAreNonnullByDefault
	public void registerListener(IGraveInventory iGraveInventory, Map<String, Object> annotation) throws Exception {
		String uniqueId = (String) annotation.get("id");
		GraveRegistry registry = iGraveInventory.getClass().getDeclaredAnnotation(GraveRegistry.class);
		LogHelper.info("Attempting to register special inventory: " + uniqueId);

		IGraveInventory existing = listenerMap.get(uniqueId);
		if (existing != null) {
			if (!(boolean) annotation.get("overridable")) {
				GraveRegistry existingRegistry = registryMap.get(uniqueId);
				if (existingRegistry != null && existingRegistry.overridable()) {
					listenerMap.put(uniqueId, iGraveInventory);
					registryMap.put(uniqueId, registry);
					LogHelper.info("Special inventory with id (" + uniqueId + ") already exists, but is able to be overwritten. Replacing existing inventory.");
				} else {
					throw new Exception("Unable to register listener for TombManyGraves with unique id: " + uniqueId + ". A listener with that id has already been registered and cannot be overwritten!");
				}
			} else {
				LogHelper.info("Special inventory with id (" + uniqueId + ") already exists, but the new one is able to be overwritten. Ignoring new inventory.");
			}
		} else {
			LogHelper.info("Added special inventory with id (" + uniqueId + ") to TombManyGraves.");
			listenerMap.put(uniqueId, iGraveInventory);
			registryMap.put(uniqueId, registry);
		}
	}

	public void finalizeListeners() {
		List<Map.Entry<String, IGraveInventory>> sorted = new ArrayList<>(listenerMap.entrySet());
		sorted.sort((x, y) -> {
			int flag = -Integer.compare(
					clampPriority(registryMap.get(x.getKey()).priority()),
					clampPriority(registryMap.get(y.getKey()).priority()));
			if (flag == 0) {
				flag = x.getKey().compareTo(y.getKey());
			}
			return flag;
		});
		sortedListeners = sorted;

		List<String> guiNames = new ArrayList<>(listenerMap.keySet());
		guiNames.sort((x, y) -> {
			if (VanillaInventory.UNIQUE_IDENTIFIER.equals(x)) {
				return -1;
			} else if (VanillaInventory.UNIQUE_IDENTIFIER.equals(y)) {
				return 1;
			} else {
				return registryMap.get(x).name().compareToIgnoreCase(registryMap.get(y).name());
			}
		});
		sortedGuiNames = guiNames;
	}

	public List<String> getSortedGuiNames() {
		return sortedGuiNames;
	}

	public HashMap<String, TempInventory> grabItemsFromPlayer(EntityPlayer player) {
		boolean shouldContinue = true;
		for (Map.Entry<String, IGraveInventory> entry : sortedListeners) {
			try {
				shouldContinue = entry.getValue().pregrabLogic(player);
			} catch (Exception e) {
				LogHelper.error("Error running pregrabLogic for " + entry.getKey());
				e.printStackTrace();
				shouldContinue = true;
			}
			if (!shouldContinue) {
				break;
			}
		}
		if (!shouldContinue) {
			return new HashMap<>(0);
		}

		HashMap<String, TempInventory> itms = new HashMap<>(sortedListeners.size());
		for (Map.Entry<String, IGraveInventory> entry : sortedListeners) {
			try {
				TempInventory data = entry.getValue().getItems(player);
				if (data != null) {
					itms.put(entry.getKey(), data);
				}
			} catch (Exception e) {
				LogHelper.error("Error collecting items from " + entry.getKey());
				e.printStackTrace();
			}
		}
		return itms;
	}

	public void insertInventory(EntityPlayer player, NBTTagCompound compound, boolean shouldForce) {
		if (compound == null) {
			return;
		}
		for (Map.Entry<String, IGraveInventory> entry : sortedListeners) {
			String key = entry.getKey();
			if (!compound.hasKey(key)) {
				continue;
			}
			TempInventory inv = null;
			try {
				inv = new TempInventory(compound.getTagList(key, 10));
				entry.getValue().insertInventory(player, inv, shouldForce);
			} catch (Exception e) {
				LogHelper.error("Error inserting inventory for " + key + "; dropping remaining items to prevent loss");
				e.printStackTrace();
				if (inv != null) {
					for (ItemStack stack : inv.getListOfNonEmptyItemStacks()) {
						InventoryHolder.dropItem(player, stack);
					}
				}
			}
		}
	}

	public List<ItemStack> generateDrops(NBTTagCompound compound) {
		List<ItemStack> drops = new ArrayList<>();
		if (compound == null) {
			return drops;
		}
		for (Map.Entry<String, IGraveInventory> entry : sortedListeners) {
			String key = entry.getKey();
			if (!compound.hasKey(key)) {
				continue;
			}
			try {
				TempInventory inv = new TempInventory(compound.getTagList(key, 10));
				drops.addAll(inv.getListOfNonEmptyItemStacks());
			} catch (Exception e) {
				LogHelper.error("Error generating drops for " + key);
				e.printStackTrace();
			}
		}
		return drops;
	}

	public Map<String, Tuple<String, List<String>>> createItemListForGui(NBTTagCompound compound) {
		Map<String, Tuple<String, List<String>>> theMap = new HashMap<>();
		if (compound == null) {
			return theMap;
		}
		for (Map.Entry<String, IGraveInventory> entry : sortedListeners) {
			String key = entry.getKey();
			if (!compound.hasKey(key)) {
				continue;
			}
			TempInventory inv = new TempInventory(compound.getTagList(key, 10));
			List<ItemStack> drops = inv.getListOfNonEmptyItemStacks();
			if (!drops.isEmpty()) {
				GraveRegistry registry = registryMap.get(key);
				theMap.put(key, new Tuple<>(registry != null ? registry.name() : key,
						TempInventory.getGuiStringsForItemStackList(drops)));
			}
		}
		return theMap;
	}
}