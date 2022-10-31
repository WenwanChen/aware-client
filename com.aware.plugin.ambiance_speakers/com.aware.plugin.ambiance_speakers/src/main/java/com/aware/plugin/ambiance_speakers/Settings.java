package com.aware.plugin.ambiance_speakers;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Plugin settings in XML @xml/preferences
    public static final String STATUS_PLUGIN_AMBIANCE_SPEAKERS = "status_plugin_ambiance_speakers";

    /**
     * How frequently do we sample the microphone (default = 1) in minutes
     */
    public static final String FREQUENCY_PLUGIN_AMBIANCE_SPEAKERS = "frequency_plugin_ambiance_speakers";

    /**
     * For how long we listen (default = 30) in seconds
     */
    public static final String PLUGIN_AMBIANCE_SPEAKERS_SAMPLE_SIZE = "plugin_ambiance_speakers_sample_size";

    /**
     * Do not include the raw sample in the data that is sent.
     */
    public static final String PLUGIN_AMBIANCE_SPEAKERS_NO_RAW = "plugin_ambiance_speakers_no_raw";


    //Plugin settings UI elements
    private static CheckBoxPreference active, raw;
    private static EditTextPreference frequency, listen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_ambiance_speakers);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        active = (CheckBoxPreference) findPreference(STATUS_PLUGIN_AMBIANCE_SPEAKERS);
        if( Aware.getSetting(this, STATUS_PLUGIN_AMBIANCE_SPEAKERS).length() == 0 ) {
            Aware.setSetting( this, STATUS_PLUGIN_AMBIANCE_SPEAKERS, true ); //by default, the setting is true on install
        }
        active.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_AMBIANCE_SPEAKERS).equals("true"));

        raw = (CheckBoxPreference) findPreference(PLUGIN_AMBIANCE_SPEAKERS_NO_RAW);
        if(Aware.getSetting(getApplicationContext(), PLUGIN_AMBIANCE_SPEAKERS_NO_RAW).isEmpty()) {
            Aware.setSetting(getApplicationContext(), PLUGIN_AMBIANCE_SPEAKERS_NO_RAW, true);
        }
        raw.setChecked(Aware.getSetting(getApplicationContext(), PLUGIN_AMBIANCE_SPEAKERS_NO_RAW).equals("true"));

        frequency = (EditTextPreference) findPreference(FREQUENCY_PLUGIN_AMBIANCE_SPEAKERS);
        if (Aware.getSetting(getApplicationContext(), FREQUENCY_PLUGIN_AMBIANCE_SPEAKERS).length() == 0) {
            Aware.setSetting(getApplicationContext(), FREQUENCY_PLUGIN_AMBIANCE_SPEAKERS, 1);
        }
        frequency.setSummary("Every " + Aware.getSetting(getApplicationContext(), FREQUENCY_PLUGIN_AMBIANCE_SPEAKERS) + " minutes");

        listen = (EditTextPreference) findPreference(PLUGIN_AMBIANCE_SPEAKERS_SAMPLE_SIZE);
        if (Aware.getSetting(getApplicationContext(), PLUGIN_AMBIANCE_SPEAKERS_SAMPLE_SIZE).length() == 0) {
            Aware.setSetting(getApplicationContext(), PLUGIN_AMBIANCE_SPEAKERS_SAMPLE_SIZE, 30);
        }
        listen.setSummary("Listen " + Aware.getSetting(getApplicationContext(), PLUGIN_AMBIANCE_SPEAKERS_SAMPLE_SIZE) + " second(s)");
        Log.d("AWARE","****************** ambiance speaker setting started! ************");

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = findPreference(key);
        if( setting.getKey().equals(FREQUENCY_PLUGIN_AMBIANCE_SPEAKERS) ) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "1"));
            frequency.setSummary("Every " + Aware.getSetting(getApplicationContext(), FREQUENCY_PLUGIN_AMBIANCE_SPEAKERS) + " minutes");
        }
        if (setting.getKey().equals(PLUGIN_AMBIANCE_SPEAKERS_SAMPLE_SIZE)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "30"));
            listen.setSummary("Listen " + Aware.getSetting(getApplicationContext(), PLUGIN_AMBIANCE_SPEAKERS_SAMPLE_SIZE) + " second(s)");
        }
        if (setting.getKey().equals(STATUS_PLUGIN_AMBIANCE_SPEAKERS)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getBoolean(key, false));
            active.setChecked(sharedPreferences.getBoolean(key, false));
        }
        if (setting.getKey().equals(PLUGIN_AMBIANCE_SPEAKERS_NO_RAW)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "true"));
            raw.setChecked(sharedPreferences.getBoolean(key, true));
        }

        if (Aware.getSetting(this, STATUS_PLUGIN_AMBIANCE_SPEAKERS).equals("true")) {
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.ambiance_speakers");
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.ambiance_speakers");
        }
    }
}
