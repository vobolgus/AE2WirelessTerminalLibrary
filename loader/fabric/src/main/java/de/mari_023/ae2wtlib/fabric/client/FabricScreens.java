package de.mari_023.ae2wtlib.fabric.client;

import net.minecraft.client.gui.screens.MenuScreens;

import appeng.client.InitScreens;

import de.mari_023.ae2wtlib.wat.WATMenu;
import de.mari_023.ae2wtlib.wat.WATScreen;
import de.mari_023.ae2wtlib.wct.TrashMenu;
import de.mari_023.ae2wtlib.wct.TrashScreen;
import de.mari_023.ae2wtlib.wct.WCTMenu;
import de.mari_023.ae2wtlib.wct.WCTScreen;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetMenu;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetScreen;
import de.mari_023.ae2wtlib.wet.WETMenu;
import de.mari_023.ae2wtlib.wet.WETScreen;

/**
 * Fabric twin of {@code de.mari_023.ae2wtlib.neoforge.NeoForgeScreens}.
 * <p>
 * Our AE2 fork replaced {@code InitScreens.register}'s first parameter with the loader-neutral
 * {@code InitScreens.MenuScreenRegistrar} (PORTING_NOTES §3.2); on NeoForge it is fed
 * {@code RegisterMenuScreensEvent::register}, on Fabric vanilla's own {@code MenuScreens::register} - the same call
 * AE2's {@code AppEngFabricClient} makes for its own menus. The list below is identical to
 * {@code NeoForgeScreens#registerScreens}, including the style paths.
 * <p>
 * {@code MenuScreens#register} is {@code private static} in vanilla; fabric-api widens it transitively, but this
 * project declares the entry in its own accesswidener as well rather than depend on another mod's (PORTING_NOTES §8.1,
 * same reasoning as {@code Player#closeContainer}).
 */
public final class FabricScreens {
    private FabricScreens() {}

    public static void registerScreens() {
        InitScreens.MenuScreenRegistrar registrar = MenuScreens::register;

        InitScreens.register(registrar, WCTMenu.TYPE, WCTScreen::new,
                "/screens/wtlib/wireless_crafting_terminal.json");
        InitScreens.register(registrar, WETMenu.TYPE, WETScreen::new,
                "/screens/wtlib/wireless_pattern_encoding_terminal.json");
        InitScreens.register(registrar, WATMenu.TYPE, WATScreen::new,
                "/screens/wtlib/wireless_pattern_access_terminal.json");
        InitScreens.register(registrar, MagnetMenu.TYPE, MagnetScreen::new, "/screens/wtlib/magnet.json");
        InitScreens.register(registrar, TrashMenu.TYPE, TrashScreen::new, "/screens/wtlib/trash.json");
    }
}
