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

package net.pierrox.lightning_launcher.util;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import net.pierrox.lightning_launcher.activities.ScriptEditor;
import net.pierrox.lightning_launcher.data.FileUtils;


/** A bare minimum provider to prevent exposng file uris */
public class FileProvider extends ContentProvider {
	public static final Uri CONTENT_URI=Uri.parse("content://net.pierrox.lightning_launcher.files/");

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public boolean onCreate() {
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		String path = uri.getPath().substring(1);
		if(!path.startsWith(FileUtils.LL_TMP_DIR.getAbsolutePath()) && !path.startsWith(FileUtils.LL_EXT_DIR.getAbsolutePath())) {
			throw new IllegalArgumentException();
		}

		File f=new File(path);
		if (f.exists()) {
			ParcelFileDescriptor pfd=ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_WRITE);
			return pfd;
		}

		throw new FileNotFoundException(path);
	}

	public static Uri getUriForFile(File to) {
		return Uri.parse(CONTENT_URI + to.getAbsolutePath());
	}
}
