package marc.internetmonitor.UI;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import marc.internetmonitor.Background.InternetCheckService;
import marc.internetmonitor.Background.Logger;
import marc.internetmonitor.R;

public class MainActivity extends Activity implements  HourReportFragment.OnFragmentInteractionListener,
                                                       DatePickerFragment.OnFragmentInteractionListener,
                                                       HourSelectFragment.OnFragmentInteractionListener,
                                                       SettingsFragment.OnFragmentInteractionListener   {


    Date selectedDate;
    Handler handler;
    static final Integer RED    = 0xffd23030;
    static final Integer ORANGE = 0xffde8d00;
    static final Integer GREEN  = 0xff009307;

    ThreadLoadConnectionsTotals threadLoadConnectionsTotals;
    HourReportFragment currentHourReportFragment;
    HourSelectFragment hourSelectFragment;
    DatePickerFragment datePickerFragment;
    SettingsFragment   settingsFragment;


    static public Date getMidgnightTime(Date date){

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();

        showHourReport(new Date());

        threadLoadConnectionsTotals = new ThreadLoadConnectionsTotals();
        threadLoadConnectionsTotals.start();

        // SET LISTENER ON SELECTED DATE
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayoutSelectDate);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if( threadLoadConnectionsTotals!=null ){
                    threadLoadConnectionsTotals.exit = true;
                }

                Action actionOpenDatePicker = new Action(){
                    @Override
                    public void execute() {

                        super.execute();

                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.setCustomAnimations(R.animator.topfragment_in_from_top, 0 );
                        datePickerFragment = DatePickerFragment.newInstance(selectedDate);
                        fragmentTransaction.replace(R.id.frameLayoutTop, datePickerFragment);
                        fragmentTransaction.commit();

                    }
                };

                clearFragments(actionOpenDatePicker);

            }
        });


        // SET LISTENER ON SETTINGS
        View viewSettingsIcon = findViewById(R.id.imageViewSettingsIcon);
        viewSettingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if( threadLoadConnectionsTotals!=null ){
                    threadLoadConnectionsTotals.exit = true;
                }

                Action action = new Action(){
                    @Override
                    public void execute() {

                        super.execute();
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.setCustomAnimations(R.animator.topfragment_in_from_top, 0 );
                        settingsFragment = SettingsFragment.newInstance();
                        fragmentTransaction.replace( R.id.frameLayoutTop, settingsFragment );
                        fragmentTransaction.commit();

                    }
                };

                clearFragments(action);

            }
        });



    }


    class Action {

        public void execute(){

        }

    }

    @Override
    protected void onResume() {
        super.onResume();


    }




    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showHourReport(final Date dateToShow){

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if( selectedDate!=null && dateToShow.getTime() > selectedDate.getTime() ) {
            fragmentTransaction.setCustomAnimations(R.animator.connection_result_in_from_right,
                                                    R.animator.connection_result_out_to_left);
                                                 //   R.animator.connection_result_in_from_left,
                                                 //   R.animator.connection_result_out_to_right);
        }
        else{
            fragmentTransaction.setCustomAnimations(R.animator.connection_result_in_from_left,
                                                    R.animator.connection_result_out_to_right);
                                                 //   R.animator.connection_result_in_from_right,
                                                 //   R.animator.connection_result_out_to_left);
        }
        currentHourReportFragment = HourReportFragment.newInstance( dateToShow );
        fragmentTransaction.replace(R.id.frameLayoutHourReportContainer, currentHourReportFragment);
        fragmentTransaction.addToBackStack("");
        fragmentTransaction.commit();

        // IF DATE TO SHOW IS A DIFFERENT DAY CHANGE SELECT HOURS OTHERWISE JUST UPDATE
        if(   selectedDate != null &&
              hourSelectFragment!=null &&
              hourSelectFragment.isVisible() &&
              dateToShow.getTime() >  getMidgnightTime(selectedDate).getTime() &&
              dateToShow.getTime() <  getMidgnightTime(selectedDate).getTime()+3600*1000*24
              ){

              hourSelectFragment.changeSelectedHour(dateToShow);
        }
        else{
            showHoursTab(dateToShow);
        }

        selectedDate = dateToShow;

    }




    private void showHoursTab(Date dateToShow){

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        hourSelectFragment = HourSelectFragment.newInstance(dateToShow);
        fragmentTransaction.replace( R.id.frameLayoutHourSelectContainer , hourSelectFragment );
        fragmentTransaction.commit();

    }



    @Override
    public void onHourSelected(Date date) {

        showHourReport(date);
    }

    @Override
    public void onDateSelected(final Date date) {

        clearFragments(new Action() {
            @Override
            public void execute() {
                super.execute();
                // STOP THREAD
                if (threadLoadConnectionsTotals != null && threadLoadConnectionsTotals.getState() != Thread.State.TERMINATED) {
                    threadLoadConnectionsTotals.exit = true;
                }
                // SET NEW SELECTED DATE
                if (date != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    calendar.set(Calendar.HOUR_OF_DAY, 12);
                    calendar.getTime();
                    showHourReport(calendar.getTime());
                } else {
                    showHourReport(new Date());
                }
                // START THREAD
                threadLoadConnectionsTotals = new ThreadLoadConnectionsTotals();
                threadLoadConnectionsTotals.start();
            }
        });


    }


    public void buttonClose(View view){

        finish();
    }

    @Override
    public void goToNextHour() {

        Date nextHour = new Date( selectedDate.getTime()+3600*1000 );
        showHourReport(nextHour);
    }

    @Override
    public void goToPreviousHour() {

        Date previousHour = new Date( selectedDate.getTime()-3600*1000 );
        showHourReport(previousHour);
    }

    @Override
    public void onCloseSettings() {

        clearFragments(new Action() {
            @Override
            public void execute() {
                super.execute();
                // STOP THREAD
                if (threadLoadConnectionsTotals != null && threadLoadConnectionsTotals.getState() != Thread.State.TERMINATED) {
                    threadLoadConnectionsTotals.exit = true;
                }
                // SET PREVIOUS SELECTED DATE OR CURRENT DATE
                if (selectedDate == null) {
                    selectedDate = new Date();
                }
                showHourReport(selectedDate);
                // START THREAD
                threadLoadConnectionsTotals = new ThreadLoadConnectionsTotals();
                threadLoadConnectionsTotals.start();
            }
        });

    }


    class ThreadLoadConnectionsTotals extends Thread{

        public Boolean exit = false;

        @Override
        public void run() {
            super.run();

            final LinearLayout linearLayoutHrTabs = (LinearLayout) findViewById(R.id.linearLayoutHrTabs);

            while( exit==false ) {

                try {

                    // DISPLAY SELECTED DATE
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            TextView textViewSelectedDate = (TextView) findViewById(R.id.textViewSelectedDate);
                            textViewSelectedDate.setText(new SimpleDateFormat("dd/MM/yyyy").format(selectedDate));
                        }
                    });

                    // DATA FOR THE ALL DAY
                    Logger.LogRecord[] logRecordsDaily = Logger.getConnectionDataForOneDay(selectedDate);
                    final Logger.ConnectionRecordsTotals connectionRecordsTotalsDaily = Logger.calculateConnectionTotals(logRecordsDaily);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            TextView textViewDailySuccess = (TextView) findViewById(R.id.textViewDailySuccess);
                            textViewDailySuccess.setText(String.valueOf(connectionRecordsTotalsDaily.totalSuccessConnection));
                            if (connectionRecordsTotalsDaily.totalSuccessConnection > 0) {
                                textViewDailySuccess.setTextColor(GREEN);
                            }
                            TextView textViewDailyFailed = (TextView) findViewById(R.id.textViewDailyFailed);
                            textViewDailyFailed.setText(String.valueOf(connectionRecordsTotalsDaily.totalFailedConnection));
                            if (connectionRecordsTotalsDaily.totalFailedConnection > 0) {
                                textViewDailyFailed.setTextColor(RED);
                            }
                            TextView textViewDailyMissing = (TextView) findViewById(R.id.textViewDailyMissing);
                            textViewDailyMissing.setText(String.valueOf(connectionRecordsTotalsDaily.totalMissingData));
                        }
                    });

                    sleep(5000);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
    }







    private void clearFragments(final Action action){


            // MOVE HOUR SELECT TO THE RIGHT AND REMOVE IT
            if( hourSelectFragment!=null && hourSelectFragment.isVisible() ) {
                //FrameLayout frameLayoutHourSelectContainer = (FrameLayout) findViewById(R.id.frameLayoutHourSelectContainer);
                View hourSelectView =  hourSelectFragment.getView();
                ObjectAnimator objectAnimatorHourSelect = ObjectAnimator.ofFloat(hourSelectView, "X", hourSelectView.getX(), hourSelectView.getX() + hourSelectView.getWidth());
                objectAnimatorHourSelect.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.remove(hourSelectFragment);
                        fragmentTransaction.commit();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                objectAnimatorHourSelect.setDuration(300);
                objectAnimatorHourSelect.start();
            }

            // MOVE HOUR REPORT TO THE LEFT AND REPLACE IT WITH DATEPICKER
            //MovingLinearLayout movingLinearLayoutHourReport = (MovingLinearLayout) currentHourReportFragment.getView();
            if( currentHourReportFragment!=null && hourSelectFragment.isVisible() ) {
                //FrameLayout frameLayoutHourReportContainer = (FrameLayout) findViewById(R.id.frameLayoutHourReportContainer);
                View hourReportView = currentHourReportFragment.getView();
                ObjectAnimator objectAnimatorHourReport = ObjectAnimator.ofFloat(hourReportView, "X", hourReportView.getX(), -hourReportView.getWidth());
                objectAnimatorHourReport.setDuration(300);
                objectAnimatorHourReport.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // REMOVE HOUR REPORT
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransaction1 = fragmentManager.beginTransaction();
                        fragmentTransaction1.remove(currentHourReportFragment);
                        fragmentTransaction1.commit();
                        action.execute();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                objectAnimatorHourReport.start();
            }

            // MOVE DATEPICKER TO THE TOP AND REMOVE IT
            if( datePickerFragment!=null && datePickerFragment.isVisible() ) {
                //final FrameLayout frameLayoutDatePicker = (FrameLayout) findViewById(R.id.frameLayoutDatePicker);
                View datePickerView = datePickerFragment.getView();
                ObjectAnimator objectAnimatorDatePicker = ObjectAnimator.ofFloat( datePickerView , "Y", datePickerView.getY() ,  -datePickerView.getHeight()  );
                objectAnimatorDatePicker.setDuration(300);
                objectAnimatorDatePicker.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                        // REMOVE HOUR REPORT
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransaction1 = fragmentManager.beginTransaction();
                        fragmentTransaction1.remove(datePickerFragment);
                        fragmentTransaction1.commit();
                        // EXECUTE NEXT ACTION
                        action.execute();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                objectAnimatorDatePicker.start();
            }


            // MOVE SETTINGS TO THE TOP AND REMOVE IT
            if( settingsFragment!=null && settingsFragment.isVisible() ) {
                //final FrameLayout frameLayoutDatePicker = (FrameLayout) findViewById(R.id.frameLayoutDatePicker);
                View settingsView = settingsFragment.getView();
                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(settingsView, "Y", settingsView.getY(), -settingsView.getHeight());
                objectAnimator.setDuration(300);
                objectAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // REMOVE HOUR REPORT
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.remove(settingsFragment);
                        fragmentTransaction.commit();
                        // EXECUTE NEXT ACTION
                        action.execute();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                objectAnimator.start();
            }


    }


}
