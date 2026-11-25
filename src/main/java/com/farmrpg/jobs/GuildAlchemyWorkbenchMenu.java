package com.farmrpg.jobs;

import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** A view of the server-owned workbench; no recipe can be crafted at this stage. */
public final class GuildAlchemyWorkbenchMenu extends AbstractContainerMenu {
    public static final int WIDTH = 312;
    public static final int HEIGHT = 220;
    public static final int INVENTORY_X = 75;
    public static final int INVENTORY_Y = 134;
    public static final int HOTBAR_Y = 190;
    public static final int RECIPE_BUTTON_BASE = 0;
    public static final int GRADE_BUTTON_BASE = 10;
    public static final int FILL_WATER_BUTTON = 20;
    private static final int PLAYER_START = GuildAlchemyWorkbenchBlockEntity.SLOT_COUNT;
    private static final int HOTBAR_START = PLAYER_START + 27;
    private static final int PLAYER_END = HOTBAR_START + 9;
    private static final int WATER = 0;
    private static final int RECIPE = 1;
    private static final int GRADE = 2;
    private static final int DATA_COUNT = 3;

    private final ContainerData data;
    @Nullable private final GuildAlchemyWorkbenchBlockEntity workbench;

    /** Client constructor: menu packets replace the placeholder contents. */
    public GuildAlchemyWorkbenchMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new ItemStackHandler(GuildAlchemyWorkbenchBlockEntity.SLOT_COUNT),
                new SimpleContainerData(DATA_COUNT), null);
    }

    /** Each player has independent recipe choices while sharing the real workbench storage. */
    public GuildAlchemyWorkbenchMenu(int id, Inventory playerInventory, GuildAlchemyWorkbenchBlockEntity workbench) {
        this(id, playerInventory, workbench.inventory(), new WorkbenchData(workbench), workbench);
    }

    private GuildAlchemyWorkbenchMenu(int id, Inventory playerInventory, IItemHandlerModifiable inventory,
            ContainerData data, @Nullable GuildAlchemyWorkbenchBlockEntity workbench) {
        super(ModMenus.GUILD_ALCHEMY.get(), id);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.workbench = workbench;
        int[][] positions = {{32, 53}, {60, 53}, {87, 53}, {42, 80}, {77, 80}, {194, 109}};
        for (int i = 0; i < positions.length; i++) {
            int index = i;
            addSlot(new SlotItemHandler(inventory, index, positions[i][0], positions[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return GuildAlchemyWorkbenchBlockEntity.isValidInput(index, stack);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    // Vanilla can grow an existing stack in place without invoking the handler.
                    if (workbench != null && !workbench.isRemoved()) workbench.setChanged();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    public int waterAmount() { return data.get(WATER); }
    public int selectedRecipe() { return data.get(RECIPE); }
    public int selectedGrade() { return data.get(GRADE); }

    @Override
    public boolean stillValid(Player player) {
        if (workbench == null) return true; // Client-side display only.
        Level level = workbench.getLevel();
        return level != null && !workbench.isRemoved()
                && level.getBlockEntity(workbench.getBlockPos()) == workbench
                && player.canInteractWithBlock(workbench.getBlockPos(), 4.0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (workbench == null || player.containerMenu != this || !stillValid(player)) return false;
        if (id >= RECIPE_BUTTON_BASE && id < RECIPE_BUTTON_BASE + 4) {
            data.set(RECIPE, id - RECIPE_BUTTON_BASE);
            return true;
        }
        if (id >= GRADE_BUTTON_BASE && id < GRADE_BUTTON_BASE + 4) {
            data.set(GRADE, id - GRADE_BUTTON_BASE);
            return true;
        }
        if (id != FILL_WATER_BUTTON) return false;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack bucket = inventory.getItem(slot);
            if (bucket.is(Items.WATER_BUCKET)) {
                if (!workbench.fillWaterBucket()) return false;
                inventory.setItem(slot, ItemUtils.createFilledResult(bucket, player,
                        BucketItem.getEmptySuccessItem(bucket, player)));
                inventory.setChanged();
                player.level().playSound(null, workbench.getBlockPos(), SoundEvents.BUCKET_EMPTY,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (workbench != null && (player.containerMenu != this || !stillValid(player))) return ItemStack.EMPTY;
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();
        boolean moved = false;
        if (index < PLAYER_START) {
            moved = moveItemStackTo(stack, PLAYER_START, PLAYER_END, true);
        } else {
            if (GuildAlchemyWorkbenchBlockEntity.isValidInput(0, stack)) {
                moved = moveItemStackTo(stack, 0, 3, false);
            } else if (GuildAlchemyWorkbenchBlockEntity.isValidInput(3, stack)) {
                moved = moveItemStackTo(stack, 3, 4, false);
            } else if (GuildAlchemyWorkbenchBlockEntity.isValidInput(4, stack)) {
                moved = moveItemStackTo(stack, 4, 5, false);
            }
            if (!moved) {
                moved = index < HOTBAR_START
                        ? moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false)
                        : moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false);
            }
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        source.onTake(player, stack);
        return original;
    }

    private static final class WorkbenchData implements ContainerData {
        private final GuildAlchemyWorkbenchBlockEntity workbench;
        private int recipe;
        private int grade;

        private WorkbenchData(GuildAlchemyWorkbenchBlockEntity workbench) { this.workbench = workbench; }
        @Override public int get(int index) {
            return switch (index) {
                case WATER -> workbench.waterAmount();
                case RECIPE -> recipe;
                case GRADE -> grade;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == RECIPE && value >= 0 && value < 4) recipe = value;
            if (index == GRADE && value >= 0 && value < 4) grade = value;
        }
        @Override public int getCount() { return DATA_COUNT; }
    }
}
