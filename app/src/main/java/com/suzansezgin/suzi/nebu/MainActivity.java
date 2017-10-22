package com.suzansezgin.suzi.nebu;

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
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Delayed;

import static android.os.SystemClock.sleep;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener {

    //etiket
    private final String TAG = getClass().getSimpleName();

	private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    private FrameLayout preview;


    ImageScanner scanner;
    String metin;

    private boolean barcodeScanned = false;
    private boolean previewing = true;

    //yazı seslendirme
    private TextToSpeech mTts;
    private boolean ttsCalisirDurumda = false;
    private static final String uttIdMetin = "metin";
    private String metin1 ="Lütfen bilgi almak istidiğiniz ürünü gösterir misiniz??";
    private static final String uttIdMetin1 = "metin1";
    private String metin2 ="Başka Bir ürün göstermek ister misiniz??";
    private String metin3 ="Sizi anlayamadım. lütfen tekrar edermisiniz?";
    private static final String uttIdMetin2 = "metin2";
    private static final String uttIdMetin3 = "metin3";
    private static final String evet="Evet";
    private static final String hayır="Hayır";
    private  static String cevap;


    //test
    private int num;
    private int s;
    private Handler handler;
    private Runnable runnable;

    //konusma algılama
    private SpeechRecognizer speechRecognizer;

    

    static {
        System.loadLibrary("iconv");
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        mTts = new TextToSpeech(this,this);

        //test
        num = 1;
        handler = new Handler();
        runnable = new Runnable() {

            @Override
            public void run() {

                if(num > 0) {

                    num--;
                    handler.postDelayed(runnable, 2000);

                }else if(num == 0){


                    num = 1;
                    speak(metin1, uttIdMetin);


                }


            }

        };
        handler.post(runnable);
        barkod();

    }
    public void barkod() {
           setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


           autoFocusHandler = new Handler();
           mCamera = getCameraInstance();

        /* Instance barcode scanner */
           scanner = new ImageScanner();
           scanner.setConfig(0, Config.X_DENSITY, 3);
           scanner.setConfig(0, Config.Y_DENSITY, 3);

           mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
           preview = (FrameLayout)findViewById(R.id.cameraPreview);
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
        }
        catch (Exception e){

        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
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
                        metin=sym.getData();
                        speak(metin, uttIdMetin1);
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
                switch (utteranceId) {
                    case uttIdMetin:
                        break;
                    case uttIdMetin1:
                        speak(metin2,uttIdMetin2);
                        break;
                    case uttIdMetin2:
                        yaz();
                        break;
                    case uttIdMetin3:
                        System.exit(0);
                        break;

                }

            }
        });
    }
    public void  yaz() {

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override public void onRmsChanged(float rmsdB) {

                // TODO Auto-generated method stub

            }

            @Override public void onResults(Bundle results) {


                ArrayList<String> speechResults= results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                for (String speechResult : speechResults) {
                    Log.i(TAG,speechResult);
                }
                cevap=speechResults.get(0);
                kontrol();




            }
            @Override public void onReadyForSpeech(Bundle params) {

                // TODO Auto-generated method stub

            }
            @Override public void onPartialResults(Bundle partialResults) {

                // TODO Auto-generated method stub

            }
            @Override public void onEvent(int eventType, Bundle params) {

                // TODO Auto-generated method stub

            }
            @Override public void onError(int error) {

                speak(metin3,uttIdMetin2);

            }
            @Override public void onEndOfSpeech() {


            }
            @Override public void onBufferReceived(byte[] buffer) {

            }

            @Override public void onBeginningOfSpeech() {



            }
        });
        Intent recognizerIntent = new Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

        speechRecognizer.startListening(recognizerIntent);

    }
    @Override
    protected void onDestroy() {

        if (speechRecognizer != null) {

            speechRecognizer.stopListening();

            speechRecognizer.cancel();

            speechRecognizer.destroy(); }

        super.onDestroy();
    }
    protected void kontrol() {
        switch (cevap){
            case evet:
                speak(metin1,uttIdMetin);
                preview.removeView(mPreview);
                barkod();
                break;
            case hayır:
                speak("güle güle",uttIdMetin3);
                break;
            default:
                speak(metin3,uttIdMetin2);
                break;
        }
    }

}
