package touma.pkumap.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by apple on 2017/11/4.
 */

public class MyPoiInfo implements Serializable {
    public final String name;
    public final double latitude;
    public final double longitude;
    public final String intro;

    private double dis = Double.MAX_VALUE;

    public MyPoiInfo(PoiInfo poiInfo) {
        String name = null;
        double latitude = 0;
        double longitude = 0;
        String intro = null;
        try {
            latitude = poiInfo.location.latitude;
            longitude = poiInfo.location.longitude;
            String info = getInfo(poiInfo.name);
            if (info != null) {
                name = parseJson(info, "name");//rename from baidu to intro
                intro = parseJson(info, "intro");
            } else {
                name = poiInfo.name;
                intro = "";
            }
        } catch (Exception e) {
            //crash if you search something like "地铁2号线" since it's not a normal poi
        }
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.intro = intro;
    }
    public MyPoiInfo(MapPoi mapPoi) {
        latitude = mapPoi.getPosition().latitude;
        longitude = mapPoi.getPosition().longitude;
        String info = getInfo(mapPoi.getName());//TODO: data sparse
        if (info != null) {
            name = parseJson(info, "name");//rename from baidu to intro
            intro = parseJson(info, "intro");
        } else {
            name = mapPoi.getName();
            intro = "";
        }
    }
    public MyPoiInfo(String name) {
        String info = getInfo(name);
        if (info != null) {
            this.name = parseJson(info, "name");//rename from recognition to intro
            String[] pos = parseJson(info, "pos").split(",");
            latitude = Double.valueOf(pos[1]);
            longitude = Double.valueOf(pos[0]);
            intro = parseJson(info, "intro");
        } else {
            this.name = name;
            latitude = 0;
            longitude = 0;
            intro = "";
        }
    }
    public MyPoiInfo(JSONObject obj) {
        String name = null;
        double latitude = 0;
        double longitude = 0;
        String intro = null;
        try {
            name = obj.getString("name");
            String[] pos = obj.getString("pos").split(",");
            latitude = Double.valueOf(pos[1]);
            longitude = Double.valueOf(pos[0]);
            intro = obj.getString("intro");
            dis = Double.valueOf(obj.getString("dis"));
        } catch (Exception e) {
            //JSONException
        }
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.intro = intro;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        MyPoiInfo poiInfo = (MyPoiInfo) obj;
        return (name != null && name.equals(poiInfo.name));//distinguishing by name is okay

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public static String getInfo(String name) {
        try {
            /*done - 4. 自定义景点搜索：find_place
            post:
            {
                'target': '***'
            }
            return:
            {
                'name': '***',
                'pos': '1.2,1.3',
                'intro': '***',
                'image_1by1_url': '图片地址',
	            'image_3by2_url': '图片地址'
            }*/
            return new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    String name = params[0];

                    HttpClientUtil httpClientUtil = new HttpClientUtil();
                    Map<String, String> paras = new HashMap<>();
                    paras.put("target", name);
                    String res = httpClientUtil.doPost("http://39.106.19.147:131/find_place", paras, "utf-8");
                    if (res.startsWith("\n") || res.equals("error: Can't find relative places in our data. ")) {
                        return null;//wrong input, return a html file or error info
                    } else {
                        return res;
                    }
                }
            }.execute(name).get();
        } catch (Exception e) {
            //Interrupted
        }
        return null;
    }

    public static MyPoiInfo getNearestPoi(LatLng latLng) {
        try {
            /*done - 3. 自定义景点匹配：find_nearest
            post:
            {
                'pos': '1.2,1.3'
            }
            return:
            [
                {
                    'name': '***',
                    'pos': '1.2,1.3',
                    'intro': '***',
                    'dis':'1.53',
                } *5
            ]*/
            return new AsyncTask<Double, Void, MyPoiInfo>() {
                @Override
                protected MyPoiInfo doInBackground(Double... params) {
                    HttpClientUtil httpClientUtil = new HttpClientUtil();
                    Map<String, String> paras = new HashMap<>();
                    paras.put("pos", params[0] + "," + params[1]);
                    String res = httpClientUtil.doPost("http://39.106.19.147:131/find_nearest", paras, "utf-8");
                    //ArrayList<MyPoiInfo> poiArray = new ArrayList<>();
                    try {
                        JSONArray array = new JSONArray(res);
                        MyPoiInfo poi = new MyPoiInfo(array.getJSONObject(0));
                        /*for (int i = 0; i < array.length(); ++i) {
                            poiArray.add(new MyPoiInfo(array.getJSONObject(i)));
                        }*/
                        if (poi.dis <= 0.0005) {
                            return poi;
                        } else {//check if the distance is small enough, and thus it may return null
                            return null;
                        }
                    } catch (Exception e) {
                        //JSONException
                    }
                    return null;
                }
            }.execute(latLng.longitude, latLng.latitude).get();
        } catch (Exception e) {
            //Interrupted
        }
        return null;
    }

    public static Bitmap getImageByUrl(String url) {//"http://ww4.sinaimg.cn/bmiddle/786013a5jw1e7akotp4bcj20c80i3aao.jpg"
        try {
            return new AsyncTask<String, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(String... params) {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpGet httpGet = new HttpGet(params[0]);
                    HttpResponse httpResponse = null;
                    try {
                        httpResponse = httpClient.execute(httpGet);
                        if (httpResponse.getStatusLine().getStatusCode() == 200) {
                            byte[] data = EntityUtils.toByteArray(httpResponse.getEntity());
                            return BitmapFactory.decodeByteArray(data, 0, data.length);
                        }
                    } catch (Exception e) {
                        //IOException
                    }
                    return null;
                }
            }.execute(url).get();
        } catch (Exception e) {
            //Interrupted
        }
        return null;
    }

    public static String parseJson(String str, String key) {
        try {
            JSONObject obj = new JSONObject(str);
            return obj.getString(key);//通过key字段获取其所包含的字符串
        } catch (Exception e) {
            //JSONException
        }
        return null;
    }
}
