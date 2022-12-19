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

import org.pytorch.Module;
import org.pytorch.LiteModuleLoader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.LiteModuleLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
public class AmbianceAnalyser extends IntentService {
    public static String result;
    public static final String uncertain = "uncertain";
    private Module module;

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

        if (module == null) {
            module = LiteModuleLoader.load(assetFilePath(getApplicationContext(), "wav2vec2.ptl"));
            Log.d("AWARE: Ambiance Speakers","******************** loaded model***************");
        }

        double now = System.currentTimeMillis();
        double elapsed = 0;
        int SEGMENT_LENGTH = 5 * 16000;
        int recordingOffset = 0;
        short[] recordingBuffer = new short[SEGMENT_LENGTH];
        long shortsRead = 0;
        StringBuilder sb = new StringBuilder();

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
                    System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, numberOfShort); }

                recordingOffset += numberOfShort;
            }

            AmbianceAnalysis analysis = new AmbianceAnalysis(this, recordingBuffer, module);
            try {
                result = analysis.estimate();
            } catch (IOException e) {
                e.printStackTrace();
            }


            recordingOffset = 16000;
            shortsRead = 16000;
            System.arraycopy(recordingBuffer, 4 * 16000, recordingBuffer, 0, recordingOffset);
            sb.append(result);

            ContentValues data = new ContentValues();
//            data.put(Provider.AmbianceSpeakers_Data.TIMESTAMP, System.currentTimeMillis());
//            data.put(Provider.AmbianceSpeakers_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            data.put(Provider.AmbianceSpeakers_Data.AMBIANCE_LEVEL, result);
            Log.d("AWARE::Ambiance Speakers", "Realtime: " + result);

            if (!Aware.getSetting(getApplicationContext(), Settings.PLUGIN_AMBIANCE_SPEAKERS_NO_RAW).equals("true")) {
                short[] audio_data = new short[buffer_size];
                ByteBuffer byteBuff = ByteBuffer.allocate(2 * buffer_size);

                for (Short a : audio_data) byteBuff.putShort(a);
                data.put(Provider.AmbianceSpeakers_Data.RAW, byteBuff.array());
            }

//            getContentResolver().insert(Provider.AmbianceSpeakers_Data.CONTENT_URI, data);

            if (Plugin.getSensorObserver() != null)
                Plugin.getSensorObserver().onRecording(data);
        }

        String str = sb.toString();
        ContentValues data = new ContentValues();
        data.put(Provider.AmbianceSpeakers_Data.TIMESTAMP, now);
        data.put(Provider.AmbianceSpeakers_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        data.put(Provider.AmbianceSpeakers_Data.AMBIANCE_LEVEL, str);
        getContentResolver().insert(Provider.AmbianceSpeakers_Data.CONTENT_URI, data);
        Log.d("AWARE::Ambiance Speakers", "Realtime: " + str);
        if (str.length() < 4) {
            if (Aware.DEBUG) Log.d("aware ambiance speakers: ", uncertain);
            sendBroadcast(new Intent(uncertain));
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


    private static String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e("AWARE::Ambiance speakers", "*********CANNOT FIND PATH**************" + assetName + ": " + e.getLocalizedMessage());

        }
        return null;
    }

}
