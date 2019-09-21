package com.microtechmd.pda.ui.activity;


import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.manager.SharePreferenceManager;
import com.microtechmd.pda.ui.widget.LockScreenView;


public class ActivityUnlockNew extends ActivityLockBase {

    public static ActivityUnlockNew instance = null;
    @Override
    public void onBackPressed() {
    }

    protected void onHomePressed() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.fragment_unlock_new);
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }


    private void initViews() {
        LockScreenView lsv =
                (LockScreenView) findViewById(R.id.image_view_unlock);
        lsv.setUnlockListener(new LockScreenView.UnlockListener() {
            @Override
            public void unlock() {
                finish();
            }
        });
    }
}
