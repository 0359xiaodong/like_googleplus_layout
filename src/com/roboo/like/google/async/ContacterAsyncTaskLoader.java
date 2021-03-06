package com.roboo.like.google.async;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.MediaStore.Images.Media;
import android.text.TextUtils;

import com.roboo.like.google.models.ContacterItem;

public class ContacterAsyncTaskLoader extends BaseAsyncTaskLoader<LinkedList<ContacterItem>>
{
	private Context mContext;

	public ContacterAsyncTaskLoader(Context context)
	{
		super(context);
		mContext = context;
	}

	@Override
	public LinkedList<ContacterItem> loadInBackground()
	{

		LinkedList<ContacterItem> data = null;
		ContentResolver resolver = mContext.getContentResolver();
		String[] strings = new String[] { Phone.DISPLAY_NAME, Phone.NUMBER, Photo.PHOTO_URI };
		Cursor phoneCursor = resolver.query(Phone.CONTENT_URI, strings, null, null, null);// 获取手机联系人
		if (phoneCursor != null && phoneCursor.getCount() > 0)
		{
			data = new LinkedList<ContacterItem>();
			while (phoneCursor.moveToNext())
			{
				ContacterItem item = new ContacterItem();
				String contactName = phoneCursor.getString(0); // 得到联系人名称
				String phoneNumber = phoneCursor.getString(1);// 得到手机号码
				String contactIcon = phoneCursor.getString(2);// 得到联系人头像Uri
				// 当手机号码为空的或者为空字段 跳过当前循环
				if (TextUtils.isEmpty(phoneNumber))
				{
					continue;
				}
				item.name = contactName;
				item.phone = phoneNumber;
				item.icon = contactIcon;
				if (!TextUtils.isEmpty(item.icon))
				{
					try
					{
						Bitmap bitmap = Media.getBitmap(mContext.getContentResolver(), Uri.parse(item.icon));
						if (null != bitmap)
						{
							item.bitmap = bitmap;
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
				if (!data.contains(item))
				{
					data.add(item);
				}

			}
			phoneCursor.close();
		}
		return data;
	}

}
