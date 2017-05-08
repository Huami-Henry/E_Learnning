package com.inesanet.dmedia.base;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.storage.StorageManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.inesanet.dmedia.R;
import com.inesanet.dmedia.util.TDevide;
import com.inesanet.dmedia.view.DilatingDotsProgressBar;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;

/**
 * Created by Administrator on 2017/5/3.
 */

public abstract class BaseActivity extends AppCompatActivity {
    public String TAG=BaseActivity.class.getSimpleName();
    private ActionBar actionBar;
    private static String lastToast=null;
    private static long lastToastTime=0;
    public LayoutInflater inflater;
    public ProgressDialog dialog_loadding;
    /**
     * 这个稀疏数组，网上说的是提高效率的
     */
    private final SparseArray<View> views = new SparseArray<>();
    private ProgressDialog progress_loadding;
    protected TextView tv_down_progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflater = LayoutInflater.from(this);
        if(getRequestedOrientation()!= ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        if (isNetworkConnected(this)) {
            setContentView(getLayout());
            initToolBar();
            init(savedInstanceState);
        } else {
            showToast("没有网络,请检查网络连接");
        }
    }
    /**
     * 将xml转换成json
     * @param xml
     * @return
     */
    protected String xml2JSON(String xml) {
        try {
            XmlToJson xmlToJson = new XmlToJson.Builder(xml).build();
            return xmlToJson.toJson().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    /**
     * 获取sd卡的路径 反射
     * @param mContext
     * @param is_vale
     * @return
     */
    protected String getStoragePath(Context mContext, boolean is_vale) {
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_vale == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private DilatingDotsProgressBar progress_bar;
    /**
     * 显示dialog
     */
    public void showDownLoading() {
        progress_loadding = new ProgressDialog(this, R.style.color_dialog);
        progress_loadding.setCanceledOnTouchOutside(false);
        progress_loadding.setCancelable(false);
        progress_loadding.show();
        progress_loadding.setContentView(R.layout.loading_dialog);
        progress_bar = (DilatingDotsProgressBar)progress_loadding.findViewById(R.id.progress);
        tv_down_progress = (TextView)progress_loadding.findViewById(R.id.tv_down_progress);
        progress_bar.showNow();
    }
    /**
     * 显示dialog
     */
    public void endDownLoading() {
        if (progress_loadding != null) {
            progress_bar.hideNow();
            progress_loadding.dismiss();
        }
    }
    /**
     * 显示dialog
     */
    public void endpregrossLoading() {
        if (progress_loadding != null) {
            progress_loadding.dismiss();
        }
    }
    /**
     * 返回一个具体的view对象
     * 这个就是借鉴的base-adapter-helper中的方法
     *
     * @param viewId
     * @param <T>
     * @return
     */
    public  <T extends View> T getView(int viewId) {
        View view = views.get(viewId);
        if (view == null) {
            view = findViewById(viewId);
            views.put(viewId, view);
        }
        return (T) view;
    }

    public void clearTop(){
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    public void showTop(){
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    /**
     * 隐藏标题栏
     */
    @TargetApi(19)
    public void hideTitleBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }
    @Override
    public Resources getResources() {
        Resources res = TApplication.getContext().getResources();
        Configuration config=new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config, res.getDisplayMetrics());
        return res;
    }
    /**
     * 检查网络链接状态
     * @param context
     * @return
     */
    public boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }
    public ActionBar getCustomBar() {
        return actionBar != null ? actionBar : null;
    }
    /**
     * 调用此方法可以初始化布局文件
     * @return
     */
    public abstract int getLayout();
    /**
     * 初始化参数的方法
     * @param savedInstanceState
     */
    public abstract void init(Bundle savedInstanceState);

    public AnimationSet getInAnimation(Context context) {
        AnimationSet out = new AnimationSet(context, null);
        AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
        alpha.setDuration(150);
        ScaleAnimation scale = new ScaleAnimation(0.6f, 1.0f, 0.6f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(150);
        out.addAnimation(alpha);
        out.addAnimation(scale);
        return out;
    }

    public AnimationSet getOutAnimation(Context context) {
        AnimationSet out = new AnimationSet(context, null);
        AlphaAnimation alpha = new AlphaAnimation(1.0f, 0.0f);
        alpha.setDuration(150);
        ScaleAnimation scale = new ScaleAnimation(1.0f, 0.6f, 1.0f, 0.6f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(150);
        out.addAnimation(alpha);
        out.addAnimation(scale);
        return out;
    }
    /**
     * 不带参数的跳转
     * @param activity
     * @param clas
     */
    public void startActivity(Activity activity, Class clas){
        Intent intent=new Intent(activity,clas);
        startActivity(intent);
    }

    /**
     * intent带参数的跳转
     * @param activity
     * @param clas
     */
    public void startActivity(Activity activity, Class clas, String key, String extras){
        Intent intent=new Intent(activity,clas);
        intent.putExtra(key, extras);
        startActivity(intent);
    }

    /**
     * 只允许 String Boolean Integer Float 类型传递过来
     * @param activity
     * @param clas
     * @param key
     * @param extras
     */
    public void startActivity(Activity activity, Class clas, String key, Object extras){
        Intent intent=new Intent(activity,clas);
        if (extras instanceof String) {
            intent.putExtra(key, (String) extras);
        }else if (extras instanceof Boolean) {
            intent.putExtra(key, (Boolean) extras);
        }else if (extras instanceof Integer) {
            intent.putExtra(key, (Integer) extras);
        }else if (extras instanceof Float) {
            intent.putExtra(key, (Float) extras);
        }
        startActivity(intent);
    }
    /**
     * intent带参数的跳转
     * @param activity
     * @param clas
     */
    public void startActivityForResult(Activity activity, Class clas, String key, String extras, int requestCode){
        Intent intent=new Intent(activity,clas);
        intent.putExtra(key, extras);
        startActivityForResult(intent, requestCode);
    }
    /**
     * intent带参数的跳转
     * @param activity
     * @param clas
     */
    public void startActivityForResult(Activity activity, Class clas, int requestCode){
        Intent intent=new Intent(activity,clas);
        startActivityForResult(intent, requestCode);
    }
    /**
     * intent带参数的跳转
     * @param activity
     * @param clas
     */
    public void startActivitySingle(Activity activity, Class clas, String key, String extras){
        Intent intent=new Intent(activity,clas);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra(key, extras);
        startActivity(intent);
    }
    /**
     * intent带boolean参数的跳转
     * @param activity
     * @param clas
     */
    public void startActivity(Activity activity, Class clas, String key, boolean is_edit, String[] keys, String[] extras){
        Intent intent=new Intent(activity,clas);
        intent.putExtra(key,is_edit);
        for (int i=0;i<keys.length;i++){
            intent.putExtra(keys[i],extras[i]);
        }
        startActivity(intent);
    }

    /**
     * 只允许 String Boolean Integer Float 类型传递过来
     * @param activity
     * @param clas
     * @param keys
     * @param extras
     */
    public void startActivity(Activity activity, Class clas, String[] keys, Object[] extras){
        Intent intent=new Intent(activity,clas);
        for (int i=0;i<keys.length;i++){
            if (extras[i] instanceof String) {
                intent.putExtra(keys[i], (String) extras[i]);
            }else if (extras[i] instanceof Boolean) {
                intent.putExtra(keys[i], (Boolean) extras[i]);
            }else if (extras[i] instanceof Integer) {
                intent.putExtra(keys[i], (Integer) extras[i]);
            }else if (extras[i] instanceof Float) {
                intent.putExtra(keys[i], (Float) extras[i]);
            }
        }
        startActivity(intent);
    }
    /**
     * intent带多个参数的跳转
     * @param activity
     * @param clas
     */
    public void startActivity(Activity activity, Class clas, String[] keys, String[] extras){
        Intent intent=new Intent(activity,clas);
//        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        for (int i=0;i<keys.length;i++){
            intent.putExtra(keys[i],extras[i]);
        }
        startActivity(intent);
    }
    /**
     * intent带多个参数的跳转
     * @param activity
     * @param clas
     */
    public void startActivitySingle(Activity activity, Class clas, String[] keys, String[] extras){
        Intent intent=new Intent(activity,clas);
        for (int i=0;i<keys.length;i++){
            intent.putExtra(keys[i],extras[i]);
        }
        startActivity(intent);
    }
    /**
     * intent带多个参数的跳转回传
     * @param activity
     * @param classes
     */
    public void startActivityForResult(Activity activity, Class classes, String[] keys, String[] extras, int requestCode){
        Intent intent=new Intent(activity,classes);
        for (int i=0;i<keys.length;i++){
            intent.putExtra(keys[i],extras[i]);
        }
        startActivityForResult(intent,requestCode);
    }
    /**
     * 带有bundle的跳转
     * @param activity
     * @param bundle
     * @param key
     * @param clas
     */
    public void startActivity(Activity activity, Bundle bundle, String key, Class clas){
        Intent intent=new Intent(activity,clas);
//        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(key, bundle);
        startActivity(intent);
    }

    /**
     * 实现parcelable接口的参数
     * @param activity
     * @param parcelable
     * @param key
     * @param clas
     */
    public void startActivity(Activity activity, Parcelable parcelable, String key, Class clas){
        Intent intent=new Intent(activity,clas);
//        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(key,parcelable);
        startActivity(intent);
    }

    /**
     * 初始化toolbar
     */
    protected void initToolBar(){

    }
    public void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    /**
     * 弹出框toast
     * @param message
     * @param duration
     * @param icon
     * @param gravity
     */
    public void showToast(String message, int duration, int icon,
                          int gravity) {
        if (message != null && !message.equalsIgnoreCase("")) {
            long time = System.currentTimeMillis();
            if (!message.equalsIgnoreCase(lastToast)
                    || Math.abs(time - lastToastTime) > 2000) {
                View view = LayoutInflater.from(TApplication.getContext()).inflate(
                        R.layout.toast_view, null);
                ((TextView) view.findViewById(R.id.title_tv)).setText(message);
                if (icon != 0) {
                    ((ImageView) view.findViewById(R.id.icon_iv))
                            .setImageResource(icon);
                    (view.findViewById(R.id.icon_iv))
                            .setVisibility(View.VISIBLE);
                }
                Toast toast = new Toast(this);
                toast.setView(view);
                toast.setGravity(Gravity.BOTTOM | gravity, 0, TDevide.dip2px(84, this));
                toast.setDuration(duration);
                toast.show();
                lastToast = message;
                lastToastTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog_loadding != null) {
            dialog_loadding.dismiss();
        }
    }

    /**
     * 验证是否是手机号码
     * @param mobile
     * @return
     */
    public static boolean checkMobile(String mobile) {
        String regex = "(\\+\\d+)?1[3458]\\d{9}$";
        return Pattern.matches(regex, mobile);
    }
}
