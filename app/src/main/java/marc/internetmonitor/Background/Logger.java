package marc.internetmonitor.Background;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;



public class Logger {

    static String INTERNET_MONITOR_PATH = Environment.getExternalStorageDirectory()+"/InternetMonitor";
    static public Boolean enableLocalLogs = true;
    static public final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");



	public static void logLocally(String info){


        if( Logger.enableLocalLogs ) {

            FileWriter fileWriter = null;
            BufferedWriter bufferedWriter = null;
            try {
                Date now = new Date();
                String fileName = new SimpleDateFormat("yyyy-MM-dd").format(now) + ".log";
                File file = new File(INTERNET_MONITOR_PATH + "/" + fileName);
                // CREATE FILE IF IT DOESN'T EXIST
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                fileWriter = new FileWriter(file.getAbsoluteFile(), true);
                bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.append( info );
                bufferedWriter.close();

            }catch( Exception e){
                e.printStackTrace();
            }finally {

                try{ bufferedWriter.close(); }catch(Exception e){ e.printStackTrace();}
                try{ fileWriter.close(); }catch(Exception e){ e.printStackTrace();}
            }

        }
	}



    public static String getLogFile(Date date) throws IOException {

        String logs = null;

        String fileName = DATE_FORMAT.format(date) + ".log";
        File file = new File(INTERNET_MONITOR_PATH + "/" + fileName);
        StringBuilder stringBuilder = new StringBuilder();

        if( file.exists() ){

             InputStream inputStream = new FileInputStream(file) ;
             byte[] bytes = new byte[1024];
             int length;
             while( ( length = inputStream.read(bytes) ) != -1  ){
                 stringBuilder.append( new String( bytes , 0 , length ) );
             }

             logs = stringBuilder.toString();

        }

        return logs;
    }





	
	public static void logObjectFields( Object obj ){
		
		Field[] fields = obj.getClass().getDeclaredFields();
		String info = obj.getClass().getSimpleName();
		for( int f=0; f<fields.length ; f++ )
		{
			try {
				String methodName = "";
				if( fields[f].getType().equals(boolean.class) )
					methodName = "is"+fields[f].getName().substring(0,1).toUpperCase()+fields[f].getName().substring(1);
				else
					methodName = "get"+fields[f].getName().substring(0,1).toUpperCase()+fields[f].getName().substring(1);
				Method getMethod = obj.getClass().getMethod(methodName,null);
				//info += fields[f].getName();//+"="+fields[f].get(obj);
				info += fields[f].getName()+"="+getMethod.invoke(obj, null)+"\n";
				
			} catch (Exception e){
								  e.printStackTrace();
							     }
		}
		logLocally( info );
	}
	
	
//////LOG PACKET OF DATA 
////////////////////////////////////////////////////////////////////////
	static public String logBytesPacket(byte[] bytesPacket, String packetName){

		String toLog = "";

		StringBuilder sbHex = new StringBuilder();
		StringBuilder sbDec = new StringBuilder();
		for (byte b : bytesPacket)
		{
			sbHex.append(String.format("%x ", b));
			sbDec.append(String.format("%d ", b));
		}
		toLog += packetName+" size  ="+bytesPacket.length+"\n";	
		toLog += packetName+" (Hex) ="+sbHex.toString()+"\n";
		toLog += packetName+" (Dec) ="+sbDec.toString()+"\n";


        logLocally(toLog);

		return toLog;
	}	






    static public class LogRecord {

        public Long timeToShow;
        public Long timeLogged;
        public Integer responseCode;
        public Long completionTime;
        public String comments;

    }

    static public class ConnectionRecordsTotals{

        public int totalSuccessConnection = 0;
        public int totalFailedConnection = 0;
        public int totalMissingData = 0;

    }


    static public ConnectionRecordsTotals calculateConnectionTotals( LogRecord[] logRecords ){

        ConnectionRecordsTotals connectionRecordsTotals = new ConnectionRecordsTotals();

        for( LogRecord logRecord : logRecords  ){

            if( logRecord.timeLogged==null ){
                connectionRecordsTotals.totalMissingData++;
            }
            else{
                if( logRecord.responseCode!=null && logRecord.responseCode==200 ){
                    connectionRecordsTotals.totalSuccessConnection++;
                }
                else{
                    connectionRecordsTotals.totalFailedConnection++;
                }
            }
        }

        return connectionRecordsTotals;
    }



    static public LogRecord[] getConnectionData( Long from , Long to  ) throws IOException {

        from  =  ( (long)( from / 1000 ) ) * 1000;
        to    =  ( (long)( to / 1000 ) ) * 1000;

        LogRecord[] logRecords = new LogRecord[ (int)( (to-from) /InternetCheckService.INTERVAL) ];
        for( int l=0 ; l<logRecords.length ; l++ ){
            logRecords[l] = new LogRecord();
            logRecords[l].timeToShow = from + ( l * InternetCheckService.INTERVAL) ;
        }

        // PARSE LOGS DATA
        String logData = getLogFile( new Date(from) );
        if( logData!=null ) {

            String[] lines = logData.split("\n");
            for (String line : lines) {
                String[] cells = line.split(",");
                Long timeLogged = Long.parseLong(cells[0]);
                if (timeLogged > from && timeLogged < to) {
                    int index = (int) ((timeLogged - from) / InternetCheckService.INTERVAL);
                    logRecords[index].timeLogged = timeLogged;
                    try {
                        logRecords[index].responseCode = Integer.parseInt(cells[1]);
                    } catch (Exception e) {
                    }
                    try {
                        logRecords[index].completionTime = Long.parseLong(cells[2]);
                    } catch (Exception e) {
                    }
                    logRecords[index].comments = cells[3];
                    //logRecords[index] = logRecord;
                }
            }

        }

        return logRecords;
    }



    static public LogRecord[] getConnectionDataForOneHour(Date date) throws ParseException, IOException {

        LogRecord logRecords[] = null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd:HH");

        String dateFromStr = simpleDateFormat.format(date);
        Long from = simpleDateFormat.parse(dateFromStr).getTime();
        Long to   = from+3600*1000;

        logRecords = getConnectionData(from, to);

        return logRecords;
    }




    static public LogRecord[] getConnectionDataForOneDay(Date date) throws IOException, ParseException {

            LogRecord[] logRecords = new LogRecord[ (int)(1000L*3600L*24L/InternetCheckService.INTERVAL) ];

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND,0);
            calendar.set(Calendar.MILLISECOND,0);

            Long from = calendar.getTime().getTime();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Long to   = calendar.getTime().getTime();

            logRecords = getConnectionData(from, to);

            return logRecords;

    }















}
