package net.blay09.mods.balm.fabric.event;


import net.blay09.mods.balm.api.event.*;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;

import java.util.ArrayList;
import java.util.List;

public class FabricBalmCommonEvents {

    private static final List<ServerPlayerTickHandler> playerTickStartHandlers = new ArrayList<>();
    private static final List<ServerPlayerTickHandler> playerTickEndHandlers = new ArrayList<>();
    private static ServerTickEvents.StartTick serverTickStartListener = null;
    private static ServerTickEvents.EndTick serverTickEndListener = null;

    public static void registerEvents(FabricBalmEvents events) {
        events.registerTickEvent(TickType.Server, TickPhase.Start, (ServerTickHandler handler) -> ServerTickEvents.START_SERVER_TICK.register(handler::handle));
        events.registerTickEvent(TickType.Server, TickPhase.End, (ServerTickHandler handler) -> ServerTickEvents.END_SERVER_TICK.register(handler::handle));
        events.registerTickEvent(TickType.ServerLevel,
                TickPhase.Start,
                (ServerLevelTickHandler handler) -> ServerTickEvents.START_WORLD_TICK.register(handler::handle));
        events.registerTickEvent(TickType.ServerLevel,
                TickPhase.End,
                (ServerLevelTickHandler handler) -> ServerTickEvents.END_WORLD_TICK.register(handler::handle));

        events.registerTickEvent(TickType.ServerPlayer, TickPhase.Start, (ServerPlayerTickHandler handler) -> {
            if (serverTickStartListener == null) {
                serverTickStartListener = server -> {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        for (ServerPlayerTickHandler playerTickHandler : playerTickStartHandlers) {
                            playerTickHandler.handle(player);
                        }
                    }
                };

                ServerTickEvents.START_SERVER_TICK.register(serverTickStartListener);
            }

            playerTickStartHandlers.add(handler);
        });

        events.registerTickEvent(TickType.ServerPlayer, TickPhase.End, (ServerPlayerTickHandler handler) -> {
            if (serverTickEndListener == null) {
                serverTickEndListener = server -> {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        for (ServerPlayerTickHandler playerTickHandler : playerTickEndHandlers) {
                            playerTickHandler.handle(player);
                        }
                    }
                };

                ServerTickEvents.END_SERVER_TICK.register(serverTickEndListener);
            }

            playerTickEndHandlers.add(handler);
        });

        events.registerEvent(ServerStartedEvent.class, () -> ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            final ServerStartedEvent event = new ServerStartedEvent(server);
            events.fireEventHandlers(event);
        }));

        events.registerEvent(ServerStoppedEvent.class, () -> ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            final ServerStoppedEvent event = new ServerStoppedEvent(server);
            events.fireEventHandlers(event);
        }));

        events.registerEvent(UseBlockEvent.class, () -> UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            final UseBlockEvent event = new UseBlockEvent(player, world, hand, hitResult);
            events.fireEventHandlers(event);
            if (event.isCanceled()) {
                return InteractionResult.FAIL;
            }

            return event.getInteractionResult();
        }));

        events.registerEvent(UseItemEvent.class, () -> {
            UseItemCallback.EVENT.register((player, world, hand) -> {
                final UseItemEvent event = new UseItemEvent(player, world, hand);
                events.fireEventHandlers(event);
                if (event.isCanceled()) {
                    return InteractionResultHolder.fail(player.getItemInHand(hand));
                }

                return new InteractionResultHolder<>(event.getInteractionResult(), player.getItemInHand(hand));
            });
        });

        events.registerEvent(PlayerLoginEvent.class, () -> ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            final PlayerLoginEvent event = new PlayerLoginEvent(listener.player);
            events.fireEventHandlers(event);
        }));

        events.registerEvent(PlayerLogoutEvent.class, () -> ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            final PlayerLogoutEvent event = new PlayerLogoutEvent(listener.player);
            events.fireEventHandlers(event);
        }));

        events.registerEvent(BreakBlockEvent.class, () -> PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            final BreakBlockEvent event = new BreakBlockEvent(world, player, pos, state, blockEntity);
            events.fireEventHandlers(event);
            return !event.isCanceled();
        }));

        events.registerEvent(PlayerRespawnEvent.class, () -> ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            final PlayerRespawnEvent event = new PlayerRespawnEvent(oldPlayer, newPlayer);
            events.fireEventHandlers(event);
        }));
    }
}
