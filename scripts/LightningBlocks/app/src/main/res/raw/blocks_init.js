/*
Pierre Hébert - Sketch - 2014
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/
var MY_PKG = "net.pierrox.lightning_launcher.llscript.blocks";


// install (or update) a script given its id in the package, and its clear name in the launcher data
function installScript(id, name) {
    // load the script (if any) among the existing ones
    var script = LL.getScriptByName(name);

    // load the touch handler from the package
    var script_text = LL.loadRawResource(MY_PKG, id);

    if(script == null) {
        // script not found: install it
        script = LL.createScript(name, script_text , 0);
    } else {
        // the script already exists: update its text
        script.setText(script_text);
    }

    return script;
}

var script_touch = installScript("sketch_touch", "Sketch / touch handler");
var script_btn = installScript("blocks_btn", "Blocks / button handler");


var d = LL.getCurrentDesktop();
var w = d.getWidth() / 2;
var h = d.getHeight() * 2 / 3;

// panel grid columns and rows
var panel_grid_c = 5;
var panel_grid_r = 8;

// create a panel, which will contain the drawing and some buttons, configure its internal size to 4x4, and set a black border around
var panel = d.addPanel(0, 0, w, h);
var ed = panel.getProperties().edit();
var box = ed.getBox("i.box");
box.setColor("bl,br,bt,bb", "ns", 0xff000000);
box.setSize("bl,br,bt,bb", 4);
box.setSize("ml,mr,mt,mb", 4);
box.setColor("c", "nsf", 0xffffffff);
ed.commit();
var panel_container = panel.getContainer();
panel_container.getProperties().edit().setInteger("gridPColumnNum", panel_grid_c).setInteger("gridPRowNum", panel_grid_r).commit();

// add and configure the game table item (no icon, label on top, event handler...)
var table = panel_container.addShortcut("game", new Intent(), 0, 0);
table.setCell(0, 1, panel_grid_c-1, panel_grid_r-1, true);
var ed = table.getProperties().edit();
ed.setBoolean("s.iconVisibility", false);
ed.setBoolean("s.labelVisibility", false);
ed.getBox("i.box").setAlignment("CENTER", "TOP");
ed.commit();

// set a blank image as the game table background
var bg = LL.createImage(512, 512);
bg.draw().drawARGB(255, 200, 200, 255);
table.setBoxBackground(bg, "n");
table.getBoxBackground("n").save();

// create buttons
var btn_left = panel_container.addShortcut("left", new Intent(), 0, 0);
btn_left.setCell(panel_grid_c-2, panel_grid_r-1, panel_grid_c-1, panel_grid_r, true);
btn_left.getProperties().edit().setBoolean("s.labelVisibility", false).setBoolean("s.iconReflection", false).setEventHandler("i.tap", EventHandler.RUN_SCRIPT, script_btn.getId()).commit();
btn_left.setDefaultIcon(LL.createImage(MY_PKG, "ic_chevron_left_black_24dp"));

var btn_right = panel_container.addShortcut("right", new Intent(), 0, 0);
btn_right.setCell(panel_grid_c-1, panel_grid_r-1, panel_grid_c, panel_grid_r, true);
btn_right.getProperties().edit().setBoolean("s.labelVisibility", false).setBoolean("s.iconReflection", false).setEventHandler("i.tap", EventHandler.RUN_SCRIPT, script_btn.getId()).commit();
btn_right.setDefaultIcon(LL.createImage(MY_PKG, "ic_chevron_right_black_24dp"));

var btn_turn_cw = panel_container.addShortcut("turn_cw", new Intent(), 0, 0);
btn_turn_cw.setCell(panel_grid_c-1, panel_grid_r-2, panel_grid_c, panel_grid_r-1, true);
btn_turn_cw.getProperties().edit().setBoolean("s.labelVisibility", false).setBoolean("s.iconReflection", false).setEventHandler("i.tap", EventHandler.RUN_SCRIPT, script_btn.getId()).commit();
btn_turn_cw.setDefaultIcon(LL.createImage(MY_PKG, "ic_rotate_right_black_24dp"));

var btn_turn_ccw = panel_container.addShortcut("turn_ccw", new Intent(), 0, 0);
btn_turn_ccw.setCell(panel_grid_c-1, panel_grid_r-3, panel_grid_c, panel_grid_r-2, true);
btn_turn_ccw.getProperties().edit().setBoolean("s.labelVisibility", false).setBoolean("s.iconReflection", false).setEventHandler("i.tap", EventHandler.RUN_SCRIPT, script_btn.getId()).commit();
btn_turn_ccw.setDefaultIcon(LL.createImage(MY_PKG, "ic_rotate_left_black_24dp"));

var btn_start_stop = panel_container.addShortcut("start_stop", new Intent(), 0, 0);
btn_start_stop.setCell(0, panel_grid_r-1, 1, panel_grid_r, true);
btn_start_stop.getProperties().edit().setBoolean("s.labelVisibility", false).setBoolean("s.iconReflection", false).setEventHandler("i.tap", EventHandler.RUN_SCRIPT, script_btn.getId()).commit();
btn_start_stop.setDefaultIcon(LL.createImage(MY_PKG, "ic_play_arrow_black_24dp"));

var btn_pause = panel_container.addShortcut("pause", new Intent(), 0, 0);
btn_pause.setCell(1, panel_grid_r-1, 2, panel_grid_r, true);
btn_pause.getProperties().edit().setBoolean("s.labelVisibility", false).setBoolean("s.iconReflection", false).setEventHandler("i.tap", EventHandler.RUN_SCRIPT, script_btn.getId()).commit();
btn_pause.setDefaultIcon(LL.createImage(MY_PKG, "ic_pause_black_24dp"));

var label_score = panel_container.addShortcut("Score:", new Intent(), 0, 0);
label_score.setCell(0, 0, 1, 1, true);
label_score.getProperties().edit().setBoolean("s.labelVisibility", true).setBoolean("s.iconVisibility", false).commit();

var value_score = panel_container.addShortcut("-", new Intent(), 0, 0);
value_score.setName("score");
value_score.setCell(1, 0, panel_grid_c, 1, true);
value_score.getProperties().edit().setBoolean("s.labelVisibility", true).setBoolean("s.iconVisibility", false).commit();