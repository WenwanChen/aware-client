package com.aware.plugin.ambiance_speakers;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AmbianceAnalyser extends IntentService {
    public static String result;

    public AmbianceAnalyser() {
        super(Aware.TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        //Check if microphone is available right now
        if(!isMicrophoneAvailable(getApplicationContext())) {
            Log.d("AWARE::Ambiance Speakers", "************************ mic not available...");
            return;
        }

        //Get minimum size of the buffer for pre-determined audio setup and minutes
        int buffer_size = AudioRecord.getMinBufferSize(AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM), AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10;
//        Log.d("AWARE::Ambiance Speakers", "************************ Recorder buffer size...");
        //Initialize audio recorder. Use MediaRecorder.AudioSource.VOICE_RECOGNITION to disable Automated Voice Gain from microphone and use DSP if available
        AudioRecord recorder = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer_size);

        while (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            //no-op while waiting microphone to initialise
        }

        if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
            recorder.startRecording();
        }

        Log.d("AWARE::Ambiance speakers", "*************** Collecting audio sample...");

        double now = System.currentTimeMillis();
        double elapsed = 0;
        int SEGMENT_LENGTH = 5 * 16000;
        int recordingOffset = 0;
        short[] recordingBuffer = new short[SEGMENT_LENGTH];
        long shortsRead = 0;
        while (elapsed < Integer.parseInt(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_AMBIANCE_SPEAKERS_SAMPLE_SIZE)) * 1000) {
            elapsed = System.currentTimeMillis() - now;
//            shortsRead = 0;
            short[] audioBuffer = new short[buffer_size / 2];

            // every 5 seconds
//            Log.d("AWARE: Ambiance Speakers", "***************** segment_length: " + SEGMENT_LENGTH);
            while (shortsRead < SEGMENT_LENGTH) {
                // for every segment of 5 seconds of data, we perform transcription
                // each successive segment’s first second is exactly the preceding segment’s last chunk
                int numberOfShort = recorder.read(audioBuffer, 0, audioBuffer.length);
                shortsRead += numberOfShort;
                if (shortsRead > SEGMENT_LENGTH) {
                    System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, (int) (numberOfShort - (shortsRead - SEGMENT_LENGTH)));
                }
                else {
//                    Log.d("AWARE: Ambiance Speakers","***************** shortsRead: "+ shortsRead);
//                    Log.d("AWARE: Ambiance Speakers", "***************** recordingoffset: " + recordingOffset);
                    System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, numberOfShort); }

                recordingOffset += numberOfShort;
            }


            AmbianceAnalysis analysis = new AmbianceAnalysis(this, recordingBuffer);
            try {
                result = analysis.estimate();
            } catch (IOException e) {
                e.printStackTrace();
            }


            recordingOffset = 16000;
            shortsRead = 16000;
            System.arraycopy(recordingBuffer, 4 * 16000, recordingBuffer, 0, recordingOffset);

            ContentValues data = new ContentValues();
            data.put(Provider.AmbianceSpeakers_Data.TIMESTAMP, System.currentTimeMillis());
            data.put(Provider.AmbianceSpeakers_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            data.put(Provider.AmbianceSpeakers_Data.AMBIANCE_LEVEL, result);
            Log.d("AWARE::Ambiance Speakers", "Realtime: " + data.toString());

            if (!Aware.getSetting(getApplicationContext(), Settings.PLUGIN_AMBIANCE_SPEAKERS_NO_RAW).equals("true")) {
                short[] audio_data = new short[buffer_size];
                ByteBuffer byteBuff = ByteBuffer.allocate(2 * buffer_size);

                for (Short a : audio_data) byteBuff.putShort(a);
                data.put(Provider.AmbianceSpeakers_Data.RAW, byteBuff.array());
            }

            getContentResolver().insert(Provider.AmbianceSpeakers_Data.CONTENT_URI, data);

            if (Plugin.getSensorObserver() != null)
                Plugin.getSensorObserver().onRecording(data);
        }


        //Release microphone and stop recording
        recorder.stop();
        recorder.release();

        Log.d("AWARE::Ambiance Speakers", "Finished audio sample...");
    }

    /**
     * Check if the microphone is available or not
     * @param context
     * @return
     */
    public static boolean isMicrophoneAvailable(Context context) {
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(new File(context.getCacheDir(), "MediaUtil#micAvailTestFile").getAbsolutePath());
        boolean available = true;
        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception exception) {
            available = false;
        }
        recorder.release();
        return available;
    }
}
