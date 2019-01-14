package hekasian.travistune;

import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    public interface MicListener {
        boolean onMicStart();

        void onMicStop();
    }

    private TimerDisplay mTimerDisplay;
    private SipdroidRecorder mRecorder;
    private AudioController mAudioControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ToggleButton mRecordButton = findViewById(R.id.toggleButton);
        mRecordButton.setChecked(false);
        mRecordButton.setOnCheckedChangeListener(recordBtnListener);

        TextView timerText = findViewById(R.id.recording_timer);

        mTimerDisplay = new TimerDisplay(timerText);

        mAudioControl = new AudioController(this);
        mAudioControl.configureRecorder();
    }

    private CompoundButton.OnCheckedChangeListener recordBtnListener =
            new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                    if (btn.isChecked()) {
                        onMicStart();
                        mTimerDisplay.reset();
                        mTimerDisplay.start();
                    } else {
                        onMicStop();
                        mTimerDisplay.stop();
                    }
                }
            };

    // TODO: need to make better below codes.
    public boolean onMicStart() {
        if (!mAudioControl.isValidRecorder()) {
            Toast.makeText(this,
                    R.string.error,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (mAudioControl.isLive()) {
            // TODO: check mic for live
            /*if (!HeadsetHelper.isHeadsetPluggedIn(mContext)) {
                showWarning(mContext,
                        R.string.no_headset_plugged_in_title,
                        R.string.no_headset_plugged_in_warning);
                return false;
            }*/
        }

        if (mRecorder == null) {
            mRecorder = new SipdroidRecorder(this, mAudioControl);
        }
        mRecorder.start();

        return true;
    }

    public void onMicStop() {
        // mRecorder will trigger onRecorderStopped when it is finished
        mRecorder.stop();

        Toast.makeText(this,
                "mic stop",
                Toast.LENGTH_SHORT).show();
    }

}
