package jlrostc.ben.x351remotecontrol;

/**
 * Created by ben on 3/22/16.
 */
public class RemoteControlPacket {
    public boolean EACEnabled;
    public float SteeringAngle;

    public RemoteControlPacket(boolean enabled, float angle) {
        EACEnabled = enabled;
        SteeringAngle = angle;
    }
}
