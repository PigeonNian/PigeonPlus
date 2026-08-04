package dev.anvilcraft.pigeonplus.data.advancement;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.advancement.criterion.NozzleGasActivatedTrigger;
import dev.anvilcraft.pigeonplus.init.AddonBlocks;
import dev.anvilcraft.pigeonplus.init.AddonItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AddonAdvancements {
    public static final AdvancementHolder ROOT;
    public static final AdvancementHolder IGNITE_NOZZLE;

    static {
        ROOT = Advancement.Builder.advancement()
            .display(
                AddonItems.LIQUEFIED_BIOGAS_BUCKET.get(),
                Component.translatable("advancements.anvilcraft_pigeon_plus.root.title"),
                Component.translatable("advancements.anvilcraft_pigeon_plus.root.description"),
                ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/misc/background/advancement.png"),
                AdvancementType.TASK,
                false,
                true,
                false
            )
            .addCriterion("join", PlayerTrigger.TriggerInstance.tick())
            .build(AnvilCraftPigeonPlus.of("root"));
        IGNITE_NOZZLE = Advancement.Builder.advancement()
            .parent(ROOT)
            .display(
                AddonBlocks.NOZZLE.asItem(),
                Component.translatable("advancements.anvilcraft_pigeon_plus.nozzle_ignition.title"),
                Component.translatable("advancements.anvilcraft_pigeon_plus.nozzle_ignition.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("activate_nozzle_gas", NozzleGasActivatedTrigger.TriggerInstance.activate())
            .build(AnvilCraftPigeonPlus.of("nozzle_ignition"));
    }

    private AddonAdvancements() {
    }
}
