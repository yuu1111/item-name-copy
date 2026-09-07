package dev.itemnamecopy.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
//? if >=1.21.6 {
/*import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
*///?} else {
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?}
import net.minecraftforge.fml.common.Mod;
//? if <1.18.2 {
/*import net.minecraftforge.fml.ModLoadingContext;
*///?}
//? if >=1.17 && <1.18.2 {
/*import net.minecraftforge.fml.IExtensionPoint;
*///?}
//? if <1.17 {
/*import net.minecraftforge.fml.ExtensionPoint;
import org.apache.commons.lang3.tuple.Pair;
*///?}
//? if >=1.18 {
import net.minecraftforge.client.event.ScreenEvent;
//?} else {
/*import net.minecraftforge.client.event.GuiScreenEvent;
*///?}

@Mod("itemnamecopy")
public final class ForgeClient {
    //? if <1.18.2 {
    /*public ForgeClient() {
        //? if >=1.17 {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> "itemnamecopy", (remoteVersion, network) -> true));
        //?} else {
        /^ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> "itemnamecopy", (remoteVersion, network) -> true));
        ^///?}
    }
    *///?}
    @Mod.EventBusSubscriber(modid = "itemnamecopy", value = Dist.CLIENT)
    public static final class ClientEvents {
        //? if >=1.21.6 {
        /*@SubscribeEvent(priority = Priority.LOWEST)
        public static boolean beforeKey(ScreenEvent.KeyPressed.Pre event) {
            return ItemNameCopyClient.tryCopy(event.getScreen(), event.getKeyCode(), event.getModifiers(),
                    ItemNameCopyClient.currentAction(), false);
        }
        *///?} else {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        //? if >=1.19 {
        public static void beforeKey(ScreenEvent.KeyPressed.Pre event) {
        //?} elif >=1.18 {
        /*public static void beforeKey(ScreenEvent.KeyboardKeyPressedEvent.Pre event) {
        *///?} else {
        /*public static void beforeKey(GuiScreenEvent.KeyboardKeyPressedEvent.Pre event) {
        *///?}
            //? if >=1.18 {
            Screen screen = event.getScreen();
            //?} else {
            /*Screen screen = event.getGui();
            *///?}
            if (ItemNameCopyClient.tryCopy(screen, event.getKeyCode(), event.getModifiers(),
                    ItemNameCopyClient.currentAction(), event.isCanceled())) {
                event.setCanceled(true);
            }
        }
        //?}
    }
}
