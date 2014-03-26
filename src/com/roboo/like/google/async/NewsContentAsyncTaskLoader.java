package com.roboo.like.google.async;

import java.io.IOException;
import java.util.LinkedList;

import android.content.Context;

import com.roboo.like.google.GoogleApplication;
import com.roboo.like.google.utils.NewsUtils;

public class NewsContentAsyncTaskLoader extends BaseAsyncTaskLoader<LinkedList<String>>
{
	private String mNewsUrl;
	public NewsContentAsyncTaskLoader(Context context, String newsUrl)
	{
		super(context);
		mNewsUrl = newsUrl; 
	}

	public LinkedList<String> loadInBackground()
	{
		LinkedList<String> data = null;
		try
		{
			data  = NewsUtils.getITHomeNewsDataList(mNewsUrl);
		
			if(GoogleApplication.TEST)
			{
				if(null != data )
				{
					for(String str : data)
					{
						System.out.println("str = " + str);
					}
				}
			}
		
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return data;
	}

}