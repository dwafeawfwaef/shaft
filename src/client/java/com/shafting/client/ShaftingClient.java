package com.shafting.client;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class ShaftingClient implements ClientModInitializer {
    public static Config config;

    @Override
    public void onInitializeClient() {
        config = Config.load();
        WorldEspRenderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> CorpseFeature.tick());
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            CorpseFeature.interact(entity);
            return InteractionResult.PASS;
        });

        ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, net.minecraft.commands.CommandBuildContext registryAccess) {
        dispatcher.register(literal("shafting").executes(context -> {
            Minecraft.getInstance().setScreen(new ShaftingScreen());
            return 1;
        }));
    }

    public static void save() { if (config != null) config.save(); }
}
