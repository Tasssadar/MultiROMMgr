package com.tassadar.multirommgr;

import android.util.Log; 
import java.io.File; 
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.util.zip.ZipEntry; 
import java.util.zip.ZipInputStream; 

/** 
 * 
 * @author jon 
 */ 
public class Decompress { 
    private String _zipFile; 
    private String _location; 

    public Decompress(String zipFile, String location) { 
        _zipFile = zipFile; 
        _location = location; 

        _dirChecker(""); 
    } 

    public boolean unzip() { 
        try  { 
            FileInputStream fin = new FileInputStream(_zipFile); 
            ZipInputStream zin = new ZipInputStream(fin); 
            ZipEntry ze = null; 
            while ((ze = zin.getNextEntry()) != null) { 
                if(ze.getName().contains("META-INF"))
                    continue;
                Log.v("Decompress", "Unzipping " + ze.getName());

                String fileName = ze.getName();
                if(ze.isDirectory() || ze.getName().contains("/")) { 
                    String split[] = ze.getName().split("/");
                    String name = "";
                    for(int i = 0; i < split.length-1; ++i)
                        name += split[i] + "/";
                    _dirChecker(name); 
                    if(ze.isDirectory())
                        continue;
                }

                FileOutputStream fout = new FileOutputStream(_location + fileName); 
                byte buff[] = new byte[1024];
                int c = 0;
                for (c = zin.read(buff, 0, 1024); c != -1; c = zin.read(buff, 0, 1024))
                    fout.write(buff, 0, c);


                zin.closeEntry(); 
                fout.close(); 

            } 
            zin.close(); 
        } catch(Exception e) { 
            Log.e("Decompress", "unzip", e); 
            return false;
        } 
        return true;
    } 

    private void _dirChecker(String dir) { 
        File f = new File(_location + dir); 

        if(!f.isDirectory()) { 
            f.mkdirs(); 
        } 
    } 
} 