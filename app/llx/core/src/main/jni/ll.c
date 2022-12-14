/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <android/native_window_jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

//#include "png.h"
//#include "jpeglib.h"

#define  LOG_TAG    "LL"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define ALPHA(color) ((color>>24)&0xff)
#define RED(color) ((color>>16)&0xff)
#define GREEN(color) ((color>>8)&0xff)
#define BLUE(color) (color&0xff)

#define SCALE_TYPE_CENTER 0
#define SCALE_TYPE_FIT 1
#define SCALE_TYPE_CENTER_CROP 2
#define SCALE_TYPE_CENTER_INSIDE 3

#define NO_KEY -1
typedef struct {
	int key;
	void* pixels;
	int width;
	int height;
	int stride;
} BitmapEntry;

// TODO use a hash table or similar
#define MAX_ENTRIES 10000
static BitmapEntry* bitmap_entries[MAX_ENTRIES];
//static int bitmap_entries_count=0;

static BitmapEntry* findBitmapEntry(int key) {
	for(int i=0; i<MAX_ENTRIES; i++) {
		BitmapEntry* be = bitmap_entries[i];
		if(be!=NULL && be->key==key) {
//		    LOGI("XXX findBitmapEntry key %d, %dx%d", be->key, be->width, be->height);
			return be;
		}
	}
//	LOGI("XXX findBitmapEntry %d not found", key);
	return NULL;
}

static void addBitmapEntry(int key, void* pixels, int width, int height, int stride, jboolean copy_pixels) {
//    LOGI("addBitmapEntry key %d, %dx%d", key, width, height);
	for(int i=0; i<MAX_ENTRIES; i++) {
		BitmapEntry* be = bitmap_entries[i];
		if(be == NULL) {
			be = malloc(sizeof(BitmapEntry));
			be->key = key;
			if(copy_pixels == JNI_TRUE) {
				be->pixels = malloc(stride*height);
				memcpy(be->pixels, pixels, stride*height);
			} else {
				be->pixels = pixels;
			}
			be->width = width;
			be->height = height;
			be->stride = stride;
			bitmap_entries[i] = be;
			return;
		}
	}
}

static void deleteBitmapEntry(int key) {
//    LOGI("XXX deleteBitmapEntry key %d", key);
	for(int i=0; i<MAX_ENTRIES; i++) {
		BitmapEntry* be = bitmap_entries[i];
		if(be!=NULL && be->key==key) {
//		    LOGI("    deleted %dx%d", be->width, be->height);
			free(be->pixels);
			free(be);
			bitmap_entries[i] = NULL;
			return;
		}
	}
}

static void clearBitmapEntries() {
    for(int i=0; i<MAX_ENTRIES; i++) {
		BitmapEntry* be = bitmap_entries[i];
		if(be!=NULL) {
			free(be->pixels);
			free(be);
			bitmap_entries[i] = NULL;
			return;
		}
	}
}

static inline unsigned SkAlpha255To256(char alpha) {
	return alpha + 1;
}
#define SkMulS16(x, y)  ((x) * (y))
#define SkAlphaMul(value, alpha256)     (SkMulS16(value, alpha256) >> 8)


static void drawBitmapEntryOnBuffer(BitmapEntry* be, ANativeWindow_Buffer *buffer) {
	void* ptr_in = be->pixels;
	void* ptr_out = buffer->bits;
	int w, h;

	if(be->width > buffer->width) {
		w = buffer->width;
		ptr_in += 4 * ((be->width - buffer->width) / 2);
	} else {
		w = be->width;
		ptr_out += 4 * ((buffer->width - be->width) / 2);
	}

	if(be->height > buffer->height) {
		h = buffer->height;
		ptr_in += be->stride * ((be->height - buffer->height) / 2);
	} else {
		h = be->height;
		ptr_out += buffer->stride * 4 * ((buffer->height - be->height) / 2);
	}

	int row_length = w * 4;
	for(int y=0; y<h; y++) {
		memcpy(ptr_out, ptr_in, row_length);
		ptr_in += be->stride;
		ptr_out += buffer->stride * 4;
	}
}

static void drawColorOnBuffer(int color, ANativeWindow_Buffer *buffer) {
	void* ptr_out = buffer->bits;

	int a = ALPHA(color);

	if(a == 255) {
		int r = RED(color);
		int g = GREEN(color);
		int b = BLUE(color);
		for(int y=0; y<buffer->height; y++) {
			char* out = (char*)ptr_out;
			for(int x=0; x<buffer->width; x++) {
				*out++ = r;
				*out++ = g;
				*out++ = b;
				*out++ = a;
			}
			ptr_out += buffer->stride * 4;
		}
	} else {
		int r = SkAlphaMul(RED(color), a);
		int g = SkAlphaMul(GREEN(color), a);
		int b = SkAlphaMul(BLUE(color), a);
		for(int y=0; y<buffer->height; y++) {
			char* out = (char*)ptr_out;
			for(int x=0; x<buffer->width; x++) {
				char o;

				// r
				o = *out;
				*out = r + (((255-a)*o)>>8);
				out++;

				// g
				o = *out;
				*out = g + (((255-a)*o)>>8);
				out++;

				// b
				o = *out;
				*out = b + (((255-a)*o)>>8);
				out++;

				// a
				o = *out;
				*out = a + (((255-a)*o)>>8);
				out++;
			}
			ptr_out += buffer->stride * 4;
		}
	}
}


void Java_net_pierrox_lightning_1launcher_views_NativeImage_nativeInit(JNIEnv *env, jclass clazz) {
	for(int i=0; i<MAX_ENTRIES; i++) {
		bitmap_entries[i] = NULL;
	}
}

jboolean Java_net_pierrox_lightning_1launcher_views_NativeImage_nativeHasImage(JNIEnv *env, jclass clazz, jint key) {
	return findBitmapEntry(key)==NULL ? JNI_FALSE : JNI_TRUE;
}

void Java_net_pierrox_lightning_1launcher_views_NativeImage_nativeDeleteImage(JNIEnv *env, jclass clazz, jint key) {
	deleteBitmapEntry(key);
}

void Java_net_pierrox_lightning_1launcher_views_NativeImage_nativeSetImage(JNIEnv *env, jclass clazz, jint key, jobject bitmap) {
	deleteBitmapEntry(key);

	AndroidBitmapInfo info;
	void* pixels;
	int ret;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return;
	}

	/*if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
        LOGE("Bitmap format is not RGB_565 !");
        return;
    }*/

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return;
	}

	addBitmapEntry(key, pixels, info.width, info.height, info.stride, JNI_TRUE);

	AndroidBitmap_unlockPixels(env, bitmap);
}

void Java_net_pierrox_lightning_1launcher_views_NativeImage_nativeDrawImageWithColorOnSurface(JNIEnv *env, jclass clazz, jint key, jint color, jint scaleType, jobject surface) {
	ANativeWindow* window = ANativeWindow_fromSurface(env, surface);

//LOGI("XXX nativeDrawImageWithColorOnSurface enter");
	if(window != NULL) {
        BitmapEntry* be = key==NO_KEY ? NULL : findBitmapEntry(key);
        if(be) {
            int window_width = ANativeWindow_getWidth(window);
            int window_height = ANativeWindow_getHeight(window);
            int width, height;
//            float sx, sy;
            switch(scaleType) {
            case SCALE_TYPE_CENTER:
                width = window_width;
                height = window_height;
                break;

            case SCALE_TYPE_FIT:
            default:
                width = be->width;
                height = be->height;
                break;

/*            case SCALE_TYPE_CENTER_CROP:
                sx = window_width / (float)be->width;
                sy = window_height / (float)be->height;
                LOGI("XXX CENTER_CROP %f %f", sx, sy);
                if(sy > sx) {
                    height = be->height;
                    width = be->width * window_width / window_height;
                } else {
                    width = be->width;
                    height = be->height * sx;
                }

                break;

            case SCALE_TYPE_CENTER_INSIDE:
                sx = window_width / (float)be->width;
                sy = window_height / (float)be->height;
                LOGI("XXX CENTER_INSIDE %f %f", sx, sy);
                if(sy > sx) {
                    width = be->width;
                    height = be->height * window_height / window_width;
                } else {
                    height = be->height;
                    width = window_width;
                }
                break;*/

            }
            ANativeWindow_setBuffersGeometry(window, width, height, WINDOW_FORMAT_RGBA_8888);
        }
		ANativeWindow_Buffer buffer;
		if (ANativeWindow_lock(window, &buffer, NULL) == 0) {
			memset(buffer.bits, 0, buffer.stride*buffer.height*4);

			int alpha = ALPHA(color);
			if(be && alpha!=255) {
				drawBitmapEntryOnBuffer(be, &buffer);
			}
			if(alpha!=0) {
				drawColorOnBuffer(color, &buffer);
			}

			ANativeWindow_unlockAndPost(window);
		}

		ANativeWindow_release(window);


	}
//	LOGI("XXX nativeDrawImageWithColorOnSurface exit");
}

JNIEXPORT jint JNICALL Java_net_pierrox_lightning_1launcher_views_NativeImage_nativeGetImageWidth(JNIEnv *env, jclass clazz, jint key) {
    BitmapEntry* be = findBitmapEntry(key);
    return be->width;
}

JNIEXPORT jint JNICALL Java_net_pierrox_lightning_1launcher_views_NativeImage_nativeGetImageHeight(JNIEnv *env, jclass clazz, jint key) {
    BitmapEntry* be = findBitmapEntry(key);
    return be->height;
}


JNIEXPORT void JNICALL Java_net_pierrox_lightning_1launcher_views_NativeImage_nativeLoadImage(JNIEnv *env, jclass clazz, jint key, jobject bitmap) {
    AndroidBitmapInfo info;
	void* pixels;
	int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
	}

//    LOGI("load image from native %d", key);

    BitmapEntry* be = findBitmapEntry(key);

    int stride = info.width*4;
	void *ptr_dst = pixels;
	void *ptr_src = be->pixels;
	int stride_src = info.stride;
	int stride_dst = be->stride;
	unsigned int y;
	for(y=0; y<info.height; y++) {
	    memcpy(ptr_dst, ptr_src, stride);
		ptr_src += stride_src;
		ptr_dst += stride_dst;
	}

    AndroidBitmap_unlockPixels(env, bitmap);
}

JNIEXPORT void JNICALL Java_net_pierrox_lightning_1launcher_views_NativeImage_nativeClearImages(JNIEnv *env, jclass clazz) {
    clearBitmapEntries();
}
