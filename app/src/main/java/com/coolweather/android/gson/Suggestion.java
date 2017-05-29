package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 *
 * Created by zj on 2017/1/18.
 */
public class Suggestion {

    @SerializedName("comf")
    public  Comfort comfort;

    @SerializedName("sport")
    public Sport sport;

    @SerializedName("cw")
    public CarWash carWash ;

    public class Sport{
        @SerializedName("txt")
        public String info;
    }

    public class CarWash{
        @SerializedName("txt")
        public String info;
    }

    public class Comfort{
        @SerializedName("txt")
        public String info;
    }
}
