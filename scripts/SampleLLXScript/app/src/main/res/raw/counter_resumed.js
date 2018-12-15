/*
Pierre Hébert - Counter 11/2016
Compatible with Lightning Launcher v14b6+.
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

var item = getEvent().getItem();

// check whether the setup has been done, if not install our extended object
var counter = item.my.counter;
if(!counter) {
    // create a new object that extends the native item
    counter = item.extend();

    // store this extended item in the item's live object storage, so that we can easily access it
    // from any other script
    item.my.counter = counter;

    // go to the next value by adding or substracting the increment, according to the backward
    // setting
    counter.nextValue = function() {
        this.data.value += this.data.increment * (this.data.backward ? -1 : 1);
        this.updateText();
    }

    // set this counter to 0
    counter.reset = function() {
        this.data.value = 0;
        this.updateText();
    }

    // this method update the item's label with the counter value
    counter.updateText = function() {
        this.setLabel(this.data.value+" "+this.data.unit);
    }

    // some getters
    counter.getIncrement = function() { return this.data.increment; }
    counter.getBackward = function() { return this.data.backward; }
    counter.getUnit = function() { return this.data.unit; }

    // related setters
    counter.setIncrement = function(increment) { this.data.increment = increment; }
    counter.setBackward = function(backward) { this.data.backward = backward; }
    counter.setUnit = function(unit) { this.data.unit = unit; this.updateText(); }

    // when storing data in tag, it is best to use a unique name, as other scripts could wish to
    // save data too
    var MY_TAG_NAME = "net.pierrox.lightning_launcher.llscript.counter";

    // define a method to load counter data from persistent storage
    counter.loadData = function() {
        var tag = this.getTag(MY_TAG_NAME);
        if(tag == null) {
            // no tag: this is a new counter, set some default data
            this.data = {
                value: 0,           // the current counter value
                increment: 1,       // by how much to increment the value
                backward: false,    // add or substract the increment
                unit: "",           // display some unit next to the value
            }
        } else {
            this.data = JSON.parse(tag);
        }

        this.updateText();
    }

    // define a method to save data to persistent storage
    counter.saveData = function() {
        this.setTag(MY_TAG_NAME, JSON.stringify(this.data));
    }

    // once everything has been setup, load data from persistent storage
    counter.loadData();
}

