var unlock_distance = item.getWidth()/3;

function reset() {
  item.getProperties().edit().setInteger("i.alpha", 255).commit();
}

var x = event.getX();
var y = event.getY();
switch(event.getAction()) {
case MotionEvent.ACTION_DOWN:
  touch_x = x;
  touch_y = y;
  distance = 0;
  break;

case MotionEvent.ACTION_MOVE:
  var dx = x - touch_x;
  var dy = y - touch_y;
  distance = Math.sqrt(dx*dx + dy*dy);
  var a = 1 - (distance>unlock_distance ? 1 : distance / unlock_distance);
  item.getProperties().edit().setInteger("i.alpha", 255*a).commit();
  break;

case MotionEvent.ACTION_UP:
  if(distance > unlock_distance) {
    unlock();
    // reset the foreground after a short delay so that it does not flicker
    setTimeout(reset, 1000);
  } else {
    reset();
  }

  break;
}

return true;