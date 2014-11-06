package com.famlinkup.androidpong;

import java.io.IOException;
import java.util.Random;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

public class SoundPlayer
{
    private final MediaPlayer mp = new MediaPlayer();
    private Context context;
    private Random random = new Random();
    
    public SoundPlayer(Context context)
    {
        this.context = context;
        mp.setVolume(1, 1);
    }
    
    private void playSound(String filename)
    {
        if(mp.isPlaying())
        {  
            mp.stop(); 
        }
        mp.reset();
        try 
        {

            AssetFileDescriptor afd;
            afd = context.getAssets().openFd(filename);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mp.prepare();
            mp.start();
        } 
        catch (IllegalStateException e) 
        {
            Log.e("SoundPlayer", e.getMessage(), e);
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        } 
        catch (IOException e) 
        {
            Log.e("SoundPlayer", e.getMessage(), e);
            Toast.makeText(context, "Couldn't find file " + filename, Toast.LENGTH_SHORT).show();
        }
    }
    
    public void connectingToBluetooth()
    {
        playSound("ConnectingToBluetooth.mp3"); 
    }
    
    public void returningHome()
    {
        playSound("ReturningHome.mp3");
    }
    
    public void connected()
    {
        playSound("Connected.mp3"); 
    }
    
    public void lookingForLegoBlock()
    {
        playSound("LookingForLegoBlocks.mp3");
    }
    
    public void blockCaptured()
    {
        playSound("BlockCaptured.mp3");
    }
    
    public void locatedLegoBlock()
    {
        int num = random.nextInt(3);
        switch (num)
        {
            case 0: playSound("LocatedLegoBlock.mp3"); break;
            case 1: playSound("HaISeeYou.mp3"); break;
            case 2: playSound("FoundOne.mp3"); break;
        }
    }
}
