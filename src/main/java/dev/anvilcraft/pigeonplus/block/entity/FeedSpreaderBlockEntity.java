package dev.anvilcraft.pigeonplus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class FeedSpreaderBlockEntity extends BlockEntity {
    public static final int FEED_SLOT = 0;
    public static final int UNUSED_SLOT = 1;

    private static final int SPREAD_PARTICLE_EVENT = 1;
    private static final int SPREAD_MATERIAL_NONE = 0;
    private static final int SPREAD_MATERIAL_BONE_MEAL = 1;
    private static final int SPREAD_MATERIAL_FEED = 2;
    private static final float PISTON_PRESS_STEP = 0.18F;
    private static final float PISTON_RELEASE_STEP = 0.25F;
    private static final float BUCKET_ROTATION_STEP = 1.0F / 24.0F;
    private static final int PISTON_RELEASE_DELAY_TICKS = 20;
    private static final int SPREAD_PARTICLE_DELAY_TICKS = 5;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FEED_SLOT && FeedSpreaderBlockEntity.isFeedOrBoneMeal(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            FeedSpreaderBlockEntity.this.setChanged();
        }
    };

    private float pistonPress;
    private float pistonPressOld;
    private boolean pistonPressing;
    private boolean pistonHolding;
    private int pistonReleaseDelay;
    private float bucketRotation;
    private float bucketRotationOld;
    private boolean bucketRotating;
    private int spreadParticleRadius;
    private int spreadParticleMaterial;
    private int spreadParticleDelayTicks;
    private boolean spreadParticleBurstPending;

    public FeedSpreaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEED_SPREADER.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public static boolean isFeedOrBoneMeal(ItemStack stack) {
        return stack.is(Items.BONE_MEAL)
            || stack.is(ItemTags.COW_FOOD)
            || stack.is(ItemTags.SHEEP_FOOD)
            || stack.is(ItemTags.GOAT_FOOD)
            || stack.is(ItemTags.HORSE_FOOD)
            || stack.is(ItemTags.LLAMA_FOOD)
            || stack.is(ItemTags.PIG_FOOD)
            || stack.is(ItemTags.CHICKEN_FOOD)
            || stack.is(ItemTags.RABBIT_FOOD)
            || stack.is(ItemTags.CAMEL_FOOD)
            || stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS);
    }

    public ItemStack insertFeed(ItemStack stack, boolean simulate) {
        return this.inventory.insertItem(FEED_SLOT, stack, simulate);
    }

    public ItemStack extractFirstItem() {
        for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
            ItemStack stack = this.inventory.extractItem(slot, this.inventory.getSlotLimit(slot), false);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public void activate(float fallDistance) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        ItemStack feed = this.inventory.getStackInSlot(FEED_SLOT);
        if (feed.isEmpty()) {
            return;
        }

        int radius = getEffectRadius(fallDistance);
        if (feed.is(Items.BONE_MEAL)) {
            this.sendSpreadParticleEvent(radius, SPREAD_MATERIAL_BONE_MEAL);
            this.applyBoneMeal(radius);
        } else {
            this.sendSpreadParticleEvent(radius, SPREAD_MATERIAL_FEED);
            this.feedAnimals(radius);
        }
    }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        return drops;
    }

    private static int getEffectRadius(float fallDistance) {
        return Mth.clamp(Mth.ceil(fallDistance), 1, 4);
    }

    private void sendSpreadParticleEvent(int radius, int material) {
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), SPREAD_PARTICLE_EVENT, packSpreadEvent(radius, material));
        this.spreadParticleRadius = radius;
        this.spreadParticleMaterial = material;
        this.spreadParticleDelayTicks = SPREAD_PARTICLE_DELAY_TICKS;
    }

    private static int packSpreadEvent(int radius, int material) {
        return (Mth.clamp(material, SPREAD_MATERIAL_NONE, SPREAD_MATERIAL_FEED) << 8) | Mth.clamp(radius, 1, 4);
    }

    private static int unpackSpreadRadius(int data) {
        return Mth.clamp(data & 0xFF, 1, 4);
    }

    private static int unpackSpreadMaterial(int data) {
        return Mth.clamp((data >> 8) & 0xFF, SPREAD_MATERIAL_NONE, SPREAD_MATERIAL_FEED);
    }

    private void sendServerSpreadParticles(ServerLevel serverLevel, int radius, int material) {
        ParticleOptions particle = material == SPREAD_MATERIAL_BONE_MEAL
            ? new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.BONE_MEAL))
            : new ItemParticleOption(ParticleTypes.ITEM, this.getSpreadParticleStack());
        int count = 60 + radius * 24;
        double spread = 0.25 + radius * 0.18;
        serverLevel.sendParticles(
            particle,
            this.worldPosition.getX() + 0.5,
            this.worldPosition.getY() + 0.82,
            this.worldPosition.getZ() + 0.5,
            count,
            spread,
            0.12,
            spread,
            0.16 + radius * 0.03
        );
        if (material == SPREAD_MATERIAL_BONE_MEAL) {
            serverLevel.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.82,
                this.worldPosition.getZ() + 0.5,
                18 + radius * 8,
                spread,
                0.12,
                spread,
                0.08
            );
        }
    }

    private void applyBoneMeal(int radius) {
        for (BlockPos targetPos : BlockPos.betweenClosed(
            this.worldPosition.offset(-radius, 0, -radius),
            this.worldPosition.offset(radius, 0, radius)
        )) {
            ItemStack feed = this.inventory.getStackInSlot(FEED_SLOT);
            if (feed.isEmpty()) {
                return;
            }
            ItemStack temporaryBoneMeal = new ItemStack(Items.BONE_MEAL);
            if (BoneMealItem.applyBonemeal(temporaryBoneMeal, this.level, targetPos, null)) {
                this.level.levelEvent(1505, targetPos, 0);
                if (this.level.random.nextFloat() < 0.3F) {
                    feed.shrink(1);
                    this.setChanged();
                }
            }
        }
    }

    private void feedAnimals(int radius) {
        ItemStack feed = this.inventory.getStackInSlot(FEED_SLOT);
        if (feed.isEmpty()) {
            return;
        }
        AABB bounds = new AABB(
            this.worldPosition.getX() - radius,
            this.worldPosition.getY() - 1,
            this.worldPosition.getZ() - radius,
            this.worldPosition.getX() + radius + 1,
            this.worldPosition.getY() + 2,
            this.worldPosition.getZ() + radius + 1
        );
        ItemStack initialFeed = feed.copy();
        List<Animal> animals = this.level.getEntitiesOfClass(Animal.class, bounds, animal -> animal.isFood(initialFeed));
        for (Animal animal : animals) {
            feed = this.inventory.getStackInSlot(FEED_SLOT);
            if (feed.isEmpty()) {
                return;
            }
            if (feedAnimal(animal, feed)) {
                feed.shrink(1);
                this.setChanged();
            }
        }
    }

    private static boolean feedAnimal(Animal animal, ItemStack feed) {
        if (!animal.isFood(feed)) {
            return false;
        }
        int age = animal.getAge();
        if (age < 0) {
            animal.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-age), true);
            return true;
        }
        if (age == 0 && animal.canFallInLove()) {
            animal.setInLove(null);
            return true;
        }
        return false;
    }

    public float getPistonPress(float partialTick) {
        float press = this.pistonPressOld + (this.pistonPress - this.pistonPressOld) * partialTick;
        return Math.max(0.0F, Math.min(press, 1.0F));
    }

    public float getBucketRotation(float partialTick) {
        float rotation = this.bucketRotationOld + (this.bucketRotation - this.bucketRotationOld) * partialTick;
        return Math.max(0.0F, Math.min(rotation, 1.0F));
    }

    public void startPistonPressAnimation() {
        if (this.pistonPress >= 1.0F || this.pistonPressing) {
            return;
        }
        this.pistonPress = 0.0F;
        this.pistonPressOld = 0.0F;
        this.pistonPressing = true;
        this.pistonHolding = false;
        this.pistonReleaseDelay = PISTON_RELEASE_DELAY_TICKS;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FeedSpreaderBlockEntity entity) {
        if (level.isClientSide()) {
            entity.tickPistonPressAnimation(level, pos);
            entity.tickBucketRotationAnimation();
        } else {
            entity.tickServerSpreadParticles();
        }
    }

    private void tickPistonPressAnimation(Level level, BlockPos pos) {
        this.pistonPressOld = this.pistonPress;
        boolean hasAnvilOnTop = hasAnvilOnTop(level, pos);
        if (!this.pistonPressing) {
            if (hasAnvilOnTop) {
                this.pistonHolding = true;
                this.pistonPress = 1.0F;
                this.pistonReleaseDelay = PISTON_RELEASE_DELAY_TICKS;
                return;
            }
            if (this.pistonHolding) {
                if (this.pistonReleaseDelay > 0) {
                    this.pistonReleaseDelay--;
                    return;
                }
                this.pistonHolding = false;
            }
            this.tickPistonReleaseAnimation();
            return;
        }
        this.pistonPress = Math.min(1.0F, this.pistonPress + PISTON_PRESS_STEP);
        if (this.pistonPress >= 1.0F) {
            this.pistonPress = 1.0F;
            this.pistonPressing = false;
            this.pistonHolding = hasAnvilOnTop;
            this.pistonReleaseDelay = PISTON_RELEASE_DELAY_TICKS;
            if (!this.bucketRotating) {
                this.startBucketRotationAnimation(SPREAD_MATERIAL_NONE, 0);
            }
        }
    }

    private void startBucketRotationAnimation(int material, int radius) {
        this.bucketRotation = 0.0F;
        this.bucketRotationOld = 0.0F;
        this.bucketRotating = true;
        this.spreadParticleMaterial = material;
        this.spreadParticleRadius = radius;
        this.spreadParticleDelayTicks = material == SPREAD_MATERIAL_NONE ? 0 : SPREAD_PARTICLE_DELAY_TICKS;
        this.spreadParticleBurstPending = material != SPREAD_MATERIAL_NONE;
    }

    private void tickBucketRotationAnimation() {
        this.bucketRotationOld = this.bucketRotation;
        if (!this.bucketRotating) {
            return;
        }
        this.bucketRotation = Math.min(1.0F, this.bucketRotation + BUCKET_ROTATION_STEP);
        if (this.spreadParticleDelayTicks > 0) {
            this.spreadParticleDelayTicks--;
        } else {
            if (this.spreadParticleBurstPending) {
                this.spawnSpreadParticleBurst();
                this.spreadParticleBurstPending = false;
            }
            this.spawnSpreadParticles();
        }
        if (this.bucketRotation >= 1.0F) {
            this.bucketRotation = 0.0F;
            this.bucketRotationOld = 0.0F;
            this.bucketRotating = false;
            this.spreadParticleMaterial = SPREAD_MATERIAL_NONE;
            this.spreadParticleRadius = 0;
            this.spreadParticleDelayTicks = 0;
            this.spreadParticleBurstPending = false;
        }
    }

    private void tickServerSpreadParticles() {
        if (!(this.level instanceof ServerLevel serverLevel)
            || this.spreadParticleMaterial == SPREAD_MATERIAL_NONE
            || this.spreadParticleRadius <= 0) {
            return;
        }
        if (this.spreadParticleDelayTicks > 0) {
            this.spreadParticleDelayTicks--;
            return;
        }
        this.sendServerSpreadParticles(serverLevel, this.spreadParticleRadius, this.spreadParticleMaterial);
        this.spreadParticleMaterial = SPREAD_MATERIAL_NONE;
        this.spreadParticleRadius = 0;
    }

    private void spawnSpreadParticles() {
        if (this.level == null || this.spreadParticleMaterial == SPREAD_MATERIAL_NONE || this.spreadParticleRadius <= 0) {
            return;
        }
        this.spawnSpreadParticles(5 + this.spreadParticleRadius * 4, 1.0);
    }

    private void spawnSpreadParticleBurst() {
        if (this.level == null || this.spreadParticleMaterial == SPREAD_MATERIAL_NONE || this.spreadParticleRadius <= 0) {
            return;
        }
        this.spawnSpreadParticles(32 + this.spreadParticleRadius * 12, 1.35);
    }

    private void spawnSpreadParticles(int count, double speedMultiplier) {
        double easedProgress = easeInOut(this.bucketRotation);
        double baseAngle = easedProgress * Math.PI * 4.0;
        double speed = (0.11 + this.spreadParticleRadius * 0.045) * speedMultiplier;
        for (int i = 0; i < count; i++) {
            double angle = baseAngle + this.level.random.nextDouble() * 0.9 - 0.45;
            double directionX = Math.cos(angle);
            double directionZ = Math.sin(angle);
            double side = (this.level.random.nextDouble() - 0.5) * 0.24 * this.spreadParticleRadius;
            double x = this.worldPosition.getX() + 0.5 + directionX * 0.46 - directionZ * side;
            double y = this.worldPosition.getY() + 0.78 + this.level.random.nextDouble() * 0.16;
            double z = this.worldPosition.getZ() + 0.5 + directionZ * 0.46 + directionX * side;
            double distanceScale = 0.65 + this.level.random.nextDouble() * 0.95;
            double xSpeed = directionX * speed * distanceScale + (this.level.random.nextDouble() - 0.5) * 0.025;
            double ySpeed = 0.045 + this.level.random.nextDouble() * 0.07;
            double zSpeed = directionZ * speed * distanceScale + (this.level.random.nextDouble() - 0.5) * 0.025;

            this.level.addParticle(this.getSpreadParticle(), x, y, z, xSpeed, ySpeed, zSpeed);
            if (this.spreadParticleMaterial == SPREAD_MATERIAL_BONE_MEAL && this.level.random.nextFloat() < 0.35F) {
                this.level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, xSpeed * 0.65, ySpeed * 0.6, zSpeed * 0.65);
                this.level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, xSpeed * 0.45, ySpeed * 0.5, zSpeed * 0.45);
            }
        }
    }

    private ParticleOptions getSpreadParticle() {
        return new ItemParticleOption(ParticleTypes.ITEM, this.getSpreadParticleStack());
    }

    private ItemStack getSpreadParticleStack() {
        if (this.spreadParticleMaterial == SPREAD_MATERIAL_BONE_MEAL) {
            return new ItemStack(Items.BONE_MEAL);
        }
        ItemStack stack = this.inventory.getStackInSlot(FEED_SLOT);
        return !stack.isEmpty() && !stack.is(Items.BONE_MEAL) ? stack.copyWithCount(1) : new ItemStack(Items.WHEAT);
    }

    private static float easeInOut(float progress) {
        return progress * progress * progress * (progress * (progress * 6.0F - 15.0F) + 10.0F);
    }

    private void tickPistonReleaseAnimation() {
        if (this.pistonPress <= 0.0F) {
            return;
        }
        if (this.pistonReleaseDelay > 0) {
            this.pistonReleaseDelay--;
            return;
        }
        this.pistonPress = Math.max(0.0F, this.pistonPress - PISTON_RELEASE_STEP);
    }

    private static boolean hasAnvilOnTop(Level level, BlockPos pos) {
        return level.getBlockState(pos.above()).getBlock() instanceof AnvilBlock;
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == SPREAD_PARTICLE_EVENT) {
            if (this.level == null || this.level.isClientSide()) {
                this.startBucketRotationAnimation(unpackSpreadMaterial(type), unpackSpreadRadius(type));
            }
            return true;
        }
        return super.triggerEvent(id, type);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
