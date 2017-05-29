package com.coolweather.android.util;

import android.text.TextUtils;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * Created jby z on 2017/1/17.
 */

public class Utility {

    public  static Weather handleWeatherResponse(String response){
        try {
            JSONObject jsonObject=new JSONObject(response);
            JSONArray jsonArray=jsonObject.getJSONArray("HeWeather");
            String weatherContent= jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent,Weather.class);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析和处理服务器返回的省级数据
     */
    public static boolean handProvinceResponse(String response){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allProvinces=new JSONArray(response);
                for (int i=0;i<allProvinces.length();i++){
                    JSONObject provinceObject =allProvinces.getJSONObject(i);
                    Province province=new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return false;
    }


    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean handCityResponse(String response,int provinceId){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allCitys=new JSONArray(response);
                for (int i=0;i<allCitys.length();i++){
                    JSONObject cityObject =allCitys.getJSONObject(i);
                    City city =new City();
                    city.setCityCode(cityObject.getInt("id"));
                    city.setCityName(cityObject.getString("name"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return false;
    }



    /**
     * 解析和处理服务器返回的城市数据
     */
    public static boolean handCountryponse(String response,int cityId){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allCountrys=new JSONArray(response);
                for (int i=0;i<allCountrys.length();i++){
                    JSONObject countyObject =allCountrys.getJSONObject(i);
                    County county =new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return false;
    }
}
