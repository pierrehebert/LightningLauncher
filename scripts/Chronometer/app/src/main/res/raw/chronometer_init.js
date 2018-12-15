/*
Pierre Hébert - Clock - 10/2016
Compatible with Lightning Launcher v14b3+.
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

var screen = getActiveScreen();
var context = screen.getContext();
var version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode%1000;
if(version < 305) {
    alert("You need Lightning Launcher V14b6 or later to use this script");
    return;
}

var MY_PKG = "net.pierrox.lightning_launcher.llscript.chronometer";

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
var script_tap = installScript("chronometer_tap", "Chronometer / tap handler");
var script_menu = installScript("chronometer_menu", "Chronometer / menu handler");
var script_pause = installScript("chronometer_paused", "Chronometer / paused handler");
var script_resume = installScript("chronometer_resumed", "Chronometer / resumed handler");

var d = screen.getCurrentDesktop();

// create a new shortcut
var chronometer = d.addShortcut("00:00:00", new Intent(), 0, 0);
chronometer.setName("Chronometer")

// customize the item
var ed = chronometer.getProperties().edit();
ed.setBoolean("i.onGrid", false);
ed.setBoolean("s.iconVisibility", false);
ed.setBoolean("s.labelVisibility", true);
ed.setFloat("s.labelFontSize", 30);
ed.setString("s.labelFontStyle", "BOLD")
ed.getBox("i.box").setAlignment("CENTER", "MIDDLE");
ed.commit();

var size = d.getWidth()/5;

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
    x -= size;
    y -= size;
}

chronometer.setPosition(x, y);
chronometer.setSize(size*2, size);

// save some default data
var data = {
    showMilliseconds: true,
    isRunning: false
}
chronometer.setTag("data", JSON.stringify(data));



// setup handlers after the init is fully done: this is especially important for the resumed event:
// it needs to be configured once everything is ready, otherwise it could be fired too soon
var ed = chronometer.getProperties().edit();
ed.setEventHandler("i.tap", EventHandler.RUN_SCRIPT, script_tap.getId());
ed.setEventHandler("i.longTap", EventHandler.ITEM_MENU, null);
ed.setEventHandler("i.resumed", EventHandler.RUN_SCRIPT, script_resume.getId());
ed.setEventHandler("i.paused", EventHandler.RUN_SCRIPT, script_pause.getId());
ed.setEventHandler("i.menu", EventHandler.RUN_SCRIPT, script_menu.getId());
ed.commit();

// enter edit mode and select the newly created item
//screen.runAction(EventHandler.EDIT_LAYOUT, clock, null);