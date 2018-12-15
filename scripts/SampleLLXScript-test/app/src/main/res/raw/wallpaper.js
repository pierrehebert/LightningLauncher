var drawing = {
     draw: function(context) {
         var canvas = context.getCanvas();
         var w = context.getWidth();
         var h = context.getHeight();

         var d = LL.getCurrentDesktop();

         var angle = Math.atan(d.getPositionY()/d.getPositionX());
         var hue = angle*180 / Math.PI;

         var color = Color.HSVToColor([hue, 1, 1]);
         drawing.paint.setColor(color);
         canvas.drawRect(0, 0, w, h, drawing.paint);
     },

     resume: function(context) {
         var animate = function() {
             context.invalidate();
             var id = ""+context.getId();
             drawing.shift = Math.random()*10 - 5;
             drawing.timers[id] = setTimeout(animate, 100);
         };
         animate();
     },

     pause: function(context) {
         var id = ""+context.getId();
         clearTimeout(drawing.timers[id]);
         delete drawing.timers[id];
     },

     paint: function() { var p = new Paint(); p.setStyle(Paint.Style.FILL); return p;}(),
     shift: 0,
     timers: {}
};

var img = LL.createImage(drawing, -1, -1);

LL.getEvent().getItem().setImage(img);