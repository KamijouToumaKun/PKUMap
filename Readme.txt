项目名称：PKUMap，代表这是针对北大的一个地图、导游类app
作者：touma（我的英文名）



声明文件：PKUMap/app/src/main/AndroidManifest.xml
声明了所用的API服务、所需的权限、APP的activity



代码部分：PKUMap/app/src/main/java，代码总量约为2000+行
1 第三方提供的开源demo
1.1 com.baidu.location包，百度地图的定位服务
1.2 com.baidu.mapapi.overlayutil包，百度地图上显示景点标记、路线规划等
1.3 com.iflytek.speech包，科大讯飞提供的语音合成功能
1.4 com.yuyh.library包，GitHub上yuyh提供的自定义气泡控件

2 自己编写的部分
2.1 touma.pkumap包
2.1.1 IntroActivity类：景点简介界面的activity
2.1.2 MainActivity类：欢迎界面的activity
2.1.3 PKUMapActivity类：地图主界面的activity
2.1.4 RecognizeActivity类：景点识别界面的activity
2.1.5 SearchActivity类：搜索界面的activity

2.2 touma.pkumap.util包
2.2.1 HttpClientUtil.java：包含三个类，它们的功能是利用org.apache.http包，在http上建立安全连接，用于和APP服务器端通信
2.2.2 MyNavigation类：用于向百度服务器异步发起搜索请求
2.2.3 MyRecognition类：用于向APP服务器端异步发起图片识别请求
2.2.4 MyPoiInfo类：PoI是指Place of Interest，百度官方用这个词组来表示代表兴趣点/景点。这个类定义了一个景点包含的全部信息，并向APP服务器端异步发起对这些信息的请求

2.3 touma.pkumap.view包
2.3.1 PoiInfoView类：一个自定义的弹窗组件，用于显示景点的简略信息



资源部分：PKUMap/app/src/main/res
1 文件夹./drawable：存放有，地图图标文件（icon打头的png文件）、背景图片文件（pkumap.jpeg）、颜色配置文件（xml文件）等
2 文件夹./layout
2.1 activity_intro.xml：景点简介界面的activity的布局文件
2.2 activity_main.xml：欢迎界面的activity的布局文件
2.3 activity_pku_map.xml：地图主界面的activity的布局文件
2.4 activity_recognize.xml：景点识别界面的activity的布局文件
2.5 activity_search.xml：搜索界面的activity的布局文件
2.6 view_poi_info：PoiInfoView弹窗组件的布局文件
2.7 view_poi_approach：当靠近某个景点时、弹出来的气泡提示控件
2.8 view_poi_query：当搜索/景点识别返回结果时、弹出来的气泡提示控件



常量定义部分：PKUMap/app/src/main/values



第三方API的jar包：PKUMap/app/src/main/jniLibs