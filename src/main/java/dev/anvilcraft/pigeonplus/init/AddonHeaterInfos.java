package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.pigeonplus.block.ElectricBlenderBlock;
import dev.anvilcraft.pigeonplus.block.entity.ModBlockEntities;
import dev.anvilcraft.pigeonplus.block.entity.NozzleExhaustBlockEntity;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.api.heat.HeatTierLine;
import dev.dubhe.anvilcraft.api.heat.HeaterInfo;
import dev.dubhe.anvilcraft.util.BlockInfo;

import java.util.Set;

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

    public static final HeaterInfo<BlockInfo> ELECTRIC_BLENDER = HeatRecorder.registerProducerInfo(
        HeaterInfo.blockState(
            AddonBlocks.ELECTRIC_BLENDER.get(),
            ElectricBlenderBlock.HEATING,
            true,
            heater -> Set.of(heater.pos().above()),
            HeatTierLine.always(HeatTier.HEATED, 2)
        )
    );
    public static void init() {
    }

    private AddonHeaterInfos() {
    }
}
