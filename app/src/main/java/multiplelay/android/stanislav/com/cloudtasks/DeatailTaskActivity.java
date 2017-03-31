package multiplelay.android.stanislav.com.cloudtasks;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeatailTaskActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView mIVpicture;

    private Button mBTNaddPicture;

    private File mTempPhoto;

    private String mImageUri = "";

    private String mRereference = "";

    private static final int REQUEST_CODE_PERMISSION_RECEIVE_CAMERA = 102;
    private static final int REQUEST_CODE_TAKE_PHOTO = 103;

    private StorageReference mStorageRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deatail_task);

        mIVpicture = (ImageView) findViewById(R.id.iv_piture);
        mBTNaddPicture = (Button) findViewById(R.id.btn_add_picture);

        mBTNaddPicture.setOnClickListener(this);

        File localFile = null;

        mRereference = getIntent().getStringExtra("Reference");
        mStorageRef = FirebaseStorage.getInstance().getReference();

        try {
            localFile = createTempImageFile(getExternalCacheDir());
            final File finalLocalFile = localFile;

            mStorageRef.child("images/" + mRereference).getFile(localFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            Picasso.with(getBaseContext())
                                    .load(Uri.fromFile(finalLocalFile))
                                    .into(mIVpicture);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.i("Load","" + e);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    //Метод для добавления фото
    private void addPhoto() {

        //Проверяем разрешение на работу с камерой
        boolean isCameraPermissionGranted = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        //Проверяем разрешение на работу с внешнем хранилещем телефона
        boolean isWritePermissionGranted = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        //Если разрешения != true
        if(!isCameraPermissionGranted || !isWritePermissionGranted) {

            String[] permissions;//Разрешения которые хотим запросить у пользователя

            if (!isCameraPermissionGranted && !isWritePermissionGranted) {
                permissions = new String[] {android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            } else if (!isCameraPermissionGranted) {
                permissions = new String[] {android.Manifest.permission.CAMERA};
            } else {
                permissions = new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            }
            //Запрашиваем разрешения у пользователя
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION_RECEIVE_CAMERA);
        } else {
            //Если все разрешения получены
            try {
                mTempPhoto = createTempImageFile(getExternalCacheDir());
                mImageUri = mTempPhoto.getAbsolutePath();

                //Создаём лист с интентами для работы с изображениями
                List<Intent> intentList = new ArrayList<>();
                Intent chooserIntent = null;


                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                takePhotoIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTempPhoto));

                intentList = addIntentsToList(this, intentList, pickIntent);
                intentList = addIntentsToList(this, intentList, takePhotoIntent);

                if (!intentList.isEmpty()) {
                    chooserIntent = Intent.createChooser(intentList.remove(intentList.size() - 1),"Choose your image source");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray(new Parcelable[]{}));
                }

                /*После того как пользователь закончит работу с приложеним(которое работает с изображениями)
                 будет вызван метод onActivityResult
                */
                startActivityForResult(chooserIntent, REQUEST_CODE_TAKE_PHOTO);
            } catch (IOException e) {
                Log.e("ERROR", e.getMessage(), e);
            }
        }
    }

    //Получаем абсолютный путь файла из Uri
    private String getRealPathFromURI(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        @SuppressWarnings("deprecation")
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int columnIndex = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(columnIndex);
    }

    /*
      File storageDir -  абсолютный путь к каталогу конкретного приложения на
      основном общем /внешнем устройстве хранения, где приложение может размещать
      файлы кеша, которыми он владеет.
     */
    public static File createTempImageFile(File storageDir) throws IOException {

        // Генерируем имя файла
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());//получаем время
        String imageFileName = "photo_" + timeStamp;//состовляем имя файла

        //Создаём файл
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    /*
    Метод для добавления интента в лист интентов
    */
    public static List<Intent> addIntentsToList(Context context, List<Intent> list, Intent intent) {
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resInfo) {
            String packageName = resolveInfo.activityInfo.packageName;
            Intent targetedIntent = new Intent(intent);
            targetedIntent.setPackage(packageName);
            list.add(targetedIntent);
        }
        return list;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_add_picture){
            addPhoto();
        }
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode , resultCode , data);
        switch (requestCode){
        case REQUEST_CODE_TAKE_PHOTO:
            if(resultCode == RESULT_OK) {
                if (data != null && data.getData() != null) {
                    mImageUri = getRealPathFromURI(data.getData());

                    Picasso.with(getBaseContext())
                            .load(data.getData())
                            .into(mIVpicture);
                    uploadFileInFireBaseStorage(data.getData());
                } else if (mImageUri != null) {
                    mImageUri = Uri.fromFile(mTempPhoto).toString();

                    Picasso.with(this)
                            .load(mImageUri)
                            .into(mIVpicture);
                    uploadFileInFireBaseStorage(Uri.fromFile((mTempPhoto)));
                }
            }
            break;
            }
    }

    public void uploadFileInFireBaseStorage (Uri uri){
        UploadTask uploadTask = mStorageRef.child("images/" + mRereference).putFile(uri);
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred());
                Log.i("Load","Upload is " + progress + "% done");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
               Uri donwoldUri = taskSnapshot.getMetadata().getDownloadUrl();
                Log.i("Load" , "Uri donwlod" + donwoldUri);
            }
        });
    }
}
