package jlrostc.ben.x351remotecontrol;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UDPSender extends Thread {
    private InetAddress IPAddress;
    private DatagramSocket mUDPSocket;
    private int mPort;
    private String mAddress;
    private int socketCounter = 0;
    private int sendFailCount = 0;
    private boolean mUDPSocketOpen = false;
    private boolean UDPEnabled = true;
    public Handler PacketHandler;

    public UDPSender(String address, int port) throws IllegalArgumentException {
        mPort = port;

        if (address.equals("")) {
            throw new IllegalArgumentException("Address must be of the form x.x.x.x");
        } else {
            mAddress = address;
        }
    }

    @Override
    public void run() {
        Looper.prepare();

        PacketHandler = new Handler (new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (UDPEnabled && mUDPSocketOpen) {
                    RemoteControlPacket rcp = (RemoteControlPacket) msg.obj;
                    if (!sendSteeringAngle(rcp.EACEnabled, rcp.SteeringAngle)) sendFailCount++;
                    socketCounter = 0;
                }
                return true;
            }
        });

        mUDPSocketOpen = OpenUDPSocket();

        Looper.loop();
    }

    private boolean OpenUDPSocket() {
        boolean success;
        try {
            IPAddress = InetAddress.getByName(mAddress);
            mUDPSocket = new DatagramSocket(null);
            mUDPSocket.setReuseAddress(true);
            mUDPSocket.bind(new InetSocketAddress(mPort));
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    private boolean sendUDPPacket(byte[] data, int length) {
        boolean success;
        try {
            mUDPSocket.send(new DatagramPacket(data, length, IPAddress, mPort));
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    private boolean sendSteeringAngle(boolean enableExternalSteering, float angle) {
        // the message that we send is 3 bytes
        // the first byte is 1 or 2. We don't send zero because zero implies no update/no change
        //      1 = steering enabled
        //      2 = steering disabled

        int enableSteering;
        if (enableExternalSteering) {
            enableSteering = 1;
        } else {
            enableSteering = 2;
        }

        // convert to byte array
        byte[] enableSteeringBytes = ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN).putInt(enableSteering).array();

        // the second and third byte is a two-byte integer between 0 and 8000

        // the XJs lock-to-lock angle is -417 to +417 degrees
        // the raw value that we send will be between -400 and +400 degrees divided by a factor of 10
        // i.e. we send between 0 and 8000

        // convert float to integer representation of angle
        int sentAngle = (int) (angle * 10);

        // add offset
        sentAngle += 4000;

        // convert to byte array
        byte[] sentAngleBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putInt(sentAngle).array();

        // bang 'em together
        byte[] sentMessage = new byte[3];
        sentMessage[0] = enableSteeringBytes[0];
        sentMessage[1] = sentAngleBytes[0];
        sentMessage[2] = sentAngleBytes[1];

        // send
        return sendUDPPacket(sentMessage, sentMessage.length);
    }
}

