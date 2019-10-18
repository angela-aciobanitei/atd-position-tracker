package com.ang.acb.positiontracker.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ang.acb.positiontracker.data.LocationRepository;

/**
 * A factory class for creating ViewModels with a constructor that takes a [MovieRepository].
 *
 * See: https://github.com/googlesamples/android-sunflower
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final LocationRepository repository;

    public static ViewModelFactory getInstance(LocationRepository repository) {
        return new ViewModelFactory(repository);
    }

    private ViewModelFactory(LocationRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LocationViewModel.class)) {
            return (T) new LocationViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
