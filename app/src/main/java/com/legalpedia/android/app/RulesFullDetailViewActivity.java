package com.legalpedia.android.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.legalpedia.android.app.models.DaoSession;
import com.legalpedia.android.app.views.MyNotesFragment;
import com.legalpedia.android.app.views.PrecedenceFullDetailFragment;
import com.legalpedia.android.app.views.PublicNotesFragment;
import com.legalpedia.android.app.views.RulesFullDetailFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by adebayoolabode on 5/12/17.
 */

public class RulesFullDetailViewActivity extends AppCompatActivity {
    private String name;
    private TextView titleView;
    private TextView bodyView;
    private Toolbar toolbar;
    private DaoSession daoSession;
    private ViewPager viewPager;
    private RulesFullDetailFragment rulesfull;
    private MyNotesFragment mynotes;
    private PublicNotesFragment pubnote;
    private TabLayout tabLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lawfulldetail_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        Intent intent=getIntent();
        name=intent.getStringExtra("name");
        Log.d("Rules ",name);



/**
        daoSession = ((App)  getApplication()).getDaoSession();
        LawsDao lawsDao = daoSession.getLawsDao();
        QueryBuilder qb = lawsDao.queryBuilder();
        List<Laws> resp=qb.where(LawsDao.Properties.Id.eq(lawid)).list();
        Laws law = resp.get(0);
        titleView = (TextView)findViewById(R.id.title);
        bodyView = (TextView)findViewById(R.id.body);
        setTitle(law.getTitle());
        titleView.setText(law.getTitle());
        String lawbody = getFullLaw(lawid);
        bodyView.setText(lawbody);

        */
        //SpannableString str = new SpannableString("Where the commissioner.");
        //str.setSpan(new BackgroundColorSpan(Color.YELLOW), 0, 11, 0);
        //bodyView.setText(str);
        //bodyView.setOnTouchListener(new CustomTouchListener());

        viewPager = (ViewPager) findViewById(R.id.viewpager);

        setupViewPager(viewPager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }


    private void setupViewPager(ViewPager viewPager) {
        rulesfull=new RulesFullDetailFragment();
        mynotes=new MyNotesFragment();
        pubnote=new PublicNotesFragment();
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(rulesfull, "Rules");
        adapter.addFrag(mynotes, "My Notes");
        adapter.addFrag(pubnote, "Public Notes");
        viewPager.setAdapter(adapter);
    }








    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        Layout layout = ((TextView) v).getLayout();
        int x = (int)event.getX();
        int y = (int)event.getY();
        if (layout!=null){
            int line = layout.getLineForVertical(y);
            int characterOffset = layout.getOffsetForHorizontal(line, x);
            Log.i("index", ""+characterOffset);
        }
        return true;


    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //    finish();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();

        }
        return true;
    }


    public class CustomTouchListener implements View.OnTouchListener {
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((TextView) view).setTextColor(0xFFFFFFFF); // white
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    ((TextView) view).setTextColor(Color.parseColor("#4a4a4a")); // lightblack
                    break;
            }
            return false;
        }
    }



    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

}
