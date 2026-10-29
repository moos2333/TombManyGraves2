package com.m4thg33k.tombmanygraves.invman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

	private static GraveInventoryManager INSTANCE = null;

	private Map<String, IGraveInventory> listenerMap = new HashMap<>();
	private List<Map.Entry<String, IGraveInventory>> sortedListeners = new ArrayList<>();
	private Map<IGraveInventory, GraveRegistry> annotationMap = new HashMap<>();

	private List<String> sortedGuiNames = new ArrayList<>();

	private GraveInventoryManager() {

	}

	public static GraveInventoryManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new GraveInventoryManager();
		}

		return INSTANCE;
	}

	public List<Map.Entry<String, IGraveInventory>> getSortedListeners() {
		return sortedListeners;
	}

	public static int getGuiColorForInventory(String key) {
		GraveInventoryManager instance = getInstance();
		if (instance.listenerMap.containsKey(key)) {
			return instance.annotationMap.get(instance.listenerMap.get(key)).color();
		}
		return 0;
	}

	private int clampPriority(int priority) {
		return Math.max(-10, Math.min(10, priority));
	}

	@ParametersAreNonnullByDefault
	public void registerListener(IGraveInventory iGraveInventory, Map<String, Object> annotation) throws Exception {
		String uniqueId = (String) annotation.get("id");
		annotationMap.put(iGraveInventory, iGraveInventory.getClass().getDeclaredAnnotation(GraveRegistry.class));
		LogHelper.info("Attempting to register special inventory: " + uniqueId);
		if (listenerMap.containsKey(uniqueId)) {
			if (!(boolean) annotation.get("overridable")) {
				if (annotationMap.get(listenerMap.get(uniqueId)).overridable()) {
					listenerMap.put(uniqueId, iGraveInventory);
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
		}
	}

	public void finalizeListeners() {
		sortedListeners = this.listenerMap.entrySet().stream().sorted((x, y) -> {
			int flag = -Integer.compare(
					clampPriority(annotationMap.get(x.getValue()).priority()),
					clampPriority(annotationMap.get(y.getValue()).priority()));
			if (flag == 0) {
				flag = x.getKey().compareTo(y.getKey());
			}
			return flag;
		}).collect(Collectors.toList());

		sortedGuiNames = this.listenerMap.keySet().stream().sorted((x, y) -> {
			if (VanillaInventory.UNIQUE_IDENTIFIER.equals(x)) {
				return -1;
			} else if (VanillaInventory.UNIQUE_IDENTIFIER.equals(y)) {
				return 1;
			} else {
				return (annotationMap.get(listenerMap.get(x)).name()).compareToIgnoreCase((annotationMap.get(listenerMap.get(y)).name()));
			}
		}).collect(Collectors.toList());
	}

	public List<String> getSortedGuiNames() {
		return sortedGuiNames;
	}

	public HashMap<String, TempInventory> grabItemsFromPlayer(EntityPlayer player) {

		Iterator<Map.Entry<String, IGraveInventory>> iter = sortedListeners.iterator();
		boolean shouldContinue = true;
		while (iter.hasNext()) {
			shouldContinue = iter.next().getValue().pregrabLogic(player);
			if (!shouldContinue) {
				break;
			}
		}
		HashMap<String, TempInventory> itms = new HashMap<>();
		if (!shouldContinue) {
			return itms;
		}

		for (Map.Entry<String, IGraveInventory> entry : sortedListeners) {
			TempInventory data = entry.getValue().getItems(player);
			if (data != null) {
				itms.put(entry.getKey(), data);
			}
		}

		return itms;
	}

	public void insertInventory(EntityPlayer player, NBTTagCompound compound, boolean shouldForce) {
		if (compound != null) {
			for (Map.Entry<String, IGraveInventory> entry : sortedListeners) {
				if (compound.hasKey(entry.getKey())) {
					entry.getValue().insertInventory(player, new TempInventory(compound.getTagList(entry.getKey(), 10)), shouldForce);
				}
			}
		}
	}

	public List<ItemStack> generateDrops(NBTTagCompound compound) {
		List<ItemStack> drops = new ArrayList<>();
		if (compound != null) {
			for (Map.Entry<String, IGraveInventory> entry : sortedListeners) {
				if (compound.hasKey(entry.getKey())) {
					TempInventory inv = new TempInventory(compound.getTagList(entry.getKey(), 10));
					drops.addAll(inv.getListOfNonEmptyItemStacks());
				}
			}
		}

		return drops;
	}

	public Map<String, Tuple<String, List<String>>> createItemListForGui(NBTTagCompound compound) {
		Map<String, Tuple<String, List<String>>> theMap = new HashMap<>();

		for (Map.Entry<String, IGraveInventory> entry : sortedListeners) {
			if (compound.hasKey(entry.getKey())) {
				TempInventory inv = new TempInventory(compound.getTagList(entry.getKey(), 10));
				List<ItemStack> drops = inv.getListOfNonEmptyItemStacks();
				if (!drops.isEmpty()) {
					theMap.put(entry.getKey(), new Tuple<>(annotationMap.get(entry.getValue()).name(), TempInventory.getGuiStringsForItemStackList(drops)));
				}
			}
		}

		return theMap;
	}

}