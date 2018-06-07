package com.successive.mlkitdemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudText;
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudTextDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int SELECT_IMAGE_REQUEST = 101;
    private static final int MY_CAMERA_PERMISSION_CODE = 102;
    private static final int STORAGE_PERMISSION_CODE = 103;
    private String TAG = MainActivity.class.getSimpleName();
    TextView text, selectPhoto, capturePhoto;
    ImageView imageView;
    String data = "";
    private Uri mImageCaptureUri = null;
    ProgressDialog progressDialog;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = findViewById(R.id.text);
        imageView = findViewById(R.id.imageview);
        selectPhoto = findViewById(R.id.selectPhoto);
        capturePhoto = findViewById(R.id.capturePhoto);
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setCancelable(false);


        if (checkSelfPermission((Manifest.permission.WRITE_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED) {
            // request the permisssion
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }

        capturePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA},
                            MY_CAMERA_PERMISSION_CODE);
                } else {
                    dispatchCameraIntent();
                }

            }
        });

        selectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data = "";
                text.setText(data);
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGE_REQUEST);
            }
        });

    }

    private void dispatchCameraIntent() {
        data = "";
        text.setText(data);
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            mImageCaptureUri = createImageUri();
            if (mImageCaptureUri != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            Uri selectedImageUri = data.getData();
            if (null != selectedImageUri) {
                imageView.setImageURI(selectedImageUri);
                progressDialog.show();
                getTextFromImage(selectedImageUri);
            }
        } else if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            if (mImageCaptureUri != null) {
                imageView.setImageURI(mImageCaptureUri);
                progressDialog.show();
                getTextFromImage(mImageCaptureUri);
            }
        }
    }

    private void getTextFromImage(Uri uri) {
        FirebaseVisionImage image;
        try {
            image = FirebaseVisionImage.fromFilePath(MainActivity.this, uri);

            FirebaseVisionTextDetector detector = FirebaseVision.getInstance()
                    .getVisionTextDetector();

            Task<FirebaseVisionText> result =
                    detector.detectInImage(image)
                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                    // Task completed successfully
                                    Log.d(TAG, "onSuccess: text extracted successfuly");
                                    for (FirebaseVisionText.Block block : firebaseVisionText.getBlocks()) {
                                        Rect boundingBox = block.getBoundingBox();
                                        Point[] cornerPoints = block.getCornerPoints();
                                        data = data + " " + block.getText();

                                    }
                                    progressDialog.hide();
                                    text.setText(data);
                                }
                            })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                            Log.d(TAG, "onFailure: txet failure");
                                        }
                                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Uri createImageUri() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory() + "/DCIM/", "mlkit");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "image" + timeStamp + ".jpg");
        Uri imageUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(MainActivity.this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    mediaFile);
        } else {
            imageUri = Uri.fromFile(mediaFile);
        }
        return imageUri;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchCameraIntent();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }

        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do your action here
            } else {
                Toast.makeText(this, "Storage permission permission denied", Toast.LENGTH_LONG).show();
            }
        }

    }
}
