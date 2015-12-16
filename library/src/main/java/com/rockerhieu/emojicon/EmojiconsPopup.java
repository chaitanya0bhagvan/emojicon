package com.rockerhieu.emojicon;/*
 * Copyright 2014 Ankush Sachdeva
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.PopupWindow;

import com.rockerhieu.emojicon.emoji.Emojicon;
import com.rockerhieu.emojicon.emoji.Nature;
import com.rockerhieu.emojicon.emoji.Objects;
import com.rockerhieu.emojicon.emoji.People;
import com.rockerhieu.emojicon.emoji.Places;
import com.rockerhieu.emojicon.emoji.Symbols;

import java.util.Arrays;
import java.util.List;

/**
 * @author Ankush Sachdeva (sankush@yahoo.co.in).
 * @author Chaitanya Bhagvan
 */

public class EmojiconsPopup extends PopupWindow implements ViewPager.OnPageChangeListener, EmojiconRecents {
    private int mEmojiTabLastSelectedIndex = -1;
    private EmojiconRecentsManager mRecentsManager;
    private int keyBoardHeight = 0;
    private Boolean pendingOpen = false;
    private Boolean isOpened = false;
    EmojiconGridView.OnEmojiconClickedListener onEmojiconClickedListener;
    private OnEmojiconBackspaceClickedListener onEmojiconBackspaceClickedListener;
    private OnSoftKeyboardOpenCloseListener onSoftKeyboardOpenCloseListener;
    private View rootView;
    private Context mContext;

    private ViewPager emojisPager;
    /**
     * Constructor
     * @param rootView	The top most layout in your view hierarchy. The difference of this view and the screen height will be used to calculate the keyboard height.
     * @param mContext The context of current activity.
     */
    public EmojiconsPopup(View rootView, Context mContext){
        super(mContext);
        this.mContext = mContext;
        this.rootView = rootView;
        View customView = createCustomView();
        setContentView(customView);
        setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        setBackgroundDrawable(new BitmapDrawable());
        //default size
//        setSize(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        setSize(LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.keyboard_height));
        //setSize((int) mContext.getResources().getDimension(R.dimen.keyboard_height), LayoutParams.MATCH_PARENT);
    }
    /**
     * Set the listener for the event of keyboard opening or closing.
     */
    public void setOnSoftKeyboardOpenCloseListener(OnSoftKeyboardOpenCloseListener listener){
        this.onSoftKeyboardOpenCloseListener = listener;
    }

    /**
     * Set the listener for the event when any of the emojicon is clicked
     */
    public void setOnEmojiconClickedListener(EmojiconGridView.OnEmojiconClickedListener listener){
        this.onEmojiconClickedListener = listener;
    }

    /**
     * Set the listener for the event when backspace on emojicon popup is clicked
     */
    public void setOnEmojiconBackspaceClickedListener(OnEmojiconBackspaceClickedListener listener){
        this.onEmojiconBackspaceClickedListener = listener;
    }

    /**
     * Use this function to show the emoji popup.
     * NOTE: Since, the soft keyboard sizes are variable on different android devices, the
     * library needs you to open the soft keyboard atleast once before calling this function.
     * If that is not possible see showAtBottomPending() function.
     *
     */
    public void showAtBottom(){
        showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
    }
    /**
     * Use this function when the soft keyboard has not been opened yet. This
     * will show the emoji popup after the keyboard is up next time.
     * Generally, you will be calling InputMethodManager.showSoftInput function after
     * calling this function.
     */
    public void showAtBottomPending(){
        if(isKeyBoardOpen())
            showAtBottom();
        else
            pendingOpen = true;
    }

    /**
     *
     * @return Returns true if the soft keyboard is open, false otherwise.
     */
    public Boolean isKeyBoardOpen(){
        return isOpened;
    }

    /**
     * Dismiss the popup
     */
    @Override
    public void dismiss() {
        super.dismiss();
        EmojiconRecentsManager
                .getInstance(mContext).saveRecents();
    }

    /**
     * Call this function to resize the emoji popup according to your soft keyboard size
     */
    public void setSizeForSoftKeyboard(){
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(isOpened) {
                    return;
                }
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);

                int screenHeight = getUsableScreenHeight();
                int heightDifference = screenHeight
                        - (r.bottom - r.top);

                Resources resources = mContext.getResources();
                int statusBarId = resources
                        .getIdentifier("status_bar_height",
                                "dimen", "android");

                //add support translucent status bar ???
//				if (!((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP))) {
//					if (statusBarId > 0) {
//						heightDifference -= mContext.getResources()
//								.getDimensionPixelSize(statusBarId);
//						//                        Log.e("keyboard height", "resourceId > 0" + heightDifference);
//					}
//				}

                if (statusBarId > 0) {
                    heightDifference -= resources
                            .getDimensionPixelSize(statusBarId);
                }
                //Resolved using http://stackoverflow.com/a/16608481/2853322
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    int navBarId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
                    boolean hasMenuKey;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        hasMenuKey = ViewConfiguration.get(mContext).hasPermanentMenuKey();
                    }
                    else
                        hasMenuKey = true; //Skip has menu key below ICS
                    boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

                    //This is a hack for lollipop
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        hasBackKey = false;
                    }

                    if (navBarId > 0 && !hasMenuKey && !hasBackKey) {
                        heightDifference -= resources.getDimensionPixelSize(navBarId);
                    }
                }

//				int statusBarResourceId = mContext.getResources()
//						.getIdentifier("status_bar_height",
//								"dimen", "android");
//				if (statusBarResourceId > 0) {
//					heightDifference -= mContext.getResources()
//							.getDimensionPixelSize(statusBarResourceId);
//				}
//
//				int navBarResourceId = mContext.getResources()
//						.getIdentifier("navigation_bar_height",
//								"dimen", "android");
//				if (navBarResourceId > 0) {
//					heightDifference -= mContext.getResources().getDimensionPixelSize(navBarResourceId);
//				}

                if (heightDifference > 150) {
                    keyBoardHeight = heightDifference;
                    setSize(LayoutParams.MATCH_PARENT, keyBoardHeight);
                    if (!isOpened) {
                        if (onSoftKeyboardOpenCloseListener != null)

                            onSoftKeyboardOpenCloseListener.onKeyboardOpen(keyBoardHeight);
                    }
                    isOpened = true;
                    if (pendingOpen) {
                        showAtBottom();
                        pendingOpen = false;
                    }
                } else {
                    isOpened = false;
                    if (onSoftKeyboardOpenCloseListener != null)
                        onSoftKeyboardOpenCloseListener.onKeyboardClose();
                }
            }
        });
    }

    private int getUsableScreenHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics metrics = new DisplayMetrics();

            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);

            return metrics.heightPixels;
        }
        else {
            return rootView.getRootView().getHeight();
        }
    }

    /**
     * Manually set the popup window size
     * @param width Width of the popup
     * @param height Height of the popup
     */
    public void setSize(int width, int height){
        setWidth(width);
        setHeight(height);
    }

    private View createCustomView() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.emojicons, null, false);
        emojisPager = (ViewPager) view.findViewById(R.id.emojis_pager);
        emojisPager.setOnPageChangeListener(this);
        EmojiconRecents recents = this;

        PagerAdapter mEmojisAdapter = new EmojisPagerAdapter(
                Arrays.asList(
                        new EmojiconRecentsGridView(mContext, null, null, this),
                        new EmojiconGridView(mContext, People.DATA, recents, this),
                        new EmojiconGridView(mContext, Nature.DATA, recents, this),
                        new EmojiconGridView(mContext, Objects.DATA, recents, this),
                        new EmojiconGridView(mContext, Places.DATA, recents, this),
                        new EmojiconGridView(mContext, Symbols.DATA, recents, this)
                )
        );

        emojisPager.setAdapter(mEmojisAdapter);

        final TabLayout emojisTab = (TabLayout) view.findViewById(R.id.emojis_tab);
        emojisTab.setTabMode(TabLayout.MODE_FIXED);
        emojisTab.setTabGravity(TabLayout.GRAVITY_FILL);
        emojisTab.setupWithViewPager(emojisPager);
        emojisTab.setTabsFromPagerAdapter(mEmojisAdapter);

        setupTab(emojisTab);

        view.findViewById(R.id.emojis_backspace).setOnTouchListener(new RepeatListener(1000, 50, new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(onEmojiconBackspaceClickedListener != null)
                    onEmojiconBackspaceClickedListener.onEmojiconBackspaceClicked(v);
            }
        }));

        // get last selected page
        mRecentsManager = EmojiconRecentsManager.getInstance(view.getContext());
        int page = mRecentsManager.getRecentPage();
        // last page was recents, check if there are recents to use
        // if none was found, go to page 1
        if (page == 0 && mRecentsManager.size() == 0) {
            page = 1;
        }

        if (page == 0) {
            onPageSelected(page);
        }
        else {
            emojisPager.setCurrentItem(page, false);
        }
        return view;
    }

    private void setupTab(TabLayout layout) {
        final LayoutInflater inflater = LayoutInflater.from(layout.getContext());
        final Resources res = layout.getContext().getResources();
        int tabcount = layout.getTabCount();
        for(int i = 0; i < tabcount; i++) {
            TabLayout.Tab tab = layout.getTabAt(i);
            switch (i) {
                case 0:
                    tab.setIcon(res.getDrawable(R.drawable.ic_emoji_recent_light));
                    break;
                case 1:
                    tab.setIcon(res.getDrawable(R.drawable.ic_emoji_people_light));
                    break;
                case 2:
                    tab.setIcon(res.getDrawable(R.drawable.ic_emoji_nature_light));
                    break;
                case 3:
                    tab.setIcon(res.getDrawable(R.drawable.ic_emoji_objects_light));
                    break;
                case 4:
                    tab.setIcon(res.getDrawable(R.drawable.ic_emoji_places_light));
                    break;
                case 5:
                    tab.setIcon(res.getDrawable(R.drawable.ic_emoji_symbols_light));
                    break;
            }
        }
    }

    @Override
    public void addRecentEmoji(Context context, Emojicon emojicon) {
        EmojiconRecentsGridView fragment = ((EmojisPagerAdapter)emojisPager.getAdapter()).getRecentFragment();
        fragment.addRecentEmoji(context, emojicon);
    }


    @Override
    public void onPageScrolled(int i, float v, int i2) {
    }

    @Override
    public void onPageSelected(int i) {
        if (mEmojiTabLastSelectedIndex == i) {
            return;
        }
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                mEmojiTabLastSelectedIndex = i;
                mRecentsManager.setRecentPage(i);
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    private static class EmojisPagerAdapter extends PagerAdapter {
        private List<EmojiconGridView> views;
        public EmojiconRecentsGridView getRecentFragment(){
            for (EmojiconGridView it : views) {
                if(it instanceof EmojiconRecentsGridView)
                    return (EmojiconRecentsGridView)it;
            }
            return null;
        }
        public EmojisPagerAdapter(List<EmojiconGridView> views) {
            super();
            this.views = views;
        }

        @Override
        public int getCount() {
            return views.size();
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = views.get(position).rootView;
            ((ViewPager)container).addView(v, 0);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object view) {
            ((ViewPager)container).removeView((View)view);
        }

        @Override
        public boolean isViewFromObject(View view, Object key) {
            return key == view;
        }
    }

    /**
     * A class, that can be used as a TouchListener on any view (e.g. a Button).
     * It cyclically runs a clickListener, emulating keyboard-like behaviour. First
     * click is fired immediately, next before initialInterval, and subsequent before
     * normalInterval.
     * <p/>
     * <p>Interval is scheduled before the onClick completes, so it has to run fast.
     * If it runs slow, it does not generate skipped onClicks.
     */
    public static class RepeatListener implements View.OnTouchListener {

        private Handler handler = new Handler();

        private int initialInterval;
        private final int normalInterval;
        private final View.OnClickListener clickListener;

        private Runnable handlerRunnable = new Runnable() {
            @Override
            public void run() {
                if (downView == null) {
                    return;
                }
                handler.removeCallbacksAndMessages(downView);
                handler.postAtTime(this, downView, SystemClock.uptimeMillis() + normalInterval);
                clickListener.onClick(downView);
            }
        };

        private View downView;

        /**
         * @param initialInterval The interval before first click event
         * @param normalInterval  The interval before second and subsequent click
         *                        events
         * @param clickListener   The OnClickListener, that will be called
         *                        periodically
         */
        public RepeatListener(int initialInterval, int normalInterval, View.OnClickListener clickListener) {
            if (clickListener == null)
                throw new IllegalArgumentException("null runnable");
            if (initialInterval < 0 || normalInterval < 0)
                throw new IllegalArgumentException("negative interval");

            this.initialInterval = initialInterval;
            this.normalInterval = normalInterval;
            this.clickListener = clickListener;
        }

        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downView = view;
                    handler.removeCallbacks(handlerRunnable);
                    handler.postAtTime(handlerRunnable, downView, SystemClock.uptimeMillis() + initialInterval);
                    clickListener.onClick(view);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE:
                    handler.removeCallbacksAndMessages(downView);
                    downView = null;
                    return true;
            }
            return false;
        }
    }

    public interface OnEmojiconBackspaceClickedListener {
        void onEmojiconBackspaceClicked(View v);
    }

    public interface OnSoftKeyboardOpenCloseListener{
        void onKeyboardOpen(int keyBoardHeight);
        void onKeyboardClose();
    }
}
