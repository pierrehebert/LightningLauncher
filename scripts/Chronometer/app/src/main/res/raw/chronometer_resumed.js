var item = getEvent().getItem();
var chrono = item.my.chrono;
if(!chrono) {
    chrono = item.extend();
    item.my.chrono = chrono;

    chrono.now = function () {
         return new Date().getTime();
    }

    chrono.isRunning = function() {
        return this.data.isRunning;
    }

    chrono.isPaused = function() {
        return this.data.isPaused;
    }

    chrono.start = function() {
        this.data.startTime = this.now();
        this.data.isRunning = true;
        this.data.isPaused = false;
        this.startRefreshLoop();
    }

    chrono.pause = function() {
        this.data.pauseTime = this.now();
        this.data.isPaused = true;
        this.stopRefreshLoop();
    }

    chrono.resume = function() {
        this.data.startTime += this.now() - this.data.pauseTime;
        this.data.isPaused = false;
        this.startRefreshLoop();
    }

    chrono.reset = function() {
        this.data.isRunning = false;
        this.data.isPaused = false;
        this.updateText();
        this.stopRefreshLoop();
    }

    chrono.updateText = function() {
        var milliSeconds

        if(this.isRunning()) {
            milliSeconds = this.data.isPaused ? this.data.pauseTime - this.data.startTime : this.now() - this.data.startTime;
        } else {
            milliSeconds = 0;
        }

        this.setLabel(this.formatTime(milliSeconds));
    }

    chrono.formatTime = function(milliSeconds) {
        var sec = Math.floor(milliSeconds/1000)%60;
        var min = Math.floor(milliSeconds/60000)%60;
        var time = (min<10?"0":"")+min + ":" + (sec<10?"0":"")+sec;
        if(this.getShowMilliseconds()) {
            var msec = milliSeconds % 1000;
            var smsec = "00"+msec;
            time += ":" + smsec.substr(smsec.length-3);
        } else {
            var hour = Math.floor(milliSeconds/3600000);
            time = (hour<10?"0":"")+hour + ":" + time;
        }

        return time;
    }

    chrono.startRefreshLoop = function() {
        this.stopRefreshLoop();

        var obj = this;
        var refresh = function() {
            obj.updateText();
            obj.data.timer = setTimeout(refresh, obj.getShowMilliseconds() ? 10 : 1000);
        }

        refresh();
    }

    chrono.stopRefreshLoop = function() {
        if(this.data.timer && this.data.timer != -1) {
            clearTimeout(this.data.timer);
            this.data.timer = -1;
        }
    }

    chrono.setShowMilliseconds = function(show) {
        this.data.showMilliseconds = show;
        this.updateText();
    }

    chrono.getShowMilliseconds = function() {
        return this.data.showMilliseconds;
    }

    chrono.loadDataFromTag = function() {
        this.data = JSON.parse(this.getTag("data"));
        this.updateText();
    }

    chrono.saveDataToTag = function() {
        this.setTag("data", JSON.stringify(this.data));
    }

    chrono.loadDataFromTag();
}

if(chrono.isRunning() && !chrono.isPaused()) {
    chrono.startRefreshLoop();
}
