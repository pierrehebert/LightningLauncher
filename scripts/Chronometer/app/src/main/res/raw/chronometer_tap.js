var chrono = getEvent().getItem().my.chrono;
if(chrono.isRunning()) {
    if(chrono.isPaused()) {
        chrono.resume();
    } else {
        chrono.pause();
    }
} else {
    chrono.start();
}