package dev.anvilcraft.pigeonplus;

import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;

@Config(name = AnvilCraftPigeonPlus.MOD_ID)
public class AddonConfig {
    @Comment("Whether to log the dirt block on common setup")
    public boolean logDirtBlock = false;

    @Comment("A magic number")
    @BoundedDiscrete(max = 24, min = 2)
    public int magicNumber = 2;

    @Comment("What you want the introduction message to be for the magic number")
    public String magicNumberIntroduction = "";

    @Comment("Height at which a rocket carrying leashed mobs scatters them")
    @BoundedDiscrete(min = 1.0, max = 30000.0)
    public int leashedRocketScatterHeight = 1000;

    @Comment("Horizontal radius in blocks that leashed mobs are scattered over")
    @BoundedDiscrete(min = 1.0, max = 30000.0)
    public int leashedRocketScatterRadius = 500;
}
