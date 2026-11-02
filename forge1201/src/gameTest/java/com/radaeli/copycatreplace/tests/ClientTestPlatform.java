package com.radaeli.copycatreplace.tests;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import com.radaeli.copycatreplace.network.ConnectedBlockLimitSync;
import net.minecraftforge.event.TickEvent;
@EventBusSubscriber(modid="copycat_replace_tests",value=Dist.CLIENT)
public final class ClientTestPlatform {
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) { if(e.phase==TickEvent.Phase.END) { ClientValidation.tick(); MultiplayerClientValidation.tick(); } }
    @SubscribeEvent public static void sound(PlaySoundEvent e) { if(e.getSound()!=null) ClientValidation.sound(e.getSound().getLocation().toString()); }
    static void syncLimit(ServerPlayer p) { ConnectedBlockLimitSync.onPlayerLoggedIn(new PlayerEvent.PlayerLoggedInEvent(p)); }

    static void verifyPonderMaterial(net.createmod.ponder.api.level.PonderLevel world, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState expected) {
        var state = world.getBlockState(pos);
        var model = net.minecraft.client.Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        var data = model.getModelData(world, pos, state, net.minecraftforge.client.model.data.ModelData.EMPTY);
        if (!com.simibubi.create.content.decoration.copycat.CopycatModel.getMaterial(data).equals(expected))
            throw new AssertionError("Rendered Ponder model does not match Copycat material at " + pos);
    }

    static void connect() {
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(),
            net.minecraft.client.Minecraft.getInstance(), net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(System.getProperty("copycat_replace.address","127.0.0.1:25585")),
            new net.minecraft.client.multiplayer.ServerData("Copycat validation",System.getProperty("copycat_replace.address","127.0.0.1:25585"),false),false);
    }
    static void disconnect() {
        var mc=net.minecraft.client.Minecraft.getInstance(); mc.level.disconnect(); mc.clearLevel(new net.minecraft.client.gui.screens.TitleScreen());
    }

    static void createWorld(String world) {
        net.minecraft.client.Minecraft.getInstance().createWorldOpenFlows().createFreshLevel(world,
            new net.minecraft.world.level.LevelSettings(world, net.minecraft.world.level.GameType.CREATIVE, false,
                net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
            new net.minecraft.world.level.levelgen.WorldOptions(42L, false, false), registries -> registries.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
    }
}
