/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.gpsview;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andi
 */
public class NmeaTest {

    public NmeaTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of ggaMessage method, of class Nmea.
     */
    @Test
    public void testGgaMessage() {
        System.out.println("ggaMessage");
        long time = 112734123L;
        double lat = 47.75;
        char nw = 'N';
        double longi = 9.2;
        char ew = 'E';
        short status = 2;
        short satTracked = 5;
        double hdop = 0.9;
        double alt = 540.0;
        double geoidAlt = 50.2;
        String expResult = "$GPGGA,112734.12,4745.0000,N,00912.0000,E,1,05,0.9,00540,M,,,,*25\n";
        String result = Nmea.ggaMessage(time, lat, nw, longi, ew, status, satTracked, hdop, alt, geoidAlt);
        assertEquals(expResult, result);

    }
    
    /**
     * Test of rmcMessage method, of class Nmea.
     */
    @Test
    public void testRmcMessage() {
        System.out.println("rmcMessage");
        long time = 112734123L;
        long date = 12345642L;
        double lat = 47.75;
        char nw = 'N';
        double longi = 9.2;
        char ew = 'E';
        short status = 2;
        double heading = 13.7;
        double speed = 17.2;
        String expResult = "$GPRMC,000000,A,4745.0000,N,00912.0000,E,9.3,13.7,000000,0.0,E,A*2C\n";
        String result = Nmea.rmcMessage(time, date, lat, nw, longi, ew,
            status, heading, speed);
        assertEquals(expResult, result);

    }
    

    /**
     * Test of gsaMessage method, of class Nmea.
     */
    @Test
    public void testGsaMessage() {
        System.out.println("gsaMessage");
        short status = 3;
        double pdop = 1.2;
        double hdop = 2.3;
        double vdop = 3.4;
        String expResult = "$GPGSA,A,3,04,08,24,32,,,,,,,,,1.2,2.3,3.4*3C\n";
        String result = Nmea.gsaMessage(status, pdop, hdop, vdop);
        assertEquals(expResult, result);
    }

    /**
     * Test of gsaMessage method, of class Nmea.
     */
    @Test
    public void testVtgMessage() {
        System.out.println("vtgMessage");
        double heading=358.2;
        double speed = 10.0;        
        String expResult = "$GPVTG,358.2,T,0.6,M,5.4,N,10.0,K*74\n";
        String result = Nmea.vtgMessage(heading,speed);
        assertEquals(expResult, result);
    }

    /**
     * Test of zdaMessage method, of class Nmea.
     */
    @Test
    public void testZdaMessage() {
        System.out.println("zdaMessage");
        long time = 123747123L;
        long date = 170217;
        String expResult = "$GPZDA,123747.12,17,02,2017,00,00*61\n";
        String result = Nmea.zdaMessage(date, time);
        assertEquals(expResult, result);
    }

    /**
     * Test of zdaMessage method, of class Nmea.
     */
    @Test
    public void testHdtMessage() {
        System.out.println("hdtMessage");
        double heading = 13.567;
        String expResult = "$GPHDT,13.57,T*35\n";
        String result = Nmea.hdtMessage(heading);
        assertEquals(expResult, result);
    }

    @Test
    public void testHdtMessage2() {
        System.out.println("hdtMessage2");
        double heading = 1.500;
        String expResult = "$GPHDT,1.50,T*01\n";
        String result = Nmea.hdtMessage(heading);
        assertEquals(expResult, result);
    }

}
