package dev.anvilcraft.pigeonplus.client.renderer.entity;

import dev.anvilcraft.pigeonplus.block.state.LargeFireworksRocketPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public class LargeFireworksRocketRenderState extends EntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean detonating;
    public int pearls;
    public final ItemStackRenderState pearlStack = new ItemStackRenderState();
    public @Nullable LargeFireworksRocketPart[] segments;
}
