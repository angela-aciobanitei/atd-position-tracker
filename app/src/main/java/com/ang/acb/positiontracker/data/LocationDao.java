package com.ang.acb.positiontracker.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.Date;
import java.util.List;

@Dao
public interface LocationDao {

    @Query("SELECT * FROM location ORDER BY saved_at")
    LiveData<List<LocationEntry>> getAllLocations();

    @Query("SELECT * FROM location WHERE saved_at BETWEEN :from AND :to")
    LiveData<List<LocationEntry>> getLocationsBetweenDates(Date from, Date to);

    @Query("SELECT * FROM location WHERE id = :id")
    LiveData<LocationEntry> getLocationById(int id);

    @Insert
    void insertLocation(LocationEntry locationEntry);

}
