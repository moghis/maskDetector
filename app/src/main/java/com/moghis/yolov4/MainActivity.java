package com.moghis.yolov4;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.io.File.separator;

public class MainActivity extends AppCompatActivity {

    private static String[] PERMISSIONS_STORAGE = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static String[] PERMISSIONS_CAMERA = {
        Manifest.permission.CAMERA
    };

    private static final int REQUEST_CAMERA = 1006;
    private static final int REQUEST_WRITE_STORAGE = 1057;
    private static final int REQUEST_READ_STORAGE = 1684;
    private static final int REQUEST_PICK_IMAGE = 2;

    private ImageView resultImageView;
    private Button camera;
    private Button gallery;
    private Button takePicBtn;
    private TextView resultImgInfoText;
    private double threshold = 0.3, nms_threshold = 0.7;

    private int width;
    private int height;

    protected Bitmap mutableBitmap;

    ExecutorService detectService = Executors.newSingleThreadExecutor();

    private AtomicBoolean detectPhotoFront = new AtomicBoolean(true);
    private AtomicBoolean detectPhotoBack = new AtomicBoolean(true);
    private DetectAnalyzer detectAnalyzerFront = new DetectAnalyzer(detectPhotoFront);
    private DetectAnalyzer detectAnalyzerBack = new DetectAnalyzer(detectPhotoBack);
    private MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MyApplication app = (MyApplication)getApplication();

        YOLOv4.init(getAssets(), true);
        resultImageView = findViewById(R.id.resultImg);

        TextView rateDetectText = findViewById(R.id.rateDetectTextView);
        rateDetectText.setTypeface(app.getTypeface());

        gallery = findViewById(R.id.galleryDetectBtn);
        gallery.setTypeface(app.getTypeface());

        camera = findViewById(R.id.CameraDetectBtn);
        camera.setTypeface(app.getTypeface());

        takePicBtn = findViewById(R.id.takePicBtn);
        takePicBtn.setTypeface(app.getTypeface());

        SeekBar rateSeekBar = findViewById(R.id.rateDetectSeekBar);
        ImageView infoImg = findViewById(R.id.infoImg);
        ImageView exitImg = findViewById(R.id.exitImg);

        resultImgInfoText = findViewById(R.id.resultImgInfoText);
        resultImgInfoText.setTypeface(app.getTypeface());

        player = MediaPlayer.create(this, R.raw.shutter_sound);

        detectAnalyzerFront.setCameraType("front");
        detectAnalyzerBack.setCameraType("back");

        final int cameraPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cameraPermission != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        PERMISSIONS_CAMERA,
                        REQUEST_CAMERA
                );

            } else {

                detectPhotoFront.set(false);
                resultImgInfoText.setVisibility(View.INVISIBLE);
                startCamera(CameraX.LensFacing.FRONT,detectAnalyzerFront);
                camera.setText("دوربین عقب");

                camera.setOnClickListener(view -> {

                    if(detectPhotoFront.get()){
                        detectPhotoFront.set(false);
                        detectPhotoBack.set(true);
                        startCamera(CameraX.LensFacing.FRONT,detectAnalyzerFront);
                        camera.setText("دوربین عقب");
                        takePicBtn.setText("عکس");
                    } else if (detectPhotoBack.get()){
                        detectPhotoBack.set(false);
                        detectPhotoFront.set(true);
                        startCamera(CameraX.LensFacing.BACK,detectAnalyzerBack);
                        camera.setText("دوربین جلو");
                        takePicBtn.setText("عکس");
                    }

                });

            }
        } else {

            detectPhotoFront.set(false);
            resultImgInfoText.setVisibility(View.INVISIBLE);
            startCamera(CameraX.LensFacing.FRONT,detectAnalyzerFront);
            camera.setText("دوربین عقب");

            camera.setOnClickListener(view -> {

                if(detectPhotoFront.get()){
                    detectPhotoFront.set(false);
                    detectPhotoBack.set(true);
                    startCamera(CameraX.LensFacing.FRONT,detectAnalyzerFront);
                    camera.setText("دوربین عقب");
                    takePicBtn.setText("عکس");
                } else if (detectPhotoBack.get()){
                    detectPhotoBack.set(false);
                    detectPhotoFront.set(true);
                    startCamera(CameraX.LensFacing.BACK,detectAnalyzerBack);
                    camera.setText("دوربین جلو");
                    takePicBtn.setText("عکس");
                }

            });

        }

        final int readStoragePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {

                gallery.setOnClickListener(view ->
                        ActivityCompat.requestPermissions(
                        MainActivity.this,
                        PERMISSIONS_STORAGE,
                        REQUEST_READ_STORAGE
                ));

            } else {

                gallery.setOnClickListener(view -> {

                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, REQUEST_PICK_IMAGE);

                });

            }

        } else {

            gallery.setOnClickListener(view -> {

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_PICK_IMAGE);

            });

        }

        rateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                detectAnalyzerFront.setFrameRate(500 - i);
                detectAnalyzerBack.setFrameRate(500 - i);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        final int writeStoragePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (writeStoragePermission != PackageManager.PERMISSION_GRANTED) {

                takePicBtn.setOnClickListener(view ->
                        ActivityCompat.requestPermissions(
                        MainActivity.this,
                        PERMISSIONS_STORAGE,
                        REQUEST_WRITE_STORAGE
                ));

            } else {

                takePicBtn.setOnClickListener(view -> {

                    if(resultImgInfoText.getVisibility() == View.INVISIBLE) {

                        try {
                            saveImage(mutableBitmap, MainActivity.this);
                            player.start();
                            Toast.makeText(MainActivity.this, "در گالری ذخیره شد", Toast.LENGTH_SHORT).show();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                    } else {
                        Toast.makeText(MainActivity.this, "عکسی برای ذخیره وجود ندارد", Toast.LENGTH_SHORT).show();
                    }

                });

            }

        } else {

            takePicBtn.setOnClickListener(view -> {

                if(resultImgInfoText.getVisibility() == View.INVISIBLE) {

                    try {
                        saveImage(mutableBitmap,MainActivity.this);
                        player.start();
                        Toast.makeText(MainActivity.this,"در گالری ذخیره شد",Toast.LENGTH_SHORT).show();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                } else {
                    Toast.makeText(MainActivity.this, "عکسی برای ذخیره وجود ندارد", Toast.LENGTH_SHORT).show();
                }

            });

        }

        infoImg.setOnClickListener(view -> {

            CustomDialog dialog = new CustomDialog(MainActivity.this,CustomDialog.INFO);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

        });

        exitImg.setOnClickListener(view -> {

            CustomDialog dialog = new CustomDialog(MainActivity.this,CustomDialog.EXIT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

        });

    }

    private void startCamera(CameraX.LensFacing lensFacing , DetectAnalyzer detectAnalyzer) {
        CameraX.unbindAll();
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setLensFacing(lensFacing)
                .setTargetResolution(new Size(480, 640))
                .build();

        Preview preview = new Preview(previewConfig);
        CameraX.bindToLifecycle(this, preview, gainAnalyzer(detectAnalyzer,lensFacing));
    }


    private UseCase gainAnalyzer(DetectAnalyzer detectAnalyzer,CameraX.LensFacing lensFacing) {
        ImageAnalysisConfig.Builder analysisConfigBuilder = new ImageAnalysisConfig.Builder();
        analysisConfigBuilder.setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE);
        analysisConfigBuilder.setTargetResolution(new Size(480, 640));
        analysisConfigBuilder.setLensFacing(lensFacing);
        ImageAnalysisConfig config = analysisConfigBuilder.build();
        ImageAnalysis analysis = new ImageAnalysis(config);
        analysis.setAnalyzer(detectService,detectAnalyzer);
        return analysis;
    }

    private class DetectAnalyzer implements ImageAnalysis.Analyzer {

        private AtomicBoolean detecting = new AtomicBoolean(false);
        private AtomicBoolean detectPhoto;
        private String cameraType = "front";
        private long lastTimestamp = -10;
        private long currentTimestamp;
        private int frameRate = 250;

        private DetectAnalyzer(AtomicBoolean detectPhoto) {
            this.detectPhoto = detectPhoto;
        }

        public void setCameraType(String cameraType){
            this.cameraType = cameraType;
        }

        public void setFrameRate(int frameRate) {
            this.frameRate = frameRate;
        }

        @Override
        public void analyze(ImageProxy image, final int rotationDegrees) {
            if (detecting.get() || detectPhoto.get()) {
                return;
            }
            detecting.set(true);
            final Bitmap bitmapsrc = imageToBitmap(image);
            if (detectService == null) {
                detecting.set(false);
                return;
            }
            detectService.execute(() -> {
                Matrix matrix = new Matrix();

                if(cameraType.equals("front")){

                    matrix.postRotate(-rotationDegrees);
                    matrix.preScale(-1.0f, 1.0f);

                } else if(cameraType.equals("back")){

                    matrix.postRotate(rotationDegrees);

                }

                width = bitmapsrc.getWidth();
                height = bitmapsrc.getHeight();
                Bitmap bitmap = Bitmap.createBitmap(bitmapsrc, 0, 0, width, height, matrix, true);

                mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                currentTimestamp = System.currentTimeMillis();
                if((currentTimestamp - lastTimestamp) >= frameRate) {

                    Box[] result = YOLOv4.detect(bitmap, threshold, nms_threshold);
                    if (result == null) {
                        detecting.set(false);
                        return;
                    }
                    mutableBitmap = drawBoxRects(mutableBitmap, result);
                    lastTimestamp = System.currentTimeMillis();

                }

                runOnUiThread(() -> {
                    detecting.set(false);
                    if (detectPhoto.get()) {
                        return;
                    }
                    resultImageView.setImageBitmap(mutableBitmap);
                });
            });
        }

        private Bitmap imageToBitmap(ImageProxy image) {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ImageProxy.PlaneProxy y = planes[0];
            ImageProxy.PlaneProxy u = planes[1];
            ImageProxy.PlaneProxy v = planes[2];
            ByteBuffer yBuffer = y.getBuffer();
            ByteBuffer uBuffer = u.getBuffer();
            ByteBuffer vBuffer = v.getBuffer();
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();
            byte[] nv21 = new byte[ySize + uSize + vSize];
            // U and V are swapped
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();

            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }

    }

    protected Bitmap drawBoxRects(Bitmap mutableBitmap, Box[] results) {
        if (results == null || results.length <= 0) {
            return mutableBitmap;
        }
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint boxPaint = new Paint();
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth((float) (4 * mutableBitmap.getWidth() / 800));
        boxPaint.setTextSize((float) (40 * mutableBitmap.getWidth() / 800));
        for (Box box : results) {
            boxPaint.setColor(box.getColor());
            boxPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(box.getLabel() + String.format(Locale.ENGLISH, " %.3f", box.getScore()), box.x0 + 3, box.y0 + 40 * (float)mutableBitmap.getWidth() / 1000, boxPaint);
            boxPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(box.getRect(), boxPaint);
        }
        return mutableBitmap;
    }

    private void saveImage(Bitmap bitmap, Context context) throws FileNotFoundException {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            ContentValues values = contentValues();
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + "mask Detector");
            values.put(MediaStore.Images.Media.IS_PENDING, true);
            // RELATIVE_PATH and IS_PENDING are introduced in API 29.

            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                saveImageToStream(bitmap, context.getContentResolver().openOutputStream(uri));
                values.put(MediaStore.Images.Media.IS_PENDING, false);
                context.getContentResolver().update(uri, values, null, null);
            }
        } else {
            File directory = new File(Environment.getExternalStorageDirectory().toString() + separator + "mask Detector");
            // getExternalStorageDirectory is deprecated in API 29

            boolean result = true;

            if (!directory.exists()) {
                result = directory.mkdirs();
            }

            if(result) {
                String fileName = System.currentTimeMillis() + ".png";
                File file = new File(directory, fileName);
                saveImageToStream(bitmap, new FileOutputStream(file));
                ContentValues values = contentValues();
                values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
                // .DATA is deprecated in API 29
                context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }
        }
    }

    private ContentValues contentValues()
    {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        }
        return values;
    }

    private void saveImageToStream(Bitmap bitmap, OutputStream outputStream) {
        if (outputStream != null) {
            try {

                if(detectPhotoFront.get() && detectPhotoBack.get())
                    bitmap.compress(Bitmap.CompressFormat.JPEG , 100, outputStream);
                else
                    bitmap.compress(Bitmap.CompressFormat.PNG , 100, outputStream);

                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onDestroy() {
        if (detectService != null) {
            detectService.shutdown();
            detectService = null;
        }
        CameraX.unbindAll();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_WRITE_STORAGE){

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if(resultImgInfoText.getVisibility() == View.INVISIBLE) {

                    try {
                        saveImage(mutableBitmap,MainActivity.this);
                        player.start();
                        Toast.makeText(MainActivity.this,"در گالری ذخیره شد",Toast.LENGTH_SHORT).show();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                } else {
                    Toast.makeText(MainActivity.this, "عکسی برای ذخیره وجود ندارد", Toast.LENGTH_SHORT).show();
                }

                takePicBtn.setOnClickListener(view -> {

                    if(resultImgInfoText.getVisibility() == View.INVISIBLE) {

                        try {
                            saveImage(mutableBitmap,MainActivity.this);
                            player.start();
                            Toast.makeText(MainActivity.this,"در گالری ذخیره شد",Toast.LENGTH_SHORT).show();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                    } else {
                        Toast.makeText(MainActivity.this, "عکسی برای ذخیره وجود ندارد", Toast.LENGTH_SHORT).show();
                    }

                });

            } else {

                Toast.makeText(MainActivity.this, "برای استفاده باید دسترسی لازمه را بدهید", Toast.LENGTH_SHORT).show();

            }

        } else if(requestCode == REQUEST_READ_STORAGE) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_PICK_IMAGE);

                gallery.setOnClickListener(view -> startActivityForResult(intent, REQUEST_PICK_IMAGE));

            } else {

                Toast.makeText(MainActivity.this, "برای استفاده باید دسترسی لازمه را بدهید", Toast.LENGTH_SHORT).show();

            }

        }else if(requestCode == REQUEST_CAMERA){

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                detectPhotoFront.set(false);
                resultImgInfoText.setVisibility(View.INVISIBLE);
                startCamera(CameraX.LensFacing.FRONT,detectAnalyzerFront);
                camera.setText("دوربین عقب");
                takePicBtn.setText("عکس");

                camera.setOnClickListener(view -> {

                    if(detectPhotoFront.get()){
                        detectPhotoFront.set(false);
                        detectPhotoBack.set(true);
                        startCamera(CameraX.LensFacing.FRONT,detectAnalyzerFront);
                        camera.setText("دوربین عقب");
                        takePicBtn.setText("عکس");
                    } else if (detectPhotoBack.get()){
                        detectPhotoBack.set(false);
                        detectPhotoFront.set(true);
                        startCamera(CameraX.LensFacing.BACK,detectAnalyzerBack);
                        camera.setText("دوربین جلو");
                        takePicBtn.setText("عکس");
                    }

                });

            } else {

                Toast.makeText(MainActivity.this, "برای استفاده از دوربین باید دسترسی لازمه را بدهید", Toast.LENGTH_SHORT).show();

                camera.setOnClickListener(view ->
                        ActivityCompat.requestPermissions(
                        MainActivity.this,
                        PERMISSIONS_CAMERA,
                        REQUEST_CAMERA
                ));

            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        camera.setText("دوربین جلو");
        takePicBtn.setText("ذخیره");
        resultImgInfoText.setVisibility(View.INVISIBLE);
        detectPhotoFront.set(true);
        detectPhotoBack.set(true);

        Bitmap image = getPicture(data.getData());
        mutableBitmap = image.copy(Bitmap.Config.ARGB_8888, true);
        Box[] result = YOLOv4.detect(image, threshold, nms_threshold);

        mutableBitmap = drawBoxRects(mutableBitmap, result);

        resultImageView.setImageBitmap(mutableBitmap);
    }

    public Bitmap getPicture(Uri selectedImage) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
        int rotate = readPictureDegree(picturePath);
        return rotateBitmapByDegree(bitmap, rotate);
    }

    public int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    public Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }


}
