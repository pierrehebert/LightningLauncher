package net.pierrox.lightning_launcher.data;

import android.content.Context;
import android.os.Build;

import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.ArrayList;

public final class ActionsDescription {

    private static final Action[] sAllActions = new Action[]{
            new Action(GlobalConfig.NOTHING, R.string.an_n, Action.CAT_NONE, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER | Action.FLAG_TYPE_SCRIPT, Build.VERSION_CODES.FROYO),

        new Action(GlobalConfig.CATEGORY, R.string.acd_l, Action.CAT_LAUNCH_AND_APPS, Action.FLAG_TYPE_DESKTOP |Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.LAUNCH_APP, R.string.an_la, Action.CAT_LAUNCH_AND_APPS, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.LAUNCH_SHORTCUT, R.string.an_ls, Action.CAT_LAUNCH_AND_APPS, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.LAUNCH_ITEM, R.string.an_i_l, Action.CAT_LAUNCH_AND_APPS, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER | Action.FLAG_ITEM, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.SHOW_APP_SHORTCUTS, R.string.an_i_as, Action.CAT_LAUNCH_AND_APPS, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER | Action.FLAG_ITEM, Build.VERSION_CODES.N_MR1),
            new Action(GlobalConfig.APP_DRAWER, R.string.an_ad, Action.CAT_LAUNCH_AND_APPS, Action.FLAG_TYPE_DESKTOP, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.SEARCH_APP, R.string.an_sa, Action.CAT_LAUNCH_AND_APPS, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.RESTART, R.string.an_re, Action.CAT_LAUNCH_AND_APPS, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),


        new Action(GlobalConfig.CATEGORY, R.string.acd_n, Action.CAT_NAVIGATION, Action.FLAG_TYPE_DESKTOP |Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.GO_HOME, R.string.an_gh, Action.CAT_NAVIGATION, Action.FLAG_TYPE_DESKTOP, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.GO_HOME_ZOOM_TO_ORIGIN, R.string.an_ghz100, Action.CAT_NAVIGATION, Action.FLAG_TYPE_DESKTOP, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.GO_DESKTOP_POSITION, R.string.an_gdp, Action.CAT_NAVIGATION, Action.FLAG_TYPE_DESKTOP, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.PREVIOUS_DESKTOP, R.string.an_pd, Action.CAT_NAVIGATION, Action.FLAG_TYPE_DESKTOP, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.NEXT_DESKTOP, R.string.an_nd, Action.CAT_NAVIGATION, Action.FLAG_TYPE_DESKTOP, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.SELECT_DESKTOP_TO_GO_TO, R.string.an_sd, Action.CAT_NAVIGATION, Action.FLAG_TYPE_DESKTOP, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.ZOOM_TO_ORIGIN, R.string.an_z100, Action.CAT_NAVIGATION, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.ZOOM_FULL_SCALE, R.string.an_zfs, Action.CAT_NAVIGATION, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.SWITCH_FULL_SCALE_OR_ORIGIN, R.string.an_zt, Action.CAT_NAVIGATION, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.BACK, R.string.an_b, Action.CAT_NAVIGATION, Action.FLAG_TYPE_DESKTOP |Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.CLOSE_APP_DRAWER, R.string.close_app_drawer, Action.CAT_NAVIGATION, Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),


        new Action(GlobalConfig.CATEGORY, R.string.acd_m, Action.CAT_MENU_STATUS_BAR, Action.FLAG_TYPE_DESKTOP |Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.LAUNCHER_MENU, R.string.an_lm, Action.CAT_MENU_STATUS_BAR, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.ITEM_MENU, R.string.an_i_me, Action.CAT_MENU_STATUS_BAR, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER | Action.FLAG_ITEM, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.CUSTOM_MENU, R.string.an_cm, Action.CAT_MENU_STATUS_BAR, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.USER_MENU, R.string.an_um, Action.CAT_MENU_STATUS_BAR, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.SHOW_HIDE_STATUS_BAR, R.string.an_ssb, Action.CAT_MENU_STATUS_BAR, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.SHOW_HIDE_APP_MENU_STATUS_BAR, R.string.an_smsb, Action.CAT_MENU_STATUS_BAR, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.SHOW_NOTIFICATIONS, R.string.an_sn, Action.CAT_MENU_STATUS_BAR, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),


        new Action(GlobalConfig.CATEGORY, R.string.acd_f, Action.CAT_FOLDERS, Action.FLAG_TYPE_DESKTOP |Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.OPEN_FOLDER, R.string.an_of, Action.CAT_FOLDERS, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.CLOSE_TOPMOST_FOLDER, R.string.an_ctf, Action.CAT_FOLDERS, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.CLOSE_ALL_FOLDERS, R.string.an_caf, Action.CAT_FOLDERS, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),


        new Action(GlobalConfig.CATEGORY, R.string.acd_ed, Action.CAT_EDITION, Action.FLAG_TYPE_DESKTOP |Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.EDIT_LAYOUT, R.string.an_el, Action.CAT_EDITION, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.ADD_ITEM, R.string.an_ao, Action.CAT_EDITION, Action.FLAG_TYPE_DESKTOP, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.MOVE_ITEM, R.string.an_i_m, Action.CAT_EDITION, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER | Action.FLAG_ITEM, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.CUSTOMIZE_ITEM, R.string.an_i_c, Action.CAT_EDITION, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER | Action.FLAG_ITEM, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.CUSTOMIZE_DESKTOP, R.string.an_cd, Action.CAT_EDITION, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.CUSTOMIZE_LAUNCHER, R.string.an_cl, Action.CAT_EDITION, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.OPEN_HIERARCHY_SCREEN, R.string.an_ohs, Action.CAT_EDITION, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),


        new Action(GlobalConfig.CATEGORY, R.string.acd_ex, Action.CAT_EXTERNAL, Action.FLAG_TYPE_DESKTOP |Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.SELECT_WALLPAPER, R.string.an_sw, Action.CAT_EXTERNAL, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.WALLPAPER_TAP, R.string.an_wt, Action.CAT_EXTERNAL, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.WALLPAPER_SECONDARY_TAP, R.string.an_wst, Action.CAT_EXTERNAL, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.SEARCH, R.string.an_s, Action.CAT_EXTERNAL, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.UNLOCK_SCREEN, R.string.an_us, Action.CAT_EXTERNAL, Action.FLAG_TYPE_DESKTOP, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.SHOW_FLOATING_DESKTOP, R.string.an_so, Action.CAT_EXTERNAL, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.HIDE_FLOATING_DESKTOP, R.string.an_ho, Action.CAT_EXTERNAL, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),


        new Action(GlobalConfig.CATEGORY, R.string.acd_a, Action.CAT_ADVANCED, Action.FLAG_TYPE_DESKTOP |Action.FLAG_TYPE_APP_DRAWER |Action.FLAG_TYPE_SCRIPT, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.RUN_SCRIPT, R.string.an_rs, Action.CAT_ADVANCED, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER | Action.FLAG_TYPE_SCRIPT, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.SET_VARIABLE, R.string.an_sv, Action.CAT_ADVANCED, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER, Build.VERSION_CODES.FROYO),
            new Action(GlobalConfig.UNSET, R.string.an_u, Action.CAT_ADVANCED, Action.FLAG_TYPE_DESKTOP | Action.FLAG_TYPE_APP_DRAWER | Action.FLAG_TYPE_SCRIPT, Build.VERSION_CODES.FROYO),
    };

	private ArrayList<Action> mActions;
	private String mActionNames[];
    private int mType;
    private boolean mForItem;

    public ActionsDescription(Context context, int type, boolean forItem) {
        mActions = new ArrayList<>(sAllActions.length);
        mType = type;
        mForItem = forItem;
        for (Action action : sAllActions) {
            boolean isItem = (action.flags & Action.FLAG_ITEM) == Action.FLAG_ITEM;
            if (((action.flags&type) == type) && (forItem || !isItem) && Build.VERSION.SDK_INT >= action.minSdkVersion) {
                mActions.add(action);
            }
        }

        int l = mActions.size();
        mActionNames = new String[l];
        for (int i = 0; i < l; i++) {
            mActionNames[i] = context.getString(mActions.get(i).label);
        }
    }

    public ArrayList<Action> getActions() {
        return mActions;
    }

    public int getType() {
        return mType;
    }

    public boolean isForItem() {
        return mForItem;
    }

    public int getActionIndex(int action) {
        for (int i = 0; i < mActions.size(); i++) {
            if (mActions.get(i).action == action) {
                return i;
            }
        }
        return 0;
	}

    public String getActionName(int action) {
        return mActionNames[getActionIndex(action)];
    }

	public int getActionAt(int index) {
		return mActions.get(index).action;
	}


	public String[] getActionNames() {
		return mActionNames;
	}
}
