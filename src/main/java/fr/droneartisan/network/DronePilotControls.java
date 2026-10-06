package fr.droneartisan.network;

public final class DronePilotControls {
    public static final int NO_ACTION = 0;
    private static final int LAST_ACTION = 6;

    private DronePilotControls() {}

    public static boolean isValid(float forward, float strafe, float vertical,
                                  float yaw, float pitch, int action, int face) {
        return Float.isFinite(forward) && Float.isFinite(strafe) && Float.isFinite(vertical)
                && Float.isFinite(yaw) && Float.isFinite(pitch)
                && Math.abs(forward) <= 1.0F && Math.abs(strafe) <= 1.0F
                && Math.abs(vertical) <= 1.0F && Math.abs(pitch) <= 90.0F
                && action >= NO_ACTION && action <= LAST_ACTION
                && (action != 1 && action != 2 || face >= 0 && face < 6);
    }
}
