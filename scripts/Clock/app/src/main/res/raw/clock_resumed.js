/*
Pierre Hébert - Clock - 10/2016
Compatible with Lightning Launcher v14b3+.
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

var clock = getEvent().getItem();

var paint = new Paint(Paint.ANTI_ALIAS_FLAG);

var textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

var transform = new Matrix();

var EXT_RADIUS = 90;

function refresh() {
    var settings = JSON.parse(clock.getTag("settings"));

    var image = clock.getBoxBackground("n");

    image.getBitmap().eraseColor(0);
    var canvas = image.draw();
    var w = clock.getWidth();
    var h = clock.getHeight();
    var iw = image.getWidth();
    var ih = image.getHeight();

    // make things so that drawing is done in a square [-100;100] unit space, 0/0 is at the center of the image
    canvas.scale(iw/w, ih/h);

    transform.setRectToRect(new RectF(-100, -100, 100, 100), new RectF(0, 0, w, h), Matrix.ScaleToFit.CENTER);
    canvas.concat(transform);

    // background
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(settings.bgColor);
    canvas.drawCircle(0, 0, EXT_RADIUS, paint);

    // outer border
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(settings.frameColor);
    paint.setStrokeWidth(4);
    canvas.drawCircle(0, 0, EXT_RADIUS, paint);



    // draw some ticks and digits
    paint.setStrokeCap(Paint.Cap.SQUARE);
    for(var t=0; t<12; t++) {
        canvas.save();
        canvas.rotate(t*30);
        canvas.drawLine(EXT_RADIUS-1, 0, EXT_RADIUS-10, 0, paint);
        canvas.restore();
    }


    if(settings.showDigits) {
        textPaint.setColor(settings.digitsColor);
        textPaint.setTextSize(settings.digitsSize);
        var fontMetrics = textPaint.getFontMetrics();
        for(var t=1; t<=12; t++) {
            var w = textPaint.measureText(t);
            var angle = -90+t*30;
            canvas.save();
            canvas.rotate(angle);
            canvas.translate(EXT_RADIUS-30, 0);
            canvas.rotate(-angle);
            canvas.drawText(t, -w/2, (-fontMetrics.ascent)/2, textPaint);
            canvas.restore();
        }
    }

    // draw needles according to the hour
    var now = new Date();
    paint.setStrokeCap(Paint.Cap.ROUND);

    // hour
    canvas.save();
    canvas.rotate((now.getHours()%12+now.getMinutes()/60)*30 - 90);
    canvas.drawLine(0, 0, settings.showDigits ? 35 : 50, 0, paint);
    canvas.restore();

    // minute
    canvas.save();
    canvas.rotate(now.getMinutes()*6 - 90);
    canvas.drawLine(0, 0, settings.showDigits ? 45 : 70, 0, paint);
    canvas.restore();

    // second
    if(settings.showSeconds) {
        paint.setStrokeWidth(2);
        canvas.save();
        canvas.rotate(now.getSeconds()*6 - 90);
        canvas.drawLine(0, 0, settings.showDigits ? 45 : 70, 0, paint);
        canvas.restore();
    }

    image.update();

    clock.setBoxBackground(image, "sf");

    // store the timeout id so that it can be cleared when pausing the item
    clock.setTag("timeout", setTimeout(refresh, 1000));
}

refresh();