package com.pauluboteciler.nebu;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.HashMap;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener {

    //etiket
    private final String TAG = getClass().getSimpleName();
    private String metin1 ="lütfen ürününüzü gösteriniz";

    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    ImageScanner scanner;
    String metin;

    private boolean barcodeScanned = false;
    private boolean previewing = true;

    //yazı seslendirme
    private TextToSpeech mTts;
    private boolean ttsCalisirDurumda = false;
    private static final String uttIdMetin1 = "metin1";

    static {
        System.loadLibrary("iconv");
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mTts = new TextToSpeech(this,this);

        speak(metin1, uttIdMetin1);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        preview.addView(mPreview);



    }

    public void onPause() {
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
        super.onPause();
        releaseCamera();
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }



    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);

            if (result != 0) {

                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    // scanText.setText("Barkod Sonucu: " + sym.getData());
                    metin=sym.getData();

                    speak(metin, uttIdMetin1);
                    // scanText.setTextColor(Color.parseColor("#00AF03"));
                    barcodeScanned = true;
                    releaseCamera();
                    Intent intent = new Intent();
                    intent.putExtra("SCAN_RESULT", sym.getData());
                    setResult(RESULT_OK, intent);
                }
            }
        }
    };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    private void speak(String word){

        if(word != null) {

            HashMap<String, String> myHashAlarm = new HashMap<>();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Hello");
            mTts.speak(word, TextToSpeech.QUEUE_FLUSH, myHashAlarm);

        }

    }
    private void speak(String word, String utteranceID){

        if(ttsCalisirDurumda) {

            if (word != null) {

                HashMap<String, String> ttsMap = new HashMap<>();
                ttsMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceID);

                if (!mTts.isSpeaking()) {

                    mTts.speak(word, TextToSpeech.QUEUE_FLUSH, ttsMap);

                } else {

                    mTts.stop();
                    mTts.speak(word, TextToSpeech.QUEUE_FLUSH, ttsMap);

                }

            }

        }else {

            Log.i(TAG, "tts henüz çalışır durumda değil.");

        }

    }
    @Override
    public void onInit(int status) {

        if(status == TextToSpeech.SUCCESS) {
            mTts.setOnUtteranceCompletedListener(this);

            ttsCalisirDurumda = true;

        }

    }

    @Override
    public void onUtteranceCompleted(final String utteranceId) {
        Log.i("CALLBACK", utteranceId); //utteranceId == "SOME MESSAGE"
        runOnUiThread(new Runnable() {

            public void run() {
                if (utteranceId.equals(uttIdMetin1)) {
                }
            }
        });
    }

}
