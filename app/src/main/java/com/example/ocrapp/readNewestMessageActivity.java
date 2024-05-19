//package com.example.ocrapp;
//
//import android.content.ContentResolver;
//import android.content.pm.PackageManager;
//import android.database.Cursor;
//import android.os.Bundle;
//import android.provider.Telephony;
//import android.speech.tts.TextToSpeech;
//import android.view.View;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.ListView;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import java.util.ArrayList;
//import java.util.Locale;
//
//public class readNewestMessageActivity extends AppCompatActivity {
//
//    private ArrayList<String> smsList = new ArrayList<>();
//    private TextToSpeech tts;
//    private ListView listView;
//    private Button speakSms;
//    private boolean isPaused = false;
//    private static final int READ_SMS_PERMISSION_CODE = 1;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_read_newest_message);
//
//        speakSms = findViewById(R.id.speakSMS);
//        listView = findViewById(R.id.listView);
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsList);
//        listView.setAdapter(adapter);
//
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{android.Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
//        } else {
//            readSms();
//        }
//
//        speakSms.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                speakSms();
//            }
//        });
//    };
//
//    private void readSms() {
//        ContentResolver contentResolver = getContentResolver();
//        Cursor cursor = contentResolver.query(
//                Telephony.Sms.CONTENT_URI,
//                null, null,
//                null,
//                null);
//
//        if (cursor != null && cursor.moveToFirst()) {
//            do {
//                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
//                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
//                smsList.add("Sender: " + address + "\nMessage: " + body);
//            } while (cursor.moveToNext());
//        }
//
//        if (cursor != null) {
//            cursor.close();
//        }
//    }
//
//    private void speakSms() {
//        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
//            @Override
//            public void onInit(int i) {
//                if (i == TextToSpeech.SUCCESS) {
//                    tts.setLanguage(Locale.US);
//                    tts.setSpeechRate(1.0f);
//                    for (int j = 0; j < smsList.size(); j++) {
//                        tts.speak(smsList.get(j).toString(), TextToSpeech.QUEUE_ADD, null);
//                    }
//                }
//            }
//        });
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == READ_SMS_PERMISSION_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                readSms();
//                ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();
//                adapter.notifyDataSetChanged();
//            }
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        if (tts != null) {
//            tts.stop();
//            tts.shutdown();
//        }
//        super.onDestroy();
//    }
//
//}

package com.example.ocrapp;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class readNewestMessageActivity extends AppCompatActivity implements SimpleGestureFilter.SimpleGestureListener {

    private static final int READ_SMS_PERMISSION_CODE = 1;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;

    private ArrayList<String> smsList = new ArrayList<>();
    private ArrayList<String> smsBodies = new ArrayList<>();
    private TextToSpeech tts;
    private ListView listView;
    private Button speakSms, findBySender;
    private FrameLayout swipeArea;

    private SimpleGestureFilter detector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_newest_message);

        speakSms = findViewById(R.id.speakSMS);
        findBySender = findViewById(R.id.findBySender);
        listView = findViewById(R.id.listView);
        swipeArea = findViewById(R.id.swipe_area);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsList);
        listView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        } else {
            readSms();
        }

        speakSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakAllSms();
            }
        });

        findBySender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> speakSingleSms(position));

        detector = new SimpleGestureFilter(readNewestMessageActivity.this, this);
        detector.setSwipeAreaView(swipeArea);
    }

    private void readSms() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null, null,
                null,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                smsList.add("Sender: " + address + "\nMessage: " + body);
                smsBodies.add(body);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    private void speakAllSms() {
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
//                    tts.setLanguage(Locale.US);
                    tts.setLanguage(new Locale("vi", "VN"));
                    tts.setSpeechRate(1.0f);
                    for (String sms : smsList) {
                        tts.speak(sms, TextToSpeech.QUEUE_ADD, null, null);
                    }
                }
            }
        });
    }

    private void speakSingleSms(int position) {
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.US);
                    tts.setSpeechRate(1.0f);
                    tts.speak(smsBodies.get(position), TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the sender's name or number");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                findMessagesBySender(spokenText);
            }
        }
    }

//    private void findMessagesBySender(String sender) {
//        ArrayList<String> foundMessages = new ArrayList<>();
//        for (String sms : smsList) {
//            if (sms.toLowerCase().contains(sender.toLowerCase())) {
//                foundMessages.add(sms);
//            }
//        }
//
//        if (foundMessages.isEmpty()) {
//            Toast.makeText(this, "No messages found for sender: " + sender, Toast.LENGTH_SHORT).show();
//        } else {
//            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, foundMessages);
//            listView.setAdapter(adapter);
//        }
//    }

    private void findMessagesBySender(String sender) {
        ArrayList<String> foundMessages = new ArrayList<>();
        ArrayList<String> foundBodies = new ArrayList<>();
        for (int i = 0; i < smsList.size(); i++) {
            String sms = smsList.get(i);
            if (sms.toLowerCase().contains(sender.toLowerCase())) {
                foundMessages.add(sms);
                foundBodies.add(smsBodies.get(i));
            }
        }

        if (foundMessages.isEmpty()) {
            Toast.makeText(this, "No messages found for sender: " + sender, Toast.LENGTH_SHORT).show();
        } else {
            smsList.clear();
            smsBodies.clear();
            smsList.addAll(foundMessages);
            smsBodies.addAll(foundBodies);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsList);
            listView.setAdapter(adapter);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readSms();
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onSwipe(int direction) {
        //Detect the swipe gestures and display toast
        String showToastMessage = "";

        switch (direction) {
            case SimpleGestureFilter.SWIPE_DOWN:
                showToastMessage = "Reading all message";
                speakAllSms();
                break;
            case SimpleGestureFilter.SWIPE_UP:
                showToastMessage = "Choosing sender.";
                promptSpeechInput();
                break;

        }
        Toast.makeText(this, showToastMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent me) {
        // Call onTouchEvent of SimpleGestureFilter class
        this.detector.onTouchEvent(me);
        return super.dispatchTouchEvent(me);
    }
    @Override
    public void onDoubleTap() {

    }
}
