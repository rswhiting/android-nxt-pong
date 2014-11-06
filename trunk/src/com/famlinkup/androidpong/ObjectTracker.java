package com.famlinkup.androidpong;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ObjectTracker
{
    private Map<ObjectType, TargetObjects> objectTypeMap = new HashMap();
    
    public ObjectTracker()
    {
        
    }
    
    public ObjectTracker(Map<ObjectType, TargetObjects> objectTypeMap)
    {
        this.objectTypeMap = objectTypeMap;
    }
    
    public void removeAll()
    {
        objectTypeMap.clear();
    }
    
    public boolean hasObjectTypes()
    {
        return !objectTypeMap.isEmpty();
    }
    
    public void addTarget(ObjectType type, TargetColor targetColor)
    {
        TargetObjects targetObjects = objectTypeMap.get(type);
        if (targetObjects == null)
        {
            targetObjects = new TargetObjects();
            objectTypeMap.put(type, targetObjects);
        }
        targetObjects.addTargetColor(targetColor);
    }
    
    public TargetObjects getTargetObjects(ObjectType type)
    {
        return objectTypeMap.get(type);
    }
    
    public Collection<ObjectType> getObjectTypes()
    {
        return objectTypeMap.keySet();
    }
    
    public FoundObject getClosestObject()
    {
        FoundObject closestObject = null;
        for (TargetObjects targetObjects: objectTypeMap.values())
        {
            FoundObject object = targetObjects.getClosestObject();
            if (object == null)
                continue;
            
            if (closestObject == null || object.y > closestObject.y)
                closestObject = object;
                
        }
        return closestObject;
    }
    
    public JSONObject toJsonObject() throws JSONException
    {
        JSONObject jsonObject = new JSONObject();
        for (Entry<ObjectType, TargetObjects> entry : objectTypeMap.entrySet())
        {
            ObjectType objectType = entry.getKey();
            JSONArray jsonArray = entry.getValue().toJsonObject();
            jsonObject.put(objectType.name(), jsonArray);
        }
        return jsonObject;
    }
    
    /**
     * Have any target colors been defined yet?
     */
    public boolean hasTargetColors(ObjectType objectType)
    {
        TargetObjects targetObjects = getTargetObjects(objectType);
        return targetObjects == null ? false : !targetObjects.getTargetColors().isEmpty();
    }

    public FoundObject getClosestObject(ObjectType objectType)
    {
        TargetObjects targetObjects = objectTypeMap.get(objectType);
        if (targetObjects == null)
            return null;
        return targetObjects.getClosestObject();
    }
}
