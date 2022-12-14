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

package net.pierrox.lightning_launcher.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.views.CropperView;
import net.pierrox.lightning_launcher_extreme.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageCropper extends ResourceWrapperActivity implements View.OnClickListener, CropperView.OnCropperViewEvent {
    public static final String INTENT_EXTRA_IMAGE = "i";
    public static final String INTENT_EXTRA_FULL_SIZE = "f";

    private File mSourceFile;
    private TextView mSelectionText;
    private CropperView mCropperView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.setTheme(this, Utils.APP_THEME_NO_ACTION_BAR);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.image_cropper);

        mSelectionText = (TextView) findViewById(R.id.sr);

        mCropperView = (CropperView) findViewById(R.id.cv);
        Bitmap bitmap;
        Intent intent = getIntent();
        String sourcePath = intent.getStringExtra(INTENT_EXTRA_IMAGE);
        mSourceFile = new File(sourcePath);
        if(intent.getBooleanExtra(INTENT_EXTRA_FULL_SIZE, false)) {
            bitmap = Utils.loadBitmap(mSourceFile, 0, 0, 0);
        } else {
            bitmap = Utils.loadScreenSizedBitmap(getWindowManager(), sourcePath);
        }
        mCropperView.setImageBitmap(bitmap);
        mCropperView.setOnCropperViewEvent(this);

        findViewById(R.id.ok).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.ok:
                final Rect r = mCropperView.getSelection();
                final Bitmap original_image = ((BitmapDrawable)mCropperView.getDrawable()).getBitmap();
                final File destinationFile = Utils.getTmpImageFile();

                if(r.left == 0 && r.top == 0 && r.width() == original_image.getWidth() && r.height() == original_image.getHeight()) {
                    boolean success;
                    // full image size: no cropping
                    if(mSourceFile.compareTo(destinationFile) == 0) {
                        // same file, nothing to do
                        success = true;
                    } else {
                        success = Utils.copyFileSafe(null, mSourceFile, destinationFile);
                    }
                    done(success);
                } else {
                    // need to crop
                    final ProgressDialog d = new ProgressDialog(this);
                    d.setMessage(getString(R.string.please_wait));
                    d.setCancelable(false);
                    d.show();

                    new AsyncTask<Void, Void, Boolean>() {

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            FileOutputStream fos = null;
                            try {
                                fos = new FileOutputStream(destinationFile);
                                // at the moment original image may not be the image read from the file since it can be downscaled
//                          if(r.left ==0 && r.top == 0 && original_image.getWidth() == r.width() && original_image.getHeight() == r.height() && original_image.getConfig() == Bitmap.Config.ARGB_8888) {
//                              original_image.compress(Bitmap.CompressFormat.PNG, 100, fos);
//                          } else {
                                Bitmap cropped_image = Bitmap.createBitmap(r.width(), r.height(), Bitmap.Config.ARGB_8888);
                                final Canvas canvas = new Canvas(cropped_image);
//                          canvas.translate(-r.left, -r.top);
                                canvas.drawBitmap(original_image, -r.left, -r.top, null);
                                cropped_image.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                cropped_image.recycle();
//                          }
                                return true;
                            } catch (Throwable t) {
                                Toast.makeText(ImageCropper.this, R.string.tr_eu, Toast.LENGTH_SHORT).show();
                                return false;
                            } finally {
                                if (fos != null) try {
                                    fos.close();
                                } catch (IOException e) {
                                }
                            }
                        }

                        @Override
                        protected void onPostExecute(Boolean success) {
                            d.dismiss();
                            done(success);
                        }
                    }.execute((Void) null);
                }

                break;

            case R.id.cancel:
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }

    private void done(boolean success) {
        setResult(success ? RESULT_OK : RESULT_CANCELED);
        if(!success) {
            Toast.makeText(this, R.string.tr_eu, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void onCropperViewSelectionChanged(Rect selection) {
        mSelectionText.setText(selection.toShortString()+" ("+selection.width()+"px x "+selection.height()+"px)");
    }

    @Override
    public void onCropperViewClick() {
        final Rect selection = mCropperView.getSelection();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View content = getLayoutInflater().inflate(R.layout.dialog_rect, null);
        builder.setTitle(getString(R.string.rect));
        builder.setView(content);
        ((TextView)content.findViewById(R.id.rect_l)).setText(getString(R.string.left));
        ((TextView)content.findViewById(R.id.rect_t)).setText(getString(R.string.top));
        ((TextView)content.findViewById(R.id.rect_r)).setText(getString(R.string.right));
        ((TextView)content.findViewById(R.id.rect_b)).setText(getString(R.string.bottom));
        ((TextView)content.findViewById(R.id.rect_w)).setText(getString(R.string.gb_w).toLowerCase());
        ((TextView)content.findViewById(R.id.rect_h)).setText(getString(R.string.gb_h).toLowerCase());
        final EditText el = (EditText)content.findViewById(R.id.rect_el);
        final EditText et = (EditText)content.findViewById(R.id.rect_et);
        final EditText er = (EditText)content.findViewById(R.id.rect_er);
        final EditText eb = (EditText)content.findViewById(R.id.rect_eb);
        final EditText ew = (EditText)content.findViewById(R.id.rect_ew);
        final EditText eh = (EditText)content.findViewById(R.id.rect_eh);
        el.setText(String.valueOf(selection.left));
        et.setText(String.valueOf(selection.top));
        er.setText(String.valueOf(selection.right));
        eb.setText(String.valueOf(selection.bottom));
        ew.setText(String.valueOf(selection.width()));
        eh.setText(String.valueOf(selection.height()));

        TextWatcher l_or_r_watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int l = Integer.parseInt(el.getText().toString());
                    int r = Integer.parseInt(er.getText().toString());
                    int w = Integer.parseInt(ew.getText().toString());
                    int new_w = r - l;
                    if(new_w != w) {
                        ew.setText(String.valueOf(new_w));
                    }
                } catch (NumberFormatException e) {

                }
            }
        };

        TextWatcher t_or_b_watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int t = Integer.parseInt(et.getText().toString());
                    int b = Integer.parseInt(eb.getText().toString());
                    int h = Integer.parseInt(eh.getText().toString());
                    int new_h = b - t;
                    if(new_h != h) {
                        eh.setText(String.valueOf(new_h));
                    }
                } catch (NumberFormatException e) {

                }
            }
        };

        TextWatcher w_watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int l = Integer.parseInt(el.getText().toString());
                    int r = Integer.parseInt(er.getText().toString());
                    int w = Integer.parseInt(ew.getText().toString());
                    int new_r = l + w;
                    if(new_r != r) {
                        er.setText(String.valueOf(new_r));
                    }
                } catch (NumberFormatException e) {

                }
            }
        };

        TextWatcher h_watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int t = Integer.parseInt(et.getText().toString());
                    int h = Integer.parseInt(eh.getText().toString());
                    int b = Integer.parseInt(eb.getText().toString());
                    int new_b = t + h;
                    if(new_b != b) {
                        eb.setText(String.valueOf(new_b));
                    }
                } catch (NumberFormatException e) {

                }
            }
        };

        el.addTextChangedListener(l_or_r_watcher);
        et.addTextChangedListener(t_or_b_watcher);
        er.addTextChangedListener(l_or_r_watcher);
        eb.addTextChangedListener(t_or_b_watcher);
        ew.addTextChangedListener(w_watcher);
        eh.addTextChangedListener(h_watcher);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    int l = Integer.parseInt(el.getText().toString());
                    int t = Integer.parseInt(et.getText().toString());
                    int r = Integer.parseInt(er.getText().toString());
                    int b = Integer.parseInt(eb.getText().toString());
                    selection.set(l, t, r, b);
                    mCropperView.setSelection(selection);
                } catch(NumberFormatException e) {

                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    public static void startActivity(Activity from, File image, int requestCode) {
        final Intent intent = new Intent(from, ImageCropper.class);
        intent.putExtra(INTENT_EXTRA_IMAGE, image.getAbsolutePath());
        from.startActivityForResult(intent, requestCode);
    }
}
