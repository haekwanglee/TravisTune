package hekasian.travistune;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import hekasian.travistune.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public interface MicListener {
        boolean onMicStart();
        void onMicStop();
    }

    private ActivityMainBinding binding;
    private TimerDisplay mTimerDisplay;
    private SipdroidRecorder mRecorder;
    private AudioController mAudioControl;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initializeAudio();
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        checkAndRequestPermissions();
    }

    private void setupUI() {
        binding.toggleButton.setChecked(false);
        binding.toggleButton.setOnCheckedChangeListener(recordBtnListener);
        mTimerDisplay = new TimerDisplay(binding.recordingTimer);
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED) {
            initializeAudio();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void initializeAudio() {
        mAudioControl = new AudioController(this);
        mAudioControl.configureRecorder();
    }

    private final CompoundButton.OnCheckedChangeListener recordBtnListener =
            (btn, isChecked) -> {
                if (btn.isChecked()) {
                    if (onMicStart()) {
                        mTimerDisplay.reset();
                        mTimerDisplay.start();
                    } else {
                        btn.setChecked(false);
                    }
                } else {
                    onMicStop();
                    mTimerDisplay.stop();
                }
            };

    public boolean onMicStart() {
        if (!mAudioControl.isValidRecorder()) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
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
        if (mRecorder != null) {
            mRecorder.stop();
            Toast.makeText(this, "mic stop", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecorder != null) {
            mRecorder.stop();
        }
        binding = null;
    }
}
