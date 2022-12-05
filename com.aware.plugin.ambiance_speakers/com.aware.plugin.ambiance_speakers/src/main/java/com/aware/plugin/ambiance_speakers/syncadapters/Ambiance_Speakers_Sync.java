package com.aware.plugin.ambiance_speakers.syncadapters;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;


import com.aware.plugin.ambiance_speakers.Provider;
import com.aware.syncadapters.AwareSyncAdapter;

/**
 * Created by denzilferreira on 01/09/2017.
 *
 * This class tells what data is synched to the server. The Uri[] needs to be in the same order as the database tables and tables fields (due to the index in the array).
 */
public class Ambiance_Speakers_Sync extends Service {
    private AwareSyncAdapter sSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new AwareSyncAdapter(getApplicationContext(), true, true);
                Log.d("synch Provider.DATABASE_TABLES[0]: " , Provider.DATABASE_TABLES[0]);
                Log.d("synch Provider.TABLES_FIELDS[0]: " , Provider.TABLES_FIELDS[0]);
                Log.d("synch Provider.AmbianceSpeakers_Data.CONTENT_URI: " , String.valueOf(Provider.AmbianceSpeakers_Data.CONTENT_URI));

                sSyncAdapter.init(
                        Provider.DATABASE_TABLES, Provider.TABLES_FIELDS,
                        new Uri[]{
                                Provider.AmbianceSpeakers_Data.CONTENT_URI
                        }
                );
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
