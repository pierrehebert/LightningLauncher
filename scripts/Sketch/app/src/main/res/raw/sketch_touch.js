/*
Pierre Hébert - Sketch - 2014
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/
var x = event.getX();
var y = event.getY();
switch(event.getAction()) {
case MotionEvent.ACTION_DOWN:
  prev_x = x;
  prev_y = y;
  var color = net_pierrox_sketch.getCurrentColor(item);
  var penSize = net_pierrox_sketch.getPenSize(item);
  paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  paint.setColor(color);
  paint.setStrokeWidth(penSize);
  paint.setStrokeCap(Paint.Cap.ROUND);
  image = item.getBoxBackground("n");
  canvas = image.draw();
  canvas.scale( image.getWidth() / item.getWidth(), image.getHeight() / item.getHeight());
  break;

case MotionEvent.ACTION_MOVE:
  canvas.drawLine(prev_x, prev_y, x, y, paint);
  prev_x = x;
  prev_y = y;
  image.update();
  break;

case MotionEvent.ACTION_UP:
  if(prev_x==x && prev_y==y) {
      canvas.drawPoint(x, y, paint);
      image.update();
  }
  image.save();
  paint = null;
  canvas = null;
  break;
}

return true;