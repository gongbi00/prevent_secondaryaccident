package com.example.accidentsystem;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Toast;

import net.daum.android.map.MapViewEventListener;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

class Packet{
    double latitude; double longitude;
    int year; int month; int day; int ap;
    int hour; int min; int sec;
}

public class MainActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapView.MapViewEventListener, MapView.POIItemEventListener {
    private static final String TAG = " ";
    MapView mapView;
    RelativeLayout mapViewContainer;
    Packet packet = new Packet();
    int i = 1;
    boolean sign1 = false;
    boolean sign2 = false;
    Packet accident_packet = new Packet();
    Packet accident_packet2 = new Packet();
    MakeThread thread;
/*
    static final int REQUEST_ENABLE_BT = 10;
    BluetoothAdapter mBluetoothAdapter;
    private BluetoothAdapter bluetoothAdapter;
    int mPairedDeviceCount = 0;
    Set<BluetoothDevice> mDevices;
    BluetoothDevice mRemoteDevice;
    BluetoothSocket mSocket = null;
    OutputStream mOutputStream = null;
    InputStream mInputStream = null;
*/
    Thread mWorkerThread = null;
    String mStrDelimiter = "\n";
    char mCharDelimiter = '\n';
    byte []readBuffer;
    int readBufferPosition;

    int LOCATION_PERMISSION_REQUEST_CODE;
    boolean isPermissionAllowed;
    Handler mHandler;
    boolean mScanning = false;
    BluetoothAdapter mBluetoothAdapter;
    int SCAN_PERIOD = 20000;
    ScanCallback mLeScanCallback;
    interface BleListener mBlelistner;
    ScanCallback mScanCallback;
/*
    private final static String TAG="Central";
    private final static int REQUEST_ENABLE_BT= 1;
    private final static int REQUEST_FINE_LOCATION= 2;
    private final static int SCAN_PERIOD= 5000;
    private BluetoothAdapter bluetoothAdapter; // ble_adapter
    private boolean is_scanning_= false;
    private boolean connected_= false;
    private Map<String, BluetoothDevice> scan_results_;
    private ScanCallback scan_cb_;
    private BluetoothLeScanner ble_scanner_;
    private Handler scan_handler_;

    public static String SERVICE_STRING = "";
    //public static UUID UUID_TDCS_SERVICE = UUID.fromString(SERVICE_STRING);
    public static String CHARACTERISTIC_COMMAND_STRING = "0000AAB1-F845-40FA-995D-658A43FEEA4C";
    public static UUID UUID_CTRL_COMMAND = UUID.fromString( CHARACTERISTIC_COMMAND_STRING );
    public static String CHARACTERISTIC_RESPONSE_STRING = "0000AAB2-F845-40FA-995D-658A43FEEA4C";
    public static UUID UUID_CTRL_RESPONSE = UUID.fromString( CHARACTERISTIC_RESPONSE_STRING );
    public final static String MAC_ADDR= "78:A5:04:58:A7:92";
*/
    Packet[] read;


    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("현재위치",String.format("check bluetooth 전"));
        //checkBluetooth();
        /*
        BluetoothManager ble_manager;
        ble_manager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        ble_adapter_ = ble_manager.getAdapter();
        //startScanA();
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // 위치 권한이 있는지 점검
            // 없다면 -1 반환
            int permission = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION);

            // 권한이 없을 때
            if (permission == PackageManager.PERMISSION_DENIED)
            {
                String[] permissions = new String[1];
                permissions[0] = android.Manifest.permission.ACCESS_COARSE_LOCATION; // 사용자에게 요청할 권한
                requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE); // 사용자에게 권한 요청
            }
            // 권한이 있을 때
            else
            {
                // 변수 값을 true 로 변경
                isPermissionAllowed = true;
            }
        }
        Log.d("현재위치",String.format("check bluetooth 후"));
        mapViewContainer = (RelativeLayout)findViewById(R.id.map_view);
        mapView = new MapView(this);
        mapView.zoomIn(false);
        mapView.setCurrentLocationEventListener(this);
        mapViewContainer.addView(mapView);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        //mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setMapViewEventListener(this);
        mapView.setPOIItemEventListener(this);

        /*
        accident_packet.latitude = 33.453079;
        accident_packet.longitude = 126.557610;
        setNow(accident_packet);
        accident_packet2.latitude = 33.451954;
        accident_packet2.longitude = 126.557709;
        setNow(accident_packet2);

        thread = new MakeThread();
        thread.start();
        */
    }

    private class MakeThread extends Thread {
        public void run() {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (true) {
                if (sign1 == true) {
                    MapPoint accident = MapPoint.mapPointWithGeoCoord(accident_packet.latitude, accident_packet.longitude);
                    MapPOIItem a = DrawAccidentMarker(accident);
                    MapPoint accident2 = MapPoint.mapPointWithGeoCoord(accident_packet2.latitude, accident_packet2.longitude);
                    MapPOIItem b = DrawAccidentMarker(accident2);
                    while (true) {
                        Calendar today = Calendar.getInstance();
                        if (today.get(Calendar.MINUTE) >= accident_packet.min + 1) {
                            mapView.removePOIItem(a);
                            sign1 = false;
                            //break;
                        }
                        if (today.get(Calendar.MINUTE) >= accident_packet2.min + 1) {
                            mapView.removePOIItem(b);
                            sign2 = false;
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // 위치 권한 요청의 응답값인지 체크
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // 권한을 획득
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // BLE 스캐닝
                scanLeDevice(true);
            }
            // 권한 획득 실패
            else
            {
                //Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_SHORT);
                finish();
            }
        }
    }
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // SCAN_PERIOD 값만큼 시간이 지나면 스캐닝 중지
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    stopScanBLE();
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            startScanBLE();
        } else {
            mScanning = false;
            stopScanBLE();
        }
        invalidateOptionsMenu();
    }

    // BLE 스캔시작
    private void startScanBLE(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {

            mBluetoothAdapter.getBluetoothLeScanner().startScan(mScanCallback);

        }
        else
        {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }

    // BLE 스캔중지
    private void stopScanBLE(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
        }
        else
        {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }
    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattCharacteristic mReadCharacteristic = null;
    private BluetoothGattCharacteristic mWriteCharateristic = null;

    private final String SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
    private final String WRITE_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    private final String READ_UUID = "0000fff2-0000-1000-8000-00805f9b34fb";

    private boolean findGattServices() {
        List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();
        if (gattServices == null) return false;

        mReadCharacteristic = null;
        mWriteCharateristic = null;

        for (BluetoothGattService gattService : gattServices){
            HashMap<String, String> currentServiceData = new HashMap<String, String>();

            if(gattService.getUuid().toString().equals(SERVICE)){
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics){
                    if(gattCharacteristic.getUuid().toString().equals(READ_UUID)){
                        final int charaProp = gattCharacteristic.getProperties();

                        if((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0){
                            try{
                                mReadCharacteristic = gattCharacteristic;
                                List<BluetoothGattDescriptor> list = mReadCharacteristic.getDescriptors();
                                Log.d(TAG, "read characteristic found : " + charaProp);

                                mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                                //리시버 설정
                                BluetoothGattDescriptor descriptor = mReadCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mBluetoothGatt.writeDescriptor(descriptor);
                            }
                            catch (Exception e){
                                e.printStackTrace();
                                return false;
                            }
                        }
                        else{
                            Log.d(TAG, "read characteristic prop is invalid : " + charaProp);
                        }
                    }
                    else if(gattCharacteristic.getUuid().toString().equalsIgnoreCase(WRITE_UUID)){
                        final int charaProp = gattCharacteristic.getProperties();

                        if((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0){
                            Log.d(TAG, "write characteristic found : " + charaProp);
                            mWriteCharateristic = gattCharacteristic;
                        }
                        else{
                            Log.d(TAG, "write characteristic prop is invalid : " + charaProp);
                        }
                    }
                }
            }
        }
        return true;
    }
    public void writeData(byte[] data){
        try{
            byte[] sendData = null;
            boolean result = false;
            if(mBluetoothGatt.connect())
            {
                if(mWriteCharateristic == null){
                    Log.i(TAG, "Write gatt characteristic is null");
                } else if(mReadCharacteristic == null){
                    Log.i(TAG, "Read gatt characteristic is null");
                } else {
                    int dataLen = data.length;

                    String sendDataG = "";
                    for(int i=0; i<data.length; i++){
                        sendDataG += String.format("%02x", data[i]);
                    }
                    System.out.println("BLE Command 데이터 : " + sendDataG);
                    sendData = data;

                    Handler hd = new Handler();
                    hd.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mWriteCharateristic.setValue(sendData);

                            if(mWriteCharateristic == null){
                                Log.d("Ble UUID가 없습니다.", "");
                                mBluetoothGatt.disconnect();
                            }else {
                                mBluetoothGatt.writeCharacteristic(mWriteCharateristic);
                            }
                        }
                    },5);
                }
            }
            else
            {
                Log.i(TAG, "Bluetooth gatt is not connected");
            }
        }
        catch (Exception e)
        {
            Log.e("", e.toString());
        }
    }
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            try
            {
                byte[] readByte = characteristic.getValue();
                if(mBlelistner != null)
                    mBlelistner.recvData(readByte); // 기기 응답을 받는 리스너
            }
            catch(Exception e)
            {
                Log.d(TAG, e.toString());
            }
        }
    }

    /*
    protected void onResume(){
        super.onResume();

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
            finish();
    }


    private void startScanA(){
        //tv_status_.setText("Scanning...");
        if (ble_adapter_ == null || !ble_adapter_.isEnabled()) {
            requestEnableBLE();
        //    tv_status_.setText("Scanning Failed: ble not enabled");
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
       //    tv_status_.setText("Scanning Failed: no fine location permission");
        }
        List<ScanFilter> filters= new ArrayList<>();
        ScanFilter scan_filter= null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scan_filter = new ScanFilter.Builder()
                    .setDeviceName("CAR-01")
                    //        .setServiceUuid(new ParcelUuid(UUID_TDCS_SERVICE))
                    .build();
            //scan_filter = new ScanFilter();
        }
        filters.add( scan_filter );
        ScanSettings settings= null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings = new ScanSettings.Builder()
                    .setScanMode( ScanSettings.SCAN_MODE_LOW_POWER )
                    .build();
        }
        scan_results_= new HashMap<>();
        scan_cb_= new BLEScanCallback( scan_results_ );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //ble_scanner_.startScan( filters, settings, scan_cb_ );
            ble_scanner_.startScan(scan_cb_);
        }

        is_scanning_ = true;
    }
    private void requestEnableBLE() {
        Intent ble_enable_intent= new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
        startActivityForResult( ble_enable_intent, REQUEST_ENABLE_BT );

    }

    private void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions( new String[]{ Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION );
        }

    }


    @SuppressLint("NewApi")
    private class BLEScanCallback extends ScanCallback{
        private Map<String, BluetoothDevice> cb_scan_results_;
        BLEScanCallback( Map<String, BluetoothDevice> _scan_results ) {
            cb_scan_results_= _scan_results;
        }

        @Override
        public void onScanResult( int _callback_type, ScanResult _result ) {
            Log.d( TAG, "onScanResult" );
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                addScanResult( _result );
            }
        }

        @Override
        public void onBatchScanResults( List<ScanResult> _results ) {
            for( ScanResult result: _results ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    addScanResult( result );
                }
            }
        }

        @Override
        public void onScanFailed( int _error ) {
            Log.e( TAG, "BLE scan failed with code " +_error );
        }
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        private void addScanResult(ScanResult _result ) {
            // get scanned device
            BluetoothDevice device= _result.getDevice();
            // get scanned device MAC address
            String device_address= device.getAddress();
            // add the device to the result list
            cb_scan_results_.put( device_address, device );
            // log
            Log.d( TAG, "scan results device: " + device );
         //   tv_status_.setText( "add scanned device: " + device_address );
        }

    }


    void checkBluetooth(){
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch(requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK){
                    selectDevice();
                }
                else if(resultCode == RESULT_CANCELED){
                    finish();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    void checkBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            finish();
            Log.d("현재위치",String.format("mBluetoothAdapter == null"));
        }
        else {
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d("현재위치",String.format("!mBluetoothAdapter.isEnabled()"));
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                Log.d("현재위치",String.format("!mBluetoothAdapter.isEnabled() 후"));
            }
            else{
                Log.d("현재위치",String.format("select device 전"));
                selectDevice();

                Log.d("현재위치",String.format("select device 후"));
            }
        }
    }

    
    void selectDevice(){
        mDevices = mBluetoothAdapter.getBondedDevices();
        mPairedDeviceCount = mDevices.size();

        if(mPairedDeviceCount == 0){
            finish();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("블루투스 장치 선택");

        List<String> listItems = new ArrayList<String>();
        for(BluetoothDevice device : mDevices)
            listItems.add(device.getName());
        listItems.add("취소");

        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int item) {
                if(item == mPairedDeviceCount)
                    finish();
                else
                    connectToSelectedDevice(items[item].toString());
            }
        });
        builder.setCancelable(false);
        AlertDialog alert = builder.create();
        alert.show();
    }

    void beginListenForData(){
        final Handler handler = new Handler();

        readBuffer = new byte[1024];
        readBufferPosition = 0;

        mWorkerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()){
                    try{
                        int bytesAvailable = mInputStream.available();
                        if(bytesAvailable > 0){
                            byte[] packetBytes = new byte[bytesAvailable];
                            mInputStream.read(packetBytes);
                            for(int i = 0; i < bytesAvailable; i++){
                                byte b = packetBytes[i];
                                if(b == mCharDelimiter){
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            //mEditReceive.setText(mEditReceive.getText().toString() + data + mStrDelimiter);
                                        }
                                    });
                                }
                                else
                                    readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                    catch (IOException e){
                        finish();
                    }
                }
            }
        });
        mWorkerThread.start();
    }

    void sendData(String msg){
        msg += mStrDelimiter;
        try{
            mOutputStream.write(msg.getBytes());
        }catch(Exception e){
            finish();
        }
    }

    BluetoothDevice getDeviceFromBondedList(String name){
        BluetoothDevice selectedDevice = null;

        for(BluetoothDevice device : mDevices){
            if(name.equals(device.getName())){
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    protected void onDestory(){
        try{
            mWorkerThread.interrupt();
            mInputStream.close();
            mOutputStream.close();
            mSocket.close();
        }catch(Exception e){}
        super.onDestroy();
    }

    void connectToSelectedDevice(String selectedDeviceName){
        mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        try{
            mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();

            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();

            beginListenForData();
        }catch (Exception e){
            finish();
        }
    }
*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.call){
            Intent callintent = new Intent(this, preparation_plan.class);
            startActivity(callintent);
        }
        /*
        if(id == R.id.action_settings)
            return true;*/
        return super.onOptionsItemSelected(item);
    }

    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        Log.d("위치 업데이트",String.format("업데이트 됨(%f, %f)",mapPointGeo.latitude, mapPointGeo.longitude));
        setNow(packet);
        packet.latitude = mapPointGeo.latitude;
        packet.longitude = mapPointGeo.longitude;
        Log.d("날짜 업데이트",String.format("업데이트 됨(%d, %d)", packet.month, packet.day));
        MapPoint currentMapPoint = MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude);
        //지도의 중심점을 변경
        mapView.setMapCenterPoint(currentMapPoint, true);
        //트래킹 모드 끄기
        //mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
    }

    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {}
    public void onCurrentLocationUpdateFailed(MapView mapView) {}
    public void onCurrentLocationUpdateCancelled(MapView mapView) {}

    public MapPOIItem DrawAccidentMarker(MapPoint accidentMapPoint){
        MapPOIItem marker = new MapPOIItem();
        marker.setItemName("사고 위치");
        marker.setTag(0);
        marker.setMapPoint(accidentMapPoint);
        marker.setMarkerType(MapPOIItem.MarkerType.RedPin);
        mapView.addPOIItem(marker);
        return marker;
    }

    public MapPOIItem DrawStopAccidentMarker(MapView mapView, MapPoint stopAccidentMapPoint){
        MapPoint.GeoCoordinate mapPointGeo = stopAccidentMapPoint.getMapPointGeoCoord();
        Log.d("정지사고 위치 ",String.format("위치(%f, %f)",mapPointGeo.latitude, mapPointGeo.longitude));
        MapPOIItem marker = new MapPOIItem();
        marker.setItemName("정지사고 위치" + i);
        marker.setTag(i);
        marker.setMapPoint(stopAccidentMapPoint);
        marker.setMarkerType(MapPOIItem.MarkerType.BluePin);
        mapView.zoomOut(false);
        mapView.addPOIItem(marker);
        i++;
        return marker;
    }
    public void DrawLine(MapView mapView, MapPoint current, MapPoint accident){
        MapPolyline line = new MapPolyline();
        line.setLineColor(0xffff0000);
        line.addPoint(current);
        line.addPoint(accident);
        mapView.addPolyline(line);
    }
    public Packet setNow(Packet packet){
        Calendar today = Calendar.getInstance();
        packet.year = today.get(Calendar.YEAR);
        packet.month = today.get(Calendar.MONTH)+1;
        packet.day = today.get(Calendar.DATE);
        packet.ap = today.get(Calendar.AM_PM);
        packet.hour = today.get(Calendar.HOUR);
        packet.min = today.get(Calendar.MINUTE);
        packet.sec = today.get(Calendar.SECOND);
        return packet;
    }
    public void onMapViewInitialized(MapView mapView){}
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint){}
    public void onMapViewZoomLevelChanged(MapView mapView, int v){}
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint){
    }

    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint){
        Packet stoppacket = new Packet();
        MapPoint.GeoCoordinate stopPointGeo = mapPoint.getMapPointGeoCoord();
        stoppacket.longitude = stopPointGeo.longitude;
        stoppacket.latitude = stopPointGeo.latitude;
        setNow(stoppacket);
        DrawStopAccidentMarker(mapView, mapPoint);
        double dis = calculateDistance(packet.latitude, packet.longitude, stoppacket.latitude, stoppacket.longitude);
        Log.d("현재위치",String.format("업데이트 됨(%f, %f)", packet.latitude, packet.longitude));
        Log.d("사고위치",String.format("업데이트 됨(%f, %f)", stoppacket.latitude, stoppacket.longitude));
        Log.d("거리 업데이트",String.format("업데이트 됨(%f)", dis));
    }
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint){}
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint){}
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint){}
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint){}

    public double calculateDistance(double currentlatitude, double currentlongitude, double accidentlatitude, double accidentlongitude){
        double x = Math.abs(currentlatitude - accidentlatitude);
        x = x * 133.33;
        double y = Math.abs(currentlongitude - accidentlongitude);
        y = y * 133.33;
        double distance = Math.sqrt(x*x+y*y);
        distance = 1000 * distance;
        return distance;
    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
      //  mapView.removePOIItem(mapPOIItem); // mapPOIItem 삭제
    }
    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) { }
    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) { }
    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) { }
}