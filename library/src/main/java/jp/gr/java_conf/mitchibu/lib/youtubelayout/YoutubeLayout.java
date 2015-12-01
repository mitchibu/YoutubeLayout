package jp.gr.java_conf.mitchibu.lib.youtubelayout;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.Arrays;

public class YoutubeLayout extends ViewGroup {
	private static final int DIRECTION_NONE = 0;
	private static final int DIRECTION_HORIZONTAL = 1;
	private static final int DIRECTION_VERTICAL = 2;

	private final int topViewID;
	private final int bodyViewID;
	private final int verticalMargin;
	private final int horizontalMargin;
	private final float topWidth;
	private final float topHeight;
	private final int touchSlop;
	private final int maximumVelocity;
	private final Scroller scroller;

	private View topView = null;
	private View bodyView = null;
	private VelocityTracker velocityTracker = null;
	private float lastX;
	private float lastY;
	private int dragDirection = DIRECTION_NONE;
	private boolean isMaximized = true;
	private boolean layoutByScrollEnd = false;
	private OnMaximizedListener onMaximizedListener = null;
	private OnMinimizedListener onMinimizedListener = null;
	private OnGotOutListener onGotOutListener = null;

	public YoutubeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		if(attrs == null) throw new RuntimeException();

		int[] attrSet = {
				R.attr.top_view,
				R.attr.body_view,
				R.attr.vertical_margin,
				R.attr.horizontal_margin,
				R.attr.top_width,
				R.attr.top_height,
		};
		Arrays.sort(attrSet);

		TypedArray a = null;
		try {
			a = context.obtainStyledAttributes(attrs, R.styleable.YoutubeLayout);
			topViewID = a.getResourceId(Arrays.binarySearch(attrSet, R.attr.top_view), 0);
			if(topViewID == 0) throw new RuntimeException();
			bodyViewID = a.getResourceId(Arrays.binarySearch(attrSet, R.attr.body_view), 0);
			if(bodyViewID == 0) throw new RuntimeException();
			verticalMargin = a.getDimensionPixelSize(Arrays.binarySearch(attrSet, R.attr.vertical_margin), 0);
			horizontalMargin = a.getDimensionPixelSize(Arrays.binarySearch(attrSet, R.attr.horizontal_margin), 0);
			topWidth = a.getFloat(Arrays.binarySearch(attrSet, R.attr.top_width), 16f);
			topHeight = a.getFloat(Arrays.binarySearch(attrSet, R.attr.top_height), 9f);
		} finally {
			if(a != null) a.recycle();
		}

		scroller = new Scroller(context);
		ViewConfiguration conf = ViewConfiguration.get(context);
		touchSlop = conf.getScaledTouchSlop();
		maximumVelocity = conf.getScaledMaximumFlingVelocity();

		super.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
			@Override
			public void onChildViewAdded(View parent, View child) {
				if(child.getId() == topViewID) topView = child;
				else if(child.getId() == bodyViewID) bodyView = child;
			}

			@Override
			public void onChildViewRemoved(View parent, View child) {
				if(child.getId() == topViewID) topView = null;
				else if(child.getId() == bodyViewID) bodyView = null;
			}
		});
	}

	public boolean isMaximized() {
		return isMaximized;
	}

	public void maximize(boolean smoothScroll) {
		isMaximized = true;
		int scrollY = getScrollY();
		int dy = -scrollY;
		if(dy != 0) {
			if(smoothScroll) {
				scroller.startScroll(getScrollX(), scrollY, 0, dy);
				invalidate();
			} else {
				scrollBy(0, dy);
			}
		}
		if(!isInPortrait()) {
			layoutByScrollEnd = true;
			invalidate();
		}
	}

	public void minimize(boolean smoothScroll) {
		isMaximized = false;
		int scrollY = getScrollY();
		int dy = -(bodyView.getHeight() + scrollY);
		if(dy != 0) {
			if(smoothScroll) {
				scroller.startScroll(getScrollX(), scrollY, 0, dy);
				invalidate();
			} else {
				scrollBy(0, dy);
			}
		}
		if(!isInPortrait()) {
			layoutByScrollEnd = true;
			invalidate();
		}
	}

	public void setOnMaximizedListener(OnMaximizedListener listener) {
		onMaximizedListener = listener;
	}

	public void setOnMinimizedListener(OnMinimizedListener listener) {
		onMinimizedListener = listener;
	}

	public void setOnGotOutListener(OnGotOutListener listener) {
		onGotOutListener = listener;
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		dragDirection = DIRECTION_NONE;
	}

	@Override
	public void computeScroll() {
		if(scroller.computeScrollOffset()) {
			scrollTo(scroller.getCurrX(), scroller.getCurrY());
			invalidate();
		} else if(dragDirection == DIRECTION_NONE) {
			if(!isInPortrait() && layoutByScrollEnd) {
				layoutByScrollEnd = false;
				requestLayout();
			} else if(!isMaximized && onGotOutListener != null && getScrollX() > 0) {
				onGotOutListener.onGotOut(this, new Cancellation());
			}
			if(isMaximized && onMaximizedListener != null) onMaximizedListener.onMaximized(this);
			else if(!isMaximized && onMinimizedListener != null) onMinimizedListener.onMinimized(this);
		}
		topViewScale();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if(isMaximized && !isInPortrait()) return false;

		switch(event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			lastX = event.getX();
			lastY = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			dragDirection = getDragDirection(event);
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			dragDirection = DIRECTION_NONE;
			break;
		}
		return dragDirection != DIRECTION_NONE;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(isMaximized && !isInPortrait()) return false;

		if(velocityTracker == null) velocityTracker = VelocityTracker.obtain();
		velocityTracker.addMovement(event);
		switch(event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			if(event.getEdgeFlags() != 0 || (!isInTopView(event) && !isInBodyView(event))) return false;
			break;
		case MotionEvent.ACTION_MOVE:
			if(dragDirection == DIRECTION_NONE) dragDirection = getDragDirection(event);
			if(dragDirection != DIRECTION_NONE) {
				final float x = event.getX(0);
				final float xDiff = dragDirection == DIRECTION_HORIZONTAL ? lastX - x : 0;
				final float y = event.getY(0);
				final float yDiff = dragDirection == DIRECTION_VERTICAL ? lastY - y : 0;
				if(canScroll((int)xDiff, (int)yDiff)) scrollBy((int)xDiff, (int)yDiff);
				lastX = event.getX();
				lastY = event.getY();
			}
			break;
		case MotionEvent.ACTION_UP:
			if(dragDirection != DIRECTION_NONE) {
				velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
				if(dragDirection == DIRECTION_VERTICAL) {
					final int velocityY = (int)velocityTracker.getYVelocity(0);
					if(velocityY > 0) minimize(true);
					else maximize(true);
				} else if(dragDirection == DIRECTION_HORIZONTAL) {
					final int velocityX = (int)velocityTracker.getXVelocity(0);
					final int scrollX = getScrollX();
					if(onGotOutListener != null && scrollX > getWidth() / 2 && velocityX < 0) {
						scroller.startScroll(scrollX, getScrollY(), getWidth() - scrollX, 0);
					} else {
						scroller.startScroll(scrollX, getScrollY(), -scrollX, 0);
					}
					invalidate();
				}
				dragDirection = DIRECTION_NONE;
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			if(dragDirection != DIRECTION_NONE) {
				if(dragDirection == DIRECTION_VERTICAL) {
					if(isMaximized) maximize(true);
					else minimize(true);
				} else if(dragDirection == DIRECTION_HORIZONTAL) {
					scroller.startScroll(getScrollX(), getScrollY(), -getScrollX(), 0);
					invalidate();
				}
				dragDirection = DIRECTION_NONE;
			}
			break;
		}
		return true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);

		if(topView != null && bodyView != null) {
			if(isMaximized && !isInPortrait()) {
				int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
				int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
				topView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
			} else {
				int topViewWidth = isInPortrait() ? width : height;
				int topViewHeight = (int)((float)topViewWidth * (topHeight / topWidth));
				int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(topViewWidth, MeasureSpec.EXACTLY);
				int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(topViewHeight, MeasureSpec.EXACTLY);
				topView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
			}

			int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
			int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height - topView.getMeasuredHeight(), MeasureSpec.EXACTLY);
			bodyView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
		}
		setMeasuredDimension(ViewCompat.resolveSizeAndState(width, widthMeasureSpec, 0), ViewCompat.resolveSizeAndState(height, heightMeasureSpec, 0));
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if(topView != null && bodyView != null) {
			int x = getMeasuredWidth() - topView.getMeasuredWidth();
			int y = 0;
			topView.layout(x, y, x + topView.getMeasuredWidth(), y + topView.getMeasuredHeight());

			x = 0;
			y += topView.getMeasuredHeight();
			bodyView.layout(x, y, x + bodyView.getMeasuredWidth(), y + bodyView.getMeasuredHeight());

			if(isMaximized) {
				scrollTo(0, 0);
			} else {
				int dy = -(bodyView.getHeight() + getScrollY());
				scrollBy(0, dy);
			}
		}
	}

	@Override
	public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
		return generateDefaultLayoutParams();
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(0, 0);
	}

	private boolean isInTopView(MotionEvent event) {
		Rect rect = new Rect();
		topView.getGlobalVisibleRect(rect);
		return rect.contains((int)event.getRawX(), (int)event.getRawY());
	}

	private boolean isInBodyView(MotionEvent event) {
		int x = (int)event.getX(0);
		int y = (int)event.getY(0);
		y += getScrollY();
		return !(bodyView.getLeft() > x || bodyView.getRight() < x) && !(bodyView.getTop() > y || bodyView.getBottom() < y);
	}

	private int getDragDirection(MotionEvent event) {
		if(!isInTopView(event)) return DIRECTION_NONE;

		final float xDiff = Math.abs(event.getX(0) - lastX);
		final float yDiff = Math.abs(event.getY(0) - lastY);
		if(!isMaximized && xDiff > touchSlop && xDiff > yDiff) {
			return DIRECTION_HORIZONTAL;
		} else if(yDiff > touchSlop && yDiff > xDiff) {
			return DIRECTION_VERTICAL;
		}
		return DIRECTION_NONE;
	}

	private boolean canScroll(@SuppressWarnings("UnusedParameters") int dx, int dy) {
		switch(dragDirection) {
		case DIRECTION_HORIZONTAL:
			return true;
		case DIRECTION_VERTICAL:
			final int newY = getScrollY() + dy;
			if(newY <= 0 && newY >= -bodyView.getHeight()) return true;
			break;
		}
		return false;
	}

	private boolean isInPortrait() {
		Configuration config = getResources().getConfiguration();
		return config.orientation == Configuration.ORIENTATION_PORTRAIT;
	}

	private void topViewScale() {
		float s = 1.0f + (float)getScrollY() / getHeight();
		ViewCompat.setPivotX(topView, topView.getWidth());
		ViewCompat.setPivotY(topView, topView.getHeight());
		ViewCompat.setScaleX(topView, s);
		ViewCompat.setScaleY(topView, s);
		ViewCompat.setTranslationX(topView, verticalMargin * (s - 1.0f));
		ViewCompat.setTranslationY(topView, horizontalMargin * (s - 1.0f));
		ViewCompat.setAlpha(bodyView, s);

		ViewCompat.setAlpha(topView, 1.0f - (float)getScrollX() / getWidth());
	}

	public static class LayoutParams extends ViewGroup.LayoutParams {
		public LayoutParams(int width, int height) {
			super(width, height);
		}
	}

	public interface OnMaximizedListener {
		void onMaximized(YoutubeLayout yt);
	}

	public interface OnMinimizedListener {
		void onMinimized(YoutubeLayout yt);
	}

	public interface OnGotOutListener {
		void onGotOut(YoutubeLayout yt, Cancellation cancellation);
	}

	public class Cancellation {
		public void cancel() {
			int scrollX = getScrollX();
			scroller.startScroll(scrollX, getScrollY(), -scrollX, 0);
			invalidate();
		}
	}
}
