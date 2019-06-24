package net.pierrox.lightning_launcher.script.api;

import net.pierrox.lightning_launcher.views.item.ItemView;

/**
 * The StopPoint object allows getting and setting stop points values.
 *
 * An instance of this object can be retrieved with any function that returns an {@link Item} when that returned item is a StopPoint; or with {@link Container#addStopPoint(float, float)}.
 */
public class StopPoint extends Item {
    /**
     * @hide
     */
    public StopPoint(Lightning lightning, ItemView itemView) {
        super(lightning, itemView);
    }

    /**
     * Return the direction on which this stop point is acting.
     * @return a binary combination of <ul><li>left to right: 1</li><li>right to left: 2</li><li>top to bottom: 4</li><li>bottom to top: 8</li></ul>
     */
    public int getDirection() {
        return getStopPoint().getDirection();
    }

    /**
     * Set this stop point direction.
     * @see #getDirection() for acceptable values
     */
    public void setDirection(int direction) {
        net.pierrox.lightning_launcher.data.StopPoint stopPoint = getStopPoint();
        stopPoint.setDirection(direction);
        stopPoint.notifyChanged();
    }

    /**
     * Return the edges on which this stop point matches.
     * @return a binary combination of <ul><li>left: 1</li><li>right: 2</li><li>top: 4</li><li>bottom: 8</li></ul>
     */
    public int getMatchingEdges() {
        return getStopPoint().getMatchEdge();
    }

    /**
     * Set this stop point matching edges.
     * @see #getMatchingEdges() for acceptable values
     */
    public void setMatchingEdges(int matchEdge) {
        getStopPoint().setMatchEdge(matchEdge);
    }

    /**
     * Return what this stop point matches.
     * @return a binary combination of <ul><li>scroll: 1</li><li>drag: 2</li></ul>
     */
    public int getMatchingWhat() {
        return getStopPoint().getWhat();
    }

    /**
     * Set this stop point matching scrolling type.
     * @see #getMatchingWhat() for acceptable values
     */
    public void setMatchingWhat(int matchingWhat) {
        getStopPoint().setWhat(matchingWhat);
    }

    /**
     * Return true if this stop point is a barrier
     */
    public boolean isBarrier() {
        return getStopPoint().isBarrier();
    }

    /**
     * Set this stop point barrier attribute.
     */
    public void setBarrier(boolean barrier) {
        getStopPoint().setBarrier(barrier);
    }

    /**
     * Return true if this stop point acts desktop wide
     */
    public boolean isDesktopWide() {
        return getStopPoint().isDesktopWide();
    }

    /**
     * Set this stop point desktop wide attribute.
     */
    public void setDesktopWide(boolean desktopWide) {
        getStopPoint().setDesktopWide(desktopWide);
    }

    /**
     * Return true if this stop point is snapping
     */
    public boolean isSnapping() {
        return getStopPoint().isSnapping();
    }

    /**
     * Set this stop point snapping attribute.
     */
    public void setSnapping(boolean snapping) {
        getStopPoint().setSnapping(snapping);
    }

    public net.pierrox.lightning_launcher.data.StopPoint getStopPoint() {
        return (net.pierrox.lightning_launcher.data.StopPoint) getItem();
    }
}
