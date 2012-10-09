package eu.tsvetkov.rabota.view;

import java.util.List;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

public class InfiniteFragmentPager extends ViewPager {

	public InfiniteFragmentPager(Context context) {
		super(context);
	}

	public InfiniteFragmentPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public Fragment getCurrentFragment() {
		return ((Adapter) getAdapter()).getItem(1);
	}

	public void init(final Adapter adapter) {
		setAdapter(adapter);
		setCurrentItem(1);
		setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			private int focusedPage = 0;

			@Override
			public void onPageScrollStateChanged(int state) {
				if (state != ViewPager.SCROLL_STATE_IDLE) {
					return;
				}

				// Update calendars depending on the scrolling.
				if (focusedPage == 0) {
					adapter.onScrollLeft();
				}
				else if (focusedPage == 2) {
					adapter.onScrollRight();
				}

				// Set the pager's current item to the middle fragment.
				setCurrentItem(1, false);
			}

			@Override
			public void onPageSelected(int position) {
				focusedPage = position;
			}
		});
	}

	public void onDataSetChange() {
		getAdapter().notifyDataSetChanged();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (getAdapter() == null) {
			throw new IllegalStateException("Before using the InfiniteFragmentPager please call the init() method to define the fragment adapter.");
		}
	}

	public abstract class Adapter extends FragmentStatePagerAdapter {

		private final List<Fragment> fragments;

		public Adapter(FragmentManager fm, List<Fragment> fragments) {
			super(fm);
			this.fragments = fragments;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public Fragment getItem(final int position) {
			return fragments.get(position);
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}

		public void onScrollLeft() {
			notifyDataSetChanged();
		}

		public void onScrollRight() {
			notifyDataSetChanged();
		}
	}
}
