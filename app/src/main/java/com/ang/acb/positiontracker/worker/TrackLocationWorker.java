package com.ang.acb.positiontracker.worker;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ang.acb.positiontracker.data.AppDatabase;
import com.ang.acb.positiontracker.data.LocationEntry;
import com.ang.acb.positiontracker.utils.AppExecutors;
import com.ang.acb.positiontracker.utils.LocationUtils;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Date;

/**
 * See: https://medium.com/androiddevelopers/workmanager-basics-beba51e94048
 * See: https://proandroiddev.com/workout-your-tasks-with-workmanager-intro-db5aefe14d66
 * See: https://proandroiddev.com/workout-your-tasks-with-workmanager-advanced-topics-c469581c235b
 * See: https://medium.com/androiddevelopers/introducing-workmanager-2083bcfc4712
 * See: https://medium.com/androiddevelopers/workmanager-periodicity-ff35185ff006
 * See: https://medium.com/@prithvibhola08/location-all-the-time-with-workmanager-8f8b58ae4bbc
 */

public class TrackLocationWorker extends Worker {

    private static final String TAG = TrackLocationWorker.class.getSimpleName() ;
    private AppDatabase database;

    /**
     * Creates an instance of the {@link Worker}.
     * @param context   the application {@link Context}
     * @param workerParams the set of {@link WorkerParameters}
     */
    public TrackLocationWorker(@NonNull Context context,
                               @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        database = AppDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            getLastKnownLocation();
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG,  "Failure in doing work" + e);
            return Result.failure();
        }
    }

    private void getLastKnownLocation() {
        // One time location request
        if (LocationUtils.isGPSEnabled(this.getApplicationContext())
                && LocationUtils.checkLocationPermission(this.getApplicationContext())) {

            // Use the Location Service client to get the user's last known location.
            LocationServices.getFusedLocationProviderClient(this.getApplicationContext())
                    .getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // Save the location data into the database.
                                LocationEntry locationEntry = new LocationEntry(
                                        location.getLatitude(),
                                        location.getLongitude(),
                                        new Date());
                                saveLocation(locationEntry);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // TODO
                        }
                    });
        }
    }

    private void saveLocation(final LocationEntry location) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                database.locationDao().insertLocation(location);
            }
        });
    }
}
