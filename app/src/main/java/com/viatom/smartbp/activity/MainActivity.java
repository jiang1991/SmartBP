package com.viatom.smartbp.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.viatom.smartbp.R;
import com.viatom.smartbp.scanner.ScanResultsAdapter;
import com.viatom.smartbp.utility.Constant;

import com.polidea.rxandroidble.scan.ScanSettings;
import com.viatom.smartbp.utility.LogUtils;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ = 25;
    private static final int ENABLE_BT_REQ = 0;
    protected static final int REQUEST_ENABLE_BT = 2;

    @BindView(R.id.action_cancel)
    Button scanButton;
    @BindView(R.id.device_list)
    RecyclerView recyclerView;

    private RxBleClient bleClient;
    private Subscription scanSubscription;
    private ScanResultsAdapter resultsAdapter;


    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        bleClient = RxBleClient.create(this);

        Constant.init(this);
        Constant.initDir();

        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        } else {
            configureResultList();
        }
    }

    @OnClick(R.id.action_cancel)
    public void onScanClick() {
        if (isScanning()) {
            scanSubscription.unsubscribe();
        } else {
            scanSubscription = bleClient.scanBleDevices(
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .build(),
                    new ScanFilter.Builder().build()
            )
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnUnsubscribe(this::clearSubscription)
                    .subscribe(resultsAdapter::addScanResult, this::onScanFailure);
        }

        updateButtonState();
    }

    private void configureResultList() {
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        resultsAdapter = new ScanResultsAdapter();
        recyclerView.setAdapter(resultsAdapter);
        resultsAdapter.setOnAdapterItemClickListener(view -> {
            final int position = recyclerView.getChildAdapterPosition(view);
            final ScanResult item = resultsAdapter.getItemAtPosition(position);
            onItemClick(item);
        });
    }

    private void onItemClick(ScanResult scanResults) {
        final String mac_address = scanResults.getBleDevice().getMacAddress();
        final Intent intent = new Intent(this, BaseActivity.class);
        intent.putExtra(BaseActivity.EXTRA_MAC_ADDRESS, mac_address);
        startActivity(intent);
    }

    private void onScanFailure(Throwable throwable) {
        if (throwable instanceof BleScanException) {
//            handleBleScanException((BleScanException) throwable);
            LogUtils.d("SCAN ERROR" + ((BleScanException) throwable).getReason());
        }
    }

    private void clearSubscription() {
        scanSubscription = null;
        resultsAdapter.clearScanResults();
        updateButtonState();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (isScanning()) {
            scanSubscription.unsubscribe();
        }
    }


    private boolean isScanning() {
        return scanSubscription != null;
    }




    private void updateButtonState() {
        LogUtils.d("SCAN: " + (isScanning() ? R.string.scanner_action_scan : R.string.scanner_action_cancel));
        scanButton.setText(isScanning() ? R.string.scanner_action_scan : R.string.scanner_action_cancel);
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

    private void showToast(final int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private void showToast(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
