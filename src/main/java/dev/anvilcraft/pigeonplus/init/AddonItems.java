package dev.anvilcraft.pigeonplus.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.util.registrater.ModelProviderUtil;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import static dev.anvilcraft.pigeonplus.AnvilCraftPigeonPlus.REGISTRUM;

public class AddonItems {
    static {
        REGISTRUM.defaultCreativeTab(AddonItemGroups.ADDON_ITEMS.getKey());
    }

    public static final ItemEntry<BucketItem> GASEOUS_BIOGAS_BUCKET = REGISTRUM.item(
            "gaseous_biogas_bucket",
            properties -> new BucketItem(AddonFluids.GASEOUS_BIOGAS.get(), properties)
        )
        .tag(Tags.Items.BUCKETS)
        .lang("Gaseous Biogas Bucket")
        .properties(properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET))
        .model(ModelProviderUtil::bucket)
        .register();

    public static final ItemEntry<BucketItem> COMPRESSED_AIR_BUCKET = REGISTRUM.item(
            "compressed_air_bucket",
            properties -> new BucketItem(AddonFluids.COMPRESSED_AIR.get(), properties)
        )
        .tag(Tags.Items.BUCKETS)
        .lang("Compressed Air Bucket")
        .properties(properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET))
        .model(ModelProviderUtil::bucket)
        .register();

    public static final ItemEntry<BucketItem> MIXED_BIOMASS_BUCKET = REGISTRUM.item(
            "mixed_biomass_bucket",
            properties -> new BucketItem(AddonFluids.MIXED_BIOMASS.get(), properties)
        )
        .tag(Tags.Items.BUCKETS)
        .lang("Mixed Biomass Bucket")
        .properties(properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET))
        .model(ModelProviderUtil::bucket)
        .register();

    public static void register() {
    }
}
