package com.famlinkup.androidpong;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class NXTMessenger implements Runnable
{
    //commands from phone to nxt
    private static final int MESSAGE_LOCATED_BALL = 1;
    private static final int MESSAGE_LOST_BALL = 2;
    private static final int MESSAGE_QUIT = 3;
    
    //commands from nxt to phone
    private static final int COMMAND_ROBOT_STARTED = 1;
    
    //robot states
    public static final int STATE_DISCONNECTED = 1;
    public static final int STATE_FOLLOW_BALL = 2;
    public static final int STATE_QUIT = 3;
    
    private static final String tag = "NXTMessenger";
    
    //STATE_FINDING_OBJECT vars
    boolean objectFound = false;
    int position = 0;
    long lastSeenObjectTs = 0;
    long lastStateSwitch = 0;
    
    //STATE_CAPTURED_OBJECT vars
    long capturedObjectTs = 0;
    
    private int state = STATE_DISCONNECTED;
    private DataInputStream is;
    private DataOutputStream os;
    private boolean connected;
    private int command;
    private SoundPlayer soundPlayer;
    private final MainActivity activity;
    
    public NXTMessenger(SoundPlayer soundPlayer, MainActivity activity)
    {
        this.soundPlayer = soundPlayer;
        this.activity = activity;
    }
    
    @Override
    public void run()
    {
        try
        {
            while(connected)
            {
                command = is.readByte();
                switch (command)
                {
                    case COMMAND_ROBOT_STARTED:
                        showToast("Robot started");
                        activity.setRotation(0);
                        //startingRotation = rawRotation;
                        setState(STATE_FOLLOW_BALL);
                        break;
                }
            }
        }
        catch (IOException ex)
        {
            Log.e("NXTMessenger", ex.getMessage(), ex);
            showToast("Disconnected");
        }
        catch (Exception ex)
        {
            Log.e("NXTMessenger", ex.getMessage(), ex);
            showToast(ex.getMessage());
            
        }
        showToast("Disconnected");
        connected = false; 
        setState(STATE_DISCONNECTED);
    }

    private void setState(int state)
    {
        if (this.state == state)
            return;
        this.state = state;
        lastStateSwitch = System.currentTimeMillis();
    }
    
    public boolean robotStarted()
    {
        return state != STATE_DISCONNECTED;
    }
    
    private void showToast(final String message)
    {
        activity.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public void connect(InputStream inputStream, OutputStream outputStream)
    {
        this.connected = true;
        this.is = new DataInputStream(inputStream);
        this.os = new DataOutputStream(outputStream);
        new Thread(this).start();
    }
    
    public void disconnect()
    {
        this.connected = false;
        setState(STATE_DISCONNECTED);
        showToast("Disconnected");
    }
    
    public boolean isConnected()
    {
        return this.connected;
    }
    
    public void sendMessage()
    {
        if (!connected)
            return;
        
        try
        {
        	int correction;
            switch (state)
            {
                
                case STATE_FOLLOW_BALL:
                    if (objectFound)
                    {
                        os.writeByte(MESSAGE_LOCATED_BALL);
                        os.writeShort(position);
                        os.flush();
                    }
                    else
                    {
                        os.writeByte(MESSAGE_LOST_BALL);
                        os.flush();
                    }
                    break;
                case STATE_QUIT:
                    os.writeByte(MESSAGE_QUIT);
                    os.flush();
                    
                    break;
                    
            }
        }
        catch(IOException ex)
        {
            Log.w(tag, "Can't write onto bluetooth stream: " + ex.getMessage());
        }
    }
   
    public int getState()
    {
        return state;
    }
    
    public void pongBallFound(int adjustment)
    {
        objectFound = true;
        this.position = adjustment;
        if (state == STATE_FOLLOW_BALL && System.currentTimeMillis() - lastSeenObjectTs > 1000) //this should go in NXTMessenger
            soundPlayer.locatedLegoBlock();
        lastSeenObjectTs = System.currentTimeMillis();
    }
    
    public void pingPongBallNotFound()
    {
        objectFound = false;
    }
    
    public void quit()
    {
        this.state = STATE_QUIT;
    }
}
