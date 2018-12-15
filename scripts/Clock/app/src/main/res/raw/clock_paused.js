/*
Pierre Hébert - Clock - 10/2016
Compatible with Lightning Launcher v14b3+.
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

// important: preserve the battery life and clear timeouts when the item is paused to avoid useless background activity
var id = getEvent().getItem().getTag("timeout");
if(id != null) {
    clearTimeout(parseInt(id));
}