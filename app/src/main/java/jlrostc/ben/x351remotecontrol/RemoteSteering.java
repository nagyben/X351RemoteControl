package jlrostc.ben.x351remotecontrol;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class RemoteSteering extends AppCompatActivity
                            implements SeekBar.OnSeekBarChangeListener {

    private SeekBar mSeekBar;
    private Button LeftButton;
    private Button RightButton;
    private TextView txtAngle;

    private final int SENSITIVITY = 1;

    private boolean EACEnabled = false;
    private float SteeringAngleRequest = 0;

    private UDPSender mUDPSender;

    private final float MINSTEERINGANGLE = -400f;
    private final float MAXSTEERINGANGLE = 400f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_remote_steering);

        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        LeftButton = (Button) findViewById(R.id.leftButton);
        RightButton = (Button) findViewById(R.id.rightButton);
        txtAngle = (TextView) findViewById(R.id.txtAngle);

        // Set component listeners
        mSeekBar.setOnSeekBarChangeListener(this);
        LeftButton.setOnTouchListener(leftButtonListener);
        RightButton.setOnTouchListener(rightButtonListener);

        // set seekbar limits
        mSeekBar.setMax(1000);

        try {
            mUDPSender = new UDPSender("192.168.16.50", 25000);
            mUDPSender.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        UpdateUI();
    }

    private void sendUpdate() {
        Message message = Message.obtain();
        message.obj = new RemoteControlPacket(this.EACEnabled, this.SteeringAngleRequest);
        mUDPSender.PacketHandler.sendMessage(message);
    }

    private void UpdateUI() {
        // calculate progress from angle
        int progress = (int) ((-SteeringAngleRequest + 400) / (MAXSTEERINGANGLE - MINSTEERINGANGLE)
                                * 1000);
        mSeekBar.setProgress(progress);
        txtAngle.setText(String.format("%.1f\u00B0", SteeringAngleRequest));
    }


    private final View.OnTouchListener rightButtonListener = new View.OnTouchListener() {
        public boolean onTouch(View view, MotionEvent motionEvent) {
            SteeringAngleRequest -= SENSITIVITY;
            sendUpdate();
            UpdateUI();
            return false;
        }
    };

    private final View.OnTouchListener leftButtonListener = new View.OnTouchListener() {
        public boolean onTouch(View view, MotionEvent motionEvent) {
            SteeringAngleRequest += SENSITIVITY;
            sendUpdate();
            UpdateUI();
            return false;
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            progress = 1000 - progress;
            // interpolate between steering limits
            SteeringAngleRequest = (float) progress / 1000
                                    * (MAXSTEERINGANGLE - MINSTEERINGANGLE)
                                    + MINSTEERINGANGLE;

            sendUpdate();
            UpdateUI();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
