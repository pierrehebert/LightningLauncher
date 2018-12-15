var item = LL.getEvent().getItem();

//var img = LL.createImage({
//    draw: function(context) {
//        var canvas = context.getCanvas();
//        var w = context.getWidth();
//        var h = context.getHeight();
//        var cx = w/2;
//        var cy = h/2;
//        var p = new Paint();
//        p.setAntiAlias(true);
//
//        var step = 1;
//        canvas.save();
//        for(var angle = 0; angle < 180; angle += step) {
//            p.setColor(Color.HSVToColor([angle*2, 1, 1]));
//            canvas.rotate(step, cx, cy);
//            canvas.drawLine(0, cy, w, cy, p);
//        }
//        canvas.restore();
//    }
//}, -1, -1);

var drawing = {
     draw: function(context) {
         var canvas = context.getCanvas();
         var w = context.getWidth();
         var h = context.getHeight();
         var cx = w/2;
         var cy = h/2;
         var p = new Paint();
         p.setAntiAlias(true);
         p.setStrokeWidth(4);

         var step = 4;
         canvas.save();
         for(var angle = 0; angle < 180; angle += step) {
            var hue = ((angle+drawing.shift)*2)%360
             p.setColor(Color.HSVToColor([hue, 1, 1]));
             canvas.rotate(step, cx, cy);
             canvas.drawLine(0, cy, w, cy, p);
         }
         canvas.restore();
     },

     resume: function(context) {
         var animate = function() {
             context.invalidate();
             var id = ""+context.getId();
             drawing.shift++;
             drawing.timers[id] = setTimeout(animate, 10);
         };
         animate();
     },

     pause: function(context) {
         var id = ""+context.getId();
         clearTimeout(drawing.timers[id]);
         delete drawing.timers[id];
     },

     shift: 0,
     timers: {}
};

var img = LL.createImage(drawing, -1, -1);

item.setImage(img);
//item.setBoxBackground(img, "n");
//item.setWindowBackground(item.getBoxBackground("n"));