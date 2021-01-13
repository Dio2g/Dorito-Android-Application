package testview.dorito;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;



public class MainActivity extends AppCompatActivity {
    private byte[] lat;
    private byte[] lng;
    private byte[] header = new byte[]{(byte)0xAB, (byte)0xBA}; //header set too 'ABBA'
    private byte[] userID; //set spare byte to 0x00
    private int enabled;
    private int scaledProgress;
    private UDP_Client Client = new UDP_Client("",0);
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //keep screen on wile app is running
        context = getApplicationContext();
        EditText userID_input = (EditText)findViewById(R.id.userID_input);
        EditText destAdd_input = (EditText)findViewById(R.id.destAdd_input);
        EditText destPort_input = (EditText)findViewById(R.id.destPort_input);

        if(!fileExists(context, "config.txt")){ //if file does not exist create new file and populate with blank info
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
                outputStreamWriter.write("Config:"+","+"0,0,0");
                outputStreamWriter.close();
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }

        try { //populate id, address and port
            userID = ByteBuffer.allocate(1).put((byte) (Integer.parseInt(readFromFile(context)[1]) & 255)).array();
            if(readFromFile(context)[3]!="0"||readFromFile(context)[3]!=null){
                userID_input.setText(readFromFile(context)[1]);
            }
            UDP_Client.setBroadcastAddress(readFromFile(context)[2]);
            if(readFromFile(context)[3]!="0"||readFromFile(context)[3]!=null){
                destAdd_input.setText(readFromFile(context)[2]);
            }
            UDP_Client.setServerPort(Integer.parseInt(readFromFile(context)[3]));
            if(readFromFile(context)[3]!="0"||readFromFile(context)[3]!=null){
                destPort_input.setText(readFromFile(context)[3]);
            }
        }catch(Exception ignored){}

        scaledProgress = (int) (40/6.6666); //scale seek bar progress down
        scaledProgress = scaledProgress<<4; //shift right four bits
        SeekBar speed_bar = (SeekBar) findViewById(R.id.speed_bar);
        speed_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scaledProgress = (int)(((double)progress)/6.6666); //scale seek bar progress down
                scaledProgress = scaledProgress<<4; //shift right four bits
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        ToggleButton follow_btn = findViewById(R.id.follow_btn);
        follow_btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { //toggle button listener
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) //if toggle is on
                {
                    enabled = 2;
                }
                else //if toggle is off
                {
                    enabled = 0;
                }
            }
        });

        String[] requiredPermissions = { //builds array of required permissions
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        boolean ok = true;

        for (int i = 0; i<requiredPermissions.length; i++){ //checks each permission to see if its been granted
            int result = ActivityCompat.checkSelfPermission(this,requiredPermissions[i]);
            if(result != PackageManager.PERMISSION_GRANTED){
                ok = false;
            }
        }

        if(!ok){  //if permissions not granted
            ActivityCompat.requestPermissions(this, requiredPermissions, 1);
            System.exit(0);
        }else{ //if permissions granted
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    try {
                        lat = ByteBuffer.allocate(4).putInt((int) (location.getLatitude() * 1E+7)).array(); //get lat and lng store in separate byte arrays
                        lng = ByteBuffer.allocate(4).putInt((int) (location.getLongitude() * 1E+7)).array();

                        //Set message
                        UDP_Client.Message = buildUpLink(lat, lng);
                        //Send message
                        UDP_Client.Send();
                    }catch(Exception ignored){}
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }
    }

    public void exitButton_onClick(View v){ //exit app
        try {
            finish();
            System.exit(0);
        }catch(Exception ignored){}
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void saveButton_onClick(View v){
        try {
            EditText userID_input = (EditText)findViewById(R.id.userID_input);
            EditText destAdd_input = (EditText)findViewById(R.id.destAdd_input);
            EditText destPort_input = (EditText)findViewById(R.id.destPort_input);
            //validation
            if((Integer.parseInt(String.valueOf(userID_input.getText()))>=0&&Integer.parseInt(String.valueOf(userID_input.getText()))<=254)&&validateIP(String.valueOf(destAdd_input.getText()))&&isInteger(String.valueOf(destPort_input.getText()))){
                String data = "Config:"+","+userID_input.getText()+","+destAdd_input.getText()+","+destPort_input.getText();
                writeToFile(data, context);
                userID = ByteBuffer.allocate(1).put((byte) (Integer.parseInt(readFromFile(context)[1])&255)).array();
                UDP_Client.setBroadcastAddress(readFromFile(context)[2]);
                UDP_Client.setServerPort(Integer.parseInt(readFromFile(context)[3]));
            }else if(!(Integer.parseInt(String.valueOf(userID_input.getText()))>=0&&Integer.parseInt(String.valueOf(userID_input.getText()))<=254)){
                Toast.makeText(getBaseContext(), "Invalid UserID please enter Integer between 0 and 254",
                        Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(getBaseContext(), "Invalid IP Address or Port",
                        Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getBaseContext(), "Invalid UserID please enter Integer between 0 and 254",
                    Toast.LENGTH_LONG).show();
        }
    }

    private int[] checksum(byte[] byteArray){ //calc checksum
        int[] checksumArray = new int[2];
        int ckA = 0, ckB = 0;
        for (int i = 0; i < byteArray.length; i++){
            ckA = (ckA + byteArray[i]) & 255;
            ckB = (ckB + ckA) & 255;
        }
        checksumArray[0] = ckA;
        checksumArray[1] = ckB;
        return checksumArray;
    }

    private byte[] buildUpLink(byte[] lat, byte[] lng){//create final packet to send
        byte[] controlByte = ByteBuffer.allocate(1).put((byte) ((scaledProgress & 255) | (enabled & 255))).array();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(header);
            outputStream.write(userID);
            outputStream.write(controlByte);
            outputStream.write(lat);
            outputStream.write(lng);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] finalOutput = outputStream.toByteArray();

        int[] checksumArray = checksum(finalOutput);
        byte[] ckAByte = ByteBuffer.allocate(1).put((byte) (checksumArray[0]&255)).array();
        byte[] ckBByte = ByteBuffer.allocate(1).put((byte) (checksumArray[1]&255)).array();

        try {
            outputStream.write(ckAByte);
            outputStream.write(ckBByte);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finalOutput = outputStream.toByteArray();

        return finalOutput;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void writeToFile(String data,Context context) { //write data to config.txt
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String[] readFromFile(Context context) {//read data from config.txt

        String readString = "";
        String[] readArray = new String[4];

        try {
            InputStream inputStream = context.openFileInput("config.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                readString = stringBuilder.toString();
                readArray = readString.split(",");
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return readArray;
    }

    public boolean fileExists(Context context, String filename) {//check file exists
        try{
            File file = context.getFileStreamPath(filename);
            if(file == null || !file.exists()) {
                return false;
            }
        }catch (Exception ignored){}

        return true;
    }

    public static boolean validateIP(final String ip) {//validate IP with regex
        String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
        return ip.matches(PATTERN);
    }

    public static boolean isInteger(String str) {//check string can be an integer
        try {
            if (str == null) {
                return false;
            }
            int length = str.length();
            if (length == 0) {
                return false;
            }
            int i = 0;
            if (str.charAt(0) == '-') {
                if (length == 1) {
                    return false;
                }
                i = 1;
            }
            for (; i < length; i++) {
                char c = str.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
            }
        }
        catch (Exception ignored){}
        return true;
    }
}