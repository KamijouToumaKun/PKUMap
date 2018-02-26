package touma.pkumap;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.util.ArrayList;

import touma.pkumap.util.MyRecognition;

/**
 * Created by apple on 2017/11/1.
 */

public class MainActivity extends AppCompatActivity {
    public enum CodeEnum {
        BTN_SEARCH, LISTVIEW, BTN_INTRO, INTRO, BTN_RECOGNIZE, RECOGNITION
    };
    private final int SDK_PERMISSION_REQUEST = 127;
    // 一个静态的Handler，Handler建议声明为静态的
    public static Handler handler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // after andrioid m,must request Permiision on runtime
        getPersimmions();
    }

    @Override
    protected void onStart() {
        super.onStart();

        new AsyncTask<Void, Void, Void>() {//一个欢迎界面
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                ImageView iv = (ImageView) findViewById(R.id.main_image);
                iv.setImageResource(R.drawable.pkumap);//it has to be loaded here, or its extremely high quality will lead to crash
            }
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    //Interrupted
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                //switch to PKUMapActivity
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, PKUMapActivity.class);
                startActivity(intent);
                finish();
                super.onPostExecute(aVoid);
            }
        }.execute();//an AsyncTask can be executed only once
    }

    @TargetApi(23)
    private void getPersimmions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            /***
             * 定位权限为必须权限，用户如果禁止，则每次进入都会申请
             */
            // 定位精确位置
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            //use camera
            if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.CAMERA);
            }
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
			/*
			 * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
			 */
            // 读写权限
            if (addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                permissionInfo += "Manifest.permission.WRITE_EXTERNAL_STORAGE Deny \n";
            }
            // 读取电话状态权限
            if (addPermission(permissions, Manifest.permission.READ_PHONE_STATE)) {
//                permissionInfo += "Manifest.permission.READ_PHONE_STATE Deny \n";
            }

            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
            }
        }
    }

    /*
    系统自动回调的情况:
    有一些情形下,调用
    1.自动授权: 如果用户已经允许了permission group中的一条A权限,那么当下次调用requestPermissions()方法请求同一个group中的B权限时,
    系统会直接调用onRequestPermissionsResult() 回调方法, 并传回PERMISSION_GRANTED的结果.
    2.自动拒绝: 如果用户选择了不再询问此条权限,那么app再次调用requestPermissions()方法来请求同一条权限的时候,
    系统会直接调用onRequestPermissionsResult()回调,返回PERMISSION_DENIED.
    */

    @TargetApi(23)
    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
            if (shouldShowRequestPermissionRationale(permission)){
                return true;
            }
            else{
                permissionsList.add(permission);
                return false;
            }

        }
        else{
            return true;
        }
    }
}

/*
     keytool -list -v -keystore ~/.android/debug.keystore, and enter
     MD5: 5A:C3:2D:40:A8:F2:F5:B9:48:2C:88:54:BD:55:AA:CD
	 SHA1: 18:ED:91:DB:65:31:71:D5:39:39:93:43:BB:03:B5:5C:98:05:F5:A0
	 SHA256: B5:BB:B6:F5:F1:C2:39:7D:6A:31:95:47:58:31:79:EE:FB:27:84:F9:77:7D:C0:9A:A5:3B:1C:BA:70:13:A0:16

	 API_KEY: EnmpCpw5weS1QB8FgDRLrY06r5PLVfUN
 */