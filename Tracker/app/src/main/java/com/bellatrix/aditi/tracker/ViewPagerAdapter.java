package com.bellatrix.aditi.tracker;

import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    private String title[] = {"Pending Requests", "Sent Requests"};
    private String user;
    private String user_name;

    public ViewPagerAdapter(FragmentManager manager, String user, String user_name) {
        super(manager);
        this.user = user;
        this.user_name = user_name;
        Log.d("user_name","user name fetched in View page adapter"+ user_name);
    }

    @Override
    public Fragment getItem(int position) {
        return TabFragment.getInstance(user_name, user, position);
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

