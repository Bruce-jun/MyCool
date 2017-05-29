package com.coolweather.android.fragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.MainActivity;
import com.coolweather.android.R;
import com.coolweather.android.WeatherActivity;
import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 *
 * Created by zj on 2017/1/17.
 */


public class ChooseAreaFragment extends Fragment {


    private List<String> dataList=new ArrayList<>();

    private List<Province> provincesList;
    private List<City> citysList;
    private List<County> countysList;

    private Province selectProvince;//选中的省
    private County selectCounty;//选中的城市
    private City selectCity;//选中的市

    private int currentLevel;//当前选中的级别

    public static  final int LEVEL_PROVINCE=0;
    public static  final int LEVEL_CITY=1;
    public static  final int LEVEL_COUNTY=2;



    private TextView mTitleText;
    private Button mBackBtn;
    private ListView listView;
    private ArrayAdapter<String> arrayAdapter;
    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area_layout,container,false);
        mTitleText = (TextView) view.findViewById(R.id.title_text);
        mBackBtn = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.lv);
        arrayAdapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(arrayAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel==LEVEL_PROVINCE){
                    selectProvince=provincesList.get(position);
                    queryCities();
                }else if (currentLevel==LEVEL_CITY){
                    selectCity=citysList.get(position);
                    queryCounties();
                }else if (currentLevel==LEVEL_COUNTY){
                    String weatherId=countysList.get(position).getWeatherId();
                    if(getActivity() instanceof MainActivity){
                        Intent intent=new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity weather= (WeatherActivity) getActivity();
                        weather.drawer.closeDrawers();
                        weather.swipeRefreshLayout.setRefreshing(true);
                        weather.requestWeather(weatherId);
                    }

                }
            }
        });
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
           public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    queryProvince();
                }
            }
        });
        queryProvince();
    }
    /**
     * 查询全国所有的省，优先从据库中查询，没有再请求服务器
     */
    private void queryProvince() {
        mTitleText.setText("中国");
        mBackBtn.setVisibility(View.GONE);
        provincesList= DataSupport.findAll(Province.class);
        if (provincesList.size()>0){
            dataList.clear();
            for (Province province:provincesList) {
               dataList.add(province.getProvinceName());
            }
            arrayAdapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            String address="http://guolin.tech/api/china";
            queryFormServer(address,"province");
        }
    }
    /**
     * 查询省份所对应的市，优先从数据库中查询，没有再请求服务器
     */
    private void queryCities() {
        mTitleText.setText(selectProvince.getProvinceName());
        mBackBtn.setVisibility(View.VISIBLE);
        citysList=DataSupport.where("provinceid=?",String.valueOf(selectProvince.getId())).find(City.class);
        if (citysList.size()>0){
            dataList.clear();
            for (City city:citysList) {
              dataList.add(city.getCityName());
            }
            arrayAdapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode=selectProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;
            queryFormServer(address,"city");
        }
    }



    /**
     * 查询全国所有的市所对应的城市，优先从数据库中查询，没有再请求服务器
     */
    private void queryCounties() {
        mTitleText.setText(selectCity.getCityName());
        mBackBtn.setVisibility(View.VISIBLE);
        countysList=DataSupport.where("cityid=?",String.valueOf(selectCity.getId())).find(County.class);
        if (countysList.size()>0){
            dataList.clear();
            for (County county:countysList) {
                dataList.add(county.getCountyName());
            }
            arrayAdapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else {
            int provinceCode=selectProvince.getProvinceCode();
            int cityCode=selectCity.getCityCode();
            String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFormServer(address,"county");
        }

    }


    private void queryFormServer(String address,final String level) {
        showProgerssDialog();
        HttpUtil.sendOkHttpResquest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(),"加载失败！",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if ("province".equals(level)){
                    result= Utility.handProvinceResponse(responseText);
                }else if ("city".equals(level)){
                    result= Utility.handCityResponse(responseText,selectProvince.getId());
                }else if ("county".equals(level)){
                    result= Utility.handCountryponse(responseText,selectCity.getId());
                }
                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();//关闭对话框
                            if ("province".equals(level)){
                               queryProvince();
                            }else if ("city".equals(level)){
                                queryCities();
                            }else if ("county".equals(level)){
                                queryCounties();
                            }
                        }
                    });
                }
            }



        });
    }

    private void showProgerssDialog() {
        if (progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if (progressDialog!=null){
            progressDialog.dismiss();
        }
    }

}
