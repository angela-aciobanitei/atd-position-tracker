package com.ang.acb.positiontracker.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ang.acb.positiontracker.BuildConfig;
import com.ang.acb.positiontracker.R;
import com.ang.acb.positiontracker.data.LocationEntry;
import com.ang.acb.positiontracker.utils.InjectorUtils;
import com.ang.acb.positiontracker.viewmodel.LocationViewModel;
import com.ang.acb.positiontracker.viewmodel.ViewModelFactory;
import com.ang.acb.positiontracker.worker.TrackLocationWorker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Tag for logging
    private static final String LOG_TAG = MapsActivity.class.getSimpleName();

    // Unique tag for the periodic work request
    private static final String LOCATION_WORK_TAG ="LOCATION_WORK_TAG";

    // Used when checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // Constant for date format
    private static final String DATE_FORMAT = "dd/MM/yyy";

    // Date formatter
    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

    // Shared Preferences keys
    public static final String PREFERENCES_DATES ="PREFERENCES_DATES";
    public static final String KEY_START_DATE_FORMATTED = "KEY_START_DATE_FORMATTED";
    public static final String KEY_END_DATE_FORMATTED = "KEY_END_DATE_FORMATTED";
    public static final String KEY_START_DATE_LONG = "KEY_START_DATE_LONG";
    public static final String KEY_END_DATE_LONG = "KEY_END_DATE_LONG";

    // Shared preference fields
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private SharedPreferences datesPreferences;
    private SharedPreferences defaultPreferences;

    // Date range picker dialog fields
    private DatePickerDialog datePickerDialog;
    private EditText startDateEditText;
    private EditText endDateEditText;
    private int currentYear;
    private int currentMonth;
    private int currentDay;
    private long startDateLong = 0L;
    private long endDateLong = 0L;
    private String startDateFormatted;
    private String endDateFormatted;

    private GoogleMap map;

    private ViewModelFactory viewModelFactory;
    private LocationViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (!checkLocationPermission()) requestPermissions();
        setupSharedPreferences();
        initViewModel();
        setupMapFragment();
    }

    private void setupSharedPreferences() {
        // Initialize shared preferences files
        datesPreferences = getSharedPreferences(PREFERENCES_DATES, Context.MODE_PRIVATE);
        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialize shared preference listener
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                updateLocationTrackingFromSharedPreferences();
            }
        };

        defaultPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        defaultPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    private void updateLocationTrackingFromSharedPreferences() {
        // Read settings
        boolean isTrackingEnabled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_position_tracking_key), true);

        // Start/stop location tracking based on read settings
        if (isTrackingEnabled) trackLocation();
        else stopTrackLocation();
    }

    public void trackLocation() {
        // Create a Constraints object that defines when the task should run
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Use PeriodicWorkRequest to create a task repeating every 30 minutes
        PeriodicWorkRequest locationWorker = new PeriodicWorkRequest.Builder(
                TrackLocationWorker.class,15, TimeUnit.MINUTES)
                .addTag(LOCATION_WORK_TAG)
                .setConstraints(constraints)
                .build();

        // Enqueue the recurring task
        WorkManager.getInstance().enqueueUniquePeriodicWork(
                LOCATION_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                locationWorker);
    }

    public void stopTrackLocation() {
        WorkManager.getInstance().cancelAllWorkByTag(LOCATION_WORK_TAG);
    }

    private boolean checkLocationPermission(){
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user
        // denied the request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                trackLocation();
            } else {
                // Permission denied.
                Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                          BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(MapsActivity.this, SettingsActivity.class));
                return true;
            case R.id.action_select_date_range:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupMapFragment() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    private void initViewModel() {
        viewModelFactory = InjectorUtils.provideViewModelFactory(getApplicationContext());

        viewModel = ViewModelProviders.of(MapsActivity.this, viewModelFactory)
                .get(LocationViewModel.class);
    }

    private void initAllLocationsList() {
        // Observe the list of Location objects cached in ViewModel.
        viewModel.getLocationsAll()
                .observe(this, new Observer<List<LocationEntry>>() {
                    @Override
                    public void onChanged(@Nullable List<LocationEntry> locations) {
                        if(locations != null && locations.size() > 0) {
                            map.clear();
                            addMarkers(map, locations);
                        }
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(LOG_TAG, "onMapReady()");
        // Initialize the map.
        map = googleMap;

        // Enable zoom controls. These are disabled by default.
        map.getUiSettings().setZoomControlsEnabled(true);
        

        // If the user has not selected a date range to filter locations by
        // (i.e. start date and end date values are still the default values of 0L),
        // then show all the locations on the map, otherwise show the filtered locations.
        long startDate = datesPreferences.getLong(KEY_START_DATE_LONG, 0L);
        long endDate = datesPreferences.getLong(KEY_END_DATE_LONG, 0L);
        if(startDate == 0L || endDate == 0L) {
            initAllLocationsList();
        } else {
            //initFilteredLocationList(startDate, endDate);
        }

        // Create custom info windows for every marker (to display information
        // to the user when they tap on a marker).
        addCustomInfoWidow(map);
    }


    private void addMarkers(GoogleMap googleMap, @NonNull List<LocationEntry> locations) {
        // Used to define latitude-longitude bounds.
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // For each location add a corresponding marker to the map.
        for (LocationEntry location : locations) {
            Log.d(LOG_TAG, location.toString());
            LatLng latLng = new LatLng(
                    location.getLatitude(),
                    location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title("You were here on " + dateFormat.format(location.getSavedAt()))
                    .snippet("Latitude: " + location.getLatitude() +
                            "\nLongitude: " + location.getLongitude());
            googleMap.addMarker(markerOptions);
            builder.include(markerOptions.getPosition());
        }

        // Move the camera so all the markers fit on the screen.
        // Note: throws IllegalStateException if points have been included.
        LatLngBounds bounds = builder.build();
        int zoomWidth = getResources().getDisplayMetrics().widthPixels;
        int zoomHeight = getResources().getDisplayMetrics().heightPixels;
        int zoomPadding = (int) (zoomWidth * 0.10);
        final CameraUpdate cameraUpdate  = CameraUpdateFactory.newLatLngBounds(
                bounds, zoomWidth, zoomHeight, zoomPadding);
        googleMap.animateCamera(cameraUpdate);
    }

    private void addCustomInfoWidow(GoogleMap googleMap) {
        // To customize the contents and design of info windows, you must create
        // a concrete implementation of the InfoWindowAdapter interface and then
        // call GoogleMap.setInfoWindowAdapter() with your implementation.
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                LinearLayout view = new LinearLayout(MapsActivity.this);
                view.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(MapsActivity.this);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(MapsActivity.this);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                view.addView(title);
                view.addView(snippet);

                return view;
            }
        });
    }
}
