package com.gpsview;

import java.time.LocalDateTime;

import com.tinkerforge.IPConnection;
import com.tinkerforge.BrickletGPS;
import com.tinkerforge.BrickletLCD20x4;
import com.tinkerforge.NotConnectedException;
import com.tinkerforge.AlreadyConnectedException;
import com.tinkerforge.BrickIMUV2;
import com.tinkerforge.TimeoutException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

//
public class TfGps {

    //Tinkerforge
    private static final String TFHOST = "localhost";
    private static final int TFPORT = 4223;
    private static final String LCDID = "rX5";
    private static final String GPSID = "suG";
    private static final String IMUID = "62eTAr";
    private static boolean exitProgram = false;

    private static final double HEADINGOFFSET = -90.0; //how imu is mounted
    private IPConnection ipcon = null;
    private BrickletLCD20x4 lcdBricklet = null;

    private BrickletGPS gpsBricklet;
    private BrickIMUV2 imuBrick;

    private int satUsed = 0;
    private short satView = 0;
    private short fix = BrickletGPS.FIX_NO_FIX;
    private double lat = 0.0;
    private char ns = 'N';
    private double longi = 0.0;
    private char ew = 'E';
    private double pdop;
    private double hdop;
    private double vdop;
    private double epe;

    private long date;
    private long time;
    private double altitude;
    private double speed;
    private double geoidSep;

    private Mode lcdMode = Mode.NONE;
    private Mode oldLcdMode;

    //IMU Accelerations
    private double roll;
    private double nick;
    private double gier;
    private int cycleCount = 0;
    private int startCycleCount = 999999999;
    private int port = 12347;

    String tcpConnection = "tcp://localhost:" + Integer.toString(port);
    private TCPServer tcpServer;

    private enum Mode {

        HELP, GPS, SPEED, LAGE, NONE;

        Mode next() {
            switch (this) {
                case HELP:
                    return GPS;
                case GPS:
                    return SPEED;
                case SPEED:
                    return LAGE;
                case LAGE:
                    return HELP;
            }
            return HELP;
        }

        Mode prev() {
            switch (this) {
                case HELP:
                    return LAGE;
                case GPS:
                    return HELP;
                case SPEED:
                    return GPS;
                case LAGE:
                    return SPEED;
            }
            return HELP;
        }
    }

    /**
     * Constructor
     */
    public TfGps() {
        try {
            this.oldLcdMode = Mode.NONE;
            this.lcdMode = Mode.NONE;
            ipcon = new IPConnection(); // Create IP connection
            ipcon.connect(TFHOST, TFPORT);
            //lcdBrickletInitialize();
            gpsBrickletInitialize();
            imuBrickletInitialize();
            //create socket for gpsd to send messages
            System.out.println("Starting server listening at port: " + port);
            tcpServer = new TCPServer(port);
            new Thread(tcpServer).start();
            Thread.sleep(5);
            RunCommand.exec("gpsd -n -G -D 9 " + tcpConnection);

            System.out.println("GpsView initialized");
        } catch (IOException | AlreadyConnectedException | InterruptedException ex) {
            Logger.getLogger(TfGps.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void imuBrickletInitialize() {
        try {
            imuBrick = new BrickIMUV2(IMUID, ipcon);
            imuBrick.addAllDataListener((short[] acceleration,
                    short[] magneticField,
                    short[] angularVelocity,
                    short[] eulerAngle,
                    short[] quaternion,
                    short[] linearAcceleration,
                    short[] gravityVector,
                    byte temperature, short calibrationStatus) -> {
                double heading = eulerAngle[0] / 16.0;
                heading = heading - HEADINGOFFSET;
                if (heading < 0.0) {
                    heading = heading + 360.0;
                }
                if (heading > 360.0) {
                    heading = heading - 360.0;
                }
                if (this.speed < 2.0) { //set heading only when not moving
                    this.gier = heading;
                }
                this.roll = eulerAngle[1] / 16.0;
                this.nick = eulerAngle[2] / 16.0;
                //System.out.println("gier IMU: " + heading + " nick: " + this.nick + "roll: "+ this.roll);
            });
            imuBrick.setAllDataPeriod(1000);

        } catch (TimeoutException | NotConnectedException ex) {
            Logger.getLogger(TfGps.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void gpsBrickletInitialize() {
        try {
            gpsBricklet = new BrickletGPS(GPSID, ipcon);
            gpsBricklet.addStatusListener((short fix, short satView, short satUsed) -> {
                this.fix = fix;
                this.satView = satView;
                this.satUsed = satUsed;
            });
            gpsBricklet.addCoordinatesListener((long lat, char ns, long longi, char ew,
                    int pdop, int hdop, int vdop, int epe) -> {
                this.lat = lat / 1000000.0;
                this.ns = ns;
                this.longi = longi / 1000000.0;
                this.ew = ew;
                this.pdop = pdop / 100.0;
                this.hdop = hdop / 100.0;
                this.vdop = vdop / 100.0;
                this.epe = epe / 100.0;
            });
            gpsBricklet.addDateTimeListener((long date, long time) -> {
                this.date = date;
                this.time = time;
            });
            gpsBricklet.addAltitudeListener((int altitude, int geoidSep) -> {
                this.altitude = altitude / 10.0;
                this.geoidSep = geoidSep / 10.0;
            });

            gpsBricklet.addMotionListener((long course, long speed) -> {
                this.speed = speed / 100.0;
                this.gier = course / 100.0;
                //System.out.println("gier gps: " + this.gier + " speed gps: " + this.speed);
            });

            gpsBricklet.setCoordinatesCallbackPeriod(1000);
            gpsBricklet.setStatusCallbackPeriod(1000);
            gpsBricklet.setDateTimeCallbackPeriod(1000);
            gpsBricklet.setAltitudeCallbackPeriod(1000);
            gpsBricklet.setMotionCallbackPeriod(1000);
            gpsBricklet.restart(BrickletGPS.RESTART_TYPE_HOT_START);

        } catch (TimeoutException | NotConnectedException ex) {
            Logger.getLogger(TfGps.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void lcdBrickletInitialize() {
        try {
            lcdBricklet = new BrickletLCD20x4(LCDID, ipcon);
            lcdBricklet.clearDisplay();
            lcdBricklet.backlightOff();
            lcdBricklet.addButtonPressedListener((s) -> {
                try {
                    lcdBricklet.backlightOn();
                    startCycleCount = cycleCount; //after 10 sec put light off

                } catch (TimeoutException | NotConnectedException ex) {
                    Logger.getLogger(TfGps.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
                switch (s) {
                    case 0: // help
                        oldLcdMode = lcdMode;
                        lcdMode = Mode.HELP;
                        System.out.println("Button 0 Help pressed");
                        break;
                    case 2:
                        lcdMode = lcdMode.next();
                        break;
                    case 1:
                        lcdMode = lcdMode.prev();
                        break;
                    case 3:
                        exitProgram = true;
                        break;
                    default:
                        break;
                }
            });
            short[] bytes = {
                0b11111111,
                0b11111111,
                0b11111111,
                0b11111111,
                0b11111111,
                0b11111111,
                0b11111111,
                0b11111111
            };
            lcdBricklet.setCustomCharacter((short) 0, bytes);

        } catch (TimeoutException | NotConnectedException ex) {
            Logger.getLogger(TfGps.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    //cyclic update of time and more - depending on settings
    private void cyclicUpdate() {
        cycleCount++;
        if (cycleCount - startCycleCount > 3) {
            //try {
            if (cycleCount - startCycleCount > 63) {
                //lcdBricklet.backlightOff();
                startCycleCount = 999999999; //to ensure never reached

            }
            //} catch (TimeoutException | NotConnectedException ex) {
            //  Logger.getLogger(TfGps.class
            //        .getName()).log(Level.SEVERE, null, ex);
            //}
            if (lcdMode == Mode.HELP) {
                lcdMode = oldLcdMode;
            }
        }
        LocalDateTime time = LocalDateTime.now();
        switch (lcdMode) {
            case HELP:
                lcdWriteHelp();
                break;
            case GPS:
                lcdWriteHeadline(time);
                lcdWriteCoord(lat, longi, ns, ew);
                break;
            case LAGE:
                lcdWriteLage();
                break;
            case SPEED:
                lcdWriteSpeed();
                break;
            default:
                break;
        }

        tcpServer.sendData(Nmea.ggaMessage(this.time, lat, ns, longi, ew, fix, (short) satUsed,
                hdop, lat, geoidSep).getBytes());
        tcpServer.sendData(Nmea.gsaMessage(fix, pdop, hdop, vdop).getBytes());
        tcpServer.sendData(Nmea.zdaMessage(date, this.time).getBytes());
        tcpServer.sendData(Nmea.vtgMessage(this.gier, this.speed).getBytes());
        tcpServer.sendData(Nmea.rmcMessage(this.time, date, lat, ns, longi, ew,
            fix, gier, speed).getBytes());
        //tcpServer.sendData(Nmea.hdtMessage(this.gier).getBytes());
        //lcdWriteCoord(47.51, 9.2, 'N', 'W');
    }

    // Maps a normal UTF-16 encoded string to the LCD charset
    static String utf16ToKS0066U(String utf16) {
        String ks0066u = "";
        char c;

        for (int i = 0; i < utf16.length(); i++) {
            int codePoint = utf16.codePointAt(i);

            if (Character.isHighSurrogate(utf16.charAt(i))) {
                // Skip low surrogate
                i++;
            }

            // ASCII subset from JIS X 0201
            if (codePoint >= 0x0020 && codePoint <= 0x007e) {
                // The LCD charset doesn't include '\' and '~', use similar characters instead
                switch (codePoint) {
                    case 0x005c:
                        c = (char) 0xa4;
                        break; // REVERSE SOLIDUS maps to IDEOGRAPHIC COMMA
                    case 0x007e:
                        c = (char) 0x2d;
                        break; // TILDE maps to HYPHEN-MINUS
                    default:
                        c = (char) codePoint;
                        break;
                }
            } // Katakana subset from JIS X 0201
            else if (codePoint >= 0xff61 && codePoint <= 0xff9f) {
                c = (char) (codePoint - 0xfec0);
            } // Special characters
            else {
                switch (codePoint) {
                    case 0x00a5:
                        c = (char) 0x5c;
                        break; // YEN SIGN
                    case 0x2192:
                        c = (char) 0x7e;
                        break; // RIGHTWARDS ARROW
                    case 0x2190:
                        c = (char) 0x7f;
                        break; // LEFTWARDS ARROW
                    case 0x00b0:
                        c = (char) 0xdf;
                        break; // DEGREE SIGN maps to KATAKANA SEMI-VOICED SOUND MARK
                    case 0x03b1:
                        c = (char) 0xe0;
                        break; // GREEK SMALL LETTER ALPHA
                    case 0x00c4:
                        c = (char) 0xe1;
                        break; // LATIN CAPITAL LETTER A WITH DIAERESIS
                    case 0x00e4:
                        c = (char) 0xe1;
                        break; // LATIN SMALL LETTER A WITH DIAERESIS                                                                                                              
                    case 0x00df:
                        c = (char) 0xe2;
                        break; // LATIN SMALL LETTER SHARP S                                                                                                                       
                    case 0x03b5:
                        c = (char) 0xe3;
                        break; // GREEK SMALL LETTER EPSILON   
                    case 0x00b5:
                        c = (char) 0xe4;
                        break; // MICRO SIGN                                                                                                                                       
                    case 0x03bc:
                        c = (char) 0xe4;
                        break; // GREEK SMALL LETTER MU
                    case 0x03c2:
                        c = (char) 0xe5;
                        break; // GREEK SMALL LETTER FINAL SIGMA
                    case 0x03c1:
                        c = (char) 0xe6;
                        break; // GREEK SMALL LETTER RHO
                    case 0x221a:
                        c = (char) 0xe8;
                        break; // SQUARE ROOT
                    case 0x00b9:
                        c = (char) 0xe9;
                        break; // SUPERSCRIPT ONE maps to SUPERSCRIPT (minus) ONE
                    case 0x00a4:
                        c = (char) 0xeb;
                        break; // CURRENCY SIGN
                    case 0x00a2:
                        c = (char) 0xec;
                        break; // CENT SIGN
                    case 0x2c60:
                        c = (char) 0xed;
                        break; // LATIN CAPITAL LETTER L WITH DOUBLE BAR
                    case 0x00f1:
                        c = (char) 0xee;
                        break; // LATIN SMALL LETTER N WITH TILDE
                    case 0x00d6:
                        c = (char) 0xef;
                        break; // LATIN CAPITAL LETTER O WITH DIAERESIS
                    case 0x00f6:
                        c = (char) 0xef;
                        break; // LATIN SMALL LETTER O WITH DIAERESIS
                    case 0x03f4:
                        c = (char) 0xf2;
                        break; // GREEK CAPITAL THETA SYMBOL
                    case 0x221e:
                        c = (char) 0xf3;
                        break; // INFINITY
                    case 0x03a9:
                        c = (char) 0xf4;
                        break; // GREEK CAPITAL LETTER OMEGA
                    case 0x00dc:
                        c = (char) 0xf5;
                        break; // LATIN CAPITAL LETTER U WITH DIAERESIS
                    case 0x00fc:
                        c = (char) 0xf5;
                        break; // LATIN SMALL LETTER U WITH DIAERESIS
                    case 0x03a3:
                        c = (char) 0xf6;
                        break; // GREEK CAPITAL LETTER SIGMA
                    case 0x03c0:
                        c = (char) 0xf7;
                        break; // GREEK SMALL LETTER PI
                    case 0x0304:
                        c = (char) 0xf8;
                        break; // COMBINING MACRON
                    case 0x00f7:
                        c = (char) 0xfd;
                        break; // DIVISION SIGN
                    default:
                    case 0x25a0:
                        c = (char) 0xff;
                        break; // BLACK SQUARE
                    }
            }

            // Special handling for 'x' followed by COMBINING MACRON
            if (c == (char) 0xf8) {
                if (!ks0066u.endsWith("x")) {
                    c = (char) 0xff; // BLACK SQUARE
                }

                if (ks0066u.length() > 0) {
                    ks0066u = ks0066u.substring(0, ks0066u.length() - 1);
                }
            }

            ks0066u += c;
        }

        return ks0066u;
    }

    private void lcdWriteSpeed() {
        try {
            lcdBricklet.writeLine((short) 0, (short) 0, "GPS - "
                    + String.format("%02d/%02d S Fix: %01d", satUsed, satView, fix));
            lcdBricklet.writeLine((short) 1, (short) 0,
                    utf16ToKS0066U(String.format("V   %02.1f m/s'        ", -1.111)));
            lcdBricklet.writeLine((short) 2, (short) 0,
                    utf16ToKS0066U(String.format("Dir %03.1f°        ", -180.1)));
            lcdBricklet.writeLine((short) 3, (short) 0,
                    utf16ToKS0066U(String.format("Alt %05.0f m        ", altitude)));

        } catch (TimeoutException | NotConnectedException ex) {
            Logger.getLogger(TfGps.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void lcdWriteLage() {
        try {
            lcdBricklet.clearDisplay();
            // ca. +/- 10 deg --> 10 cols
            short col = (short) (9.5 + roll / 1.0);
            if (col < 0) {
                col = 0;
            }
            if (col > 19) {
                col = 19;
            }
            //System.out.println("Line: " + line + " Col: " + col);
            char marker = 0x8;
            lcdBricklet.writeLine((short) 0, (short) 0, utf16ToKS0066U("Head:       " + gier + "°"));
            lcdBricklet.writeLine((short) 1, (short) 0, utf16ToKS0066U("Nick:       " + nick + "°"));
            lcdBricklet.writeLine((short) 2, (short) 0, utf16ToKS0066U("Roll:       " + roll + "°"));
            lcdBricklet.writeLine((short) 3, col, "" + marker);

        } catch (TimeoutException | NotConnectedException ex) {
            Logger.getLogger(TfGps.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void lcdWriteHelp() {
        try {
            lcdBricklet.writeLine((short) 0, (short) 0,
                    "Button 1: Help/Light");
            lcdBricklet.writeLine((short) 1, (short) 0,
                    "Button 2: Prev Mode ");
            lcdBricklet.writeLine((short) 2, (short) 0,
                    "Button 3: Next Mode ");
            lcdBricklet.writeLine((short) 3, (short) 0,
                    "Button 4: Exit      ");

        } catch (TimeoutException | NotConnectedException ex) {
            Logger.getLogger(TfGps.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void lcdWriteHeadline(LocalDateTime time) {
        try {
            lcdBricklet.writeLine((short) 0, (short) 0, "GPS - "
                    + String.format("%02d/%02d S Fix: %01d", satUsed, satView, fix));
            lcdBricklet.writeLine((short) 1, (short) 0, DateTimeUtil.format(time));

        } catch (TimeoutException | NotConnectedException ex) {
            Logger.getLogger(TfGps.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void lcdWriteCoord(double lat, double longi, char ns, char ew) {
        double latFract = lat % 1;
        double latDeg = lat - latFract;
        double longFract = longi % 1;
        double longDeg = longi - longFract;
        double latMin = latFract * 60.0;
        double longMin = longFract * 60.0;
        try {
            lcdBricklet.writeLine((short) 2, (short) 0,
                    utf16ToKS0066U(String.format("%c %02.0f° %02.3f'        ", ns, latDeg, latMin)));
            lcdBricklet.writeLine((short) 3, (short) 0,
                    utf16ToKS0066U(String.format("%c %02.0f° %02.3f'        ", ew, longDeg, longMin)));

        } catch (TimeoutException | NotConnectedException ex) {
            Logger.getLogger(TfGps.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        TfGps myGps = new TfGps();
        while (!exitProgram) {
            try {
                Thread.sleep(1000);
                myGps.cyclicUpdate();

            } catch (InterruptedException ex) {
                Logger.getLogger(TfGps.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
