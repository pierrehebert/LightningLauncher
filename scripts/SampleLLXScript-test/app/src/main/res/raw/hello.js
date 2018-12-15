LL.bindClass('android.view.animation.AccelerateDecelerateInterpolator');
LL.bindClass('android.view.animation.AnimationUtils');

var item = LL.getEvent().getItem();

var properties = item.getProperties();
var was_on_grid = properties.getBoolean('i.onGrid');
var interpolator = new AccelerateDecelerateInterpolator();

if(was_on_grid) {
    properties.edit().setBoolean('i.onGrid', false).commit();
}

var start = AnimationUtils.currentAnimationTimeMillis();
var duration = 4000;
var turn = 5;

rotate();

function rotate() {
    var now = AnimationUtils.currentAnimationTimeMillis();
    var s = (now-start)/duration;
    if(s > 1) s = 1;

    item.setRotation(interpolator.getInterpolation(s)*360*turn);

    if(s<1) {
        setTimeout(rotate, 0);
    }
}
