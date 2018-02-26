package touma.pkumap.util;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by apple on 2017/11/13.
 */

public class MyRecognition {
    public static String beginRecognition(String imagePath) {
        try {
            /*done - 1. 图片识别：img_rec
            post:
            {
                'image': 图片字节码
            }
            return:
            {
                'image': '***'
            }*/
            return new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    HttpClientUtil httpClientUtil = new HttpClientUtil();
                    Map<String, File> my_paras = new HashMap<>();
                    File file = new File(params[0]);
                    if (file.exists()) {
                        my_paras.put("image", file);
                        return MyPoiInfo.parseJson(httpClientUtil.doPostFile("http://39.106.19.147:131/img_rec", my_paras, "utf-8"), "image");
                    } else {
                        return "absence";
                    }
                }
            }.execute(imagePath).get();
        } catch (Exception e) {
            //Interrupt
        }
        return null;
    }
}