/*
Pierre Hébert - Counter 11/2016
Compatible with Lightning Launcher v14b6+.
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

var counter = item.my.counter;

// for our sample counter, customize the normal mode menu only
var mode = menu.getMode();
if(mode == Menu.MODE_ITEM_NO_EM) {
    // shows how to replace standard items with only our customized items
    menu.getMainItemsView().removeAllViews();

    // add a "Reset" button
    menu.addMainItem("Reset", function(v) {
        counter.reset();
        menu.close();
    });

    // add another "Settings" button
    menu.addMainItem("Settings", function(v) {
        showSettings();
        menu.close();
    });
}


bindClass("android.app.AlertDialog");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceListView");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceCategory");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceCheckBox");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceSlider");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceText");

// this function displays an alert dialog filled with a preference screen
function showSettings() {
    var screen = getActiveScreen();
    var context = screen.getContext();

    // create various preferences
    var prefMainCategory = new LLPreferenceCategory(0, "Main");
    var prefIncrement = new LLPreferenceSlider(0, "Increment", null, counter.getIncrement(), null, "FLOAT", 0.5, 3, 0.5, null);
    var prefBackward = new LLPreferenceCheckBox(0, "Backward", "Instead of adding the increment value, substract it", counter.getBackward(), null);
    var prefUnit = new LLPreferenceText(0, "Unit", counter.getUnit(), null);

    // create the list view, it will hold preferences created above
    var listView = new LLPreferenceListView(context, null);

    // assign preferences to the list view
    listView.setPreferences([
        prefMainCategory,
            prefIncrement,
            prefBackward,
            prefUnit,
    ]);

    // create a dialog and set the list view as the main content view
    var builder=new AlertDialog.Builder(context);
    builder.setView(listView);
    builder.setTitle("Settings");
    builder.setPositiveButton("Save",{onClick:function(dialog,id) {
        // save back data
        counter.setIncrement(prefIncrement.getValue());
        counter.setBackward(prefBackward.isChecked());
        counter.setUnit(prefUnit.getValue());
        dialog.dismiss();
    }});
    builder.setNegativeButton("Cancel", null);
    builder.show();
}