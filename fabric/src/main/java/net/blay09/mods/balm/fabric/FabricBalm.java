package net.blay09.mods.balm.fabric;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.config.AbstractBalmConfig;
import net.blay09.mods.balm.api.container.BalmContainerProvider;
import net.blay09.mods.balm.api.entity.BalmEntity;
import net.blay09.mods.balm.api.fluid.FluidTank;
import net.blay09.mods.balm.config.ExampleConfig;
import net.blay09.mods.balm.config.ExampleConfigData;
import net.blay09.mods.balm.fabric.provider.FabricBalmProviders;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;

public class FabricBalm implements ModInitializer {

    @Override
    public void onInitialize() {
        ((FabricBalmHooks) Balm.getHooks()).initialize();
        ((AbstractBalmConfig) Balm.getConfig()).initialize();
        ExampleConfig.initialize();

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            CompoundTag data = ((BalmEntity) oldPlayer).getFabricBalmData();
            ((BalmEntity) newPlayer).setFabricBalmData(data);
        });

        var providers = ((FabricBalmProviders) Balm.getProviders());
        providers.registerProvider(new ResourceLocation("balm", "container"), Container.class);
        providers.registerProvider(new ResourceLocation("balm", "fluid_tank"), FluidTank.class);

        ItemStorage.SIDED.registerFallback((world, pos, state, blockEntity, direction) -> {
            if (blockEntity instanceof BalmContainerProvider containerProvider) {
                return InventoryStorage.of(containerProvider.getContainer(direction), direction);
            }

            return null;
        });
    }
}
