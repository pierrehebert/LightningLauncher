/*
Pierre Hébert - Week Day 11/2016
Based on the script of Jappie Toutenhoofd https://plus.google.com/+JappieToutenhoofd/posts/P1aKQjkPMij
Compatible with Lightning Launcher v14b6+.
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

var MY_PKG = "net.pierrox.lightning_launcher.llscript.week_day";

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

var script_menu = installScript("week_day_menu", "Week Day / menu handler");

var d = screen.getCurrentDesktop();

var week_day = d.addShortcut("0", new Intent(), 0, 0);
week_day.setName("Week Day")

var settings = {
    dayNames: "sund,mon,tue,wed,thu,fri,sat",
    hlColor: "#ff0000"
}
week_day.setTag(MY_PKG, JSON.stringify(settings));

var ed = week_day.getProperties().edit();
ed.setBoolean("i.onGrid", false);
ed.setBoolean("s.iconVisibility", false);
ed.setBoolean("s.labelVisibility", true);
ed.getBox("i.box").setAlignment("CENTER", "MIDDLE");
ed.setEventHandler("i.menu", EventHandler.RUN_SCRIPT, script_menu.getId());
ed.commit();

var formula = 'var unused=$ll_day_name; \n'+
              'var reload=$week_day_reload; \n'+
              'var tag = JSON.parse(item.getTag("'+MY_PKG+'"));\n'+
              'var days=tag.dayNames.split(",");\n'+
              'var now=new Date().getDay();\n'+
              'days[now]="<font color=\'"+tag.hlColor+"\'>"+days[now]+"</font>";\n'+
              'return days.join(" ");'
week_day.setBinding("s.label", formula, true);

var size = d.getWidth()/2;

// use the last screen touch position, if any, as location for the new item
bindClass("java.lang.Integer");
var x = screen.getLastTouchX();
var y = screen.getLastTouchY();
if(x == Integer.MIN_VALUE || y == Integer.MIN_VALUE) {
    // no previous touch event, use a default position (can happen when using the hardware menu key for instance)
    x = d.getWidth()/2;
    y = d.getHeight()/2;
}

// trick: let the system compute the needed size, and center around the touch point
week_day.setSize(0, 0);
setTimeout(function() {
    x-=week_day.getWidth()/2;
    y-=week_day.getHeight()/2;
    week_day.setPosition(x, y);
}, 0);
