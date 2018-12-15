package net.pierrox.lightning_launcher.data;

import android.content.Context;

import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.JsonFields;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.StopPointView;

import org.json.JSONException;
import org.json.JSONObject;

public class StopPoint extends Item {
	
    public static final int DIRECTION_LEFT_TO_RIGHT = 1;
    public static final int DIRECTION_RIGHT_TO_LEFT = 2;
    public static final int DIRECTION_TOP_TO_BOTTOM = 4;
    public static final int DIRECTION_BOTTOM_TO_TOP = 8;

    public static final int MATCH_EDGE_LEFT = 1;
    public static final int MATCH_EDGE_RIGHT = 2;
    public static final int MATCH_EDGE_TOP = 4;
    public static final int MATCH_EDGE_BOTTOM = 8;

    public static final int STOP_SCROLL = 1;
    public static final int STOP_DRAG = 2;

    private int mDirection = DIRECTION_LEFT_TO_RIGHT | DIRECTION_RIGHT_TO_LEFT | DIRECTION_TOP_TO_BOTTOM | DIRECTION_BOTTOM_TO_TOP;
    private int mMatchEdge = MATCH_EDGE_LEFT | MATCH_EDGE_RIGHT | MATCH_EDGE_TOP | MATCH_EDGE_BOTTOM;
    private int mWhat = STOP_SCROLL|STOP_DRAG;
    private boolean mIsBarrier = false;
    private boolean mIsDesktopWide = false;
    private boolean mIsSnapping = false;
    private EventAction mReachedAction = EventAction.UNSET();

    public int mCurrentViewX;
    public int mCurrentViewY;

    public StopPoint(Page page) {
        super(page);
    }

    @Override
	public void createFromJSONObject(JSONObject o) throws JSONException {
		readItemFromJSONObject(o);

        mDirection = o.optInt(JsonFields.STOP_POINT_DIRECTION, DIRECTION_LEFT_TO_RIGHT | DIRECTION_RIGHT_TO_LEFT | DIRECTION_TOP_TO_BOTTOM | DIRECTION_BOTTOM_TO_TOP);
        mMatchEdge = o.optInt(JsonFields.STOP_POINT_MATCH_EDGE, MATCH_EDGE_LEFT | MATCH_EDGE_RIGHT | MATCH_EDGE_TOP | MATCH_EDGE_BOTTOM);
        mWhat = o.optInt(JsonFields.STOP_POINT_WHAT, STOP_SCROLL|STOP_DRAG);
        mIsBarrier = o.optBoolean(JsonFields.STOP_POINT_BARRIER, false);
        mIsDesktopWide = o.optBoolean(JsonFields.STOP_POINT_DESKTOP_WIDE, false);
        mIsSnapping = o.optBoolean(JsonFields.STOP_POINT_SNAPPING, false);
        if(o.has(JsonFields.STOP_POINT_REACHED_EVENT_ACTION)) {
            EventAction ea = new EventAction();
            JsonLoader.loadFieldsFromJSONObject(ea, o.getJSONObject(JsonFields.STOP_POINT_REACHED_EVENT_ACTION), null);
            mReachedAction = ea;
        } else {
            int action = o.optInt(JsonFields.STOP_POINT_REACHED_ACTION, GlobalConfig.UNSET);
            String data = o.optString(JsonFields.STOP_POINT_REACHED_DATA, null);
            mReachedAction = new EventAction(action, data);
        }
	}

    public void copyFrom(StopPoint from) {
        mDirection = from.mDirection;
        mMatchEdge = from.mMatchEdge;
        mWhat = from.mWhat;
        mIsBarrier = from.mIsBarrier;
        mIsDesktopWide = from.mIsDesktopWide;
        mIsSnapping = from.mIsSnapping;
        mReachedAction = new EventAction(from.mReachedAction.action, from.mReachedAction.data);
    }

	@Override
	public Item clone() {
		return null;
	}

    @Override
    public int getAlpha() {
        return 255;
    }

    @Override
    public ItemView createView(Context context) {
        return new StopPointView(context, this);
    }

    public int getDirection() {
        return mDirection;
    }

    public void setDirection(int direction) {
        mDirection = direction;
    }

    public int getMatchEdge() {
        return mMatchEdge;
    }

    public void setMatchEdge(int match_edge) {
        mMatchEdge = match_edge;
        mPage.setModified();
    }

    public int getWhat() {
        return mWhat;
    }

    public void setWhat(int what) {
        mWhat = what;
        mPage.setModified();
    }

    public boolean isBarrier() {
        return mIsBarrier;
    }

    public void setBarrier(boolean barrier) {
        mIsBarrier = barrier;
        mPage.setModified();
    }

    public boolean isDesktopWide() {
        return mIsDesktopWide;
    }

    public void setDesktopWide(boolean desktop_wide) {
        mIsDesktopWide = desktop_wide;
        mPage.setModified();
    }

    public boolean isSnapping() {
        return mIsSnapping;
    }

    public void setSnapping(boolean snapping) {
        mIsSnapping = snapping;
        mPage.setModified();
    }
    
    public EventAction getReachedAction() {
    	return mReachedAction;
    }

    public void setReachedAction(EventAction reachedAction) {
        mReachedAction = reachedAction;
    }
}
