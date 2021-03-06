package com.xujl.baselibrary.mvp.common;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;

import com.xujl.baselibrary.mvp.port.IBasePresenter;

/**
 * Created by xujl on 2017/7/4.
 */

public class BaseToolBarModule {
    protected Toolbar mToolbar;
    private ActionBar mActionBar;

    /**
     * 使用此构造器会根据子类返回的toolBar的布局id自动创建toolBar并和内容布局拼接
     * 到一起
     *
     * @param activity
     * @param layoutId activity布局id
     */
    public BaseToolBarModule (Activity activity, int layoutId, LayoutConfig config) {

//        if (config.isEnableDataBinding()) {
//            mDataBinding = DataBindingUtil.setContentView(activity, layoutId);
//            mRootView = (ViewGroup) activity.findViewById(R.id.dataBindingRootLayout);
//            mContentLayout = mRootView.getChildAt(0);
//            //找到导航栏控件
//            findToolBar(activity, config);
//            return;
//        }
//        mRootView = new LinearLayout(activity);
//        mRootView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        ((LinearLayout) mRootView).setOrientation(LinearLayout.VERTICAL);
//        mContentLayout = LayoutInflater.from(activity).inflate(layoutId, null);
//        mContentLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        //找到导航栏控件
//        findToolBar(activity, config);
//        mRootView.addView(mContentLayout);
    }

    public View findToolBar (Context context, LayoutConfig config, View rootView) {
        int toolBarId = getToolBarId();
        //判断view或presenter是否传入了导航id，有就直接寻找导航栏
        if (config.getToolBarId() != 0) {
            toolBarId = config.getToolBarId();
            mToolbar = (Toolbar) rootView.findViewById(toolBarId);
            return mToolbar;
        }
        //没有则采用动态加载默认导航栏
        mToolbar = (Toolbar) LayoutInflater.from(context).inflate(getToolBarLayoutId(), null).findViewById(toolBarId);
        return mToolbar;
    }




    public void initSetting (IBasePresenter presenter) {
        if (presenter instanceof Fragment) {
            return;
        }
        AppCompatActivity activity = (AppCompatActivity) presenter;
        activity.setSupportActionBar(mToolbar);
        mActionBar = activity.getSupportActionBar();
    }

    /**
     * 返回toolbar布局id
     *
     * @return
     */
    protected int getToolBarLayoutId () {
        return 0;
    }

    /**
     * 返回toolbar id
     *
     * @return
     */
    protected int getToolBarId () {
        return 0;
    }

    public Toolbar getToolbar () {
        return mToolbar;
    }

    public ActionBar getActionBar () {
        return mActionBar;
    }

}
