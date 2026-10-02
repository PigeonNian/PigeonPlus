package dev.anvilcraft.pigeonplus.util;

import dev.anvilcraft.pigeonplus.init.AddonFluids;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 把「暴露在空气中的排水口」包装成一个压缩空气源。
 *
 * <h3>26.1 的改写</h3>
 * 流体能力从 {@code IFluidHandler} 换成了
 * {@link ResourceHandler}{@code <}{@link FluidResource}{@code >}：
 * <ul>
 *   <li>「槽位」改为「资源 + 数量」两个方法：
 *       {@code getResource(i)} / {@code getAmountAsLong(i)}。</li>
 *   <li>容量按 (槽位, 资源) 查询：{@code getCapacityAsLong(i, resource)}。</li>
 *   <li>{@code fill}/{@code drain} 换成
 *       {@code insert(...)} / {@code extract(...)}，且都接收
 *       {@link TransactionContext}——操作只在事务提交后才真正生效。</li>
 * </ul>
 *
 * <p>本类不再继承 {@code InfinityFluidTank}：那个基类的语义是
 * 「若干个真实槽位 + 无限容量」，而这里需要的是「代理真实槽位 +
 * 追加一个**虚拟**气罐」，索引映射完全自定义，直接实现接口更清晰，
 * 也不会与基类的内部槽位列表互相干扰。
 *
 * <p>虚拟气罐不写入任何状态：它表示「空气中取之不尽的压缩空气」，
 * 每次抽取都凭空产出，因此不需要（也不应该）持久化。
 */
public class CompressedAirDrainFluidHandler implements ResourceHandler<FluidResource> {
    private static final int AIR_CAPACITY = DrainBlockEntity.CAPACITY;

    private final Level level;
    private final BlockPos pos;
    private final ResourceHandler<FluidResource> delegate;

    public CompressedAirDrainFluidHandler(Level level, BlockPos pos, ResourceHandler<FluidResource> delegate) {
        this.level = level;
        this.pos = pos.immutable();
        this.delegate = delegate;
    }

    // ------------------------------------------------------------------
    // 槽位布局：前 delegate.size() 个是排水口自己的真实槽位，
    // 最后一个（索引 delegate.size()）是虚拟气罐。
    // ------------------------------------------------------------------

    @Override
    public int size() {
        return this.delegate.size() + 1;
    }

    @Override
    public FluidResource getResource(int index) {
        if (index < this.delegate.size()) {
            return this.delegate.getResource(index);
        }
        return this.canExtractAir()
            ? FluidResource.of(AddonFluids.COMPRESSED_AIR.get())
            : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        if (index < this.delegate.size()) {
            return this.delegate.getAmountAsLong(index);
        }
        return this.canExtractAir() ? AIR_CAPACITY : 0;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        if (index < this.delegate.size()) {
            return this.delegate.getCapacityAsLong(index, resource);
        }
        return AIR_CAPACITY;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (index < this.delegate.size()) {
            return this.delegate.isValid(index, resource);
        }
        // 虚拟气罐只出不进
        return false;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (index < this.delegate.size()) {
            return this.delegate.insert(index, resource, amount, transaction);
        }
        // 往「空气」里灌流体没有意义
        return 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (index < this.delegate.size()) {
            return this.delegate.extract(index, resource, amount, transaction);
        }
        if (!this.canExtractAir() || amount <= 0) {
            return 0;
        }
        // 只认压缩空气
        if (!resource.getFluid().isSame(AddonFluids.COMPRESSED_AIR.get())) {
            return 0;
        }
        int extracted = Math.min(amount, AIR_CAPACITY);
        if (extracted <= 0) {
            return 0;
        }
        // 虚拟气罐没有任何状态，因此「回滚」对它而言本就是空操作，
        // 不需要 SnapshotJournal 之类的快照机制。
        // 抽气粒子是纯表现层，直接触发即可；即便外层事务最终回滚，
        // 也只是一次无害的视觉误差（不会有资源凭空产生）。
        this.spawnAirIntakeParticles(extracted);
        return extracted;
    }

    /**
     * 内部真实槽位是否全空。
     *
     * <p>只有排水口自己没东西时，才让它「从空气中吸气」——
     * 否则会掩盖掉真实流体的抽取，导致玩家桶里的流体排不出去。
     */
    public boolean isInternalTankEmpty() {
        for (int i = 0; i < this.delegate.size(); i++) {
            if (!this.delegate.getResource(i).isEmpty() && this.delegate.getAmountAsLong(i) > 0) {
                return false;
            }
        }
        return true;
    }

    /** 排水口是否暴露在空气中（有任意一面朝向空气）。 */
    public boolean canExtractAir() {
        return this.isInternalTankEmpty() && isDrainAirExposed(this.level, this.pos);
    }

    private void spawnAirIntakeParticles(int amount) {
        if (amount <= 0 || !(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        RandomSource random = serverLevel.getRandom();
        int count = Math.clamp(amount / 80, 2, 8);
        Vec3 center = Vec3.atCenterOf(this.pos);
        for (int i = 0; i < count; i++) {
            double x = this.pos.getX() - 1.0 + random.nextDouble() * 3.0;
            double y = this.pos.getY() - 1.0 + random.nextDouble() * 3.0;
            double z = this.pos.getZ() - 1.0 + random.nextDouble() * 3.0;
            Vec3 particlePos = new Vec3(x, y, z);
            Vec3 velocity = center.subtract(particlePos)
                .normalize()
                .scale(0.08 + random.nextDouble() * 0.08)
                .add(
                    (random.nextDouble() - 0.5) * 0.015,
                    (random.nextDouble() - 0.5) * 0.015,
                    (random.nextDouble() - 0.5) * 0.015
                );
            serverLevel.sendParticles(
                ParticleTypes.CLOUD,
                particlePos.x,
                particlePos.y,
                particlePos.z,
                0,
                velocity.x,
                velocity.y,
                velocity.z,
                1.0
            );
        }
    }

    public static boolean isDrainAirExposed(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos checkPos = pos.relative(direction);
            if (!level.isLoaded(checkPos)) {
                continue;
            }
            BlockState state = level.getBlockState(checkPos);
            if (state.isAir() && state.getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
