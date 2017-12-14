package com.xujl.baselibrary.mvp.presenter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xujl.baselibrary.Logger;
import com.xujl.baselibrary.mvp.common.BasePresenterHelper;
import com.xujl.baselibrary.mvp.port.Callback;
import com.xujl.baselibrary.mvp.port.IBaseModel;
import com.xujl.baselibrary.mvp.port.IBasePresenter;
import com.xujl.baselibrary.mvp.port.IBaseView;
import com.xujl.baselibrary.mvp.port.LifeCycleCallback;
import com.xujl.baselibrary.utils.ListUtils;
import com.xujl.baselibrary.utils.PermissionsHelper;
import com.xujl.rxlibrary.BaseObservable;
import com.xujl.rxlibrary.BaseObservableEmitter;
import com.xujl.rxlibrary.BaseObserver;
import com.xujl.rxlibrary.RxHelper;
import com.xujl.rxlibrary.RxLife;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * ━━━━━━神兽出没━━━━━━
 * 　　　┏┓　　　┏┓
 * 　　┏┛┻━━━┛┻┓
 * 　　┃　　　　　　　┃
 * 　　┃　　　━　　　┃
 * 　　┃　┳┛　┗┳　┃
 * 　　┃　　　　　　　┃
 * 　　┃　　　┻　　　┃
 * 　　┃　　　　　　　┃
 * 　　┗━┓　　　┏━┛
 * 　　　　┃　　　┃  神兽保佑
 * 　　　　┃　　　┃  代码无bug
 * 　　　　┃　　　┗━━━┓
 * 　　　　┃　　　　　　　┣┓
 * 　　　　┃　　　　　　　┏┛
 * 　　　　┗┓┓┏━┳┓┏┛
 * 　　　　　┃┫┫　┃┫┫
 * 　　　　　┗┻┛　┗┻┛
 * ━━━━━━感觉萌萌哒━━━━━━
 * Created by xujl on 2017/4/28.
 */

public abstract class BaseFragmentPresenter<V extends IBaseView, M extends IBaseModel> extends Fragment implements IBasePresenter
        , EasyPermissions.PermissionCallbacks {

    //<editor-fold desc="基础变量">
    /**
     * 视图
     */
    protected V mView;
    /**
     * 数据
     */
    protected M mModel;
    protected View mRootView;
    /**
     * 通用逻辑帮助类
     */
    protected BasePresenterHelper mPresenterHelper;
    /**
     * Fragment当前状态是否可见
     */
    protected boolean isVisible;
    /**
     * 是否已加载过
     */
    protected boolean isLoaded;
    /**
     * 是否初始化完成了控件
     */
    protected boolean isViewCompleted;
    /**
     * model是否已经初始化
     */
    protected boolean isModelInit;
    /**
     * 是否每次fragment重新显示都重新懒加载
     */
    protected boolean isEveryReload;
    protected LayoutInflater inflater;
    protected ViewGroup container;
    /**
     * 生命周期回调
     */
    private LifeCycleCallback mLifeCycleCallback;
    /**
     * 动态申请的权限
     * 特指界面加载完成后
     * 单独进行的权限请求
     */
    private String[] varyPermissions;
    protected RxLife mRxLife = new RxLife();
    //</editor-fold>

    //<editor-fold desc="抽象方法">

    /**
     * 初始化逻辑代码，由实现类实现
     *
     * @param savedInstanceState
     */
    protected abstract void initPresenter (Bundle savedInstanceState);

    /**
     * 自动创建view实例，用于关闭mvp模式下。抽象基类应实现此方法
     */
    protected abstract IBaseView autoCreateView ();

    /**
     * 自动创建model实例，用于关闭mvp模式下。抽象基类应实现此方法
     */
    protected abstract IBaseModel autoCreateModel ();

    //</editor-fold>

    //<editor-fold desc="模板方法">
    @Nullable
    @Override
    public View onCreateView (LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //首选加载项，在布局加载之前需要加载的东西
        firstLoading(savedInstanceState);
        //初始化view
        createView();
        //创建布局
        mRootView = createLayout(inflater, container, savedInstanceState);
        if (mLifeCycleCallback != null) {
            mLifeCycleCallback.onCreateLife(savedInstanceState);
        }
        return mRootView;
    }

    @Override
    public void onActivityCreated (@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mView.showLoading();
        //初始化控件
        mView.initView(this);
        isViewCompleted = true;
        isEveryReload = isEveryReload();
    }

    private void createView () {
        //不是mvp模式时，直接创建子类实例，不使用反射
        if (!isMVP()) {
            mView = (V) autoCreateView();
            return;
        }
        try {
            final Class<? extends V> viewClassType = getViewClassType();
            /*
             *   判断是否返回了view的实际类型的，返回则通过类类型反射创建实例,
             *   否则尝试使用全限定名进行反射创建对象
             */
            if (viewClassType != null) {
                mView = viewClassType.newInstance();
            } else {
                String className = getClass().getSimpleName();
                String viewClassName = classNameToCreateView(getViewClassPackageName(), className);
                mView = (V) Class.forName(viewClassName).newInstance();
            }
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (mView == null) {
            throw new NullPointerException("mView初始化失败");
        }
    }

    private void createModel () {
        //不是mvp模式时，直接创建子类实例，不使用反射
        if (!isMVP()) {
            mModel = (M) autoCreateModel();
            return;
        }
        try {
            final Class<? extends M> modelClassType = getModelClassType();
            /*
             *   判断是否返回了model的实际类型的，返回则通过类类型反射创建实例,
             *   否则尝试使用全限定名进行反射创建对象
             */

            if (modelClassType != null) {
                mModel = modelClassType.newInstance();
            } else {
                String className = getClass().getSimpleName();
                String modelClassName = classNameToCreateModel(getModelClassPackageName(), className);
                mModel = (M) Class.forName(modelClassName).newInstance();
            }
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (mModel == null) {
            throw new NullPointerException("mModel初始化失败");
        }
    }

    /**
     * 获取view包路径
     *
     * @return
     */
    protected String getViewClassPackageName () {
        return null;
    }

    /**
     * 获取model包路径
     *
     * @return
     */
    protected String getModelClassPackageName () {
        return null;
    }

    /**
     * 获取view实际类型
     *
     * @return
     */
    protected Class<? extends V> getViewClassType () {
        return null;
    }

    /**
     * 获取model实际类型
     *
     * @return
     */
    protected Class<? extends M> getModelClassType () {
        return null;
    }

    /**
     * 尝试通过包名和presenter类名创建view的全限定名
     *
     * @param viewClassPackageName
     * @param simpleName
     * @return
     */
    protected String classNameToCreateView (String viewClassPackageName, String simpleName) {
        return viewClassPackageName + "." + simpleName.replace("Presenter", "");
    }

    /**
     * 尝试通过包名和presenter类名创建model的全限定名
     *
     * @param viewClassPackageName
     * @param simpleName
     * @return
     */
    protected String classNameToCreateModel (String viewClassPackageName, String simpleName) {
        return viewClassPackageName + "." + simpleName.replace("Presenter", "") + "Model";
    }

    /**
     * 创建布局
     */
    protected View createLayout (LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.inflater = inflater;
        this.container = container;
        return mView.createUI(this);
    }

    /**
     * 加载布局之前需要执行的代码，例如请求全屏
     *
     * @param savedInstanceState
     */
    protected void firstLoading (Bundle savedInstanceState) {
        //TO DO
    }

    protected BasePresenterHelper getPresenterHelper () {
        if (mPresenterHelper == null) {
            mPresenterHelper = new BasePresenterHelper();
        }
        return mPresenterHelper;
    }

    public void setPresenterHelper (BasePresenterHelper presenterHelper) {
        mPresenterHelper = presenterHelper;
    }

    /**
     * fragment不可见回调
     */
    protected void onInVisible () {
        //TO DO
        if (isLoaded && isViewCompleted && isEveryReload) {
            isLoaded = false;
        }
    }

    /**
     * fragment可见回调
     */
    protected void onVisible () {
        //数据未加载过并且控件已经初始化完成时，进行懒加载
        if (!isLoaded && isViewCompleted) {
            isLoaded = true;
            RxHelper.onCreate(mRxLife)
                    .createDelay(120)
                    .newThreadToMain()
                    .run(new BaseObserver<Object>() {
                        @Override
                        public void onComplete () {
                            super.onComplete();
                            lazyLoad();
                            mView.dismissLoading();
                        }
                    });

        }
    }

    /**
     * 复写此方法实现懒加载数据
     */
    protected void lazyLoad () {
    }

    /**
     * 是否每次显示fragment都重新加载
     *
     * @return
     */
    protected boolean isEveryReload () {
        return false;
    }

    /**
     * 重置当前的加载状态标识，让下次fragment再次显示时重新加载
     */
    protected void resetLoadingState () {
        isLoaded = false;
    }

    @Override
    public void setUserVisibleHint (boolean isVisibleToUser) {//设置fragment是否可见
        super.setUserVisibleHint(isVisibleToUser);
        isVisible = isVisibleToUser;
        if (isVisible) {
            onVisible();
        } else {
            onInVisible();
        }
    }
    //</editor-fold>

    //<editor-fold desc="配置方法">

    public LayoutInflater getInflater () {
        return inflater;
    }

    public ViewGroup getContainer () {
        return container;
    }

    @Override
    public int getToolBarId () {
        return 0;
    }

    @Override
    public boolean enableToolBar () {
        return true;
    }

    @Override
    public boolean isAddParentLayout () {
        return true;
    }

    @Override
    public boolean enableDataBinding () {
        return false;
    }

    @Override
    public boolean isMVP () {
        return true;
    }
    //</editor-fold>

    //<editor-fold desc="动态权限">

    /**
     * @param permissions
     * @return 是否已经获取所有权限
     */
    protected boolean requestPermissions (String[] permissions) {
        //不需要权限或者需要的权限
        if (ListUtils.isEmpty(permissions) || EasyPermissions.hasPermissions(exposeContext(), permissions)) {
            //继续加载
            Logger.e("requestPermissions", "已获取到申请的所有权限");
            return true;
        }
        varyPermissions = permissions;
        //没有授权时发起授权
        EasyPermissions.requestPermissions(this, permissionsDescription(varyPermissions), 995, varyPermissions);
        return false;
    }

    /**
     * 申请的权限描述信息
     * 如果需要变更权限描述，请复写此方法
     * 并返回相关描述信息
     *
     * @param permissions
     * @return
     */
    protected String permissionsDescription (String[] permissions) {
        return "必要的权限:\t" +
                PermissionsHelper.getNoPermissionsStrings(exposeContext(), permissions) +
                "缺少权限会导致无法使用部分功能";
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 下面两个方法是实现EasyPermissions的EasyPermissions.PermissionCallbacks接口
     * 分别返回授权成功和失败的权限
     */
    @Override
    public void onPermissionsGranted (int requestCode, List<String> perms) {
        /*
         动态申请的权限varyPermissions不为空时
         表示当前正在进行的权限申请是动态申请
         申请全部被通过时直接回调权限申请成功方法
         */
        if (!ListUtils.isEmpty(varyPermissions)) {
            if (!ListUtils.isEmpty(perms) && perms.size() >= ListUtils.getSize(varyPermissions)) {
                permissionsComplete(varyPermissions);
                varyPermissions = null;
            }
        }
    }

    @Override
    public void onPermissionsDenied (final int requestCode, List<String> perms) {
        if (!ListUtils.isEmpty(perms)) {
            permissionsRefuse(varyPermissions);
            varyPermissions = null;
        }
    }

    /**
     * 权限授权成功
     * 动态申请的权限
     * 成功时会回调此方法
     */
    protected void permissionsComplete (String[] permissions) {

    }

    /**
     * 申请的权限被拒绝
     * 需要自定义显示时
     * 复写此方法
     */
    protected void permissionsRefuse (String[] permissions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("系统提示");
        builder.setCancelable(false);
        builder.setMessage("缺少权限:\t" + PermissionsHelper.getNoPermissionsStrings(exposeContext(), permissions) + "，是否跳转系统设置手动开启权限？");
        builder.setPositiveButton("去设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                Intent localIntent = new Intent();
                localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= 9) {
                    localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                    localIntent.setData(Uri.fromParts("package", getActivity().getPackageName(), null));
                } else if (Build.VERSION.SDK_INT <= 8) {
                    localIntent.setAction(Intent.ACTION_VIEW);
                    localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                    localIntent.putExtra("com.android.settings.ApplicationPkgName", getActivity().getPackageName());
                }
                startActivity(localIntent);
                exit();
            }
        });
        builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                exit();
            }
        });
        builder.show();
    }
    //</editor-fold>

    //<editor-fold desc="其他方法">

    /**
     * 生命周期回调，设置后各个生命周期方法会回调此接口
     *
     * @param mLifeCycleCallback
     */
    protected void setmLifeCycleCallback (LifeCycleCallback mLifeCycleCallback) {
        this.mLifeCycleCallback = mLifeCycleCallback;
    }

    /**
     * 恢复被回收的界面
     *
     * @param savedInstanceState
     */
    protected void resumePresenter (@NonNull Bundle savedInstanceState) {

    }

    //</editor-fold>

    //<editor-fold desc="Getter方法">
    @Override
    public Context exposeContext () {
        return getContext();
    }

    @Override
    public BaseActivityPresenter exposeActivity () {
        return (BaseActivityPresenter) getActivity();
    }

    @Override
    public IBaseView exposeView () {
        return mView;
    }

    @Override
    public IBaseModel exposeModel () {
        return mModel;
    }

    //</editor-fold>

    //<editor-fold desc="生命周期">
    @Override
    public void onStart () {
        super.onStart();
        if (mLifeCycleCallback != null) {
            mLifeCycleCallback.onStartLife();
        }
    }


    @Override
    public void onResume () {
        super.onResume();
        if (mLifeCycleCallback != null) {
            mLifeCycleCallback.onResumeLife();
        }
        if (isModelInit) {
            return;
        }
        isModelInit = true;
        subToMain(new Callback() {
                      @Override
                      public void callback () {
                          createModel();
                          mModel.initModel(BaseFragmentPresenter.this);
                      }
                  },
                new Callback() {
                    @Override
                    public void callback () {
                        initPresenter(null);
                        mView.dismissLoading();
                        //判断界面是否可见，如果需要在界面可见时才加载某些功能需要把相关代码写在lazyLoad ()方法中
                        isVisible = getUserVisibleHint();
                        if (isVisible) {
                            onVisible();
                        } else {
                            onInVisible();
                        }
                    }
                });

    }

    @Override
    public void onPause () {
        super.onPause();
        if (mLifeCycleCallback != null) {
            mLifeCycleCallback.onPauseLife();
        }
    }

    @Override
    public void onStop () {
        super.onStop();
        if (mLifeCycleCallback != null) {
            mLifeCycleCallback.onStopLife();
        }
    }

    @Override
    public void onDestroy () {
        mRxLife.destroyAll();
        super.onDestroy();
        if (mLifeCycleCallback != null) {
            mLifeCycleCallback.onDestroyLife();
        }
        mView = null;
        mModel = null;
    }
    //</editor-fold>

    /**
     * 子线程任务，完成后回调主线程
     *
     * @param taskCallback
     * @param callback
     */
    protected void subToMain (final Callback taskCallback, final Callback callback) {
        RxHelper.onCreate(mRxLife)
                .createNormal(new BaseObservable<Object>() {
                    @Override
                    public void emitAction (BaseObservableEmitter<Object> e) throws Exception {
                        if (taskCallback != null) {
                            taskCallback.callback();
                        }
                        Thread.sleep(70);
                        e.onComplete();
                    }
                })
                .newThreadToMain()
                .run(new BaseObserver<Object>() {
                    @Override
                    public void onComplete () {
                        super.onComplete();
                        if (callback != null) {
                            callback.callback();
                        }
                    }
                });
    }
}
