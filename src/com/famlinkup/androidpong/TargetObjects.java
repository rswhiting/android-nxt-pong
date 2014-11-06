package com.famlinkup.androidpong;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TargetObjects
{
    private List<TargetColor> targetColors = new ArrayList();
    private List<FoundObject> foundObjects = new ArrayList();
    private FoundObject closestObject;
    
    public void addTargetColor(TargetColor targetColor)
    {
        this.targetColors.add(targetColor);
    }
    
    public List<TargetColor> getTargetColors()
    {
        return targetColors;
    }
    
    public FoundObject getClosestObject()
    {
        return closestObject;
    }
    
    public void remove(TargetColor targetColor)
    {
        targetColors.remove(targetColor);
    }
    
    public JSONArray toJsonObject() throws JSONException
    {
        JSONArray jsonArray = new JSONArray();
        for (TargetColor targetColor : targetColors)
            jsonArray.put(targetColor.toJsonObject());

        return jsonArray;
    }
    
    public void setFoundObjects(List<FoundObject> foundObjects, int minYThreshold)
    {
        this.foundObjects = new ArrayList();
        for (FoundObject foundObject : foundObjects)
        {
            if (foundObject.y > minYThreshold)
                this.foundObjects.add(foundObject);
        }
        
        closestObject = null;
        for (FoundObject object : this.foundObjects)
        {
            if (closestObject == null || object.y > closestObject.y)
                closestObject = object;      
        }
    }
}
