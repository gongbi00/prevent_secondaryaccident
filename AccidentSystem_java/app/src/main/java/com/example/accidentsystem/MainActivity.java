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

    Thread mWorkerThread = null;
    String mStrDelimiter = "\n";
    char mCharDelimiter = '\n';
    byte []readBuffer;
    int readBufferPosition;


    Packet[] read;


    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("현재위치",String.format("check bluetooth 전"));

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