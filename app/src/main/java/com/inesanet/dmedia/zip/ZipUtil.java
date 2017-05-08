package com.inesanet.dmedia.zip;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.File;

public class ZipUtil
{
	private static final String password = "d2x1z0s6";//需要修改

	public static void unZipFileWithProgress(final File zipFile, final String filePath, final Handler handler,
                                             final boolean isDeleteZip) throws ZipException
	{
		ZipFile zFile = new ZipFile(zipFile);
		zFile.setFileNameCharset("GBK");

		if (!zFile.isValidZipFile())
		{ // 验证.zip文件是否合法，包括文件是否存在、是否为zip文件、是否被损坏等
			throw new ZipException("压缩文件不合法,可能被损坏.");
		}
		File destDir = new File(filePath); // 解压目录
		if (destDir.isDirectory() && !destDir.exists())
		{
			destDir.mkdir();
		}
		if (zFile.isEncrypted())
		{
			zFile.setPassword(password); // 设置密码
		}

		final ProgressMonitor progressMonitor = zFile.getProgressMonitor();
		Thread thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Bundle bundle = null;
				Message msg = null;
				try
				{
					int precentDone = 0;
					if (handler == null)
					{
						return;
					}
					handler.sendEmptyMessage(CompressStatus.START);
					while (true)
					{
						// 启动线程，每隔一秒向外handler一条消息
						Thread.sleep(5);
						precentDone = progressMonitor.getPercentDone();
						bundle = new Bundle();
						bundle.putInt(CompressStatus.PERCENT, precentDone);
						msg = new Message();
						msg.what = CompressStatus.HANDLING;
						msg.setData(bundle);
						handler.sendMessage(msg);
						if (precentDone >= 100)
						{
							break;
						}
					}
					handler.sendEmptyMessage(CompressStatus.COMPLETED);
				}
				catch (InterruptedException e)
				{
					bundle = new Bundle();
					bundle.putString(CompressStatus.ERROR_COM, e.getMessage());
					msg = new Message();
					msg.what = CompressStatus.ERROR;
					msg.setData(bundle);
					handler.sendMessage(msg);
					e.printStackTrace();
				}
				finally
				{
					if (isDeleteZip)
					{
						zipFile.delete();
					}
				}
			}
		});
		thread.start();
		zFile.setRunInThread(true);
		zFile.extractAll(filePath); // 解压到此文件夹中
	}
}
