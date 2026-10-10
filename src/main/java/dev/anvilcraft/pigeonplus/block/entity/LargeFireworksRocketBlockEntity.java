package dev.anvilcraft.pigeonplus.block.entity;

import com.mojang.serialization.Codec;
import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LargeFireworksRocketBlockEntity extends BlockEntity {

    public static final int MAX_STARS = 16;

    public static final int MAX_PEARLS = 4;

    private static final Codec<List<FireworkExplosion>> EXPLOSIONS_CODEC =
        FireworkExplosion.CODEC.listOf();
    private static final String TAG_EXPLOSIONS = "Explosions";
    private static final String TAG_PEARL_OWNERS = "PearlOwners";
    private final List<FireworkExplosion> explosions = new ArrayList<>();

    private final List<UUID> pearlOwners = new ArrayList<>();

    public LargeFireworksRocketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_FIREWORKS_ROCKET.get(), pos, state);
    }

    public List<FireworkExplosion> getExplosions() {
        return List.copyOf(this.explosions);
    }

    public int getStarCount() {
        return this.explosions.size();
    }

    public boolean isFull() {
        return this.explosions.size() >= MAX_STARS;
    }

    public boolean addExplosion(FireworkExplosion explosion) {
        if (this.isFull()) {
            return false;
        }
        this.explosions.add(explosion);
        this.setChanged();
        return true;
    }

    public int getPearlCount() {
        return this.pearlOwners.size();
    }

    public List<UUID> getPearlOwners() {
        return List.copyOf(this.pearlOwners);
    }

    public boolean isPearlFull() {
        return this.pearlOwners.size() >= MAX_PEARLS;
    }

    public boolean addPearl(UUID owner) {
        if (this.isPearlFull()) {
            return false;
        }
        this.pearlOwners.add(owner);
        this.setChanged();
        this.syncPearls();
        return true;
    }

    private void syncPearls() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.level.sendBlockUpdated(
            this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void writePearls(ValueOutput output) {
        if (this.pearlOwners.isEmpty()) {
            return;
        }
        ValueOutput.ValueOutputList owners = output.childrenList(TAG_PEARL_OWNERS);
        for (UUID owner : this.pearlOwners) {
            owners.addChild().putString("Id", owner.toString());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.writePearls(output);
        if (this.explosions.isEmpty()) {
            return;
        }
        output.store(TAG_EXPLOSIONS, EXPLOSIONS_CODEC, this.explosions);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.pearlOwners.clear();
        for (ValueInput owner : input.childrenListOrEmpty(TAG_PEARL_OWNERS)) {
            owner.getString("Id").ifPresent(raw -> {
                try {
                    if (!this.isPearlFull()) {
                        this.pearlOwners.add(UUID.fromString(raw));
                    }
                } catch (IllegalArgumentException error) {
                    AnvilCraftPigeonPlus.LOGGER.warn(
                        "Large firework: unparsable pearl owner UUID, skipped: {}", raw);
                }
            });
        }
        this.explosions.clear();
        input.read(TAG_EXPLOSIONS, EXPLOSIONS_CODEC).ifPresent(loaded -> {
            this.explosions.clear();
            this.explosions.addAll(loaded.subList(0, Math.min(loaded.size(), MAX_STARS)));
        });
    }
}
