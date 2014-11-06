package com.famlinkup.androidpong;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements CvCameraViewListener2, SensorEventListener
{
    private static final String TAG = "OCVSample::Activity";
    private static final int WIDTH = 800;
    private static final int HEIGHT = 480;
    private static String tag = "MainActivity";
    private Preferences prefs;
    private UncaughtExceptionHandler defaultUEH;
    private SensorManager sensorManager;
    private long lastSensorTime;
    private double rotation = 0;
    private double prevNormX = 0;
    private double prevNormY = 0;
    private long lastLocatedTime = 0;
    
    //sensor data
    float[] gData = new float[3];           // Gravity or accelerometer
    float[] mData = new float[3];           // Magnetometer
    float[] orientation = new float[3];
    float[] Rmat = new float[9];
    float[] R2 = new float[9];
    float[] Imat = new float[9];
    boolean haveGrav = false;
    boolean haveAccel = false;
    boolean haveMag = false;

    //state
    private static final int STATE_CHOOSE_COLOR = 1;
    private static final int STATE_RUN = 2;
    private static final int STATE_VIEWFINDER = 3; //turn the camera back on
    
    //mats
    private Mat rgba;
    private Mat mHSV;
    private Mat mHSVThreshed;
    private Mat unionedMask;
    
    //menu items
    private MenuItem miConnectBluetooth = null;
    
    //UI vars
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private BroadcastReceiver    btMonitor = null;
    private GestureDetector gestureDetector;
    private SoundPlayer soundPlayer;
    private int minYThreshold = 100;
    private Dialog dialog;
    
    //other vars
    private int cameraState = STATE_VIEWFINDER;
    private ObjectType callibratingType;
    private ObjectTracker objectTracker = new ObjectTracker();
    
    //bluetooth vars
    final String ROBOTNAME = "NXT";
    private BluetoothAdapter btInterface;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothSocket socket;
    private NXTMessenger messenger;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    
    private class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
	 {
	     @Override
	     public void uncaughtException(final Thread thread, final Throwable ex) 
	     {
	    	 StringWriter sw = new StringWriter();
	    	 PrintWriter pw = new PrintWriter(sw);
	    	 ex.printStackTrace(pw);
	    	 final String stacktrace = sw.toString(); // stack trace as a string

	    	 runOnUiThread(new Runnable() {  

	    	        public void run() {  
	    	        	//Toast.makeText(getApplicationContext(), stacktrace, Toast.LENGTH_LONG).show();
	    	        	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
	    	        	AlertDialog dialog = alertDialogBuilder.setTitle("Error").setMessage(stacktrace)
	    	        			.setPositiveButton("Ok", new DialogInterface.OnClickListener() 
	    	        			{
									@Override
									public void onClick(DialogInterface dialog, int which) {
										defaultUEH.uncaughtException(thread, ex);
										System.exit(2);
										
									}
								}).create();
	    	        	dialog.show();
	    	        	TextView textView = (TextView) dialog.findViewById(android.R.id.message);
	    	        	textView.setTextSize(12);
	    	        }  
	    	    }); 
	    	 
	     }
	 };

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        gestureDetector = new GestureDetector(this,new GestureListener());
        soundPlayer = new SoundPlayer(this);
        messenger = new NXTMessenger(soundPlayer, this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        setupBTMonitor();
        setContentView(R.layout.camera);
        
        //load any saved colors
        this.prefs = new Preferences(this);
        objectTracker = prefs.loadObjectTracker();
        minYThreshold = prefs.loadMinYThreshold();

        if (mIsJavaCamera)
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        else
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);

        
        mOpenCvCameraView.setMaxFrameSize(WIDTH, HEIGHT);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
        mOpenCvCameraView.setOnTouchListener(new OnTouchListener()
        {
            
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });
    }
    
    
    private class GestureListener extends GestureDetector.SimpleOnGestureListener
    {

        @Override
        public boolean onDown(MotionEvent event) 
        {
            if (cameraState != STATE_CHOOSE_COLOR)
            {
                if (event.getY() < 100)
                    showDialog();
                
            }
            
            Log.w("touch", "(" + event.getX() + ", " + event.getY() + ")");
            
            int matCols = rgba.cols();
            int matRows = rgba.rows();

            int x = (int)((event.getX() / mOpenCvCameraView.getWidth()) * (double) matCols);
            int y = (int)((event.getY() / mOpenCvCameraView.getHeight()) * (double) matRows);

            if (cameraState != STATE_CHOOSE_COLOR)
                return false;
            

            Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

            if ((x < 0) || (y < 0) || (x > matCols) || (y > matRows)) return false;

            Rect touchedRect = new Rect();

            int samplingWidth = 4;
            touchedRect.x = (x>samplingWidth) ? x-samplingWidth : 0;
            touchedRect.y = (y>samplingWidth) ? y-samplingWidth : 0;

            touchedRect.width = (x+samplingWidth < matCols) ? x + samplingWidth - touchedRect.x : matCols - touchedRect.x;
            touchedRect.height = (y+samplingWidth < matRows) ? y + samplingWidth - touchedRect.y : matRows - touchedRect.y;

            Mat touchedRegionRgba = rgba.submat(touchedRect);

            Mat touchedRegionHsv = new Mat();
            Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_BGR2HSV);

            // Calculate average color of touched region
            Scalar selectedHsv = Core.sumElems(touchedRegionHsv);
            Scalar selectedRgb = Core.sumElems(touchedRegionRgba);
            int pointCount = touchedRect.width*touchedRect.height;
            for (int i = 0; i < selectedHsv.val.length; i++)
            {
                selectedHsv.val[i] /= pointCount;
                selectedRgb.val[i] /= pointCount;
            }
            
            Log.w("Selected HSV", selectedHsv.val[0] + ", " + selectedHsv.val[1] + ", " + selectedHsv.val[2] + ", ");
            objectTracker.addTarget(callibratingType, new TargetColor(selectedHsv, selectedRgb));
            prefs.saveObjectTracker(objectTracker);
            
            touchedRegionRgba.release();
            touchedRegionHsv.release();
            
            cameraState = STATE_RUN;

            return true; // don't need subsequent touch events
        }
        
        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) 
        {
            if (cameraState == STATE_RUN)
                cameraState = STATE_VIEWFINDER;
            else if(cameraState == STATE_VIEWFINDER)
                cameraState = STATE_RUN;
            
            return true;
        }
    }
    
    private void showDialog()
    {
        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog);
        
        //add colors
        LinearLayout layoutColors = (LinearLayout) dialog.findViewById(R.id.layoutColors);
        layoutColors.removeAllViews();
        for (final ObjectType objectType : ObjectType.values())
        {
            final LinearLayout horizLayout = new LinearLayout(this);
            horizLayout.setPadding(8, 1, 8, 1);
            horizLayout.setOrientation(LinearLayout.HORIZONTAL);
            layoutColors.addView(horizLayout);
            
            //add object type name
            TextView txtObjectType = new TextView(this);
            txtObjectType.setWidth(100);
            txtObjectType.setText(objectType.name() + ":");
            horizLayout.addView(txtObjectType);
            
            int i = 1;
            int buttonWidth = 75;
            final TargetObjects targetObjects = objectTracker.getTargetObjects(objectType);
            if (targetObjects != null)
            {
                for (final TargetColor targetColor : targetObjects.getTargetColors())
                {
                    final Button button = new Button(this);
                    button.setWidth(buttonWidth);
                    Scalar rgb = targetColor.getRgb();
                    button.setText(String.valueOf(i));
                    button.setTextColor(Color.WHITE);
                    button.setBackgroundColor(Color.rgb((int)rgb.val[0], (int)rgb.val[1], (int)rgb.val[2]));
                    button.setOnClickListener(new OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage("Would you like to replace or remove this color?")
                                .setPositiveButton("Replace", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface d, int id) {
                                        targetObjects.remove(targetColor);
                                        callibratingType = objectType;
                                        cameraState = STATE_CHOOSE_COLOR;
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("Remove", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        targetObjects.remove(targetColor);
                                        horizLayout.removeView(button);
                                    }
                                }).create().show();
                        }
                    });
                    horizLayout.addView(button);
                    i++;
                }
            }
            
            Button btnAdd = new Button(this);
            btnAdd.setWidth(buttonWidth);
            btnAdd.setText("+");
            btnAdd.setBackgroundColor(Color.WHITE);
            btnAdd.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    callibratingType = objectType;
                    cameraState = STATE_CHOOSE_COLOR;
                    dialog.dismiss();
                }
            });
            horizLayout.addView(btnAdd);
            
        }
        
        //add remove all button
        Button btnRemoveAll = (Button) dialog.findViewById(R.id.btnRemoveAll);
        btnRemoveAll.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                objectTracker.removeAll();
                prefs.saveObjectTracker(objectTracker);
                cameraState = STATE_VIEWFINDER;
                dialog.dismiss();
            }
        });
        
        //connect bluetooth
        Button btnConnectBluetooth = (Button) dialog.findViewById(R.id.btnConnectBluetooth);
        btnConnectBluetooth.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                soundPlayer.connectingToBluetooth();
                BluetoothDevice bluetoothDevice = findRobot();
                connectToRobot(bluetoothDevice);
            }
        });
        
        //init seekbar
        SeekBar seekbar = (SeekBar) dialog.findViewById(R.id.seekBar1);
        seekbar.setProgress(minYThreshold);
        seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                minYThreshold = progress;
                prefs.saveMinYThreshold(minYThreshold);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                prefs.saveMinYThreshold(minYThreshold);
                
            }
            
        });
        
        Window window = dialog.getWindow();
        window.setLayout(800, 600);
        dialog.show();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        messenger.quit();
        
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        
        registerReceiver(btMonitor, new IntentFilter("android.bluetooth.device.action.ACL_CONNECTED"));
        registerReceiver(btMonitor, new IntentFilter("android.bluetooth.device.action.ACL_DISCONNECTED"));
        
        Sensor rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void onDestroy() {
        super.onDestroy();
        messenger.quit();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
        Log.i(TAG, "called onCreateOptionsMenu");
        miConnectBluetooth.setTitle(messenger.isConnected() ? "Disconnect" : "Connect");
        return true;
    }
    
    private void setupBTMonitor()
    {
        btMonitor = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent.getAction().equals("android.bluetooth.device.action.ACL_CONNECTED"))
                {
                    handleConnected();
                }
                if (intent.getAction().equals("android.bluetooth.device.action.ACL_DISCONNECTED"))
                {
                    handleDisconnected();
                }

            }
        };

    }
    
    public BluetoothDevice findRobot()
    {
        try
        {
            btInterface = BluetoothAdapter.getDefaultAdapter();
            if (!btInterface.isEnabled())
            {
                btInterface.enable();
                while (!(btInterface.isEnabled()))
                {
                    
                }
                Toast.makeText(MainActivity.this, "Bluetooth turned on", Toast.LENGTH_LONG).show();
            }
            
            pairedDevices = btInterface.getBondedDevices();
            Log.i(tag, "Found [" + pairedDevices.size() + "] devices.");
            Iterator<BluetoothDevice> it = pairedDevices.iterator();
            while (it.hasNext())
            {
                BluetoothDevice bd = it.next();
                if (bd.getName().equalsIgnoreCase(ROBOTNAME))
                {
                    Log.i(tag, "Found Robot!");
                    Log.i(tag, bd.getAddress());
                    Log.i(tag, bd.getBluetoothClass().toString());
                    return bd;
                }
            }
        } 
        catch (Exception e)
        {
            Log.e(tag, "Failed in findRobot(): " + e.getMessage());
        }
        return null;
    }
    
    private void connectToRobot(BluetoothDevice bd)
    {
        try
        {
            socket = bd.createRfcommSocketToServiceRecord(java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            socket.connect();
        } 
        catch (IOException e)
        {
            Toast.makeText(MainActivity.this, "NXT not available", Toast.LENGTH_LONG).show();
            Log.e(tag, "Error connecting to robot: " + e.getMessage());
        } 
    }
    
    //called when bluetooth has been connected to peer
    private void handleConnected()
    {
        try
        {
            soundPlayer.connected();
            messenger.connect(socket.getInputStream(), socket.getOutputStream());
            Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_LONG).show();
            cameraState = STATE_RUN;
            if (dialog != null && dialog.isShowing())
                dialog.dismiss();
        } 
        catch (Exception e)
        {
            disconnectFromRobot();
        }
    }

    public void disconnectFromRobot()
    {
        try
        {
            Log.i(tag, "Attempting to break BT connection");
            messenger.quit();
            socket.close();
        } catch (Exception e)
        {
            Log.e(tag, "Error in DoDisconnect [" + e.getMessage() + "]");
        }
    }
    
    private void handleDisconnected()
    {
        messenger.disconnect();
    }

    public void onCameraViewStarted(int width, int height) {
        rgba = new Mat();
        mHSV = new Mat();
        mHSVThreshed = new Mat();
    }

    public void onCameraViewStopped() {
    }
    
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) 
    {
        //4 channels, rows 480, cols 800
        rgba = inputFrame.rgba();
        if (cameraState == STATE_CHOOSE_COLOR || !objectTracker.hasObjectTypes() || cameraState == STATE_VIEWFINDER)
        {
           return rgba;
        }
 
        if (unionedMask == null)
        	unionedMask = new Mat(rgba.height(), rgba.width(), CvType.CV_8UC1, new Scalar(0));
        unionedMask.setTo(new Scalar(0)); //0 ms
       
        //convert it to HSV color space (3 channels)
        Imgproc.cvtColor(rgba, mHSV, Imgproc.COLOR_BGR2HSV, 0); //about 20 ms
        
        //only keep the colors that are the hue we want
        ObjectType objectType = ObjectType.BALL;
        TargetObjects targetObjects = objectTracker.getTargetObjects(objectType);
        if (targetObjects != null)
        {
            for (TargetColor targetColor : targetObjects.getTargetColors())
            {
                
                //produces 1 channel (1's if it's in range, 0 otherwise)
                Core.inRange(mHSV, targetColor.getLowerBound(), targetColor.getUpperBound(), mHSVThreshed); //about 12 ms
                
                Core.bitwise_or(unionedMask, mHSVThreshed, unionedMask); //2 ms
            }
        }
        
        //erode then dilate to make them more bloby
        Imgproc.erode(unionedMask, unionedMask, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3))); //about 8 ms (for size of 3x3)
        Imgproc.dilate(unionedMask, unionedMask, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 10))); //about 50 ms for 15x15, 18 ms for 7x7
        

        
        //mask the original image by the threshold so we can see the video for the matching objects
        rgba.copyTo(mHSV, unionedMask); //8 ms
        drawMinYThreshold(mHSV);
        
        //find the objects in our scene
        if (targetObjects != null)
        {
            List<FoundObject> foundObjects = getFoundObjects(unionedMask);
            Log.w("objects", String.valueOf(foundObjects.size()));
            targetObjects.setFoundObjects(foundObjects, minYThreshold);
            FoundObject closestObject = targetObjects.getClosestObject();
            
            if (closestObject != null)
            {
            	lastLocatedTime = System.currentTimeMillis();
            	
                //draw target
                drawHairPin(mHSV, closestObject.x, closestObject.y); //0-1 ms
               
                //send info to nxt
                double normX = ((2 * closestObject.x) / (double) WIDTH) - 1;
                double normY = ((2 * closestObject.y) / (double) HEIGHT) - 1;
                
                messenger.pongBallFound((int)(normX * 100));
                
                prevNormX = normX;
                prevNormY = normY;
            }
            else
            {
            	//if the ball was last seen close to the right or left edge of the screen less than 1.5 seconds ago
            	// still send a found message
            	if (System.currentTimeMillis() - lastLocatedTime < 1500 && Math.abs(prevNormX) > 0.85)
            	{
                	messenger.pongBallFound((int)(prevNormX * 100));	
            	}
            	else
            	{
            		messenger.pingPongBallNotFound();
            	}
            }
            messenger.sendMessage();
            
        }
   
        
        return mHSV;
    }
    
    private void drawHairPin(Mat mat, int x, int y)
    {
        Scalar green = new Scalar(0, 255, 0, 255);
        
        //top left
        Core.line(mat, new Point(x-30, y-30), new Point(x-15, y-30), green, 2);
        Core.line(mat, new Point(x-30, y-30), new Point(x-30, y-15), green, 2);
        
        //top right
        Core.line(mat, new Point(x+30, y-30), new Point(x+15, y-30), green, 2);
        Core.line(mat, new Point(x+30, y-30), new Point(x+30, y-15), green, 2);
        
        //bottom left
        Core.line(mat, new Point(x-30, y+30), new Point(x-15, y+30), green, 2);
        Core.line(mat, new Point(x-30, y+30), new Point(x-30, y+15), green, 2);
        
        //bottom right
        Core.line(mat, new Point(x+30, y+30), new Point(x+15, y+30), green, 2);
        Core.line(mat, new Point(x+30, y+30), new Point(x+30, y+15), green, 2);
        
        //plus
        Core.line(mat, new Point(x-15, y), new Point(x+15, y), green);
        Core.line(mat, new Point(x, y+15), new Point(x, y-15), green);
    }
    
    private void drawMinYThreshold(Mat mat)
    {
        Scalar blue = new Scalar(0, 0, 255, 255);
        Core.line(mat, new Point(15, minYThreshold), new Point(WIDTH - 15, minYThreshold), blue, 2);
    }
    
    private List<FoundObject> getFoundObjects(Mat mat)
    {
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(unionedMask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        List<FoundObject> objects = new ArrayList<FoundObject>(contours.size());
        for (MatOfPoint contour : contours) 
        {
            Moments m = Imgproc.moments(contour, false);
            int x = (int) (m.get_m10() / m.get_m00());
            int y = (int) (m.get_m01() / m.get_m00());
            double contourarea = Imgproc.contourArea(contour);
            objects.add(new FoundObject(x, y, contourarea));
        }
        return objects;
    }

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (lastSensorTime == 0)
		{
			lastSensorTime = event.timestamp;
			return;
		}
		
		
//		else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) 
//		{
//            float[] rotationV = new float[16];
//			SensorManager.getRotationMatrixFromVector(rotationV, event.values);
//			
//			float[] orientationValuesV = new float[3];
//			SensorManager.getOrientation(rotationV, orientationValuesV);
//			
//			messenger.setRawRotation((int) Math.toDegrees(orientationValuesV[0]));
//        }
      
	}
	
	public void setRotation(double rotation)
	{
		this.rotation = rotation;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// TODO Auto-generated method stub
		
	}
}
