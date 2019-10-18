package com.ang.acb.positiontracker.utils;

import android.content.Context;

import com.ang.acb.positiontracker.data.AppDatabase;
import com.ang.acb.positiontracker.data.LocationRepository;
import com.ang.acb.positiontracker.viewmodel.ViewModelFactory;

/**
 * Enables injection of data sources.
 *
 * See: https://github.com/googlesamples/android-sunflower/blob/master/app/src/main/java/com/google/samples/apps/sunflower/utilities
 */
public class InjectorUtils {

    private static LocationRepository provideLocationRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context.getApplicationContext());
        AppExecutors executors = AppExecutors.getInstance();
        return LocationRepository.getInstance(database, executors);
    }

    public static ViewModelFactory provideViewModelFactory(Context context) {
        LocationRepository repository = provideLocationRepository(context);
        return ViewModelFactory.getInstance(repository);
    }
}
