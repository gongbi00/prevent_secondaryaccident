package com.example.accidentsystem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.daum.android.map.MapViewEventListener;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;
import java.util.Timer;


class Packet{
    double latitude; double longitude;
    int year; int month; int day; int ap;
    int hour; int min; int sec;
}


public class MainActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapView.MapViewEventListener, MapView.POIItemEventListener {
    static MapView mapView;
    RelativeLayout mapViewContainer;
    public static Packet packet = new Packet();
    int i = 1;

    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapViewContainer = (RelativeLayout)findViewById(R.id.map_view);
        mapView = new MapView(this);
        mapView.zoomIn(false);
        mapView.setCurrentLocationEventListener(this);
        mapViewContainer.addView(mapView);
        //TextView disview = (TextView) findViewById(R.id.Dis);
        //disview.bringToFront();
        //disview.setBackgroundColor(Color.YELLOW);
        //disview.setTextColor(Color.BLACK);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        mapView.setMapViewEventListener(this);
        mapView.setPOIItemEventListener(this);

        thread st = new thread();
        st.start();
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
        return super.onOptionsItemSelected(item);
    }

    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        setNow(packet);
        packet.latitude = mapPointGeo.latitude;
        packet.longitude = mapPointGeo.longitude;
        MapPoint currentMapPoint = MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude);
        mapView.setMapCenterPoint(currentMapPoint, true);
    }

    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {}
    public void onCurrentLocationUpdateFailed(MapView mapView) {}
    public void onCurrentLocationUpdateCancelled(MapView mapView) {}

    public static MapPOIItem DrawAccidentMarker(double latitude, double longitude){
        MapPoint accidentMapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
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
        MapPOIItem marker = new MapPOIItem();
        marker.setItemName("정지사고 위치");
        //marker.setTag(i);
        marker.setMapPoint(stopAccidentMapPoint);
        marker.setMarkerType(MapPOIItem.MarkerType.BluePin);
        mapView.zoomOut(false);
        mapView.addPOIItem(marker);
        //i++;
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
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint){ }

    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint){
        Packet stoppacket = new Packet();
        MapPoint.GeoCoordinate stopPointGeo = mapPoint.getMapPointGeoCoord();
        stoppacket.longitude = stopPointGeo.longitude;
        stoppacket.latitude = stopPointGeo.latitude;
        setNow(stoppacket);
        DrawStopAccidentMarker(mapView, mapPoint);
        String latit = "n" + String.valueOf(stoppacket.latitude) + "\r\n" ;
        String longi = "e" + String.valueOf(stoppacket.longitude) + "\r\n";

        DeviceControlActivity.transmit(latit);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DeviceControlActivity.transmit(longi);
            }
        },1500);
        /*
        double dis = calculateDistance(stoppacket.latitude, stoppacket.longitude);
        Log.d("현재위치",String.format("업데이트 됨(%f, %f)", packet.latitude, packet.longitude));
        Log.d("사고위치",String.format("업데이트 됨(%f, %f)", stoppacket.latitude, stoppacket.longitude));
        Log.d("거리 업데이트",String.format("업데이트 됨(%f)", dis));
         */
    }
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint){}
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint){}
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint){}
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint){}

    public static double calculateDistance(double accidentlatitude, double accidentlongitude){
        double x = Math.abs(packet.latitude - accidentlatitude);
        x = x * 133.33;
        double y = Math.abs(packet.longitude - accidentlongitude);
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

    class thread extends Thread{
        public void run(){
            while(true){
                String latit = "n" + String.valueOf(packet.latitude) + "\r\n" ;
                String longi = "e" + String.valueOf(packet.longitude) + "\r\n";
                DeviceControlActivity.transmit(latit);
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DeviceControlActivity.transmit(longi);
                    }
                },1000);
                try{
                    Thread.sleep(2000);
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
