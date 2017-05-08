package com.inesanet.dmedia.biz;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class CopyFileBiz {
    private CopyProgressListener listener;
    public CopyFileBiz(CopyProgressListener listener) {
        this.listener = listener;
    }

    /**
     * 计算文件夹的大小
     * @return
     */
    public long getFolderSize(String file_name){
        File file = new File(file_name);
        long size = 0;
        try {
            java.io.File[] fileList = file.listFiles();
            for (int i = 0; i < fileList.length; i++)
            {
                if (fileList[i].isDirectory())
                {
                    size +=getFolderSize(fileList[i].getAbsolutePath()+"/");

                }else{
                    size +=fileList[i].length();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    private static double copy_size = 0;
    private Map<String, Long> copy_cache = new HashMap<>();

    /**
     * 复制整个文件夹内容
     *
     * @return boolean
     */
    public void copyFolder(String oldPath, String newPath, long folderSize) {

        copy_cache.put("copy_progress", 0L);
        try {
            (new File(newPath)).mkdirs(); //如果文件夹不存在 则建立新文件夹
            File a = new File(oldPath);
            String[] file = a.list();
            File temp = null;
            long size = 0;
            for (int i = 0; i < file.length; i++) {
                if (oldPath.endsWith(File.separator)) {
                    temp = new File(oldPath + file[i]);
                } else {
                    temp = new File(oldPath + File.separator + file[i]);
                }
                if (temp.isFile()) {
                    FileInputStream input = new FileInputStream(temp);
                    FileOutputStream output = new FileOutputStream(newPath + "/" + (temp.getName()).toString());
                    byte[] b = new byte[1024 * 5];
                    int len;
                    while ((len = input.read(b)) != -1) {
                        output.write(b, 0, len);
//                        long cache_copy_progress = SPCache.getLong("cache_copy_progress", 0);
//                        cache_copy_progress += len;
//                        SPCache.putLong("cache_copy_progress", cache_copy_progress);
//                        long cache_size = SPCache.getLong("cache_copy_progress", 0);
//                        int progress = (int) (cache_size * 1.0f / folderSize * 100);
                        listener.showProgress(len, "copy");
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
                if (temp.isDirectory()) {//如果是子文件夹
                    copyFolder(oldPath + "/" + file[i], newPath + "/" + file[i], folderSize);
                }
            }
            copy_size = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static long del_size = 0;
    /**
     * 复制整个文件夹内容
     *
     * @return boolean
     */
    public void delFolder(String oldPath, long folderSize) {
        try {
            File a = new File(oldPath);
            String[] file = a.list();
            File temp = null;
            for (int i = 0; i < file.length; i++) {
                if (oldPath.endsWith(File.separator)) {
                    temp = new File(oldPath + file[i]);
                } else {
                    temp = new File(oldPath + File.separator + file[i]);
                }
                if (temp.isFile()) {
                    del_size+=temp.length();
                    temp.delete();
                    int progress = (int) (del_size * 1.0f / folderSize * 100);
                    listener.showProgress(progress,"del");
                }
                if (temp.isDirectory()) {//如果是子文件夹
                    delFolder(oldPath + "/" + file[i],folderSize);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    long size_del=0;
    /**
     * 删除单个文件
     * @param   filePath    被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    public boolean deleteFile(String filePath, long folderSize) {

        File file = new File(filePath);
        long size = file.length();
        if (file.isFile() && file.exists()) {
            boolean success = file.delete();
            if (success) {
                listener.showProgress(size,"del");
            }
            return success;
        }
        return false;
    }

    /**
     * 删除文件夹以及目录下的文件
     * @param   filePath 被删除目录的文件路径
     * @return  目录删除成功返回true，否则返回false
     */
    public boolean deleteDirectory(String filePath, long folderSize) {
        boolean flag = false;
        //如果filePath不以文件分隔符结尾，自动添加文件分隔符
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        File[] files = dirFile.listFiles();
        //遍历删除文件夹下的所有文件(包括子目录)
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                //删除子文件
                flag = deleteFile(files[i].getAbsolutePath(),folderSize);
                if (!flag) break;
            } else {
                //删除子目录
                flag = deleteDirectory(files[i].getAbsolutePath(),folderSize);
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //删除当前空目录
        return dirFile.delete();
    }

    /**
     *  根据路径删除指定的目录或文件，无论存在与否
     *@return 删除成功返回 true，否则返回 false。
     */
    public boolean DeleteFolder(String filePath, long folderSize) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        } else {
            if (file.isFile()) {
                // 为文件时调用删除文件方法
                return deleteFile(filePath,folderSize);
            } else {
                // 为目录时调用删除目录方法
                return deleteDirectory(filePath,folderSize);
            }
        }
    }
    public interface CopyProgressListener {
        void showProgress(long size, String tag);
    }
}
