package com.viatom.smartbp.profile;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.viatom.smartbp.R;
import com.viatom.smartbp.scanner.ScannerFragment;
import com.viatom.smartbp.utility.Constant;

import java.util.UUID;

/**
 * Created by wangjiang on 2017/6/8.
 */

public abstract class BleProfileActivity extends AppCompatActivity implements ScannerFragment.OnDeviceSelectedListener, BleManagerCallbacks {
    private static final String TAG = "BaseProfileActivity";

    private static final String SIS_CONNECTION_STATUS = "connection_status";
    private static final String SIS_DEVICE_NAME = "device_name";
    protected static final int REQUEST_ENABLE_BT = 2;

    private BleManager<? extends BleManagerCallbacks> mBleManager;

    private TextView mDeviceNameView;
    private TextView mBatteryLevelView;
    private Button mConnectButton;

    private boolean mDeviceConnected = false;
    private String mDeviceName;

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }

        mBleManager = initializeManager();

        onInitialize(savedInstanceState);
        onCreateView(savedInstanceState);
        setUpView();
        onViewCreated(savedInstanceState);
    }

    protected void onInitialize(final Bundle savedInstanceState) {
        // empty default implementation
    }

    protected abstract void onCreateView(final Bundle savedInstanceState);

    protected void onViewCreated(final Bundle savedInstanceState) {
        // empty default implementation
    }

    protected final void setUpView() {
        // set GUI
        mDeviceNameView = (TextView) findViewById(R.id.device_name);
        mBatteryLevelView = (TextView) findViewById(R.id.battery);
    }

    @Override
    public void onBackPressed() {
        mBleManager.disconnect();
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SIS_CONNECTION_STATUS, mDeviceConnected);
        outState.putString(SIS_DEVICE_NAME, mDeviceName);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mDeviceConnected = savedInstanceState.getBoolean(SIS_CONNECTION_STATUS);
        mDeviceName = savedInstanceState.getString(SIS_DEVICE_NAME);

        if (mDeviceConnected) {
            mConnectButton.setText(R.string.action_disconnect);
        } else {
            mConnectButton.setText(R.string.action_connect);
        }
    }


    public void onConnectClicked(final View view) {
        if (isBLEEnabled()) {
            if (!mDeviceConnected) {
                setDefaultUI();
                showDeviceScanningDialog(getFilterUUID());
            } else {
                mBleManager.disconnect();
            }
        } else {
            showBLEDialog();
        }
    }

    @Override
    public void onDeviceSelected(final BluetoothDevice device, final String name) {
        Constant.selectedDevice = device;
        showToast(name != null ? name : getString(R.string.not_available));
    }

    @Override
    public void onDialogCanceled() {
        // do nothing
    }

    @Override
    public void onDeviceConnecting(final BluetoothDevice device) {
        // do nothing
    }

    @Override
    public void onDeviceConnected(final BluetoothDevice device) {
        mDeviceConnected = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectButton.setText(R.string.action_disconnect);
            }
        });
    }

    @Override
    public void onDeviceDisconnecting(final BluetoothDevice device) {
        // do nothing
    }

    @Override
    public void onDeviceDisconnected(final BluetoothDevice device) {
        mDeviceConnected = false;
        mBleManager.close();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectButton.setText(R.string.action_connect);
                mDeviceNameView.setText(getDefaultDeviceName());
                mBatteryLevelView.setText(R.string.not_available);
            }
        });
    }

    @Override
    public void onLinklossOccur(final BluetoothDevice device) {
        mDeviceConnected = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mBatteryLevelView != null)
                    mBatteryLevelView.setText(R.string.not_available);
            }
        });
    }

    @Override
    public void onServicesDiscovered(final BluetoothDevice device, boolean optionalServicesFound) {
        // this may notify user or show some views
    }

    @Override
    public void onDeviceReady(final BluetoothDevice device) {
        // empty default implementation
    }

    @Override
    public void onBondingRequired(final BluetoothDevice device) {
        showToast(R.string.bonding);
    }

    @Override
    public void onBonded(final BluetoothDevice device) {
        showToast(R.string.bonded);
    }

    @Override
    public boolean shouldEnableBatteryLevelNotifications(final BluetoothDevice device) {
        // Yes, we want battery level updates
        return true;
    }

    @Override
    public void onBatteryValueReceived(final BluetoothDevice device, final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mBatteryLevelView != null)
                    mBatteryLevelView.setText(getString(R.string.battery, value));
            }
        });
    }

    @Override
    public void onError(final BluetoothDevice device, final String message, final int errorCode) {
        Log.e(TAG, "Error occurred: " + message + ",  error code: " + errorCode);
        showToast(message + " (" + errorCode + ")");
    }

    @Override
    public void onDeviceNotSupported(final BluetoothDevice device) {
        showToast(R.string.not_supported);
    }

    protected void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BleProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void showToast(final int messageResId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BleProfileActivity.this, messageResId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected boolean isDeviceConnected() {
        return mDeviceConnected;
    }

    protected String getDeviceName() {
        return mDeviceName;
    }

    protected abstract BleManager<? extends BleManagerCallbacks> initializeManager();

    protected abstract void setDefaultUI();

    protected abstract int getDefaultDeviceName();

//    protected abstract int getAboutTextId();

    protected abstract UUID getFilterUUID();

    private void showDeviceScanningDialog(final UUID filter) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ScannerFragment dialog = ScannerFragment.getInstance(filter);
                dialog.show(getSupportFragmentManager(), "scan_fragment");
            }
        });
    }

    private void ensureBLESupported() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    protected boolean isBLEEnabled() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    protected void showBLEDialog() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }
}
