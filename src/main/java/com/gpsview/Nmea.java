/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.gpsview;

import com.tinkerforge.BrickletGPS;
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
        int fix = (status == BrickletGPS.FIX_NO_FIX) ? 0 : 1;
        message = message + String.format(Locale.US, "%1d,%02d,", fix, satTracked);
        message = message + String.format(Locale.US, "%.1f,%05.0f,M,,,,*", hdop, alt);
        message = message + getSum(message);
        message = message + "\n";
        return message;
    }
    
    
//    GPRMC
//Der GPRMC-Datensatz (RMC = recommended minimum sentence C, empfohlener Minimumdatensatz) ist eine Empfehlung für das Minimum, was ein GPS-Empfänger ausgeben soll.

//$GPRMC,191410,A,4735.5634,N,00739.3538,E,0.0,0.0,181102,0.4,E,A*19
//       ^      ^ ^           ^            ^   ^   ^      ^     ^
//       |      | |           |            |   |   |      |     |
//       |      | |           |            |   |   |      |     Neu in NMEA 2.3:
//       |      | |           |            |   |   |      |     Art der Bestimmung
//       |      | |           |            |   |   |      |     A=autonomous (selbst)
//       |      | |           |            |   |   |      |     D=differential
//       |      | |           |            |   |   |      |     E=estimated (geschätzt)
//       |      | |           |            |   |   |      |     N=not valid (ungültig)
//       |      | |           |            |   |   |      |     S=simulator
//       |      | |           |            |   |   |      |   
//       |      | |           |            |   |   |      Missweisung (mit Richtung)
//       |      | |           |            |   |   |     
//       |      | |           |            |   |   Datum: 18.11.2002     
//       |      | |           |            |   |        
//       |      | |           |            |   Bewegungsrichtung in Grad (wahr)
//       |      | |           |            |
//       |      | |           |            Geschwindigkeit über Grund (Knoten)
//       |      | |           |            
//       |      | |           Längengrad mit (Vorzeichen)-Richtung (E=Ost, W=West)
//       |      | |           007° 39.3538' Ost
//       |      | |                        
//       |      | Breitengrad mit (Vorzeichen)-Richtung (N=Nord, S=Süd)
//       |      | 46° 35.5634' Nord
//       |      |
//       |      Status der Bestimmung: A=Active (gültig); V=void (ungültig)
//       |
//       Uhrzeit der Bestimmung: 19:14:10 (UTC-Zeit)
//   
     static String rmcMessage(long time, long date, double lat, char nw, double longi, char ew,
            short status, double heading, double speed) {
        String message = "";
        //double timeDouble = time / 1000.0;
        char fix = (status == BrickletGPS.FIX_NO_FIX) ? 'V' : 'A';
        long myTime = time / 1000; //cut last 3 digits
        message = message + String.format(Locale.US, "$GPRMC,%06d,%1c,",myTime,fix);
        double latFract = lat % 1;
        double latDeg = lat - latFract;
        double longFract = longi % 1;
        double longDeg = longi - longFract;
        double latMin = latFract * 60.0;
        double longMin = longFract * 60.0;
        message = message + String.format(Locale.US, "%02.0f%02.4f,%1c,", latDeg, latMin, nw);
        message = message + String.format(Locale.US, "%03.0f%02.4f,%1c,", longDeg, longMin, ew);
        message = message + String.format(Locale.US, "%3.1f,%3.1f,%06d,0.0,E,A*",speed / 1.85,heading,date);
        message = message + getSum(message);
        message = message + "\n";
        return message;
    }
    

    static String gsaMessage(short status, double pdop, double hdop, double vdop, short satUsed) {
        String message = "";

        //$GPGSA,A,3,04,05,,09,12,,,24,,,,,2.5,1.3,2.1*39
        message = message + String.format(Locale.US, "$GPGSA,A,%1d,", status);
        String satString = "";
        for(int i=0; i<12;i++) {
            if (i < satUsed) {
                satString = satString + String.format(Locale.US, "%02d,",i+1);
            } else {
                satString = satString + ",";
            }
        }
        message = message + satString + String.format(Locale.US, "%.1f,%.1f,%.1f*",
                pdop, hdop, vdop);
        message = message + getSum(message);
        message = message + "\n";
        return message;
    }

    static String vtgMessage(double heading, double speed) {
        //        GPVTG
        //
        //Der GPVTG-Datensatz enthält Daten zur Bewegungsgeschwindigkeit und Richtung.
        //
        //$GPVTG,0.0,T,359.6,M,0.0,N,0.0,K*47
        //       ^     ^       ^     ^
        //       |     |       |     |
        //       |     |       |     Geschwindigkeit über Grund in km/h (K)
        //       |     |       |
        //       |     |       Geschwindigkeit über Grund in Knoten (N)
        //       |     |      
        //       |     Kurs (magnetisch, M)     
        //       |
        //       Kurs (wahr, T)
        String message = "";
        double mheading = heading + 2.4;
        if (mheading > 360.0) {
            mheading = mheading - 360.0;
        }
        message = message + String.format(Locale.US, "$GPVTG,%3.1f,T,%3.1f,M,%3.1f,N,%3.1f,K*",
                heading, mheading, speed / 1.85, speed);
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
        String timeString = String.format(Locale.US, "%06d", date);
        message = message + String.format(Locale.US, "%s,%s,20%s,00,00*",
                timeString.substring(0, 2), timeString.substring(2, 4), timeString.substring(4, 6));
        message = message + getSum(message);
        message = message + "\n";
        return message;
    }

    static String hdtMessage(double heading) {
        String message = "";
        message = message + String.format(Locale.US, "$GPHDT,%3.2f,T*", heading);
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
