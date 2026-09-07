package dev.itemnamecopy.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ScreenEvent;

@Mod("itemnamecopy")
public final class ForgeClient {
    @Mod.EventBusSubscriber(modid = "itemnamecopy", value = Dist.CLIENT)
    public static final class ClientEvents {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void beforeKey(ScreenEvent.KeyPressed.Pre event) {
            if (ItemNameCopyClient.tryCopy(event.getScreen(), event.getKeyCode(), event.getModifiers(),
                    ItemNameCopyClient.currentAction(), event.isCanceled())) {
                event.setCanceled(true);
            }
        }
    }
}
