/*
Pierre Hébert - Clock - 10/2016
Compatible with Lightning Launcher v14b3+.
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

// add a "Settings" menu in the main item menu (either edit mode or normal mode)
var mode = menu.getMode();
if(mode == Menu.MODE_ITEM_NO_EM || mode == Menu.MODE_ITEM_EM) {
    menu.addMainItem("Clock settings", function(v) {
        showSettings();
        menu.close();
    });
}


bindClass("android.app.AlertDialog");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceListView");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceCategory");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceColor");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceCheckBox");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceSlider");

// build and show a dialog with clock preferences
function showSettings() {
    var screen = getActiveScreen();
    var context = screen.getContext();

    // extract settings stored in the item tag
    var settings = JSON.parse(item.getTag("settings"));

    // description of our preferences
    var prefCategoryFrame = new LLPreferenceCategory(0, "Frame");
    var prefFrameColor = new LLPreferenceColor(0, "Color", null, settings.frameColor, null, true);
    var prefBgColor = new LLPreferenceColor(0, "Background color", null, settings.bgColor, null, true);
    var prefShowSeconds = new LLPreferenceCheckBox(0, "Show seconds", null, settings.showSeconds, null);
    var prefCategoryDigits = new LLPreferenceCategory(0, "Digits");
    var prefShowDigits = new LLPreferenceCheckBox(0, "Show digits", null, settings.showDigits, null);
    var prefDigitsColor = new LLPreferenceColor(0, "Digits color", null, settings.digitsColor, null, true);
    var prefDigitsSize = new LLPreferenceSlider(0, "Digits size", null, settings.digitsSize, null, "INT", 10, 25, 1, null);

    prefShowDigits.setDependencies([prefDigitsColor, prefDigitsSize], null);

    // create a list view to display our preferences
    var listView = new LLPreferenceListView(context, null);
    listView.setPreferences([
        prefCategoryFrame,
            prefFrameColor,
            prefBgColor,
            prefShowSeconds,
        prefCategoryDigits,
            prefShowDigits,
            prefDigitsColor,
            prefDigitsSize
    ]);

    // setup the dialog and assign its content view with the preference list view
    var builder=new AlertDialog.Builder(context);
    builder.setView(listView);
    builder.setTitle("Clock settings");
    builder.setPositiveButton("Save",{onClick:function(dialog,id){
        // update the settings and save them back in the item's tag
        settings.frameColor = prefFrameColor.getColor();
        settings.bgColor = prefBgColor.getColor();
        settings.showSeconds = prefShowSeconds.isChecked();
        settings.showDigits = prefShowDigits.isChecked();
        settings.digitsColor = prefDigitsColor.getColor();
        settings.digitsSize = prefDigitsSize.getValue();
        item.setTag("settings", JSON.stringify(settings));
        dialog.dismiss();
    }});
    builder.setNegativeButton("Cancel", null);
    builder.show();
}