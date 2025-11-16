package com.example.arbolesapp;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.File;

public class SpeciesPhoto implements Parcelable {

    public final String path;
    public final String species;
    public final String projectName;

    public SpeciesPhoto(String path, String species, String projectName) {
        this.path = path;
        this.species = species;
        this.projectName = projectName;
    }

    protected SpeciesPhoto(Parcel in) {
        path = in.readString();
        species = in.readString();
        projectName = in.readString();
    }

    public static final Creator<SpeciesPhoto> CREATOR = new Creator<SpeciesPhoto>() {
        @Override
        public SpeciesPhoto createFromParcel(Parcel in) {
            return new SpeciesPhoto(in);
        }

        @Override
        public SpeciesPhoto[] newArray(int size) {
            return new SpeciesPhoto[size];
        }
    };

    public File asFile() {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        return new File(path);
    }

    public String getFileName() {
        File file = asFile();
        return file != null ? file.getName() : "";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(species);
        dest.writeString(projectName);
    }
}