/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.gpsview;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andi
 */
public class Nmea {

    static String ggaMessage(long time, double lat, char nw, double longi, char ew,
            short status, short satTracked, double hdop, double alt, double geoidAlt) {
        String message = "";
        double timeDouble = time / 1000.0;
        message = message + String.format(Locale.US, "$GPGGA,%08.2f,", timeDouble);
        double latFract = lat % 1;
        double latDeg = lat - latFract;
        double longFract = longi % 1;
        double longDeg = longi - longFract;
        double latMin = latFract * 60.0;
        double longMin = longFract * 60.0;
        message = message + String.format(Locale.US, "%02.0f%02.4f,%1c,", latDeg, latMin, nw);
        message = message + String.format(Locale.US, "%03.0f%02.4f,%1c,", longDeg, longMin, ew);
        int fix = (status == 1) ? 0 : 1;
        message = message + String.format(Locale.US, "%1d,%02d,", fix, satTracked);
        message = message + String.format(Locale.US, "%.1f,%05.0f,M,,,,*", hdop, alt);
        message = message + getSum(message);
        message = message + "\n";
        return message;
    }

    static String gsaMessage(short status, double pdop, double hdop, double vdop) {
        String message = "";

        //$GPGSA,A,3,04,05,,09,12,,,24,,,,,2.5,1.3,2.1*39
        message = message + String.format(Locale.US, "$GPGSA,A,%1d,", status);
        message = message + String.format(Locale.US, "04,08,24,32,,,,,,,,,%.1f,%.1f,%.1f*",
                pdop, hdop, vdop);
        message = message + getSum(message);
        message = message + "\n";
        return message;
    }

    static String zdaMessage(long date, long time) {
        //         $GPZDA,201530.00,04,07,2002,00,00*60
        //
        //where:
        //	hhmmss    HrMinSec(UTC)
        //        dd,mm,yyy Day,Month,Year
        //        xx        local zone hours -13..13
        //        yy        local zone minutes 0..59
        //        *CC       checksum
        String message = "";
        Double timeDouble = time / 1000.0;
        message = message + String.format(Locale.US, "$GPZDA,%08.2f,", timeDouble);
        String timeString = String.format(Locale.US,"%06d",date);
        message = message + String.format(Locale.US,"%s,%s,20%s,00,00*",
                timeString.substring(0, 2),timeString.substring(2,4),timeString.substring(4,6));
        message = message + getSum(message);
        message = message + "\n";
        return message;
    }
    
    static String hdtMessage(double heading) {
        String message = "";
        message = message + String.format(Locale.US,"$GPHDT,%3.2f,T*", heading);
        message = message + getSum(message);
        message = message + "\n";
        return message;
    }

    private static String getSum(String in) {
        int checksum = 0;
        if (in.startsWith("$")) {
            in = in.substring(1, in.length());
        }

        int end = in.indexOf('*');
        if (end == -1) {
            end = in.length();
        }
        for (int i = 0; i < end; i++) {
            checksum = checksum ^ in.charAt(i);
        }
        String hex = Integer.toHexString(checksum);
        if (hex.length() == 1) {
            hex = "0" + hex;
        }
        return hex.toUpperCase();
    }

    public static void writeMessage(String file, String message) {
        try {
            Files.write(Paths.get(file), message.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ex) {
            Logger.getLogger(Nmea.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
