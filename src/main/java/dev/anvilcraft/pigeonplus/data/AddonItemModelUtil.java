package dev.anvilcraft.pigeonplus.data;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumItemModelGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class AddonItemModelUtil {
    public static void oversizedItem(RegistrumItemModelGenerator generator, Item item, Identifier model) {
        generator.itemModelOutput.accept(
            item, ItemModelUtils.plainModel(model), new ClientItem.Properties(true, true, 1.0F));
    }

    private AddonItemModelUtil() {
    }
}
