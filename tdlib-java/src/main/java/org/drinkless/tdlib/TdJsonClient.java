package org.drinkless.tdlib;

public final class TdJsonClient {
    static {
        try {
            System.loadLibrary("tdjni");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public static native long create();

    public static native void send(long clientId, String request);

    public static native String receive(long clientId, double timeout);

    public static native String execute(long clientId, String request);

    public static native void destroy(long clientId);

    private TdJsonClient() {}
}
