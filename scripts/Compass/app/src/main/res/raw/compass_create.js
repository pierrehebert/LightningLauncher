/*

Lightning 3D Compass - Pierre Hébert - 2016
This software is provided as is.
You are free to use, duplicate and modify it without restriction provided that you retain this notice.

This is a simple port of my application "Marine Compass" (https://play.google.com/store/apps/details?id=net.pierrox.mcompass)

Don't look too closely at the code, it's a bit messy.
*/

bindClass("java.lang.reflect.Array");
bindClass("java.lang.Integer");
bindClass("java.lang.Byte");
bindClass("java.nio.ByteBuffer");
bindClass("java.nio.ByteOrder");
bindClass("javax.microedition.khronos.opengles.GL10");
bindClass("android.graphics.Bitmap");
bindClass("android.graphics.Canvas");
bindClass("android.graphics.PixelFormat");
bindClass("android.opengl.GLSurfaceView");
bindClass("android.widget.ScrollView");
bindClass("android.widget.HorizontalScrollView");
bindClass("android.view.View");
bindClass("android.view.ViewGroup");
bindClass("android.view.Gravity");
bindClass("android.widget.TextView");
bindClass("android.widget.FrameLayout");
bindClass("android.widget.ImageView");
bindClass("android.hardware.SensorManager");
bindClass("android.hardware.Sensor");

var DETAIL_X = [ 15, 25, 50 ];
var DETAIL_Y = [ 3, 6, 6 ];
var RING_HEIGHT= [ 2, 3, 3 ];

var TEXTURE_RING = 0;
var TEXTURE_DIAL = 1;

var CARDINAL_POINTS = [ "N", "W", "S", "E" ];

// preference values
var mDetailsLevel = 0;
var mReversedRing = false;

var mTextures;

var mRingVertexBuffer = null;
var mRingNormalBuffer = null;
var mRingTexCoordBuffer = null;
var mRingIndexBuffer = null;

var mDialVertexBuffer;
var mDialNormalBuffer;
var mDialTexCoordBuffer;
var mDialIndexBuffer;

var mCapVertexBuffer;
var mCapIndexBuffer;

var mNeedObjectsUpdate = true;
var mNeedTextureUpdate = true;

function buildObjects() {
    buildRingObject();
	buildCapObject();
    buildDialObject();

	mNeedObjectsUpdate=false;
}


function buildRingObject() {
    // build vertices
    var dx=DETAIL_X[mDetailsLevel];
    var dy=DETAIL_Y[mDetailsLevel];
    var rh=RING_HEIGHT[mDetailsLevel];

    var vertices = Array.newInstance(Integer.TYPE, ((dx+1)*(rh+1))*3);
    var normals = Array.newInstance(Integer.TYPE, ((dx+1)*(rh+1))*3);
    var n=0;
    for(var i=0; i<=dx; i++) {
        for(var j=0; j<=rh; j++) {
            var a = i*(Math.PI*2)/dx;
            var b = j*Math.PI/(dy*2);

            var x = Math.sin(a)*Math.cos(b);
            var y = -Math.sin(b);
            var z = Math.cos(a)*Math.cos(b);

            vertices[n] = (x*65536);
            vertices[n+1] = (y*65536);
            vertices[n+2] = (z*65536);
            normals[n] = vertices[n];
            normals[n+1] = vertices[n+1];
            normals[n+2] = vertices[n+2];
            n+=3;
        }
    }

    // build textures coordinates
    var texCoords = Array.newInstance(Integer.TYPE, (dx+1)*(rh+1)*2);
    n=0;
    for(var i=0; i<=dx; i++) {
        for(var j=0; j<=rh; j++) {
            texCoords[n++] = (i<<16)/dx;
            texCoords[n++] = (j<<16)/rh;
        }
    }

    // build indices
    var indices = Array.newInstance(Byte.TYPE, dx*rh*3*2);
    n=0;
    for(var i=0; i<dx; i++) {
        for(var j=0; j<rh; j++) {
            var p0=(rh+1)*i+j;
            indices[n++]=p0;
            indices[n++]=p0+rh+1;
            indices[n++]=p0+1;

            indices[n++]=p0+rh+1;
            indices[n++]=p0+rh+2;
            indices[n++]=p0+1;
        }
    }
    var vbb = ByteBuffer.allocateDirect(vertices.length*4);
    vbb.order(ByteOrder.nativeOrder());
    mRingVertexBuffer = vbb.asIntBuffer();
    mRingVertexBuffer.put(vertices);
    mRingVertexBuffer.position(0);

    var nbb = ByteBuffer.allocateDirect(normals.length*4);
    nbb.order(ByteOrder.nativeOrder());
    mRingNormalBuffer = nbb.asIntBuffer();
    mRingNormalBuffer.put(normals);
    mRingNormalBuffer.position(0);

    mRingIndexBuffer = ByteBuffer.allocateDirect(indices.length);
    mRingIndexBuffer.put(indices);
    mRingIndexBuffer.position(0);

    var tbb = ByteBuffer.allocateDirect(texCoords.length*4);
    tbb.order(ByteOrder.nativeOrder());
    mRingTexCoordBuffer = tbb.asIntBuffer();
    mRingTexCoordBuffer.put(texCoords);
    mRingTexCoordBuffer.position(0);
}

function buildCapObject() {
    var dx=DETAIL_X[mDetailsLevel];
    var dy=DETAIL_Y[mDetailsLevel];
    var rh=RING_HEIGHT[mDetailsLevel];

    var h=dy-rh;

    // build vertices
    var vertices = Array.newInstance(Integer.TYPE, ((dx+1)*(h+1))*3);
    var n=0;
    for(var i=0; i<=dx; i++) {
        for(var j=rh; j<=dy; j++) {
            var a = i*(Math.PI*2)/dx;
            var b = j*Math.PI/(dy*2);

            var x = Math.sin(a)*Math.cos(b);
            var y = -Math.sin(b);
            var z = Math.cos(a)*Math.cos(b);

            vertices[n++] = (x*65536);
            vertices[n++] = (y*65536);
            vertices[n++] = (z*65536);
        }
    }

    // build indices
    var indices = Array.newInstance(Byte.TYPE, dx*h*3*2);
    n=0;
    for(var i=0; i<dx; i++) {
        for(var j=0; j<h; j++) {
            var p0= ((h+1)*i+j);
            indices[n++]=p0;
            indices[n++]= (p0+h+1);
            indices[n++]= (p0+1);

            indices[n++]= (p0+h+1);
            indices[n++]= (p0+h+2);
            indices[n++]= (p0+1);
        }
    }

    var vbb = ByteBuffer.allocateDirect(vertices.length*4);
    vbb.order(ByteOrder.nativeOrder());
    mCapVertexBuffer = vbb.asIntBuffer();
    mCapVertexBuffer.put(vertices);
    mCapVertexBuffer.position(0);

    mCapIndexBuffer = ByteBuffer.allocateDirect(indices.length);
    mCapIndexBuffer.put(indices);
    mCapIndexBuffer.position(0);
}

function buildDialObject() {
    // build vertices
    var dx=DETAIL_X[mDetailsLevel];

    var vertices=Array.newInstance(Integer.TYPE, (dx+2)*3);
    var normals=Array.newInstance(Integer.TYPE, (dx+2)*3);
    var n=0;
    // center of the dial
    vertices[n] = 0;
    vertices[n+1] = 0;
    vertices[n+2] = 0;
    normals[n] = 0;
    normals[n+1] = 1<<16;
    normals[n+2] = 0;
    n+=3;
    for(var i=0; i<=dx; i++) {
        var a = i*(Math.PI*2)/dx;

        var x = Math.sin(a);
        var z = Math.cos(a);

        vertices[n] = (x*65536);
        vertices[n+1] = 0;
        vertices[n+2] = (z*65536);
        normals[n] = 0;
        normals[n+1] = 1<<16;
        normals[n+2] = 0;
        n+=3;
    }

    // build textures coordinates
    var texCoords=Array.newInstance(Integer.TYPE, (dx+2)*2);
    n=0;
    texCoords[n++] = (0.5*65536);
    texCoords[n++] = (0.5*65536);
    for(var i=0; i<=dx; i++) {
        var a = i*(Math.PI*2)/dx;

        var x = (Math.sin(a)+1)/2;
        var z = (Math.cos(a)+1)/2;

        texCoords[n++] = (x*65536);
        texCoords[n++] = (z*65536);
    }

    // build indices
    var indices=Array.newInstance(Byte.TYPE, dx+2);
    n=0;
    for(var i=0; i<=(dx+1); i++) {
        indices[n++]=i;
    }

    var vbb = ByteBuffer.allocateDirect(vertices.length*4);
    vbb.order(ByteOrder.nativeOrder());
    mDialVertexBuffer = vbb.asIntBuffer();
    mDialVertexBuffer.put(vertices);
    mDialVertexBuffer.position(0);

    var nbb = ByteBuffer.allocateDirect(normals.length*4);
    nbb.order(ByteOrder.nativeOrder());
    mDialNormalBuffer = nbb.asIntBuffer();
    mDialNormalBuffer.put(normals);
    mDialNormalBuffer.position(0);

    mDialIndexBuffer = ByteBuffer.allocateDirect(indices.length);
    mDialIndexBuffer.put(indices);
    mDialIndexBuffer.position(0);

    var tbb = ByteBuffer.allocateDirect(texCoords.length*4);
    tbb.order(ByteOrder.nativeOrder());
    mDialTexCoordBuffer = tbb.asIntBuffer();
    mDialTexCoordBuffer.put(texCoords);
    mDialTexCoordBuffer.position(0);
}

function draw(gl) {
    // rebuild objects or textures if needed
    if(mNeedObjectsUpdate) {
        buildObjects();
    }

    if(mNeedTextureUpdate) {
        buildTextures(gl);
    }



    var dx=DETAIL_X[mDetailsLevel];
    var dy=DETAIL_Y[mDetailsLevel];
    var rh=RING_HEIGHT[mDetailsLevel];

    gl.glFrontFace(GL10.GL_CW);
    gl.glColor4x(1<<16, 0<<16, 0<<16, 1<<16);

    // common parameters for the ring and the dial
    gl.glEnable(GL10.GL_TEXTURE_2D);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

    gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    gl.glColor4x(1<<16, 1<<16, 1<<16, 1<<16);
    gl.glScalex(120000, 120000, 120000);

    // draw the ring
    gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[TEXTURE_RING]);
    gl.glVertexPointer(3, GL10.GL_FIXED, 0, mRingVertexBuffer);
    gl.glNormalPointer(GL10.GL_FIXED, 0, mRingNormalBuffer);
    gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, mRingTexCoordBuffer);
    gl.glDrawElements(GL10.GL_TRIANGLES, dx*rh*6, GL10.GL_UNSIGNED_BYTE, mRingIndexBuffer);

    // draw the dial
    gl.glFrontFace(GL10.GL_CCW);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[TEXTURE_DIAL]);
    gl.glVertexPointer(3, GL10.GL_FIXED, 0, mDialVertexBuffer);
    gl.glNormalPointer(GL10.GL_FIXED, 0, mDialNormalBuffer);
    gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, mDialTexCoordBuffer);
    gl.glDrawElements(GL10.GL_TRIANGLE_FAN, dx+2, GL10.GL_UNSIGNED_BYTE, mDialIndexBuffer);
    gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);

    // draw the cap
    gl.glFrontFace(GL10.GL_CW);
    gl.glColor4x(0<<16, 0<<16, 0<<16, 1<<16);
    gl.glDisable(GL10.GL_TEXTURE_2D);
    gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FIXED, 0, mCapVertexBuffer);
    gl.glDrawElements(GL10.GL_TRIANGLES, dx*(dy-rh)*6, GL10.GL_UNSIGNED_BYTE, mCapIndexBuffer);
}

function buildTextures(gl) {
    mTextures=Array.newInstance(Integer.TYPE, 2);


    gl.glGenTextures(2, mTextures, 0);
    buildRingTexture(gl);
    buildDialTexture(gl);

    mNeedTextureUpdate=false;
}

function buildRingTexture(gl) {
    gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[TEXTURE_RING]);
    gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);
    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);

    var length = 512;
    var height = 64;
    var b = Bitmap.createBitmap(length, height, Bitmap.Config.ARGB_8888);
    b.eraseColor(0xff000000);
    var canvas = new Canvas(b);

    var p = new Paint();
    p.setAntiAlias(true);

    // draw minor graduations in grey
    /*p.setColor(0xffa0a0a0);
    for(int d=0; d<360; d++) {
        canvas.drawLine(d*2, 0, d*2, 10, p);
    }*/

    // draw medium graduations in white
    p.setColor(0xffffffff);
    for(var d=0; d<360; d+=10) {
        var pos=d*length/360;
        canvas.drawLine(pos, 0, pos, 20, p);
    }

    // draw major graduations in red
    p.setColor(0xffff0000);
    for(var d=0; d<360; d+=90) {
        var pos=d*length/360;
        canvas.drawLine(pos, 0, pos, 30, p);
    }

    // use center alignment for text
    p.setTextAlign(Paint.Align.CENTER);

    // draw minor graduations text
    p.setTextSize(9);
    p.setColor(0xffffffff);
    for(var d=0; d<360; d+=30) {
        // do not draw 0/90/180/270
        var pos=d*length/360;
        var angle=mReversedRing ? (360+180-d)%360 : 360-d;
        if(d%90!=0) canvas.drawText(Integer.toString(angle), pos, 30, p);
    }

    // draw N/O/S/E
    // hack : go till 360, so that "N" is printed at both end of the texture...
    p.setTextSize(20);
    p.setColor(0xffff0000);
    for(var d=0; d<=360; d+=90) {
        var pos=d*length/360;
        if(mReversedRing) {
            canvas.drawText(CARDINAL_POINTS[((d+180)/90)%4], pos, 50, p);
        } else {
            canvas.drawText(CARDINAL_POINTS[(d/90)%4], pos, 50, p);
        }
    }

    p.setShader(new LinearGradient(0, 5, 0, 0, 0xff000000, 0xffffffff, Shader.TileMode.CLAMP));
    canvas.drawRect(0, 0, length, 5, p);
    var bb=ByteBuffer.allocate(length*height*4);
    b.copyPixelsToBuffer(bb);
    bb.position(0);
    gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, length, height, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
}

function buildDialBitmap(radius) {
    var b=Bitmap.createBitmap(radius*2, radius*2, Bitmap.Config.ARGB_8888);
    var canvas=new Canvas(b);

    var p=new Paint();
    p.setAntiAlias(true);

    // external shaded ring
    var colors=[0xff000000, 0xff000000, 0xffffffff, 0xff000000, 0x00000000];
    var positions=[0, 0.94, 0.95, 0.98, 1.0];
    p.setShader(new RadialGradient(radius, radius, radius, colors, positions, Shader.TileMode.CLAMP));
    canvas.drawCircle(radius, radius, radius, p);
    p.setShader(null);

    // build the inner decoration, using two symmetrical paths
    var pathl=new Path();
    pathl.moveTo(radius, radius/2);
    pathl.lineTo(radius+20, radius-20);
    pathl.lineTo(radius, radius);
    pathl.close();
    var pathr=new Path();
    pathr.moveTo(radius, radius/2);
    pathr.lineTo(radius-20, radius-20);
    pathr.lineTo(radius, radius);
    pathr.close();
    canvas.save();
    for(var i=0; i<4; i++) {
        canvas.rotate(i*90, radius, radius);
        p.setColor(0xff808080);
        canvas.drawPath(pathl, p);
        p.setColor(0xffffffff);
        canvas.drawPath(pathr, p);
    }
    canvas.restore();

    // draw medium graduations in white
    p.setColor(0xffffffff);
    p.setStrokeWidth(2);
    for(var i=0; i<360; i+=10) {
        canvas.save();
        canvas.rotate(i, radius, radius);
        canvas.drawLine(radius, radius*2, radius, 1.75*radius, p);
        canvas.restore();
    }


    // draw major graduations in red
    p.setColor(0xffff0000);
    p.setStrokeWidth(3);
    for(var i=0; i<360; i+=90) {
        canvas.save();
        canvas.rotate(i, radius, radius);
        canvas.drawLine(radius, radius*2, radius, 1.70*radius, p);
        canvas.restore();
    }

    // medium graduation texts
    p.setTextSize(24);
    p.setTextAlign(Paint.Align.CENTER);
    p.setColor(0xffffffff);
    for(var i=0; i<360; i+=30) {
        // do not draw 0/90/180/270
        if((i%90)!=0) {
            var a = -i*(Math.PI*2)/360;
            var x = (Math.sin(a)*0.7*radius+radius);
            var y = (Math.cos(a)*0.7*radius+radius);

            canvas.save();
            canvas.rotate(i, x, y);
            canvas.drawText(Integer.toString(i), x, y, p);
            canvas.restore();
        }
    }

    // draw N/O/S/E
    p.setTextSize(40);
    p.setColor(0xffff0000);
    for(var i=0; i<360; i+=90) {
        var a = i*(Math.PI*2)/360;
        var x = (Math.sin(a)*0.65*radius+radius);
        var y = (Math.cos(a)*0.65*radius+radius);

        canvas.save();
        canvas.rotate(-i, x, y);
        canvas.drawText(CARDINAL_POINTS[i/90], x, y, p);
        canvas.restore();
    }

    return b;
}

function buildDialTexture(gl) {
    var params = Array.newInstance(Integer.TYPE, 1);
    gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, params, 0);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[TEXTURE_DIAL]);
    gl.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 1);
    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);

    var radius=256;

    var b = buildDialBitmap(radius);

    //GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, b, 0);
    var bb=ByteBuffer.allocate(radius*2*radius*2*4);
    b.copyPixelsToBuffer(bb);
    bb.position(0);
    gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, radius*2, radius*2, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
}

function setDetailsLevel(detailsLevel) {
    if(detailsLevel!=mDetailsLevel) {
        mDetailsLevel=detailsLevel;
        mNeedObjectsUpdate=true;
    }
}

function setReversedRing(reversedRing) {
    if(reversedRing!=mReversedRing) {
        mReversedRing=reversedRing;
        mNeedTextureUpdate=true;
    }
}


var mAzimuth=0;
var mPitch=0;
var mRoll=0;

var compassRenderer = {
    getConfigSpec: function() {
        var configSpec = [
            EGL10.EGL_DEPTH_SIZE,   16,
            EGL10.EGL_NONE
        ];
        return configSpec;
    },

    onSurfaceChanged: function(gl, width, height) {
         gl.glViewport(0, 0, width, height);

         var ratio = width / height;
         gl.glMatrixMode(GL10.GL_PROJECTION);
         gl.glLoadIdentity();
         gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
    },

    onSurfaceCreated: function(gl, config) {
         gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

//      	 gl.glClearColorx(1<<16, 1<<16, 1<<16, 1<<16);
      	 gl.glClearColorx(0, 0, 0, 0);

         gl.glEnable(GL10.GL_CULL_FACE);
         gl.glShadeModel(GL10.GL_SMOOTH);
         gl.glEnable(GL10.GL_DEPTH_TEST);
         gl.glEnable(GL10.GL_NORMALIZE);
         gl.glEnable(GL10.GL_COLOR_MATERIAL);

         gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
         gl.glEnable(GL10.GL_BLEND);

         var lightAmbient = [ 0.2, 0.2, 0.2, 1.0 ];
         var lightDiffuse = [ 0.9, 0.9, 0.9, 1.0 ];
         var lightSpecular = [ 1.0, 1.0, 1.0, 1.0 ];
         var lightPosition = [ 0, 0, 3, 0.0 ];
         var lightDirection = [ 0, 0, -1 ];

         gl.glEnable(GL10.GL_LIGHTING);
         gl.glEnable(GL10.GL_LIGHT0);

         gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, lightAmbient, 0);
         gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, lightDiffuse, 0);
         gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, lightSpecular, 0);
         gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPosition, 0);
         gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPOT_DIRECTION, lightDirection, 0);
         gl.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_CUTOFF, 1.2);
         gl.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_EXPONENT, 20.0);

         buildTextures(gl);
    },

    onDrawFrame: function(gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glTranslatef(0, 0, -3);

        gl.glRotatef(mPitch+90,  1, 0, 0);
        gl.glRotatef(-mRoll, 0, 0, 1);
        gl.glRotatef(mAzimuth+180, 0, 1, 0);

        gl.glTranslatef(0, 0.7, 0);

        draw(gl);
    },

    setOrientation: function(azimuth, pitch, roll) {
    	mAzimuth=azimuth;
    	mPitch=pitch;
    	mRoll=roll;
    },

    setParameters: function(detailsLevel, reversedRing) {
    	setDetailsLevel(detailsLevel);
    	setReversedRing(reversedRing);
    }
}

compassRenderer.setParameters(2, false);

var context = item.getScreen().getContext();

/*********************************** SETUP THE COMPASS ITEM CLASS *********************************/

var compass = item.extend();
item.my.compass = compass;

compass.isRunning = function() {
    return this.data.running;
}

compass.start = function() {
    this.doStart();
    this.data.running = true;
}

compass.stop = function() {
    this.doStop();
    this.data.running = false;
}

compass.pause = function() {
    if(this.isRunning()) {
        this.doStop();
    }
}

compass.resume = function() {
    if(this.isRunning()) {
        this.doStart();
    }
}

compass.sensorManager = context.getSystemService(Context.SENSOR_SERVICE);

compass.orientationSensor = compass.sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

compass.sensorEventListener = {
    onSensorChanged: function(event) {
        if (event.sensor == compass.orientationSensor) {
            var azimuth = event.values[0];
//            TODO handle landscape mode based on screen orientation
//            if(isLandscapeDevice) {
//                azimuth -= 90;
//            }
            compassRenderer.setOrientation(azimuth, event.values[1], event.values[2]);
        }
    }
}

compass.doStart = function() {
    this.sensorManager["registerListener(android.hardware.SensorEventListener,android.hardware.Sensor,int)"](
        this.sensorEventListener,
        this.orientationSensor,
        SensorManager.SENSOR_DELAY_GAME
    );
}

compass.doStop = function() {
    this.sensorManager["unregisterListener(android.hardware.SensorEventListener,android.hardware.Sensor)"](
        this.sensorEventListener,
        this.orientationSensor
    );
}

compass.updateParameters = function() {
    compassRenderer.setParameters(this.data.detailsLevel, this.data.reversedRing);
}

var MY_TAG_NAME = "net.pierrox.lightning_launcher.llscript.compass";
compass.loadData = function() {
    var tag = this.getTag(MY_TAG_NAME);
    if(tag == null) {
        // no tag: this is a new counter, set some default data
        this.data = {
            running: false,
            detailsLevel: 2,
            reversedRing: true,
        }
    } else {
        this.data = JSON.parse(tag);
    }

    this.updateParameters();
}

compass.saveData = function() {
    this.setTag(MY_TAG_NAME, JSON.stringify(this.data));
}

compass.loadData();

/************************************** SETUP THE COMPASS VIEW ************************************/

var glSurfaceView = new GLSurfaceView(context);
glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
glSurfaceView.setRenderer(compassRenderer);
glSurfaceView.setVisibility(View.GONE);

// uncomment the code related to textView if you wish to display a message
//var textView = new TextView(context);
//textView.setText("Paused\n\nTap to start");
//textView.setGravity(Gravity.CENTER);
//textView.setTextColor(0xffffffff);
//textView.setShadowLayer(1, 0, 0, 0xff000000);

var imageView = new ImageView(context);
imageView.setImageBitmap(buildDialBitmap(256));

var fl = new FrameLayout(context);
fl.addView(glSurfaceView);
fl.addView(imageView);
//fl.addView(textView);

fl.setOnClickListener({
    onClick: function(v) {
        if(compass.isRunning()) {
            compass.stop();
            glSurfaceView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
//            textView.setVisibility(View.VISIBLE);
        } else {
            compass.start();
            glSurfaceView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
//            textView.setVisibility(View.GONE);
        }
    }
})

return fl;