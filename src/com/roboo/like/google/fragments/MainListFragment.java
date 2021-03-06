package com.roboo.like.google.fragments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.BounceInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.nineoldandroids.view.ViewHelper;
import com.roboo.like.google.BaseActivity;
import com.roboo.like.google.BaseLayoutActivity;
import com.roboo.like.google.GoogleApplication;
import com.roboo.like.google.LocationActivity;
import com.roboo.like.google.MainActivity;
import com.roboo.like.google.MoodActivity;
import com.roboo.like.google.NewsActivity;
import com.roboo.like.google.PictureActivity;
import com.roboo.like.google.R;
import com.roboo.like.google.TextActivity;
import com.roboo.like.google.abs.ptr.OnCancleListener;
import com.roboo.like.google.adapters.NewsListAdapter;
import com.roboo.like.google.async.NewsListAsyncTaskLoader;
import com.roboo.like.google.commons.YMDComparator;
import com.roboo.like.google.infinite.FixedSpeedScroller;
import com.roboo.like.google.infinite.InfinitePagerAdapter;
import com.roboo.like.google.infinite.InfiniteViewPager;
import com.roboo.like.google.infinite.ViewPagerEx.OnPageChangeListener;
import com.roboo.like.google.infinite.ViewPagerEx.PageTransformer;
import com.roboo.like.google.models.NewsItem;
import com.roboo.like.google.progressbutton.ProcessButton;
import com.roboo.like.google.progressbutton.ProgressGenerator;
import com.roboo.like.google.progressbutton.ProgressGenerator.OnCompleteListener;
import com.roboo.like.google.utils.CardToastUtils;
import com.roboo.like.google.utils.FileUtils;
import com.roboo.like.google.utils.MD5Utils;
import com.roboo.like.google.utils.NetWorkUtils;
import com.roboo.like.google.views.FooterView;
import com.roboo.like.google.views.HeaderView;
import com.roboo.like.google.views.StickyListHeadersListView;
import com.roboo.like.google.views.helper.PoppyListViewHelper;
import com.roboo.like.google.views.helper.PullToRefreshHelper;
import com.roboo.like.google.views.helper.PullToRefreshHelper.DefaultHeaderTransformer;
import com.roboo.like.google.views.helper.PullToRefreshHelper.OnRefreshListener;

public class MainListFragment extends BaseMainFragment implements LoaderCallbacks<LinkedList<NewsItem>>
{
	private static final String DECLARED_OPERA_FAST_SCROLLER_FIELD = "mFastScroller";// FastScroller
	private static final String DECLARED_OVERLAY_SIZE = "mOverlaySize";// int
	private static final String DECLARED_OVERLAY_POS = "mOverlayPos"; // RectF
	private static final String DECLARED_OVERLAY_DRAWABLE = "mOverlayDrawable"; // Drawable
	private static final String DECLARED_PAINT = "mPaint"; // Paint
	/** Bundle当前加载数据的页数Key */
	private static final String ARG_CURRENT_PAGENO = "current_pageno";
	/** Bundle当前获取新闻URL */
	private static final String ARG_NEWS_URL = "news_url";
	/** 获取的是当前第几页的新闻数据 */
	private int mCurrentPageNo = 1;
	private int mStubCurrentPageNo = mCurrentPageNo;
	/** 用于记录兩條新聞日期不相同时，该比较字符串所在List集合的索引位置，在生成HeaderId时进行获取 */
	private LinkedList<Integer> mSectionIndex = new LinkedList<Integer>();
	/** ListView */
	private StickyListHeadersListView mListView;
	/** 当ListView向上滚动时会出现的View的辅助类 */
	private PoppyListViewHelper mPoppyListViewHelper;
	/** ActionBar下拉刷新的辅助类 */
	private PullToRefreshHelper mPullToRefreshAttacher;
	/** 当ListView向上滚动时会出现的View */
	private View mPoppyView;
	/** 图片 */
	private Button mBtnPicture;
	/** 我的位置 */
	private Button mBtnLocation;
	/** 心情 */
	private Button mBtnMood;
	/** 文字 */
	private Button mBtnText;
	/** 当第一次获取数据为空时显示的View */
	private View mListEmptyView;
	/** 新闻列表适配器 */
	private NewsListAdapter mAdapter;
	/** ListView最后一列是否可见的标志 */
	public boolean mLastItemVisible;
	/** ListView 的 FooterView */
	private FooterView mFooterView;
	/** ListView 的 HeaderView */
	private HeaderView mHeaderView;
	/** 新闻列表适配器的数据源 */
	private LinkedList<NewsItem> mData;
	/** 当点击FooterView时显示加载数据标识 */
	private ProgressBar mFooterProgressBar;
	private int mProgress = 0;
	/** 正在加载数据中…… */
	private Button mBtnLoadNext;
	/** HeaderView 中的ViewPager */
	private InfiniteViewPager mAdViewPager;
	protected int mPosition = 0;
	protected Handler mHandler = new Handler();

	private boolean mSwapRunnableHasStart = false;
	/** 模拟广告图片轮转间隔时间 */
	private static final long SWAP_INTERVAL_TIME = 3000L;
	private String mChannelURL;
	private boolean mShouldShowCardToast = false;
	private Runnable mCreateDesktopRunnable = new Runnable()
	{
		public void run()
		{
			MainActivity activity = (MainActivity) getActivity();
			activity.showCreateDesktopDialog();
		}
	};
	private Runnable mSwapRunnable = new Runnable()
	{
		public void run()
		{
			if (mAdViewPager != null)
			{
				mSwapRunnableHasStart = true;
				mHeaderView.getIndicator().setCurrentItem(mPosition % getRealPagerCount(), true);
				mAdViewPager.nextItem();
				mPosition++;
				mHandler.postDelayed(mSwapRunnable, SWAP_INTERVAL_TIME);
				mHandler.sendEmptyMessage(mPosition);
			}
		}
	};

	/** 创建一个 ContentFragment 实例 */
	public static MainListFragment newInstance(String newsUrl)
	{
		MainListFragment fragment = new MainListFragment();
		Bundle bundle = new Bundle();
		bundle.putString(ARG_NEWS_URL, newsUrl);
		fragment.setArguments(bundle);
		return fragment;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = null;
		view = inflater.inflate(R.layout.fragment_main_list, null);// TODO
		// mFooterView = new FooterView(getActivity(), FooterView.TYPE_PROGRESS_BUTTON);
		mFooterView = (FooterView) view.findViewById(R.id.fv_footerview);
		mListEmptyView = view.findViewById(android.R.id.empty);
		mHeaderView = new HeaderView(getActivity());
		mAdViewPager = mHeaderView.getViewPager();
		mFooterProgressBar = mFooterView.getFooterProgressBar();
		mBtnLoadNext = mFooterView.getButton();
		mListView = (StickyListHeadersListView) view.findViewById(R.id.slhlv_list);
		mListView.setFastScrollEnabled(getActivity().getSharedPreferences(getActivity().getPackageName(), Context.MODE_PRIVATE).getBoolean(BaseActivity.PREF_FAST_SCROLL, true));
		mListView.setFastScrollAlwaysVisible(getActivity().getSharedPreferences(getActivity().getPackageName(), Context.MODE_PRIVATE).getBoolean(BaseActivity.PREF_FAST_SCROLL, true));
		mPoppyListViewHelper = new PoppyListViewHelper(getActivity());
		mPullToRefreshAttacher = PullToRefreshHelper.get(getActivity());
		mPullToRefreshAttacher.setOnCancleListener(getOnCancleListener());
		mInited = true;
		return view;
	}

	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		mPoppyView = mPoppyListViewHelper.createPoppyViewOnListView(R.id.slhlv_list, R.layout.poppyview);
		mPoppyView.setVisibility(View.GONE);
		mFooterView.setVisibility(View.GONE);
		mBtnPicture = (Button) mPoppyView.findViewById(R.id.btn_picture);
		mBtnLocation = (Button) mPoppyView.findViewById(R.id.btn_location);
		mBtnMood = (Button) mPoppyView.findViewById(R.id.btn_mood);
		mBtnText = (Button) mPoppyView.findViewById(R.id.btn_text);
		mPullToRefreshAttacher.addRefreshableView(mListView, getOnRefreshListener());
		mData = getOfflineData(getArguments().getString(ARG_NEWS_URL));
		if (null != mData)
		{
			mPoppyView.setVisibility(View.VISIBLE);
			mFooterView.setVisibility(View.VISIBLE);
			mAdapter = new NewsListAdapter(getActivity(), mData, mSectionIndex);
			// mListView.addFooterView(mFooterView);
			mListView.addHeaderView(mHeaderView);
			mListView.setAdapter(mAdapter);
			mBtnLoadNext.setOnClickListener(new OnClickListenerImpl());
			Collections.sort(generateHeaderId(mData), new YMDComparator());
			mAdapter.setSectionIndex(mSectionIndex);
			mAdapter.notifyDataSetChanged();
			if (!mSwapRunnableHasStart)
			{
				mAdViewPager.setAdapter(getPagerAdapter());
				mAdViewPager.setOffscreenPageLimit(getRealPagerCount());
				FixedSpeedScroller fixedSpeedScroller = new FixedSpeedScroller(getActivity(), new BounceInterpolator());
				mAdViewPager.setFixedScroller(fixedSpeedScroller);
				mHeaderView.getIndicator().setViewPager(mAdViewPager);
				mAdViewPager.setOnPageChangeListener(getOnPageChangeListener());
				mHandler.postDelayed(mSwapRunnable, SWAP_INTERVAL_TIME);
				mAdViewPager.setPageTransformer(true, getTransformer());
				mSwapRunnableHasStart = true;
			}
		}
		if (savedInstanceState == null)
		{
			loadFirstData();
		}
		// modifyDefaultListViewFieldValue();

	}

	private OnCancleListener getOnCancleListener()
	{
		return new OnCancleListener()
		{
			public void onCancle()
			{
				getLoaderManager().destroyLoader(0);
				mPullToRefreshAttacher.setRefreshComplete();
				mDebug = true;
				if (mDebug)
				{
					Toast.makeText(getActivity(), "结束任务", Toast.LENGTH_SHORT).show();
				}
			}
		};
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if (!mPullToRefreshAttacher.isRefreshing())
		{
			mShouldShowCardToast = false;
		}
	}

	public void onResume()
	{
		super.onResume();
		setListener();
	}

	private void setListener()
	{
		mListView.setOnItemClickListener(new OnListItemClickListenerImpl());
		OnClickListenerImpl onClickListenerImpl = new OnClickListenerImpl();
		mBtnLocation.setOnClickListener(onClickListenerImpl);
		mBtnMood.setOnClickListener(onClickListenerImpl);
		mBtnPicture.setOnClickListener(onClickListenerImpl);
		mBtnText.setOnClickListener(onClickListenerImpl);
		mFooterView.getButton().setOnClickListener(onClickListenerImpl);
	}

	private OnRefreshListener getOnRefreshListener()
	{
		return new OnRefreshListener()
		{
			public void onRefreshStarted(View view)
			{
				loadFirstData();
			}
		};
	}

	private class OnListItemClickListenerImpl implements OnItemClickListener
	{
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			if (parent.getAdapter().getItemViewType(position) == AbsListView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER)
			{
				// if (mFooterView.getType() == FooterView.TYPE_BUTTON)
				// {
				// loadNextData();
				// }
			}
			else
			{
				NewsActivity.actionNews(getActivity(), (NewsItem) parent.getAdapter().getItem(position));
			}
		}
	}

	private OnCompleteListener<Object> getOnCompleteListener()
	{
		return new OnCompleteListener<Object>()
		{
			public Object onComplete()
			{
				mBtnLoadNext.setEnabled(true);
				((ProcessButton) mBtnLoadNext).onNormalState();
				return null;
			}

			@Override
			public Object doInBackgroundProcess()
			{
				loadNextData();
				mBtnLoadNext.setEnabled(false);
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable()
				{
					public void run()
					{
						((ProcessButton) mBtnLoadNext).setProgress(mProgress);
						if (mProgress < 100)
						{
							handler.postDelayed(this, generateDelay());
						}
						else
						{
							onComplete();
						}
						mProgress = new Random().nextInt(100);
					}
				}, generateDelay());

				return null;
			}

			private long generateDelay()
			{
				return new Random().nextInt(1000);
			}
		};
	}

	private void loadFirstData()
	{
		mShouldShowCardToast = true;
		if (mListEmptyView.getVisibility() == View.VISIBLE)
		{
			mListEmptyView.setVisibility(View.GONE);
		}
		if (!NetWorkUtils.isNetworkAvailable(getActivity()))
		{
			DefaultHeaderTransformer transformer = (DefaultHeaderTransformer) mPullToRefreshAttacher.getHeaderTransformer();
			transformer.setRefreshingText("正在获取离线数据");
		}
		Bundle bundle = getArguments();
		mCurrentPageNo = 1;
		bundle.putInt(ARG_CURRENT_PAGENO, mCurrentPageNo);
		if (GoogleApplication.mCurrentType == GoogleApplication.TYPE_GEEKPARK)
		{
			int pageNo = Integer.MAX_VALUE;
			bundle.putInt(ARG_CURRENT_PAGENO, pageNo);
		}
		mPullToRefreshAttacher.setRefreshing(true);
		getActivity().getSupportLoaderManager().restartLoader(0, bundle, MainListFragment.this);
	}

	/** initLoader/reStartLoader方法被调用时会执行onCreateLoader */
	public Loader<LinkedList<NewsItem>> onCreateLoader(int id, Bundle args)
	{
		GoogleApplication.TEST = false;
		if (GoogleApplication.TEST)
		{
			System.out.println("当前加载的是第   " + args.getInt(ARG_CURRENT_PAGENO, 1) + " 页数据");
		}
		mFooterProgressBar.setVisibility(View.VISIBLE);
		mBtnLoadNext.setText("正在加载数据中……");
		return new NewsListAsyncTaskLoader(getActivity(), args.getString(ARG_NEWS_URL), args.getInt(ARG_CURRENT_PAGENO, 1));
	}

	@Override
	public void onLoadFinished(Loader<LinkedList<NewsItem>> loader, LinkedList<NewsItem> data)
	{
		mBtnLoadNext.setEnabled(true);
		mProgress = 100;
		((ProcessButton) mBtnLoadNext).onNormalState();
		if (data != null)
		{
			int updateCount = 0;
			if (null == mData)
			{
				mData = data;
				updateCount = mData.size();
				mAdapter = new NewsListAdapter(getActivity(), mData, mSectionIndex);
				// mListView.addFooterView(mFooterView);
				mListView.addHeaderView(mHeaderView);
				mListView.setAdapter(mAdapter);
				mBtnLoadNext.setOnClickListener(new OnClickListenerImpl());
			}
			else
			{
				updateCount = handleAddData(data).size();
			}
			Collections.sort(generateHeaderId(mData), new YMDComparator());
			mAdapter.setSectionIndex(mSectionIndex);
			mAdapter.notifyDataSetChanged();
			for (NewsItem item : data)
			{
				GoogleApplication.TEST = false;
				if (GoogleApplication.TEST)
				{
					System.out.println("Item = 【 " + item + " 】 ");
				}
			}
			String messageText = "加载  " + updateCount + " 条新数据";
			if (mCurrentPageNo == 1)
			{
				messageText = " 更新  " + updateCount + " 条新数据";
			}
			if (mShouldShowCardToast)
			{
				new CardToastUtils(getActivity()).setShowToastStyle(CardToastUtils.SHOW_ANIMATION_TOP_TO_DOWN_STYLE).showAndAutoDismiss(messageText);
				mShouldShowCardToast = false;
			}
			mBtnLoadNext.setText("点击加载下一页");
		}
		else
		{
			if (mData == null)
			{
				mListEmptyView.setVisibility(View.VISIBLE);
			}
			mBtnLoadNext.setText("所有数据加载完毕");
		}
		if (null != mData && !mSwapRunnableHasStart)
		{
			mPoppyView.setVisibility(View.VISIBLE);
			mFooterView.setVisibility(View.VISIBLE);
			mAdViewPager.setAdapter(getPagerAdapter());
			mAdViewPager.setOffscreenPageLimit(getRealPagerCount());
			FixedSpeedScroller fixedSpeedScroller = new FixedSpeedScroller(getActivity(), new BounceInterpolator());
			mAdViewPager.setFixedScroller(fixedSpeedScroller);
			mHeaderView.getIndicator().setViewPager(mAdViewPager);
			mAdViewPager.setOnPageChangeListener(getOnPageChangeListener());
			mHandler.postDelayed(mSwapRunnable, SWAP_INTERVAL_TIME);
			mAdViewPager.setPageTransformer(true, getTransformer());
		}
		if (!NetWorkUtils.isNetworkAvailable(getActivity()))
		{
			mBtnLoadNext.setText("设置网络");
		}
		mFooterProgressBar.setVisibility(View.INVISIBLE);
		mPullToRefreshAttacher.setRefreshComplete();
		if (isNeedShowCreateDesktopDialog())
		{
			mHandler.postDelayed(mCreateDesktopRunnable, 500L);
		}
	}

	/** 跳转到设置界面 */
	public void networkSettings()
	{
		Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	private class OnClickListenerImpl implements OnClickListener
	{
		public void onClick(View v)
		{
			switch (v.getId())
			{
			case R.id.btn_picture:// 图片
				picture();
				break;
			case R.id.btn_location:// 位置
				location();
				break;
			case R.id.btn_mood:// 心情
				mood();
				break;
			case R.id.btn_text:// 文字
				text();
				break;
			case R.id.btn_load_next:// 加载下一页
			case R.id.progress_btn_load_next:
			case R.id.pbtn_load_next:
				// loadNextData();
				doLoadNextData();// 包含了loadNextData()
				break;
			}
		}

		private void doLoadNextData()
		{
			mShouldShowCardToast = true;
			mProgress = 50;
			new ProgressGenerator(getOnCompleteListener()).start((ProcessButton) mBtnLoadNext);
		}

		/** 图片 */
		public void picture()
		{
			PictureActivity.actionPicture(getActivity());
		}

		/** 位置 */
		public void location()
		{
			LocationActivity.actionLocation(getActivity());
		}

		/** 心情 */
		public void mood()
		{
			MoodActivity.actionMood(getActivity());
		}

		/** 文字 */
		public void text()
		{
			TextActivity.actionText(getActivity());
		}
	}

	private boolean isNeedShowCreateDesktopDialog()
	{
		boolean flag = false;
		SharedPreferences preferences = getActivity().getSharedPreferences(getActivity().getPackageName(), Context.MODE_PRIVATE);
		flag = preferences.contains(BaseLayoutActivity.PREF_APP_FIRST_START);
		if (!flag)
		{
			preferences.edit().putBoolean(BaseLayoutActivity.PREF_APP_FIRST_START, true).commit();
		}
		return !flag;
	}

	private OnPageChangeListener getOnPageChangeListener()
	{
		return new OnPageChangeListener()
		{
			public void onPageSelected(int position)
			{
				mPosition = position;
				mHeaderView.getIndicator().setCurrentItem(position % getRealPagerCount(), true);
			}

			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
			{

			}

			public void onPageScrollStateChanged(int state)
			{

			}
		};
	}

	private int getRealPagerCount()
	{
		PagerAdapter adapter = mAdViewPager.getAdapter();
		if (adapter instanceof InfinitePagerAdapter)
		{
			return ((InfinitePagerAdapter) adapter).getRealCount();
		}
		else
		{
			return adapter.getCount();
		}
	}

	private PageTransformer getTransformer()
	{
		PageTransformer pageTransformer = new PageTransformer()
		{
			public void transformPage(View page, float position)
			{
				ViewHelper.setTranslationX(page, position < 0 ? 0f : -page.getWidth() * position);
			}
		};

		return pageTransformer;
	}

	private PagerAdapter getPagerAdapter()
	{
		PagerAdapter pagerAdapter = new PagerAdapter()
		{
			public boolean isViewFromObject(View view, Object object)
			{
				return view == object;
			}

			public int getCount()
			{
				return 3;
			}

			@Override
			public int getItemPosition(Object object)
			{
				return POSITION_NONE;
			}

			public Object instantiateItem(ViewGroup container, int position)
			{
				ImageView imageView = null;
				if (null != getActivity())
				{
					imageView = new ImageView(getActivity());
					LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
					imageView.setScaleType(ScaleType.FIT_XY);
					imageView.setImageResource(getResources().getIdentifier("ic_test" + (1 + position), "drawable", getActivity().getPackageName()));
					imageView.setLayoutParams(params);
					container.addView(imageView);
				}
				return imageView;
			}

			@Override
			public void destroyItem(ViewGroup container, int position, Object object)
			{
				container.removeView((View) object);
			}
		};
		InfinitePagerAdapter wrappedAdapter = new InfinitePagerAdapter(pagerAdapter);
		return wrappedAdapter;
	}

	/** 处理数据重复问题,以及将最新的数据放在最上面 */
	private LinkedList<NewsItem> handleAddData(LinkedList<NewsItem> data)
	{
		LinkedList<NewsItem> tmpItems = new LinkedList<NewsItem>();
		for (int i = data.size() - 1; i >= 0; i--)
		{
			NewsItem item = data.get(i);
			if (!mData.contains(item))
			{
				if (mCurrentPageNo == 1)
				{
					mData.addFirst(item);
				}
				else
				{
					mData.add(item);
				}
				tmpItems.add(item);
			}
		}
		return tmpItems;
	}

	@Override
	public void onLoaderReset(Loader<LinkedList<NewsItem>> loader)
	{}

	private void loadNextData()
	{
		// =========================================================================
		// =========================================================================
		// 如果网络可用的话点击就去加载下一页数据， 如果不可用的话点击就跳转到系统网络设置界面
		// =========================================================================
		// =========================================================================
		if (!NetWorkUtils.isNetworkAvailable(getActivity()))
		{
			networkSettings();
		}
		else
		{
			Bundle bundle = getArguments();
			mStubCurrentPageNo += 1;
			mCurrentPageNo = mStubCurrentPageNo;
			bundle.putInt(ARG_CURRENT_PAGENO, mStubCurrentPageNo);
			if (GoogleApplication.mCurrentType == GoogleApplication.TYPE_GEEKPARK)// 极客公园要对currentpageno进行特殊处理
			{
				int pageNo = Integer.parseInt(mData.getLast().t);
				bundle.putInt(ARG_CURRENT_PAGENO, pageNo - 1);
			}
			getActivity().getSupportLoaderManager().restartLoader(0, bundle, this);
		}
	}

	private LinkedList<NewsItem> generateHeaderId(LinkedList<NewsItem> nonHeaderIdList)
	{
		Map<String, Integer> mHeaderIdMap = new HashMap<String, Integer>();
		int mHeaderId = 1;
		LinkedList<NewsItem> hasHeaderIdList;

		for (int i = 0; i < nonHeaderIdList.size(); i++)
		{
			NewsItem item = nonHeaderIdList.get(i);
			String ymd = item.getTime();
			if (!mHeaderIdMap.containsKey(ymd))
			{
				item.setHeaderId(mHeaderId);
				mHeaderIdMap.put(ymd, mHeaderId);
				mHeaderId++;
				mSectionIndex.add(i);
			}
			else
			{
				item.setHeaderId(mHeaderIdMap.get(ymd));
			}
		}
		hasHeaderIdList = nonHeaderIdList;
		return hasHeaderIdList;
	}

	/** 修改ListView一些默认属性值 */
	private void modifyDefaultListViewFieldValue()
	{
		try
		{
			Field field = AbsListView.class.getDeclaredField(DECLARED_OPERA_FAST_SCROLLER_FIELD);
			field.setAccessible(true);
			Object object = field.get(mListView);
			field = field.getType().getDeclaredField(DECLARED_PAINT);// 获取绘制文字的画笔
			field.setAccessible(true);
			Paint paint = new Paint();
			paint.setColor(getResources().getColor(R.color.red_color));
			paint.setAntiAlias(true);
			paint.setTextAlign(Paint.Align.CENTER);
			paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18, getResources().getDisplayMetrics()));
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			field.set(object, paint);

			field = field.getType().getDeclaredField(DECLARED_OVERLAY_POS);
			field.setAccessible(true);
			RectF rectF = (RectF) field.get(object);
			RectF newRectF = new RectF();
			newRectF.left = rectF.left;
			newRectF.top = rectF.top;
			newRectF.right = rectF.left + 200;
			newRectF.bottom = rectF.top + 20;
			field.set(object, newRectF);

			field = field.getType().getDeclaredField(DECLARED_OVERLAY_SIZE);
			field.setAccessible(true);
			field.set(object, 50);
			field = field.getType().getDeclaredField(DECLARED_OVERLAY_DRAWABLE);
			Drawable overDrawable = getResources().getDrawable(R.drawable.ic_fast_scroll_label_right);
			overDrawable.setBounds(0, 0, 100, 40);
			field.setAccessible(true);
			field.set(object, overDrawable);

			System.out.println("修改滑动时现在字体大小成功  " + field.getInt(object));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/** 从文件中获取本地的离线数据 */
	private LinkedList<NewsItem> getOfflineData(String channelUrl)
	{
		LinkedList<NewsItem> data = null;
		try
		{
			if (!TextUtils.isEmpty(channelUrl))
			{
				File dirFile = FileUtils.getFileCacheDir(getActivity(), FileUtils.TYPE_NEWS_LIST);
				File dataFile = new File(dirFile, MD5Utils.generate(channelUrl));
				if (dataFile.exists())
				{
					ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(dataFile));
					data = (LinkedList<NewsItem>) objectInputStream.readObject();
					objectInputStream.close();
					GoogleApplication.TEST = true;
					if (GoogleApplication.TEST)
					{
						System.out.println("从本地文件读取对象成功");
					}
				}
			}
		}
		catch (StreamCorruptedException e)
		{
			e.printStackTrace();
		}
		catch (OptionalDataException e)
		{
			e.printStackTrace();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
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

	@Override
	public void setFastScrollEnable(boolean enable)
	{
		mListView.setFastScrollEnabled(enable);
		mListView.setFastScrollAlwaysVisible(enable);
	}
}
