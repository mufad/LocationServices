package com.droiddigger.locationservices.Activities;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.droiddigger.locationservices.Constants.Constants;
import com.droiddigger.locationservices.R;
import com.droiddigger.locationservices.Services.DetectedActivitiesIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class TrackRecords extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, ResultCallback<Status> {

    TextView update;
    GoogleApiClient mGoogleApiClient;
    protected  ActivityDetectionBroadcastReceiver activityDetectionBroadcastReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_records);
        update= (TextView) findViewById(R.id.update);
        activityDetectionBroadcastReceiver=new ActivityDetectionBroadcastReceiver();
        buildGoogleApiClient();
    }
    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this).
                addOnConnectionFailedListener(this).
                addApi(ActivityRecognition.API).build();
    }

    public void remove(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                mGoogleApiClient,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
        //mRequestButton.setEnabled(true);
        //mCancelButton.setEnabled(false);
    }


    public void request(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
        //mRequestButton.setEnabled(false);
        //mCancelButton.setEnabled(true);
    }
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            Log.e("BLA", "Successfully added activity detection.");

        } else {
            Log.e("BLA", "Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }

    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver{
        protected  static final String TAG="receiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity>updatedActivities=intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);
            String strStatus="";
            for (DetectedActivity thisActivity:updatedActivities){
                strStatus += getActivityString(thisActivity)+"\n";
            }
            update.setText(strStatus);
        }
    }
    private String getActivityString(DetectedActivity detectedActivity) {
        Resources resources = this.getResources();
        String activity;
        switch (detectedActivity.getType()) {
            case DetectedActivity.IN_VEHICLE:
                activity = resources.getString(R.string.in_vehicle);
            case DetectedActivity.ON_BICYCLE:
                activity = resources.getString(R.string.on_bicycle);
            case DetectedActivity.ON_FOOT:
                activity = resources.getString(R.string.on_foot);
            case DetectedActivity.RUNNING:
                activity = resources.getString(R.string.running);
            case DetectedActivity.STILL:
                activity = resources.getString(R.string.still);
            default:
                activity = resources.getString(R.string.unidentifiable_activity);
        }

        return activity + " " + detectedActivity.getConfidence() + "%";
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(activityDetectionBroadcastReceiver,
                        new IntentFilter(Constants.BROADCAST_ACTION));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(activityDetectionBroadcastReceiver);
        super.onPause();
    }
}
