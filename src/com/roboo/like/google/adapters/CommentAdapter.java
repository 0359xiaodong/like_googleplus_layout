package com.roboo.like.google.adapters;

import java.util.LinkedList;

import com.roboo.like.google.R;
import com.roboo.like.google.models.CommentItem;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/***
 * 新闻评论适配器
 * @author bo.li
 *
 * 2014-8-18 上午10:57:53
 *
 * TODO
 */
public class CommentAdapter extends BaseAdapter
{
	private LinkedList<CommentItem> mData;
	private Activity mActivity;

	public CommentAdapter(Activity mActivity, LinkedList<CommentItem> data)
	{
		this.mData = data;
		this.mActivity = mActivity;
	}

	@Override
	public int getCount()
	{
		return null == mData ? 0 : mData.size();
	}

	@Override
	public Object getItem(int position)
	{
		return null == mData ? null : mData.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		CommentItem item = mData.get(position);
		if (null != item)
		{
			if (convertView == null)
			{
				convertView = LayoutInflater.from(mActivity).inflate(R.layout.comment_list_item,
					null);//TODO
			}
			TextView tvFloor = ViewHolder.getView(convertView, R.id.tv_floor);
			TextView tvNick = ViewHolder.getView(convertView, R.id.tv_nick);
			TextView tvPhoneType = ViewHolder.getView(convertView, R.id.tv_phone_type);
			TextView tvAddress = ViewHolder.getView(convertView, R.id.tv_address);
			TextView tvTime = ViewHolder.getView(convertView, R.id.tv_time);
			TextView tvContent = ViewHolder.getView(convertView, R.id.tv_content);
			TextView tvAgree = ViewHolder.getView(convertView, R.id.tv_agree);
			TextView tvDisAgree = ViewHolder.getView(convertView, R.id.tv_disAgree);
			((ViewGroup) tvFloor.getParent()).setVisibility(View.VISIBLE);
			tvFloor.setText(item.floor);
			tvNick.setText(item.nick);
			tvPhoneType.setText(item.phoneType);
			tvAddress.setText(item.address);
			tvTime.setText(item.replyTime);
			tvContent.setText(item.replyContent);
			tvAgree.setText(item.agreeCount);
			tvDisAgree.setText(item.disAgressCount);
		}
		return convertView;
	}

}
