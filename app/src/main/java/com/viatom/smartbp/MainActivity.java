package com.viatom.smartbp;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.viatom.smartbp.dfu.DfuService;
import com.viatom.smartbp.dfu.SettingActivity;
import com.viatom.smartbp.fragment.HistoryFragment;
import com.viatom.smartbp.fragment.MeasureMainFragment;
import com.viatom.smartbp.fragment.SettingFragment;
import com.viatom.smartbp.fragment.UploadCancelFragment;
import com.viatom.smartbp.profile.BPManager;
import com.viatom.smartbp.profile.BleManager;
import com.viatom.smartbp.profile.BleManagerCallbacks;
import com.viatom.smartbp.profile.BleProfileActivity;
import com.viatom.smartbp.scanner.ScannerFragment;
import com.viatom.smartbp.utility.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements ScannerFragment.OnDeviceSelectedListener{

    private static final int PERMISSION_REQ = 25;
    private static final int ENABLE_BT_REQ = 0;
    protected static final int REQUEST_ENABLE_BT = 2;

    private BluetoothDevice mSelectedDevice;


    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Constant.init(this);
        Constant.initDir();

        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        } else {
            showDeviceScanningDialog();
        }


    }

    protected UUID getFilterUUID() {
        return null;
    }

    private void showDeviceScanningDialog() {
        final ScannerFragment dialog = ScannerFragment.getInstance(null);
        dialog.show(getSupportFragmentManager(), "scan_fragment");
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ScannerFragment dialog = ScannerFragment.getInstance(filter);
                dialog.show(getSupportFragmentManager(), "scan_fragment");
            }
        });*/
    }

    @Override
    public void onDeviceSelected(final BluetoothDevice device, final String name) {
        Constant.selectedDevice = device;
        Constant.selectedDeviceName = name != null ? name : getString(R.string.not_available);

        Intent intent = new Intent(this, BaseActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDialogCanceled() {
        // do nothing
    }


    /*@Override
    protected UUID getFilterUUID() {
        // change this
        return null;
    }


    @Override
    protected BleManager<BleManagerCallbacks> initializeManager() {
        final BPManager manager = BPManager.getBPManager(getApplicationContext());
        manager.setGattCallbacks(this);
        return manager;
    }

    @Override
    public void onServicesDiscovered(final BluetoothDevice device, final boolean optionalServicesFound) {
        // this may notify user or show some views
    }

    @Override
    protected void setDefaultUI() {
        //
    }

    @Override
    protected int getDefaultDeviceName() {
        return R.string.app_name;
    }*/

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

    private void showToast(final int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private void showToast(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
