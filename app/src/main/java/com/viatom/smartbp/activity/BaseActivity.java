package com.viatom.smartbp.activity;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;
import com.viatom.smartbp.R;
import com.viatom.smartbp.fragment.HistoryFragment;
import com.viatom.smartbp.fragment.MeasureStartFragment;
import com.viatom.smartbp.fragment.SettingFragment;
import com.viatom.smartbp.items.Device;
import com.viatom.smartbp.utility.BtCmdUtils;
import com.viatom.smartbp.utility.Constant;
import com.viatom.smartbp.utility.LogUtils;
import com.viatom.smartbp.utility.StringUtils;

import java.util.UUID;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;


public class BaseActivity extends AppCompatActivity {

    public static final String EXTRA_MAC_ADDRESS = "extra_mac_address";
    public static final String CMD_DEVICE_INFO = "a50000e100000000";

    private UUID read_uuid, write_uuid;

    public BottomNavigationView bnve;
    public Toolbar toolbar;
    public int[] titles = new int[]{R.string.nav_measure, R.string.nav_history, R.string.nav_setting};
    public RxBleClient bleClient;
    private RxBleDevice bleDevice;
    private Subscription subscription;
    private Observable<RxBleConnection> connection;

    public RequestQueue queue;

    private String mac_address;
    private String device_info;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        context = this;
        queue = Volley.newRequestQueue(context);
        bleClient = RxBleClient.create(context);

        read_uuid = UUID.fromString(Constant.READ_CHARACTERISTIC);
        write_uuid = UUID.fromString(Constant.WRTITE_CHARACTERISTIC);

        initView();
        initFragment();
        initEvent();

        mac_address = getIntent().getStringExtra(EXTRA_MAC_ADDRESS);
        connectDevice(mac_address);
    }

    public void connectDevice(String mac_address) {
//        String mac_address = device.getAddress();
        bleDevice = bleClient.getBleDevice(mac_address);

        bleDevice.observeConnectionStateChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnectionStateChange);


        // listener
        connection = bleDevice
                .establishConnection(false)
                .compose(new ConnectionSharingAdapter())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(this::updateUI);

        connection.subscribe(
                this::onConnectionReceived,
                this::onConnectionFailure
        );

    }

    private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }


    /* change BNVE style */
    private void initView() {
        toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        bnve = (BottomNavigationView) findViewById(R.id.bnve);
    }

    /* create fragments */
    private void initFragment() {
        iniMeasureFragment();
        toolbar.setTitle(titles[0]);
    }

    /* set listeners */
    private void initEvent() {
        // select listener

        bnve.setOnNavigationItemSelectedListener((@NonNull MenuItem item) -> {
            switch (item.getItemId()) {
                case R.id.nav_measure:
                    iniMeasureFragment();
                    toolbar.setTitle(titles[0]);
                    return true;
                case R.id.nav_history:
                    toolbar.setTitle(titles[1]);
                    iniHistoryFragment();
                    return true;
                case R.id.nav_setting:
                    toolbar.setTitle(titles[2]);
                    iniSettingFragment();
                    return true;
            }
            return false;
        });
    }


    public void updateUI() {
        //
    }

    /* measure main fragment */
    public void iniMeasureFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        MeasureStartFragment fragment = new MeasureStartFragment();
        /* show MAC address */
        fragment.showMacAddress(mac_address);
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);

        ft.commit();
    }

    /* history fragment */
    public void iniHistoryFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        HistoryFragment fragment = new HistoryFragment();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);

        ft.commit();
    }

    /* setting fragment */
    public void iniSettingFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        SettingFragment fragment = new SettingFragment();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);

        ft.commit();
    }

    /*
    * get device info
    * update the UI
    * onDeviceConnected
    * */
    public void getDeviceInfo() {
        // read & write
        /*connection
                .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(read_uuid)
                        .doOnNext(bytes -> {
                            LogUtils.d(bytes.toString());
                        })
                        .flatMap(bytes -> rxBleConnection.writeCharacteristic(write_uuid, StringUtils.hexToBytes(CMD_DEVICE_INFO)))
                )
                .subscribe(
                        writeBytes -> {
                            // Written data.
                        },
                        this::onWriteFailure
                );*/


        /*get device info */
        connection
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(read_uuid))
                .doOnNext(notificationObservable -> {
                    // Notification has been set up
                })
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::onNotificationReceived,
                        this::onNotificationSetupFailure
                );


        connection
                .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(write_uuid, BtCmdUtils.getDeviceInfo()))
                .subscribe(
                        bytes -> onWriteSuccess(),
                        this::onWriteFailure
                );




        /* get update info */
        /*JsonObjectRequest request = new JsonObjectRequest(Constant.DEVICE_UPDATE_EN_URL, null, (JSONObject response) -> {
            String updateDfv = response.optString("version");
            LogUtils.d("DFV: " + updateDfv);
        }, (VolleyError error) -> LogUtils.d(error.toString()));

        queue.add(request);*/
    }

    public void onConnectionStateChange(RxBleConnection.RxBleConnectionState state) {
        LogUtils.d(state.toString());
        updateUI();
    }

    public void onConnectionReceived(RxBleConnection connection) {
        LogUtils.d("connected");
        getDeviceInfo();
    }

    public void onNotificationReceived(byte[] bytes) {
        LogUtils.d("Notification Received: " + StringUtils.bytesToHex(bytes));
        Device device = new Device(new Device.DeviceInfo(bytes));
        LogUtils.d("sn: " + device.getDeviceInfo().getSn());
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        //noinspection ConstantConditions
        LogUtils.d("Notifications error: " + throwable);
        Snackbar.make(findViewById(R.id.toolBar), "Notifications error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onWriteSuccess() {
        //noinspection ConstantConditions
        LogUtils.d("Write success");
        Snackbar.make(findViewById(R.id.toolBar), "Write success", Snackbar.LENGTH_SHORT).show();
    }

    private void onWriteFailure(Throwable throwable) {
        //noinspection ConstantConditions
        LogUtils.d("Write error: " + throwable);
        Snackbar.make(findViewById(R.id.toolBar), "Write error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        LogUtils.d("Connection error: " + throwable);
        Snackbar.make(findViewById(R.id.toolBar), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

}
