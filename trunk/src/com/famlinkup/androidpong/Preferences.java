package com.famlinkup.androidpong;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.core.Scalar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.widget.Toast;

public class Preferences
{
    private SharedPreferences prefs;
    private Context context;
    
    public Preferences(Context context)
    {
        this.context = context;
        this.prefs = context.getSharedPreferences("nxt", Context.MODE_PRIVATE);
    }
    
    // encoded as {"GOOD":[{"h":12,"s":27,"v":3}]}
    public ObjectTracker loadObjectTracker()
    {
        Map<ObjectType, TargetObjects> map = new HashMap();
        try
        {
            String objectTypeMapString = prefs.getString("objectTypeMap", "{}");
            JSONObject jsonMap = new JSONObject(objectTypeMapString);
            Iterator<String> objectTypeIterator = jsonMap.keys();
            while (objectTypeIterator.hasNext())
            {
                TargetObjects targetObjects = new TargetObjects();
                String key = objectTypeIterator.next();
                ObjectType objectType = ObjectType.valueOf(key);
                JSONArray jsonTargetColors = jsonMap.getJSONArray(key);
                
                for (int i=0; i<jsonTargetColors.length(); i++)
                {
                    JSONObject jsonTargetColor = jsonTargetColors.getJSONObject(i);
                    int h = jsonTargetColor.getInt("h");
                    int s = jsonTargetColor.getInt("s");
                    int v = jsonTargetColor.getInt("v");
                    int r = jsonTargetColor.getInt("r");
                    int g = jsonTargetColor.getInt("g");
                    int b = jsonTargetColor.getInt("b");
                    TargetColor targetColor = new TargetColor(new Scalar(h, s, v), new Scalar(r, g, b));
                    targetObjects.addTargetColor(targetColor);
                }
                map.put(objectType, targetObjects);
            }
        } 
        catch (Exception e)
        {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("Preferences", e.getMessage(), e);
        }
        
        ObjectTracker objectTracker = new ObjectTracker(map);
        return objectTracker; 
    }
    
    public void saveObjectTracker(ObjectTracker objectTracker)
    {
        try
        {
            JSONObject jsonObject = objectTracker.toJsonObject();
            String json = jsonObject.toString();
            Log.w("json", json);
            
            Editor editor = prefs.edit();
            editor.putString("objectTypeMap", json);
            editor.commit();
        }
        catch(Exception e)
        {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("Preferences", e.getMessage(), e);
        }
        
    }
    
    public int loadMinYThreshold()
    {
        return prefs.getInt("minYThreshold", 0);
    }
    
    public void saveMinYThreshold(int minYThreshold)
    {
        Editor editor = prefs.edit();
        editor.putInt("minYThreshold", minYThreshold);
        editor.commit();
    }
}
