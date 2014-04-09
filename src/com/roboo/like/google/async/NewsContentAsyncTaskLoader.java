package com.roboo.like.google.async;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

import android.content.Context;

import com.roboo.like.google.GoogleApplication;
import com.roboo.like.google.utils.FileUtils;
import com.roboo.like.google.utils.MD5Utils;
import com.roboo.like.google.utils.NewsUtils;

public class NewsContentAsyncTaskLoader extends BaseAsyncTaskLoader<LinkedList<String>>
{
	private String mNewsUrl;
	private Context mContext;

	public NewsContentAsyncTaskLoader(Context context, String newsUrl)
	{
		super(context);
		mContext = context;
		mNewsUrl = newsUrl;
	}

	@SuppressWarnings("unchecked")
	public LinkedList<String> loadInBackground()
	{
		LinkedList<String> data = null;
		try
		{
			File file = new File(FileUtils.getFileCacheDir(mContext, FileUtils.TYPE_NEWS_CONTENT), MD5Utils.generate(mNewsUrl));
			if (file.exists())
			{
				ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
				data = (LinkedList<String>) objectInputStream.readObject();
				objectInputStream.close();
				GoogleApplication.TEST = true;
				if(GoogleApplication.TEST)
				{
					System.out.println("从本地文件读取对象成功");
				}
			}
			else
			{
				data = NewsUtils.getITHomeNewsDataList(mNewsUrl);
				saveNewsContentData(data);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		return data;
	}

	private void saveNewsContentData(LinkedList<String> data)
	{
		File dirFile = FileUtils.getFileCacheDir(mContext, FileUtils.TYPE_NEWS_CONTENT);
		File dataFile = new File(dirFile, MD5Utils.generate(mNewsUrl));
		try
		{
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(dataFile));
			objectOutputStream.writeObject(data);
			objectOutputStream.close();
			GoogleApplication.TEST = true;
			if (GoogleApplication.TEST)
			{
				System.out.println("新闻内容对象写入文件成功 :: 文件路径 = "+dataFile.getAbsolutePath());
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
