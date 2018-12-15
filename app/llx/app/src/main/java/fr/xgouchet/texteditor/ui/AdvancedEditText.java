/*THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS (
XAVIER GOUCHET ) BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.*/
package fr.xgouchet.texteditor.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Scroller;

/**
 * TODO create a syntax highlighter
 */
public class AdvancedEditText extends EditText implements OnKeyListener, OnGestureListener {

    public interface OnAdvancedEditTextEvent {
        public boolean onLeftEdgeSwipe();
        public boolean onTap();
        public void onPinchStart();
        public void onPinchZoom(double scale);
    }
	/**
	 * @param context
	 *            the current context
	 * @param attrs
	 *            some attributes
	 * @category ObjectLifecycle
	 */
	public AdvancedEditText(Context context, AttributeSet attrs) {
		super(context, attrs);

		mPaintNumbers = new Paint();
		mPaintNumbers.setTypeface(Typeface.MONOSPACE);
		mPaintNumbers.setAntiAlias(true);

		mPaintHighlight = new Paint();

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mScale = displayMetrics.density;
		mScaledDensity = displayMetrics.scaledDensity;

		mPadding = (int) (mPaddingDP * mScale);

		mHighlightedLine = mHighlightStart = -1;

		mDrawingRect = new Rect();
		mLineBounds = new Rect();

		mGestureDetector = new GestureDetector(getContext(), this);

        mPaintHighlight.setColor(Color.BLACK);
        mPaintNumbers.setColor(Color.GRAY);
        mPaintHighlight.setAlpha(48);
        mPaintNumbers.setTextSize(getTextSize());

        setFlingToScroll(true);
        setWordWrap(true);
        setShowLineNumbers(true);
	}

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        mPaintNumbers.setTextSize((size>18 ? 18 : size)*mScaledDensity);
		updateLinePadding();
    }

    /**
	 * @see android.widget.TextView#computeScroll()
	 * @category View
	 */
	public void computeScroll() {

		if (mTedScroller != null) {
			if (mTedScroller.computeScrollOffset()) {
				scrollTo(mTedScroller.getCurrX(), mTedScroller.getCurrY());
			}
		} else {
			super.computeScroll();
		}
	}

	private int mDeferredScrollToLine = -1;
	public void scrollToLine(int line) {
		Layout layout = getLayout();
		if(layout == null) {
			mDeferredScrollToLine = line;
			return;
		}

		int count = getLineCount();
		Rect r = new Rect();

		int line_number = 1;
		final String text = getText().toString();
		int offset = 0;
		for (int i = 0; i < count; i++) {
			if(line_number >= line) {
				// need to set the selection now, otherwise the EditText will scroll back to the current selection, which is probably not at the same line
				setSelection(layout.getLineStart(i));
				break;
			}

			getLineBounds(i, r);
			offset = r.bottom;

			boolean line_end = text.substring(layout.getLineStart(i), layout.getLineEnd(i)).indexOf('\n')!=-1;

			if(line_end) {
				line_number++;
			}
		}

		int max = layout.getLineBounds(count-1, null) - getHeight() - mPadding;
		if(max < 0) max = 0;
		if(offset > max) {
			offset = max;
		}

		offset -= getHeight()/2;
		if(offset < 0) {
			offset = 0;
		}

		scrollTo(0, offset);
	}

	public int getSelectionLine() {
		Layout layout = getLayout();
		if(layout == null) {
			return 1;
		}

		int count = getLineCount();
		int line_number = 1;
		final String text = getText().toString();
		int selectionStart = getSelectionStart();
		for (int i = 0; i < count; i++) {
			if(layout.getLineStart(i) <= selectionStart && layout.getLineEnd(i) > selectionStart) {
				return line_number;
			}

			boolean line_end = text.substring(layout.getLineStart(i), layout.getLineEnd(i)).indexOf('\n')!=-1;

			if(line_end) {
				line_number++;
			}
		}

		return 1;
	}

	/**
	 * @see android.widget.EditText#onDraw(android.graphics.Canvas)
	 * @category View
	 */
	public void onDraw(Canvas canvas) {
        final Layout layout = getLayout();
        if(layout==null) {
            super.onDraw(canvas);
            return;
        }

		if(mDeferredScrollToLine != -1) {
			final int l = mDeferredScrollToLine;
			mDeferredScrollToLine = -1;
			scrollToLine(l);
		}

		int count, lineX, baseline;

		count = getLineCount();

		// get the drawing boundaries
		getDrawingRect(mDrawingRect);

		// display current line
		computeLineHighlight();

		// draw line numbers
		lineX = mDrawingRect.left + mLinePadding - mPadding;
		int min = 0;
		int max = count;
		getLineBounds(0, mLineBounds);
		int startBottom = mLineBounds.bottom;
		int startTop = mLineBounds.top;
		getLineBounds(count - 1, mLineBounds);
		int endBottom = mLineBounds.bottom;
		int endTop = mLineBounds.top;
		if (count > 1 && endBottom > startBottom && endTop > startTop) {
			min = Math.max(min, ((mDrawingRect.top - startBottom) * (count - 1)) / (endBottom - startBottom));
			max = Math.min(max, ((mDrawingRect.bottom - startTop) * (count - 1)) / (endTop - startTop) + 1);
		}
        int line_number = 1;
		int first_visible_line = -1;
        boolean draw_line_number = true;
        final String text = getText().toString();
		for (int i = 0; i < max; i++) {
            boolean line_end = text.substring(layout.getLineStart(i), layout.getLineEnd(i)).indexOf('\n')!=-1;
            if(i >= min) {
                baseline = getLineBounds(i, mLineBounds);
				if(mLineBounds.top > mDrawingRect.bottom - mPadding) {
					// over
					break;
				}
                if ((line_number - 1 == mHighlightedLine)) {
                    canvas.drawRect(mLineBounds, mPaintHighlight);
                }
                if (draw_line_number) {
                    if ((mMaxSize != null) && (mMaxSize.x < mLineBounds.right)) {
                        mMaxSize.x = mLineBounds.right;
                    }

                    if (mShowLineNumbers && mLineBounds.bottom >= mDrawingRect.top + mPadding) {
						if(first_visible_line == -1) {
							first_visible_line = line_number;
							mFirstVisibleLine = first_visible_line;
						}
                        canvas.drawText(String.valueOf(line_number), mDrawingRect.left + mPadding, baseline, mPaintNumbers);
                    }
                }
            }

            if(line_end) {
                line_number++;
            }

            draw_line_number = line_end;
		}

        if (mShowLineNumbers) {
            canvas.drawLine(lineX, mDrawingRect.top, lineX, mDrawingRect.bottom, mPaintNumbers);
        }

		getLineBounds(count - 1, mLineBounds);
		if (mMaxSize != null) {
			mMaxSize.y = mLineBounds.bottom;
			mMaxSize.x = Math.max(mMaxSize.x + mPadding - mDrawingRect.width(), 0);
            mMaxSize.y = Math.max(mMaxSize.y + mPadding - mDrawingRect.height(), 0);
		}

		super.onDraw(canvas);
	}

	/**
	 * @see android.view.View.OnKeyListener#onKey(android.view.View, int,
	 *      android.view.KeyEvent)
	 */
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		return false;
	}

	/**
	 * @see android.widget.TextView#onTouchEvent(android.view.MotionEvent)
	 * @category GestureDetection
	 */
	public boolean onTouchEvent(MotionEvent event) {
		if(mTedScroller != null && !mTedScroller.isFinished()) {
			mTedScroller.abortAnimation();
		}

		if (mGestureDetector != null) {
			boolean res = mGestureDetector.onTouchEvent(event);
            if(res) {
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.onTouchEvent(cancel);
                return true;
            }
		}

        float dx = 0, dy = 0;
        boolean two_pointers = event.getPointerCount() == 2;
        if (two_pointers) {
            dx = event.getX(0) - event.getX(1);
            dy = event.getY(0) - event.getY(1);
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (two_pointers) {
                    mInitialPinchDistance = Math.sqrt(dx * dx + dy * dy);
                    mOnAdvancedEditTextEvent.onPinchStart();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (two_pointers) {
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    mOnAdvancedEditTextEvent.onPinchZoom(distance / mInitialPinchDistance);
                }
                break;

        }

		return super.onTouchEvent(event);
	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onDown(android.view.MotionEvent)
	 * @category GestureDetection
	 */
	public boolean onDown(MotionEvent e) {
		return false;
	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onSingleTapUp(android.view.MotionEvent)
	 * @category GestureDetection
	 */
	public boolean onSingleTapUp(MotionEvent e) {

        if(mOnAdvancedEditTextEvent != null) {
            boolean res = mOnAdvancedEditTextEvent.onTap();
            if(res) {
                return true;
            }
        }

		if (isEnabled()) {
			((InputMethodManager) getContext().getSystemService(
					Context.INPUT_METHOD_SERVICE)).showSoftInput(this,
					InputMethodManager.SHOW_IMPLICIT);
		}
        return false;
	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onShowPress(android.view.MotionEvent)
	 * @category GestureDetection
	 */
	public void onShowPress(MotionEvent e) {
	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onLongPress(android.view.MotionEvent)
	 */
	public void onLongPress(MotionEvent e) {

	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onScroll(android.view.MotionEvent,
	 *      android.view.MotionEvent, float, float)
	 */
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// mTedScroller.setFriction(0);
        if(e1.getX() < mLinePadding && mOnAdvancedEditTextEvent != null) {
			mSkipNextFling = true;
            return mOnAdvancedEditTextEvent.onLeftEdgeSwipe();
        }
		return false;
	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onFling(android.view.MotionEvent,
	 *      android.view.MotionEvent, float, float)
	 */
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (!mFlingToScroll) {
			return true;
		}

		if(mSkipNextFling) {
			mSkipNextFling = false;
			return true;
		}

		if (mTedScroller != null) {
			mTedScroller.fling(getScrollX(), getScrollY(), -(int) velocityX,
					-(int) velocityY, 0, mMaxSize.x, 0, mMaxSize.y);
		}
		return true;
	}

	/**
	 * Update view settings from the app preferences
	 * 
	 * @category Custom
	 */
	/*public void updateFromSettings() {

		if (isInEditMode()) {
			return;
		}

		setTypeface(Settings.getTypeface(getContext()));

		// wordwrap
		setHorizontallyScrolling(!Settings.WORDWRAP);

		// color Theme
		switch (Settings.COLOR) {
		case COLOR_NEGATIVE:
			setBackgroundResource(R.drawable.textfield_black);
			setTextColor(Color.WHITE);
			mPaintHighlight.setColor(Color.WHITE);
			mPaintNumbers.setColor(Color.GRAY);
			break;
		case COLOR_MATRIX:
			setBackgroundResource(R.drawable.textfield_matrix);
			setTextColor(Color.GREEN);
			mPaintHighlight.setColor(Color.GREEN);
			mPaintNumbers.setColor(Color.rgb(0, 128, 0));
			break;
		case COLOR_SKY:
			setBackgroundResource(R.drawable.textfield_sky);
			setTextColor(Color.rgb(0, 0, 64));
			mPaintHighlight.setColor(Color.rgb(0, 0, 64));
			mPaintNumbers.setColor(Color.rgb(0, 128, 255));
			break;
		case COLOR_DRACULA:
			setBackgroundResource(R.drawable.textfield_dracula);
			setTextColor(Color.RED);
			mPaintHighlight.setColor(Color.RED);
			mPaintNumbers.setColor(Color.rgb(192, 0, 0));
			break;
		case COLOR_CLASSIC:
		default:
			setBackgroundResource(R.drawable.textfield_white);
			setTextColor(Color.BLACK);
			mPaintHighlight.setColor(Color.BLACK);
			mPaintNumbers.setColor(Color.GRAY);
			break;
		}
		mPaintHighlight.setAlpha(48);

		// text size
		setTextSize(Settings.TEXT_SIZE);
		mPaintNumbers.setTextSize(Settings.TEXT_SIZE * mScale * 0.85f);

		// refresh view
		postInvalidate();
		refreshDrawableState();



		// padding
		mLinePadding = mPadding;
		int count = getLineCount();
		if (mShowLineNumbers) {
			mLinePadding = (int) (Math.floor(Math.log10(count)) + 1);
			mLinePadding = (int) ((mLinePadding * mPaintNumbers.getTextSize())
					+ mPadding + (Settings.TEXT_SIZE * mScale * 0.5));
			setPadding(mLinePadding, mPadding, mPadding, mPadding);
		} else {
			setPadding(mPadding, mPadding, mPadding, mPadding);
		}
	}*/

    public void setFlingToScroll(boolean flingToScroll) {
        if (flingToScroll) {
            mTedScroller = new Scroller(getContext());
            mMaxSize = new Point();
        } else {
            mTedScroller = null;
            mMaxSize = null;
        }
    }

    public void setShowLineNumbers(boolean showLineNumbers) {
        mShowLineNumbers = showLineNumbers;
        updateLinePadding();
    }

	private void updateLinePadding() {
		if (mShowLineNumbers) {
			int max_text_size = (int) Math.ceil(mPaintNumbers.measureText("0000"));
			mLinePadding = mPadding*3 + max_text_size;
		} else {
			mLinePadding = mPadding;
		}
		if(mLinePadding != getPaddingLeft()) {
			setPadding(mLinePadding, mPadding, mPadding, mPadding);
		}
	}

    public void setWordWrap(boolean wordWrap) {
        mWordWrap = wordWrap;
        setHorizontallyScrolling(!wordWrap);
    }

    public void setListener(OnAdvancedEditTextEvent listener) {
        mOnAdvancedEditTextEvent = listener;
    }

	/**
	 * Compute the line to highlight based on selection
	 */
	protected void computeLineHighlight() {
		int i, line, selStart;
		String text;

		if (!isEnabled()) {
			mHighlightedLine = -1;
			return;
		}

		selStart = getSelectionStart();
		if (mHighlightStart != selStart) {
			text = getText().toString();

			line = i = 0;
			while (i < selStart) {
				i = text.indexOf("\n", i);
				if (i < 0) {
					break;
				}
				if (i < selStart) {
					++line;
				}
				++i;
			}

			mHighlightedLine = line;
		}
	}

	/** The line numbers paint */
	protected Paint mPaintNumbers;
	/** The line numbers paint */
	protected Paint mPaintHighlight;
	/** the offset value in dp */
	protected int mPaddingDP = 6;
	/** the padding scaled */
	protected int mPadding, mLinePadding;
	/** the scale for desnity pixels */
	protected float mScale;
	protected float mScaledDensity;

	/** the scroller instance */
	protected Scroller mTedScroller;
	/** the velocity tracker */
	protected GestureDetector mGestureDetector;
	/** the Max size of the view */
	protected Point mMaxSize;

	/** the highlighted line index */
	protected int mHighlightedLine;
	protected int mHighlightStart;

	protected Rect mDrawingRect, mLineBounds;

    protected boolean mFlingToScroll = true;
    protected boolean mShowLineNumbers;
    protected boolean mWordWrap;

    private double mInitialPinchDistance;

    protected OnAdvancedEditTextEvent mOnAdvancedEditTextEvent;

	private int mFirstVisibleLine;
	private boolean mSkipNextFling;

}
