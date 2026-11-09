package com.example.soundping.mixin;

import com.example.soundping.SoundPings;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void soundping$onSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        SoundPings.onSound(packet.getSound(), packet.getX(), packet.getY(), packet.getZ());
    }
}
