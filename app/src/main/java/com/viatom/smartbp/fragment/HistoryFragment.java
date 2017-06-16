package com.viatom.smartbp.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.viatom.smartbp.R;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 */
public class HistoryFragment extends Fragment {

    private View rootView;

    public HistoryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_history, container, false);
        setUI();
        return rootView;
    }

    public void setUI() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());

        TextView textView = (TextView) rootView.findViewById(R.id.history_text);
        textView.setText(format.format(curDate));
    }

}
