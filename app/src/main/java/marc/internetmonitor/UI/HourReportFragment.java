package marc.internetmonitor.UI;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import marc.internetmonitor.Background.InternetCheckService;
import marc.internetmonitor.Background.Logger;
import marc.internetmonitor.R;



public class HourReportFragment extends Fragment implements View.OnTouchListener{

    private static final String ARG_DATE_PARAM = "dateParam";
    public  static final SimpleDateFormat DATE_HR_FORMAT = new SimpleDateFormat("yyyy-MM-dd:HH");
    static final Integer RED    = 0xffd23030;
    static final Integer ORANGE = 0xffde8d00;
    static final Integer GREEN  = 0xff009307;


   // private String dateAsStr;
    private Date selectedHour;
    private OnFragmentInteractionListener mListener;
    private Handler handler;
    public  Boolean keepRefreshing = true;
    private ThreadLoadConnectionsStatus threadLoadConnectionsStatus;
    private GestureDetector gestureDetector;
    private View.OnTouchListener onTouchListener;
    private RecyclerView recyclerViewConnectionRecords;
    private RecyclerViewAdapterConnectionRecords recyclerViewAdapterConnectionRecords;



    public static HourReportFragment newInstance( Date date ) {

        HourReportFragment fragment = new HourReportFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE_PARAM,DATE_HR_FORMAT.format(date) );
        fragment.setArguments(args);
        return fragment;

    }

    public HourReportFragment() {
        // Required empty public constructor
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            try {
                selectedHour = DATE_HR_FORMAT.parse( getArguments().getString(ARG_DATE_PARAM) );
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        handler = new Handler();
        gestureDetector = new GestureDetector( getActivity() , new HourReportFragmentOnGestureListener());
        onTouchListener = this;



    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        MovingLinearLayout movingLinearLayout = (MovingLinearLayout) inflater.inflate(R.layout.fragment_hour_report, container, false);
        movingLinearLayout.setOnTouchListener(this);

        // CREATE RECYCLER VIEW AND ADD ADAPTER WITH EMPTY DATA
        recyclerViewConnectionRecords = (RecyclerView) movingLinearLayout.findViewById(R.id.recyclerViewConnectionRecords);
        recyclerViewConnectionRecords.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerViewConnectionRecords.setOnTouchListener(onTouchListener);


        return movingLinearLayout;

    }

    @Override
    public void onStart() {
        super.onStart();

    }


    @Override
    public void onResume() {
        super.onResume();
        threadLoadConnectionsStatus = new ThreadLoadConnectionsStatus();
        threadLoadConnectionsStatus.start();

    }

    @Override
    public void onDetach() {

        keepRefreshing = false;
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener {


        public void goToNextHour();

        public void goToPreviousHour();

    }


    class ThreadLoadConnectionsStatus extends Thread {

        @Override
        public void run() {
            super.run();

            try {

                final ProgressBar progressBarHoursRecords = (ProgressBar) getActivity().findViewById(R.id.progressBarLoadingConnectionRecords);
                final TextView textViewTimeFrame = (TextView) getActivity().findViewById(R.id.textViewTimeFrame);

                final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                final Date startDateTime = selectedHour;

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewTimeFrame.setText(new SimpleDateFormat("dd/MM/yyyy").format(startDateTime) + "  " + simpleDateFormat.format(startDateTime) + "-" + simpleDateFormat.format(new Date(startDateTime.getTime() + 3600 * 1000)));
                        recyclerViewConnectionRecords.setVisibility(View.GONE);
                        progressBarHoursRecords.setVisibility(View.VISIBLE);
                    }
                });

                // WAIT FOR ANIMATION TO COMPLETE
                long animTime = getResources().getInteger(R.integer.animation_time);
                sleep(animTime+50);

                // GET DATA
                Logger.LogRecord[] logRecords = Logger.getConnectionDataForOneHour(startDateTime);
                final ArrayList<Logger.LogRecord> logRecordArrayList = new ArrayList<Logger.LogRecord>(Arrays.asList(logRecords));

                // BUILD LIST FOR THE FIRST TIME
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        recyclerViewAdapterConnectionRecords = new RecyclerViewAdapterConnectionRecords(logRecordArrayList);
                        recyclerViewConnectionRecords.setAdapter(recyclerViewAdapterConnectionRecords);

                    }
                });

                // STOP SPINNER AND SHOW LIST
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recyclerViewConnectionRecords.setVisibility(View.VISIBLE);
                        progressBarHoursRecords.setVisibility(View.GONE);
                    }
                });


                sleep(1000);
                // IF WE ARE SHOWING CURRENT HOUR MOVE TO CURRENT TIME
                if(  (startDateTime.getTime()+(3600L*1000L)) <  System.currentTimeMillis()  ||  System.currentTimeMillis()<startDateTime.getTime()  ){
                    keepRefreshing = false;
                }
                else {
                    final int index = (int) ((System.currentTimeMillis() - startDateTime.getTime()) / InternetCheckService.INTERVAL);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerViewConnectionRecords.getLayoutManager();
                            linearLayoutManager.scrollToPositionWithOffset(index, 300);
                        }
                    });
                }


                // REFRESH CURRENT TIME DATA
                while( keepRefreshing ) {


                    if(  (startDateTime.getTime()+(3600L*1000L)) <  System.currentTimeMillis()  ||  System.currentTimeMillis()<startDateTime.getTime()  ){
                        keepRefreshing = false;
                    }
                    else {


                        Long from = System.currentTimeMillis() - 30 * 1000; // NOW -30sec
                        Long to = System.currentTimeMillis();
                        logRecords = Logger.getConnectionData(from, to);

                        final ArrayList<Logger.LogRecord> refreshedLogRecords = new ArrayList<Logger.LogRecord>( Arrays.asList(logRecords) );
                        for(Logger.LogRecord logRecord : refreshedLogRecords){
                            logRecord.refreshed = true;
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                recyclerViewAdapterConnectionRecords.refreshLogRegords( refreshedLogRecords , startDateTime );
                            }
                        });

                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




    @Override
    public boolean onTouch(View v, MotionEvent event) {

        return gestureDetector.onTouchEvent(event);
    }



    class HourReportFragmentOnGestureListener implements GestureDetector.OnGestureListener{

        @Override
        public boolean onDown(MotionEvent e) {

            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if( velocityX < -700  &&  ( Math.abs(velocityX) >  Math.abs(velocityY)*2 ) ){
                keepRefreshing = false;
                mListener.goToNextHour();
            }
            if(  ( velocityX > 700    &&  ( Math.abs(velocityX) > Math.abs(velocityY)*2  )  )   ){
                keepRefreshing = false;
                mListener.goToPreviousHour();
            }

            return false;
        }

    }




    private class RecyclerViewAdapterConnectionRecords extends RecyclerView.Adapter<RecyclerViewAdapterConnectionRecords.ViewHolderConnectionRecord>{


        List<Logger.LogRecord> logRecords;
        SimpleDateFormat simpleDateFormatTime = new SimpleDateFormat("HH:mm:ss");


        public RecyclerViewAdapterConnectionRecords(List<Logger.LogRecord> logRecords) {
            this.logRecords = logRecords;
        }


        public void refreshLogRegords(List<Logger.LogRecord> logRecords , Date startDateTime ){

            for( int l=0; l< logRecords.size(); l++ ) {

                int index = (int) ( (   logRecords.get(l).timeToShow - startDateTime.getTime() ) / InternetCheckService.INTERVAL);
                this.logRecords.remove(index);
                this.logRecords.add(index, logRecords.get(l) );

            }

            notifyDataSetChanged();

        }


        @Override
        public ViewHolderConnectionRecord onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = LayoutInflater.from( parent.getContext()).inflate(R.layout.connection_record , null );

            ViewHolderConnectionRecord viewHolderConnectionRecord = new ViewHolderConnectionRecord(view);

            return viewHolderConnectionRecord;
        }


        @Override
        public void onBindViewHolder(ViewHolderConnectionRecord holder, int position) {

            if ( logRecords.get(position).timeLogged != null ) {

                holder.textViewTime.setText(simpleDateFormatTime.format(new Date(logRecords.get(position).timeLogged)));
                if (logRecords.get(position).responseCode != null && logRecords.get(position).responseCode == 200) {

                    holder.checkBoxConnectionResponseCode.setChecked(true);
                    holder.textViewResponseTime.setText(logRecords.get(position).completionTime + " ms");
                    holder.textViewResponseTime.setTextColor(0xff000000);
                } else {
                    holder.textViewResponseTime.setText("FAILED");
                    holder.textViewResponseTime.setTextColor(RED);
                }

            }
            else{
                holder.textViewTime.setText(simpleDateFormatTime.format(new Date( logRecords.get(position).timeToShow ) ) );
                holder.checkBoxConnectionResponseCode.setChecked(false);
                holder.textViewResponseTime.setText("NO DATA");
                holder.textViewResponseTime.setTextColor(0xff000000);
            }

            if( logRecords.get(position).refreshed ){
                holder.itemView.setBackgroundColor(0x44666666);
            }
            else{
                holder.itemView.setBackgroundColor(0x00000000);
            }

        }



        @Override
        public int getItemCount() {
            return logRecords.size();
        }



        public class ViewHolderConnectionRecord extends RecyclerView.ViewHolder{

            TextView textViewTime;
            CheckBox checkBoxConnectionResponseCode;
            TextView textViewResponseTime;
            View itemView;

            public ViewHolderConnectionRecord(View itemView) {
                super(itemView);

                this.itemView = itemView;
                textViewTime = (TextView) itemView.findViewById(R.id.textViewTime);
                checkBoxConnectionResponseCode = (CheckBox) itemView.findViewById(R.id.checkBoxConnectionResponseCode);
                textViewResponseTime = (TextView) itemView.findViewById(R.id.textViewResponseTime);

            }
        }








    }








}
