package com.viatom.smartbp.fragment;


import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.viatom.smartbp.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class MeasureStartFragment extends Fragment {

    private View rootView;
    private TextView mDeviceNameView;
    private TextView mBatteryLevelView;

    private static final int PERMISSION_REQ = 25;
    private static final int ENABLE_BT_REQ = 0;
    private static final String SIS_CONNECTION_STATUS = "connection_status";
    private static final String SIS_DEVICE_NAME = "device_name";
    protected static final int REQUEST_ENABLE_BT = 2;

    private boolean mDeviceConnected = false;

    private BluetoothDevice mSelectedDevice;

    private String mac_address;

    public MeasureStartFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_measure_start, container, false);
        iniUI();
        return rootView;
    }

    public void iniUI() {
//        mDeviceNameView = (TextView) rootView.findViewById(R.id.device_name);
        mBatteryLevelView = (TextView) rootView.findViewById(R.id.battery);

//        mDeviceNameView.setText(mac_address);

//        mDeviceNameView.setText(Constant.selectedDeviceName);
    }

    public void showMacAddress(String address) {
        mac_address = address;
    }

    public void refreshhUI() {
        //
    }

    public void setDefaultUI() {
        //
    }


    /*@Override
    public void onDeviceSelected(final BluetoothDevice device, final String name) {
        mSelectedDevice = device;
    }

    @Override
    public void onDialogCanceled() {
        // do nothing
    }*/

    private void showToast(final int messageResId) {
        Toast.makeText(getActivity(), messageResId, Toast.LENGTH_SHORT).show();
    }

    private void showToast(final String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

}
