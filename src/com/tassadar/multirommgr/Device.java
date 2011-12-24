package com.tassadar.multirommgr;

public class Device
{
    public Device(String n, String p, String m, String v, String r[][])
    {
        name = n;
        link_package = p;
        link_manifest = m;
        link_versions = v;
        link_recovery = r;
    }
    
    public String name;
    public String link_package;
    public String link_manifest;
    public String link_versions;
    public String link_recovery[][];
};
