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

    public AmbianceAnalysis(Context c, short[] audio, Module m) {
        context = c;
        audio_data = audio;
        module = m;
    }


    /**
     * Estimate the number of concurrent speakers for each 5-s segment
     *
     * @return the number of concurrent speakers
     */
    public String estimate() throws IOException {
        // input of wav2vec is float32
        int len =  audio_data.length;
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

}
