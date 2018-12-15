/*
Pierre Hébert - Counter 11/2016
Compatible with Lightning Launcher v14b6+.
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

var counter = item.my.counter;

var mode = menu.getMode();
if(mode == Menu.MODE_ITEM_NO_EM) {
    menu.getMainItemsView().removeAllViews();

    menu.addMainItem("Settings", function(v) {
        showSettings();
        menu.close();
    });
}


bindClass("android.app.AlertDialog");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceListView");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceCategory");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceText");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceColor");

var MY_PKG = "net.pierrox.lightning_launcher.llscript.week_day";

function showSettings() {
    var screen = getActiveScreen();
    var context = screen.getContext();

    var tag = JSON.parse(item.getTag(MY_PKG));

    // create various preferences
    var prefMainCategory = new LLPreferenceCategory(0, "Main");
    var prefHlColor = new LLPreferenceColor(0, "Highlight color", null, parseInt("0xff"+tag.hlColor.substr(1)), null, false);
    var prefDayNames = new LLPreferenceText(0, "Day names", tag.dayNames, null);

    // create the list view, it will hold preferences created above
    var listView = new LLPreferenceListView(context, null);

    // assign preferences to the list view
    listView.setPreferences([
        prefMainCategory,
            prefHlColor,
            prefDayNames,
    ]);

    // create a dialog and set the list view as the main content view
    var builder=new AlertDialog.Builder(context);
    builder.setView(listView);
    builder.setTitle("Settings");
    builder.setPositiveButton("Save",{onClick:function(dialog,id) {
        // save back data
        var newColor = prefHlColor.getColor();
        if (newColor < 0) newColor = 0xFFFFFFFF + newColor + 1;
        var h = "000000"+newColor.toString(16);
        tag.hlColor = "#"+h.substr(h.length-6);
        tag.dayNames = prefDayNames.getValue();
        item.setTag(MY_PKG, JSON.stringify(tag));
        setVariableInteger("week_day_reload", new Date().getTime());
        dialog.dismiss();
    }});
    builder.setNegativeButton("Cancel", null);
    builder.show();
}