package com.aware.plugin.ambiance_speakers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.content.SyncRequest;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import android.Manifest;
import android.util.Log;

import com.aware.utils.Scheduler;
import org.json.JSONException;
import org.json.JSONObject;


public class Plugin extends Aware_Plugin {

    public static final String SCHEDULER_PLUGIN_AMBIANCE_SPEAKERS = "SCHEDULER_PLUGIN_AMBIANCE_SPEAKERS";

    @Override
    public void onCreate() {
        super.onCreate();

        //This allows plugin data to be synced on demand from broadcast Aware#ACTION_AWARE_SYNC_DATA
        AUTHORITY = "com.aware.plugin.ambiance_speakers.provider.ambiance_speakers";
        TAG = "AWARE::Ambiance Speakers";
        REQUIRED_PERMISSIONS.add(Manifest.permission.RECORD_AUDIO);

        /**
         * Plugins share their current status, i.e., context using this method.
         * This method is called automatically when triggering
         * {@link Aware#ACTION_AWARE_CURRENT_CONTEXT}
         **/
//        CONTEXT_PRODUCER = new ContextProducer() {
//            @Override
//            public void onContext() {
//                //Broadcast your context here
//            }
//        };

    }

    /**
     * Allow callback to other applications when data is stored in provider
     */
    private static AWARESensorObserver awareSensor;
    public static void setSensorObserver(AWARESensorObserver observer) {
        awareSensor = observer;
    }
    public static AWARESensorObserver getSensorObserver() {
        return awareSensor;
    }

    public interface AWARESensorObserver {
        void onRecording(ContentValues data);
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {
            Log.d(TAG,"***************** entered Plugin!!!!!! ");
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_AMBIANCE_SPEAKERS).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_AMBIANCE_SPEAKERS, true);
            } else {
                if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_AMBIANCE_SPEAKERS).equalsIgnoreCase("false")) {
                    Aware.stopPlugin(getApplicationContext(), getPackageName());
                    Log.d(TAG,"******************* stopped?????");
                    return START_STICKY;
                }
            }

            if (Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_AMBIANCE_SPEAKERS).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_AMBIANCE_SPEAKERS, 1);
            }
            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_AMBIANCE_SPEAKERS_SAMPLE_SIZE).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_AMBIANCE_SPEAKERS_SAMPLE_SIZE, 30);
            }
            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_AMBIANCE_SPEAKERS_NO_RAW).isEmpty()) {
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_AMBIANCE_SPEAKERS_NO_RAW, true); //disables raw audio recording by default
            }
            Log.d(TAG,"***************** check settings: " + Settings.PLUGIN_AMBIANCE_SPEAKERS_SAMPLE_SIZE);

            try {
                Scheduler.Schedule audioSampler = Scheduler.getSchedule(this, SCHEDULER_PLUGIN_AMBIANCE_SPEAKERS);
                Log.d(TAG,"***************** getscheduler is null then schedule: " + audioSampler);
                if (audioSampler == null || audioSampler.getInterval() != Long.parseLong(Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_AMBIANCE_SPEAKERS))) {
                    audioSampler = new Scheduler.Schedule(SCHEDULER_PLUGIN_AMBIANCE_SPEAKERS)
                            .setInterval(Long.parseLong(Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_AMBIANCE_SPEAKERS)))
                            .setActionType(Scheduler.ACTION_TYPE_SERVICE)
                            .setActionClass(getPackageName() + "/" + AmbianceAnalyser.class.getName());
                    Scheduler.saveSchedule(this, audioSampler);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

//            //Enable our plugin's sync-adapter to upload the data to the server if part of a study
//            if (Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE).length() >= 0 && !Aware.isSyncEnabled(this, Provider.getAuthority(this)) && Aware.isStudy(this) && getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone)) {
//                Log.d("sync check: ","satisfied if condition");
//                ContentResolver.setIsSyncable(Aware.getAWAREAccount(this), Provider.getAuthority(this), 1);
//                ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), true);
//                ContentResolver.addPeriodicSync(
//                        Aware.getAWAREAccount(this),
//                        Provider.getAuthority(this),
//                        Bundle.EMPTY,
//                        Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60
//                );
//            }


            if (Aware.isStudy(this)) {
                ContentResolver.setIsSyncable(Aware.getAWAREAccount(this), "com.aware.plugin.ambiance_speakers.provider.ambiance_speakers", 1);
                ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), "com.aware.plugin.ambiance_speakers.provider.ambiance_speakers", true);
                long frequency = Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60;
                SyncRequest request = new SyncRequest.Builder()
                        .syncPeriodic(frequency, frequency / 3)
                        .setSyncAdapter(Aware.getAWAREAccount(this), "com.aware.plugin.ambiance_speakers.provider.ambiance_speakers")
                        .setExtras(new Bundle()).build();
                ContentResolver.requestSync(request);
            }

            Log.d("sync check(freq webservice): ",Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE));
            Log.d("sync check(isSyncEnabled): ",String.valueOf(Aware.isSyncEnabled(this, "com.aware.plugin.ambiance_speakers.provider.ambiance_speakers")));
            Log.d("sync check(is study)", String.valueOf(Aware.isStudy(this)));
            Log.d("sync check(package)",String.valueOf(getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone)));


        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), "com.aware.plugin.ambiance_speakers.provider.ambiance_speakers", false);
        ContentResolver.removePeriodicSync(
                Aware.getAWAREAccount(this),
                "com.aware.plugin.ambiance_speakers.provider.ambiance_speakers",
                Bundle.EMPTY
        );

        Scheduler.removeSchedule(this, SCHEDULER_PLUGIN_AMBIANCE_SPEAKERS);
        Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_AMBIANCE_SPEAKERS, false);
    }
}
