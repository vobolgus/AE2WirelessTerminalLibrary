package de.mari_023.ae2wtlib.neoforge;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

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
 * Moved out of {@code AE2wtlib} because upstream AE2's {@code InitScreens.register} takes a
 * {@code RegisterMenuScreensEvent} while our Fabric fork takes the loader-neutral
 * {@code InitScreens.MenuScreenRegistrar} (PORTING_NOTES §3.2). The Fabric twin lands in W5.
 */
public final class NeoForgeScreens {
    private NeoForgeScreens() {}

    public static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, WCTMenu.TYPE, WCTScreen::new, "/screens/wtlib/wireless_crafting_terminal.json");
        InitScreens.register(event, WETMenu.TYPE, WETScreen::new,
                "/screens/wtlib/wireless_pattern_encoding_terminal.json");
        InitScreens.register(event, WATMenu.TYPE, WATScreen::new,
                "/screens/wtlib/wireless_pattern_access_terminal.json");
        InitScreens.register(event, MagnetMenu.TYPE, MagnetScreen::new, "/screens/wtlib/magnet.json");
        InitScreens.register(event, TrashMenu.TYPE, TrashScreen::new, "/screens/wtlib/trash.json");
    }
}
