package dev.anvilcraft.pigeonplus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.anvilcraft.pigeonplus.block.entity.LargeFireworksRocketBlockEntity;
import dev.anvilcraft.pigeonplus.block.state.LargeFireworksRocketPart;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketEntity;
import dev.anvilcraft.pigeonplus.entity.LargeFireworksRocketSeatEntity;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LargeFireworksRocketBlock extends SimpleMultiPartBlock<LargeFireworksRocketPart>
    implements IHammerRemovable, EntityBlock {
    public static final MapCodec<LargeFireworksRocketBlock> CODEC = simpleCodec(LargeFireworksRocketBlock::new);
    public static final EnumProperty<LargeFireworksRocketPart> PART =
        EnumProperty.create("part", LargeFireworksRocketPart.class);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final AABB[] GLOBAL_SHAPE = {
        new AABB(11.0, 0.0, 11.0, 23.0, 32.0, 23.0),
        new AABB(-8.0, 32.0, -8.0, 24.0, 48.0, 24.0),
        new AABB(1.0, 25.0, 5.0, 3.0, 32.0, 7.0),
        new AABB(0.0, 18.0, 4.0, 2.0, 25.0, 6.0),
        new AABB(-8.0, 48.0, -8.0, 24.0, 96.0, 24.0),
        new AABB(-16.0, 112.0, -16.0, 32.0, 118.0, 32.0),
        new AABB(-12.0, 118.0, -12.0, 28.0, 125.0, 28.0),
        new AABB(-8.0, 125.0, -8.0, 24.0, 133.0, 24.0),
        new AABB(-4.0, 133.0, -4.0, 20.0, 142.0, 20.0),
        new AABB(-8.0, 96.0, -8.0, 24.0, 112.0, 24.0),
    };

    private static final Map<Direction, Map<LargeFireworksRocketPart, VoxelShape>> SHAPES_BY_FACING =
        createShapesByFacing();

    public LargeFireworksRocketBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(PART, LargeFireworksRocketPart.Y0_C)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACING);
    }

    @Override
    public Property<LargeFireworksRocketPart> getPart() {
        return PART;
    }

    @Override
    public LargeFireworksRocketPart[] getParts() {
        return LargeFireworksRocketPart.values();
    }

    @Override
    public BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        return state.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public Vec3i getMainPartOffset() {
        return Vec3i.ZERO;
    }

    @Override
    public VoxelShape getPartShape(BlockState state) {
        if (!state.hasProperty(PART) || !state.hasProperty(FACING)) {
            return Shapes.block();
        }
        return SHAPES_BY_FACING
            .getOrDefault(state.getValue(FACING), Map.of())
            .getOrDefault(state.getValue(PART), Shapes.block());
    }

    @Override
    public BlockState getModelHolderState(BlockState original) {
        return original.setValue(PART, LargeFireworksRocketPart.Y1_C);
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof AnvilHammerItem) {
            return InteractionResult.PASS;
        }
        if (held.is(Items.FIREWORK_STAR)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return level instanceof ServerLevel serverLevel
                ? this.installStar(serverLevel, pos, state, player, held)
                : InteractionResult.PASS;
        }
        if (held.is(Items.ENDER_PEARL)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return level instanceof ServerLevel serverLevel
                ? this.installPearl(serverLevel, pos, state, player, held)
                : InteractionResult.PASS;
        }
        if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) {
            return this.ignite(state, level, pos, player, hand, held);
        }

        if (held.isEmpty()) {
            return this.sitOrStand(level, pos, state, player);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult sitOrStand(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {

            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        if (player.getVehicle() instanceof LargeFireworksRocketSeatEntity) {
            player.stopRiding();
            return InteractionResult.CONSUME;
        }
        BlockPos top = this.getMainPartPos(pos, state).above(LargeFireworksRocketPart.HEIGHT);
        for (LargeFireworksRocketSeatEntity seat : this.seatsAt(serverLevel, top)) {
            if (seat.isVehicle()) {
                return InteractionResult.PASS;
            }
            player.startRiding(seat);
            return InteractionResult.CONSUME;
        }

        LargeFireworksRocketSeatEntity seat = new LargeFireworksRocketSeatEntity(
            serverLevel, top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
        serverLevel.addFreshEntity(seat);
        player.startRiding(seat);
        return InteractionResult.CONSUME;
    }

    private List<LargeFireworksRocketSeatEntity> seatsAt(ServerLevel level, BlockPos top) {
        return level.getEntitiesOfClass(LargeFireworksRocketSeatEntity.class, new AABB(top).inflate(2.0));
    }

    @Nullable
    private Entity detachRider(ServerLevel level, BlockPos mainPos) {
        Entity rider = null;
        for (LargeFireworksRocketSeatEntity seat
            : this.seatsAt(level, mainPos.above(LargeFireworksRocketPart.HEIGHT))) {
            if (rider == null && !seat.getPassengers().isEmpty()) {
                rider = seat.getPassengers().getFirst();
            }
            seat.discard();
        }
        if (rider != null) {
            rider.stopRiding();
        }
        return rider;
    }

    private InteractionResult ignite(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        ItemStack held
    ) {
        if (level.isClientSide()) {

            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        BlockPos mainPos = this.getMainPartPos(pos, state);

        List<FireworkExplosion> explosions = List.of();
        List<UUID> pearlOwners = List.of();
        if (level.getBlockEntity(mainPos) instanceof LargeFireworksRocketBlockEntity fireworks) {
            explosions = fireworks.getExplosions();
            pearlOwners = fireworks.getPearlOwners();
        }

        Entity rider = this.detachRider(serverLevel, mainPos);
        this.removePartsAndUpdate(level, pos);
        LargeFireworksRocketEntity rocket = LargeFireworksRocketEntity.launch(
            serverLevel, mainPos, state.getValue(FACING), player, explosions, pearlOwners);
        if (rider != null) {

            rider.startRiding(rocket);
        }

        if (!player.getAbilities().instabuild) {
            if (held.is(Items.FLINT_AND_STEEL)) {
                held.hurtAndBreak(1, player, hand);
            } else {
                held.consume(1, player);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(held.getItem()));
        return InteractionResult.CONSUME;
    }

    private InteractionResult installPearl(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        Player player,
        ItemStack stack
    ) {
        BlockPos mainPos = this.getMainPartPos(pos, state);
        if (!(level.getBlockEntity(mainPos) instanceof LargeFireworksRocketBlockEntity fireworks)) {
            return InteractionResult.PASS;
        }
        if (fireworks.isPearlFull()) {
            player.sendOverlayMessage(
                Component.translatable(
                    "message.anvilcraft_pigeon_plus.large_fireworks_rocket.pearl_full",
                    LargeFireworksRocketBlockEntity.MAX_PEARLS
                ));
            return InteractionResult.FAIL;
        }
        if (!fireworks.addPearl(player.getUUID())) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(
            null, mainPos, SoundEvents.ENDER_PEARL_THROW, SoundSource.BLOCKS,
            1.0F, 1.0F
        );
        player.sendOverlayMessage(
            Component.translatable(
                "message.anvilcraft_pigeon_plus.large_fireworks_rocket.pearl_installed",
                fireworks.getPearlCount(),
                LargeFireworksRocketBlockEntity.MAX_PEARLS
            ));
        return InteractionResult.CONSUME;
    }

    private InteractionResult installStar(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        Player player,
        ItemStack stack
    ) {
        BlockPos mainPos = this.getMainPartPos(pos, state);
        if (!(level.getBlockEntity(mainPos) instanceof LargeFireworksRocketBlockEntity fireworks)) {
            return InteractionResult.PASS;
        }
        if (fireworks.isFull()) {
            player.sendOverlayMessage(
                Component.translatable(
                    "message.anvilcraft_pigeon_plus.large_fireworks_rocket.full",
                    LargeFireworksRocketBlockEntity.MAX_STARS
                ));
            return InteractionResult.FAIL;
        }
        FireworkExplosion explosion = stack.get(DataComponents.FIREWORK_EXPLOSION);
        if (explosion == null) {
            explosion = FireworkExplosion.DEFAULT;
        }
        if (!fireworks.addExplosion(explosion)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(
            null, mainPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS,
            1.0F, 1.0F
        );
        player.sendOverlayMessage(
            Component.translatable(
                "message.anvilcraft_pigeon_plus.large_fireworks_rocket.installed",
                fireworks.getStarCount(),
                LargeFireworksRocketBlockEntity.MAX_STARS
            ));
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return this.isMainPart(state) ? new LargeFireworksRocketBlockEntity(pos, state) : null;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
            instanceof LargeFireworksRocketBlockEntity fireworks) {
            for (FireworkExplosion explosion : fireworks.getExplosions()) {
                ItemStack star = new ItemStack(Items.FIREWORK_STAR);
                star.set(DataComponents.FIREWORK_EXPLOSION, explosion);
                drops.add(star);
            }
            for (int i = 0; i < fireworks.getPearlCount(); i++) {
                drops.add(new ItemStack(Items.ENDER_PEARL));
            }
        }
        return drops;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return !Block.isShapeFullBlock(this.getPartShape(state)) && state.getFluidState().isEmpty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(PART, state.getValue(PART).rotate(rotation))
            .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(PART, state.getValue(PART).mirror(mirror))
            .setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    private static Map<Direction, Map<LargeFireworksRocketPart, VoxelShape>> createShapesByFacing() {
        Map<Direction, Map<LargeFireworksRocketPart, VoxelShape>> byFacing = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Map<LargeFireworksRocketPart, VoxelShape> shapes = new EnumMap<>(LargeFireworksRocketPart.class);
            for (LargeFireworksRocketPart part : LargeFireworksRocketPart.values()) {
                shapes.put(part, slicePartShape(part, facing));
            }
            byFacing.put(facing, shapes);
        }
        return byFacing;
    }

    private static VoxelShape slicePartShape(LargeFireworksRocketPart part, Direction facing) {
        double minX = part.getOffsetX() * 16.0;
        double minY = part.getOffsetY() * 16.0;
        double minZ = part.getOffsetZ() * 16.0;
        double maxX = minX + 16.0;
        double maxY = minY + 16.0;
        double maxZ = minZ + 16.0;
        List<AABB> localParts = new ArrayList<>();
        for (AABB source : GLOBAL_SHAPE) {
            AABB globalPart = rotateShape(source, facing);
            double clippedMinX = Math.max(globalPart.minX, minX);
            double clippedMinY = Math.max(globalPart.minY, minY);
            double clippedMinZ = Math.max(globalPart.minZ, minZ);
            double clippedMaxX = Math.min(globalPart.maxX, maxX);
            double clippedMaxY = Math.min(globalPart.maxY, maxY);
            double clippedMaxZ = Math.min(globalPart.maxZ, maxZ);
            if (clippedMinX >= clippedMaxX || clippedMinY >= clippedMaxY || clippedMinZ >= clippedMaxZ) {
                continue;
            }
            localParts.add(new AABB(
                clippedMinX - minX, clippedMinY - minY, clippedMinZ - minZ,
                clippedMaxX - minX, clippedMaxY - minY, clippedMaxZ - minZ
            ));
        }
        return localParts.isEmpty() ? Shapes.empty() : ShapeUtil.merge(localParts.toArray(AABB[]::new));
    }

    private static AABB rotateShape(AABB box, Direction facing) {
        return switch (facing) {
            case EAST -> new AABB(16.0 - box.maxZ, box.minY, box.minX,
                                  16.0 - box.minZ, box.maxY, box.maxX);
            case SOUTH -> new AABB(16.0 - box.maxX, box.minY, 16.0 - box.maxZ,
                                   16.0 - box.minX, box.maxY, 16.0 - box.minZ);
            case WEST -> new AABB(box.minZ, box.minY, 16.0 - box.maxX,
                                  box.maxZ, box.maxY, 16.0 - box.minX);
            default -> box;
        };
    }
}
