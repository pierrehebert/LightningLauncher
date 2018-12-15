package net.pierrox.lightning_launcher.script.api.screen;

import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.script.api.Desktop;
import net.pierrox.lightning_launcher.script.api.Lightning;
import net.pierrox.lightning_launcher.views.ItemLayout;

/**
 * A specialized ActivityScreen used for the main home screen.
 */
public class HomeScreen extends ActivityScreen {
    /**
     * @hide
     */
    public HomeScreen(Lightning lightning, net.pierrox.lightning_launcher.engine.Screen screen) {
        super(lightning, screen);
    }

    /**
     * Returns a desktop by its name, as set in the "Configure desktop" screen. The desktop name can be retrieved using {@link Desktop#getName()}.
     * This method will return undefined if the no desktop by that name can be found
     *
     * @param name name of the desktop
     */
    public Desktop getDesktopByName(String name) {
        GlobalConfig gc = mLightning.getEngine().getGlobalConfig();
        for(int n=gc.screensNames.length-1; n>=0; n--) {
            if(name.equals(gc.screensNames[n])) {
                int desktopId = gc.screensOrder[n];
                mScreen.loadRootItemLayout(desktopId, false, false, true);
                ItemLayout itemLayout = mScreen.getItemLayoutsForPage(desktopId)[0];
                return (Desktop) mLightning.getCachedContainer(itemLayout);
            }
        }
        return null;
    }

    /**
     * Go to a specified desktop, without changing the current position in this desktop.
     * @param id desktop identifier
     */
    public void goToDesktop(int id) {
        mScreen.loadRootItemLayout(id, false, true, true);
    }


    /**
     * Go to a specified desktop and set the current absolute position in this desktop, setting a scale of 1 and using animations. This method does nothing when the script is run in background.
     * @param id desktop identifier
     * @param x absolute X position, in pixel
     * @param y absolute Y position, in pixel
     */
    public void goToDesktopPosition(int id, float x, float y) {
        goToDesktopPosition(id, x, y, 1, true);
    }

    /**
     * Go to a specified desktop and set the current absolute position in this desktop. This method does nothing when the script is run in background.
     * @param id desktop identifier
     * @param x absolute X position, in pixel
     * @param y absolute Y position, in pixel
     * @param scale zoom factor (1=100%, 0.5=50%, negative values are acceptable, 0 is not very useful)
     * @param animate whether to animate the move
     */
    public void goToDesktopPosition(int id, float x, float y, float scale, boolean animate) {
        mScreen.goToDesktopPosition(id, -x * scale, -y * scale, scale, animate);
    }

}
