package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.anvilcraft.pigeonplus.block.entity.NozzleExhaustBlockEntity;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.api.heat.HeatTierLine;
import dev.dubhe.anvilcraft.api.heat.HeaterInfo;

public final class AddonHeaterInfos {
    public static final HeaterInfo<NozzleExhaustBlockEntity> NO_MAGNET_NOZZLE_EXHAUST = HeatRecorder.registerProducerInfo(
        HeaterInfo.blockEntity(
            ModBlockEntities.NOZZLE_EXHAUST,
            rocketExhaust -> rocketExhaust.getHeatingPoses().getFirst(),
            HeatTierLine.always(HeatTier.INCANDESCENT, 2)
        )
    );

    public static final HeaterInfo<NozzleExhaustBlockEntity> MAGNET_NOZZLE_EXHAUST = HeatRecorder.registerProducerInfo(
        HeaterInfo.blockEntity(
            ModBlockEntities.NOZZLE_EXHAUST,
            rocketExhaust -> rocketExhaust.getHeatingPoses().getSecond(),
            HeatTierLine.always(HeatTier.INCANDESCENT, 20)
        )
    );

    private AddonHeaterInfos() {
    }
}
