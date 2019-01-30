package net.pierrox.lightning_launcher.util;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import net.pierrox.lightning_launcher.activities.ScriptEditor;


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
		File f=new File(uri.getPath());

		if (f.exists()) {
			ParcelFileDescriptor pfd=ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_WRITE);
			return pfd;
		}

		throw new FileNotFoundException(uri.getPath());
	}

	public static Uri getUriForFile(File to) {
		return Uri.parse(CONTENT_URI + to.getAbsolutePath());
	}
}
