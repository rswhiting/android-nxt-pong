package com.famlinkup.androidpong;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Scalar;

public class TargetColor
{
    private Scalar hsvLowerBound;
    private Scalar hsvUpperBound;
    private Scalar hsv;
    private Scalar rgb;
    private List<FoundObject> foundObjects;
    private FoundObject closestObject;
    
    public TargetColor(Scalar hsv, Scalar rgb)
    {
        this.hsv = hsv;
        this.rgb = rgb;
        double low = hsv.val[0] - 10;
        if (low < 0)
            low = 0;
        double high = hsv.val[0] + 10;
        if (high > 179)
            high = 179;
        
        int[] hueTolerance = getTolerance((int)hsv.val[0], 6, 6, 180);
        int[] satTolerance = getTolerance((int)hsv.val[1], 50, 255, 255);
        int[] valTolerance = getTolerance((int)hsv.val[2], 50, 50, 255);
        
        hsvLowerBound = new Scalar(hueTolerance[0], satTolerance[0], valTolerance[0]);
        hsvUpperBound = new Scalar(hueTolerance[1], satTolerance[1], valTolerance[1]);
    }
    
    public JSONObject toJsonObject() throws JSONException
    {
        JSONObject json = new JSONObject();
        json.put("h", (int)hsv.val[0]);
        json.put("s", (int)hsv.val[1]);
        json.put("v", (int)hsv.val[2]);
        json.put("r", (int)rgb.val[0]);
        json.put("g", (int)rgb.val[1]);
        json.put("b", (int)rgb.val[2]);
        return json;
    }
    
    public Scalar getHsv()
    {
        return hsv;
    }
    
    public Scalar getRgb()
    {
        return rgb;
    }
    
    public Scalar getLowerBound()
    {
        return hsvLowerBound;
    }
    
    public Scalar getUpperBound()
    {
        return hsvUpperBound;
    }
    
    private int[] getTolerance(int val, int lowerTolerance, int higherTolerance, int higherBound)
    {
        int lower = val - lowerTolerance;
        if (lower < 0)
            lower = 0;
        int higher = val + higherTolerance;
        if (higher > higherBound)
            higher = higherBound;
        return new int[] {lower, higher};
    }
    
}
