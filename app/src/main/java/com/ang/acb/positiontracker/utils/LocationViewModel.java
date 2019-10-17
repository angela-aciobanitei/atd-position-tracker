package com.ang.acb.positiontracker.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ang.acb.positiontracker.data.LocationEntry;
import com.ang.acb.positiontracker.data.LocationRepository;

import java.util.Date;
import java.util.List;

public class LocationViewModel extends ViewModel {

    private LocationRepository repository;
    private LiveData<List<LocationEntry>> locationsAll;
    private LiveData<List<LocationEntry>> locationsFiltered;
    private MutableLiveData<Date> fromDate = new MutableLiveData<>();
    private MutableLiveData<Date> toDate = new MutableLiveData<>();

    public LocationViewModel(LocationRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<LocationEntry>> getLocationsAll() {
        if (locationsAll == null) {
            locationsAll = repository.getAllLocations();
        }
        return locationsAll;
    }

    public LiveData<List<LocationEntry>> getLocationsBetweenDates(Date from, Date to) {
        if (locationsFiltered == null) {
            locationsFiltered = repository.getLocationsBetweenDates(from, to);
        }
        return  locationsFiltered;
    }

}
