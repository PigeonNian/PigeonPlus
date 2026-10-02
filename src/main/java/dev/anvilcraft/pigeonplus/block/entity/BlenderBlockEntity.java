package dev.anvilcraft.pigeonplus.block.entity;

import dev.anvilcraft.pigeonplus.block.BlenderBlock;
import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.anvilcraft.pigeonplus.util.FluidTransactions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

public class BlenderBlockEntity extends BlockEntity {
    private static final int AIR_CAPACITY = 1000;
    private static final int AIR_CONSUME_PER_TICK = 20;

    /**
     * 压缩空气储罐。
     *
     * <p>26.1 起用 {@link FluidStacksResourceHandler}（新流体能力的基类）取代
     * 旧的 {@code FluidTank}。两者的差别不只是改名：新接口按
     * 「资源 + 数量」表达，且 {@code insert}/{@code extract} 都要走事务。
     * 只接受压缩空气这一点仍由覆写 {@code isValid} 保证。
     */
    private final FluidStacksResourceHandler compressedAirTank = new FluidStacksResourceHandler(1, AIR_CAPACITY) {
        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.getFluid().isSame(AddonFluids.COMPRESSED_AIR.get());
        }

        @Override
        protected void onContentsChanged(int index, FluidStack stack) {
            BlenderBlockEntity.this.onAirChanged();
        }
    };
    private final ResourceHandler<FluidResource> inputHandler = new InputOnlyFluidHandler();

    public BlenderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public BlenderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLENDER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlenderBlockEntity blockEntity) {
        if (blockEntity.compressedAirTank.getAmountAsLong(0) > 0) {
            blockEntity.setWorking(true);
            FluidResource air = blockEntity.compressedAirTank.getResource(0);
            FluidTransactions.extract(blockEntity.compressedAirTank, 0, air, AIR_CONSUME_PER_TICK);
        } else {
            blockEntity.setWorking(false);
        }
    }

    @Nullable
    public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction side) {
        if (side == null || side == this.getBlockState().getValue(BlenderBlock.FACING).getOpposite()) {
            return this.inputHandler;
        }
        return null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // 26.1 的 ValueOutput 支持直接写子对象，不再需要手工开 CompoundTag
        this.compressedAirTank.serialize(output.child("CompressedAir"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.compressedAirTank.deserialize(input.childOrEmpty("CompressedAir"));
    }

    private void onAirChanged() {
        this.setChanged();
        this.setWorking(this.compressedAirTank.getAmountAsLong(0) > 0);
    }

    private void setWorking(boolean working) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        BlockState state = this.getBlockState();
        if (state.hasProperty(BlenderBlock.WORKING) && state.getValue(BlenderBlock.WORKING) != working) {
            this.level.setBlock(this.worldPosition, state.setValue(BlenderBlock.WORKING, working), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * 只允许注入、不允许抽取的视图。
     *
     * <p>搅拌机从上方管道接收压缩空气，但不应该被管道反向抽走，
     * 因此这里把所有 {@code extract} 都返回 0。
     */
    private class InputOnlyFluidHandler implements ResourceHandler<FluidResource> {
        @Override
        public int size() {
            return compressedAirTank.size();
        }

        @Override
        public FluidResource getResource(int index) {
            return compressedAirTank.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return compressedAirTank.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return compressedAirTank.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return compressedAirTank.isValid(index, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return compressedAirTank.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }
}
