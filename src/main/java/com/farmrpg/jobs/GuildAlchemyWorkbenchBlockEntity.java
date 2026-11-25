package com.farmrpg.jobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Server-owned storage for one workbench. Crafting will be added separately. */
public final class GuildAlchemyWorkbenchBlockEntity extends BlockEntity {
    public static final int SLOT_COUNT = 6;
    public static final int FIRST_HERB_SLOT = 0;
    public static final int LAST_HERB_SLOT = 2;
    public static final int CATALYST_SLOT = 3;
    public static final int BOTTLE_SLOT = 4;
    public static final int OUTPUT_SLOT = 5;
    public static final int WATER_CAPACITY = 4000;
    private static final String INVENTORY_TAG = "Inventory";
    private static final String WATER_TAG = "WaterAmount";

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidInput(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final FluidTank water = new FluidTank(WATER_CAPACITY, stack -> stack.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
    };

    public GuildAlchemyWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GUILD_ALCHEMY_WORKBENCH.get(), pos, state);
    }

    static boolean isValidInput(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot >= FIRST_HERB_SLOT && slot <= LAST_HERB_SLOT) {
            return stack.getItem() instanceof MoonlightHerbItem;
        }
        if (slot == CATALYST_SLOT) {
            return stack.is(Items.REDSTONE) || stack.is(Items.GLOWSTONE_DUST) || stack.is(Items.BLAZE_POWDER);
        }
        return slot == BOTTLE_SLOT && stack.is(Items.GLASS_BOTTLE);
    }

    // Package-private: no hopper or pipe capability is exposed at this stage.
    ItemStackHandler inventory() {
        return inventory;
    }

    public int waterAmount() {
        return water.getFluidAmount();
    }

    /** Fill a whole vanilla water bucket, or leave the tank unchanged. */
    boolean fillWaterBucket() {
        if (level == null || level.isClientSide) {
            return false;
        }
        FluidStack bucket = new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME);
        if (water.fill(bucket, FluidAction.SIMULATE) != FluidType.BUCKET_VOLUME) {
            return false;
        }
        return water.fill(bucket, FluidAction.EXECUTE) == FluidType.BUCKET_VOLUME;
    }

    /** Future crafting can drain only after all ingredients have been validated. */
    boolean drainWater(int amount, boolean simulate) {
        if (level == null || level.isClientSide || amount <= 0) {
            return false;
        }
        FluidStack available = water.drain(amount, FluidAction.SIMULATE);
        if (available.getAmount() != amount) {
            return false;
        }
        if (!simulate) {
            water.drain(amount, FluidAction.EXECUTE);
        }
        return true;
    }

    void dropInventory() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            while (!inventory.getStackInSlot(slot).isEmpty()) {
                ItemStack stack = inventory.extractItem(slot, Integer.MAX_VALUE, false);
                if (stack.isEmpty()) {
                    break;
                }
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(INVENTORY_TAG, inventory.serializeNBT(registries));
        tag.putInt(WATER_TAG, waterAmount());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag inventoryTag = tag.getCompound(INVENTORY_TAG).copy();
        // A malformed Size field must never expand the six-slot workbench.
        inventoryTag.putInt("Size", SLOT_COUNT);
        inventory.deserializeNBT(registries, inventoryTag);
        loadWater(tag);
    }

    private void loadWater(CompoundTag tag) {
        int amount = Mth.clamp(tag.getInt(WATER_TAG), 0, WATER_CAPACITY);
        water.setFluid(amount == 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, amount));
    }

    // Only water is broadcast to nearby clients; menu viewers receive slot updates separately.
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(WATER_TAG, waterAmount());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadWater(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }
}
