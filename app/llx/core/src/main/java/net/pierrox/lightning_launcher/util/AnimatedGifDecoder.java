/*
 * File downloaded from:
 *	https://code.google.com/p/giffiledecoder/
 * Based on GifDecoder.java at:
 *	http://code.google.com/p/android-gifview/
 *	http://code.google.com/p/animated-gifs-in-android/
 * GIF specification:
 *	http://www.w3.org/Graphics/GIF/spec-gif89a.txt
 *
 * Contributors: Nick Frolov, Aleksandr Shardakov
 */

package net.pierrox.lightning_launcher.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class AnimatedGifDecoder
{
	private static final int MIN_DELAY = 20;
	private static final int ENFORCED_DELAY = 100;
	 
	private static final int DISPOSE_NONE		  = 0;
	//private static final int DISPOSE_LEAVE		 = 1;
	private static final int DISPOSE_BACKGROUND  = 2;
	private static final int DISPOSE_PREVIOUS	 = 3;
	 
	private final File file;
	private InputStream in;
	private long headerSize;
	private boolean hasFrame;
	private int loopIndex;
	private int frameIndex;
	
	private int width; // full image width
	private int height; // full image height
	private int loopCount; // iterations; 0 = repeat forever
	private boolean hasGct;
	private final int[] gct = new int[256]; // global color table
	private boolean hasLct;
	private final int[] lct = new int[256]; // local color table
	private int[] act; // active color table
	private boolean interlace; // interlace flag
	private int ix, iy, iw, ih; // current image rectangle
	private int dispose; 
	private boolean transparency; // use transparent color
	private int delay; // delay in milliseconds
	private int transIndex; // transparent color index

	private int[] baseImage;
	private int[] tempImage;
	
	private final byte[] ctbuf = new byte[256*3]; // color table reading buffer
	
	// Block reader data
	private final byte[] block = new byte[256+1]; // current data block
	
	// LZW decoder pixel stack size
	private static final int MAX_STACK_SIZE = 4096; 
	// LZW decoder working arrays
	private final int[] ptable = new int[MAX_STACK_SIZE+1];
	private final int[] ltable = new int[MAX_STACK_SIZE+1];
	private final int[] ltableTemplate = new int[MAX_STACK_SIZE+1];
	private int[] pixels;
	
	private final IOException EX_EOF = new IOException("Unexpected end of file");
	private final IOException EX_IFF = new IOException("Incorrect file format");
	
	public AnimatedGifDecoder(File file) {
		this.file = file;
	}

	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}

	public int getDelay() {
		return delay;
	}

	public int getLoopCount() {
		return loopCount;
	}

	public void start() throws IOException	{
		reopenStream();
		
		// file header
		String id = "";
		for (int i = 0; i < 6; i++)
			id += (char) read();
		if (!id.startsWith("GIF"))
			throw EX_IFF;

		// logical screen size
		width = readShort();
		height = readShort();
		
		// packed fields: 
		// 1 : global color table flag, 
		// 2-4 : color resolution
		// 5 : gct sort flag
		// 6-8 : gct size
		int packed = read();
		hasGct = (packed & 0x80) != 0; 
		int gctSize = 2 << (packed & 0x07); 
		
		read(); // background color index, not used
		read(); // pixel aspect ratio, not used

		headerSize = 13;
		
		if (hasGct) {
			readColorTable(gct, gctSize);
			headerSize += 3 * gctSize;
		}
		
		// Init fields
		baseImage = new int[width * height];

		for (int i = 0; i < ltableTemplate.length; i++)
			ltableTemplate[i] = 1;
		
		loopCount = 1;
		loopIndex = 0;
		resetLoop();
	}
	
	private void reopenStream() throws IOException	{
		if (in != null)
			in.close();
		in = new BufferedInputStream(new FileInputStream(file), 32768);
	}
	
	public void stop() {
		try { if (in != null) in.close(); } catch (IOException ex) {}
		in = null;
	}
	
	private void readColorTable(int[] tab, int ncolors) throws IOException {
		int nbytes = 3 * ncolors;
		int n = in.read(ctbuf, 0, nbytes);
		if (n < nbytes) 
			throw EX_IFF;
		
		int i = 0;
		int j = 0;
		while (i < ncolors) {
			int r = ((int) ctbuf[j++]) & 0xff;
			int g = ((int) ctbuf[j++]) & 0xff;
			int b = ((int) ctbuf[j++]) & 0xff;
			
			// Bitmap.copyPixelsFromBuffer with ARGB_8888 really expects ABGR, so doing that.
			// Note that Bitmap.createBitmap takes ARGB.
			tab[i++] = 0xff000000 | (b << 16) | (g << 8) | r;
		}
	}
	
	private void resetLoop() {
		hasFrame = true;
		frameIndex = 0;
		resetFrame();
		Arrays.fill(baseImage, 0);
	}
	
	public boolean hasFrame() {
		return (hasFrame && (loopCount == 0 || loopIndex < loopCount));
	}
	
	public int[] readFrame() throws IOException {
		while (hasFrame) {
			int code = read();
			switch (code) {
				case 0x2C: // image 
					return readImage();
				case 0x21: // extension
					readExtension();
					break;
				case 0x3b: // terminator
					hasFrame = false;
					break;
				case 0x00: // bad byte, but keep going and see what happens 
					break;
				default:
					throw EX_IFF;
			}
		}

		return null;
		
//		// completed all images
//		if (frameIndex == 0)
//			throw new IOException("No GIF frames in file");
//
//		loopIndex++;
//		if (loopCount != 0 && loopIndex >= loopCount)
//			return null;
//
//		resetLoop();
//
//		// rewind file
//		reopenStream();
//		in.skip(headerSize);
//
//		// recursion - read first frame
//		return readFrame();
	}

	private void readExtension() throws IOException {
		int code = read();
		switch (code) {
			case 0xf9: // graphics control extension
				readGraphicControlExt();
				break;
			case 0xff: // application extension
				readBlock();
				String app = "";
				for (int i = 0; i < 11; i++) {
					app += (char) block[i];
				}
				if (app.equals("NETSCAPE2.0")) {
					readNetscapeExt();
				} else {
					skip(); // don't care
				}
				break;
			case 0xfe:// comment extension
				skip();
				break;
			case 0x01:// plain text extension
				skip();
				break;
			default: // uninteresting extension
				skip();
		}
	}
	
	private void readNetscapeExt() throws IOException {
		while (readBlock()>0){
			if (block[0] == 1) {
				// loop count 
				// This is sometimes ignored, and sometimes browsers add one more 
				// loop for some reason. I'm gonna do the specified number of loops.
				int b1 = ((int) block[1]) & 0xff;
				int b2 = ((int) block[2]) & 0xff;
				loopCount = (b2 << 8) | b1;
			}
		}
	}

	private void resetFrame() {
		dispose = DISPOSE_NONE;
		transparency = false;
		delay = ENFORCED_DELAY;
		transIndex = 0;
	}
	
	private void readGraphicControlExt() throws IOException {
		read(); // block size
		int packed = read(); // packed fields
		dispose = (packed & 0x1c) >> 2; // disposal method
		if (dispose != DISPOSE_BACKGROUND && dispose != DISPOSE_PREVIOUS)
			dispose = DISPOSE_NONE;
		transparency = (packed & 1) != 0;
		delay = readShort() * 10; // delay in milliseconds
		if (delay < MIN_DELAY)
			delay = ENFORCED_DELAY;
		transIndex = read(); // transparent color index
		read(); // block terminator
	}
	
	private int[] readImage() throws IOException {
		// (sub)image position & size
		ix = readShort(); 
		iy = readShort();
		iw = readShort();
		ih = readShort();
		
		// packed fields
		// 1 -	local color table flag
		// 2 -	interlace flag
		// 3 -	lct sorted
		// 4-5 - reserved 
		// 6-8 - lct size
		int packed = read();
		hasLct = (packed & 0x80) != 0; 
		int lctSize = 2 << (packed & 0x07); 
		interlace = (packed & 0x40) != 0;
		
		if (hasLct) {
			readColorTable(lct, lctSize);
			act = lct; // make local table active
		} else {
			if (!hasGct) 
				throw EX_IFF;
			act = gct; // make global table active
		}

		// image data
		if (transparency) {
			int c = act[transIndex];
			act[transIndex] = 0;
			decodeBitmapData();
			act[transIndex] = c;
		} else {
			decodeBitmapData();
		}
		skip();
		
		// draw image
		int[] image = drawImage(); 
		frameIndex++;

		return image;
	}

	private void decodeBitmapData() throws IOException {
		final int npix = iw * ih;
		if ((pixels == null) || (pixels.length < npix))
			pixels = new int[npix]; // allocate new pixel array
		
		final byte[] _block = block;
		final int[] _ptable = ptable;
		final int[] _ltable = ltable;
		final int[] _pixels = pixels;
		final int[] _act = act;

		// Initialize GIF data stream decoder.
		final int data_size = read();
		final int clear = 1 << data_size;
		final int end_of_information = clear + 1;
		int available = clear + 1;
		int code_size = data_size + 1;
		int code_mask = (1 << code_size) - 1;

		int bits, code, old_code, count, datum, bi, pi, old_pi;

		System.arraycopy(ltableTemplate, 0, _ltable, 0, clear);

		// Decode GIF pixel stream.		
		datum = bits = count = code = old_code = pi = old_pi = bi = 0;
		while (pi < npix) {
			while (bits < code_size) {
				// Load bytes until there are enough bits for a code.
				if (count == 0) {
					// Read a new data block.
					count = readBlock();
					if (count==0)
						break;
					bi = 0;
				}
				datum |= (_block[bi]&0xff)<<bits;
				bits += 8;
				bi++;
				count--;
			}
			// Get the next code.
			code = datum & code_mask;
			datum >>= code_size;
			bits -= code_size;
			// Interpret the code
			final boolean fl = code<available; 
			if (!fl){
				old_pi=pi;
				if (old_code>clear){
					final int p = _ptable[old_code];
					final int len = _ltable[old_code];
					for (int i=0; i<len; i++)
						_pixels[pi++]=_pixels[p+i];							
				} else
					_pixels[pi++]=_act[old_code];
				_pixels[pi++]=_pixels[old_pi];				
			}
			if (available<MAX_STACK_SIZE){
			   _ptable[available]=old_pi;
			   _ltable[available]=_ltable[old_code]+1;
			}
			available++;						
			if (available>code_mask && available<MAX_STACK_SIZE) {
				code_size++;
				code_mask |= available;
			}
			if (fl){
				if (code==clear){
					// Reset decoder.
					code_size = data_size + 1;
					code_mask = (1 << code_size) - 1;
					available = clear + 1;
					old_code = 0;
				} else if (code==end_of_information){
					// Stop decode
					break;
				} else {
					old_pi=pi;
					if (code>clear){
						final int p = _ptable[code];
						final int len = _ltable[code];
						for (int i=0; i<len; i++)
							_pixels[pi++]=_pixels[p+i];							
					} else
						_pixels[pi++]=_act[code];
				}
			}
			old_code = code;	
		}
		for (int i = pi; i < npix; i++)
			_pixels[i] = 0; // clear missing pixels
	}

	private int[] drawImage() {
		int[] dest;
		if (dispose == DISPOSE_NONE) {
			dest = baseImage;
		} else {
			if (tempImage == null)
				tempImage = new int[width * height];
			System.arraycopy(baseImage, 0, tempImage, 0, baseImage.length);
			dest = tempImage;
		}

		if (!interlace && dispose != DISPOSE_BACKGROUND 
				&& ix == 0 && iy == 0 && iw == width && ih == height)
			drawImageFaster(dest);
		else
			drawImageRegular(dest);

		return dest;
	}

	private void drawImageFaster(final int[] dest) {
		final int[] _pixels = pixels;
		for (int i = 0; i < dest.length; i++) {
			final int c = _pixels[i];
			if (c != 0) 
				dest[i] = c;
		}
	}
	
	private void drawImageRegular(final int[] dest) {
		final int[] _pixels = pixels;
		int pass = 1;
		int inc = 8;
		int iline = 0;
		for (int i = 0; i < ih; i++) {
			int line = i;
			if (interlace) {
				if (iline >= ih) {
					pass++;
					switch (pass) {
					case 2:
						iline = 4;
						break;
					case 3:
						iline = 2;
						inc = 4;
						break;
					case 4:
						iline = 1;
						inc = 2;
						break;
					default:
						break;
					}
				}
				line = iline;
				iline += inc;
			}
			line += iy;
			if (line < height) {
				int k = line * width;
				int dx = k + ix; // start of line in dest
				int dlim = dx + iw; // end of dest line
				if ((k + width) < dlim) {
					dlim = k + width; // past dest edge
				}
				int sx = i * iw; // start of line in source
				while (dx < dlim) {
					// map color and insert in image
					final int c = _pixels[sx++];
					if (c != 0) 
						dest[dx] = c;
					
					// if will be disposing to background, also modify canvas
					if (dispose == DISPOSE_BACKGROUND)
						baseImage[dx] = 0; 					
					dx++;
				}
			}
		}
	}
	
	// Reads a single byte from the input stream.
	private int read() throws IOException {
		int res = in.read();
		if (res==-1)
			throw EX_EOF;
		return res;
	}

	// Reads next 16-bit value, LSB first
	private int readShort() throws IOException {
		final int a = in.read();
		if (a==-1)
			throw EX_EOF;
		final int b = in.read();
		if (b==-1)
			throw EX_EOF;
		return a | (b << 8);
	}
	
	// Reads next variable length block from input.
	private int readBlock() throws IOException {
		final int size = in.read();
		if (size == -1)
			throw EX_EOF;
		int n = 0;
		while (n < size) {
			final int count = in.read(block, n, size - n);
			if (count == -1)
				throw EX_EOF;
			n += count;
		}
		return size;
	}

	// Skips variable length blocks up to and including next zero length block.
	private void skip() throws IOException {
		while (readBlock()>0);
	}
}

