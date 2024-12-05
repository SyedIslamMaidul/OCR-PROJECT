package com.example.testrecognition;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.google.android.material.snackbar.Snackbar;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.testrecognition.databinding.ActivityMainBinding;



import java.io.IOException;

public class MainActivity extends AppCompatActivity {



    //UI views or in this we are delcare the button id
    private MaterialButton inputImageBtn;
    private MaterialButton recognizeTextBtn;
    private ShapeableImageView imageIv;

    private EditText recognizedTextEt;

    //Tag
    private static final String TAG = "MAIN_TAG";


    //Uri of the image that we will take from camera/Gallery
    private Uri imageUri = null;


    //to handle the result of Camera/Gallery permission
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;

    //arrays of permission required to pick image from Camera, gallery
    private String[] cameraPermissions ;
    private String[] storagePermissions;


    //progress dialog
    private ProgressDialog progressDialog;

    //text Recognizing
    private TextRecognizer textRecognizer;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init UI Views
        inputImageBtn = findViewById(R.id.inputImageBtn);
        recognizeTextBtn = findViewById(R.id.recognizeTextBtn);
        imageIv = findViewById(R.id.imageIv);
        recognizedTextEt = findViewById(R.id.recognizedTextEt);

        //init arrays of permission required for camera gallery
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //init setup the progress dialog, show while text from image is being recognized
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //init TextRecognizer
        textRecognizer =  TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);


        // handle click, show input image dialog
        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputImageDialog();
            }
        });

        //handle click start recognizing text from image we took from camera/gallery
        recognizeTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //check if image s picked or not picked if imageIri is not null

                if (imageUri == null)
                {
                    //imageUri is null, which means we haven't picked image yet, can't recognize text
                    Toast.makeText(MainActivity.this,"Pick image first.....",Toast.LENGTH_SHORT).show();
                }
                else
                {
                    //imageUri is not null, which means we have picked image,  we can recognize text
                    recognizedTextFromImage();
                }
            }
        });

    }

    private void recognizedTextFromImage() {
        Log.d(TAG, "recognizedTextFromImage: ");

        progressDialog.setMessage("Preparing image...");
        progressDialog.show();

        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);

            progressDialog.setMessage("Recognizing text....");

            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>(){
                        @Override
                        public void onSuccess(Text text)
                        {
                            progressDialog.dismiss();
                            String recognizedText = text.getText();
                            Log.d(TAG, "onSuccess: recognizedText "+recognizedText);

                            recognizedTextEt.setText(recognizedText);

                        }
                    })
            .addOnFailureListener(new OnFailureListener(){
                @Override
                public void onFailure(@NonNull Exception e){
// failed recognizing text from image, dismiss dialog show reason in toast
                    progressDialog.dismiss();
                    Log.e(TAG, "onFailure: ", e);
                    Toast.makeText(MainActivity.this,"Failed recognizing text due ton"+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });


        }
        catch (Exception e) {
            //Exception occurred while preparing image, dismiss dialog show reason in toast
            progressDialog.dismiss();
            Log.e(TAG, "recognizedTextFromImage: ", e);
            Toast.makeText(this,"Failed prepairing image due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void showInputImageDialog() {
        PopupMenu popupMenu = new PopupMenu(this, inputImageBtn);

        popupMenu.getMenu().add(Menu.NONE, 1, 1, "CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "GALLERY");


        popupMenu.show();


        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                //get item id that is clicked from popupmenu

                int id = menuItem.getItemId();
                if(id==1)
                {
                    //camera is clicked check if camera permission are granted or not
                    Log.d(TAG, "onMenuItemClick: Camera Clicked....");
                    if (checkCameraPermission())
                    {
                        //camera permission granted, we can launch camera intent
                        pickImageCamera();
                    }
                    else
                    {
                        //camera permission not granted, request the camera permission
                        requestCameraPermission();
                    }
                }
                else if (id==2)
                {
                    //Gallery is clicked check if storage permission are granted or not
                    Log.d(TAG, "onMenuItemClick: Gallery Clicked");
                    if (checkStoragePermission())
                    {
                        //storage permission granted, we can launch gallery intent
                        pickImageGallery();
                    }
                    else {
                        // storage permission not granted request the storage permission
                        requestStoragePermission();
                    }
                }
                return true;
            }
        });


    }

    private void pickImageGallery()
    {
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
                    if(result.getResultCode() == Activity.RESULT_OK)
                    {
                        // image picked
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri"+imageUri);

                        //set to image view
                        imageIv.setImageURI(imageUri);
                    }

                    else
                    {
                        Log.d(TAG, "onActivityResult: cancelled");
                        //cancelled
                        Toast.makeText(MainActivity.this, "Cancelled",Toast.LENGTH_SHORT);

                    }


                }
            }
    );

    private void pickImageCamera()
    {
        Log.d(TAG, "pickImageCamera: ");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, " Sample Description");


        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);


    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result)
                {
                    // here we will receive the image, if taken from camera
                    if(result.getResultCode() == Activity.RESULT_OK)
                    {
                        //image is taken from camera
                        // we already have the image in imageuri using function pickImageCamera()
                        Log.d(TAG, "onActivityResult: imageUri"+imageUri);
                        imageIv.setImageURI(imageUri);
                    }
                    else
                    {
                     //cancelled
                        Log.d(TAG, "onActivityResult: cancelled");
                     Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }

                }
            }
    );

    private boolean checkStoragePermission()
    {
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission()
    {

        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission ()
    {
        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

return cameraResult && storageResult;
    }

    private void requestCameraPermission()
    {
        //request camera permission for camera intent
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }


    //handle permission result

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions,grantResults);


        switch (requestCode)
        {
            case CAMERA_REQUEST_CODE:{
                if(grantResults.length>0)
                {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && storageAccepted)
                    {
                        pickImageCamera();
                    }
                    else
                    {
                        //one or both permission are denied, can't launch camera intent
                        Toast.makeText(this, "Camera & Storage permissions are required", Toast.LENGTH_SHORT).show();
                    }

                }
                else {
                    // Nether allowed not denied, rather cancelled
                    Toast.makeText(this,"Cancelled",Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                //check if same action from permission dialog performed or not Allow / deny
                if (grantResults.length>0)
                {
                    //Check if storage permission granted contains boolean result either true or false
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (storageAccepted)
                    {
                        //storage permission granted, we can launch gallery intent
                        pickImageGallery();
                    }
                    else {

                        //storage permission denied, can't  launch gallery intent
                        Toast.makeText(this,"Storage permission is requied", Toast.LENGTH_SHORT).show();

                    }

                }

            }
            break;
        }
    }

}