package com.coolweather.android;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 *
 * Created by zj on 2017/1/18.
 */

public class WeatherActivity extends Activity {

    private TextView title;
    private TextView  title_update_time;
    private TextView degree_text;
    private TextView weather_info_text;
    private LinearLayout forecast_layout;
    private TextView aqi_text;
    private TextView pm25_text;
    private TextView comfort_text;
    private TextView car_wash_text;
    private TextView sport_text;
    private ScrollView sv_weather_layout;
    private ImageView imageView;
    private SharedPreferences preferences;
    public SwipeRefreshLayout swipeRefreshLayout;

    private String mWeatherId;
    public DrawerLayout drawer;
    private Button navButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_layout);
        if (Build.VERSION.SDK_INT>=21){
            View decorView=getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        initView();
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString= preferences.getString("weather",null);

        if (weatherString!=null){
            //有缓存时直接解析天气数据
            Weather weather= Utility.handleWeatherResponse(weatherString);
            mWeatherId =weather.basic.weatherId;
            showWeatherInfo(weather);
        }else{
            //无缓存时请求服务器查询天气
            mWeatherId=getIntent().getStringExtra("weather_id");
            sv_weather_layout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mWeatherId=getIntent().getStringExtra("weather_id");
                requestWeather(mWeatherId);
            }
        });
        String bingpic=  preferences.getString("bing_pic",null);
        if (bingpic!=null){
            Glide.with(this).load(bingpic).into(imageView);
        }else{
            loadBingpic();
        }
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(GravityCompat.START);
            }
        });

    }

    /**
     * 加载必应每日一图
     */
    private void loadBingpic(){
        String bingPicUrl="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpResquest(bingPicUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic=response.body().string();
                SharedPreferences.Editor edit=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                edit.putString("bing_pic",bingPic).apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(imageView);
                    }
                });
            }
        });
    }

    /**
     * 根据天qing气Id请求城市天气信息
     * @param weatherId
     */
    public void requestWeather(String weatherId) {

        Toast.makeText(WeatherActivity.this,"获取天气信息！",Toast.LENGTH_SHORT).show();
        String weatherUrl="http://guolin.tech/api/weather?cityid="+
                weatherId+"&key=a8f7c0e162634ad59827337f61a2e27f";
        HttpUtil.sendOkHttpResquest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败！",Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                  runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       Toast.makeText(WeatherActivity.this,"获取天气信息成功！",Toast.LENGTH_SHORT).show();
                       if (weather!=null&&"ok".equals(weather.status)){
                           SharedPreferences.Editor editor=PreferenceManager.
                                   getDefaultSharedPreferences(WeatherActivity.this).edit();
                           editor.putString("weather",responseText);
                           editor.apply();
                           showWeatherInfo(weather);
                       }
                       swipeRefreshLayout.setRefreshing(false);
                   }
               });
            }
        });
        loadBingpic();
    }


    /**
     * 显示天气信息，处理Weather中的实体类数据
     * @param weather
     */
    private void showWeatherInfo(Weather weather) {
        String cityName=weather.basic.cityName;
        String updateTime=weather.basic.update.updateTime.split(" ")[1];
        String degree=weather.now.temperature+"℃";
        String weatherInfo=weather.now.more.info;
        title.setText(cityName);
        title_update_time.setText(updateTime);
        degree_text.setText(degree);
        weather_info_text.setText(weatherInfo);
        forecast_layout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
          View view= LayoutInflater.from(this).inflate(R.layout.forecast_item,forecast_layout,false);
            TextView date_text= (TextView) view.findViewById(R.id.date_text);
            TextView info_text= (TextView) view.findViewById(R.id.info_text);
            TextView max_text= (TextView) view.findViewById(R.id.max_text);
            TextView min_text= (TextView) view.findViewById(R.id.min_text);
            date_text.setText(forecast.date);
            info_text.setText(forecast.more.info);
            max_text.setText(forecast.temperature.max);
            min_text.setText(forecast.temperature.min);
            forecast_layout.addView(view);
        }
        if (weather.aqi!=null){
            aqi_text.setText(weather.aqi.city.aqi);
            pm25_text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒  适  度:  "+weather.suggestion.comfort.info;
        String carWash = "洗车指数:  "+weather.suggestion.carWash.info;
        String  sport  = "运动建议:  "+weather.suggestion.sport.info;
        comfort_text.setText(comfort);
        car_wash_text.setText(carWash);
        sport_text.setText(sport);
        sv_weather_layout.setVisibility(View.VISIBLE);
    }

    /**
     * 初始化控件
     */
    private void initView() {
        drawer = (DrawerLayout) findViewById(R.id.drawer);
        navButton = (Button) findViewById(R.id.nav_button);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        imageView = (ImageView) findViewById(R.id.bing_pic_img);
        forecast_layout = (LinearLayout) findViewById(R.id.forecast_layout);
        sv_weather_layout = (ScrollView) findViewById(R.id.sv_weather_layout);

        title = (TextView) findViewById(R.id.title_city);
        title_update_time = (TextView) findViewById(R.id.title_update_time);
        degree_text = (TextView) findViewById(R.id.degree_text);
        weather_info_text = (TextView) findViewById(R.id.weather_info_text);

        aqi_text = (TextView) findViewById(R.id.aqi_text);
        pm25_text = (TextView) findViewById(R.id.pm25_text);

        comfort_text = (TextView) findViewById(R.id.comfort_text);
        car_wash_text = (TextView) findViewById(R.id.car_wash_text);
        sport_text = (TextView) findViewById(R.id.sport_text);
    }


    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)){
            drawer.closeDrawers();
        }else{
            finish();
        }
    }
}
