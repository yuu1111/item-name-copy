package dev.itemnamecopy.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "itemnamecopy", dist = Dist.CLIENT)
public final class NeoForgeClient {
    public NeoForgeClient() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, NeoForgeClient::beforeKey);
    }

    private static void beforeKey(ScreenEvent.KeyPressed.Pre event) {
        if (ItemNameCopyClient.tryCopy(event.getScreen(), event.getKeyCode(), event.getModifiers(),
                ItemNameCopyClient.currentAction(), event.isCanceled())) {
            event.setCanceled(true);
        }
    }
}
