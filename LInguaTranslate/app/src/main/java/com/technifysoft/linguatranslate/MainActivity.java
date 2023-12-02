package com.technifysoft.linguatranslate;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //----------------------------------------Translate---------------------------------------------

    //UI Views
    private EditText sourceLanguageEt;
    private TextView destinationLanguageTv;
    private MaterialButton sourceLanguageChooseBtn;
    private MaterialButton destinationLanguageChooseBtn;
    private MaterialButton translateBtn;

    //translator options to set and destination language e.g. Engllish -> Urdr
    private TranslatorOptions translatorOptions;
    //Translator object, for configuring it eith the source and target languages
    private Translator translator;
    //ProgressDialog to show while translation process
    private ProgressDialog progressDialog;
    //will contain list with language code and title
    private ArrayList<ModelLanguage> languageArrayList;
    //to show logs
    private static final String TAG = "MAIN_TAG";

    private String sourceLanguageCode = "en";
    private String sourceLanguageTitle = "English";
    private String destinationLanguageCode = "ur";
    private String destinationLanguageTitle = "Urdu";

    //----------------------------------------------------------------------------------------------

    //----------------------------------------Speech To Text----------------------------------------

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    //views from activity
    EditText mTextTv;
    MaterialButton mVoiceBtn;

    private final ActivityResultLauncher<Intent> speechActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            // Get text array from voice intent
                            ArrayList<String> resultArray = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            // Set to text view
                            mTextTv.setText(resultArray.get(0));
                        }
                    });

    //----------------------------------------------------------------------------------------------

    //--------------------------------Text To Speech Source Language--------------------------------

    //views
    EditText mTextET;
    MaterialButton mSpeakBtn, mStopBtn;

    TextToSpeech mTTS;

    //----------------------------------------------------------------------------------------------

    //--------------------------------Text To Speech Destination Language---------------------------

    //views

    Button mSpeak2Btn, mStop2Btn;

    //----------------------------------------------------------------------------------------------

    //--------------------------------------Text Recognizer-----------------------------------------

    //UI Views
    private MaterialButton inputImageBtn;
    private MaterialButton recognizeTextBtn;
    private ShapeableImageView imageTv;
    private EditText recognizedTextET;

    //Uri of the image that we will take from camera/gallery
    private Uri imageUri = null;

    //to handle the result of camera/gallery permission
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;

    //arrays of permission required to pick image from camera, gallery
    private String[] cameraPermission;
    private String[] storagePermission;

    //TextRecognizer
    private TextRecognizer textRecognizer;

    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //------------------------------------------Translate---------------------------------------

        //init UI Views
        sourceLanguageEt = findViewById(R.id.sourceLanguageEt);
        destinationLanguageTv = findViewById(R.id.destinationLanguageTv);
        sourceLanguageChooseBtn = findViewById(R.id.sourceLanguageChooseBtn);
        destinationLanguageChooseBtn = findViewById(R.id.destinationLanguageChooseBtn);
        translateBtn = findViewById(R.id.translateBtn);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        loadAvailableLanguages();

        //handle sourceLanguageChooseBtn click, choose source language (from list) which you want to translate
        sourceLanguageChooseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sourceLanguageChoose();
            }
        });
        //handle destinationLanguageChooseBtn cllick, choose destination language (from list) to which youo want to translate
        destinationLanguageChooseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view ) {
                destinationLanguageChoose();
            }
        });
        //handle TranslateBtn click, translate text to desired language
        translateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });

        //------------------------------------------------------------------------------------------

        //--------------------------------------speech to text--------------------------------------

        mTextTv = findViewById(R.id.sourceLanguageEt);
        mVoiceBtn = findViewById(R.id.voiceBtn);

        //button click to show speech to text dialog
        mVoiceBtn.setOnClickListener(view -> speak());

        //------------------------------------------------------------------------------------------

        //---------------------------Text To Speech Source Language---------------------------------

        mTextET = findViewById(R.id.sourceLanguageEt);
        mSpeakBtn = findViewById(R.id.speakBtn);
        mStopBtn = findViewById(R.id.stopBtn);

        mTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                //if there is no error then set language
                if (status != TextToSpeech.ERROR) {
                    mTTS.setLanguage(Locale.UK);
                } else {
                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //speak btn click
        mSpeakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get text from edit text
                String toSpeak = mTextET.getText().toString().trim();
                if (toSpeak.equals("")) {
                    //if there is no text to edit text
                    Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, toSpeak, Toast.LENGTH_SHORT).show();
                    //speak the text
                    mTTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });

        //stop btn click
        mStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTTS.isSpeaking()) {
                    //if it is speaking then stop
                    mTTS.stop();
                    mTTS.shutdown();
                } else {
                    //not speaking
                    Toast.makeText(MainActivity.this, "Not", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //------------------------------------------------------------------------------------------

        //----------------------------Text To Speech Destination Language---------------------------

        mSpeak2Btn = findViewById(R.id.speak2Btn);
        mStop2Btn = findViewById(R.id.stop2Btn);

        //speak btn click
        mSpeak2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get text from edit text
                String toSpeak = mTextET.getText().toString().trim();
                if (toSpeak.equals("")) {
                    //if there is no text to edit text
                    Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, toSpeak, Toast.LENGTH_SHORT).show();
                    //speak the text
                    mTTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });

        //stop btn click
        mStop2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTTS.isSpeaking()) {
                    //if it is speaking then stop
                    mTTS.stop();
                    mTTS.shutdown();
                } else {
                    //not speaking
                    Toast.makeText(MainActivity.this, "Not", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //--------------------------------------Text Recognizer-------------------------------------

        //init UI Views
        inputImageBtn = findViewById(R.id.inputImageBtn);
        recognizeTextBtn = findViewById(R.id.recognizedTextBtn);
        imageTv = findViewById(R.id.imageTv);
        recognizedTextET = findViewById(R.id.sourceLanguageEt);

        //init arrays of permission required for camera, gallery
        cameraPermission = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //init setup the progress dialog, show while text from image is being recognized
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //init TextRecognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        //handle click, show input image dialog
        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputImageDialog();
            }
        });

        //handle click, start recognizing text from image we took from camera/gallery
        recognizeTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check if image is picked or not, picked if imageUri is not null
                if (imageUri == null){
                    //imageUri is null, which means we haven't picked if image yet, can't recognize text
                    Toast.makeText(MainActivity.this, "Pick image first", Toast.LENGTH_SHORT).show();
                }else {
                    //imageUri is not null, which meanas we have picked image, we can recognize text
                    recognizedTextFromImage();
                }
            }
        });

        //------------------------------------------------------------------------------------------

    }

    //------------------------------------Translate-------------------------------------------------

    private String sourceLanguageText = "";

    private void validateData() {
        //input text to be translated
        sourceLanguageText = sourceLanguageEt.getText().toString().trim();
        //print in logs
        Log.d(TAG, "validateData: sourceLanguageText: "+sourceLanguageText);

        //validate data if empty show error message, otherwise start translation
        if (sourceLanguageText.isEmpty()){
            Toast.makeText(this, "Enter text to translate...", Toast.LENGTH_SHORT).show();
        } else {
            startTranslations();
        }
    }

    private void startTranslations() {
        //set progress message and show
        progressDialog.setMessage("Processing language model...");
        progressDialog.show();

        //init TranslatorOptions with source and target languages e.g. en and ur
        translatorOptions = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(destinationLanguageCode)
                .build();
        translator = Translation.getClient(translatorOptions);

        //init DownloadConditions with option to requireWifi (Optional)
        DownloadConditions downloadConditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();
        //start downloading translation model if required (will download 1st time)
        translator.downloadModelIfNeeded(downloadConditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //translation model ready to translated, lets translate
                        Log.d(TAG, "onSuccess: model ready, starting translate...");

                        //change progress message to translating...
                        progressDialog.setMessage("Translating...");

                        //start translation process
                        translator.translate(sourceLanguageText)
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        //successfully translated
                                        Log.d(TAG, "onSuccess: translatedText: "+translatedText);
                                        progressDialog.dismiss();

                                        destinationLanguageTv.setText(translatedText);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //failed to translated
                                        progressDialog.dismiss();
                                        Log.e(TAG, "onFailure: ", e);
                                        Toast.makeText(MainActivity.this, "failed to translate due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to to ready translation model, can't proceed to translation
                        progressDialog.dismiss();
                        Log.e(TAG, "onFailure: ", e);
                        Toast.makeText(MainActivity.this, "failed to ready model due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sourceLanguageChoose(){
        //init PopupMenu param 1 is context, param 2 is the UI view around which we want to show the pop up menu, to choose source language from list
        PopupMenu popupMenu = new PopupMenu(this, sourceLanguageChooseBtn);

        //from languageArraylist we will display language titles
        for (int i=0; i<languageArrayList.size(); i++){
            //keep adding titles in pop up menu item: param 1 is groupId, param 2 is itemId, param 3 is order, param 4 is title
            popupMenu.getMenu().add(Menu.NONE, i,i, languageArrayList.get(i).languageTitle);
        }

        // show popup menu
        popupMenu.show();

        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //get clicked item id which is position/index from the list
                int position = item.getItemId();

                //get code and title of the language selected
                sourceLanguageCode = languageArrayList.get(position).languageCode;
                sourceLanguageTitle = languageArrayList.get(position).languageTitle;
                //set the selected language to sourceLanguageChooseBtn as text and sourceLanguageEt as hint
                sourceLanguageChooseBtn.setText(sourceLanguageTitle);
                sourceLanguageEt.setHint("Enter"+sourceLanguageTitle);

                //show in logs
                Log.d(TAG, "onMenuItemClick: sourceLanguageCode: "+sourceLanguageCode);
                Log.d(TAG, "onMenuItemClick: sourceLanguageCode: "+sourceLanguageTitle);
                return false;
            }
        });
    }

    private void destinationLanguageChoose(){
        //init PopupMenu param is context, param 2 is the UI view around which we want to show the popup menu, to choose language from list
        PopupMenu popupMenu = new PopupMenu(this, destinationLanguageChooseBtn);

        //from languageArrayList as will display language titles
        for (int i=0; i<languageArrayList.size(); i++){
            //keep adding titles in popup menu ite: param 1 is groupId, param 2 is itemID, param 3 is order, param 4 is title
            popupMenu.getMenu().add(Menu.NONE, i,i, languageArrayList.get(i).getLanguageTitle());
        }

        // show popup menu
        popupMenu.show();

        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //get clicked item id which is position/index from the list
                int position = item.getItemId();

                //get code and title of the language selected
                destinationLanguageCode= languageArrayList.get(position).languageCode;
                destinationLanguageTitle = languageArrayList.get(position).languageTitle;

                //set the selected language to sourceLanguageChooseBtn as text
                destinationLanguageChooseBtn.setText(destinationLanguageTitle);

                //show in logs
                Log.d(TAG, "onMenuItemClick: destinationLanguageCode: "+destinationLanguageCode);
                Log.d(TAG, "onMenuItemClick: destinationLanguageTitle: "+destinationLanguageTitle);

                return false;
            }
        });
    }


    private void loadAvailableLanguages() {
        //init language array list before starting adding data into it
        languageArrayList = new ArrayList<>();

        //get list of all language codes e.g. en, ur, ar
        List<String> languageCodeList = TranslateLanguage.getAllLanguages();
        //to make list containing both the language code e.g. an and language title e.g. English
        for (String languageCode: languageCodeList){
            //get language title from language code e.g. en -> English
            String languageTitle = new Locale(languageCode).getDisplayLanguage();
            //print language code and title in logs
            Log.d(TAG, "loadAvailableLanguages: languageCode: "+languageCode);
            Log.d(TAG, "loadAvailableLanguages: languageTitle: "+languageTitle);

            //prepare language model and add in list
            ModelLanguage modelLanguage = new ModelLanguage(languageCode, languageTitle);
            languageArrayList.add(modelLanguage);
        }
    }

    //----------------------------------------------------------------------------------------------

    //------------------------------------Speech to text--------------------------------------------

    private void speak() {
        // Intent to show speech to text dialog
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi, speak something");

        // Start intent
        try {
            // If there was no error, show dialog
            speechActivityResultLauncher.launch(intent);
        } catch (Exception e) {
            // If there was some error
            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    //receive voice input anda handle it

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_CODE_SPEECH_INPUT:{
                if (resultCode == RESULT_OK && null!=data){
                    //get text array from voice intent
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    //set to text view
                    mTextTv.setText((result.get(0)));
                }
                break;
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    //---------------------------------------Text Recognizer----------------------------------------

    private void recognizedTextFromImage() {
        Log.d(TAG, "recognizedTextFromImage: ");
        //set message and show progress dialog
        progressDialog.setMessage("Preparing image...");
        progressDialog.show();


        try {
            // //prepare InputImage from imageUri
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);
            //image prepared, we are about to start text recognition process, change progress message
            progressDialog.setMessage("Recognizing text...");
            //start text recognition process from image
            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            //process completed, dismiss dialog
                            progressDialog.dismiss();
                            //get the recognized text
                            String recognizedText = text.getText();
                            Log.d(TAG, "onSuccess: recognizedText: "+recognizedText);
                            //set the recognized text to edit text
                            recognizedTextET.setText(recognizedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed recognizing text from image, dismiss dialog, show reason in Toast
                            progressDialog.dismiss();
                            Log.e(TAG, "onFailure: ", e);
                            Toast.makeText(MainActivity.this, "Failed recognizing text due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException e) {
            //exception occurred while preparing InputImage, dismiss dialog, show reason in Toast
            progressDialog.dismiss();
            Log.e(TAG, "recognizedTextFromImage: ", e);
            Toast.makeText(this, "Failed preparing image due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showInputImageDialog() {
        //init PopupMenu param1 is context, param2 is UI view where you want to show PopupMenu
        PopupMenu popupMenu = new PopupMenu(this, inputImageBtn);

        //add item camera, gallery to popupmenu, parm2 is menu id, parm3 is position of this menu items list, param 4 is title of the menu
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "GALLERY");

        //Show PopupMenu
        popupMenu.show();

        //handle PopupMenu item clicks
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //get item id that is clicked from PopupMenu
                int id = menuItem.getItemId();
                if (id==1){
                    //camera is click, check if camera permissions are granted or not
                    Log.d(TAG, "onMenuItemClick: Camera Clicked...");
                    if (checkCamerapermission()){
                        //camera permission granted, we can launch camera intent
                        pickImageCamera();
                    }else {
                        //camera permissions not granted, request the camera permissions
                        requestCameraPermission();
                    }
                }
                else if (id==2){
                    //gallery is clicked, check if storage permission is granted or not
                    Log.d(TAG, "onMenuItemClick: Gallery Clicked...");
                    if (checkStoragePermission()){
                        //storage permission granted, we can launch the gallery intent
                        pickImageGallery();
                    }else {
                        //storage permission not granted, request the storage permission
                        requestStoragePermission();
                    }
                }
                return true;
            }
        });
    }

    private void pickImageGallery(){
        Log.d(TAG, "pickImageGallery: ");
        //intent to pick image from gallery, will show all resources from where we can pick the image
        Intent intent = new Intent(Intent.ACTION_PICK);
        //set type of file we want to pick i.e. image
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here we will receive the image, if picked
                    if (result.getResultCode() == Activity.RESULT_OK){
                        //image picked
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri "+imageUri);
                        //set to imageview
                        imageTv.setImageURI(imageUri);
                    }else {
                        Log.d(TAG, "onActivityResult: cancelled");
                        //cancelled
                        Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");
        //get ready the image data to store in MediaStore
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description");
        //Image Uri
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        //Intent to launch camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        // Start the camera activity with the specified request code
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here we will receive the image, if taken from camera
                    if (result.getResultCode() == Activity.RESULT_OK){
                        //image is taken from camera
                        //we already have the image in imageUri using function pickImageCamera()
                        Log.d(TAG, "onActivityResult: imageUri "+imageUri);
                        imageTv.setImageURI(imageUri);
                    }else {
                        //cancelled
                        Log.d(TAG, "onActivityResult: cancelled");
                        Toast.makeText(MainActivity.this,"Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private boolean checkStoragePermission(){
        /*check if storage permission are allowed or not
        return true if allowed, false is not allowed*/
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission(){
        //request storage permission (for gallery image pick)
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkCamerapermission(){
        /*check if camera & storage permission are allowed or not
        return true if allowed, false is not allowed*/
        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return cameraResult && storageResult;
    }

    private void requestCameraPermission(){
        //request camera permission (for camera intent)
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    //handle permission result

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                //check if some action from permission dialog performed or not allow/Deny
                if (grantResults.length>0){
                    //check if camera, storage permission granted, contains boolean results either true or false
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    //check if both permission are granted or not
                    if (cameraAccepted && storageAccepted){
                        //both permission (camera & gallery) are granted, we can launch camera instead
                        pickImageCamera();
                    }else{
                        //one or both permission are denied, can't launch camera instead
                        Toast.makeText(this, "Camera & Storage permission are required", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    //nether allowed not denied, rather cancelled
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                //check if some action from permission dialog performed or not allow/deny
                if (grantResults.length>0){
                    //check if storage permission granted, contains boolean result either true or false
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    //check if storage permission is granted or not
                    if (storageAccepted){
                        //storage permission granted, we can launch gallery intent
                        pickImageGallery();
                    }else {
                        //storage permission denied, can't launch gallery instead
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }

    //----------------------------------------------------------------------------------------------
}