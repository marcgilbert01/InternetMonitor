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
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //dateAsStr = getArguments().getString(ARG_DATE_PARAM);
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
    public void onStart() {
        super.onStart();
        threadLoadConnectionsStatus = new ThreadLoadConnectionsStatus();
        threadLoadConnectionsStatus.start();

    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        MovingLinearLayout movingLinearLayout = (MovingLinearLayout) inflater.inflate(R.layout.fragment_hour_report, container, false);
        movingLinearLayout.setOnTouchListener( this );

        //
        recyclerViewConnectionRecords = (RecyclerView) movingLinearLayout.findViewById(R.id.recyclerViewConnectionRecords);
        recyclerViewConnectionRecords.setLayoutManager( new LinearLayoutManager(getActivity()) );
        recyclerViewConnectionRecords.setOnTouchListener(onTouchListener);


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
                sleep(animTime);

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


                /*
                SimpleDateFormat simpleDateFormatTime = new SimpleDateFormat("HH:mm:ss");
                int l = 0;
                while (l < logRecords.length && keepRefreshing == true) {

                    //System.out.println( logRecord.timeLogged );
                    final TableRow tableRowConnectionRecord = (TableRow) inflater.inflate(R.layout.connection_record, null);
                    TextView textViewTime = (TextView) tableRowConnectionRecord.findViewById(R.id.textViewTime);
                    CheckBox checkBoxConnectionResponse = (CheckBox) tableRowConnectionRecord.findViewById(R.id.checkBoxConnectionResponseCode);
                    TextView textViewResponseTime = (TextView) tableRowConnectionRecord.findViewById(R.id.textViewResponseTime);
                    if ( logRecords[l].timeLogged != null ) {

                        textViewTime.setText(simpleDateFormatTime.format(new Date(logRecords[l].timeLogged)));
                        if ( logRecords[l].responseCode!=null && logRecords[l].responseCode == 200) {
                            checkBoxConnectionResponse.setChecked(true);
                            textViewResponseTime.setText( logRecords[l].completionTime+" ms" );
                        }
                        else{
                            textViewResponseTime.setText("FAILED");
                            textViewResponseTime.setTextColor(RED);
                        }

                    } else {
                        textViewTime.setText(simpleDateFormatTime.format(new Date(logRecords[l].timeToShow)));
                        textViewResponseTime.setText("NO DATA");
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            tableLayoutConnectionRecords.addView(tableRowConnectionRecord);
                        }
                    });
                    l++;
                }
                */


                // STOP SPINNER AND SHOW LIST
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recyclerViewConnectionRecords.setVisibility(View.VISIBLE);
                        progressBarHoursRecords.setVisibility(View.GONE);
                    }
                });

                sleep(2000);

                // IF WE ARE SHOWING CURRENT HOUR MOVE TO CURRENT TIME
                /*
                if(  (startDateTime.getTime()+(3600L*1000L)) <  System.currentTimeMillis()  ||  System.currentTimeMillis()<startDateTime.getTime()  ){
                    keepRefreshing = false;
                }
                else {
                    final int index = (int) ((System.currentTimeMillis() - startDateTime.getTime()) / InternetCheckService.INTERVAL);
                    int top = recyclerViewConnectionRecords.getChildAt(index).getTop();
                    recyclerViewConnectionRecords.scrollTo(0, top-200 );
                }
                */

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

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                recyclerViewAdapterConnectionRecords.refreshLogRegords( refreshedLogRecords , startDateTime );
                            }
                        });

/*
                        l = 0;
                        while( l<logRecords.length && keepRefreshing==true ){



                            final TableRow tableRowConnectionRecord = (TableRow) inflater.inflate(R.layout.connection_record, null);
                            TextView textViewTime = (TextView) tableRowConnectionRecord.findViewById(R.id.textViewTime);
                            CheckBox checkBoxConnectionResponse = (CheckBox) tableRowConnectionRecord.findViewById(R.id.checkBoxConnectionResponseCode);
                            TextView textViewResponseTime = (TextView) tableRowConnectionRecord.findViewById(R.id.textViewResponseTime);

                            if (logRecords[l].timeLogged != null) {

                                textViewTime.setText(simpleDateFormatTime.format(new Date(logRecords[l].timeLogged)));
                                if ( logRecords[l].responseCode!=null && logRecords[l].responseCode == 200) {
                                    checkBoxConnectionResponse.setChecked(true);
                                    textViewResponseTime.setText( logRecords[l].completionTime+" ms" );
                                }
                                else{
                                    textViewResponseTime.setText("FAILED");
                                    textViewResponseTime.setTextColor(RED);
                                }

                            } else {
                                textViewTime.setText(simpleDateFormatTime.format(new Date(logRecords[l].timeToShow)));
                                textViewResponseTime.setText("NO DATA");
                            }

                            final int index = (int) ( (logRecords[l].timeToShow - startDateTime.getTime() ) / InternetCheckService.INTERVAL);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    tableRowConnectionRecord.setBackgroundColor(0x44666666);
                                    tableLayoutConnectionRecords.removeViewAt(index);
                                    tableLayoutConnectionRecords.addView(tableRowConnectionRecord, index);
                                }
                            });
                            l++;
                        }
*/


                    }



                    // IF WE DISPLAYING HOURS IN THE PAST STOP REFRESHING
                    /*
                    if(  (startDateTime.getTime()+(3600L*1000L)) <  System.currentTimeMillis()  ||  System.currentTimeMillis()<startDateTime.getTime()  ){
                        keepRefreshing = false;
                    }
                    else {

                        Long from = System.currentTimeMillis() - 30 * 1000; // NOW -30sec
                        Long to = System.currentTimeMillis();
                        logRecords = Logger.getConnectionData(from, to);
                        l = 0;
                        while( l<logRecords.length && keepRefreshing==true ){

                            final TableRow tableRowConnectionRecord = (TableRow) inflater.inflate(R.layout.connection_record, null);
                            TextView textViewTime = (TextView) tableRowConnectionRecord.findViewById(R.id.textViewTime);
                            CheckBox checkBoxConnectionResponse = (CheckBox) tableRowConnectionRecord.findViewById(R.id.checkBoxConnectionResponseCode);
                            TextView textViewResponseTime = (TextView) tableRowConnectionRecord.findViewById(R.id.textViewResponseTime);

                            if (logRecords[l].timeLogged != null) {

                                textViewTime.setText(simpleDateFormatTime.format(new Date(logRecords[l].timeLogged)));
                                if ( logRecords[l].responseCode!=null && logRecords[l].responseCode == 200) {
                                    checkBoxConnectionResponse.setChecked(true);
                                    textViewResponseTime.setText( logRecords[l].completionTime+" ms" );
                                }
                                else{
                                    textViewResponseTime.setText("FAILED");
                                    textViewResponseTime.setTextColor(RED);
                                }

                            } else {
                                textViewTime.setText(simpleDateFormatTime.format(new Date(logRecords[l].timeToShow)));
                                textViewResponseTime.setText("NO DATA");
                            }

                            final int index = (int) ( (logRecords[l].timeToShow - startDateTime.getTime() ) / InternetCheckService.INTERVAL);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    tableRowConnectionRecord.setBackgroundColor(0x44666666);
                                    tableLayoutConnectionRecords.removeViewAt(index);
                                    tableLayoutConnectionRecords.addView(tableRowConnectionRecord, index);
                                }
                            });
                            l++;
                        }
                    }
                    sleep(5000);
                    */
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

            //System.out.println("########## FLING ########### velocityX="+velocityX+"  velocityY="+velocityY);
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

            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.connection_record,null);

            ViewHolderConnectionRecord viewHolderConnectionRecord = new ViewHolderConnectionRecord(view);

            return viewHolderConnectionRecord;
        }


        @Override
        public void onBindViewHolder(ViewHolderConnectionRecord holder, int position) {

            if (logRecords.get(position).timeLogged != null) {

                holder.textViewTime.setText(simpleDateFormatTime.format(new Date(logRecords.get(position).timeLogged)));
                if (logRecords.get(position).responseCode != null && logRecords.get(position).responseCode == 200) {

                    holder.checkBoxConnectionResponseCode.setChecked(true);
                    holder.textViewResponseTime.setText(logRecords.get(position).completionTime + " ms");
                } else {
                    holder.textViewResponseTime.setText("FAILED");
                    holder.textViewResponseTime.setTextColor(RED);
                }

            }
            else{
                holder.textViewTime.setText(simpleDateFormatTime.format(new Date( logRecords.get(position).timeToShow ) ) );
                holder.textViewResponseTime.setText("NO DATA");

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

            public ViewHolderConnectionRecord(View itemView) {
                super(itemView);

                textViewTime = (TextView) itemView.findViewById(R.id.textViewTime);
                checkBoxConnectionResponseCode = (CheckBox) itemView.findViewById(R.id.checkBoxConnectionResponseCode);
                textViewResponseTime = (TextView) itemView.findViewById(R.id.textViewResponseTime);

            }
        }

    }








}
