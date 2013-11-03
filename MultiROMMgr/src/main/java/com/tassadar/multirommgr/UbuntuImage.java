package com.tassadar.multirommgr;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class UbuntuImage implements Comparator<UbuntuFile> {
    public UbuntuImage(JSONObject img) throws JSONException {
        version = img.getInt("version");
        description = img.getString("description");

        JSONArray f = img.getJSONArray("files");
        for(int i = 0; i < f.length(); ++i) {
            JSONObject file = f.getJSONObject(i);
            files.add(new UbuntuFile(file));
        }

        Collections.sort(files, this);
    }

    @Override
    public int compare(UbuntuFile f1, UbuntuFile f2) {
        return f1.order - f2.order;
    }

    public int version;
    public String description;
    public ArrayList<UbuntuFile> files = new ArrayList<UbuntuFile>();
}
