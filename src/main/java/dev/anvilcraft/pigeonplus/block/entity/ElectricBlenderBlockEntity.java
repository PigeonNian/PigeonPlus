package dev.anvilcraft.pigeonplus.block.entity;

import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.block.ElectricBlenderBlock;
import dev.anvilcraft.pigeonplus.init.AddonHeaterInfos;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ElectricBlenderBlockEntity extends BlockEntity implements IPowerConsumer {
    public static final int POWER = 32;

    private PowerGrid grid;

    public ElectricBlenderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ElectricBlenderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_BLENDER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricBlenderBlockEntity blockEntity) {
        blockEntity.flushState(level, pos);

        boolean gridWorking = blockEntity.isGridWorking();
        blockEntity.setHeating(gridWorking);
        blockEntity.setWorking(blockEntity.hasHead() && gridWorking);
        blockEntity.updateHeater(level, pos, gridWorking);
    }

    private void updateHeater(Level level, BlockPos pos, boolean heating) {
        if (heating) {
            HeaterManager.addProducer(pos, level, AddonHeaterInfos.ELECTRIC_BLENDER);
        } else {
            HeaterManager.removeProducer(pos, level, AddonHeaterInfos.ELECTRIC_BLENDER);
        }
    }

    public boolean hasHead() {
        BlockState state = this.getBlockState();
        return state.hasProperty(BlenderBlock.HEAD)
            && state.getValue(BlenderBlock.HEAD) != BlenderBlock.BlenderHead.NONE;
    }

    @Override
    public int getInputPower() {
        return POWER;
    }

    @Override
    public Level getCurrentLevel() {
        return Objects.requireNonNull(this.getLevel());
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Nullable
    @Override
    public PowerGrid getGrid() {
        return this.grid;
    }

    private void setWorking(boolean working) {
        this.setProperty(BlenderBlock.WORKING, working);
    }

    private void setHeating(boolean heating) {
        this.setProperty(ElectricBlenderBlock.HEATING, heating);
    }

    private void setProperty(BooleanProperty property, boolean value) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        BlockState state = this.getBlockState();
        if (state.hasProperty(property) && state.getValue(property) != value) {
            this.level.setBlock(this.worldPosition, state.setValue(property, value), Block.UPDATE_CLIENTS);
        }
    }
}
