package touma.pkumap;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.baidu.location.Poi;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiBoundSearchOption;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import touma.pkumap.util.MyNavigation;
import touma.pkumap.util.MyPoiInfo;

/**
 * Created by apple on 2017/11/1.
 */

public class SearchActivity extends AppCompatActivity implements OnGetPoiSearchResultListener, OnGetSuggestionResultListener {

    private final String citystr = "北京";
    private PoiSearch mPoiSearch = null;
    private SuggestionSearch mSuggestionSearch = null;
    private List<String> suggest;
    /**
     * 搜索关键字输入窗口
     */
    private AutoCompleteTextView keyWorldsView = null;
    private ArrayAdapter<String> sugAdapter = null;
    private int loadIndex = 0;

    ArrayList<Map<String, Object>> mData = new ArrayList<Map<String, Object>>();
    SimpleAdapter listViewAdapter;
        //android.R belongs to android, R belongs to me.

    int radius = 500;
    LatLngBounds searchbound = new LatLngBounds.Builder().include(MyNavigation.southwest).include(MyNavigation.northeast).build();

    int searchType = 0;  // 搜索的类型，在显示时区分

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(this);

        // 初始化建议搜索模块，注册建议搜索事件监听
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(this);
        ListView mListView = (ListView) findViewById(R.id.search_lv);

        Map<String, Object> item = new HashMap<String, Object>();//must load an item here
        item.put("Title", "");
        item.put("Text", "");
        mData.add(item);
        listViewAdapter = new SimpleAdapter(SearchActivity.this, mData, android.R.layout.simple_list_item_2,
                new String[]{"Title", "Text"}, new int[]{android.R.id.text1, android.R.id.text2});
        //android.R != R
        mListView.setAdapter(listViewAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Intent intent = new Intent();
                Object res = mData.get(arg2).get("Poi");
                if (res instanceof MyPoiInfo) {
                    intent.putExtra("Poi", (MyPoiInfo) res);
                }
                setResult(MainActivity.CodeEnum.LISTVIEW.ordinal(), intent);
                finish();
            }
        });

        Button btn_city = (Button) findViewById(R.id.btn_city);
        btn_city.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchButtonProcess();
            }
        });

        Button btn_nearby = (Button) findViewById(R.id.btn_nearby);
        btn_nearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchNearbyProcess();
            }
        });

        Button btn_bound = (Button) findViewById(R.id.btn_bound);
        btn_bound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBoundProcess();
            }
        });

        keyWorldsView = (AutoCompleteTextView) findViewById(R.id.searchkey);
        sugAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line);
        keyWorldsView.setAdapter(sugAdapter);
        keyWorldsView.setThreshold(1);
        /**
         * 当输入关键字变化时，动态更新建议列表
         */
        keyWorldsView.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {

            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {

            }

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2,
                                      int arg3) {
                if (cs.length() <= 0) {
                    return;
                }

                /**
                 * 使用建议搜索服务获取建议列表，结果在onSuggestionResult()中更新
                 */
                //confine the searching area
                String keystr = cs.toString();
                /*if (!keystr.contains("北大") || !keystr.contains("北京大学")) {
                    keystr = "北京大学" + keystr;
                }*/
                mSuggestionSearch
                        .requestSuggestion((new SuggestionSearchOption())
                                .keyword(keystr).city(citystr));
            }
        });

        Button btn_autocomplete = (Button) findViewById(R.id.btn_autocomplete);
        btn_autocomplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyWorldsView.dismissDropDown();
            }
        });
    }

    @Override
    protected void onDestroy() {
        mPoiSearch.destroy();
        mSuggestionSearch.destroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(MainActivity.CodeEnum.LISTVIEW.ordinal(), intent);
        super.onBackPressed();
    }

    /**
     * 响应城市内搜索按钮点击事件: now it's used to search for custom poi by name
     *
     * @param
     */
    public void searchButtonProcess() {
        searchType = 1;
        String keystr = keyWorldsView.getText().toString();
        /*mPoiSearch.searchInCity((new PoiCitySearchOption())
                .city(citystr).keyword(keystr).pageNum(loadIndex));
                */
        MyPoiInfo poi = new MyPoiInfo(keystr);
        if (poi.intro != "") {
            mData.clear();
            Map<String, Object> item = new HashMap<>();
            item.put("Title", poi.name);
            item.put("Text", poi.intro);
            item.put("Poi", poi);
            mData.add(item);
            listViewAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(SearchActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }

        /*TODO: sometimes crash
        MessageQueue-JNI: java.lang.IllegalStateException: The content of the adapter has changed but ListView did not receive a notification.
        Make sure the content of your adapter is not modified from a background thread, but only from the UI thread.
        Make sure your adapter calls notifyDataSetChanged() when its content changes.
        [in ListView(2131558515, class android.widget.ListView) with Adapter(class android.widget.SimpleAdapter)]*/
    }

    /**
     * 响应周边搜索按钮点击事件
     *
     * @param
     */
    public void searchNearbyProcess() {
        searchType = 2;
        PoiNearbySearchOption nearbySearchOption = new PoiNearbySearchOption().keyword(keyWorldsView.getText()
                .toString()).sortType(PoiSortType.distance_from_near_to_far).location(MyNavigation.center)
                .radius(radius).pageNum(loadIndex);
        mPoiSearch.searchNearby(nearbySearchOption);
    }

    public void goToNextPage() {
        loadIndex++;
        searchButtonProcess();
    }

    /**
     * 响应区域搜索按钮点击事件
     *
     * @param
     */
    public void searchBoundProcess() {
        searchType = 3;
        mPoiSearch.searchInBound(new PoiBoundSearchOption().bound(searchbound)
                .keyword(keyWorldsView.getText().toString()));

    }

    /**
     * 获取POI搜索结果，包括searchInCity，searchNearby，searchInBound返回的搜索结果
     * @param result
     */
    public void onGetPoiResult(PoiResult result) {
        if (result == null || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
            Toast.makeText(SearchActivity.this, "抱歉，未找到结果", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            List<PoiInfo> poiInfos = result.getAllPoi();
            mData.clear();
            for (PoiInfo info:poiInfos) {
                MyPoiInfo poi = new MyPoiInfo(info);
                if (poi != null && poi.name != null) {
                    Map<String,Object> item = new HashMap<>();
                    item.put("Title",  info.name);
                    item.put("Text",  info.address);
                    item.put("Poi", poi);
                    mData.add(item);
                }
            }
            listViewAdapter.notifyDataSetChanged();
            return;
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {

            // 当输入关键字在本市没有找到，但在其他城市找到时，返回包含该关键字信息的城市列表

        }
    }

    /**
     * 获取POI详情搜索结果，得到searchPoiDetail返回的搜索结果
     * @param result
     */
    public void onGetPoiDetailResult(PoiDetailResult result) {
        if (result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(SearchActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText(SearchActivity.this, result.getName() + ": " + result.getAddress(), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

    }

    /**
     * 获取在线建议搜索结果，得到requestSuggestion返回的搜索结果
     * @param res
     */
    @Override
    public void onGetSuggestionResult(SuggestionResult res) {
        if (res == null || res.getAllSuggestions() == null) {
            return;
        }
        suggest = new ArrayList<String>();
        int count = 0;
        for (SuggestionResult.SuggestionInfo info : res.getAllSuggestions()) {
            if (info.key != null) {
                suggest.add(info.key);
                count ++;
                if (count >= 3) {
                    break;
                }
            }
        }
        sugAdapter = new ArrayAdapter<String>(SearchActivity.this, android.R.layout.simple_dropdown_item_1line, suggest);
        keyWorldsView.setAdapter(sugAdapter);
        sugAdapter.notifyDataSetChanged();
    }
}