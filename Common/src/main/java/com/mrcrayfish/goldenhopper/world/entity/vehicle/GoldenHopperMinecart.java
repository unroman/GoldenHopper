package com.mrcrayfish.goldenhopper.world.entity.vehicle;

import com.mrcrayfish.goldenhopper.core.ModBlocks;
import com.mrcrayfish.goldenhopper.core.ModEntities;
import com.mrcrayfish.goldenhopper.core.ModItems;
import com.mrcrayfish.goldenhopper.world.inventory.GoldenHopperMenu;
import com.mrcrayfish.goldenhopper.world.level.block.entity.AbstractHopperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Author: MrCrayfish
 */
public abstract class GoldenHopperMinecart extends AbstractMinecartContainer implements Hopper, WorldlyContainer
{
    private boolean blocked = true;
    private int transferTicker = -1;
    private final BlockPos lastPosition = BlockPos.ZERO;

    public GoldenHopperMinecart(EntityType<?> type, Level level)
    {
        super(type, level);
    }

    public GoldenHopperMinecart(Level level, double x, double y, double z)
    {
        super(ModEntities.GOLDEN_HOPPER_MINECART.get(), x, y, z, level);
    }

    public boolean isBlocked()
    {
        return blocked;
    }

    public void setBlocked(boolean blocked)
    {
        this.blocked = blocked;
    }

    public void setTransferTicker(int transferTicker)
    {
        this.transferTicker = transferTicker;
    }

    public boolean canTransfer()
    {
        return this.transferTicker > 0;
    }

    @Override
    protected AbstractContainerMenu createMenu(int windowId, Inventory playerInventory)
    {
        return new GoldenHopperMenu(windowId, playerInventory, this);
    }

    @Override
    public BlockState getDefaultDisplayBlockState()
    {
        return ModBlocks.GOLDEN_HOPPER.get().defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset()
    {
        return 1;
    }

    @Override
    public ItemStack getPickResult()
    {
        return new ItemStack(ModItems.GOLDEN_HOPPER_MINECART.get());
    }

    @Override
    public Type getMinecartType()
    {
        return Type.HOPPER;
    }

    @Override
    public double getLevelX()
    {
        return this.getX();
    }

    @Override
    public double getLevelY()
    {
        return this.getY() + 0.5D;
    }

    @Override
    public double getLevelZ()
    {
        return this.getZ();
    }

    @Override
    public int getContainerSize()
    {
        return 6;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    {
        return index != 0 && (this.getItem(0).isEmpty() || stack.getItem() == this.getItem(0).getItem());
    }

    @Override
    public int[] getSlotsForFace(Direction side)
    {
        return IntStream.range(1, this.getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction)
    {
        return index != 0 && (this.getItem(0).isEmpty() || stack.getItem() == this.getItem(0).getItem());
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction)
    {
        return index != 0;
    }

    @Override
    public void activateMinecart(int x, int y, int z, boolean receivingPower)
    {
        if(receivingPower == this.isBlocked())
        {
            this.setBlocked(!receivingPower);
        }
    }

    @Override
    public void tick()
    {
        super.tick();
        if(!this.level.isClientSide && this.isAlive() && this.isBlocked())
        {
            BlockPos pos = this.blockPosition();
            if(pos.equals(this.lastPosition))
            {
                this.transferTicker--;
            }
            else
            {
                this.setTransferTicker(0);
            }

            if(!this.canTransfer())
            {
                this.setTransferTicker(0);
                if(this.captureDroppedItems())
                {
                    this.setTransferTicker(4);
                    this.setChanged();
                }
            }
        }

    }

    protected boolean captureDroppedItems()
    {
        if(!HopperBlockEntity.suckInItems(this.level, this))
        {
            List<ItemEntity> list = this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(0.25D, 0.0D, 0.25D), EntitySelector.ENTITY_STILL_ALIVE);
            if(!list.isEmpty())
            {
                return HopperBlockEntity.addItem(this, list.get(0));
            }
        }
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound)
    {
        super.addAdditionalSaveData(compound);
        compound.putInt("TransferCooldown", this.transferTicker);
        compound.putBoolean("Enabled", this.blocked);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound)
    {
        super.readAdditionalSaveData(compound);
        this.transferTicker = compound.getInt("TransferCooldown");
        this.blocked = !compound.contains("Enabled") || compound.getBoolean("Enabled");
    }
}
