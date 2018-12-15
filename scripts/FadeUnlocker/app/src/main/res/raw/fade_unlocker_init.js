var screen = getActiveScreen();
var context = screen.getContext();
var version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode%1000;
if(version < 300) {
    alert("You need Lightning Launcher V14b3 or later to use this script");
    return;
}

var MY_PKG = "net.pierrox.lightning_launcher.llscript.fade_unlocker";


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

var script_touch = installScript("fade_unlocker_touch", "Fade Unlocker / touch handler");
var script_menu = installScript("fade_unlocker_menu", "Fade Unlocker / menu handler");

var d = screen.getCurrentDesktop();

var unlocker = d.addShortcut("unlocker", new Intent(), 0, 0);

var ed = unlocker.getProperties().edit();
ed.setBoolean("i.onGrid", false);
ed.setBoolean("s.iconVisibility", false);
ed.setBoolean("s.labelVisibility", false);
ed.getBox("i.box").setAlignment("CENTER", "TOP");
ed.setEventHandler("i.touch", EventHandler.RUN_SCRIPT, script_touch.getId());
ed.setEventHandler("i.menu", EventHandler.RUN_SCRIPT, script_menu.getId());
ed.commit();

// use the last screen touch position, if any, as location for the new item
bindClass("java.lang.Integer");
var width = d.getWidth();
var height = d.getHeight();
if(height > width) {
    width = width*2/3;
    height = width;
} else {
    width = height*2/3;
    height = height;
}

var x = screen.getLastTouchX();
var y = screen.getLastTouchY();
if(x == Integer.MIN_VALUE || y == Integer.MIN_VALUE) {
    // no previous touch event, use a default position (can happen when using the hardware menu key for instance)
    x = (d.getWidth()-width)/2;
    y = (d.getHeight()-height)/2;;
} else {
    // center around the touch position
    x -= width/2;
    y -= height/2;
}
unlocker.setPosition(x, y);
unlocker.setSize(width, height);


bindClass("android.view.Gravity");
var toast = Toast.makeText(context, "Pick an image to fade out while swiping on the unlocker", Toast.LENGTH_LONG);
toast.setGravity(Gravity.CENTER, 0, 0);
toast.show();
var image = screen.pickImage(0);
unlocker.setBoxBackground(image, "n", true);