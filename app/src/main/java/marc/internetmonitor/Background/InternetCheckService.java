package marc.internetmonitor.Background;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class InternetCheckService extends Service {


    static public final Long INTERVAL = 10000L;
    static String urkToCheck = "http://google.com";
    static public final String INTERNET_MONITOR_PREFS = "InternetMonitorPrefs";
    static public final String URL_TO_CHECK_PREF = "urlToCheck";

    static MonitorThread monitorThread;
    static Boolean stopMonitor = false;


    public InternetCheckService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // LOAD SHARED PREFERENCES
        SharedPreferences  sharedPreferences = getSharedPreferences( INTERNET_MONITOR_PREFS , MODE_PRIVATE );
        urkToCheck = sharedPreferences.getString( URL_TO_CHECK_PREF , urkToCheck );

        // START THREAD
        if( monitorThread == null || monitorThread.getState()== Thread.State.TERMINATED ){
            stopMonitor = false;
            monitorThread = new MonitorThread();
            monitorThread.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // STOP THREAD
        stopMonitor = true;
        monitorThread.interrupt();

        // SAVE SHARED PREFS
        SharedPreferences.Editor editor = getSharedPreferences(URL_TO_CHECK_PREF, MODE_PRIVATE).edit();
        editor.putString( URL_TO_CHECK_PREF , urkToCheck );
        editor.commit();
    }


    @Override
    public IBinder onBind(Intent intent) {

        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }




    class MonitorThread extends Thread{


        @Override
        public void run() {
            super.run();

            // KEEP CPU RUNNING EVEN WHEN PHONE IS ASLEEP
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK ,"InternetMonitorWakelockTag" );
            wakeLock.acquire();

            // CHECK ACCESS TO URL EVERY 10SEC
            long sleepFinishedTime = System.currentTimeMillis();

            while( stopMonitor==false ){

                // START CONNECTION CHECK
                long timeOut = INTERVAL - (System.currentTimeMillis()-sleepFinishedTime) - 1000;
                ConnectionCheckThread connectionCheckThread = new ConnectionCheckThread( urkToCheck ,  (int)timeOut );

                // SLEEP FOR N SECONDS
                try {
                    sleep( INTERVAL -  (System.currentTimeMillis()-sleepFinishedTime)   );
                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                 }

                sleepFinishedTime = System.currentTimeMillis();
                // WRITE CONNECTION RESULT
                if( connectionCheckThread!=null ) {

                    String toLog = System.currentTimeMillis() + "," + connectionCheckThread.getResponseCode() + "," + connectionCheckThread.getCompletionTime() + ",.\n";
                    Logger.logLocally(toLog);
                }

            }

            // RELEASE CPU LOCK
            wakeLock.release();

            stopSelf();
        }
    }







    class ConnectionCheckThread extends Thread{

        String  urlToCheck = null;
        Integer connectionTimeout = null;

        Integer responseCode   = null;
        Long    completionTime = null;

        public ConnectionCheckThread( String url , Integer timeOut ) {

            urlToCheck = url;
            connectionTimeout = timeOut;
            this.start();
        }

        @Override
        public void run() {
            super.run();

            HttpURLConnection httpURLConnection = null;
            InputStream inputStream = null;
            try {
                URL url = new URL(urlToCheck);
                long startTime = System.currentTimeMillis();
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout(connectionTimeout);
                httpURLConnection.setReadTimeout(connectionTimeout);
                responseCode = httpURLConnection.getResponseCode();
                inputStream = httpURLConnection.getInputStream();
                final byte data[] = new byte[1024];
                while ( inputStream.read(data, 0, 1024) != -1 ) {
                }
                completionTime = System.currentTimeMillis()-startTime;
            }catch (Exception e){
                                  e.printStackTrace();
                                }
            finally {

                try { inputStream.close(); } catch (Exception e) { e.printStackTrace(); }
                try { httpURLConnection.disconnect(); }catch (Exception e) { e.printStackTrace(); }
            }

        }

        // GET
        public Integer getResponseCode() {
            return responseCode;
        }

        public Long getCompletionTime() {
            return completionTime;
        }
    }




    static public Boolean isInternetCheckRunning(){

        Boolean running = false;

        if( monitorThread!=null && monitorThread.getState() != Thread.State.TERMINATED ){
            running = true;
        }

        return running;
    }

    public static String getUrkToCheck() {
        return urkToCheck;
    }

    public static void setUrkToCheck(String urkToCheck) {
        InternetCheckService.urkToCheck = urkToCheck;
    }

}












