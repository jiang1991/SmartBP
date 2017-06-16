package com.viatom.smartbp.dfu;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.viatom.smartbp.MainActivity;
import com.viatom.smartbp.R;
import com.viatom.smartbp.fragment.PermissionRationaleFragment;
import com.viatom.smartbp.fragment.UploadCancelFragment;

import java.io.File;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class SettingActivity extends AppCompatActivity implements UploadCancelFragment.CancelFragmentListener {

    private static final String TAG = "DfuActivity";


    private static final String DATA_DEVICE = "device";
    private static final String DATA_FILE_TYPE = "file_type";
    private static final String DATA_FILE_TYPE_TMP = "file_type_tmp";
    private static final String DATA_FILE_PATH = "file_path";
    private static final String DATA_FILE_STREAM = "file_stream";
    private static final String DATA_INIT_FILE_PATH = "init_file_path";
    private static final String DATA_INIT_FILE_STREAM = "init_file_stream";
    private static final String DATA_STATUS = "status";
    private static final String DATA_DFU_COMPLETED = "dfu_completed";
    private static final String DATA_DFU_ERROR = "dfu_error";

    private static final String EXTRA_URI = "uri";

    private static final int PERMISSION_REQ = 25;
    private static final int ENABLE_BT_REQ = 0;
    private static final int SELECT_FILE_REQ = 1;
    private static final int SELECT_INIT_FILE_REQ = 2;

    private TextView mTextPercentage;
    private TextView mTextUploading;
    private ProgressBar mProgressBar;

    private Button mUploadButton, mConnectButton;

    private BluetoothDevice mSelectedDevice;
    private String mFilePath;
    private Uri mFileStreamUri;
    private String mInitFilePath;
    private Uri mInitFileStreamUri;
    private int mFileType;
    private int mFileTypeTmp;
    private boolean mStatusOk;
    private boolean mResumed;
    private boolean mDfuCompleted;
    private String mDfuError;

    public Toolbar toolbar;
    public BottomNavigationView bnve;

    /**
     * The progress listener receives events from the DFU Service.
     * If is registered in onCreate() and unregistered in onDestroy() so methods here may also be called
     * when the screen is locked or the app went to the background. This is because the UI needs to have the
     * correct information after user comes back to the activity and this information can't be read from the service
     * as it might have been killed already (DFU completed or finished with error).
     */
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
                        final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
                    final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
                        final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        manager.cancel(DfuService.NOTIFICATION_ID);
                    }
                }, 200);
            } else {
                mDfuError = message;
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        setGUI();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//            FileHelper.createSamples(this);
        } else {
            final DialogFragment dialog = PermissionRationaleFragment.getInstance(R.string.permission_sd_text, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            dialog.show(getSupportFragmentManager(), null);
        }

        // restore saved state
        mFileType = DfuService.TYPE_AUTO; // Default
        if (savedInstanceState != null) {
            mFileType = savedInstanceState.getInt(DATA_FILE_TYPE);
            mFileTypeTmp = savedInstanceState.getInt(DATA_FILE_TYPE_TMP);
            mFilePath = savedInstanceState.getString(DATA_FILE_PATH);
            mFileStreamUri = savedInstanceState.getParcelable(DATA_FILE_STREAM);
            mInitFilePath = savedInstanceState.getString(DATA_INIT_FILE_PATH);
            mInitFileStreamUri = savedInstanceState.getParcelable(DATA_INIT_FILE_STREAM);
            mSelectedDevice = savedInstanceState.getParcelable(DATA_DEVICE);
            mStatusOk = mStatusOk || savedInstanceState.getBoolean(DATA_STATUS);
            mUploadButton.setEnabled(mSelectedDevice != null && mStatusOk);
            mDfuCompleted = savedInstanceState.getBoolean(DATA_DFU_COMPLETED);
            mDfuError = savedInstanceState.getString(DATA_DFU_ERROR);
        }

        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(DATA_FILE_TYPE, mFileType);
        outState.putInt(DATA_FILE_TYPE_TMP, mFileTypeTmp);
        outState.putString(DATA_FILE_PATH, mFilePath);
        outState.putParcelable(DATA_FILE_STREAM, mFileStreamUri);
        outState.putString(DATA_INIT_FILE_PATH, mInitFilePath);
        outState.putParcelable(DATA_INIT_FILE_STREAM, mInitFileStreamUri);
        outState.putParcelable(DATA_DEVICE, mSelectedDevice);
        outState.putBoolean(DATA_STATUS, mStatusOk);
        outState.putBoolean(DATA_DFU_COMPLETED, mDfuCompleted);
        outState.putString(DATA_DFU_ERROR, mDfuError);
    }

    private void setGUI() {

        mUploadButton = (Button) findViewById(R.id.action_upload);
        mTextPercentage = (TextView) findViewById(R.id.textViewProgress);
        mTextUploading = (TextView) findViewById(R.id.textViewUploading);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar_file);

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.nav_setting);
        bnve = (BottomNavigationView) findViewById(R.id.bnve);
        bnve.getMenu().getItem(2).setChecked(true);
        bnve.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_measure:
                        startActivity(new Intent().setClass(SettingActivity.this, MainActivity.class));
                        return true;
                    case R.id.nav_history:
                        //
                        return true;
                    default:
                        return false;
                }
            }
        });

        if (isDfuServiceRunning()) {
            mStatusOk = true;
            showProgressBar();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumed = true;
        if (mDfuCompleted)
            onTransferCompleted();
        if (mDfuError != null)
            showErrorMessage(mDfuError);
        if (mDfuCompleted || mDfuError != null) {
            // if this activity is still open and upload process was completed, cancel the notification
            final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(DfuService.NOTIFICATION_ID);
            mDfuCompleted = false;
            mDfuError = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case SELECT_FILE_REQ: {
                // clear previous data
                mFileType = mFileTypeTmp;
                mFilePath = null;
                mFileStreamUri = null;

                // and read new one
                final Uri uri = data.getData();
			/*
			 * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
			 * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
			 */
                if (uri.getScheme().equals("file")) {
                    // the direct path to the file has been returned
                    final String path = uri.getPath();
                    final File file = new File(path);
                    mFilePath = path;

                } else if (uri.getScheme().equals("content")) {
                    // an Uri has been returned
                    mFileStreamUri = uri;
                    // if application returned Uri for streaming, let's us it. Does it works?
                    // FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
                    final Bundle extras = data.getExtras();
                    if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
                        mFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);

                    // file name and size must be obtained from Content Provider
                    final Bundle bundle = new Bundle();
                    bundle.putParcelable(EXTRA_URI, uri);
                }
                break;
            }
            case SELECT_INIT_FILE_REQ: {
                mInitFilePath = null;
                mInitFileStreamUri = null;

                // and read new one
                final Uri uri = data.getData();
			/*
			 * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
			 * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
			 */
                if (uri.getScheme().equals("file")) {
                    // the direct path to the file has been returned
                    mInitFilePath = uri.getPath();
                } else if (uri.getScheme().equals("content")) {
                    // an Uri has been returned
                    mInitFileStreamUri = uri;
                    // if application returned Uri for streaming, let's us it. Does it works?
                    // FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
                    final Bundle extras = data.getExtras();
                    if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
                        mInitFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);
                }
                break;
            }
            default:
                break;
        }
    }



    /**
     * Callback of UPDATE/CANCEL button on DfuActivity
     */
    public void onUploadClicked(final View view) {
        if (isDfuServiceRunning()) {
            showUploadCancelDialog();
            return;
        }

        // Check whether the selected file is a HEX file (we are just checking the extension)
        if (!mStatusOk) {
            Toast.makeText(this, R.string.dfu_file_status_invalid_message, Toast.LENGTH_LONG).show();
            return;
        }

        // Save current state in order to restore it if user quit the Activity
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        showProgressBar();

        final boolean keepBond = false;
        final boolean forceDfu = false;
        final boolean enablePRNs = Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
        String value = String.valueOf(DfuServiceInitiator.DEFAULT_PRN_VALUE);
        int numberOfPackets;
        try {
            numberOfPackets = Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            numberOfPackets = DfuServiceInitiator.DEFAULT_PRN_VALUE;
        }

        final DfuServiceInitiator starter = new DfuServiceInitiator(mSelectedDevice.getAddress())
                .setDeviceName(mSelectedDevice.getName())
                .setKeepBond(keepBond)
                .setForceDfu(forceDfu)
                .setPacketsReceiptNotificationsEnabled(enablePRNs)
                .setPacketsReceiptNotificationsValue(numberOfPackets)
                .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true);
        if (mFileType == DfuService.TYPE_AUTO)
            starter.setZip(mFileStreamUri, mFilePath);
        else {
            starter.setBinOrHex(mFileType, mFileStreamUri, mFilePath).setInitFile(mInitFileStreamUri, mInitFilePath);
        }
        starter.start(this, DfuService.class);
    }

    private void showUploadCancelDialog() {
        final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        final Intent pauseAction = new Intent(DfuService.BROADCAST_ACTION);
        pauseAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_PAUSE);
        manager.sendBroadcast(pauseAction);

        final UploadCancelFragment fragment = UploadCancelFragment.getInstance();
        fragment.show(getSupportFragmentManager(), TAG);
    }


    /*@Override
    public void onDeviceSelected(final BluetoothDevice device, final String name) {
        mSelectedDevice = device;
        mUploadButton.setEnabled(mStatusOk);
        mDeviceNameView.setText(name != null ? name : getString(R.string.not_available));
    }*/


    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mTextPercentage.setVisibility(View.VISIBLE);
        mTextPercentage.setText(null);
        mTextUploading.setText(R.string.dfu_status_uploading);
        mTextUploading.setVisibility(View.VISIBLE);
        mConnectButton.setEnabled(false);
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
        clearUI(false);
        showToast("Upload failed: " + message);
    }

    private void clearUI(final boolean clearDevice) {
        mProgressBar.setVisibility(View.INVISIBLE);
        mTextPercentage.setVisibility(View.INVISIBLE);
        mTextUploading.setVisibility(View.INVISIBLE);
        mConnectButton.setEnabled(true);
        mUploadButton.setEnabled(false);
        mUploadButton.setText(R.string.dfu_action_upload);
        if (clearDevice) {
            mSelectedDevice = null;
        }
        // Application may have lost the right to these files if Act
        mFilePath = null;
        mFileStreamUri = null;
        mInitFilePath = null;
        mInitFileStreamUri = null;
        mStatusOk = false;
    }

    private void showToast(final int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private void showToast(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean isDfuServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DfuService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
