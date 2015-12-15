package com.qiku.bug_report.ui;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.qiku.bug_report.R;

public class ReportList extends Activity {
    //The middle angle is 45-degrees
    private static final double MIDDLE_ANGLE_THETA = Math.atan2(1, 1);
    private GestureDetector mGestureDetector;
    private ActionBar mActionBar;
    private AbsReportListFragment mCurrentListFragment;
    private Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionBar = getActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayShowTitleEnabled(true);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.report_list_actions, menu);
        mMenu = menu;
        mActionBar.addTab(mActionBar.newTab().setText(R.string.report_list_tab_outbox)
                .setTabListener(new TabListener<ReportListOutboxFragment>("outbox",
                        ReportListOutboxFragment.class)));

        mActionBar.addTab(mActionBar.newTab().setText(R.string.report_list_tab_sent)
                .setTabListener(new TabListener<ReportListSentFragment>("sent",
                        ReportListSentFragment.class)));

        mActionBar.addTab(mActionBar.newTab().setText(R.string.report_list_tab_invalid)
                .setTabListener(new TabListener<ReportListInvalidFragment>("invalid",
                        ReportListInvalidFragment.class)));

        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return doFling(e1, e2, velocityX, velocityY);
            }
        });
        onTabSwitched();
        return true;
    }

    public Menu getMenu(){
        return mMenu;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return mCurrentListFragment.onOptionsItemSelected(item.getItemId());
    }

    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    public boolean doFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if(e1 == null || e2 == null)
        	
        	
            return false;
        float deltaX = Math.abs(e2.getX() - e1.getX());
        float deltaY = Math.abs(e2.getY() - e1.getY());
        double tangent = Math.atan2(deltaY, deltaX);
        //switch tabs only if the angle between the two (start and end) points is below 45-degrees
        if(MIDDLE_ANGLE_THETA < tangent)
            return false;
        float xDisplacement = e1.getX() - e2.getX();
        if (xDisplacement > 50) {
            int curTab = getActionBar().getSelectedNavigationIndex();
            getActionBar().setSelectedNavigationItem(
                    curTab == getActionBar().getTabCount() - 1 ? 0 : curTab + 1);
        } else if (xDisplacement < -50) {
            int curTab = getActionBar().getSelectedNavigationIndex();
            getActionBar().setSelectedNavigationItem(
                    curTab == 0 ? getActionBar().getTabCount() - 1 : curTab - 1);
        }
        return true;
    }

    private void onTabSwitched(){
        if(mMenu!=null){
            mMenu.findItem(R.id.action_selectall).setVisible(false);
            mMenu.findItem(R.id.report_upload_switcher).setVisible(false);
            int[] itemIds = mCurrentListFragment.getSupportedMenuItemIds();
            for(int i=0; i<itemIds.length; i++){
                mMenu.findItem(itemIds[i]).setVisible(true);
            }
        }
    }

    private class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private final Class<T> mClass;
        private Fragment mFragment;
        private String mTag;

        public TabListener(String tag, Class<T> clz){
            mTag = tag;
            mClass = clz;

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.  If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            mFragment = getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(ReportList.this, mClass.getName());
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
            mCurrentListFragment = (AbsReportListFragment)mFragment;
            onTabSwitched();
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {

        }
    }
}
