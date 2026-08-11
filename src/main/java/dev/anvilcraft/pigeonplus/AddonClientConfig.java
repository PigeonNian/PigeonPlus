package dev.anvilcraft.pigeonplus;

import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import net.neoforged.fml.config.ModConfig;

@Config(name = AnvilCraftPigeonPlus.MOD_ID, type = ModConfig.Type.CLIENT)
public class AddonClientConfig {

    @Comment("Multiplier for the nozzle screen shake amplitude (0.0 disables shaking)")
    @BoundedDiscrete(min = 0.0, max = 5.0)
    public float nozzleShakeAmplitude = 1.0F;

    @Comment("Radius in blocks around a firing nozzle within which the screen shake is felt")
    @BoundedDiscrete(min = 1.0, max = 64.0)
    public float nozzleShakeRange = 12.0F;
}