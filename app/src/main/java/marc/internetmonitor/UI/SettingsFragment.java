package marc.internetmonitor.UI;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.Fragment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;


import marc.internetmonitor.Background.InternetCheckService;
import marc.internetmonitor.R;


public class SettingsFragment extends Fragment {


    private OnFragmentInteractionListener mListener;
    private ThreadInternetCheckIsRunning threadInternetCheckIsRunning;
    private Handler handler;
    private ToggleButton toggleButton;
    private EditText editTextUrlToCheck;
    private Button buttonCloseSettings;

    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance() {

        SettingsFragment fragment = new SettingsFragment();
        return fragment;

    }

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        MovingLinearLayout movingLinearLayout = (MovingLinearLayout) inflater.inflate(R.layout.fragment_settings, container, false);

        // SET LISTENER ON START STOP SERVICE BUTTON
        toggleButton = (ToggleButton) movingLinearLayout.findViewById(R.id.toggleButtonInternetCheckService);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if( InternetCheckService.isInternetCheckRunning()==false ) {
                    // START SERVICE
                    Intent intent = new Intent(getActivity(), InternetCheckService.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getActivity().startService(intent);
                }
                else{
                    // STOP SERVICE
                    Intent intent = new Intent(getActivity(), InternetCheckService.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getActivity().stopService(intent);
                }
            }
        });


        // SET LISTENER ON URL TO CHECK TEXT FIELD
        editTextUrlToCheck = (EditText) movingLinearLayout.findViewById(R.id.editTextUrlToCheck);
        editTextUrlToCheck.setText( InternetCheckService.getUrkToCheck() );
        editTextUrlToCheck.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                InternetCheckService.setUrkToCheck( s.toString() );
            }
        });

        // SET CLOSE BUTTON LISTENER
        buttonCloseSettings = (Button) movingLinearLayout.findViewById(R.id.buttonCloseSettings);
        buttonCloseSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onCloseSettings();
            }
        });



        return movingLinearLayout;

    }



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener {

        public void onCloseSettings();
    }





    class ThreadInternetCheckIsRunning extends Thread{

        Boolean exit = false;
        ToggleButton toggleButton;

        public ThreadInternetCheckIsRunning(ToggleButton toggleButton) {
            this.toggleButton = toggleButton;
        }

        @Override
        public void run() {
            super.run();

            while( exit==false ){

                // SLEEP
                try {
                    sleep(500);
                } catch (InterruptedException e) {   e.printStackTrace();   }

                // CHECK AND DISPLAY
                if( toggleButton!=null ) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleButton.setChecked(InternetCheckService.isInternetCheckRunning());
                        }
                    });
                }
                else{
                    exit = true;
                }

            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        if( threadInternetCheckIsRunning==null || threadInternetCheckIsRunning.getState()== Thread.State.TERMINATED ){

            threadInternetCheckIsRunning = new ThreadInternetCheckIsRunning(toggleButton);
            threadInternetCheckIsRunning.start();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if( threadInternetCheckIsRunning!=null ){
            threadInternetCheckIsRunning.exit = true;
        }
    }


}
