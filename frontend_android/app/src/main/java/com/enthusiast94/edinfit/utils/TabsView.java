package com.enthusiast94.edinfit.utils;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;

/**
 * Created by manas on 05-01-2016.
 */
public class TabsView extends LinearLayout {

    private ViewPager viewPager;
    private LayoutInflater inflater;
    private TabContentProvider tabContentProvider;
    private int currentlySelectedPosition;
    private int previouslySelectedPosition;

    public TabsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflater = LayoutInflater.from(context);
        currentlySelectedPosition = 0;
        previouslySelectedPosition = 0;

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
    }

    public void setViewPager(final ViewPager viewPager) {
        if (this.viewPager != null) {
            throw new IllegalStateException("ViewPager has already been set");
        }

        this.viewPager = viewPager;
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                setSelectedTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        // add tabs for each view pager page
        if (!(viewPager.getAdapter() instanceof TabContentProvider)) {
            throw new IllegalArgumentException("Provided ViewPager must implement TabContentProvider interface");
        }

        tabContentProvider = (TabContentProvider) viewPager.getAdapter();
        for (int i=0; i<viewPager.getAdapter().getCount(); i++) {
            final View tabView = inflater.inflate(R.layout.view_tab, null);

            tabView.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            tabView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    int tabPosition = indexOfChild(tabView);
                    viewPager.setCurrentItem(tabPosition);
                    setSelectedTab(tabPosition);
                }
            });

            addView(tabView);

            setTabViewSelected(tabView, false);
        }
    }

    public void setSelectedTab(int position) {
        previouslySelectedPosition = currentlySelectedPosition;
        currentlySelectedPosition = position;

        setTabViewSelected(getChildAt(previouslySelectedPosition), false);
        setTabViewSelected(getChildAt(currentlySelectedPosition), true);
    }

    private void setTabViewSelected(View tabView, boolean shouldSelect) {
        TextView titleTextView = (TextView) tabView.findViewById(R.id.title_textview);
        ImageView iconImageView = (ImageView) tabView.findViewById(R.id.icon_imageview);

        int tabPosition = indexOfChild(tabView);

        titleTextView.setText(tabContentProvider.getTitle(tabPosition));
        iconImageView.setImageResource(tabContentProvider.getIcon(tabPosition));

        if (shouldSelect) {
            titleTextView.setTextColor(tabContentProvider.getSelectedTitleColor(tabPosition));
            iconImageView.setColorFilter(tabContentProvider.getSelectedIconColor(tabPosition));
        } else {
            titleTextView.setTextColor(tabContentProvider.getUnselectedTitleColor(tabPosition));
            iconImageView.setColorFilter(tabContentProvider.getUnselectedIconColor(tabPosition), PorterDuff.Mode.SRC_ATOP);
        }
    }

    public interface TabContentProvider {
        int getTitle(int position);
        int getIcon(int position);
        int getSelectedTitleColor(int position);
        int getUnselectedTitleColor(int position);
        int getSelectedIconColor(int position);
        int getUnselectedIconColor(int position);
    }
}
