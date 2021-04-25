package com.rockon999.android.leanbacklauncher;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.rockon999.android.leanbacklauncher.MainActivity.IdleListener;
import com.rockon999.android.leanbacklauncher.apps.AppsUpdateListener.SearchPackageChangeListener;
import com.rockon999.android.leanbacklauncher.util.Util;

import java.util.Random;

public class SearchOrbView extends FrameLayout implements IdleListener, SearchPackageChangeListener {
    private int mClickDeviceId, mColorBright, mColorDim, mCurrentIndex;
    private final String mFocusedKeyboardText, mFocusedMicText, mFocusedText;

    private Context mContext;
    private boolean mEatDpadCenterKeyDown;
    private final int mFocusedColor;
    private Handler mHandler;
    private final int mIdleTextFlipDelay;
    private final boolean mIsHintFlippingAllowed;
    private FrameLayout mKeyboardContainer;
    private Drawable mKeyboardFocusedIcon;
    private final int mKeyboardOrbAnimationDuration;
    private float mKeyboardOrbProgress;
    private androidx.leanback.widget.SearchOrbView mKeyboardOrbView, mMicOrbView;
    private Drawable mKeyboardUnfocusedIcon;
    private final int mLaunchFadeDuration;
    private SearchLaunchListener mListener;
    private ObjectAnimator mOrbAnimation;
    private final String mSearchHintText;
    private final Intent mSearchIntent;
    private final int mSearchOrbsSpacing;
    private Runnable mSwitchRunnable;
    private TextSwitcher mSwitcher;
    private final String[] mTextToShow;
    private final int mUnfocusedColor;
    private boolean mWahlbergUx;
    protected View mWidgetView;

    public interface SearchLaunchListener {
        void onSearchLaunched();
    }

    class SearchOrbFocusListener implements OnFocusChangeListener {
        SearchOrbFocusListener() {
        }

        public void onFocusChange(View v, boolean hasFocus) {
            SearchOrbView.this.setSearchState(false);
        }
    }

    class KeyboardOrbFocusListener implements OnFocusChangeListener {
        KeyboardOrbFocusListener() {
        }

        public void onFocusChange(View v, boolean hasFocus) {
            SearchOrbView.this.setSearchState(false);
            SearchOrbView.this.mKeyboardOrbView.setOrbIcon(hasFocus ? SearchOrbView.this.mKeyboardFocusedIcon : SearchOrbView.this.mKeyboardUnfocusedIcon);
            SearchOrbView.this.mKeyboardOrbView.setOrbColor(hasFocus ? SearchOrbView.this.mColorBright : SearchOrbView.this.mColorDim);
        }
    }

    class TextHintFactory implements ViewFactory {
        LayoutInflater inflater;
        final Context viewContext;

        TextHintFactory(Context viewContext) {
            this.viewContext = viewContext;
            this.inflater = (LayoutInflater) this.viewContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public View makeView() {
            if (this.inflater != null) {
                return this.inflater.inflate(R.layout.search_orb_text_hint, SearchOrbView.this, false);
            }
            return null;
        }
    }

    /* renamed from: SearchOrbView.4 */
    class C01724 implements Runnable {
        C01724() {
        }

        public void run() {
            int old = SearchOrbView.this.mCurrentIndex;
            SearchOrbView.this.mCurrentIndex = new Random().nextInt(SearchOrbView.this.mTextToShow.length);
            if (old == SearchOrbView.this.mCurrentIndex) {
                SearchOrbView.this.mCurrentIndex = (SearchOrbView.this.mCurrentIndex + 1) % SearchOrbView.this.mTextToShow.length;
            }
            SearchOrbView.this.configSwitcher(true, false, 0);
            SearchOrbView.this.mSwitcher.setText(SearchOrbView.this.mTextToShow[SearchOrbView.this.mCurrentIndex] + " ");
            SearchOrbView.this.mHandler.postDelayed(this, (long) SearchOrbView.this.mIdleTextFlipDelay);
        }
    }

    /* renamed from: SearchOrbView.5 */
    class C01735 implements AnimatorListener {
        C01735() {
        }

        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            if (SearchOrbView.this.mKeyboardOrbView.hasFocus()) {
                SearchOrbView.this.mMicOrbView.requestFocus();
            }
        }

        public void onAnimationCancel(Animator animation) {
        }

        public void onAnimationRepeat(Animator animation) {
        }
    }

    /* renamed from: SearchOrbView.6 */
    class C01746 implements OnClickListener {
        final /* synthetic */ boolean val$isTouchExplorationEnabled;

        C01746(boolean val$isTouchExplorationEnabled) {
            this.val$isTouchExplorationEnabled = val$isTouchExplorationEnabled;
        }

        public void onClick(View view) {
            boolean hasFocus;
            hasFocus = SearchOrbView.this.mKeyboardOrbView != null && SearchOrbView.this.mKeyboardOrbView.hasFocus();
            boolean success = this.val$isTouchExplorationEnabled ? Util.startSearchActivitySafely(SearchOrbView.this.mContext, SearchOrbView.this.mSearchIntent, hasFocus) && SearchOrbView.this.mListener != null : Util.startSearchActivitySafely(SearchOrbView.this.mContext, SearchOrbView.this.mSearchIntent, SearchOrbView.this.mClickDeviceId, hasFocus) && SearchOrbView.this.mListener != null;
            if (success) {
                SearchOrbView.this.animateOut();
                SearchOrbView.this.mListener.onSearchLaunched();
            }
        }
    }

    public SearchOrbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        boolean z = false;
        this.mKeyboardOrbProgress = 0.0f;
        this.mCurrentIndex = 0;
        this.mHandler = new Handler();
        this.mClickDeviceId = -1;
        this.mSearchIntent = Util.getSearchIntent();
        this.mContext = context;
        Theme theme = this.mContext.getTheme();
        Resources res = context.getResources();
        this.mTextToShow = res.getStringArray(R.array.search_orb_text_to_show);
        this.mIdleTextFlipDelay = res.getInteger(R.integer.search_orb_idle_hint_flip_delay);
        this.mLaunchFadeDuration = res.getInteger(R.integer.search_orb_text_fade_duration);
        this.mSearchHintText = context.getString(R.string.search_hint_text) + " ";
        this.mFocusedText = context.getString(R.string.focused_search_hint_text);
        this.mFocusedMicText = context.getString(R.string.focused_search_mic_hint_text);
        this.mFocusedKeyboardText = context.getString(R.string.focused_search_keyboard_hint_text);
        this.mFocusedColor = getColor(res, R.color.search_orb_focused_hint_color, theme);
        this.mUnfocusedColor = getColor(res, R.color.search_orb_unfocused_hint_color, theme);
        if (res.getBoolean(R.bool.is_hint_flipping_allowed)) {
            z = isKatnissPackagePresent();
        }
        this.mIsHintFlippingAllowed = z;
        this.mWahlbergUx = useWahlbergUx();
        this.mSearchOrbsSpacing = res.getDimensionPixelSize(R.dimen.search_orbs_spacing);
        this.mKeyboardOrbAnimationDuration = res.getInteger(R.integer.lb_search_orb_scale_duration_ms);
    }

    private boolean useWahlbergUx() {
        try {
            Resources searchResources = this.mContext.getPackageManager().getResourcesForApplication("com.google.android.katniss");
            int resId = 0;
            if (searchResources != null) {
                resId = searchResources.getIdentifier("katniss_uses_new_google_logo", "bool", "com.google.android.katniss");
            }
            if (resId != 0) {
                return searchResources.getBoolean(resId);
            }
        } catch (NameNotFoundException ignored) {
        }
        return false;
    }

    private boolean isKatnissPackagePresent() {
        PackageInfo packageInfo;
        try {
            packageInfo = this.mContext.getPackageManager().getPackageInfo("com.google.android.katniss", 0);
        } catch (NameNotFoundException e) {
            packageInfo = null;
        }
        return packageInfo != null;
    }

    public void onFinishInflate() {
        super.onFinishInflate(); // todo
        Resources res = this.mContext.getResources();
        Theme theme = this.mContext.getTheme();
        this.mWidgetView = findViewById(R.id.widget_wrapper);
        this.mMicOrbView = (androidx.leanback.widget.SearchOrbView) findViewById(R.id.mic_orb);
        this.mMicOrbView.setOnFocusChangeListener(new SearchOrbFocusListener());
        initializeSearchOrbs();
        initTextSwitcher(getContext());
    }

    public void onSearchPackageChanged() {
        if (useWahlbergUx() != this.mWahlbergUx) {
            this.mWahlbergUx = useWahlbergUx();
            initializeSearchOrbs();
            setSearchState(false);
        }
    }

    public String getSearchPackageName() {
        return "com.google.android.katniss";
    }

    private void initializeSearchOrbs() {
        Resources res = this.mContext.getResources();
        Theme theme = this.mContext.getTheme();
        if (this.mOrbAnimation != null && (this.mOrbAnimation.isRunning() || this.mOrbAnimation.isStarted())) {
            this.mOrbAnimation.cancel();
        }
        this.mOrbAnimation = null;
        setKeyboardOrbProgress(0.0f);
        if (this.mWahlbergUx) {
            this.mKeyboardOrbView = (androidx.leanback.widget.SearchOrbView) findViewById(R.id.keyboard_orb);
            this.mKeyboardContainer = (FrameLayout) findViewById(R.id.keyboard_orb_container);
            this.mKeyboardFocusedIcon = res.getDrawable(R.drawable.ic_keyboard_blue, theme);
            this.mKeyboardUnfocusedIcon = res.getDrawable(R.drawable.ic_keyboard_grey, theme);
            this.mColorBright = getColor(res, R.color.search_orb_bg_bright_color, theme);
            this.mColorDim = getColor(res, R.color.search_orb_bg_dim_color, theme);
            this.mKeyboardOrbView.setOrbIcon(this.mKeyboardUnfocusedIcon);
            this.mKeyboardOrbView.enableOrbColorAnimation(false);
            if (this.mKeyboardOrbView != null) {
                this.mKeyboardOrbView.setOnFocusChangeListener(new KeyboardOrbFocusListener());
                this.mOrbAnimation = ObjectAnimator.ofFloat(this, "keyboardOrbProgress", 0.0f);
                this.mOrbAnimation.setDuration((long) this.mKeyboardOrbAnimationDuration);
                setKeyboardOrbProgress(0.0f);
            }
        } else {
            this.mKeyboardFocusedIcon = null;
            this.mKeyboardUnfocusedIcon = null;
            this.mKeyboardOrbView = null;
            this.mKeyboardContainer = null;
        }

        if (this.mWahlbergUx) {
            this.mMicOrbView.setOrbColor(getColor(res, R.color.search_orb_bg_bright_color, theme));
            this.mMicOrbView.setOrbIcon(res.getDrawable(R.drawable.ic_mic_color, theme));
            this.mMicOrbView.enableOrbColorAnimation(false);
        } else {
            this.mMicOrbView.setOrbColor(getColor(res, R.color.search_orb_bg_color_old, theme), getColor(res, R.color.search_orb_bg_bright_color_old, theme));
            this.mMicOrbView.setOrbIcon(res.getDrawable(R.drawable.ic_search_mic_out_normal, theme));
            this.mMicOrbView.enableOrbColorAnimation(true);
        }
    }

    public static int getColor(Resources res, int id, @Nullable Theme theme) {
        if (VERSION.SDK_INT >= 23) {
            return res.getColor(id, theme);
        }
        return res.getColor(id);
    }

    private boolean focusIsOnSearchView() {
        return this.mMicOrbView.hasFocus() || this.mKeyboardOrbView != null && this.mKeyboardOrbView.hasFocus();
    }

    private void setSearchState(boolean isReset) {
        this.mHandler.removeCallbacks(this.mSwitchRunnable);
        boolean focused = focusIsOnSearchView();
        int old = this.mCurrentIndex;
        boolean isKeyboard = this.mWahlbergUx && focused && !this.mMicOrbView.hasFocus();
        this.mCurrentIndex = focused ? !isKeyboard ? -2 : -3 : -1;
        if (old != this.mCurrentIndex) {
            boolean z;
            boolean useFade = !(old == -1 || this.mCurrentIndex == -1);
            z = !isReset;
            configSwitcher(z, focused, useFade ? 2 : 1);
            if (this.mWahlbergUx) {
                this.mSwitcher.setText(getHintText(focused, isKeyboard));
            } else {
                this.mSwitcher.setText(getHintText(focused, false));
            }
        }
        if (this.mKeyboardOrbView != null) {
            animateKeyboardOrb(focused);
        }
    }

    private String getHintText(boolean focused, boolean isKeyboard) {
        if (this.mWahlbergUx) {
            return focused ? isKeyboard ? this.mFocusedKeyboardText : this.mFocusedMicText : this.mSearchHintText;
        }
        return focused ? this.mFocusedText : this.mSearchHintText;
    }

    private void initTextSwitcher(Context context) {
        this.mSwitcher = (TextSwitcher) findViewById(R.id.text_switcher);
        this.mSwitcher.setAnimateFirstView(false);
        this.mSwitcher.setFactory(new TextHintFactory(context));
        this.mSwitchRunnable = new C01724();
        reset();
    }

    private void configSwitcher(boolean switchViews, boolean focused, int animType) {
        int inAnim;
        int outAnim;
        View v = switchViews ? this.mSwitcher.getNextView() : this.mSwitcher.getCurrentView();
        if (v instanceof TextView) {
            TextView textView = (TextView) v;
            textView.setTextColor(focused ? this.mFocusedColor : this.mUnfocusedColor);
            textView.setTypeface(null, 2);
        }
        if (animType == 1) {
            inAnim = R.anim.slide_in_left;
            outAnim = R.anim.slide_out_right;
        } else if (animType == 0) {
            inAnim = R.anim.slide_in_bottom;
            outAnim = R.anim.slide_out_top;
        } else {
            inAnim = R.anim.fade_in;
            outAnim = R.anim.fade_out;
        }
        this.mSwitcher.setInAnimation(this.mContext, inAnim);
        this.mSwitcher.setOutAnimation(this.mContext, outAnim);
    }

    public void reset() {
        this.mHandler.removeCallbacks(this.mSwitchRunnable);
        this.mSwitcher.reset();
        this.mCurrentIndex = 0;
        setSearchState(true);
    }

    public void onIdleStateChange(boolean isIdle) {
        if (this.mIsHintFlippingAllowed) {
            this.mHandler.removeCallbacks(this.mSwitchRunnable);
            if (isIdle && isAttachedToWindow() && !this.mMicOrbView.hasFocus()) {
                this.mHandler.post(this.mSwitchRunnable);
            }
        }
    }

    public void onVisibilityChange(boolean isVisible) {
        boolean z = false;
        if (!isVisible) {
            reset();
        } else if (this.mKeyboardOrbView != null && this.mKeyboardOrbView.hasFocus()) {
            this.mMicOrbView.requestFocus();
        }
        androidx.leanback.widget.SearchOrbView searchOrbView = this.mMicOrbView;
        if (isVisible && this.mMicOrbView.hasFocus() && !this.mWahlbergUx) {
            z = true;
        }
        searchOrbView.enableOrbColorAnimation(z);
    }

    private void setVisibile(boolean visible) {
        animateVisibility(this.mWidgetView, visible);
        animateVisibility(this.mSwitcher, visible);
    }

    private void animateVisibility(View view, boolean visible) {
        view.clearAnimation();
        ViewPropertyAnimator anim = view.animate().alpha(visible ? 1.0f : 0.0f).setDuration((long) this.mLaunchFadeDuration);
        if (!(!this.mWahlbergUx || this.mKeyboardOrbView == null || visible)) {
            anim.setListener(new C01735());
        }
        anim.start();
    }

    private void animateKeyboardOrb(boolean visible) {
        if (this.mOrbAnimation != null) {
            if (this.mOrbAnimation.isStarted()) {
                this.mOrbAnimation.cancel();
            }
            if (this.mKeyboardOrbProgress != (visible ? 1.0f : 0.0f)) {
                this.mOrbAnimation.setFloatValues(this.mKeyboardOrbProgress, visible ? 1.0f : 0.0f);
                this.mOrbAnimation.start();
            }
        }
    }

    private void setKeyboardOrbProgress(float prog) {
        boolean focusable = ((double) prog) == 1.0d;
        boolean visible = prog > 0.0f;
        this.mKeyboardOrbProgress = prog;
        if (this.mKeyboardOrbView != null) {
            this.mKeyboardOrbView.setFocusable(focusable);
            this.mKeyboardOrbView.setAlpha(prog);
            this.mKeyboardContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
            this.mKeyboardContainer.setScaleX(prog);
            this.mKeyboardContainer.setScaleY(prog);
            MarginLayoutParams lp = (MarginLayoutParams) this.mKeyboardContainer.getLayoutParams();
            float offset = ((((float) this.mKeyboardOrbView.getMeasuredWidth()) / (2.0f * prog)) + ((float) this.mSearchOrbsSpacing)) * (1.0f - prog);
            lp.setMarginStart((int) (((float) (this.mKeyboardOrbView.getMeasuredWidth() + this.mSearchOrbsSpacing)) * prog));
            this.mKeyboardOrbView.setTranslationX(offset);
            this.mKeyboardContainer.setLayoutParams(lp);
            this.mKeyboardContainer.requestLayout();
        }
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        reset();
    }

    public void onAttachedToWindow() {
        boolean isTouchExplorationEnabled;
        super.onAttachedToWindow();
        setVisibile(true);
        AccessibilityManager am = (AccessibilityManager) this.mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null) {
            isTouchExplorationEnabled = am.isEnabled() && am.isTouchExplorationEnabled();
            OnClickListener listener = new C01746(isTouchExplorationEnabled);

            this.mMicOrbView.setOnClickListener(listener);

            if (this.mKeyboardOrbView != null) {
                this.mKeyboardOrbView.setOnClickListener(listener);
            }
        }
    }

    public void setLaunchListener(SearchLaunchListener listener) {
        this.mListener = listener;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        if (Util.isConfirmKey(event.getKeyCode())) {
            if (event.isLongPress()) {
                this.mEatDpadCenterKeyDown = true;
                Util.playErrorSound(getContext());
                return true;
            } else if (action == 1) {
                if (this.mEatDpadCenterKeyDown) {
                    this.mEatDpadCenterKeyDown = false;
                    return true;
                }
                this.mClickDeviceId = event.getDeviceId();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void animateIn() {
        setVisibile(true);
    }

    private void animateOut() {
        setVisibile(false);
        reset();
    }

    public void setActivated(boolean active) {
    }
}
