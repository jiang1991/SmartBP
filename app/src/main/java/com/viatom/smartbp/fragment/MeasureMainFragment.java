package com.viatom.smartbp.fragment;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.viatom.smartbp.R;
import com.viatom.smartbp.scanner.ScannerFragment;
import com.viatom.smartbp.utility.Constant;

import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 */
public class MeasureMainFragment extends Fragment implements PermissionRationaleFragment.PermissionDialogListener {

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

    public MeasureMainFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_measure_main, container, false);
        iniUI();
        return rootView;
    }

    public void iniUI() {
        mDeviceNameView = (TextView) rootView.findViewById(R.id.device_name);
        mBatteryLevelView = (TextView) rootView.findViewById(R.id.battery);

        mDeviceNameView.setText(Constant.selectedDeviceName);
    }

    public void refreshhUI() {
        //
    }

    public void setDefaultUI() {
        //
    }


    @Override
    public void onRequestPermission(final String permission) {
        ActivityCompat.requestPermissions(getActivity(), new String[] {permission}, PERMISSION_REQ);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQ: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // do nothing
                } else {
                    Toast.makeText(getActivity(), R.string.no_required_permission, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
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
