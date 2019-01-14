/* Mic.java
   An auto-tune app for Android

   Copyright (c) 2010 Ethan Chen

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
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hekasian.travistune.wave.WaveWriter;
import hekasian.travistune.util.AudioHelper;
import hekasian.travistune.util.Constants;

public class SipdroidRecorder {

    private static final String TAG = "SipdroidRecorder";

    public interface RecorderStoppedListener {
        void onRecorderStopped();
    }

    private static final int RECORDER_MESSAGE_IO_ERROR = 8675308;
    private static final int RECORDER_MESSAGE_RECORD_ERROR = 8675310;
    private static final int RECORDER_MESSAGE_FINISHED = 8675307;

    private final Context mContext;
    private RecordThread mWriterThread;
    private AudioController mAudioControl;
    private AutotalentController mAutotalentControl;
    private List<RecorderStoppedListener> mListeners;

    public SipdroidRecorder(Context context,
                            AudioController audioControl) {
        mContext = context;
        mAudioControl = audioControl;
        mAutotalentControl = new AutotalentController(mContext);
        mListeners = new ArrayList<>();
    }

    public void registerRecorderStoppedListener(RecorderStoppedListener listener) {
        mListeners.add(listener);
    }

    public void start() {
        try {
            mWriterThread = new RecordThread();
            mWriterThread.start();
            Toast.makeText(mContext,
                    R.string.recording_started_toast,
                    Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(mContext,
                    R.string.error,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void stop() {
        if (isRunning()) {
            mWriterThread.close();
            try {
                mWriterThread.join();
            } catch (InterruptedException e) {
                // Do nothing
            }
            mWriterThread = null;
        }
    }

    public boolean isRunning() {
        return mWriterThread != null
                && mWriterThread.getState() != Thread.State.NEW && mWriterThread
                .getState() != Thread.State.TERMINATED;
    }

    private Handler recorderHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECORDER_MESSAGE_RECORD_ERROR:
                    Toast.makeText(mContext,
                            R.string.error,
                            Toast.LENGTH_SHORT).show();
                    break;
                case RECORDER_MESSAGE_IO_ERROR:
                    Toast.makeText(mContext,
                            R.string.error,
                            Toast.LENGTH_SHORT).show();
                    break;
                case RECORDER_MESSAGE_FINISHED:
                    for (RecorderStoppedListener l : mListeners) {
                        l.onRecorderStopped();
                    }
                    break;
            }
        }
    };

    private class RecordThread extends Thread {
        private final AudioRecord mAudioRecord;
        private AudioTrack mAudioTrack;
        private WaveWriter mWavWriter;
        private final int mFrameSize;
        private final int mFrameRate;
        private final long mFramePeriod;
        private final int mBufSize;
        private boolean mRunning;

        public RecordThread() {
            mFrameSize = 160;
            mFramePeriod = 1000 / (mAudioControl.getSampleRate() / mFrameSize);
            mFrameRate = (int) (mAudioControl.getSampleRate() / mFrameSize * 1.5);
            mBufSize = mFrameSize * (mFrameRate + 1);
            mRunning = false;
            mAudioRecord = mAudioControl.getRecorder();
            try {
                FileOutputStream out = mContext.openFileOutput(
                        mContext.getString(R.string.default_recording_name), Context.MODE_PRIVATE);
                mWavWriter = new WaveWriter(out, mAudioRecord.getSampleRate(),
                        AudioHelper.getChannelConfig(Constants.DEFAULT_CHANNEL_CONFIG),
                        AudioHelper.getPcmEncoding(Constants.DEFAULT_PCM_FORMAT));
                if (mAudioControl.isLive()) {
                    mAudioTrack = mAudioControl.getPlayer();

                    //AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;
                    //mAudioTrack.setPreferredDevice();

                    Log.v(TAG, "recording thread");

                }
            } catch (IOException e) {
                Log.e(TAG, "Unable to write WAV file", e);
            }
        }

        // weird little hack; eliminates the nasty click when AudioTrack
        // (dis)engages by playing
        // a few milliseconds of silence before starting AudioTrack
        // This quirky hack taken from PdCore of pd-for-android project
        private void avoidClickHack(Context context) {
            try {
                MediaPlayer mp = MediaPlayer.create(context, R.raw.silence);
                mp.start();
                Thread.sleep(10);
                mp.stop();
                mp.release();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }

        public synchronized void close() {
            mRunning = false;
        }

        public void initialize() throws IOException {
            mWavWriter.createWaveFile();
            if (mAudioControl.isLive()) {
                mAutotalentControl.initializeAutotalent(mAudioControl.getSampleRate());
                AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                am.setMode(AudioManager.MODE_NORMAL);

                AudioDeviceInfo[] deviceList = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                Log.i(TAG, "deviceList.length : " + deviceList.length);

                AudioDeviceInfo outPutAudioDevice = null;
                int outPutPriority = 0; //min
                for (int index = 0; index < deviceList.length; index++) {
                    Log.i(TAG, "device type : " + deviceList[index].getType());
                    // TODO: set preference device, need to check priority value.
                    if(deviceList[index].getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            deviceList[index].getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            deviceList[index].getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ) {

                        if(outPutPriority < deviceList[index].getType()) {
                            outPutPriority = deviceList[index].getType();
                            outPutAudioDevice = deviceList[index];
                        }
                    }
                }

                mAudioTrack.setPreferredDevice(outPutAudioDevice);
                Log.i(TAG, "setPreferredDevice : " + outPutAudioDevice.getType() );
                //am.setSpeakerphoneOn(true);

            }
        }

        public void cleanup() {
            // stop things
            if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                mAudioRecord.stop();
            }
            mAudioRecord.release();
            if (mAudioControl.isLive()) {
                mAudioTrack.stop();
                mAudioTrack.release();
                mAutotalentControl.closeAutotalent();
            }
            // close file
            try {
                mWavWriter.closeWaveFile();
            } catch (IOException e) {
                // no recovery possible here
                e.printStackTrace();
            }
        }

        public void run() {
            Message msg;
            int num;
            long now, nextFrameDelay, lastFrameTime = 0;
            short[] buf = new short[mBufSize];

            Log.i(TAG, "start run, mBufSize:" + mBufSize);

            try {
                initialize();
                mRunning = true;
                avoidClickHack(mContext);

                mAudioRecord.startRecording();
                if (mAudioControl.isLive()) {
                    mAudioTrack.play();

                }
                while (mRunning) {

                    // delay reading if it's not time for the next frame
                    now = System.currentTimeMillis();
                    nextFrameDelay = mFramePeriod - (now - lastFrameTime);
                    lastFrameTime = now;
                    if (nextFrameDelay > 0) {
                        try {
                            sleep(nextFrameDelay);
                        } catch (InterruptedException e) {
                            // Do nothing
                        }
                        lastFrameTime = lastFrameTime + nextFrameDelay;
                    }

                    num = mAudioRecord.read(buf, 0, mFrameSize);

                    if (mAudioControl.isLive()) {
                        processLiveAudio(buf, num);
                        mAudioTrack.write(buf, 0, num);
                    }

                    // todo: process live audio check
                    mWavWriter.write(buf, 0, num);

                }
                msg = recorderHandler.obtainMessage(RECORDER_MESSAGE_FINISHED);
            } catch (IllegalStateException e) {
                msg = recorderHandler.obtainMessage(RECORDER_MESSAGE_RECORD_ERROR);
            } catch (IOException e) {
                // file IO error, no recovery possible?
                e.printStackTrace();
                msg = recorderHandler.obtainMessage(RECORDER_MESSAGE_IO_ERROR);
            }
            cleanup();
            recorderHandler.sendMessage(msg);
        }

        private void processLiveAudio(short[] samples, int numSamples) throws IOException {
            mAutotalentControl.process(samples, numSamples);
        }
    }
}
