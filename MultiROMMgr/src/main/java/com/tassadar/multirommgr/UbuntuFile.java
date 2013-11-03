package com.tassadar.multirommgr;

import org.json.JSONException;
import org.json.JSONObject;

public class UbuntuFile {

    public UbuntuFile(JSONObject file) throws JSONException {
        checksum = file.getString("checksum");
        order = file.getInt("order");
        path = file.getString("path");
        signature = file.getString("signature");
        size = file.getInt("size");
    }

    public String checksum;
    public int order;
    public String path;
    public String signature;
    public int size;
}
