package com.ang.acb.positiontracker.data;

import androidx.lifecycle.LiveData;

import com.ang.acb.positiontracker.utils.AppExecutors;

import java.util.Date;
import java.util.List;

/**
 * Repository module for handling data operations.
 *
 * See: https://developer.android.com/jetpack/docs/guide#truth
 * See: https://github.com/googlesamples/android-architecture-components/tree/master/GithubBrowserSample
 */
public class LocationRepository {

    // For Singleton instantiation.
    private static volatile LocationRepository sInstance;

    private final AppDatabase appDatabase;
    private final AppExecutors appExecutors;

    // Prevent direct instantiation.
    private LocationRepository(AppDatabase appDatabase,  AppExecutors appExecutors) {
        this.appDatabase = appDatabase;
        this.appExecutors = appExecutors;
    }

    // Returns the single instance of this class, creating it if necessary.
    public static LocationRepository getInstance(AppDatabase appDatabase,  AppExecutors appExecutors) {
        if (sInstance == null) {
            synchronized (LocationRepository.class) {
                if (sInstance == null) {
                    sInstance = new LocationRepository(appDatabase, appExecutors);
                }
            }
        }
        return sInstance;
    }

    public LiveData<List<LocationEntry>> getAllLocations() {
        return appDatabase.locationDao().getAllLocations();
    }

    public LiveData<List<LocationEntry>> getLocationsBetweenDates(Date from, Date to) {
        return appDatabase.locationDao().getLocationsBetweenDates(from, to);
    }

    public LiveData<LocationEntry> getLocationById(int id) {
        return appDatabase.locationDao().getLocationById(id);
    }

    public void insertLocation(final LocationEntry locationEntry) {
        appExecutors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                appDatabase.locationDao().insertLocation(locationEntry);
            }
        });
    }

}
