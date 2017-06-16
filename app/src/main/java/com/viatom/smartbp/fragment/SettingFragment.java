package com.viatom.smartbp.fragment;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.viatom.smartbp.R;
import com.viatom.smartbp.dfu.DfuService;
import com.viatom.smartbp.utility.Constant;
import com.viatom.smartbp.utility.LogUtils;

import org.json.JSONObject;

import java.io.File;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFragment extends Fragment implements UploadCancelFragment.CancelFragmentListener {
    private static final String TAG = "Setting";

    private static final String PREFS_DEVICE_NAME = "PREFS_DEVICE_NAME";
    private static final String PREFS_FILE_NAME = "PREFS_FILE_NAME";
    private static final String PREFS_FILE_TYPE = "PREFS_FILE_TYPE";
    private static final String PREFS_FILE_SIZE = "PREFS_FILE_SIZE";

    private static final String DATA_STATUS = "status";
    private static final String DATA_DFU_COMPLETED = "dfu_completed";
    private static final String DATA_DFU_ERROR = "dfu_error";

    /* smartBP dfu */
//    private static final File DFU_FILE = new File(Constant.download_dir,"dfu_smbp_0_0_13_v4.zip");
    /* snore O2 */
    private static final File DFU_FILE = new File(Constant.download_dir,"SnoreO2_app_1.3.5.zip");

    private View rootView;

    public TextView mAppVersion;
    public TextView mDfv;
    public TextView mUpdateDfv;
    public TextView mFileNameView;
    public TextView mFileTypeView;
    public TextView mFileSizeView;
    public TextView mFileStatusView;
    public TextView mTextPercentage;
    public TextView mTextUploading;
    public ProgressBar mProgressBar;
    public Button mUploadButton;

    public String mFilePath;
    public Uri mFileStreamUri;
    public int mFileType;
    public boolean mStatusOk;
    public boolean mResumed;
    public boolean mDfuCompleted;
    public String mDfuError;

    private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(final String deviceAddress) {
            mProgressBar.setIndeterminate(true);
            mTextPercentage.setText(R.string.dfu_status_connecting);
        }

        @Override
        public void onDfuProcessStarting(final String deviceAddress) {
            mProgressBar.setIndeterminate(true);
            mTextPercentage.setText(R.string.dfu_status_starting);
        }

        @Override
        public void onEnablingDfuMode(final String deviceAddress) {
            mProgressBar.setIndeterminate(true);
            mTextPercentage.setText(R.string.dfu_status_switching_to_dfu);
        }

        @Override
        public void onFirmwareValidating(final String deviceAddress) {
            mProgressBar.setIndeterminate(true);
            mTextPercentage.setText(R.string.dfu_status_validating);
        }

        @Override
        public void onDeviceDisconnecting(final String deviceAddress) {
            mProgressBar.setIndeterminate(true);
            mTextPercentage.setText(R.string.dfu_status_disconnecting);
        }

        @Override
        public void onDfuCompleted(final String deviceAddress) {
            mTextPercentage.setText(R.string.dfu_status_completed);
            if (mResumed) {
                // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onTransferCompleted();

                        // if this activity is still open and upload process was completed, cancel the notification
                        final NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                        manager.cancel(DfuService.NOTIFICATION_ID);
                    }
                }, 200);
            } else {
                // Save that the DFU process has finished
                mDfuCompleted = true;
            }
        }

        @Override
        public void onDfuAborted(final String deviceAddress) {
            mTextPercentage.setText(R.string.dfu_status_aborted);
            // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onUploadCanceled();

                    // if this activity is still open and upload process was completed, cancel the notification
                    final NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(DfuService.NOTIFICATION_ID);
                }
            }, 200);
        }

        @Override
        public void onProgressChanged(final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
            mProgressBar.setIndeterminate(false);
            mProgressBar.setProgress(percent);
            mTextPercentage.setText(getString(R.string.dfu_uploading_percentage, percent));
            if (partsTotal > 1)
                mTextUploading.setText(getString(R.string.dfu_status_uploading_part, currentPart, partsTotal));
            else
                mTextUploading.setText(R.string.dfu_status_uploading);
        }

        @Override
        public void onError(final String deviceAddress, final int error, final int errorType, final String message) {
            if (mResumed) {
                showErrorMessage(message);

                // We have to wait a bit before canceling notification. This is called before DfuService creates the last notification.
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // if this activity is still open and upload process was completed, cancel the notification
                        final NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                        manager.cancel(DfuService.NOTIFICATION_ID);
                    }
                }, 200);
            } else {
                mDfuError = message;
            }
        }
    };

    public SettingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_setting, container, false);
        setUI();
        return rootView;

        // file helper
    }


    public void onDestory() {
        super.onDestroy();
        DfuServiceListenerHelper.unregisterProgressListener(getActivity(), mDfuProgressListener);
    }


    private void setUI() {
        mFileType = DfuService.TYPE_AUTO;
        DfuServiceListenerHelper.registerProgressListener(getActivity(), mDfuProgressListener);

        // set app and device version
        mAppVersion = (TextView) rootView.findViewById(R.id.setting_app_version);
        mDfv = (TextView) rootView.findViewById(R.id.setting_dfv);
        mUpdateDfv = (TextView) rootView.findViewById(R.id.setting_update_dfv);
        mFileNameView = (TextView) rootView.findViewById(R.id.file_name);
        mFileTypeView = (TextView) rootView.findViewById(R.id.file_type);
        mFileSizeView = (TextView) rootView.findViewById(R.id.file_size);
        mFileStatusView = (TextView) rootView.findViewById(R.id.file_status);

        mAppVersion.setText(getAppVersion());
        mDfv.setText(getDfv());
        mFileNameView.setText(DFU_FILE.getName());
        mFileTypeView.setText("");
        mFileSizeView.setText(DFU_FILE.length() + "B");
        mFileStatusView.setText(DATA_STATUS);
        mFileStreamUri = Uri.fromFile(DFU_FILE);

        mUploadButton = (Button) rootView.findViewById(R.id.action_upload);
        mTextPercentage = (TextView) rootView.findViewById(R.id.textViewProgress);
        mTextUploading = (TextView) rootView.findViewById(R.id.textViewUploading);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressbar_file);

        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onUploadClicked(view);
            }
        });

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (isDfuServiceRunning()) {
            mFileNameView.setText(preferences.getString(PREFS_FILE_NAME, ""));
            mFileTypeView.setText(preferences.getString(PREFS_FILE_TYPE, ""));
            mFileSizeView.setText(preferences.getString(PREFS_FILE_SIZE, ""));
            // check file
            mFileStatusView.setText(R.string.dfu_file_status_ok);
            mStatusOk = true;
            showProgressBar();
        }

    }

    public void refreshUI() {
        //
    }

    /*
    * DFU
    *
    **/
    public void onUploadClicked(final View view){
        if (isDfuServiceRunning()) {
            showUploadCancelDialog();
            return;
        }

        if (!mStatusOk) {
            showToast("duf file is bot valid");
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFS_FILE_NAME, mFileNameView.getText().toString());
        editor.putString(PREFS_FILE_TYPE, mFileTypeView.getText().toString());
        editor.putString(PREFS_FILE_SIZE, mFileSizeView.getText().toString());
        editor.apply();

        showProgressBar();

        final boolean keepBond = false;
        final boolean forceDfu = false;
        final boolean enablePRNs = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ;
        String value = String.valueOf(DfuServiceInitiator.DEFAULT_PRN_VALUE);
        int numberOfPackets = Integer.parseInt(value);

        final DfuServiceInitiator starter = new DfuServiceInitiator(Constant.selectedDevice.getAddress())
                .setDeviceName(Constant.selectedDevice.getName())
                .setKeepBond(keepBond)
                .setForceDfu(forceDfu)
                .setPacketsReceiptNotificationsEnabled(enablePRNs)
                .setPacketsReceiptNotificationsValue(numberOfPackets)
                .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true);
        starter.setZip(mFileStreamUri, mFileStreamUri.getPath());
        starter.start(getActivity(), DfuService.class);

    }

    private void showUploadCancelDialog() {
        final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity());
        final Intent pauseAction = new Intent(DfuService.BROADCAST_ACTION);
        pauseAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_PAUSE);
        manager.sendBroadcast(pauseAction);

        final UploadCancelFragment fragment = UploadCancelFragment.getInstance();
        fragment.show(getActivity().getSupportFragmentManager(), "Device Firmware Update");
    }

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mTextPercentage.setVisibility(View.VISIBLE);
        mTextPercentage.setText(null);
        mTextUploading.setText(R.string.dfu_status_uploading);
        mTextUploading.setVisibility(View.VISIBLE);
        mUploadButton.setEnabled(true);
        mUploadButton.setText(R.string.dfu_action_upload_cancel);
    }

    private void onTransferCompleted() {
        clearUI(true);
        showToast(R.string.dfu_success);
    }

    public void onUploadCanceled() {
        clearUI(false);
        showToast(R.string.dfu_aborted);
    }

    @Override
    public void onCancelUpload() {
        mProgressBar.setIndeterminate(true);
        mTextUploading.setText(R.string.dfu_status_aborting);
        mTextPercentage.setText(null);
    }

    private void showErrorMessage(final String message) {
//        clearUI(false);
        showToast("Upload failed: " + message);
    }

    private void clearUI(final boolean clearDevice) {
        mProgressBar.setVisibility(View.INVISIBLE);
        mTextPercentage.setVisibility(View.INVISIBLE);
        mTextUploading.setVisibility(View.INVISIBLE);
        mUploadButton.setEnabled(false);
        mUploadButton.setText(R.string.dfu_action_upload);
        if (clearDevice) {
            //
        }
        mStatusOk = false;
    }

    // get APP version
    private String getAppVersion() {
        String appVersion = "";
        try {
            PackageManager manager = getActivity().getPackageManager();
            PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);
            appVersion = info.versionName;
        } catch (Exception e) {
            LogUtils.d(e.toString());
        }
        return appVersion == null ? getString(R.string.not_available_value) : appVersion;
    }

    // get device firmware version
    private String getDfv() {
        String dfv = "";
        //
        return dfv == null ? getString(R.string.not_available_value) : dfv;
    }

    // get update dfv
    public void getUpdateDfv(String updateDfv) {
        mUpdateDfv.setText(updateDfv);
    }

    private void showToast(final int messageResId) {
        Toast.makeText(getActivity(), messageResId, Toast.LENGTH_SHORT).show();
    }

    private void showToast(final String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    private boolean isDfuServiceRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DfuService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
