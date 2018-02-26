package touma.pkumap;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.demo.LocationApplication;
import com.baidu.location.service.LocationService;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.map.Marker;
import com.yuyh.library.BubblePopupWindow;

import touma.pkumap.util.MyNavigation;
import touma.pkumap.util.MyPoiInfo;

/**
 * Created by apple on 2017/11/1.
 */

public class PKUMapActivity extends AppCompatActivity {
    private BaiduMap mMap;
    private MapView mMapView;
    private Marker mSelectedMarker = null;
    private Marker mSrcMarker = null;
    private Marker mDestMarker = null;
    private MyPoiInfo mSelectedPoi = null;
    private MyPoiInfo mSrcPoi = null;
    private MyPoiInfo mDestPoi = null;
    private MyPoiInfo mNearestPoi = null;

    private boolean useBackupLocation = false;
    private LocationService locationService;
    final private BDAbstractLocationListener mListener = new BDAbstractLocationListener() {
        //Don't use old-fashioned BDLocationListener myLocationListener & LocationClient mLocationClient anymore!
        //or there will be an error 62 and you'll be fixed at LagLng (4.9E-324, 4.9E-324)
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            String info = "其他类型错误";
            switch (bdLocation.getLocType()) {
                case BDLocation.TypeGpsLocation:
                    info = "GPS定位";
                case BDLocation.TypeNetWorkLocation:
                    info = "网络定位";
                case BDLocation.TypeOffLineLocation:
                    info = "离线定位";
                useBackupLocation = false;
                break;

                case BDLocation.TypeServerError:
                    info = "服务端网络定位失败";
                case BDLocation.TypeNetWorkException:
                    info = "网络状态不佳";
                case BDLocation.TypeCriteriaException:
                    info = "手机问题：请先关闭飞行模式，或试着重启手机";
                default: ;
                useBackupLocation = true;
                break;
            }
//            Toast.makeText(PKUMapActivity.this, info, Toast.LENGTH_SHORT).show();
            if (!useBackupLocation) {
                LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                mMap.setMyLocationData(new MyLocationData.Builder()
                        .accuracy(bdLocation.getRadius())
                        .latitude(bdLocation.getLatitude())
                        .longitude(bdLocation.getLongitude())
                        .build());//TODO: error 62!!!
                approachPoi(latLng);
            }
        }
    };
    private LocationManager backupLocationManager;

    private View poiView;
    private Button btn_navi;
    private boolean isInNavigation = false;
    private MyNavigation myNavigation;
    private TextView tvApproach;
    private TextView tvQuery;
    private Button btn_recognize;

    private BubblePopupWindow viewApproach = new BubblePopupWindow(PKUMapActivity.this);
    private BubblePopupWindow viewQuery = new BubblePopupWindow(PKUMapActivity.this);
    private Runnable viewApproachClose;//bubbleClose will be called more than once, so it cannot be AsyncTask/Thread
    private Runnable viewQueryClose;

    private void selectPoi(MyPoiInfo poi, boolean ifMoveCamera) {//there's a bug: animateMapStatus/setMapStatus failure
        //remove old POI
        if (mSelectedPoi != null) {
            mSelectedMarker.remove();
        }
        //add new POI
        if (poi != null) {
            LatLng location = new LatLng(poi.latitude, poi.longitude);
            mSelectedMarker = (Marker) mMap.addOverlay(
                    new MarkerOptions()
                            .position(location)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding))//must add icon here
                            .zIndex(0)
                            .title(poi.name));
            if (ifMoveCamera) {
                mMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(location));
            }
            //update POIInfoView
            TextView poi_title = ((TextView) findViewById(R.id.view_poi_title));
            poi_title.setText(poi.name);
            TextView poi_text = ((TextView) findViewById(R.id.view_poi_text));
            poi_text.setText(poi.intro);
            ImageView imageView = (ImageView) findViewById(R.id.view_poi_image);
            String info = MyPoiInfo.getInfo(poi.name);
            if (info != null) {
                Bitmap bitmap = MyPoiInfo.getImageByUrl(MyPoiInfo.parseJson(info, "image_1by1_url"));
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    imageView.setImageResource(android.R.drawable.presence_offline);
                }
            } else {
                imageView.setImageResource(android.R.drawable.presence_offline);
            }
            poiView.setVisibility(View.VISIBLE);//must be put here
        } else {
            poiView.setVisibility(View.INVISIBLE);
        }
        //update POI
        mSelectedPoi = poi;

        Button btn_src = (Button) findViewById(R.id.btn_src);
        if (mSrcPoi==null || !mSrcPoi.equals(mSelectedPoi)) {
            btn_src.setText("起点");
        } else {
            btn_src.setText("取消起点");
        }
        Button btn_dest = (Button) findViewById(R.id.btn_dest);
        if (mDestPoi==null || !mDestPoi.equals(mSelectedPoi)) {
            btn_dest.setText("终点");
        } else {
            btn_dest.setText("取消终点");
        }
    }

    public void setSrcPoi(MyPoiInfo poi) {
        //remove old src
        if (mSrcPoi != null) {
            mSrcMarker.remove();
        }
        //add new src
        if (poi != null) {
            mSrcMarker = (Marker) mMap.addOverlay(
                    new MarkerOptions()
                            .position(new LatLng(poi.latitude, poi.longitude))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_st))//must add icon here
                            .zIndex(0)
                            .title(poi.name));
        }
        //update src
        mSrcPoi = poi;
    }

    public void setDestPoi(MyPoiInfo poi) {
        //remove old dest
        if (mDestPoi != null) {
            mDestMarker.remove();
        }
        //add new dest
        if (poi != null) {
            mDestMarker = (Marker) mMap.addOverlay(
                    new MarkerOptions()
                            .position(new LatLng(poi.latitude, poi.longitude))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_en))//must add icon here
                            .zIndex(0)
                            .title(poi.name));
        }
        //update dest
        mDestPoi = poi;
    }

    public MyPoiInfo getSelectedPoi() {
        return mSelectedPoi;
    }

    public MyPoiInfo getSrcPoi() {
        return mSrcPoi;
    }

    public MyPoiInfo getDestPoi() {
        return mDestPoi;
    }

    private void approachPoi(LatLng latLng) {
        //if you are close to the nearest Poi, set popup window
        final MyPoiInfo info = MyPoiInfo.getNearestPoi(latLng);
        if (info!=null && (mNearestPoi==null || (mNearestPoi!=null && !mNearestPoi.equals(info)))) {
            mNearestPoi = info;
            tvApproach.setText("您到了" + mNearestPoi.name + "的附近");
            viewApproach.show(btn_recognize, Gravity.LEFT, 0);
            new Thread(viewApproachClose).start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //requestCode stands for the previous activity
        if (requestCode == MainActivity.CodeEnum.BTN_SEARCH.ordinal()) {
//            selectPoi(((MyPoiInfo) data.getSerializableExtra("Poi")), true);
            MyPoiInfo poi = (MyPoiInfo) data.getSerializableExtra("Poi");
            if (poi != null) {
                final MyPoiInfo nearest = MyPoiInfo.getNearestPoi(new LatLng(poi.latitude, poi.longitude));
                if (nearest != null) {//may be null
                    selectPoi(nearest, false);
//                    tvQuery.setText("您想找的应该是：" + nearest.name); //TODO: name cannot be updated on textView here!!!
                    viewQuery.show(btn_recognize, Gravity.LEFT, 0);
                    new Thread(viewQueryClose).start();
                } else {
                    Toast.makeText(PKUMapActivity.this, "请搜索燕园范围内的景点哦！", Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == MainActivity.CodeEnum.BTN_INTRO.ordinal()) {

        } else if (requestCode == MainActivity.CodeEnum.BTN_RECOGNIZE.ordinal()) {
            final MyPoiInfo poi = (MyPoiInfo) data.getSerializableExtra("Poi");
            if (poi != null) {//in case that you don't choose any photo in the album
                selectPoi(poi, true);
//                tvQuery.setText("您想找的应该是：" + poi.name); //TODO: name cannot be updated on textView here
                viewQuery.show(btn_recognize, Gravity.LEFT, 0);
                new Thread(viewQueryClose).start();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_pku_map);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //About views and listeners:
        Button btn_search = (Button) findViewById(R.id.btn_search);
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //switch to SearchActivity
                Intent intent = new Intent(PKUMapActivity.this, SearchActivity.class);
                startActivityForResult(intent, MainActivity.CodeEnum.BTN_SEARCH.ordinal());
            }
        });

        btn_navi = (Button) findViewById(R.id.btn_navigation);
        btn_navi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInNavigation) {
                    myNavigation.endNavigation();
                    isInNavigation = false;
                    btn_navi.setText("导航");
                }
                else {
                    if (mSrcPoi != null && mDestPoi != null && mSrcPoi != mDestPoi) {
                        myNavigation.beginNavigation(mSrcPoi, mDestPoi);
                        isInNavigation = true;
                        btn_navi.setText("取消导航");
                    }
                    else {
                        Toast.makeText(PKUMapActivity.this, "请确认起点终点均已设置", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btn_recognize = (Button) findViewById(R.id.btn_recognize);
        btn_recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //switch to RecognizeActivity
                Intent intent = new Intent(PKUMapActivity.this, RecognizeActivity.class);
                startActivityForResult(intent, MainActivity.CodeEnum.BTN_RECOGNIZE.ordinal());
            }
        });

        poiView = (View) findViewById(R.id.view_poi_info);
        poiView.setVisibility(View.INVISIBLE);

        onViewReady();//About BubblePopupWindow & PoiInfoView
        onMapReady();//About map:
        onLocationPoiReady();//About location & poi
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onStop() {
        locationService.unregisterListener(mListener); //注销掉监听
        locationService.stop(); //停止定位服务
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    private String getBestProvider() {
        backupLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);//// 查询精度：高; ACCURACY_COARSE/NO_REQUIREMENT
        criteria.setAltitudeRequired(false);// 是否查询海拨：否
        criteria.setBearingRequired(false);// 是否查询方位角 : 否
        criteria.setCostAllowed(true);// 是否允许付费：是
        criteria.setPowerRequirement(Criteria.POWER_LOW);// 电量要求：低;NO_REQUIREMENT
        return backupLocationManager.getBestProvider(criteria, true);
    }

    private void beginBackupLocationListener() {
        /* GPS Constant Permission */ //TODO: What the hell are these two arguments???
        final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
        final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

        String provider = getBestProvider();//it initializes backupLocationManager and have to be done first

        //backupLocationManager.setTestProviderEnabled("gps", true); is that useful?
        if (backupLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                || backupLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
            // Check location service
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Check location permission
                backupLocationManager.requestLocationUpdates(provider, 1000, 0, new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                if (useBackupLocation) {
//                                    Toast.makeText(PKUMapActivity.this, "备用定位", Toast.LENGTH_SHORT).show();
                                    mMap.setMyLocationData(new MyLocationData.Builder()
                                            .latitude(location.getLatitude())
                                            .longitude(location.getLongitude())
                                            .build());
                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    approachPoi(latLng);
                                }
                            }
                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {}
                            @Override
                            public void onProviderEnabled(String provider) {}
                            @Override
                            public void onProviderDisabled(String provider) {}
                        }
                );
                //Location location = locationManager.getLastKnownLocation(provider); it's garbage
            } else {
                // The ACCESS_COARSE_LOCATION is denied, then I request it and manage the result in
                // onRequestPermissionsResult() using the constant MY_PERMISSION_ACCESS_FINE_LOCATION
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSION_ACCESS_COARSE_LOCATION);
                }
                // The ACCESS_FINE_LOCATION is denied, then I request it and manage the result in
                // onRequestPermissionsResult() using the constant MY_PERMISSION_ACCESS_FINE_LOCATION
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSION_ACCESS_FINE_LOCATION);
                }
                //TODO: What if denied?
            }
        } else {
            Toast.makeText(PKUMapActivity.this, "请先开启手机定位！", Toast.LENGTH_SHORT).show();
        }
    }

    private void onViewReady() {
        View bubbleViewApproach = getLayoutInflater().inflate(R.layout.view_popup_approach, null);
        tvApproach = (TextView) bubbleViewApproach.findViewById(R.id.tv_approach);
        tvApproach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //switch to IntroActivity
                Intent intent = new Intent(PKUMapActivity.this, IntroActivity.class);
                intent.putExtra("Poi", mNearestPoi);
                startActivityForResult(intent, MainActivity.CodeEnum.BTN_INTRO.ordinal());
            }
        });
        viewApproach.setBubbleView(bubbleViewApproach); // 设置气泡内容
        viewApproachClose = new Runnable() {
            @Override
            public void run() {
                MainActivity.handler.postDelayed(new Runnable() {// 设置线程，5秒自动关闭
                    @Override
                    public void run() {
                        if (viewApproach.isShowing()) {
                            viewApproach.dismiss();//无法在子线程中访问UI组件，UI组件的属性必须在UI线程中访问
                        }
                    }
                }, 5000);
            }
        };

        View bubbleViewQuery = getLayoutInflater().inflate(R.layout.view_popup_query, null);
        tvQuery = (TextView) bubbleViewQuery.findViewById(R.id.tv_query);
        tvQuery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //switch to IntroActivity
                Intent intent = new Intent(PKUMapActivity.this, IntroActivity.class);
                intent.putExtra("Poi", mNearestPoi);
                startActivityForResult(intent, MainActivity.CodeEnum.BTN_INTRO.ordinal());
            }
        });
        viewQuery.setBubbleView(bubbleViewQuery); // 设置气泡内容
        viewQueryClose = new Runnable() {
            @Override
            public void run() {
                MainActivity.handler.postDelayed(new Runnable() {// 设置线程，5秒自动关闭
                    @Override
                    public void run() {
                        if (viewQuery.isShowing()) {
                            viewQuery.dismiss();//无法在子线程中访问UI组件，UI组件的属性必须在UI线程中访问
                        }
                    }
                }, 5000);
            }
        };
    }

    private void onMapReady() {
        mMapView = (MapView) findViewById(R.id.map);
        mMap = mMapView.getMap();
        myNavigation = new MyNavigation(mMap);
        // Changing map type
        mMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);//MAP_TYPE_HYBRID; MAP_TYPE_SATELLITE; MAP_TYPE_TERRAIN; MAP_TYPE_NONE
        // Showing / hiding your current location
        mMap.setMyLocationEnabled(true);
        // Enable / Disable Compass icon
        mMap.getUiSettings().setCompassEnabled(true);
        // Enable / Disable gestures
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setOverlookingGesturesEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);

        mMap.setIndoorEnable(true);
        //mMap.showMapPoi(true);
        mMap.setMaxAndMinZoomLevel(20, 17);
        mMap.setMyLocationConfigeration(new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.NORMAL, true, null));//or COMPASS & FOLLOWING: always follow

        // PKU, Beijing: adb -s emulator-5554 emu geo fix 116.3180 39.9980
        mMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(MyNavigation.center));

        //if there's a click on the map, select POI
        mMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                MyPoiInfo info = MyPoiInfo.getNearestPoi(latLng);
                selectPoi(info, false);//info may be null
            }
            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
//                selectPoi(new MyPoiInfo(mapPoi), false); mapPoi may not be there in our database
                MyPoiInfo info = MyPoiInfo.getNearestPoi(mapPoi.getPosition());
                selectPoi(info, false);//info may be null
                return true;
            }
        });
    }

    private void onLocationPoiReady() {
        // -----------location config ------------
        locationService = ((LocationApplication) getApplication()).locationService;
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        locationService.registerListener(mListener);
        //注册监听
        locationService.setLocationOption(locationService.getDefaultLocationClientOption());
        // 定位SDK start之后会默认发起一次定位请求，开发者无须判断isstart并主动调用request
        locationService.start();
        //backup locationListener;
        beginBackupLocationListener();
    }
}