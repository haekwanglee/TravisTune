/* AudioConfigurator.java
   An auto-tune app for Android

   Copyright (c) 2016 Ethan Chen

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc.,
   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package hekasian.travistune;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import hekasian.travistune.util.Constants;

public class AudioController {
    private static final String TAG = "AudioController";

    private Context mContext;
    private int mInputBufferSize;
    private int mInputSampleRate;
    private boolean mIsLive;

    public AudioController(Context context) {
        mContext = context;

        // TODO: set preference
        //SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        //loadPreferences(sharedPrefs);
        //sharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);

        //mInputBufferSize = 0;
        //mInputSampleRate = 44100;
        mIsLive = true;
    }

    public int getSampleRate() {
        return mInputSampleRate;
    }

    public boolean isLive() {
        return mIsLive;
    }

    public boolean isValidRecorder() {

        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                mInputSampleRate, Constants.DEFAULT_CHANNEL_CONFIG,
                Constants.DEFAULT_PCM_FORMAT, mInputBufferSize);
        boolean valid = recorder.getState() == AudioRecord.STATE_INITIALIZED;
        recorder.release();
        return valid;
    }

    public AudioRecord getRecorder()
            throws IllegalArgumentException {

        // todo: auth issue.
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                mInputSampleRate, Constants.DEFAULT_CHANNEL_CONFIG,
                Constants.DEFAULT_PCM_FORMAT, mInputBufferSize);
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalArgumentException("Unable to initialize AudioRecord, buffer: " +
                    mInputBufferSize);
        }

        return recorder;
    }

    public void configureRecorder() {
        int[] sampleRates = {
                Constants.SAMPLE_RATE_44KHZ,
                Constants.SAMPLE_RATE_22KHZ,
                Constants.SAMPLE_RATE_11KHZ,
                Constants.SAMPLE_RATE_8KHZ,
        };
        double[] multipliers = {
                1.0, 0.5, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0
        };

        // Assumption: The system isn't lying about this being the absolute minimum
        int minBufferSize = AudioRecord.getMinBufferSize(Constants.DEFAULT_SAMPLE_RATE,
                Constants.DEFAULT_CHANNEL_CONFIG, Constants.DEFAULT_PCM_FORMAT);

        for (int sampleRate : sampleRates) {
            for (double multiplier : multipliers) {
                AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        sampleRate, Constants.DEFAULT_CHANNEL_CONFIG, Constants.DEFAULT_PCM_FORMAT,
                        (int) (minBufferSize * multiplier));

                if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                    saveBufferSize((int) (minBufferSize * multiplier));
                    saveSampleRate(sampleRate);
                    recorder.release();
                    return;
                }
                recorder.release();
            }
        }

        // Could not find valid setting
        Toast.makeText(mContext,
                R.string.error,
                Toast.LENGTH_SHORT).show();
    }

    public AudioTrack getPlayer() {
        int bufferSize = AudioTrack.getMinBufferSize(mInputSampleRate,
                Constants.DEFAULT_CHANNEL_OUT_CONFIG, Constants.DEFAULT_PCM_FORMAT);
        AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, mInputSampleRate,
                Constants.DEFAULT_CHANNEL_OUT_CONFIG, Constants.DEFAULT_PCM_FORMAT,
                bufferSize, AudioTrack.MODE_STREAM);
        if (player.getState() != AudioTrack.STATE_INITIALIZED) {
            throw new IllegalArgumentException("Unable to initialize AudioRecord, buffer: " +
                    bufferSize);
        }

        return player;
    }

    private void saveSampleRate(int sampleRate) {
        mInputSampleRate = sampleRate;
    }

    private void saveBufferSize(int bufferSize) {
        mInputBufferSize = bufferSize;
    }

    private void loadPreferences(SharedPreferences sharedPrefs) {

    }


}