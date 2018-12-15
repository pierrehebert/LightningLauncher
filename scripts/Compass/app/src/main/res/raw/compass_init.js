/*
Pierre Hébert - Counter 11/2016
Compatible with Lightning Launcher v14b6+.
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

/*
This is a sample plugin which goal is to show how to define an object that inherits from the
Lightning Launcher native item. It also demonstrates the use of a custom menu and settings screen.
*/

var screen = getActiveScreen();
var context = screen.getContext();
var version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode%1000;
if(version < 305) {
    alert("You need Lightning Launcher V14b6 or later to use this script");
    return;
}

var MY_PKG = "net.pierrox.lightning_launcher.llscript.compass";

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
var script_create = installScript("compass_create", "Compass / create view");
var script_paused = installScript("compass_paused", "Compass / paused handler");
var script_resumed = installScript("compass_resumed", "Compass / resumed handler");

var d = screen.getCurrentDesktop();

// create a new custom view
var compass = d.addCustomView(0, 0);
compass.setName("Compass")

var ed = compass.getProperties().edit();
ed.setBoolean("i.onGrid", false);
ed.commit();

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

compass.setPosition(x, y);
compass.setSize(size, size);



// setup handlers after the init is fully done: this is especially important for the resumed event:
// it needs to be configured once everything is ready, otherwise it could be fired too soon
var ed = compass.getProperties().edit();
ed.setString("v.onCreate", script_create.getId());
ed.setEventHandler("i.resumed", EventHandler.RUN_SCRIPT, script_resumed.getId());
ed.setEventHandler("i.paused", EventHandler.RUN_SCRIPT, script_paused.getId());
//ed.setEventHandler("i.longTap", EventHandler.ITEM_MENU, null);
//ed.setEventHandler("i.menu", EventHandler.RUN_SCRIPT, script_menu.getId());
ed.commit();