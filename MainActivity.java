package com.theta360.pluginapplication;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.AudioSource;
import android.media.MediaRecorder.OutputFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;
import java.io.File;

public class MainActivity extends PluginActivity {

    private static final String RECORDER_TAG = "Recorder";
    private static final String PLAYER_TAG = "Player";

    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;
    private String soundFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        soundFilePath = getFilesDir() + File.separator + "mySound.wav";

        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == KeyReceiver.KEYCODE_WLAN_ON_OFF) {
                    if (!isRecording) {
                        startRecorder();
                        notificationLedBlink(LedTarget.LED7, LedColor.RED, 2000);
                    } else {
                        stopRecorder();
                        notificationLedHide(LedTarget.LED7);
                    }
                } else if (keyCode == KeyReceiver.KEYCODE_CAMERA && !isRecording) {
                    startPlayer();
                }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {
            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
    }

    private void startRecorder() {
        new MediaRecorderPrepareTask().execute();
    }

    private void stopRecorder() {
        try {
            mediaRecorder.stop();
        } catch (RuntimeException e) {
            Log.d(RECORDER_TAG, "RuntimeException: stop() is called immediately after start()");
            deleteSoundFile();
        } finally {
            isRecording = false;
            releaseMediaRecorder();
        }
        Log.d(RECORDER_TAG, "Stop");
    }

    private void startPlayer() {
        File file = new File(soundFilePath);
        if (!file.exists()) {
            return;
        }
        file = null;

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0);

        MediaPlayer mediaPlayer = new MediaPlayer();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        try {
            mediaPlayer.setAudioAttributes(attributes);
            mediaPlayer.setDataSource(soundFilePath);
            mediaPlayer.setVolume(1.0f, 1.0f);
            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });
            mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            mediaPlayer.prepare();
            Log.d(PLAYER_TAG, "Start");
        } catch (Exception e) {
            Log.e(RECORDER_TAG, "Exception starting MediaPlayer: " + e.getMessage());
            mediaPlayer.release();
            notificationError("");
        }
    }

    private boolean prepareMediaRecorder() {
        Log.d(RECORDER_TAG, soundFilePath);
        deleteSoundFile();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setParameters("RicUseBFormat=false");

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(AudioSource.MIC);
        mediaRecorder.setOutputFormat(OutputFormat.DEFAULT);
        mediaRecorder.setAudioEncoder(AudioEncoder.DEFAULT);
        mediaRecorder.setOutputFile(soundFilePath);

        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            Log.e(RECORDER_TAG, "Exception preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }

        return true;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void deleteSoundFile() {
        File file = new File(soundFilePath);
        if (file.exists()) {
            file.delete();
        }
        file = null;
    }

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long
     * blocking operation.
     */
    private class MediaRecorderPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (prepareMediaRecorder()) {
                mediaRecorder.start();
                isRecording = true;
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Log.e(RECORDER_TAG, "MediaRecorder prepare failed");
                notificationError("");
                return;
            }
            Log.d(RECORDER_TAG, "Start");
        }
    }

}