package com.inesanet.dmedia;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.google.gson.Gson;
import com.inesanet.dmedia.acceptNet.BaseNetDataBiz;
import com.inesanet.dmedia.acceptNet.OkHttp;
import com.inesanet.dmedia.base.BaseActivity;
import com.inesanet.dmedia.base.BaseConsts;
import com.inesanet.dmedia.biz.CopyFileBiz;
import com.inesanet.dmedia.model.ConfigModel;
import com.inesanet.dmedia.util.SPCache;
import com.inesanet.dmedia.zip.CompressStatus;
import com.inesanet.dmedia.zip.ZipUtil;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements View.OnClickListener,BaseNetDataBiz.RequestListener,CopyFileBiz.CopyProgressListener{
    private static final String DOWN_CONFIG = "DOWN_CONFIG";
    private static final String DOWN_UPDATE_FILE = "DOWN_UPDATE_FILE";
    private TextView update;
    private LinearLayout dialog_view;
    private final String downloadUrl = BaseConsts.BASE_FILE_CONFIG;
    private TextView version_info;
    private TextView ignore_update;
    private RelativeLayout progress_view;
    private RoundCornerProgressBar progress;
    private TextView tv_progress;
    private String ExternalStorageDirectory;
    @Override
    public int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    public void init(Bundle savedInstanceState) {
        ExternalStorageDirectory = getStoragePath(this, false);
        initView();
        downLoadInfoFile();
    }
    private void initView() {
        progress_view = getView(R.id.progress_view);
        progress = getView(R.id.progress);
        tv_progress = getView(R.id.tv_progress);
        update = getView(R.id.update);
        dialog_view = getView(R.id.dialog_view);
        version_info = getView(R.id.version_info);
        ignore_update = getView(R.id.ignore_update);
        update.setOnClickListener(this);
        ignore_update.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ignore_update:
                dialog_view.setVisibility(View.GONE);
                break;
            case R.id.update:
                dialog_view.setVisibility(View.GONE);
                download();
                break;
        }
    }
    private boolean need_update = false;

    //下载完配置信息文件之后来判断是否需要更新
    public void downLoadInfoFile(){
        biz_down_config = new BaseNetDataBiz(this);
        biz_down_config.downloadFile(downloadUrl,DOWN_CONFIG);
    }

    private BaseNetDataBiz biz_down_config;
    private void download() {
        if (!TextUtils.isEmpty(update_url)) {
            progress_view.setVisibility(View.VISIBLE);
            setProgressBar(0, "正在下载：");
            //开始下载更新包
            biz_down_config = new BaseNetDataBiz(this);
            biz_down_config.downloadFile(update_url, DOWN_UPDATE_FILE);
            OkHttp.asyncPost(update_url, DOWN_UPDATE_FILE, new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {

                }
                @Override
                public void onResponse(final Response response) throws IOException {
                    startDownUpdateFileDecompress(response);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //解压文件
                            final File file = new File(ExternalStorageDirectory + "/11111/");
                            if (!file.exists()) {
                                file.mkdir();
                            }
                            String zipFilePath = ExternalStorageDirectory + getNameFromUrl(update_url);
                            File zipFile = new File(zipFilePath);
                            try
                            {
                                ZipUtil.unZipFileWithProgress(zipFile,ExternalStorageDirectory+"/11111/",handler,false);
                            }catch (ZipException e){
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
        }
    }

    /**
     * 下载进度设置
     * @param progress_size
     * @param tip_progress
     */
    public void setProgressBar(int progress_size,String tip_progress){
        progress.setProgress(progress_size);
        tv_progress.setText(tip_progress+progress_size+"%");
    }
    /**
     * 下载文件 结束之后解压,并且删除压缩包  重启盒子
     * @param response
     */
    private void startDownUpdateFileDecompress(final Response response) {
        //开始准备下载文件
        InputStream is = null;
        byte[] buf = new byte[2048];
        int len = 0;
        FileOutputStream fos = null;
        // 储存下载文件的目录
        final String ExternalStorageDirectory = getStoragePath(MainActivity.this, false);
        try {
            is = response.body().byteStream();
            final long total = response.body().contentLength();
            File file = new File(ExternalStorageDirectory, getNameFromUrl(update_url));
            fos = new FileOutputStream(file);
            long sum = 0;
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
                sum += len;
                final int progress = (int) (sum * 1.0f / total * 100);
                if (progress != SPCache.getInt(update_url, 0)) {
                    SPCache.putInt(update_url,progress);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setProgressBar(progress,"下载进度：");
                        }
                    });
                }
            }
            fos.flush();
            // 下载完成
        } catch (Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast("更新失败,重启后重新操作.");
                }
            });
        } finally {
            SPCache.remove(update_url);
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
            }
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
            }
        }
    }

    private long folderSize;
    /**
     * 解压进度
     */
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case CompressStatus.START:
                    setProgressBar(1, "开始解压");
                    break;
                case CompressStatus.HANDLING:
                    Bundle b = msg.getData();
                    setProgressBar(b.getInt(CompressStatus.PERCENT),"解压进度: ");
                    break;
                case CompressStatus.COMPLETED:
                    setProgressBar(1, "升级进度: ");
                    final CopyFileBiz biz = new CopyFileBiz(MainActivity.this);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            folderSize = biz.getFolderSize(ExternalStorageDirectory + "/11111/11111_dl/");
                            biz.copyFolder(ExternalStorageDirectory + "/11111/11111_dl/", ExternalStorageDirectory + "/11111/", folderSize);
                            biz.DeleteFolder(ExternalStorageDirectory + "/11111/11111_dl/",folderSize);
                        }
                    }).start();

                    break;
                case CompressStatus.ERROR:
                    setProgressBar(1, "升级失败");
                    break;
            }
        }
    };
    private Handler handler_update=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int progress = msg.what;
            switch (update_tag) {
                case "copy":
                    setProgressBar(progress, "升级进度: ");
                    break;
                case "del":
                    setProgressBar(progress, "文件清理: ");
                    if (progress >= 100) {
                        progress_view.setVisibility(View.GONE);
                        SPCache.putString(BaseConsts.CONFIG_VERSION, update_version);
                        restartApp();
                    }
                    break;
            }
        }
    };
    /**
     * 下载文件名
     * @param downloadUrl
     * @return
     */
    public String getNameFromUrl(String downloadUrl) {
        return downloadUrl.substring(downloadUrl.lastIndexOf("/"), downloadUrl.length());
    }

    private String update_url;//当前需要更新的链接
    private String update_version;
    private List<ConfigModel.VersionInfo> versionInfos = new ArrayList<>();//缓存所有的版本信息
    @Override
    public void onResponse(BaseNetDataBiz.Model model) {
        String xml_str = model.getJson();
        String tag = model.getTag();
        if (tag.equals(DOWN_CONFIG)) {//下载配置信息
            String json = xml2JSON(xml_str);
            if (!TextUtils.isEmpty(json)) {
                getConfigInfo(json);
            }
        }
    }

    /**
     * 获取当前的配置信息 并且判断是否需要更新
     * @param json
     */
    private void getConfigInfo(String json) {
        //1：解析获取的配置信息
        Gson gson = new Gson();
        ConfigModel configModel = gson.fromJson(json, ConfigModel.class);
        for (ConfigModel.VersionInfo info : configModel.getVersion()) {
            versionInfos.add(info);
        }
        //2:获取缓存过的配置信息
        String cache_version = SPCache.getString(BaseConsts.CONFIG_VERSION, "");//最多有一个数据 version_name  1.0.1
        // 3: 比较版本号之间的差异和第几个相同
        //1、都不相同的话就从第一个开始更新
        if (TextUtils.isEmpty(cache_version) && !TextUtils.isEmpty(json)) {//这样就代表没有更新过所以弹出更新对话框
            need_update = true;
            dialog_view.setVisibility(View.VISIBLE);
            getDownloadUrl(0);//获取配置文件中的第一个文件作为更新信息
            version_info.setText("新版本:"+update_version);
        } else {
            //2、有一个相同的话就从下一个开始更新
            int index = 0;//判断第几个相同
            for (ConfigModel.VersionInfo info : configModel.getVersion()) {
                if (cache_version.equals(info.getLatestversion())) {//判断当前version是否跟我的配置文件有相同的
                    //和我的配置文件中的version相同
                    if (index < configModel.getVersion().size() - 1) {
                        need_update = true;
                        getDownloadUrl(index + 1);//获取配置文件中需要更新的url
                        dialog_view.setVisibility(View.VISIBLE);
                        version_info.setText("当前版本:"+cache_version+"\n更新版本:"+update_version);
                        //更新完成之后将当前的版本号写入存储文件
                    } else if (index == configModel.getVersion().size() - 1) {
                        //更新到最后一个了所以推出当前方法
                        return;
                    }
                }
                index++;
            }
            if (!need_update) {
                index = 0;
                getDownloadUrl(index);//获取配置文件中需要更新的url
                version_info.setText("当前版本:"+cache_version+"\n新版本:"+update_version);
            }
        }
    }
    /**
     * 获取需要更新的 url
     * @param position
     */
    public void getDownloadUrl(int position){
        update_url=versionInfos.get(position).getUrl();
        update_version = versionInfos.get(position).getLatestversion();
    }
    @Override
    public void OnFailure(Request r, IOException o) {
        if (!r.tag().equals(DOWN_CONFIG)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast("更新失败,请检查网络配置情况,稍后尝试!");
                }
            });
        }
    }
    public void restartApp(){
        Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent restartIntent = PendingIntent.getActivity(getApplicationContext(), 123456, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, restartIntent); // 1秒钟后重启应用
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    private long count_size;
    private long del_size;
    private String update_tag="copy";
    @Override
    public void showProgress(final long progress, final String tag) {
        update_tag = tag;
        int progress_update=0;
        switch (tag) {
            case "copy":
                count_size += progress;
                progress_update = (int) (count_size * 1.0f / folderSize * 100);
                if (progress_update > 100) {
                    progress_update = 100;
                }
                break;
            case "del":
                del_size += progress;
                progress_update = (int) (del_size * 1.0f / folderSize * 100);
                if (progress_update > 100) {
                    progress_update = 100;
                }
                break;
        }
        handler_update.sendEmptyMessage(progress_update);
    }
}
