package net.pierrox.lightning_launcher.script.api;

import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.views.ItemLayout;

/**
 * The desktop is a kind of container, and is typically displayed as the home screen. 
 * It includes all capabilities of the base container, plus some other ones specific to desktops.
 *
 * An instance of this object can be retrieved with any function that returns a {@link Container} when that returned container is a Desktop; or with {@link net.pierrox.lightning_launcher.script.api.screen.HomeScreen#getDesktopByName(String)} or {@link net.pierrox.lightning_launcher.script.api.screen.Screen#getCurrentDesktop()}.
 */
public class Desktop extends Container {

	/**
	 * @hide
	 */
	Desktop(Lightning lightning, ItemLayout itemLayout) {
		super(lightning, itemLayout);
	}
	
	/**
	 * Returns the name of this desktop as configured in the 'Configure desktops' screen.
	 */
	public String getName() {
		GlobalConfig gc = mLightning.getEngine().getGlobalConfig();
		return gc.screensNames[gc.getPageIndex(getPage().id)];
	}
}
