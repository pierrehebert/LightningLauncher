/*
Pierre Hébert - Counter 11/2016
Compatible with Lightning Launcher v14b6+.
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

// When the item is paused, it means that the launcher may be suspended and stopped, or the item
// may be rebuilt. As a consequence, save current data to persistent storage, they will be loaded
// again when the item is rebuilt
getEvent().getItem().my.counter.saveData();