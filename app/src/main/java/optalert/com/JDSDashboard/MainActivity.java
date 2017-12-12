package optalert.com.JDSDashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.anastr.speedviewlib.base.Gauge;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

public class MainActivity extends Activity {
    private final static String TAG = "JDSSpeedometer";
    private final static String APP_NAME = "optalert.com.JDSDashboard";

    private final static String TEST_MODE = "Test Mode";
    private final static String DEBUG_INFO = "Debug Info";
    private final static String MUTE_ALARM = "Mute Alarm";
    private final static String VALID_TONE = "Valid Tone";
    private final static String CHECK_PIC = "Check Pic";
    private static String[] SETTINGS = new String[] {TEST_MODE, DEBUG_INFO, MUTE_ALARM, VALID_TONE, CHECK_PIC};
    private boolean mTestMode = false;
    private boolean mDebugInfo = false;
    private boolean mMuteAlarm = false;
    private boolean mValidTone = false;
    private boolean mCheckPic = false;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private TextView mVersion;

    private final static String JSON_MESSAGE_JDS_SCORE          = "jds_score";
    private final static String JSON_MESSAGE_LEC_ALARM          = "lec_alarm";
    private final static String JSON_MESSAGE_FACE_TRACK         = "face_track";
    private final static String JSON_MESSAGE_FACE_TRACK_ALARM   = "face_track_alarm";
    private final static String JSON_MESSAGE_JDS_ALERT          = "jds_alert";
    private final static String JSON_MESSAGE_VALID              = "valid";

    private final static String START_TEST = "Start Test";
    private final static String STOP_TEST = "Stop Test";

    private Context mContext;
    private LocationManager mLocationMgr;
    private Button mTestBtn;
    private ImageView mViewNeedle;
    private TextView mViewGpsStatus;
    private TextView mViewAltitude;
    private TextView mViewLatitude;
    private TextView mViewLongitude;
    private TextView mViewGpsSpeed;
    private TextView mViewSpeed;
    private Handler mLocationHandler;
    private int mConsecutiveSpeedIndex;
    private int CONSECUTIVE_SPEED_MAX = 5;
    private float MOVING_SPEED = 1.0f;
    private boolean mMovingState = false;

    private CircleView mCircleFaceAvailable;
    private ImageView mViewEyeClosure;
    private TextView mViewSocketStatus;
    private TextView mViewNoJdsScore;
    private TextView mViewServerIP;
    private TextView mViewClientIP;
    private String mClientIPAddr;
    private TextView mViewJdsScore;
    private TextView mViewLec;
    private TextView mViewFaceAvailable;
    private TextView mViewFactTrackAlarm;
    private TextView mViewJdsAlert;
    private TextView mViewValid;
    private JDSDoughnut mJDSWidget;
    private Handler mJDSHandler;

    private float MIN_JDS = 0.0f;
    private float MAX_JDS = 10.0f;
    private float MEDIUM_ALERT = 4.5f;
    private float HIGH_ALERT = 5.0f;
    private float mLastJDS = MIN_JDS;
    private float mJDSDirection = 1.0f;
    private float PIVOTX = 0.5f;
    private float PIVOTY = 0.711f;
    private float MIN_DEGREE = -105.5f;
    private float MAX_DEGREE = 103.5f;
    private float MIN_SPEED = 0.0f;
    private float MAX_SPEED = 257.5f;
    private int MAX_DURATION = 1000;
    private float mLastDegree = MIN_DEGREE;
    private float mLastSpeed = MIN_SPEED;
    private float mSpeedDirection = 1.0f;
    private UDPReceiver mUDPReceiver;
    private MediaPlayer mMediaPlayer;
    private int mResId;
    private static final int GPS_UPDATE_INTERVAL = 5000;
    private static final int SOCKET_CONNECT_TIMEOUT = 10000;
    private static final int UDP_SERVER_PORT = 8023;
    private static final int TCP_SERVER_PORT = 8023;
    private static final int MAX_UDP_DATAGRAM_LEN = 1500;

    private TCPServer mTCPServer;
    private String mReceivedPicPath;
    private ImageView mViewPic;
    private long mLastTap = Long.MIN_VALUE;
    private final static long DOUBLE_TAP_DELAY_ms = 1000;

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            //Log.d(TAG, "onLocationChanged(): location=" + location.toString());
            updateLocationUI(location);
            mLocationHandler.removeCallbacksAndMessages(null);
            mLocationHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    resetLocationUI();
                }
            }, GPS_UPDATE_INTERVAL);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled()");
            if (!mTestMode) showGPSDisabledAlertToUser();
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled()");
            if (!mTestMode) mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged(): status=" +
                    (status == LocationProvider.AVAILABLE ? "AVAILABLE" : "UNAVAILABLE"));
            if (status != LocationProvider.AVAILABLE)
                resetLocationUI();
        }
    };

    private DrawerLayout.DrawerListener mDrawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            Log.d(TAG, "onDrawerSlide()");
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            Log.d(TAG, "onDrawerOpened()");
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            Log.d(TAG, "onDrawerClosed(), mTestMode=" + mTestMode + ", mDebugInfo=" + mDebugInfo);
            updateModeSetting();
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            Log.d(TAG, "onDrawerStateChanged()");
        }
    };

    private ListView.OnItemClickListener mOnItemClickListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG, "onItemClick(): position=" + position + ", value=" + mDrawerList.getItemAtPosition(position));
            for (int i = 0; i < mDrawerList.getCount(); i++) {
                String setting = mDrawerList.getItemAtPosition(i).toString();
                if (setting.equals(TEST_MODE)) {
                    mTestMode = mDrawerList.isItemChecked(i);
                    Log.d(TAG, "Test Mode=" + mTestMode);
                } else if (setting.equals(DEBUG_INFO)) {
                    mDebugInfo = mDrawerList.isItemChecked(i);
                    Log.d(TAG, "Debug Info=" + mDebugInfo);
                } else if (setting.equals(MUTE_ALARM)) {
                    mMuteAlarm = mDrawerList.isItemChecked(i);
                    Log.d(TAG, "Mute Alarm=" + mMuteAlarm);
                } else if (setting.equals(VALID_TONE)) {
                    mValidTone = mDrawerList.isItemChecked(i);
                    Log.d(TAG, "Valid Tone=" + mValidTone);
                } else if (setting.equals(CHECK_PIC)) {
                    mCheckPic = mDrawerList.isItemChecked(i);
                    Log.d(TAG, "Check Pic=" + mCheckPic);
                    mDrawerLayout.closeDrawers();
                    if (mCheckPic) {
                        showPic(getPicPath());
                    } else {
                        mViewPic.setVisibility(View.GONE);
                    }
                }
            }
        }
    };

    private Button.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            if (mTestBtn.getText().equals(START_TEST)) {
                mTestBtn.setText(STOP_TEST);
                mTestBtn.setTextColor(Color.RED);
                mLastSpeed = MIN_SPEED;
                mLastDegree = MIN_DEGREE;
                mLastJDS = MIN_JDS;
                mSpeedDirection = 1.0f;
                mJDSDirection = 1.0f;
                mViewNoJdsScore.setVisibility(View.INVISIBLE);
                mJDSWidget.setTextVisibility(true);
                mJDSWidget.setDoughnutVisibility(true);
                speedTestProgram();
                JDSTestProgram();
            } else if (mTestBtn.getText().equals("Stop Test")) {
                mTestBtn.setText(START_TEST);
                mTestBtn.setTextColor(Color.DKGRAY);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mLocationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.addDrawerListener(mDrawerListener);

        mDrawerList = (ListView) findViewById(R.id.settingsList);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, SETTINGS));
        mDrawerList.setOnItemClickListener(mOnItemClickListener);

        mVersion = (TextView) findViewById(R.id.version);
        mVersion.setText("JDS Dashboard Ver. " + getVersion());

        mJDSWidget = (JDSDoughnut) findViewById(R.id.JDSDoughnut);
        initJDSWidget();

        mViewPic = (ImageView) findViewById(R.id.receivedImg);
        mTestBtn = (Button) findViewById(R.id.download);
        mTestBtn.setOnClickListener(mOnClickListener);
        mViewNeedle = (ImageView) findViewById(R.id.needle);
        mViewSpeed = (TextView) findViewById(R.id.speed);
        updateNeedle(MIN_SPEED);

        mViewGpsStatus = (TextView) findViewById(R.id.gpsStatus);
        mViewAltitude = (TextView) findViewById(R.id.gpsAltitude);
        mViewLatitude = (TextView) findViewById(R.id.gpsLatitude);
        mViewLongitude = (TextView) findViewById(R.id.gpsLongitude);
        mViewGpsSpeed = (TextView) findViewById(R.id.gpsSpeed);
        resetLocationUI();

        mViewEyeClosure = (ImageView) findViewById(R.id.eyeClosure);
        mViewNoJdsScore = (TextView) findViewById(R.id.noJDSScore);
        mCircleFaceAvailable = (CircleView) findViewById(R.id.circleFaceAvailable);
        mViewJdsScore = (TextView) findViewById(R.id.jdsScore);
        mViewLec = (TextView) findViewById(R.id.lec);
        mViewFaceAvailable = (TextView) findViewById(R.id.faceAvailable);
        mViewFactTrackAlarm = (TextView) findViewById(R.id.faceTrackAlarm);
        mViewJdsAlert = (TextView) findViewById(R.id.jdsAlert);
        mViewValid = (TextView) findViewById(R.id.valid);
        mViewClientIP = (TextView) findViewById(R.id.clientIP);
        mViewServerIP = (TextView) findViewById(R.id.serverIP);
        mViewSocketStatus = (TextView) findViewById(R.id.socketStatus);
        resetJDSUI();

        updateModeSetting();

        mMediaPlayer = new MediaPlayer();
        mLocationHandler = new Handler();
        mJDSHandler = new Handler();
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = getWindow().getDecorView();
            // Set the IMMERSIVE flag.
            // Set the content to appear under the system bars so that the content
            // doesn't resize when the system bars hide and show.
            Log.d(TAG, "hideSystemUI()");
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        hideSystemUI();

        if (mUDPReceiver == null) {
            mUDPReceiver = new UDPReceiver();
            mUDPReceiver.start();
        }

        if (mTCPServer == null) {
            mTCPServer = new TCPServer();
            mTCPServer.start();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        if (mUDPReceiver != null) {
            mUDPReceiver.kill();
            mUDPReceiver = null;
        }

        if (mTCPServer != null) {
            mTCPServer.kill();
            mTCPServer = null;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        if (mLocationMgr != null) mLocationMgr.removeUpdates(mLocationListener);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private class UDPReceiver extends Thread {
        private boolean bKeepRunning = true;
        private JSONObject lastMessage;
        DatagramSocket socket = null;

        public void run() {
            JSONObject message;
            byte[] data = new byte[MAX_UDP_DATAGRAM_LEN];
            DatagramPacket packet = new DatagramPacket(data, data.length);

            try {
                socket = new DatagramSocket(UDP_SERVER_PORT);
                Log.d(TAG, "UDPReceiver socket created.");
                while(bKeepRunning) {
                    socket.receive(packet);
                    mClientIPAddr = packet.getAddress().getHostAddress();
                    message = new JSONObject(new String(data, 0, packet.getLength()));
                    lastMessage = message;
                    runOnUiThread(updateJDSUI);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        private void kill() {
            Log.d(TAG, "UDPReceiver kill()");
            bKeepRunning = false;
            if (socket != null) {
                Log.d(TAG, "UDPReceiver socket closed.");
                socket.close();
            }
        }

        private JSONObject getLastMessage() {
            return lastMessage;
        }
    }

    private void updateModeSetting()
    {
        updateNeedle(MIN_SPEED);
        resetJDSUI();
        resetLocationUI();

        mTestBtn.setVisibility(mTestMode ? View.VISIBLE : View.INVISIBLE);
        mTestBtn.setText(START_TEST);
        mTestBtn.setTextColor(Color.DKGRAY);
        mViewSpeed.setVisibility(mTestMode ? View.VISIBLE : View.INVISIBLE);
        mViewNoJdsScore.setVisibility(mTestMode ? View.INVISIBLE : View.VISIBLE);
        mJDSWidget.setTextVisibility(mTestMode);
        mViewGpsStatus.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewAltitude.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewLatitude.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewLongitude.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewGpsSpeed.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewJdsScore.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewLec.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewFaceAvailable.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewFactTrackAlarm.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewJdsAlert.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewValid.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewClientIP.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewServerIP.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);
        mViewSocketStatus.setVisibility(mDebugInfo ? View.VISIBLE : View.INVISIBLE);

        if (!mTestMode) {
            if (mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            } else {
                showGPSDisabledAlertToUser();
            }
        } else {
            mLocationMgr.removeUpdates(mLocationListener);
        }
    }

    private void initJDSWidget()
    {
        mJDSWidget.setMaxSpeed((int)MAX_JDS);
        mJDSWidget.setWithTremble(false);
        mJDSWidget.setSpeedometerWidth(80.0f);
        mJDSWidget.setSpeedTextPosition(Gauge.Position.BOTTOM_CENTER);
        mJDSWidget.setSpeedTextPadding(25.0f);
        mJDSWidget.setUnitTextSize(mJDSWidget.dpTOpx(20.0f));
        mJDSWidget.setUnitUnderSpeedText(true);
        mJDSWidget.setUnitSpeedInterval(30.0f);
        mJDSWidget.setLowSpeedPercent((int)((MEDIUM_ALERT)*10));
        mJDSWidget.setMediumSpeedPercent((int)((HIGH_ALERT)*10));
        mJDSWidget.speedTo(MIN_JDS);
    }

    private Runnable updateJDSUI = new Runnable() {
        public void run() {
            if (mUDPReceiver == null || mTestMode) return;
            JSONObject message = mUDPReceiver.getLastMessage();
            Log.d(TAG, "receive UDP data: " + message.toString());
            try {
                String jds = message.getString(JSON_MESSAGE_JDS_SCORE);
                String lec_alarm = message.getInt(JSON_MESSAGE_LEC_ALARM) == 1
                        ? "Long eyelid closure" : "LEC normal";
                String face_track = message.getInt(JSON_MESSAGE_VALID) == 1
                        ? "Face available" : "Face unavailable";
                String face_track_alarm = message.getInt(JSON_MESSAGE_FACE_TRACK_ALARM) == 1
                        ? "Face track lost" : "Face track normal";
                String jds_alert = message.getInt(JSON_MESSAGE_JDS_ALERT) == 0 ? "No JDS alert" :
                        (message.getInt(JSON_MESSAGE_JDS_ALERT) == 1 ? "Medium alert" : "High alert");
                String valid = message.getInt(JSON_MESSAGE_VALID) == 0 ? "Invalid" : "Valid";

                mViewClientIP.setText("Client: " + mClientIPAddr);

                mViewJdsScore.setText("JDS: " + jds);
                if (jds.equals("-.-")) {
                    mViewNoJdsScore.setVisibility(View.VISIBLE);
                    mJDSWidget.setTextVisibility(false);
                    updateJDS(MIN_JDS);
                } else {
                    mViewNoJdsScore.setVisibility(View.INVISIBLE);
                    mJDSWidget.setTextVisibility(true);
                    updateJDS(Float.parseFloat(jds));
                }

                mViewLec.setText(lec_alarm);
                if (message.getInt(JSON_MESSAGE_LEC_ALARM) == 1) {
                    mViewNoJdsScore.setVisibility(View.INVISIBLE);
                    mJDSWidget.setTextVisibility(false);
                    mViewEyeClosure.setVisibility(View.VISIBLE);
                    if (mMovingState)
                        playTones(mContext, mMediaPlayer, R.raw.highalarm);
                } else {
                    mViewEyeClosure.setVisibility(View.INVISIBLE);
                }

                mViewFaceAvailable.setText(face_track);
                if (message.getInt(JSON_MESSAGE_VALID) == 1) {
                    mCircleFaceAvailable.setVisibility(View.VISIBLE);
                    mCircleFaceAvailable.setColor(Color.GREEN);
                } else {
                    mCircleFaceAvailable.setVisibility(View.VISIBLE);
                    mCircleFaceAvailable.setColor(Color.RED);
                }

                mViewFactTrackAlarm.setText(face_track_alarm);
                if (message.getInt(JSON_MESSAGE_FACE_TRACK_ALARM) == 1) {
                    mJDSWidget.setDoughnutVisibility(false);
                    if (mViewEyeClosure.getVisibility() == View.INVISIBLE) {
                        if (jds.equals("-.-")) mViewNoJdsScore.setVisibility(View.VISIBLE);
                        else mJDSWidget.setTextVisibility(true);
                    }
                    //playTones(mContext, mMediaPlayer, R.raw.pleasetakecare);
                } else {
                    mJDSWidget.setDoughnutVisibility(true);
                }

                mViewJdsAlert.setText(jds_alert);
                if (message.getInt(JSON_MESSAGE_JDS_ALERT) == 1) {
                    playTones(mContext, mMediaPlayer, R.raw.mediumriskwarning);
                } else if (message.getInt(JSON_MESSAGE_JDS_ALERT) == 2) {
                    playTones(mContext, mMediaPlayer, R.raw.highriskwarning);
                }

                mViewValid.setText(valid);
                if (mValidTone) {
                    if (message.getInt(JSON_MESSAGE_VALID) == 0) {
                        if (mMediaPlayer != null && mResId != R.raw.valid)
                            mMediaPlayer.reset();
                        playTones(mContext, mMediaPlayer, R.raw.valid);
                    } else {
                        if (mMediaPlayer != null && mResId == R.raw.valid) {
                            mMediaPlayer.reset();
                            mResId = -1;
                        }
                    }
                }

                mViewSocketStatus.setText("");

                mJDSHandler.removeCallbacksAndMessages(null);
                mJDSHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resetJDSUI();
                    }
                }, SOCKET_CONNECT_TIMEOUT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void resetJDSUI() {
        mViewSocketStatus.setText("Socket LOST");
        mCircleFaceAvailable.setVisibility(View.INVISIBLE);
        mViewEyeClosure.setVisibility(View.INVISIBLE);
        mViewNoJdsScore.setVisibility(View.VISIBLE);
        mViewJdsScore.setText("");
        mViewLec.setText("");
        mViewFaceAvailable.setText("");
        mViewFactTrackAlarm.setText("");
        mViewJdsAlert.setText("");
        mViewValid.setText("");
        mViewClientIP.setText("");
        mViewServerIP.setText("Server: " + getLocalIpAddress());
        mJDSWidget.setTextVisibility(false);
        mJDSWidget.setDoughnutVisibility(true);
        mJDSWidget.speedTo(MIN_SPEED, (int)(mLastJDS * 100));
    }

    public void updateLocationUI(Location location) {
        mViewGpsStatus.setTextColor(Color.GREEN);
        mViewGpsStatus.setText("GPS: AVAILABLE");
        mViewAltitude.setTextColor(Color.GREEN);
        mViewAltitude.setText("Altitude: " + location.getAltitude());
        mViewLatitude.setTextColor(Color.GREEN);
        mViewLatitude.setText("Latitude: " + location.getLatitude());
        mViewLongitude.setTextColor(Color.GREEN);
        mViewLongitude.setText("Longitude: " + location.getLongitude());
        if (location.hasSpeed()) {
            Log.d(TAG, "GPS speed=" + location.getSpeed() + " m/s");
            mViewSpeed.setTextColor(Color.GREEN);
            float speed = location.getSpeed() * 3600 / 1000;
            mViewSpeed.setText(String.format(Locale.getDefault(), "%.1f km/h", speed));
            updateNeedle(speed);

            mViewGpsSpeed.setTextColor(Color.GREEN);
            mViewGpsSpeed.setText(String.format(Locale.getDefault(), "Speed: %.2f m/s", location.getSpeed()));
            if (location.getSpeed() < MOVING_SPEED) {
                mConsecutiveSpeedIndex = 0;
                mMovingState = false;
            } else {
                mConsecutiveSpeedIndex++;
            }

            if (mConsecutiveSpeedIndex == CONSECUTIVE_SPEED_MAX) {
                Log.d(TAG, "Moving state is: " + mMovingState);
                mMovingState = true;
                mConsecutiveSpeedIndex = 0;
            }
        } else {
            mViewSpeed.setTextColor(Color.DKGRAY);
            mViewGpsSpeed.setTextColor(Color.DKGRAY);
            mConsecutiveSpeedIndex = 0;
            mMovingState = false;
        }
    }

    public void resetLocationUI() {
        mViewGpsStatus.setTextColor(Color.RED);
        mViewGpsStatus.setText("GPS: UNAVAILABLE");
        mViewAltitude.setText("");
        mViewLatitude.setText("");
        mViewLongitude.setText("");
        mViewGpsSpeed.setText("");
        mViewSpeed.setText("km/h");
        mViewSpeed.setTextColor(Color.DKGRAY);
        mConsecutiveSpeedIndex = 0;
        mMovingState = false;
    }

    private void updateNeedle(float speed)
    {
        mViewNeedle.clearAnimation();

        if (speed > MAX_SPEED)
            speed = MAX_SPEED;
        if (speed < MIN_SPEED)
            speed = MIN_SPEED;
        float deltaDegree = (speed - mLastSpeed) / MAX_SPEED * (MAX_DEGREE - MIN_DEGREE);
        int duration = (int) (((speed > mLastSpeed) ? (speed - mLastSpeed) : (mLastSpeed - speed)) / MAX_SPEED * MAX_DURATION);

        RotateAnimation animation = new RotateAnimation(mLastDegree, mLastDegree + deltaDegree,
                Animation.RELATIVE_TO_SELF, PIVOTX, Animation.RELATIVE_TO_SELF, PIVOTY);
        animation.setDuration(duration);
        animation.setInterpolator(new LinearInterpolator());
        animation.setFillAfter(true);
        mViewNeedle.startAnimation(animation);
        mLastSpeed = speed;
        mLastDegree = mLastDegree + deltaDegree;
    }

    private void speedTestProgram()
    {
        final boolean isRunning = mTestBtn.getText().equals(STOP_TEST);
        mLocationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                float speed = mLastSpeed + 0.5f*mSpeedDirection;
                if (speed > MAX_SPEED || speed < MIN_SPEED)
                    mSpeedDirection = -mSpeedDirection;
                mViewSpeed.setText("" + speed + " km/h");
                updateNeedle(speed);
                if (isRunning) speedTestProgram();
            }
        }, 0);
    }

    private void updateJDS(float jds)
    {
        //Log.d(TAG, "updateJDS(): jds=" + jds);
        int color = mJDSWidget.getLowSpeedColor();
        mJDSWidget.setUnit("LO");
        if (jds >= MEDIUM_ALERT && jds < HIGH_ALERT) {
            color = mJDSWidget.getMediumSpeedColor();
            mJDSWidget.setUnit("MED");
        } else if (jds >= HIGH_ALERT) {
            color = mJDSWidget.getHighSpeedColor();
            mJDSWidget.setUnit("HI");
        }
        mJDSWidget.setSpeedTextColor(color);
        mJDSWidget.setUnitTextColor(color);
        mJDSWidget.speedTo(jds, (int)(Math.abs(jds - mLastJDS) * 100));
        mLastJDS = jds;
    }

    private void JDSTestProgram()
    {
        final boolean isRunning = mTestBtn.getText().equals(STOP_TEST);
        mLocationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                float jds = mLastJDS + 0.1f*mJDSDirection;
                if (jds > MAX_JDS || jds < MIN_JDS) {
                    mJDSDirection = -mJDSDirection;
                    Log.d(TAG, "change JDS direction: " + mJDSDirection);
                }
                updateJDS(jds);
                if (isRunning) JDSTestProgram();
            }
        }, 200);
    }

    private void showGPSDisabledAlertToUser()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Enable GPS to use application")
                .setCancelable(false)
                .setPositiveButton("Enable GPS",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private int getVersion() {
        int versionCode = 0;
        try {
            versionCode = getPackageManager().getPackageInfo(APP_NAME, PackageManager.GET_META_DATA).versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean playTones(Context context, MediaPlayer mp, int resId)
    {
        if (mMuteAlarm) return false;
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId);
            if (afd != null && mp != null && !mp.isPlaying()) {
                mResId = resId;
                mp.reset();
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mp.prepare();
                mp.start();
                afd.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "play tone failed: " + resId + ", msg: " + e);
        }
        return false;
    }

    private class TCPServer extends Thread {
        private boolean bKeepRunning = true;
        private ServerSocket server = null;
        private Socket client = null;
        private InputStream is = null;
        private FileOutputStream fos = null;
        private BufferedOutputStream bos = null;

        public void run() {
            byte[] data = new byte[1024 * 8];
            int bytesRead;

            try {
                server = new ServerSocket(TCP_SERVER_PORT);
                while (bKeepRunning) {
                    Log.d(TAG, "TCP server receiving...");
                    client = server.accept();
                    File pic = createPic();
                    is = client.getInputStream();
                    fos = new FileOutputStream(pic);
                    bos = new BufferedOutputStream(fos);

                    while ((bytesRead = is.read(data)) > -1) {
                        bos.write(data, 0, bytesRead);
                    }
                    fos.flush();
                    bos.flush();
                    mReceivedPicPath = pic.getAbsolutePath();
                    Log.d(TAG, "Received file: " + mReceivedPicPath);
                    if (mCheckPic) {
                        showPic(mReceivedPicPath);
                    } else {
                        mViewPic.setVisibility(View.GONE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private File createPic() {
            File pic = null;
            File picDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "receivedPics");
            if (picDir.exists()) {
                File[] files = picDir.listFiles();
                for (File file : files) {
                    file.delete();
                }
            }
            picDir.mkdir();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS", Locale.US);
            String picName = "OPTIMG_" + simpleDateFormat.format(new Date());
            try {
                pic = File.createTempFile(picName, ".jpg", picDir);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return pic;
        }

        private void kill() {
            Log.d(TAG, "TCPServer kill()");
            bKeepRunning = false;
            try {
                if (is != null) is.close();
                if (fos != null) fos.close();
                if (bos != null) bos.close();
                if (server != null) server.close();
                if (client != null) client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getPicPath() {
        String path = null;
        if (mReceivedPicPath != null && new File(mReceivedPicPath).exists()) {
            path = mReceivedPicPath;
        } else {
            File picDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "receivedPics");
            if (picDir.exists()) {
                File[] files = picDir.listFiles();
                if (files.length >= 1) path = files[0].getAbsolutePath();
            }
        }
        return path;
    }

    private void showPic(final String path) {
        Log.d(TAG, "showPic() path=" + path);
        if (path == null) {
            Toast.makeText(this, "No received picture.", Toast.LENGTH_LONG).show();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    mViewPic.setImageBitmap(bitmap);
                    mViewPic.setVisibility(View.VISIBLE);
                    mViewPic.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                long time = event.getEventTime();
                                if (mLastTap == Long.MIN_VALUE) {
                                    mLastTap = time;
                                } else {
                                    if (time - mLastTap < DOUBLE_TAP_DELAY_ms) {
                                        mViewPic.setVisibility(View.GONE);
                                    }
                                    mLastTap = Long.MIN_VALUE;
                                }
                            }
                            return false;
                        }
                    });
                }
            });
        }
    }
}
