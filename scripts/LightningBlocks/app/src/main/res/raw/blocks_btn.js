/*
Pierre Hébert - Sketch - 2014
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

var item = LL.getEvent().getItem();
var drawing = item.getParent().getItemByLabel("drawing");

// this handler is associated with several buttons, check the button label to see what to do
var label = item.getName();
if(label == "color") {
    var color = item.getProperties().getBox("i.box").getColor("c", "n");
    drawing.setTag(color);
} else if(label == "clear") {
    if(confirm("Clear this sketch ?")) {
        var image = drawing.getBoxBackground("n");
        image.draw().drawARGB(255,255,255,255);
        image.update();
        image.save();
    }
}