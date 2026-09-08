package dev.itemnamecopy.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
//? if <1.20.6 {
/*import net.neoforged.fml.loading.FMLEnvironment;
 *///?}

//? if >=1.20.6 {
@Mod(value = "itemnamecopy", dist = Dist.CLIENT)
//?} else {
/*@Mod("itemnamecopy")
 *///?}
public final class NeoForgeClient {
    public NeoForgeClient() {
        //? if <1.20.6 {
        /*if (FMLEnvironment.dist != Dist.CLIENT) return;
         *///?}
        ClientEvents.register();
    }

    private static final class ClientEvents {
        private static void register() {
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ClientEvents::beforeKey);
        }

        private static void beforeKey(ScreenEvent.KeyPressed.Pre event) {
            boolean copied = ItemNameCopyClient.tryCopy(
                    event.getScreen(),
                    event.getKeyCode(),
                    event.getModifiers(),
                    ItemNameCopyClient.currentAction(),
                    event.isCanceled()
            );
            if (copied) {
                event.setCanceled(true);
            }
        }
    }
}
