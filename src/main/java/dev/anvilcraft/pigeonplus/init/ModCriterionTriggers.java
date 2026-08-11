package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus;
import dev.anvilcraft.pigeonplus.advancement.criterion.NozzleExplosionTrigger;
import dev.anvilcraft.pigeonplus.advancement.criterion.NozzleGasActivatedTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCriterionTriggers {
    private static final DeferredRegister<CriterionTrigger<?>> REGISTER =
        DeferredRegister.create(Registries.TRIGGER_TYPE, AnvilCraftPigeonPlus.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, NozzleExplosionTrigger> NOZZLE_EXPLOSION =
        REGISTER.register("nozzle_explosion", NozzleExplosionTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, NozzleGasActivatedTrigger> NOZZLE_GAS_ACTIVATED =
        REGISTER.register("nozzle_gas_activated", NozzleGasActivatedTrigger::new);

    private ModCriterionTriggers() {
    }

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }
}
