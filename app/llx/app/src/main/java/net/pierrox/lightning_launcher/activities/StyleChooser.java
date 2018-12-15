package net.pierrox.lightning_launcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.JsonFields;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher_extreme.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class StyleChooser extends ResourceWrapperListActivity implements OnItemLongClickListener {
	public static String INTENT_EXTRA_NEW="a";
	public static String INTENT_EXTRA_PATH="b";
	
	private static final int DIALOG_CONFIRM_DELETE=1;
	
	private ArrayList<StyleEntry> mStyles;
	private File mSelectedStyleFile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Utils.setTheme(this, Utils.APP_THEME);

		super.onCreate(savedInstanceState);
		
		getListView().setOnItemLongClickListener(this);
		
		loadThemes();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String path;
        StyleEntry se = mStyles.get(position);
        if(se.file == null) {
            File themes_dir=StyleChooser.getThemesDir(this);
            path = themes_dir.getAbsolutePath()+File.separator+new Date().getTime();
        } else {
            path = se.file.getAbsolutePath();
        }
		Intent data=new Intent();
		data.putExtra(INTENT_EXTRA_PATH, path);
		data.putExtra(INTENT_EXTRA_NEW, position==0);
		setResult(RESULT_OK, data);
		finish();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		mSelectedStyleFile=mStyles.get(position).file;
        if(mSelectedStyleFile != null) {
            showDialog(DIALOG_CONFIRM_DELETE);
        }
        return true;
    }
	
	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder;
		switch(id) {
		case DIALOG_CONFIRM_DELETE:
			builder=new AlertDialog.Builder(this);
			builder.setTitle(R.string.delete_style);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					mSelectedStyleFile.delete();
					mSelectedStyleFile=null;
					loadThemes();
				}
			});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();
		}
		
		return super.onCreateDialog(id);
	}

	public static File getThemesDir(Context context) {
		File themes_dir=FileUtils.getStylesDir(LLApp.get().getAppEngine().getBaseDir());
		if(!themes_dir.exists()) {
			themes_dir.mkdir();

            PageConfig page_config;

            page_config=new PageConfig();
            installBuiltinTheme(context, themes_dir, page_config, R.string.style_s1, "s1");

            page_config=new PageConfig();
            page_config.gridPRowNum = page_config.gridLRowNum = 8;
            page_config.defaultShortcutConfig.iconScale = 0.7f;
            page_config.defaultShortcutConfig.iconReflection = false;
            page_config.defaultShortcutConfig.labelFontSize = 9;
            page_config.defaultShortcutConfig.labelVsIconPosition = ShortcutConfig.LabelVsIconPosition.BOTTOM;
            installBuiltinTheme(context, themes_dir, page_config, R.string.style_s2, "s2");

            page_config=new PageConfig();
            page_config.gridPRowNum = page_config.gridLRowNum = 4;
            page_config.gridPColumnNum = page_config.gridLColumnNum = 2;
            page_config.defaultShortcutConfig.labelMaxLines = 2;
            page_config.defaultShortcutConfig.labelShadowRadius = 2;
            page_config.defaultShortcutConfig.labelVsIconPosition = ShortcutConfig.LabelVsIconPosition.BOTTOM;
            page_config.defaultShortcutConfig.iconScale = 1.5f;
            page_config.defaultShortcutConfig.iconReflection = false;
            page_config.defaultShortcutConfig.labelFontSize = 22;
            installBuiltinTheme(context, themes_dir, page_config, R.string.style_s3, "s3");

            page_config=new PageConfig();
            page_config.gridPColumnNum = page_config.gridLColumnNum = 1;
            page_config.defaultShortcutConfig.iconVisibility=false;
            page_config.defaultItemConfig.box=new Box();
            page_config.defaultItemConfig.box.ah=Box.AlignH.RIGHT;
            page_config.defaultShortcutConfig.labelFontSize=38;
            installBuiltinTheme(context, themes_dir, page_config, R.string.style_s9, "s9");

            page_config=new PageConfig();
            page_config.gridPRowNum = page_config.gridLRowNum = 7;
            page_config.gridPColumnNum = page_config.gridLColumnNum = 1;
            page_config.defaultItemConfig.box_s = "10::10::::::::10:::::-1946157057::::::::::::RIGHT::";
            page_config.gridLayoutModeHorizontalLineColor = 16777215;
            page_config.defaultShortcutConfig.labelVsIconMargin = 10;
            page_config.defaultShortcutConfig.iconReflection = false;
            page_config.defaultShortcutConfig.labelFontSize = 24;
            page_config.defaultShortcutConfig.labelVsIconPosition = ShortcutConfig.LabelVsIconPosition.LEFT;
            installBuiltinTheme(context, themes_dir, page_config, R.string.style_s4, "s4");

            page_config=new PageConfig();
            page_config.gridPRowNum = page_config.gridLRowNum = 7;
            page_config.gridPColumnNum = page_config.gridLColumnNum = 1;
            page_config.defaultItemConfig.box_s = "13:8:13:8::::::::::::::::::::::-1168005121::LEFT::";
            page_config.defaultShortcutConfig.labelShadowRadius = 2;
            page_config.defaultShortcutConfig.iconVisibility = false;
            page_config.defaultShortcutConfig.labelShadowOffsetX = 1;
            page_config.defaultShortcutConfig.labelShadowOffsetY = 1;
            page_config.defaultShortcutConfig.labelFontColor = -1710876;
            page_config.defaultShortcutConfig.labelShadowColor = -13553616;
            page_config.defaultShortcutConfig.labelFontSize = 40;
            page_config.defaultShortcutConfig.labelFontStyle = ShortcutConfig.FontStyle.ITALIC;
            installBuiltinTheme(context, themes_dir, page_config, R.string.style_s5, "s5");

            page_config=new PageConfig();
            page_config.gridPRowNum = page_config.gridLRowNum = 4;
            page_config.gridPColumnNum = page_config.gridLColumnNum = 1;
            page_config.defaultItemConfig.box_s = "10::10:::::6::15::3::::::::-256::::::1139474176::LEFT::";
            page_config.defaultShortcutConfig.labelShadow = false;
            page_config.defaultShortcutConfig.iconScale = 2;
            page_config.defaultShortcutConfig.iconReflection = false;
            page_config.defaultShortcutConfig.selectionColorLabel = -256;
            page_config.defaultShortcutConfig.labelVsIconMargin = 10;
            page_config.defaultShortcutConfig.labelMaxLines = 2;
            page_config.defaultShortcutConfig.labelVsIconPosition = ShortcutConfig.LabelVsIconPosition.RIGHT;
            page_config.defaultShortcutConfig.labelFontSize = 40;
            installBuiltinTheme(context, themes_dir, page_config, R.string.style_s6, "s6");

            page_config=new PageConfig();
            page_config.gridPColumnNum = page_config.gridLColumnNum = 4;
            page_config.defaultItemConfig.box_s = "4:4:4:4:1:1:1:1:3:3:3:3:-10533435:-10533435:-8494369:-8494369:::::::::-9615107:-178474::::";
            page_config.defaultShortcutConfig.labelShadow = false;
            page_config.defaultShortcutConfig.labelMaxLines = 2;
            page_config.defaultShortcutConfig.labelVisibility = false;
            page_config.defaultShortcutConfig.iconReflection = false;
            installBuiltinTheme(context, themes_dir, page_config, R.string.style_s7, "s7");

            page_config=new PageConfig();
            page_config.gridPColumnNum = page_config.gridLColumnNum = 3;
            page_config.defaultItemConfig.box_s = "8:8:8:8:8:8:8:8:4::4::-256:-16776961:-65536:-16711936:-16776961:-65536:-16711936:-256:::::-1526023937:::::";
            page_config.defaultShortcutConfig.selectionColorLabel = -256;
            page_config.defaultShortcutConfig.iconReflection = false;
            page_config.defaultShortcutConfig.labelVsIconPosition = ShortcutConfig.LabelVsIconPosition.BOTTOM;
            installBuiltinTheme(context, themes_dir, page_config, R.string.style_s8, "s8");
        }

		return themes_dir;
	}
	
	private static void installBuiltinTheme(Context context, File themes_dir, PageConfig page_config, int label, String file_name) {
        File out=new File(themes_dir.getAbsolutePath()+File.separator+file_name);
        if(out.exists()) {
            return;
        }

		try {
			JSONObject o= JsonLoader.toJSONObject(page_config, new PageConfig());
			o.put(JsonFields.LABEL, context.getString(label));
			o.put("defaultItemConfig", JsonLoader.toJSONObject(page_config.defaultItemConfig, new ItemConfig()));
			o.put("defaultShortcutConfig", JsonLoader.toJSONObject(page_config.defaultShortcutConfig, new ShortcutConfig()));
//			o.put("defaultWidgetConfig", Utils.toJSONObject(page_config.defaultWidgetConfig, new WidgetConfig()));
//			o.put("defaultFolderConfig", Utils.toJSONObject(page_config.defaultFolderConfig, new FolderConfig()));
			
            FileUtils.saveStringToFile(o.toString(), out);
		} catch (IOException e) {
			// pass
		} catch (JSONException e) {
			// pass
		}
	}
	
	private void loadThemes() {
		File themes_dir=getThemesDir(this);
		File[] all_files=themes_dir.listFiles();
		int l=all_files.length;
		boolean has_new = getIntent().getBooleanExtra(INTENT_EXTRA_NEW, false);
        mStyles = new ArrayList<StyleEntry>();
        if(has_new) {
            mStyles.add(new StyleEntry(null, getString(R.string.new_style)));
        }
		for(int i=0; i<l; i++) {
            File f = all_files[i];
			JSONObject o=FileUtils.readJSONObjectFromFile(f);
			if(o != null) {
				try {
                    mStyles.add(new StyleEntry(f, o.getString(JsonFields.LABEL)));
				} catch (JSONException e) {
					//themes_names.add("<unknown>";
				}
			}
		}

        Collections.sort(mStyles, new Comparator<StyleEntry>() {
            @Override
            public int compare(StyleEntry styleEntry, StyleEntry styleEntry2) {
                if(styleEntry.file==null) return -1;
                if(styleEntry2.file==null) return 1;
                return styleEntry.name.compareToIgnoreCase(styleEntry2.name);
            }
        });
		
		ArrayAdapter<StyleEntry> adapter=new ArrayAdapter<StyleEntry>(this, android.R.layout.simple_list_item_1, mStyles);
		setListAdapter(adapter);
	}

    private static class StyleEntry {
        public File file;
        public String name;

        private StyleEntry(File file, String name) {
            this.file = file;
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }
}
