/*
Pierre Hébert - Clock - 10/2016
Compatible with Lightning Launcher v14b3+.
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

var screen = getActiveScreen();
var context = screen.getContext();
var version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode%1000;
if(version < 300) {
    alert("You need Lightning Launcher V14b3 or later to use this script");
    return;
}

var MY_PKG = "net.pierrox.lightning_launcher.llscript.clock";

// install (or update) a script given its id in the package, and its clear name in the launcher data
function installScript(id, name) {
    // use our package name to classify scripts
    var path = '/'+MY_PKG.replace(/\./g, '/');

    // load the script (if any) among the existing ones
    var script = getScriptByPathAndName(path, name);

    // load the script text from the package
    var script_text = loadRawResource(MY_PKG, id);

    if(script == null) {
        // script not found: install it
        script = createScript(path, name, script_text , 0);
    } else {
        // the script already exists: update its text
        script.setText(script_text);
    }

    return script;
}

// install or update scripts in the launcher, using the script found in the res/raw directory
var script_resumed = installScript("clock_resumed", "Clock / resumed handler");
var script_paused = installScript("clock_paused", "Clock / paused handler");
var script_menu = installScript("clock_menu", "Clock / menu handler");

var d = screen.getCurrentDesktop();

// create a new shortcut
var clock = d.addShortcut("clock", new Intent(), 0, 0);

// customize the item (hide text and icon because we are using the background, set event handlers)
var ed = clock.getProperties().edit();
ed.setBoolean("i.onGrid", false);
ed.setBoolean("s.iconVisibility", false);
ed.setBoolean("s.labelVisibility", false);
ed.getBox("i.box").setAlignment("CENTER", "TOP");
ed.commit();

// create a background image
var bg = Image.createImage(512, 512);
clock.setBoxBackground(bg, "n", true);

// the default clock size, a third of the container width
var size = d.getWidth()/3;

// use the last screen touch position, if any, as location for the new item
bindClass("java.lang.Integer");
var x = screen.getLastTouchX();
var y = screen.getLastTouchY();
if(x == Integer.MIN_VALUE || y == Integer.MIN_VALUE) {
    // no previous touch event, use a default position (can happen when using the hardware menu key for instance)
    x = size;
    y = size;
} else {
    // center around the touch position
    x -= size/2;
    y -= size/2;
}

clock.setPosition(x, y);
clock.setSize(size, size);

// save some default settings, see clock_resume.js and clock_menu.js for use of these settings
var defaultSettings = {
    frameColor: 0xffffffff,
    bgColor: 0x40ffffff,
    showSeconds: true,
    showDigits: true,
    digitsSize: 20,
    digitsColor: 0xffffffff,
};
clock.setTag("settings", JSON.stringify(defaultSettings));

// setup handlers after the init is fully done: this is especially important for the resumed event:
// it needs to be configured once everything is ready, otherwise it could be fired too soon
var ed = clock.getProperties().edit();
ed.setEventHandler("i.resumed", EventHandler.RUN_SCRIPT, script_resumed.getId());
ed.setEventHandler("i.paused", EventHandler.RUN_SCRIPT, script_paused.getId());
ed.setEventHandler("i.tap", EventHandler.NOTHING, null);
ed.setEventHandler("i.menu", EventHandler.RUN_SCRIPT, script_menu.getId());
ed.commit();

// enter edit mode and select the newly created item
screen.runAction(EventHandler.EDIT_LAYOUT, clock, null);