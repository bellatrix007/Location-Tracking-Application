package com.bellatrix.aditi.tracker;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    private String title[] = {"Pending Requests", "Sent Requests"};
    private String user;

    public ViewPagerAdapter(FragmentManager manager, String user) {
        super(manager);
        this.user = user;
    }

    @Override
    public Fragment getItem(int position) {
        return TabFragment.getInstance(user, position);
    }

    @Override
    public int getCount() {
        return title.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return title[position];
    }
}

