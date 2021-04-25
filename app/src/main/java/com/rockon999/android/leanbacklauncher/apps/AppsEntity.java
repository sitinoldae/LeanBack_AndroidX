package com.rockon999.android.leanbacklauncher.apps;

import android.annotation.SuppressLint;
import android.content.Context;

import com.rockon999.android.leanbacklauncher.R;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;

public class AppsEntity {
    private AppsDbHelper mDbHelper;
    private HashMap<String, Long> mLastOpened;
    private HashMap<String, Long> mOrder;
    private String mPackageName;
    private boolean mIsFavorite;

    public AppsEntity(Context context, AppsDbHelper helper, String packageName, long lastOpenTime, long initialOrder, boolean isFavorite) {
        this(context, helper, packageName, isFavorite);
        setLastOpenedTimeStamp(null, lastOpenTime);
        setOrder(null, initialOrder);
    }

    public AppsEntity(Context ctx, AppsDbHelper helper, String packageName, boolean isFavorite) {
        this.mLastOpened = new HashMap<>();
        this.mOrder = new HashMap<>();
        this.mDbHelper = helper;
        this.mPackageName = packageName;
        this.mIsFavorite = isFavorite;
    }

    public Set<String> getComponents() {
        return this.mOrder.keySet();
    }

    public void setLastOpenedTimeStamp(String component, long timeStamp) {
        this.mLastOpened.put(component, timeStamp);
    }

    public long getLastOpenedTimeStamp(String component) {
        Long lastOpened = this.mLastOpened.get(component);
        if (lastOpened == null) {
            lastOpened = this.mLastOpened.get(null);
            if (lastOpened == null) {
                lastOpened = 0L;
            }
        }
        return lastOpened;
    }

    public long getOrder(String component) {
        Long order = this.mOrder.get(component);
        if ((order == null || order == 0) && this.mOrder.keySet().size() == 1) {
            order = this.mOrder.values().iterator().next();
            if (order == null) {
                order = 0L;
            }
            setOrder(component, order);
        }
        if (order != null) {
            return order;
        }
        return 0;
    }

    public void setOrder(String component, long order) {
        this.mOrder.put(component, order);
    }

    public String getKey() {
        return this.mPackageName;
    }

    @SuppressLint("PrivateResource")
    public synchronized void onAction(int actionType, String component, String group) {
        long time = new Date().getTime();
        if (this.mDbHelper.getMostRecentTimeStamp() >= time) {
            time = this.mDbHelper.getMostRecentTimeStamp() + 1;
        }
        switch (actionType) {
            case R.styleable.Preference_android_icon /*0*/:
                if (getLastOpenedTimeStamp(component) == 0) {
                    setLastOpenedTimeStamp(component, time);
                    break;
                }
                break;
            case R.styleable.RecyclerView_android_descendantFocusability /*1*/:
                setLastOpenedTimeStamp(component, time);
                break;
            case R.styleable.Preference_android_layout /*3*/:
                this.mLastOpened.clear();
                break;
        }
    }

    public boolean isFavorite() {
        return mIsFavorite;
    }

    public void setFavorited(boolean category) {
        this.mIsFavorite = category;
    }
}
