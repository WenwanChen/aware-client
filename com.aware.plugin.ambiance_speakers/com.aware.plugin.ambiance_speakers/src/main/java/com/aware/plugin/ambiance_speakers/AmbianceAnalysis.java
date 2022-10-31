package com.aware.plugin.ambiance_speakers;

import android.content.Context;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

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

import com.aware.Aware;

import ca.uol.aig.fftpack.RealDoubleFFT;

public class AmbianceAnalysis {

    private Context context;
    private static short[] audio_data;
    private Module module;

    public AmbianceAnalysis(Context c, short[] audio) {
        context = c;
        audio_data = audio;
    }


    /**
     * Estimate the number of concurrent speakers for each 5-s segment
     *
     * @return the number of concurrent speakers
     */
    public String estimate() throws IOException {
        int len = audio_data.length;
        if (module == null) {
            module = LiteModuleLoader.load(assetFilePath(context, "wav2vec2.ptl"));
            Log.d("AWARE: Ambiance Speakers","******************** loaded model***************");
        }

        // input of wav2vec is float32
        double[] wav2vecinput = new double[len];

        // feed in float values between -1.0f and 1.0f by dividing the signed 16-bit inputs.
        for (int i = 0; i < len; ++i) {
            wav2vecinput[i] = audio_data[i] / (float)Short.MAX_VALUE;
        }

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(len);
        for (double val : wav2vecinput)
            inTensorBuffer.put((float)val);

        Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, len});
        final String result = module.forward(IValue.from(inTensor)).toStr();
        Log.d("AWARE: Ambiance Speakers","************************* Prediction results: " + result);

        return result;


    }


    private String assetFilePath(Context context, String assetName) {
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
//    private String assetFilePath(Context context, String assetName) {
////        Log.d("AWARE: Ambiance Speakers:","********* this is context.getFilesDir: " + context.getFilesDir().getAbsolutePath());
//        File file = new File(context.getFilesDir(), assetName);
////        Log.d("AWARE: Ambiance Speakers:","********* this is file: " + file.getAbsolutePath());
//        if (file.exists() && file.length() > 0) {
////            Log.e("AWARE: Ambiance Speakers", "**************** getAbsolutePath: " + file.getAbsolutePath());
//            return file.getAbsolutePath();
//        }
//
//        try (InputStream is = context.getResources().openRawResource(R.raw.wav2vec2)) {
//            try (OutputStream os = new FileOutputStream(file)) {
//                byte[] buffer = new byte[4 * 1024];
//                int read;
//                while ((read = is.read(buffer)) != -1) {
//                    os.write(buffer, 0, read);
//                }
//                os.flush();
//            }
//            return file.getAbsolutePath();
//        } catch (IOException e) {
//            Log.e("AWARE::Ambiance speakers", "*********CANNOT FIND PATH**************" + assetName + ": " + e.getLocalizedMessage());
//        }
//        return null;
//    }
}
