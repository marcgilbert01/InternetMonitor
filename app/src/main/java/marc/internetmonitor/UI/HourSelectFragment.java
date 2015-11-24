package marc.internetmonitor.UI;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import marc.internetmonitor.Background.Logger;
import marc.internetmonitor.R;


public class HourSelectFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_SELECTED_DATE = "selectedDate";
    private static final SimpleDateFormat DATE_HR_FORMAT = new SimpleDateFormat("yyyy-MM-dd:HH");
    static final Integer RED    = 0xffd23030;
    static final Integer ORANGE = 0xffde8d00;
    static final Integer GREEN  = 0xff009307;


    private OnFragmentInteractionListener mListener;
    private Date selectedHour = new Date();
    private Handler handler;
    private ThreadLoadHourlyConnectionsTotals threadLoadHourlyConnectionsTotals;
    private MovingLinearLayout movingLinearLayout;
    public Boolean keepRefreshing = true;

    public static HourSelectFragment newInstance( Date selectedDate ) {

        HourSelectFragment fragment = new HourSelectFragment();
        Bundle args = new Bundle();
        args.putString( ARG_SELECTED_DATE , DATE_HR_FORMAT.format(selectedDate) );
        fragment.setArguments(args);

        return fragment;
    }

    public HourSelectFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        handler = new Handler();

        if (getArguments() != null) {
            try {
                String newSelectedDateHourStr =  getArguments().getString(ARG_SELECTED_DATE);
                Date newSelectedDateHour = DATE_HR_FORMAT.parse(newSelectedDateHourStr);
                selectedHour = newSelectedDateHour;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        movingLinearLayout = (MovingLinearLayout) inflater.inflate(R.layout.fragment_hour_select, container, false);

        // SHOWS HOURS TAB
        LinearLayout linearLayoutHrTabs = (LinearLayout) movingLinearLayout.findViewById(R.id.linearLayoutHrTabs);
        for(int h=0; h<=23 ; h++ ){
            View viewHrTab = inflater.inflate( R.layout.hr_tab, null );
            TextView textViewHour = (TextView)viewHrTab.findViewById(R.id.textViewHour);
            textViewHour.setText(h + ":00");
            final int finalH = h;
            viewHrTab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(selectedHour);
                    calendar.set(Calendar.HOUR_OF_DAY, finalH);
                    calendar.getTime();
                    mListener.onHourSelected(calendar.getTime());
                 }
            });

            linearLayoutHrTabs.addView(viewHrTab);
        }

        threadLoadHourlyConnectionsTotals = new ThreadLoadHourlyConnectionsTotals();
        threadLoadHourlyConnectionsTotals.start();

        displayHourTabAsSelected();

        return movingLinearLayout;

    }

    @Override
    public void onResume() {

        super.onResume();

        // DISPLAY TAB AS SELECTED
        new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    displayHourTabAsSelected();
                    sleep(500);
                    moveScrollViewToSelectedHour();
                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
            }
        }.start();
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
        keepRefreshing = false;

    }




    public interface OnFragmentInteractionListener {

        public void onHourSelected(Date date);
    }



    class ThreadLoadHourlyConnectionsTotals extends Thread{

        @Override
        public void run() {
            super.run();

            // DATA FOR EACH HOUR
            try {

                while( keepRefreshing ) {

                    final LinearLayout linearLayoutHrTabs = (LinearLayout) movingLinearLayout.findViewById(R.id.linearLayoutHrTabs);
                    int c = 0;
                    while (c < linearLayoutHrTabs.getChildCount() && keepRefreshing == true) {

                        // GET DATA
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(selectedHour);
                        calendar.set(Calendar.HOUR_OF_DAY, c);
                        Date date = calendar.getTime();
                        Logger.LogRecord[] logRecords = Logger.getConnectionDataForOneHour(date);
                        final Logger.ConnectionRecordsTotals connectionRecordsTotals = Logger.calculateConnectionTotals(logRecords);

                        // DISPLAY DATA
                        final int finalC = c;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                                View view = linearLayoutHrTabs.getChildAt(finalC);
                                TextView textViewHrSuccess = (TextView) view.findViewById(R.id.textViewHrSuccess);
                                textViewHrSuccess.setText(String.valueOf(connectionRecordsTotals.totalSuccessConnection));
                                if (connectionRecordsTotals.totalSuccessConnection > 0) {
                                    textViewHrSuccess.setTextColor(GREEN);
                                }
                                TextView textViewHrFailed = (TextView) view.findViewById(R.id.textViewHrFailed);
                                textViewHrFailed.setText(String.valueOf(connectionRecordsTotals.totalFailedConnection));
                                if (connectionRecordsTotals.totalFailedConnection > 0) {
                                    textViewHrFailed.setTextColor(RED);
                                }
                                TextView textViewHrMissing = (TextView) view.findViewById(R.id.textViewHrMissing);
                                textViewHrMissing.setText(String.valueOf(connectionRecordsTotals.totalMissingData));
                            }
                        });
                        c++;
                    }

                    sleep(5000);

                }

            }catch (Exception e){
                                  e.printStackTrace();
                                }
         }
    }





    private void displayHourTabAsSelected(){

//System.out.println("~############## HANFLER="+handler);

        handler.post(new Runnable() {
            @Override
            public void run() {
                LinearLayout linearLayoutHrTabs = (LinearLayout) movingLinearLayout.findViewById(R.id.linearLayoutHrTabs);
                for (int c = 0; c < linearLayoutHrTabs.getChildCount(); c++) {
                    View viewTab  = linearLayoutHrTabs.getChildAt(c);
                    viewTab.setBackgroundColor(0x00000000);
                    TextView textView = (TextView) viewTab.findViewById(R.id.textViewHour);
                    textView.setTextColor( 0xffffffff );
                }
                View viewTab = linearLayoutHrTabs.getChildAt(getHourSelected());
                viewTab.setBackgroundResource(R.color.internetmonitor_listbackground);
                ObjectAnimator animation = ObjectAnimator.ofFloat(viewTab, "rotationX", 0f, 360f);
                animation.setDuration(600);
                animation.start();
            }
        });

    }


    private int getHourSelected(){

        int hr = -1;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedHour);
        hr = calendar.get(Calendar.HOUR_OF_DAY);

        return hr;
    }


    public void changeSelectedHour(Date newSelectedHour){

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(newSelectedHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        newSelectedHour = calendar.getTime();

        calendar = Calendar.getInstance();
        calendar.setTime(selectedHour);
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date selectedDayMidnight = calendar.getTime();

        if(  newSelectedHour.getTime() > selectedDayMidnight.getTime()  &&  newSelectedHour.getTime() < selectedDayMidnight.getTime()+3600*1000*24 ) {
            selectedHour = newSelectedHour;
            displayHourTabAsSelected();
        }

    }


    private void moveScrollViewToSelectedHour() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedHour);
        final int hr = calendar.get(Calendar.HOUR_OF_DAY);
        handler.post(new Runnable() {
            @Override
            public void run() {
                ScrollView scrollViewHrTabs = (ScrollView) movingLinearLayout.findViewById(R.id.scrollViewHrTabs);
                LinearLayout linearLayoutHrTabs = (LinearLayout) scrollViewHrTabs.findViewById(R.id.linearLayoutHrTabs);
                View viewTab = linearLayoutHrTabs.getChildAt(hr);
                //scrollViewHrTabs.scrollTo(0, viewTab.getTop());
                scrollViewHrTabs.smoothScrollTo(0, viewTab.getTop());
            }
        });

    }



}
