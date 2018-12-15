// must be shortcut
var item = LL.getCurrentDesktop().getItemByName('a');
var from = LL.getCurrentDesktop().getItemByName('b');
var folder = LL.getCurrentDesktop().getItemByName('c');


// load a png and set it as the default icon
this.test0 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    item.setDefaultIcon(img);
}

// load a 9 patch and set it as the default icon
this.test1 = function() {
    var img = LL.createImage("/sdcard/gallery_selected_default.9.png");
    item.setDefaultIcon(img);
}

// load a gif and set it as the default icon
this.test2 = function() {
    var img = LL.createImage("/sdcard/test5.gif");
    item.setDefaultIcon(img);
}

// clear the default icon
this.test0b = function() {
    item.setDefaultIcon(null);
}

// load a png and set it as the custom icon
this.test3 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    item.setCustomIcon(img);
}

// load a 9 patch and set it as the custom icon
this.test4 = function() {
    var img = LL.createImage("/sdcard/gallery_selected_default.9.png");
    item.setCustomIcon(img);
}

// load a gif and set it as the custom icon
this.test5 = function() {
    var img = LL.createImage("/sdcard/test5.gif");
    item.setCustomIcon(img);
}

// clear the custom icon
this.test5b = function() {
    item.setCustomIcon(null);
}

// load a png and set it as the image
this.test6 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    item.setImage(img);
}

// load a 9 patch and set it as the image
this.test7 = function() {
    var img = LL.createImage("/sdcard/gallery_selected_default.9.png");
    item.setImage(img);
}

// load a gif and set it as the image
this.test8 = function() {
    var img = LL.createImage("/sdcard/test5.gif");
    item.setImage(img);
}

// modify and update an image
this.test9 = function() {
    var img = item.getImage();
    var c = img.draw();
    var p = new Paint();
    p.setColor(Color.BLUE);
    p.setStyle(Paint.Style.STROKE);
    c.drawARGB(255, 255, 255, 255);
    var w = img.getWidth();
    var h = img.getHeight();
    for(var i=0; i<w; i+=2) {
        c.drawLine(i, 0, i, h, p);
    }
    img.update();
}

// create an image and assign it as the custom icon
this.test10 = function() {
    var img = LL.createImage(400, 400);
    var c = img.draw();
    var p = new Paint();
    p.setColor(Color.BLACK);
    p.setStyle(Paint.Style.STROKE);
    c.drawARGB(255, 255, 255, 255);
    for(var i=0; i<100; i+=2) {
        c.drawRect(50+i, 50+i, 350-i, 350-i, p);
    }
    item.setCustomIcon(img);
}

// create an image and assign it as the image
this.test11 = function() {
    var img = LL.createImage(400, 400);
    var c = img.draw();
    var p = new Paint();
    p.setColor(Color.BLACK);
    p.setStyle(Paint.Style.STROKE);
    c.drawARGB(255, 255, 255, 255);
    for(var i=0; i<100; i+=2) {
        c.drawRect(50+i, 50+i, 350-i, 350-i, p);
    }
    item.setImage(img);
}

// start the animation for a given number of times (must use an animated gif...)
this.test12 = function() {
    var img = item.getImage();
    img.setLoopCount(3);
    img.start();
}

// stop the animation
this.test13 = function() {
    var img = item.getImage();
    img.stop();
}

// display the animation status
this.test14 = function() {
    var img = item.getImage();
    alert(img.isPlaying());
}

// copy a png from an item to another (custom icon to custom icon)
this.test15 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    from.setCustomIcon(img);
    item.setCustomIcon(from.getCustomIcon());
}

// copy 9 patch from an item to another (custom icon to custom icon)
this.test16 = function() {
    var img = LL.createImage("/sdcard/gallery_selected_default.9.png");
    from.setCustomIcon(img);
    item.setCustomIcon(from.getCustomIcon());
}

// copy a gif from an item to another (custom icon to custom icon)
this.test17 = function() {
    var img = LL.createImage("/sdcard/test5.gif");
    from.setCustomIcon(img);
    item.setCustomIcon(from.getCustomIcon());
}

// copy a png from an item to another (image to image)
this.test18 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    from.setCustomIcon(img);
    item.setImage(from.getImage());
}

// copy 9 patch from an item to another (image to image)
this.test19 = function() {
    var img = LL.createImage("/sdcard/gallery_selected_default.9.png");
    from.setCustomIcon(img);
    item.setImage(from.getImage());
}

// copy a gif from an item to another (image to image)
this.test20 = function() {
    var img = LL.createImage("/sdcard/test5.gif");
    from.setCustomIcon(img);
    item.setImage(from.getImage());
}

// copy a png from an item to another  (image to custom icon)
this.test21 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    from.setCustomIcon(img);
    item.setCustomIcon(from.getImage());
}

// copy 9 patch from an item to another (image to custom icon)
this.test22 = function() {
    var img = LL.createImage("/sdcard/gallery_selected_default.9.png");
    from.setCustomIcon(img);
    item.setCustomIcon(from.getImage());
}

// copy a gif from an item to another (image to custom icon)
this.test23 = function() {
    var img = LL.createImage("/sdcard/test5.gif");
    from.setCustomIcon(img);
    item.setCustomIcon(from.getImage()); // should not work, persisting animation is not yet supported
}

// copy a png from an item to another (custom icon to image)
this.test24 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    from.setCustomIcon(img);
    item.setImage(from.getCustomIcon());
}

// copy 9 patch from an item to another (custom icon to image)
this.test25 = function() {
    var img = LL.createImage("/sdcard/gallery_selected_default.9.png");
    from.setCustomIcon(img);
    item.setImage(from.getCustomIcon());
}

// copy a gif from an item to another (custom icon to image)
this.test26 = function() {
    var img = LL.createImage("/sdcard/test5.gif");
    from.setCustomIcon(img);
    item.setImage(from.getCustomIcon());
}

// modify an animation frame
this.test27 = function() {
    var img = item.getImage();
    var frame = img.getFrameImage(1);
    var c = frame.draw();
    var p = new Paint();
    p.setColor(Color.BLUE);
    p.setStyle(Paint.Style.STROKE);
    c.drawARGB(255, 255, 255, 255);
    var w = img.getWidth();
    var h = img.getHeight();
    for(var i=0; i<w; i+=2) {
        c.drawLine(i, 0, i, h, p);
    }
    frame.update();
}

// modify the animation frame count
this.test28 = function() {
    var img = item.getImage();
    alert(img.getFrameCount()+" "+img.getTotalDuration());
    img.setFrameCount(10);
    alert(img.getFrameCount()+" "+img.getTotalDuration());
    img.setFrameCount(5);
    alert(img.getFrameCount()+" "+img.getTotalDuration());
    img.setFrameCount(15);
    alert(img.getFrameCount()+" "+img.getTotalDuration());
}

// recreate an animation
this.test29 = function() {
    var img = item.getImage();
    var n = 10;
    img.setFrameCount(n);

    var p = new Paint();
    p.setAntiAlias(true);
    p.setTextSize(30);
    for(var i=0; i<n; i++) {
        var frame = img.getFrameImage(i);
        var c = frame.draw();
        c.drawARGB(255, 255, 255, 255);
        c.drawText(""+i, 40, 50, p);
        img.setFrameDuration(i, 100*(n-i));
        frame.update();
    }
    img.start(5);
}

// create an animation from scratch
this.test30 = function() {
    var n = 5;
    var img = LL.createAnimation(100, 100, n, 500, 0);

    var p = new Paint();
    p.setAntiAlias(true);
    p.setTextSize(30);
    for(var i=0; i<n; i++) {
        var frame = img.getFrameImage(i);
        var c = frame.draw();
        c.drawARGB(255, 255, 255, 255);
        c.drawText(""+i, 40, 50, p);
        frame.update();
   }

   item.setCustomIcon(img);
}

// create an animation from scratch, save it and reload it
this.test30b = function() {
    var n = 5;
    var img = LL.createAnimation(100, 100, n, 500, 0);

    var p = new Paint();
    p.setAntiAlias(true);
    p.setColor(Color.YELLOW);
    p.setTextSize(30);
    for(var i=0; i<n; i++) {
        var frame = img.getFrameImage(i);
        img.setFrameDuration(i, 500+i*200);
        var c = frame.draw();
        c.drawText(""+i, 40, 50, p);
        frame.update();
   }

    img.setLoopCount(3);

   var path = "/sdcard/LightningLauncher/tmp/out.gif";
   img.saveToFile(path);
   var img2 = LL.createImage(path);
   item.setCustomIcon(img2);
   //new File(path).delete();
}

// pick any kind of image
this.test31 = function() {
    var img = LL.pickImage(0);
    item.setImage(img);
}

// box background png
this.test32 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    item.setBoxBackground(img, "nsf");
}

// box background 9 patch
this.test33 = function() {
    var img = LL.createImage("/sdcard/gallery_selected_default.9.png");
    item.setBoxBackground(img, "nsf");
}

// box background gif
this.test34 = function() {
    var img = LL.createImage("/sdcard/test5.gif");
    item.setBoxBackground(img, "nsf");
}

// box background png (persistent)
this.test35 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    item.setBoxBackground(img, "nsf", true);
}

// box background 9 patch (persistent)
this.test36 = function() {
    var img = LL.createImage("/sdcard/gallery_selected_default.9.png");
    from.setCustomIcon(img);
    item.setBoxBackground(img, "nsf", true);
}

// box background gif (persistent)
this.test37 = function() {
    var img = LL.createImage("/sdcard/test5.gif");
    from.setCustomIcon(img);
    item.setBoxBackground(img, "nsf", true);
}

// icon layer png
this.test38 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    item.setIconLayer(img, "b");
}

// icon layer 9 patch
this.test39 = function() {
    var img = LL.createImage("/sdcard/gallery_selected_default.9.png");
    item.setIconLayer(img, "b");
}

// icon layer gif
this.test40 = function() {
    var img = LL.createImage("/sdcard/test5.gif");
    item.setIconLayer(img, "b");
}

// folder window background png
this.test41 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    folder.setWindowBackground(img);
}

// folder window background 9 patch
this.test42 = function() {
    var img = LL.createImage("/sdcard/gallery_selected_default.9.png");
    folder.setWindowBackground(img);
}

// folder window background gif
this.test43 = function() {
    var img = LL.createImage("/sdcard/test5.gif");
    folder.setWindowBackground(img);
}

// crop an image
this.test44 = function() {
    var img = LL.createImage("/sdcard/icon.png");
    var cropped = LL.cropImage(img, true);
//    alert(cropped.getWidth()+"x"+cropped.getHeight());
    item.setCustomIcon(cropped);
}

// scripted animation
this.test45 = function() {
    var timers = {};
    var img = LL.createImage({
        draw: function(context) {
            var canvas = context.getCanvas();
            canvas.drawARGB(255, 255, 255, 0);
            canvas.drawText("date: "+new Date()+" "+context.getWidth()+"x"+context.getHeight(), 50, 50, new Paint());
        },

        resume: function(context) {
            var animate = function() {
                context.invalidate();
                var id = ""+context.getId();
                timers[id] = setTimeout(animate, 500);
            };
            animate();
        },

        pause: function(context) {
            var id = ""+context.getId();
            clearTimeout(timers[id]);
            delete timers[id];
        }
    }, 800, -1);

    item.setImage(img);
}

var which = 44;
//var which = prompt("Test to run?", this.next);
//this.next = Number(which)+1;
this["test"+which]();
