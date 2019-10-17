package com.ang.acb.positiontracker.data;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "location")
public class LocationEntry {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private double latitude;
    private double longitude;
    @ColumnInfo(name = "saved_at")
    private Date savedAt;

    @Ignore
    public LocationEntry(double latitude, double longitude, Date savedAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.savedAt = savedAt;
    }

    public LocationEntry(int id, double latitude, double longitude, Date savedAt) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.savedAt = savedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Date getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(Date savedAt) {
        this.savedAt = savedAt;
    }

    @NonNull
    @Override
    public String toString() {
        return "[Location] - " +
                " [id] " + id +
                " [lat] " + latitude +
                " [long] "  + longitude +
                " [date] " + savedAt;
    }
}
