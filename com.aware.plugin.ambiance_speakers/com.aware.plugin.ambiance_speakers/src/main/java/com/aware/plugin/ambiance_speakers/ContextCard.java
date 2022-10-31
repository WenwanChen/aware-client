package com.aware.plugin.ambiance_speakers;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.aware.plugin.ambiance_speakers.Provider.AmbianceSpeakers_Data;

import com.aware.utils.IContextCard;

public class ContextCard implements IContextCard {

    //Constructor used to instantiate this card
    public ContextCard() {
    }

    private TextView ambiance_level;
    private TextView category;

    @Override
    public View getContextCard(final Context context) {
        //Load card layout
        View card = LayoutInflater.from(context).inflate(R.layout.ambiance_speaker_layout, null);

        ambiance_level = card.findViewById(R.id.ambiance_level);
        category = card.findViewById(R.id.category);
        Log.d("Context Card", "******************** get context card ************");
        Plugin.setSensorObserver(new Plugin.AWARESensorObserver() {
            @Override
            public void onRecording(ContentValues data) {
                context.sendBroadcast(new Intent("AMBIANCE_SPEAKERS").putExtra("data", data));
            }
        });

        //Register the broadcast receiver that will update the UI from the background service (Plugin)
        IntentFilter filter = new IntentFilter("AMBIANCE_SPEAKERS");
        context.registerReceiver(audioUpdater, filter);

        //Return the card to AWARE/apps
        return card;
    }

    private AmbianceSpeakersUpdater audioUpdater = new AmbianceSpeakersUpdater();
    public class AmbianceSpeakersUpdater extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ContentValues data = intent.getParcelableExtra("data");
            Log.d("AWARE: Ambiance Speakers", "******************** Do you receive any data???************" + data.getAsDouble(AmbianceSpeakers_Data.AMBIANCE_LEVEL));
            if(data.getAsDouble(AmbianceSpeakers_Data.AMBIANCE_LEVEL) == null) {
                ambiance_level.setText("Social Ambiance: 0 concurrent spkr");
                category.setText("Level: None");
            }
            else {
                ambiance_level.setText("Social Ambiance: " + data.getAsDouble(AmbianceSpeakers_Data.AMBIANCE_LEVEL) + " concurrent spkr");
                int num = Integer.valueOf(AmbianceSpeakers_Data.AMBIANCE_LEVEL);
                if(num == 0) {
                    category.setText("Level: None");
                }
                else if(num < 2) {
                    category.setText("Level: Low");
                }
                else if(num < 5) {
                    category.setText("Level: Moderate");
                }
                else {
                    category.setText("Level: High");
                }
            }


        }
    }

}
