package com.m4thg33k.tombmanygraves.inventory;

import java.util.EnumMap;
import java.util.Map;

import com.m4thg33k.tombmanygraves.api.GraveInventoryHelper;
import com.m4thg33k.tombmanygraves.api.GraveRegistry;
import com.m4thg33k.tombmanygraves.api.IGraveInventory;
import com.m4thg33k.tombmanygraves.api.TempInventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import thebetweenlands.api.capability.IEquipmentCapability;
import thebetweenlands.api.item.IEquippable;
import thebetweenlands.common.capability.equipment.EnumEquipmentInventory;
import thebetweenlands.common.registries.CapabilityRegistry;

@GraveRegistry(id = BetweenlandsInventory.UNIQUE_IDENTIFIER, name = "Betweenlands Equipment", overridable = false)
public class BetweenlandsInventory implements IGraveInventory {

    public static final String UNIQUE_IDENTIFIER = "thebetweenlands";

    private static final Map<EnumEquipmentInventory, Integer> OFFSETS = new EnumMap<>(EnumEquipmentInventory.class);
    private static final int TOTAL_SLOTS;

    static {
        int offset = 0;
        for (EnumEquipmentInventory type : EnumEquipmentInventory.VALUES) {
            OFFSETS.put(type, offset);
            offset += type.maxSize;
        }
        TOTAL_SLOTS = offset;
    }

    @Override
    public boolean pregrabLogic(EntityPlayer player) {
        return true;
    }

    @Override
    public TempInventory getItems(EntityPlayer player) {
        IEquipmentCapability cap = player.getCapability(CapabilityRegistry.CAPABILITY_EQUIPMENT, (EnumFacing) null);
        if (cap == null) {
            return null;
        }

        TempInventory temp = new TempInventory(TOTAL_SLOTS);
        boolean hasAny = false;

        for (EnumEquipmentInventory type : EnumEquipmentInventory.VALUES) {
            IInventory inv = cap.getInventory(type);
            int offset = OFFSETS.get(type);
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (stack.getItem() instanceof IEquippable) {
                    ((IEquippable) stack.getItem()).onUnequip(stack, player, inv);
                }
                temp.setInventorySlotContents(offset + i, stack.copy());
                inv.setInventorySlotContents(i, ItemStack.EMPTY);
                hasAny = true;
            }
        }

        return hasAny ? temp : null;
    }

    @Override
    public void insertInventory(EntityPlayer player, TempInventory graveItems, boolean shouldForce) {
        IEquipmentCapability cap = player.getCapability(CapabilityRegistry.CAPABILITY_EQUIPMENT, (EnumFacing) null);
        if (cap == null) {
            for (ItemStack stack : graveItems.getListOfNonEmptyItemStacks()) {
                GraveInventoryHelper.dropItem(player, stack);
            }
            return;
        }

        for (EnumEquipmentInventory type : EnumEquipmentInventory.VALUES) {
            IInventory inv = cap.getInventory(type);
            int offset = OFFSETS.get(type);

            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack graveItem = graveItems.getStackInSlot(offset + i);
                if (graveItem.isEmpty()) {
                    continue;
                }
                if (inv.getStackInSlot(i).isEmpty()) {
                    inv.setInventorySlotContents(i, graveItem);
                    if (graveItem.getItem() instanceof IEquippable) {
                        ((IEquippable) graveItem.getItem()).onEquip(graveItem, player, inv);
                    }
                } else {
                    GraveInventoryHelper.dropItem(player, graveItem);
                }
            }

            for (int i = inv.getSizeInventory(); i < type.maxSize; i++) {
                ItemStack overflow = graveItems.getStackInSlot(offset + i);
                if (!overflow.isEmpty()) {
                    GraveInventoryHelper.dropItem(player, overflow);
                }
            }
        }
    }
}