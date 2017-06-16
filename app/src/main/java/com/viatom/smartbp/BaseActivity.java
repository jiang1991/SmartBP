package com.viatom.smartbp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
//import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.viatom.smartbp.fragment.HistoryFragment;
import com.viatom.smartbp.fragment.MeasureMainFragment;
import com.viatom.smartbp.fragment.SettingFragment;
import com.viatom.smartbp.utility.Constant;
import com.viatom.smartbp.utility.JsonRequest;
import com.viatom.smartbp.utility.LogUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends AppCompatActivity {

    private VpAdapter adapter;
    private ViewPager viewPager;
    private List<Fragment> fragments;
    public BottomNavigationView bnve;
    private MenuItem preMenuItem;
    public Toolbar toolbar;
    public int[] titles = new int[]{R.string.nav_measure, R.string.nav_history, R.string.nav_setting};

    public MeasureMainFragment measureMainFragment;
    public HistoryFragment historyFragment;
    public SettingFragment settingFragment;

    public RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        queue = Volley.newRequestQueue(this);

        initView();
        initFragment();
        initEvent();

        getDeviceInfo();
    }

    /* change BNVE style */
    private void initView() {
        toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(titles[0]);

        bnve = (BottomNavigationView) findViewById(R.id.bnve);
        viewPager = (ViewPager) findViewById(R.id.view_pager);
    }

    /* create fragments */
    private void initFragment() {
        fragments = new ArrayList<>(3);

        // create measure fragment and add it
        measureMainFragment = new MeasureMainFragment();

        // create history fragment and add it
        historyFragment = new HistoryFragment();

        // create setting fragment and add it
        settingFragment = new SettingFragment();

        fragments.add(measureMainFragment);
        fragments.add(historyFragment);
        fragments.add(settingFragment);

        // set adapter
        adapter = new VpAdapter(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(adapter);

    }

    /* set listeners */
    private void initEvent() {

        // select listener
        bnve.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_measure:
                        viewPager.setCurrentItem(0);
                        toolbar.setTitle(titles[0]);
                        return true;
                    case R.id.nav_history:
                        viewPager.setCurrentItem(1);
                        toolbar.setTitle(titles[1]);
                        historyFragment.setUI();
                        return true;
                    case R.id.nav_setting:
                        viewPager.setCurrentItem(2);
                        toolbar.setTitle(titles[2]);
//                        startActivity(new Intent().setClass(MainActivity.this, SettingActivity.class));
                        return true;
                }
                return false;
            }
        });

        // selector change listener
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //
            }

            @Override
            public void onPageSelected(int position) {
                if (preMenuItem != null) {
                    preMenuItem.setChecked(false);
                } else {
                    bnve.getMenu().getItem(0).setChecked(false);
                }
                bnve.getMenu().getItem(position).setChecked(true);
                toolbar.setTitle(titles[position]);
                preMenuItem = bnve.getMenu().getItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //
            }
        });
    }

    /*
    * get device info
    * update the UI
    * onDeviceConnected
    * */
    public void getDeviceInfo() {
        /* get device info */

        /* get update info */
        JsonObjectRequest request = new JsonObjectRequest(Constant.DEVICE_UPDATE_EN_URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String updateDfv = response.optString("version");
                LogUtils.d("DFV: " + updateDfv);
//                settingFragment.getUpdateDfv(updateDfv);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                LogUtils.d(error.toString());
            }
        });

        queue.add(request);
    }



    /* view pager adapter */
    private static class VpAdapter extends FragmentPagerAdapter {
        private List<Fragment> data;

        public VpAdapter(FragmentManager fm, List<Fragment> data) {
            super(fm);
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Fragment getItem(int position) {
            return data.get(position);
        }
    }


}
