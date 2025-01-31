package com.example.customkeyboard.viewmodel;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.customkeyboard.R;
import com.example.customkeyboard.utils.Utils;
import com.example.customkeyboard.utils.VoiceRecorder;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechContext;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import timber.log.Timber;

public class VoiceSpeechTextViewModel extends ViewModel implements ResponseObserver<StreamingRecognizeResponse> {

    VoiceRecorder mVoiceRecorder;
    VoiceRecorder.Callback callback;
    WeakReference<Context> context;
    SpeechClient speechClient;
    boolean isVoiceOngoing = false;
    boolean isLanguageChanged = false;
    ClientStream<StreamingRecognizeRequest> stream;
    StreamingRecognitionConfig streamingConfig;
    Thread recordingThread;
    MutableLiveData<String> transcript = new MutableLiveData<>();
    MutableLiveData<Utils.VoiceStatus> status = new MutableLiveData<>(Utils.VoiceStatus.DEFAULT);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private final Set<String> processedWords = new HashSet<>();

    public VoiceSpeechTextViewModel() {
        callback = new VoiceRecorder.Callback() {

            @Override
            public void onVoice(byte[] data, int size) {
                status.postValue(Utils.VoiceStatus.LISTENING);
                transcribeRecording(data, size);
            }

            @Override
            public void onVoiceEnd() {
                status.postValue(Utils.VoiceStatus.DEFAULT);
                if (stream != null) {
                    startVoiceRecorder();
                }
            }
        };
    }

    public void initializeSpeechClient(Context context) {
        try {
            this.context = new WeakReference<>(context);
            GoogleCredentials credentials = GoogleCredentials.fromStream(context.getResources().openRawResource(R.raw.voicecredential));
            FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);
            speechClient = SpeechClient.create(SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build());
        } catch (IOException e) {
            Timber.e("InitException%s", e.getMessage());
        }
    }

    public void checkPermission(boolean isLanguageChanged) {
        this.isLanguageChanged = isLanguageChanged;
        if (ContextCompat.checkSelfPermission(context.get(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent("REQUEST_PERMISSION");
            context.get().sendBroadcast(intent);
        } else {
            startVoiceRecorder();
        }
    }

    public void startVoiceRecorder() {
        playSound();
        if (isVoiceOngoing) {
            isVoiceOngoing = false;
            streamingConfig = null;
            stream.closeSend();
            stream = null;
            stopVoiceRecorder();
            status.postValue(Utils.VoiceStatus.DEFAULT);
            return;
        }
        isVoiceOngoing = true;
        createRecognizeRequestFromVoice();
        mVoiceRecorder = new VoiceRecorder(callback);
        mVoiceRecorder.start();
    }

    public void stopVoiceRecorderByKeyPress() {
        if (isVoiceOngoing) {
            isVoiceOngoing = false;
            streamingConfig = null;
            stream.closeSend();
            stream = null;
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                Timber.tag("stopVoiceRecorder").e("%s", e.getMessage());
            }
            recordingThread = null;
        }
    }

    private void transcribeRecording(byte[] data, int bufferSize) {
        new Thread(() -> {
            try {
                stream.send(
                        StreamingRecognizeRequest.newBuilder()
                                .setAudioContent(ByteString.copyFrom(data, 0, bufferSize))
                                .build());
            } catch (Exception e) {
                Timber.tag("transcribeRecording").e("%s", e.getMessage());
            }
        }).start();
    }

    private void createRecognizeRequestFromVoice() {
        try {
            stream = speechClient.streamingRecognizeCallable().splitCall(this);
            SpeechContext speechContext = SpeechContext.newBuilder()
                    .setBoost(20.0f) // Improve correct word selection
                    .build();

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16000)
                    .setModel("latest_long")
                    .setProfanityFilter(false)
                    .setUseEnhanced(true)
                    .setEnableWordTimeOffsets(true)
//                    .setEnableAutomaticPunctuation(!isLanguageChanged)
                    .setLanguageCode(isLanguageChanged ? "en-US" : "ta-IN")
                    .addSpeechContexts(speechContext)
                    .build();

            streamingConfig = StreamingRecognitionConfig.newBuilder()
                    .setConfig(config)
                    .setSingleUtterance(false)
                    .setInterimResults(true)
                    .build();
            stream.send(
                    StreamingRecognizeRequest.newBuilder()
                            .setStreamingConfig(streamingConfig)
                            .build());
        } catch (Exception e) {
            Timber.tag("createRecognizeRequestFromVoice").e("%s", e.getMessage());
        }
    }

    private void playSound() {
        MediaPlayer mediaPlayer;
        if (isVoiceOngoing) {
            mediaPlayer = MediaPlayer.create(context.get(), R.raw.transcribe_stop);
            status.postValue(Utils.VoiceStatus.DEFAULT);
        } else {
            mediaPlayer = MediaPlayer.create(context.get(), R.raw.transcribe_start);
        }
        mediaPlayer.start();

    }

    public LiveData<Utils.VoiceStatus> getVoiceStatus() {
        return status;
    }

    public LiveData<String> getTranscript() {
        return transcript;
    }

    @Override
    public void onStart(StreamController controller) {
        status.postValue(Utils.VoiceStatus.IDLE);
    }

    @Override
    public void onResponse(StreamingRecognizeResponse response) {
        new Thread(() -> {
            if (response.getResultsList().isEmpty()) return;

            // Get the last recognition result from the response
            StreamingRecognitionResult lastResult = response.getResultsList()
                    .get(response.getResultsList().size() - 1);

            // Check if there are alternatives in the last result
            if (!lastResult.getAlternativesList().isEmpty()) {
                // Extract the transcript from the first alternative
                String transcriptText = lastResult.getAlternatives(0).getTranscript();
                // Split the transcript into words and process them
                String[] words = transcriptText.split("\\s+"); // Split by spaces
                StringBuilder newWords = new StringBuilder();

                for (String word : words) {
                    // Check if the word has already been processed
                    if (!processedWords.contains(word)) {
                        // If not processed, add it to the set and append to newWords
                        processedWords.add(word);
                        newWords.append(word).append(" ");
                    }
                }
                if (newWords.length() > 0) {
                    transcript.postValue(newWords.toString().trim());
                }
            }
        }).start();

    }

    @Override
    public void onError(Throwable t) {
        Timber.tag("onError").e("%s", t.getMessage());
    }

    @Override
    public void onComplete() {
    }
}
