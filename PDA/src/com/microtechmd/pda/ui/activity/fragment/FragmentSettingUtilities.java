package com.microtechmd.pda.ui.activity.fragment;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.database.DbHistory;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.ValueByte;
import com.microtechmd.pda.library.entity.ValueInt;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.ui.activity.ActivityDataTest;
import com.microtechmd.pda.ui.activity.ActivityMain;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.ui.widget.WidgetSettingItem;
import com.microtechmd.pda.util.DataCleanUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static com.microtechmd.pda.ui.activity.ActivityPDA.COMMMESSAGETIPS;
import static com.microtechmd.pda.ui.activity.ActivityPDA.DATE_CHANGE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HIMESSAGETIPS;
import static com.microtechmd.pda.ui.activity.ActivityPDA.LOMESSAGETIPS;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_DATE_TIME;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_SETTING;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingTips.HYPER_DEFAULT;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingTips.HYPO_DEFAULT;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettings.SETTING_HYPER;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettings.SETTING_HYPO;

public class FragmentSettingUtilities extends FragmentBase
        implements
        EntityMessage.Listener {

    private View mRootView;
    private static final String SCREEN_OFF_TIMEOUT = "screen_off_timeout";
    private static final int SCREEN_OFF_TIMEOUT_10S = 10 * 1000;
    private static final int SCREEN_OFF_TIMEOUT_1M = 60 * 1000;
    private static final int SCREEN_OFF_TIMEOUT_5M = 5 * 50 * 1000;
    private static final int SCREEN_OFF_TIMEOUT_10M = 10 * 60 * 1000;
    private static final int SCREEN_OFF_TIMEOUT_1H = 60 * 60 * 1000;
    private int screen_off_timeout;
    private String[] items;
    private int checkIndex = 0;
    private WidgetSettingItem settingItem;
    private AlertDialog dialog;

    private String versionName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_setting_utilities, container, false);
        screen_off_timeout = (int) SPUtils.get(getActivity(), SCREEN_OFF_TIMEOUT, SCREEN_OFF_TIMEOUT_1M);
        items = getResources().getStringArray(R.array.screen_timeout_array);
        updateVersion();
        switch (screen_off_timeout) {
            case SCREEN_OFF_TIMEOUT_10S:
                checkIndex = 0;
                break;
            case SCREEN_OFF_TIMEOUT_1M:
                checkIndex = 1;
                break;
            case SCREEN_OFF_TIMEOUT_5M:
                checkIndex = 2;
                break;
            case SCREEN_OFF_TIMEOUT_10M:
                checkIndex = 3;
                break;
            case SCREEN_OFF_TIMEOUT_1H:
                checkIndex = 4;
                break;
            default:
                break;
        }
        settingItem = (WidgetSettingItem) mRootView.findViewById(R.id.item_timeout_lock);
        Settings.System.putInt(getActivity().getContentResolver(),
                android.provider.Settings.System.SCREEN_OFF_TIMEOUT, screen_off_timeout);
        if (settingItem != null) {
            settingItem.setItemValue(items[checkIndex]);
        }
        return mRootView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_COMM, this);
        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_GLUCOSE, this);
        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_MONITOR, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_COMM, this);
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_GLUCOSE, this);
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_MONITOR, this);
    }


    private void updateVersion() {
        //获取当前版本号getPackageName()是你当前类的包名，0代表是获取版本信息版本名称
        PackageInfo pi = null;
        try {
            pi = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String name = pi != null ? pi.versionName : null;
        if (TextUtils.isEmpty(name)) {
            return;
        }
        versionName = name;
//        WidgetSettingItem settingItem = (WidgetSettingItem) mRootView.findViewById(R.id.item_software_version);
//        if (settingItem != null) {
//            settingItem.setItemValue(name);
//        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        switch (v.getId()) {
            case R.id.ibt_back:
                ((ActivityPDA) getActivity())
                        .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                                ParameterComm.SETTING_TYPE,
                                new byte[]{(byte) TYPE_SETTING}));
                break;
            case R.id.item_language:
                setLanguage();
                break;

            case R.id.item_timeout_lock:
                setTimeOutLock();
                break;

            case R.id.item_recovery:
                recovery();
                break;

            case R.id.item_data_test:
                startActivity(new Intent(getActivity(), ActivityDataTest.class));
                break;
            case R.id.item_about:
                showVersionMsg();
                break;
            default:
                break;
        }
    }

    @Override
    public void onReceive(EntityMessage message) {
        switch (message.getOperation()) {
            case EntityMessage.OPERATION_SET:
                handleSet(message);
                break;

            case EntityMessage.OPERATION_GET:
                break;

            case EntityMessage.OPERATION_EVENT:
                handleEvent(message);
                break;

            case EntityMessage.OPERATION_NOTIFY:
                handleNotification(message);
                break;

            case EntityMessage.OPERATION_ACKNOWLEDGE:
                handleAcknowledgement(message);
                break;

            case EntityMessage.OPERATION_UNPAIR:
                handleUnPair(message);
                break;
            default:
                break;
        }
    }

    private void handleSet(EntityMessage message) {
    }


    protected void handleEvent(EntityMessage message) {
        switch (message.getEvent()) {
            case EntityMessage.EVENT_TIMEOUT:
                dismissDialogProgress();
                break;

            default:
                break;
        }
    }

    protected void handleNotification(EntityMessage message) {
    }


    protected void handleAcknowledgement(final EntityMessage message) {
    }

    private void handleUnPair(EntityMessage message) {
        byte[] data = message.getData();
        if (data[0] == EntityMessage.FUNCTION_OK) {
            dismissDialogProgress();
            pair(RFAddress.RF_ADDRESS_UNPAIR);
            ((ActivityPDA) getActivity())
                    .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                            ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_SET,
                            ParameterComm.RESET_DATA,
                            new byte[]{(byte) 3}));
            ((ActivityPDA) getActivity())
                    .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                            ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                            ParameterComm.UNPAIRNOSIGNAL,
                            null));
            ((ActivityPDA) getActivity()).handleMessage(
                    new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                            ParameterGlobal.PORT_MONITOR,
                            ParameterGlobal.PORT_MONITOR,
                            EntityMessage.OPERATION_SET,
                            ParameterComm.UNPAIR_SUCCESS,
                            null));
            recovery();
        }
    }

    private void setLanguage() {
        String[] languageItems = getResources().getStringArray(R.array.language_array);
        final int languageIndex;
        String language = Locale.getDefault().getLanguage();
        switch (language) {
            case "zh":
                languageIndex = 0;
                break;
            case "en":
                languageIndex = 1;
                break;
            default:
                languageIndex = 0;
                break;
        }
        final FragmentSingleChoice fragmentInput = new FragmentSingleChoice();
        Bundle bundle = new Bundle();
        bundle.putStringArray("items", languageItems);
        bundle.putInt("checkIndex", languageIndex);
        fragmentInput.setArguments(bundle);
        showDialogConfirm(getString(R.string.setting_general_language), "", "",
                fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                int index = fragmentInput.getCheckIndex();
                                if (languageIndex == index) {
                                    return true;
                                }
                                switch (index) {
                                    case 0:
                                        updateLanguage(Locale.SIMPLIFIED_CHINESE);
                                        break;
                                    case 1:
                                        updateLanguage(Locale.ENGLISH);
                                        break;
                                    default:

                                        break;
                                }
                                break;

                            default:
                                break;
                        }

                        return true;
                    }
                });
    }

    private void updateLanguage(Locale paramLocale) {
        try {
            @SuppressLint("PrivateApi") Class localClass1 =
                    Class.forName("android.app.ActivityManagerNative");
            Object localObject1 =
                    localClass1.getMethod("getDefault", new Class[0])
                            .invoke(localClass1, new Object[0]);
            Object localObject2 = localObject1.getClass()
                    .getMethod("getConfiguration", new Class[0])
                    .invoke(localObject1, new Object[0]);
            localObject2.getClass().getDeclaredField("locale").set(localObject2,
                    paramLocale);
            localObject2.getClass().getDeclaredField("userSetLocale")
                    .setBoolean(localObject2, true);
            Class localClass2 = localObject1.getClass();
            Class[] arrayOfClass = new Class[1];
            arrayOfClass[0] = Configuration.class;
            Method localMethod =
                    localClass2.getMethod("updateConfiguration", arrayOfClass);
            Object[] arrayOfObject = new Object[1];
            arrayOfObject[0] = localObject2;
            localMethod.invoke(localObject1, arrayOfObject);

//            重新启动Activity
            Intent intent = new Intent(getActivity(), ActivityMain.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            // 杀掉进程
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);

//            finish();
//            Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
//            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(i);


//            finish();
//            startActivity(new Intent(this, ActivityTransform.class));
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }

    @SuppressLint("SetTextI18n")
    private void showVersionMsg() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View v = inflater.inflate(R.layout.layout_version, null);
        TextView title = (TextView) v.findViewById(R.id.title);
        TextView message = (TextView) v.findViewById(R.id.message);
        TextView ok = (TextView) v.findViewById(R.id.ok);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setCancelable(true);

        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams lp = window.getAttributes();

        lp.width = 260;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(lp);
        window.setContentView(v);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_HOME:
                        dialog.dismiss();
                        return true;
                }
                return false;
            }
        });
//        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);

        title.setText(R.string.about);
        message.setText(getResources().getString(R.string.setting_general_version) + versionName);
        ok.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private void setTimeOutLock() {
        final FragmentSingleChoice fragmentInput = new FragmentSingleChoice();
        Bundle bundle = new Bundle();
        bundle.putStringArray("items", items);
        bundle.putInt("checkIndex", checkIndex);
        fragmentInput.setArguments(bundle);
        showDialogConfirm(getString(R.string.timeout_lock), "", "",
                fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                checkIndex = fragmentInput.getCheckIndex();
                                switch (checkIndex) {
                                    case 0:
                                        screen_off_timeout = SCREEN_OFF_TIMEOUT_10S;
                                        break;
                                    case 1:
                                        screen_off_timeout = SCREEN_OFF_TIMEOUT_1M;
                                        break;
                                    case 2:
                                        screen_off_timeout = SCREEN_OFF_TIMEOUT_5M;
                                        break;
                                    case 3:
                                        screen_off_timeout = SCREEN_OFF_TIMEOUT_10M;
                                        break;
                                    case 4:
                                        screen_off_timeout = SCREEN_OFF_TIMEOUT_1H;
                                        break;
                                    default:
                                        break;
                                }
                                SPUtils.put(getActivity(), SCREEN_OFF_TIMEOUT, screen_off_timeout);
                                Settings.System.putInt(getActivity().getContentResolver(),
                                        android.provider.Settings.System.SCREEN_OFF_TIMEOUT, screen_off_timeout);
                                settingItem.setItemValue(items[checkIndex]);
                                break;

                            default:
                                break;
                        }

                        return true;
                    }
                });

//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
//                .setTitle(R.string.timeout_lock)
//                .setCancelable(true)
//                .setSingleChoiceItems(items, checkIndex, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface arg0, int index) {
//                        switch (index) {
//                            case 0:
//                                screen_off_timeout = SCREEN_OFF_TIMEOUT_10S;
//                                break;
//                            case 1:
//                                screen_off_timeout = SCREEN_OFF_TIMEOUT_1M;
//                                break;
//                            case 2:
//                                screen_off_timeout = SCREEN_OFF_TIMEOUT_5M;
//                                break;
//                            case 3:
//                                screen_off_timeout = SCREEN_OFF_TIMEOUT_10M;
//                                break;
//                            case 4:
//                                screen_off_timeout = SCREEN_OFF_TIMEOUT_1H;
//                                break;
//                            default:
//                                break;
//                        }
//                    }
//                })
//                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        SPUtils.put(getActivity(), SCREEN_OFF_TIMEOUT, screen_off_timeout);
//                        Settings.System.putInt(getActivity().getContentResolver(),
//                                android.provider.Settings.System.SCREEN_OFF_TIMEOUT, screen_off_timeout);
//                        switch (screen_off_timeout) {
//                            case SCREEN_OFF_TIMEOUT_10S:
//                                checkIndex = 0;
//                                break;
//                            case SCREEN_OFF_TIMEOUT_1M:
//                                checkIndex = 1;
//                                break;
//                            case SCREEN_OFF_TIMEOUT_5M:
//                                checkIndex = 2;
//                                break;
//                            case SCREEN_OFF_TIMEOUT_10M:
//                                checkIndex = 3;
//                                break;
//                            case SCREEN_OFF_TIMEOUT_1H:
//                                checkIndex = 4;
//                                break;
//                            default:
//                                break;
//                        }
//                        settingItem.setItemValue(items[checkIndex]);
//                    }
//                })
//                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//
//                    }
//                });
//        dialog = builder.create();
//        dialog.show();
//        setCustomDialogStyle(dialog);
//        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
//            @Override
//            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
//                switch (keyCode) {
//                    case KeyEvent.KEYCODE_HOME:
//                        dialog.dismiss();
//                        return true;
//                }
//                return false;
//            }
//        });
//        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
    }

    public static void setCustomDialogStyle(AlertDialog dialog) {
        Button button1 = (Button) dialog.findViewById(android.R.id.button1);//设置底部Button
        button1.setTextColor(Color.WHITE);//文字颜色

        Button button2 = (Button) dialog.findViewById(android.R.id.button2);
        button2.setTextColor(Color.WHITE);
    }

    private void recovery() {
        String mRFAddress = getAddress(((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .getExtras(ActivityPDA.SETTING_RF_ADDRESS, null));

        if ((mRFAddress.equals("")) || (mRFAddress.equals(RFAddress.RF_ADDRESS_UNPAIR))) {
            FragmentInput fragmentInput = new FragmentInput();
            fragmentInput.setComment(getString(R.string.recovery_clear));
            showDialogConfirm(getString(R.string.recovery), "",
                    "", fragmentInput, new FragmentDialog.ListenerDialog() {
                        @Override
                        public boolean onButtonClick(int buttonID, Fragment content) {
                            switch (buttonID) {
                                case FragmentDialog.BUTTON_ID_POSITIVE:
                                    restoreHg();
                                    cleanCache();
                                    ((ActivityPDA) getActivity()).handleMessage(
                                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    EntityMessage.OPERATION_SET,
                                                    ParameterComm.BROADCAST_SAVA, new byte[]
                                                    {
                                                            (byte) 1
                                                    }));
                                    ((ActivityPDA) getActivity()).handleMessage(
                                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    EntityMessage.OPERATION_SET,
                                                    ParameterComm.BROADCAST_SAVA, new byte[]
                                                    {
                                                            (byte) 1
                                                    }));
                                    ((ActivityPDA) getActivity()).handleMessage(
                                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    EntityMessage.OPERATION_SET,
                                                    ParameterMonitor.COUNTDOWNVIEW_VISIBLE,
                                                    new ValueInt(0).getByteArray()));
                                    clearPair();
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.set(Calendar.YEAR, 2000);
                                    calendar.set(Calendar.MONTH, 0);
                                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                                    calendar.set(Calendar.MINUTE, 0);
                                    calendar.set(Calendar.SECOND, 0);
                                    SystemClock.setCurrentTimeMillis(calendar.getTimeInMillis());
                                    ((ActivityPDA) getActivity())
                                            .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                                                    ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                                                    ParameterComm.SETTING_TYPE,
                                                    new byte[]{(byte) TYPE_DATE_TIME}));
                                    break;

                                default:
                                    break;
                            }
                            return true;
                        }
                    });
        } else {
            final FragmentUnPair fragmentUnPair = new FragmentUnPair();
            fragmentUnPair
                    .setComment(getString(R.string.unpair_recovery));
            showDialogConfirm(getString(R.string.recovery), "",
                    "", fragmentUnPair, new FragmentDialog.ListenerDialog() {
                        @Override
                        public boolean onButtonClick(int buttonID, Fragment content) {
                            switch (buttonID) {
                                case FragmentDialog.BUTTON_ID_POSITIVE:
                                    if (fragmentUnPair.getIsRemove()) {
                                        ((ActivityPDA) getActivity())
                                                .getDataStorage(
                                                        ActivityPDA.class.getSimpleName())
                                                .setExtras(ActivityPDA.SETTING_RF_ADDRESS,
                                                        null);
                                        ((ActivityPDA) getActivity())
                                                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                                                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_SET,
                                                        ParameterComm.RESET_DATA,
                                                        new byte[]{(byte) 3}));
                                        ((ActivityPDA) getActivity())
                                                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                                                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                                                        ParameterComm.UNPAIRNOSIGNAL,
                                                        null));
                                        recovery();
                                    } else {
                                        showDialogProgress();
                                        ((ActivityPDA) getActivity())
                                                .handleMessage(new EntityMessage(
                                                        ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                        ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                                        ParameterGlobal.PORT_COMM,
                                                        ParameterGlobal.PORT_COMM,
                                                        EntityMessage.OPERATION_UNPAIR,
                                                        ParameterComm.PARAM_MAC,
                                                        new byte[]{}));
                                    }
                                    break;

                                default:
                                    break;
                            }
                            return true;
                        }
                    });
        }
    }

    private void clearPair() {
        ((ActivityPDA) getActivity())
                .getDataStorage(
                        ActivityPDA.class.getSimpleName())
                .setExtras(ActivityPDA.SETTING_RF_ADDRESS,
                        null);
        // Set remote address
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.PARAM_RF_LOCAL_ADDRESS,
                        new RFAddress(RFAddress.RF_ADDRESS_UNPAIR).getByteArray()));

        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.PARAM_RF_REMOTE_ADDRESS,
                        new RFAddress(RFAddress.RF_ADDRESS_UNPAIR).getByteArray()));
    }

    private void restoreHg() {
//        String mRFAddress = getAddress(((ActivityPDA) getActivity())
//                .getDataStorage(ActivityPDA.class.getSimpleName())
//                .getExtras(ActivityPDA.SETTING_RF_ADDRESS, null));
//        if ((!mRFAddress.equals("")) && (!mRFAddress
//                .equals(RFAddress.RF_ADDRESS_UNPAIR))) {
//            ((ActivityPDA) getActivity())
//                    .handleMessage(new EntityMessage(
//                            ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
//                            ParameterGlobal.PORT_GLUCOSE,
//                            ParameterGlobal.PORT_GLUCOSE,
//                            EntityMessage.OPERATION_SET,
//                            ParameterGlucose.PARAM_BG_LIMIT,
//                            new ValueByte(HYPER_DEFAULT).getByteArray()
//                    ));
//            ((ActivityPDA) getActivity())
//                    .handleMessage(new EntityMessage(
//                            ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
//                            ParameterGlobal.PORT_GLUCOSE,
//                            ParameterGlobal.PORT_GLUCOSE,
//                            EntityMessage.OPERATION_SET,
//                            ParameterGlucose.PARAM_FILL_LIMIT,
//                            new ValueByte(HYPO_DEFAULT).getByteArray()
//                    ));
//        }
        updateHyper(HYPER_DEFAULT);
        updateHypo(HYPO_DEFAULT);
    }

    private void cleanCache() {
        ArrayList<DbHistory> errList = ((ApplicationPDA) getActivity().getApplication()).getDataErrListAll();
        errList.clear();
        ((ApplicationPDA) getActivity().getApplication()).setDataErrListAll(errList);
        List<History> dataList = ((ApplicationPDA) getActivity().getApplication()).getDataListAll();
        dataList.clear();
        ((ApplicationPDA) getActivity().getApplication()).setDataListAll(dataList);
        DataCleanUtil.cleanSharedPreference(getActivity());
        SPUtils.clear(getActivity());
        Settings.System.putInt(getActivity().getContentResolver(),
                android.provider.Settings.System.SCREEN_OFF_TIMEOUT, SCREEN_OFF_TIMEOUT_10S);
        ((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .clear();
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_CONTROL, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.CLEAN_DATABASES,
                        new byte[]{}));
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_CONTROL, ParameterGlobal.PORT_MONITOR,
                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_SET,
                        ParameterComm.CLEAN_DATABASES,
                        new byte[]{}));
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.CLEAN_DATABASES,
                        new byte[]{}));
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_SET,
                        ParameterComm.RESET_DATA,
                        new byte[]{(byte) 0}));
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.UNPAIRNOSIGNAL,
                        null));
    }

    private void updateHyper(int hyper) {
        ((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .setInt(SETTING_HYPER, hyper);
    }

    private void updateHypo(int hypo) {
        ((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .setInt(SETTING_HYPO, hypo);
    }

    private void showDialogConfirm(String title, String buttonTextPositive,
                                   String buttonTextNegative, Fragment content,
                                   FragmentDialog.ListenerDialog listener) {
        final FragmentDialog fragmentDialog = new FragmentDialog();
        fragmentDialog.setHomeCancelFlag(true);
        fragmentDialog.setTitle(title);
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                buttonTextPositive);
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
                buttonTextNegative);
        fragmentDialog.setContent(content);
        fragmentDialog.setListener(listener);
        fragmentDialog.show(getChildFragmentManager(), null);
    }

    public String getAddress(byte[] addressByte) {
        if (addressByte != null) {
            for (int i = 0; i < addressByte.length; i++) {
                if (addressByte[i] < 10) {
                    addressByte[i] += '0';
                } else {
                    addressByte[i] -= 10;
                    addressByte[i] += 'A';
                }
            }

            return new String(addressByte);
        } else {
            return "";
        }
    }

    private void pair(String addressString) {

        if (addressString.equals(RFAddress.RF_ADDRESS_UNPAIR)) {
            ((ActivityPDA) getActivity())
                    .getDataStorage(
                            ActivityPDA.class.getSimpleName())
                    .setExtras(ActivityPDA.SETTING_RF_ADDRESS,
                            null);
        } else {
            ((ActivityPDA) getActivity())
                    .getDataStorage(
                            ActivityPDA.class.getSimpleName())
                    .setExtras(ActivityPDA.SETTING_RF_ADDRESS,
                            new RFAddress(addressString).getByteArray());
        }

        // Set remote address
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.PARAM_RF_LOCAL_ADDRESS,
                        new RFAddress(addressString).getByteArray()));

        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.PARAM_RF_REMOTE_ADDRESS,
                        new RFAddress(addressString).getByteArray()));
    }

    private void showDialogProgress() {
        ((ActivityPDA) getActivity()).showDialogProgress();
    }


    private void dismissDialogProgress() {
        ((ActivityPDA) getActivity()).dismissDialogProgress();
    }
}
