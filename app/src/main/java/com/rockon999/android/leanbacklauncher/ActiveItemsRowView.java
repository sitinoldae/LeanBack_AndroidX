package com.rockon999.android.leanbacklauncher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.OnChildSelectedListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.rockon999.android.leanbacklauncher.animation.ViewDimmer;
import com.rockon999.android.leanbacklauncher.animation.ViewDimmer.DimState;
import com.rockon999.android.leanbacklauncher.apps.AppsAdapter;
import com.rockon999.android.leanbacklauncher.apps.BannerSelectedChangedListener;
import com.rockon999.android.leanbacklauncher.apps.BannerView;
import com.rockon999.android.leanbacklauncher.apps.LaunchPoint;
import com.rockon999.android.leanbacklauncher.apps.OnEditModeChangedListener;
import com.rockon999.android.leanbacklauncher.util.Util;
import com.rockon999.android.leanbacklauncher.widget.EditModeView;
import com.rockon999.android.leanbacklauncher.widget.EditModeViewActionListener;

import java.util.ArrayList;

public class ActiveItemsRowView extends HorizontalGridView implements OnChildSelectedListener, OnHierarchyChangeListener, BannerSelectedChangedListener, EditModeViewActionListener, OnEditModeChangedListener, OnGlobalFocusChangeListener {
    private static boolean DEBUG;
    private static String TAG;
    private int mCardSpacing;
    AdapterDataObserver mChangeObserver;
    private View mCurView;
    protected DimState mDimState;
    private ArrayList<OnEditModeChangedListener> mEditListeners;
    private boolean mEditMode;
    private boolean mEditModePending;
    private EditModeView mEditModeView;
    private boolean mIsAdjustable;
    private boolean mIsEditable;
    private View mLastFocused;
    private RowCountChangeListener mListener;
    private int mNumRows;
    private int mRowHeight;

    private int minNumberOfRows;
    private int maxNumberOfRows;

    public void setRowConstraints(int minNumberOfRows, int maxNumberOfRows) {
        if (minNumberOfRows == 0 || maxNumberOfRows == 0) {
            return;
        }

        // todo abs?
        this.minNumberOfRows = Math.abs(minNumberOfRows);
        this.maxNumberOfRows = Math.abs(maxNumberOfRows);
    }

    public interface RowCountChangeListener {
        void onRowCountChanged();
    }

    /* renamed from: ActiveItemsRowView.1 */
    class C01531 extends AdapterDataObserver {
        C01531() {
        }

        @SuppressLint("RestrictedApi")
        public void onChanged() {
            Log.i(TAG, "onChanged");
            ActiveItemsRowView.this.adjustNumRows();
            Adapter adapter = ActiveItemsRowView.this.getAdapter();
            if ((adapter instanceof AppsAdapter) && ((AppsAdapter) adapter).takeItemsHaveBeenSorted()) {
                ActiveItemsRowView.this.setSelectedPosition(0);
            }
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
            ActiveItemsRowView.this.adjustNumRows();
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
            ActiveItemsRowView.this.adjustNumRows();
        }
    }

    /* renamed from: ActiveItemsRowView.2 */
    class C01542 implements Runnable {
        C01542() {
        }

        public void run() {
            ActiveItemsRowView.this.getLayoutParams().height = ((ActiveItemsRowView.this.mNumRows * ActiveItemsRowView.this.mRowHeight) + ((ActiveItemsRowView.this.mNumRows - 1) * ActiveItemsRowView.this.mCardSpacing)) + (ActiveItemsRowView.this.getPaddingTop() + ActiveItemsRowView.this.getPaddingBottom());
            ActiveItemsRowView.this.setNumRows(ActiveItemsRowView.this.mNumRows);
            ActiveItemsRowView.this.setRowHeight(ActiveItemsRowView.this.mRowHeight);
            if (ActiveItemsRowView.this.mListener != null) {
                ActiveItemsRowView.this.mListener.onRowCountChanged();
            }
        }
    }

    static {
        TAG = "ActiveItemsRowView";
        DEBUG = true;
    }

    public ActiveItemsRowView(Context context) {
        this(context, null, 0);
    }

    public ActiveItemsRowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("RestrictedApi")
    public ActiveItemsRowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mChangeObserver = new C01531();
        setChildrenDrawingOrderEnabled(true);
        setOnChildSelectedListener(this);
        setAnimateChildLayout(true);
        this.mEditListeners = new ArrayList<>();
        this.mDimState = DimState.INACTIVE;

        Resources res = context.getResources();

        this.minNumberOfRows = res.getInteger(R.integer.min_num_banner_rows);
        this.maxNumberOfRows = res.getInteger(R.integer.max_num_banner_rows);
    }

    public void setAdapter(Adapter adapter) {
        if (adapter != null) {
            Log.i(TAG, "setAdapter->adapter->className:" + adapter.getClass().getName());

            if (getAdapter() != null) {
                getAdapter().unregisterAdapterDataObserver(this.mChangeObserver);
            }
            super.setAdapter(adapter);

            adapter.registerAdapterDataObserver(this.mChangeObserver);
        }
    }

    public void onChildAttachedToWindow(View child) {
        super.onChildAttachedToWindow(child);
        Log.i(TAG, "onChildAttachedToWindow->child->className:" + child.getClass().getName());
        if (child instanceof BannerView) {
            addEditModeListener((BannerView) child);
            ((BannerView) child).addSelectedListener(this.mEditModeView);
            ((BannerView) child).addSelectedListener(this);
            ((BannerView) child).setOnEditModeChangedListener(this);
        }
        child.setActivated(ViewDimmer.dimStateToActivated(this.mDimState));
        if (child instanceof DimmableItem) {
            ((DimmableItem) child).setDimState(this.mDimState, false);
        }
        child.setZ((float) getResources().getDimensionPixelOffset(R.dimen.unselected_item_z));
    }

    public void onChildDetachedFromWindow(View child) {
        super.onChildDetachedFromWindow(child);
        if (child instanceof BannerView) {
            removeEditModeListener((BannerView) child);
            ((BannerView) child).removeSelectedListener(this.mEditModeView);
            ((BannerView) child).removeSelectedListener(this);
            ((BannerView) child).setOnEditModeChangedListener(null);
        }
    }

    public int getNumRows() {
        return this.mNumRows;
    }

    public void setEditModeView(EditModeView editModeView) {
        if (this.mEditModeView != null) {
            this.mEditModeView.removeActionListener(this);
        }
        this.mEditModeView = editModeView;
        this.mEditModeView.addActionListener(this);
    }

    public void setActivated(boolean activated) {
        if (ViewDimmer.dimStateToActivated(this.mDimState) != activated) {
            this.mDimState = ViewDimmer.activatedToDimState(activated);
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View view = getChildAt(i);
                if (view != null) {
                    view.setActivated(activated);
                    if (view instanceof DimmableItem) {
                        ((DimmableItem) view).setDimState(this.mDimState, false);
                    }
                }
            }
        }
    }

    public void onSelectedChanged(BannerView v, boolean selected) {
        DimState dimState;
        if (isUninstallDisallowed(v) && selected) {
            this.mEditModeView.clearUninstallAndFinishLayers();
        } else {
            this.mEditModeView.onSelectedChanged(v, selected);
        }
        int childCount = getChildCount();
        refreshSelectedView();
        if (selected) {
            dimState = DimState.EDIT_MODE;
        } else {
            dimState = DimState.ACTIVE;
        }
        this.mDimState = dimState;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mCurView && (child instanceof BannerView)) {
                ((BannerView) child).setDimState(this.mDimState, false);
            }
        }
    }

    private boolean isUninstallDisallowed(BannerView v) {
        boolean z = true;
        if (v == null) {
            return true;
        }
        LaunchPoint lp = getViewLaunchPoint(v);
        if (!(lp == null || Util.isSystemApp(getContext(), getViewPackageName(v)))) {
            z = lp.isInstalling();
        }
        return z;
    }

    public boolean isRowActive() {
        return ViewDimmer.dimStateToActivated(this.mDimState);
    }

    public void onChildSelected(ViewGroup parent, View child, int position, long id) {
        Log.i(TAG, "onNChildSelected->position:" + position);

        if (child == null) return; // todo

        Log.i(TAG, "onNChildSelected->child:" + child.getClass().getName());
        if (child != this.mCurView) {
            if (this.mEditMode) {
                this.mLastFocused = this.mCurView;
            }
            this.mCurView = child;
        }
        postInvalidateDelayed(50);
    }

    public void refreshSelectedView() {
        @SuppressLint("RestrictedApi") int pos = getSelectedPosition();
        int childCount = 0;
        Adapter adapter = getAdapter();
        if (adapter != null) {
            childCount = adapter.getItemCount();
        }
        if (childCount > 0 && pos >= 0 && pos < childCount) {
            ViewHolder holder = findViewHolderForAdapterPosition(pos);
            if (holder == null || holder.itemView == null) {
                this.mCurView = null;
            } else if (this.mCurView != holder.itemView) {
                this.mCurView = holder.itemView;
            }
        }
    }

    public View getCurView() {
        if (this.mCurView == null) {
            refreshSelectedView();
        }
        return this.mCurView;
    }

    public void addEditModeListener(OnEditModeChangedListener listener) {
        this.mEditListeners.add(listener);
    }

    public void removeEditModeListener(OnEditModeChangedListener listener) {
        this.mEditListeners.remove(listener);
    }

    public void setEditMode(boolean editMode) {
        this.mEditModePending = false;
        if (this.mIsEditable) {
            if (Util.isInTouchExploration(getContext())) {
                ((Activity) getContext()).setTitle(R.string.title_app_edit_mode);
            }
            if (this.mEditMode != editMode) {
                if (DEBUG) {
                    Log.d(TAG, "change EditMode ============== " + editMode);
                }
                this.mEditMode = editMode;

                if (!editMode) {
                    getViewTreeObserver().removeOnGlobalFocusChangeListener(this);
                    AppsAdapter adapter = (AppsAdapter) getAdapter();

                    if (adapter != null) {
                        adapter.saveAppOrderSnapshot();
                    }


                } else if (isAccessibilityEnabled()) {
                    getViewTreeObserver().addOnGlobalFocusChangeListener(this);
                }
                Log.i(TAG, "setEditMode->mEditListeners->isEmpty:" + mEditListeners.isEmpty());
                if (!this.mEditListeners.isEmpty()) {
                    for (OnEditModeChangedListener listener : this.mEditListeners) {
                        Log.i(TAG, "setEditMode->listener->className:" + listener.getClass().getName());
                        listener.onEditModeChanged(editMode);
                    }
                }
                if (DEBUG || Log.isLoggable(TAG, 2)) {
                    Log.d(TAG, "Edit Mode is now " + this.mEditMode + ".");
                }
            }
        }
    }

    public boolean getEditMode() {
        return this.mEditMode;
    }

    public void setIsRowEditable(boolean isEditable) {
        this.mIsEditable = isEditable;
    }

    public boolean isRowEditable() {
        return this.mIsEditable;
    }

    public void setIsNumRowsAdjustable(boolean isAdjustable) {
        this.mIsAdjustable = isAdjustable;
        setOnHierarchyChangeListener(isAdjustable ? this : null);
    }

    public void adjustNumRows(int cardSpacing, int rowHeight) {
        int numOfRows = calculateNumRows();

        if (this.mIsAdjustable && this.mNumRows != numOfRows) {
            this.mNumRows = numOfRows;
            this.mCardSpacing = cardSpacing;
            this.mRowHeight = rowHeight;
            post(new C01542());
        }
    }

    private int calculateNumRows() {
        // todo double check this

        int itemCount = getAdapter().getItemCount();

        int cutOff = getResources().getInteger(R.integer.row_cut_off);

        double initNumOfRows = ((double) itemCount) / cutOff;

        int numOfRows = (int) Math.ceil(initNumOfRows);

        numOfRows = Math.min(numOfRows, maxNumberOfRows);
        numOfRows = Math.max(numOfRows, minNumberOfRows);

        if (numOfRows > itemCount) {
            numOfRows = itemCount;
        }

        return numOfRows;
    }

    private void adjustNumRows() {
        // todo double check this

        adjustNumRows(this.mCardSpacing, this.mRowHeight);
    }

    public void childHasTransientStateChanged(View child, boolean hasTransientState) {
    }

    public void onChildViewAdded(View parent, View child) {
        adjustNumRows();
    }

    public void onChildViewRemoved(View parent, View child) {
        adjustNumRows();
    }

    public void onEditModeExitTriggered() {
        if (isRowActive() && this.mCurView != null) {
            this.mCurView.requestFocus();
            setBannerDrawableUninstallState(false);
        }
        if (Util.isInTouchExploration(getContext())) {
            ((Activity) getContext()).setTitle(R.string.app_label);
        }
        setEditMode(false);
    }

    public void onFocusLeavingEditModeLayer(int from) {
        if (isRowActive()) {
            if (this.mLastFocused != null && (this.mLastFocused instanceof BannerView) && (this.mCurView instanceof BannerView)) {
                this.mLastFocused.requestFocus();
                if (!this.mLastFocused.equals(this.mCurView)) {
                    focusOnNewPosition();
                    this.mLastFocused = this.mCurView;
                }
                if (from == 1) {
                    this.mLastFocused.setSelected(true);
                    this.mEditModeView.setBannerUninstallModeWithAnimation(false, (BannerView) this.mLastFocused, this);
                }
            } else if (getChildCount() > 0 && this.mEditMode) {
                focusOnNewPosition();
            }
        }
    }


    public String onPrepForUninstall() {
        String packageName = "";
        if (this.mLastFocused == null || !(getAdapter() instanceof AppsAdapter)) {
            return packageName;
        }
        return getViewPackageName(this.mLastFocused);
    }

    private LaunchPoint getViewLaunchPoint(View view) {
        if (getAdapter() instanceof AppsAdapter) {
            int position = getChildAdapterPosition(view);
            AppsAdapter adapter = (AppsAdapter) getAdapter();
            if (!(adapter == null || position == -1)) {
                return adapter.getLaunchPointForPosition(position);
            }
        }
        return null;
    }

    private String getViewPackageName(View view) {
        LaunchPoint lp = getViewLaunchPoint(view);
        if (lp != null) {
            return lp.getPackageName();
        }
        return null;
    }

    public void onUninstallComplete() {
        if (this.mLastFocused != null) {
            this.mLastFocused.setSelected(false);
            if (this.mLastFocused instanceof BannerView) {
                ((BannerView) this.mLastFocused).notifyEditModeManager(false);
            }
            setChildrenLastFocusedBanner(null);
            setBannerDrawableUninstallState(false);
            focusOnNewPosition();
        }
    }

    private void focusOnNewPosition() {
        Log.i(TAG, "focusOnNewPosition");
        int newFocusPosition = indexOfChild(this.mCurView) + this.mNumRows;
        if (isAccessibilityEnabled()) {
            newFocusPosition = 0;
        }
        int numApps = getChildCount();
        if (newFocusPosition >= numApps) {
            newFocusPosition = numApps - 1;
        }
        if (newFocusPosition == indexOfChild(this.mCurView)) {
            newFocusPosition--;
        }
        if (newFocusPosition < 0 || !(this.mCurView instanceof BannerView)) {
            onEditModeExitTriggered();
            return;
        }
        onSelectedChanged((BannerView) this.mCurView, false);
        View newFocusView = getChildAt(newFocusPosition);
        newFocusView.requestFocus();
        newFocusView.setSelected(false);
    }

    public void onUninstallFailure() {
        if (this.mLastFocused != null) {
            this.mLastFocused.requestFocus();
            setBannerDrawableUninstallState(false);
        }
    }

    private Drawable getLastFocusedBannerDrawable() {
        Drawable drawable = null;
        View banner = this.mLastFocused.findViewById(R.id.app_banner);
        if (!(banner instanceof ImageView)) {
            return makeBitmapDrawableFromFallback(banner);
        }
        if (!(getAdapter() instanceof AppsAdapter)) {
            return null;
        }
        int position = getChildAdapterPosition(this.mLastFocused);
        AppsAdapter adapter = (AppsAdapter) getAdapter();
        if (position != -1) {
            drawable = adapter.getDrawableFromLaunchPoint(position);
        }
        return drawable;
    }

    private Drawable makeBitmapDrawableFromFallback(View fallback) {
        Bitmap bitmap = Bitmap.createBitmap((int) getResources().getDimension(R.dimen.banner_width), (int) getResources().getDimension(R.dimen.banner_height), Config.ARGB_8888);
        fallback.draw(new Canvas(bitmap));
        return new BitmapDrawable(getResources(), bitmap);
    }

    public void setBannerDrawableUninstallState(boolean uninstalling) {
        int i = 8;
        if (this.mLastFocused instanceof BannerView) {
            View bannerView = this.mLastFocused.findViewById(R.id.app_banner);
            if (bannerView != null) {
                Drawable drawable;
                if (uninstalling) {
                    drawable = getResources().getDrawable(R.drawable.dashed_holder, null);
                    bannerView.setBackground(drawable);
                } else {
                    //Drawable drawable = getResources().getDrawable(R.drawable.banner_background, null);
                    if (bannerView instanceof LinearLayout && mLastFocused instanceof BannerView) {
                        int backColor = ((BannerView) mLastFocused).getBannerBackColor();
                        if (backColor != -1)
                            bannerView.setBackgroundColor(backColor);
                        else {
                            drawable = getResources().getDrawable(R.drawable.banner_background, null);
                            bannerView.setBackground(drawable);
                        }
                    }
                    //bannerView.setBackground(bannerView.getBackground());
                }
                if (bannerView instanceof LinearLayout) {
                    int i2;
                    View icon = this.mLastFocused.findViewById(R.id.banner_icon);
                    if (uninstalling) {
                        i2 = 8;
                    } else {
                        i2 = 0;
                    }
                    icon.setVisibility(i2);
                    View text = this.mLastFocused.findViewById(R.id.banner_label);
                    if (!uninstalling) {
                        i = 0;
                    }
                    text.setVisibility(i);
                }
                if (bannerView instanceof ImageView) {
                    ImageView imageView = (ImageView) bannerView;
                    if (uninstalling) {
                        drawable = null;
                    } else {
                        drawable = getLastFocusedBannerDrawable();
                    }
                    imageView.setImageDrawable(drawable);
                }
                if (!uninstalling) {
                    this.mLastFocused = null;
                }
            }
        }
    }

    public void setUninstallState() {
        refreshSelectedView();
        if (!isUninstallDisallowed((BannerView) this.mCurView) && (this.mCurView instanceof BannerView)) {
            this.mLastFocused = this.mCurView;
            this.mEditModeView.requestUninstallIconFocus((BannerView) this.mCurView, this);
            this.mEditModeView.setBannerDrawable(getLastFocusedBannerDrawable());
        }
    }

    public void setChildrenLastFocusedBanner(BannerView view) {
        Log.i(TAG, "setChildrenLastFocusBanner");
        int children = getChildCount();
        for (int i = 0; i < children; i++) {
            if (getChildAt(i) instanceof BannerView) {
                ((BannerView) getChildAt(i)).setLastFocusedBanner(view);
            }
        }
    }

    public void setLastFocused() {
        this.mLastFocused = this.mCurView;
    }

    public void onEditModeChanged(boolean editMode) {
        Log.i(TAG, "onEditModeChanged");
        setEditMode(editMode);
    }

    public void swapAppOrder(BannerView movingBanner, BannerView otherBanner) {
        ((AppsAdapter) getAdapter()).moveLaunchPoint(getChildAdapterPosition(movingBanner), getChildAdapterPosition(otherBanner), true);
    }

    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        Log.i(TAG, "onGlobalFocusChanged");
        if (!(!isRowActive() || oldFocus == null || newFocus == null)) {
            if ((oldFocus instanceof BannerView) && (newFocus instanceof BannerView)) {
                if (oldFocus.isSelected()) {
                    swapAppOrder((BannerView) oldFocus, (BannerView) newFocus);
                    oldFocus.requestFocus();
                }
            } else if (newFocus.equals(this.mEditModeView.getUninstallIcon())) {
                setUninstallState();
                setChildrenLastFocusedBanner((BannerView) this.mLastFocused);
            } else if (oldFocus.equals(this.mEditModeView.getUninstallIcon())) {
                this.mEditModeView.setBannerUninstallModeWithAnimation(false, (BannerView) this.mCurView, this);
                setChildrenLastFocusedBanner(null);
            } else if (newFocus.equals(this.mEditModeView.getFinishButton())) {
                setChildrenLastFocusedBanner((BannerView) this.mCurView);
            } else if (oldFocus.equals(this.mEditModeView.getFinishButton())) {
                setChildrenLastFocusedBanner(null);
            }
        }
    }

    public View focusSearch(View focused, int direction) {
        Log.i(TAG, "focusSearch");
        if (!this.mEditMode) {
            return super.focusSearch(focused, direction);
        }
        int position = getChildAdapterPosition(focused);
        int numRows = getNumRows();
        if (focused.isSelected()) {
            if (getItemAnimator().isRunning()) {
                return focused;
            }
            if (getLayoutDirection() == 1 && (direction == 17 || direction == 66)) {
                direction = direction == 17 ? 66 : 17;
            }
            if (direction == 130) {
                if (position % numRows >= numRows - 1 || position >= getAdapter().getItemCount() - 1) {
                    setUninstallState();
                    return this.mEditModeView.getUninstallIcon();
                }
                moveLaunchPoint(position, position + 1, true);
                return focused;
            } else if (direction == 33) {
                if (position % numRows > 0) {
                    moveLaunchPoint(position, position - 1, true);
                }
                return focused;
            } else if (direction == 66) {
                moveLaunchPoint(position, position + numRows, true);
                return focused;
            } else if (direction == 17) {
                moveLaunchPoint(position, position - numRows, true);
                return focused;
            }
        } else if (direction == 130 && position % numRows == numRows - 1) {
            setLastFocused();
            return this.mEditModeView.getFinishButton();
        } else if (direction == 130 && position == getAdapter().getItemCount() - 1) {
            return focused;
        } else {
            if (direction == 33 && position % numRows == 0) {
                return focused;
            }
        }
        return super.focusSearch(focused, direction);
    }

    public void setEditModePending(boolean isPending) {
        this.mEditModePending = isPending;
    }

    public boolean isEditModePending() {
        return this.mEditModePending;
    }

    private boolean isAccessibilityEnabled() {
        return ((AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE)).isEnabled();
    }

    private boolean moveLaunchPoint(int fromPosition, int toPosition, boolean userAction) {
        Log.i(TAG, "moveLaunchPoint");
        return ((AppsAdapter) getAdapter()).moveLaunchPoint(fromPosition, toPosition, true);
    }
}
