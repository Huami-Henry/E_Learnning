package com.inesanet.dmedia.util;

import android.support.v4.util.Pools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileCopyExecutors {
    public FileInputStream input=null;
    public FileOutputStream output=null;
    public FileCopyExecutors(File old_file, String copy_file, long folderSize) {
        long copy_size = 0;
        try {
            input = new FileInputStream(old_file);
            output = new FileOutputStream(copy_file);
            byte[] b = new byte[1024 * 5];
            int len;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
                copy_size += len;
                int progress = (int) (copy_size * 1.0f / folderSize * 100);
                SPCache.putInt("cacheProgress", progress);
            }
            output.flush();
            output.close();
            input.close();
        } catch (Exception e) {

        }
    }
    private static final Pools.SynchronizedPool<FileCopyExecutors> sPool = new Pools.SynchronizedPool<>(10);

    public static FileCopyExecutors obtain(File old_file, String copy_file, long folderSize) {
        FileCopyExecutors instance = sPool.acquire();
        return (instance != null) ? instance : new FileCopyExecutors(old_file,copy_file,folderSize);
    }
    public void recycle() {
        sPool.release(this);
    }
}
