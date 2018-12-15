package net.pierrox.lightning_launcher.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.script.ScriptManager;
import net.pierrox.lightning_launcher_extreme.R;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class FileAndDirectoryPickerDialog extends AlertDialog implements DialogInterface.OnClickListener, View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    public interface FileAndDirectoryPickerDialogListener {
        void onFileSelected(File file, File newDirectory);
    }

    private boolean mForDirectory;
    private boolean mForFont;
    private boolean mMergeSingleDirectories;
    private ScriptManager mScriptManager;
    private File mRootDirectory;
    private FileAndDirectoryPickerDialogListener mListener;

    private Button mUpButton;
    private ListView mFileListView;
    private TextView mCurrentPathView;
    private TextView mEmptyView;
    private File mCurrentDirectory;
    private FileFilter mFileFilter;
    private String[] mExtensions;

    private Bitmap mDirectoryIcon;
    private Bitmap mFileIcon;

    public static void showForFont(Context context, File initialDirectory, FileAndDirectoryPickerDialogListener listener) {
        String[] ttfExtensions = {".ttf", ".otf"};
        File rootDirectory = new File("/");
        if(initialDirectory == null) {
            initialDirectory = Environment.getExternalStorageDirectory();
        }
        new FileAndDirectoryPickerDialog(context, false, true, initialDirectory, rootDirectory, ttfExtensions, null, listener).show();
    }

    public static void showForScriptDirectory(Context context, File initialDirectory, ScriptManager scriptManager, FileAndDirectoryPickerDialogListener listener) {
        File rootDirectory = scriptManager.getScriptsDir();
        if(initialDirectory == null) {
            initialDirectory = rootDirectory;
        }
        new FileAndDirectoryPickerDialog(context, true, false, initialDirectory, rootDirectory, null, scriptManager, listener).show();
    }

    private FileAndDirectoryPickerDialog(Context context, boolean forDirectory, boolean forFont, File initialDirectory, File rootDirectory, String[] extensions, ScriptManager scriptManager, FileAndDirectoryPickerDialogListener listener) {
        super(context);

        mForDirectory = forDirectory;
        mMergeSingleDirectories = forDirectory;
        mForFont = forFont;
        mCurrentDirectory = initialDirectory;
        mRootDirectory = rootDirectory;
        mScriptManager = scriptManager;
        mListener = listener;

        mExtensions = extensions;

        Resources resources = context.getResources();
        int fgColor = resources.getColor(R.color.color_primary);// )context.getClass() ==Dashboard.class ? Color.BLACK : Color.LTGRAY;
        int size = resources.getDimensionPixelSize(R.dimen.list_item_icon_size);
        mDirectoryIcon = Utils.createIconFromText(size, ".", fgColor);
        mFileIcon = Utils.createIconFromText(size, "/", fgColor);

        if(mForDirectory) {
//            mFileFilter = new FileFilter() {
//                @Override
//                public boolean accept(File pathname) {
//                    return pathname.isDirectory();
//                }
//            };
        } else {
            mFileFilter = new FileFilter() {
                @SuppressLint("DefaultLocale")
                @Override
                public boolean accept(File pathname) {
                    if (mExtensions == null || mExtensions.length == 0) {
                        return true;
                    }
                    if (pathname.isDirectory()) return true;
                    return matchExtension(pathname.getName());
                }
            };
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        View view = getLayoutInflater().inflate(R.layout.file_dir_picker_dialog, null);
        setView(view);

        mUpButton = (Button) view.findViewById(R.id.up);
        mUpButton.setText(R.string.file_picker_activity_up);
        mUpButton.setOnClickListener(this);

        mCurrentPathView = (TextView) view.findViewById(R.id.path);

        mFileListView = (ListView) view.findViewById(android.R.id.list);
        mFileListView.setOnItemClickListener(this);
        mFileListView.setOnItemLongClickListener(this);

        mEmptyView = (TextView) view.findViewById(R.id.empty);
        mEmptyView.setText(R.string.nfh);

        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), this);
        if(mForDirectory) {
            setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), this);
        }

        gotoDirectory(mCurrentDirectory);

        super.onCreate(savedInstanceState);

        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        ViewGroup content = (ViewGroup) getWindow().findViewById(android.R.id.content);
        View child = content.getChildAt(0);
        child.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        child.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        child.requestLayout();
        child = content.findViewById(android.R.id.custom);
        child.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        child.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        child.requestLayout();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == BUTTON_POSITIVE) {
            mListener.onFileSelected(mCurrentDirectory, mCurrentDirectory);
            dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.up) {
            // remove symlinks to ensure that it's not possible to go back elsewhere
            String canonicalCurrent, canonicalRoot;
            try {
                canonicalCurrent = mCurrentDirectory.getCanonicalPath();
                canonicalRoot = mRootDirectory.getCanonicalPath();
            } catch(IOException e) {
                canonicalCurrent = "";
                canonicalRoot = "";
            }

            if(mMergeSingleDirectories) {
                while (!canonicalCurrent.equals(canonicalRoot)) {
                    File parent = mCurrentDirectory.getParentFile();
                    if (parent != null) {
                        mCurrentDirectory = parent;
                        try {
                            canonicalCurrent = mCurrentDirectory.getCanonicalPath();
                        } catch (IOException e) {
                            break;
                        }

                        File[] files = mCurrentDirectory.listFiles();
                        if (files.length > 1) {
                            gotoDirectory(parent);
                        }
                    }

                }
            } else {
                if(!canonicalCurrent.equals(canonicalRoot)) {
                    File parent = mCurrentDirectory.getParentFile();
                    if(parent != null) {
                        gotoDirectory(parent);
                    }
                }
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File file = (File) mFileListView.getAdapter().getItem(position);
        if(file.isDirectory()) {
            gotoDirectory(file);
        } else {
            if(!mForDirectory) {
                mListener.onFileSelected(file, mCurrentDirectory);
                dismiss();
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        File file = (File) mFileListView.getAdapter().getItem(i);
        if(!file.isDirectory()) {
            if(mForFont) {
                showDeleteFontDialog(file);
            } else {
                showDeleteFileDialog(file);
            }
            return true;
        } else {
            return false;
        }
    }

    private void showDeleteFileDialog(final File file) {
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.delete_file);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                file.delete();
                gotoDirectory(mCurrentDirectory);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    private void showDeleteFontDialog(final File file) {
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        EditText preview = new EditText(context);
        preview.setTextSize(30);
        preview.setText("abcdefghijklmnopqrstuvwxyz\nABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789");
        preview.setTypeface(Typeface.createFromFile(file));
        builder.setView(preview);
        builder.setPositiveButton(R.string.delete_file, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDeleteFileDialog(file);
            }
        });
        builder.setNegativeButton(android.R.string.ok, null);
        builder.create().show();
    }

    private boolean matchExtension(String name) {
        if(mExtensions == null) {
            return true;
        }
        String lname = name.toLowerCase();
        for(String e : mExtensions) {
            if(lname.endsWith(e)) {
                return true;
            }
        }
        return false;
    }

    private void gotoDirectory(File dir) {
        mCurrentDirectory = dir;

        File[] files = mCurrentDirectory.listFiles(mFileFilter);
        if(files != null) {
            if(mMergeSingleDirectories) {
                for (int n=files.length-1; n>=0; n--) {
                    File d = files[n];
                    for(;;) {
                        File[] subDirs = d.listFiles();
                        if(subDirs != null && subDirs.length == 1 && subDirs[0].isDirectory()) {
                            d = subDirs[0];
                        } else {
                            break;
                        }
                    }
                    files[n] = d;
                }
            }

            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File arg0, File arg1) {
                    if(arg0.isDirectory() && !arg1.isDirectory()) return -1;
                    if(arg1.isDirectory() && !arg0.isDirectory()) return 1;
                    return arg0.getName().compareTo(arg1.getName());
                }
            });
        } else {
            files = new File[0];
        }
        ArrayList<File> items = new ArrayList<>(Arrays.asList(files));

        mFileListView.setAdapter(new FileAdapter(getContext(), items));
        String rootPath = mRootDirectory.getAbsolutePath();
        if(!rootPath.endsWith("/")) {
            rootPath += "/";
        }
        String currentPath = mCurrentDirectory.getAbsolutePath();
        if(!currentPath.endsWith("/")) {
            currentPath += "/";
        }
        String path = "/" + currentPath.substring(rootPath.length(), currentPath.length());

        /*
        /  / -> /
        / /a -> /a
        /scripts -> /
        /scripts/a -> /a
        */
        mCurrentPathView.setText(path);
        mEmptyView.setVisibility(files.length==0 ? View.VISIBLE : View.GONE);
    }

    private static final String FONT_ITEM_PREVIEW = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private class FileAdapter extends ArrayAdapter<File> {
        private HashMap<File,Typeface> mTypefaces = new HashMap<>();

        public FileAdapter(Context context, List<File> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.two_lines_list_item, null);
            }

            File f = getItem(position);
            CheckedTextView line1 = (CheckedTextView) convertView.findViewById(android.R.id.text1);
            TextView line2 = (TextView) convertView.findViewById(android.R.id.text2);
            String text1, text2 = null;
            Typeface typeface = Typeface.DEFAULT;
            String name = f.getName();
            if(f.isDirectory()) {
                int length = mCurrentDirectory.getAbsolutePath().length();
                if(length > 1) {
                    // different from "/"
                    length++;
                }
                text1 = f.getAbsolutePath().substring(length);
                Utils.setEnabledStateOnViews(convertView, true);
            } else {
                if(mScriptManager != null) {
                    text1 = null;
                    try {
                        int id = Integer.parseInt(name);
                        Script script = mScriptManager.getOrLoadScript(id);
                        text1 = script.toString();
                    } catch (NumberFormatException e) {
                        // pass
                    }
                    if(text1 == null) {
                        text1 = name;
                    }
                } else {
                    text1 = name;
                    if (matchExtension(name)) {
                        Typeface font = mTypefaces.get(f);
                        if (font == null) {
                            try {
                                font = Typeface.createFromFile(f);
                                mTypefaces.put(f, font);
                            } catch (Throwable t) {
                                // pass
                            }
                        }
                        if (font != null) {
                            typeface = font;
                            text2 = FONT_ITEM_PREVIEW;
                        }
                    }
                }
                Utils.setEnabledStateOnViews(convertView, !mForDirectory);
            }
            if(text2 == null) {
                line2.setVisibility(View.GONE);
            } else {
                line2.setVisibility(View.VISIBLE);
                line2.setTypeface(typeface);
                line2.setText(text2);
            }
            line1.setText(text1);

            ((ImageView)convertView.findViewById(android.R.id.icon)).setImageBitmap(f.isDirectory() ? mDirectoryIcon : mFileIcon);


            return convertView;
        }
    }
}
