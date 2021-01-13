package testview.dorito;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import android.os.AsyncTask;
import android.os.Build;

public class UDP_Client
{
    public static byte[] Message;
    public static String BroadcastAddress;
    public static int SERVER_PORT;

    public UDP_Client(String BroadcastAddress, int SERVER_PORT) {
        UDP_Client.BroadcastAddress = BroadcastAddress;
        UDP_Client.SERVER_PORT = SERVER_PORT;
    }

    public static void setBroadcastAddress(String broadcastAddress) {
        BroadcastAddress = broadcastAddress;
    }

    public static void setServerPort(int serverPort) {
        SERVER_PORT = serverPort;
    }

    public static String getBroadcastAddress() {
        return BroadcastAddress;
    }

    public static int getServerPort() {
        return SERVER_PORT;
    }

    public static void Send()
    {
        AsyncTask<Void, Void, Void> async_client = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try (DatagramSocket ds = new DatagramSocket(58100)) {
                    DatagramPacket dp;
                    dp = new DatagramPacket(Message, Message.length, InetAddress.getByName(BroadcastAddress), SERVER_PORT);
                    ds.setBroadcast(true);
                    ds.send(dp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
            }
        };

        if (Build.VERSION.SDK_INT >= 11) async_client.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else async_client.execute();
    }
}
