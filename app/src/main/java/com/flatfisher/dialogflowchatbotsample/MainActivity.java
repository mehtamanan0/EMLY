package com.flatfisher.dialogflowchatbotsample;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.bassaer.chatmessageview.model.Message;
import com.github.bassaer.chatmessageview.view.ChatView;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ai.api.AIServiceException;
import ai.api.RequestExtras;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.GsonFactory;
import ai.api.model.AIContext;
import ai.api.model.AIError;
import ai.api.model.AIEvent;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,TextToSpeech.OnInitListener {

    public static final String TAG = MainActivity.class.getName();
    private Gson gson = GsonFactory.getGson();
    private AIDataService aiDataService;
    private ChatView chatView;
    private User myAccount;
    private User droidKaigiBot;
    private Double english ;
    private Double disadvantage;
    private String intent = "";
    private ImageButton btnSpeak;
    private TextToSpeech tts;
    private final int REQ_CODE_SPEECH_INPUT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tts = new TextToSpeech(this, this);

        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        initChatView();

        //Language, Dialogflow Client access token 
        final LanguageConfig config = new LanguageConfig("en", "80a20aec9205449bb61a5648fb6c6b60");
        initService(config);

        final Message receivedMessage = new Message.Builder()
                .setUser(droidKaigiBot)
                .setRightMessage(false)
                .setMessageText("Hi there! I am Emly, your personal employment evaluator!")
                .build();
        chatView.receive(receivedMessage);
        speakOut("Hi there! I am Emly, your personal employment evaluator!");
    }

    @Override
    public void onClick(View v) {
        //new message
        final Message message = new Message.Builder()
                .setUser(myAccount)
                .setRightMessage(true)
                .setMessageText(chatView.getInputText())
                .hideIcon(true)
                .build();
        //Set to chat view
        chatView.send(message);
        if (intent.equals("english")) {
            english = Double.parseDouble(chatView.getInputText());
        }
        if (intent.equals("Disadvantage")) {
            disadvantage = Double.parseDouble(chatView.getInputText());
        }
        sendRequest(chatView.getInputText());
        //Reset edit text
        chatView.setInputText("");
    }

    /*
    * AIRequest should have query OR event
    */
    private void sendRequest(String text) {
        Log.d(TAG, text);
        final String queryString = String.valueOf(text);
        final String eventString = null;
        final String contextString = null;

        if (TextUtils.isEmpty(queryString) && TextUtils.isEmpty(eventString)) {
            onError(new AIError(getString(R.string.non_empty_query)));
            return;
        }

        new AiTask().execute(queryString, eventString, contextString);
    }

    @Override
    public void onInit(int i) {

    }

    public class AiTask extends AsyncTask<String, Void, AIResponse> {
        private AIError aiError;

        @Override
        protected AIResponse doInBackground(final String... params) {
            final AIRequest request = new AIRequest();
            String query = params[0];
            String event = params[1];
            String context = params[2];

            if (!TextUtils.isEmpty(query)){
                request.setQuery(query);
            }

            if (!TextUtils.isEmpty(event)){
                request.setEvent(new AIEvent(event));
            }

            RequestExtras requestExtras = null;
            if (!TextUtils.isEmpty(context)) {
                final List<AIContext> contexts = Collections.singletonList(new AIContext(context));
                requestExtras = new RequestExtras(contexts, null);
            }

            try {
                return aiDataService.request(request, requestExtras);
            } catch (final AIServiceException e) {
                aiError = new AIError(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(final AIResponse response) {
            if (response != null) {
                onResult(response);
            } else {
                onError(aiError);
            }
        }
    }


    private void onResult(final AIResponse response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Variables
                gson.toJson(response);
                final Status status = response.getStatus();
                final Result result = response.getResult();
                final String speech = result.getFulfillment().getSpeech();
                final Metadata metadata = result.getMetadata();
                final HashMap<String, JsonElement> params = result.getParameters();

                // Logging
                Log.d(TAG, "onResult");
                Log.i(TAG, "Received success response");
                Log.i(TAG, "Status code: " + status.getCode());
                Log.i(TAG, "Status type: " + status.getErrorType());
                Log.i(TAG, "Resolved query: " + result.getResolvedQuery());
                Log.i(TAG, "Action: " + result.getAction());
                Log.i(TAG, "Speech: " + speech);

                if (metadata != null) {
                    Log.i(TAG, "Intent id: " + metadata.getIntentId());
                    Log.i(TAG, "Intent name: " + metadata.getIntentName());
                    if (metadata.getIntentName().equals("english")){
                        intent = "english";
                        Log.i(TAG, "Speech: " + metadata.getIntentName());
                    }
                    if (metadata.getIntentName().equals("Disadvantage")){
                        intent = "Disadvantage";
                        Log.i(TAG, "Speech: " + metadata.getIntentName());
                    }
                    if (metadata.getIntentName().equals("follow")) {
                        intent = "follow";
                    }
                    if (metadata.getIntentName().equals("exit")){
                        Log.i(TAG, "Speech: " + metadata.getIntentName());
                        intent = "exit";
                        int percent = model(english, disadvantage);
                        if (percent < 60) {
                            final Message receivedMessage = new Message.Builder()
                                    .setUser(droidKaigiBot)
                                    .setRightMessage(false)
                                    .setMessageText("Based on what we know from past data," +
                                            " unfortunately I think Vincent would need help now to have a better employment prospect " +
                                            "in future, His score : " + Integer.toString(percent))
                                    .build();
                            chatView.receive(receivedMessage);
                            speakOut("Based on what we know from past data," +
                                    " unfortunately I think Vincent would need help now to have a better employment prospect " +
                                    "in future, His score : " + Integer.toString(percent));
                        }
                        else if (percent > 60) {
                            final Message receivedMessage = new Message.Builder()
                                    .setUser(droidKaigiBot)
                                    .setRightMessage(false)
                                    .setMessageText("Based on what we know from past data, " +
                                            "I believe Vincent is doing great. His score : " + Integer.toString(percent))
                                    .build();
                            chatView.receive(receivedMessage);
                            speakOut("Based on what we know from past data, " +
                                    "I believe Vincent is doing great. His score : " + Integer.toString(percent));
                        }

                    }
                }

                if (params != null && !params.isEmpty()) {
                    Log.i(TAG, "Parameters: ");
                    for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                        Log.i(TAG, String.format("%s: %s",
                                entry.getKey(), entry.getValue().toString()));
                    }
                }



                //Update view to bot says
                if (metadata != null) {
                    if (!metadata.getIntentName().equals("exit")) {
                        final Message receivedMessage = new Message.Builder()
                                .setUser(droidKaigiBot)
                                .setRightMessage(false)
                                .setMessageText(speech.replace("__name__", "Vincent"))
                                .build();
                        chatView.receive(receivedMessage);
                        speakOut(speech.replace("__name__", "Vincent"));
                    }
                }
            }
        });
    }

    private void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,error.toString());
            }
        });
    }

    private void initChatView() {
        int myId = 0;
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        String myName = "Advisor";
        myAccount = new User(myId, myName, icon);

        int botId = 1;
        String botName = "Emly";
        droidKaigiBot = new User(botId, botName, icon);

        chatView = findViewById(R.id.chat_view);
        chatView.setRightBubbleColor(ContextCompat.getColor(this, R.color.lightBlue500));
        chatView.setLeftBubbleColor(Color.LTGRAY);
        chatView.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
        chatView.setSendButtonColor(ContextCompat.getColor(this, R.color.lightBlue500));
        chatView.setSendIcon(R.drawable.ic_action_send);
        chatView.setRightMessageTextColor(Color.WHITE);
        chatView.setLeftMessageTextColor(Color.BLACK);
        chatView.setUsernameTextColor(Color.GRAY);
        chatView.setSendTimeTextColor(Color.GRAY);
        chatView.setDateSeparatorColor(Color.GRAY);
        chatView.setInputTextHint("new message");
        chatView.setMessageMarginTop(5);
        chatView.setMessageMarginBottom(5);
        chatView.setOnClickSendButtonListener(this);
    }

    private void initService(final LanguageConfig languageConfig) {
        final AIConfiguration.SupportedLanguages lang =
                AIConfiguration.SupportedLanguages.fromLanguageTag(languageConfig.getLanguageCode());
        final AIConfiguration config = new AIConfiguration(languageConfig.getAccessToken(),
                lang,
                AIConfiguration.RecognitionEngine.System);
        aiDataService = new AIDataService(this, config);
    }

    private int model(Double englishwa, Double disadvantage){
        englishwa = min_max(englishwa, 1, 10);
        disadvantage = min_max(disadvantage, 1, 10);
        Log.i(TAG, "val: " + englishwa);
        Log.i(TAG, "val: " + disadvantage);
        return (int) (69.366 + (13.812 * englishwa) - (26.827 * disadvantage));
    }

    public double min_max(double val, double min, double max)
    {
        double new_min = 0;
        double new_max = 0.5;

        val = (((val-min)/(max-min))*(new_max-new_min))+new_min;
        return val;

    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    final Message message = new Message.Builder()
                            .setUser(myAccount)
                            .setRightMessage(true)
                            .setMessageText(result.get(0))
                            .hideIcon(true)
                            .build();
                    //Set to chat view
                    chatView.send(message);
                    sendRequest(result.get(0));
                }
                break;
            }

        }
    }

    private void speakOut(String textwa) {

        tts.speak(textwa, TextToSpeech.QUEUE_FLUSH, null);
    }
}