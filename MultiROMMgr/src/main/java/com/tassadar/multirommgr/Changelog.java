package com.tassadar.multirommgr;

import org.json.JSONException;
import org.json.JSONObject;

public class Changelog {
    public Changelog(JSONObject o) throws JSONException {
        name = o.getString("name");
        url = o.getString("url");
    }

    public String name;
    public String url;
}
