/*
Pierre Hébert - Sketch - 2014
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

var item = getEvent().getItem();
var container = item.getParent();
var drawing = container.getItemByLabel("drawing");

// this handler is associated with several buttons, check the button label to see what to do
var name = item.getName();
if(name == "settings") {
    net_pierrox_sketch.showSettings(drawing);
} else if(name == "clear") {
    if(confirm("Clear this sketch ?")) {
        net_pierrox_sketch.clear(drawing);
    }
} else {
    // neither settings nor clear, this is a color
    var oldColor = net_pierrox_sketch.getCurrentColor(drawing);
    net_pierrox_sketch.setColorButtonSelection(container, oldColor, false)

    var newColor = item.getProperties().getBox("i.box").getColor("c", "n");
    net_pierrox_sketch.setColorButtonSelection(container, newColor, true)

    net_pierrox_sketch.setCurrentColor(drawing, newColor);
}