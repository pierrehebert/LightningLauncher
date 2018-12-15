/*
Pierre Hébert - Sketch - 2014
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

var MY_PKG = "net.pierrox.lightning_launcher.llscript.sketch";

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

var script_touch = installScript("sketch_touch", "Sketch / touch handler");
var script_btn = installScript("sketch_btn", "Sketch / button handler");
var script_library = installScript("sketch_library", "Sketch / library");

var d = screen.getCurrentDesktop();
var density = context.getResources().getDisplayMetrics().density;

// panel grid columns and rows, changing these values affect the layout of the buttons and the drawing itself
var panel_grid_c = 7;
var panel_grid_r = 6;

// create a panel, which will contain the drawing and some buttons, configure its internal size to 4x4, and set a black border around
var panel = d.addPanel(0, 0, 0, 0);
d.setItemZIndex(panel.getId(), d.getAllItems().length);
var ed = panel.getProperties().edit();
ed.setBoolean("i.onGrid", false);
var box = ed.getBox("i.box");
box.setSize("bl,br,bt,bb", 0);
box.setSize("pl,pr,pt,pb", 20*density);
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

panel.setPosition(x, y);
panel.setSize(width, height);

panel.setBoxBackground(Image.createImage(context.getPackageName(), "android:drawable/alert_light_frame"), "ns", true);
var panel_container = panel.getContainer();
var ed = panel_container.getProperties().edit();
ed.setInteger("gridPColumnNum", panel_grid_c);
ed.setInteger("gridPRowNum", panel_grid_r);
ed.setEventHandler("load", EventHandler.RUN_SCRIPT, script_library.getId());
ed.commit();

// add and configure the drawing item (no icon, label on top, event handler...)
var drawing = panel_container.addShortcut("drawing", new Intent(), 0, 0);
drawing.setCell(0, 0, panel_grid_c, panel_grid_r-1, true);
var ed = drawing.getProperties().edit();
ed.setBoolean("s.iconVisibility", false);
ed.setBoolean("s.labelVisibility", false);
var box = ed.getBox("i.box");
box.setAlignment("CENTER", "TOP");
box.setColor("bb", "ns", 0xff000000);
box.setSize("bb", 1);
ed.setEventHandler("i.touch", EventHandler.RUN_SCRIPT, script_touch.getId());
ed.commit();

// set a blank white image as the sketchpad background
var bg = LL.createImage(512, 512);
bg.draw().drawARGB(255, 255, 255, 255);
drawing.setBoxBackground(bg, "n", true);

// create some buttons (colors, clear)
function createActionButton(icon, name, x) {
    var btn = panel_container.addShortcut(icon, new Intent(), 0, 0);
    btn.setName(name)
    btn.setCell(x, panel_grid_r-1, x+1, panel_grid_r, true);
    var ed = btn.getProperties().edit();
    ed.setFloat("s.labelFontSize", 17*density);
    ed.setInteger("s.labelFontColor", 0xff000000);
    ed.setString("s.labelFontTypeFace", "i");
    ed.setBoolean("s.iconVisibility", false);
    ed.setBoolean("s.labelShadow", false);
    ed.setEventHandler("i.tap", EventHandler.RUN_SCRIPT, script_btn.getId());
    ed.commit();

    return btn;
}

createActionButton("4", "clear", panel_grid_c-1);
createActionButton("[", "settings", panel_grid_c-2);

var defaultColor = 0xff000000;

var colors = [ 0xff000000, 0xffffff00, 0xffff0000, 0xff0000ff, 0xffffffff];
for(var i=0; i<colors.length; i++) {
    var color = colors[i];
    var btn = panel_container.addShortcut("color", new Intent(), 0, 0);
    btn.setName("color_"+color);
    btn.setCell(i, panel_grid_r-1, i+1, panel_grid_r, true);
    ed = btn.getProperties().edit();
    ed.setBoolean("s.labelVisibility", false).setBoolean("s.iconVisibility", false);
    ed.setEventHandler("i.tap", EventHandler.RUN_SCRIPT, script_btn.getId())
    box = ed.getBox("i.box");
    box.setColor("c", "ns", color);
    box.setColor("bl,bt,br,bb", "ns", 0xff404040 );
    box.setSize("bl,bt,br,bb", color==defaultColor ? 6 : 1);
    box.setSize("ml,mr,mt,mb", 8);
    ed.commit();
}

// save some default settings
var defaultSettings = {
    penSize: 5,
    currentColor: defaultColor,
};
drawing.setTag("settings", JSON.stringify(defaultSettings));

// initiate the script library which contains common functions
script_library.run(screen, null);