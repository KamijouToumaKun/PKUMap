package touma.pkumap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import touma.pkumap.util.MyPoiInfo;
import touma.pkumap.util.MyRecognition;

/**
 * Created by apple on 2017/11/15.
 */

public class RecognizeActivity extends AppCompatActivity {
    private static final int PHOTO_REQUEST_TAKEPHOTO = 61;// 6_1, 拍照
    private static final int PHOTO_REQUEST_GALLERY = 62;// 6_2, 从相册中选择
    private static final int PHOTO_REQUEST_CUT = 63;// 6_3, 结果
    //    "/storage/self/primary"
    //原图像 路径
    private static String imgPathOri;
    //原图像 URI
    Uri imgUriOri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ImageView iv = (ImageView) findViewById(R.id.recog_image);
        iv.setImageResource(R.drawable.pkumap);//it has to be loaded here, or its extremely high quality will lead to crash

        Button btn_upload = (Button) findViewById(R.id.btn_upload);
        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(MainActivity.CodeEnum.RECOGNITION.ordinal(), intent);
        super.onBackPressed();
    }

    //提示对话框方法
    private void showDialog() {
        new AlertDialog.Builder(this)
                .setTitle("图片识别")
                .setPositiveButton("拍照", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // 调用系统的拍照功能
                        openCamera();
                    }
                })
                .setNegativeButton("相册", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent(Intent.ACTION_PICK, null);
                        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
                        startActivityForResult(intent, PHOTO_REQUEST_GALLERY);
                    }
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //see http://blog.csdn.net/fuhao476200/article/details/71487432
        //see https://github.com/DanialFu/CameraDemo/blob/master/app/src/main/java/com/danielfu/camerademo/MainActivity.java
        switch (requestCode) {
            case PHOTO_REQUEST_TAKEPHOTO: {//path doesn't need modifying
                //I cannot save it to the album... let it go
                break;
            }
            case PHOTO_REQUEST_GALLERY: {//path needs modifying
                if (data != null && data.getData() != null) {
                    Uri imgUriSel = data.getData();
                    // 这里开始的第二部分，获取图片的路径：
                    String[] proj = {MediaStore.Images.Media.DATA};
                    // 好像是android多媒体数据库的封装接口，具体的看Android文档
                    @SuppressWarnings("deprecation")
                    Cursor cursor = managedQuery(imgUriSel, proj, null, null, null);
                    if (cursor == null) {
                        imgPathOri = imgUriSel.getPath();
                    } else {
                        // 按我个人理解 这个是获得用户选择的图片的索引值
                        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        // 将光标移至开头 ，这个很重要，不小心很容易引起越界
                        cursor.moveToFirst();
                        // 最后根据索引值获取图片路径
                        imgPathOri = cursor.getString(column_index);
                        cursor.close();
                    }
                }
                break;
            }
        }
        Intent intent = new Intent();
        if (imgPathOri != null) {
            String name = MyRecognition.beginRecognition(imgPathOri);
            intent.putExtra("Poi", new MyPoiInfo(name));
        }
        setResult(MainActivity.CodeEnum.RECOGNITION.ordinal(), intent);
        finish();//may return null if you don't choose any photo in the album
    }

    /**
     * 创建原图像保存的文件
     * @return
     * @throws IOException
     */
    private File createOriImageFile() throws IOException {
        String imgNameOri = "HomePic_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File pictureDirOri = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/OriPicture");
        if (!pictureDirOri.exists()) {
            pictureDirOri.mkdirs();
        }
        File image = File.createTempFile(
                imgNameOri,         /* prefix */
                ".jpg",             /* suffix */
                pictureDirOri       /* directory */
        );
        imgPathOri = image.getAbsolutePath();
        return image;
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File oriPhotoFile = null;
        try {
            oriPhotoFile = createOriImageFile();
        } catch (Exception e) {
            //IOEXception
        }
        if (oriPhotoFile != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                /**
                 * 7.0 调用系统相机拍照不再允许使用Uri方式，应该替换为FileProvider
                 * 并且这样可以解决MIUI系统上拍照返回size为0的情况
                 */
                imgUriOri = FileProvider.getUriForFile(RecognizeActivity.this, getPackageName() + ".provider", oriPhotoFile);
            } else {
                imgUriOri = Uri.fromFile(oriPhotoFile);
            }
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);//私有目录读写权限
            // 指定调用相机拍照后照片的储存路径
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUriOri);
            startActivityForResult(intent, PHOTO_REQUEST_TAKEPHOTO);
        }
    }
}
