package touma.pkumap.util;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;

import java.io.Serializable;

/**
 * Created by apple on 2017/11/4.
 */

//It needs authority to use map directly
public class MyNavigation {

    public static final LatLng center = new LatLng(39.9980, 116.3180);
    public static final LatLng southwest = new LatLng(39.9913, 116.3111);
    public static final LatLng northeast = new LatLng(40.0058, 116.3285);
    private RoutePlanSearch mSearch;
    private OnGetRoutePlanResultListener listener;
    private WalkingRouteOverlay overlay = null;

    public MyNavigation(BaiduMap baiduMap) {
        final BaiduMap mMap = baiduMap;
        //创建步行线路规划检索监听者；
        listener = new OnGetRoutePlanResultListener() {

            @Override
            public void onGetDrivingRouteResult(DrivingRouteResult arg0) {

            }

            @Override
            public void onGetTransitRouteResult(TransitRouteResult arg0) {

            }

            @Override
            public void onGetWalkingRouteResult(WalkingRouteResult result) {
                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                    //抱歉，未找到结果
                }
                else if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
                    // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
                    // result.getSuggestAddrInfo()
                }
                else if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                    overlay = new WalkingRouteOverlay(mMap);
                    overlay.setData(result.getRouteLines().get(0));
                    overlay.addToMap(); // 将所有Overlay 添加到地图上
                    overlay.zoomToSpan(); // 缩放地图，使所有Overlay都在合适的视野内 注：
                    // 该方法只对Marker类型的overlay有效
                }
            }

            @Override
            public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

            }

            @Override
            public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

            }

            @Override
            public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

            }
        };
        overlay = new WalkingRouteOverlay(mMap);
    }

    public void beginNavigation(MyPoiInfo srcPoi, MyPoiInfo destPoi) {
        //创建步行线路规划检索实例；
        mSearch = RoutePlanSearch.newInstance();
        //设置步行线路规划检索监听者；
        mSearch.setOnGetRoutePlanResultListener(listener);
        //准备检索起、终点信息；
//        PlanNode stNode = PlanNode.withCityNameAndPlaceName("北京", name1);
//        PlanNode enNode = PlanNode.withCityNameAndPlaceName("北京", name2);
        PlanNode stNode = PlanNode.withLocation(new LatLng(srcPoi.latitude, srcPoi.longitude));
        PlanNode enNode = PlanNode.withLocation(new LatLng(destPoi.latitude, destPoi.longitude));

        //发起步行线路规划检索；
        mSearch.walkingSearch((new WalkingRoutePlanOption())
                .from(stNode)
                .to(enNode));
    }

    public void endNavigation() {
        if (overlay != null) {
            overlay.removeFromMap();
        }
        mSearch.destroy();
    }
}
