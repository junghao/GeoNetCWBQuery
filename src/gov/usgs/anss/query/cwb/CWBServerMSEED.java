/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.usgs.anss.query.cwb;

import gov.usgs.anss.edge.IllegalSeednameException;
import gov.usgs.anss.query.EdgeQueryOptions;
import gov.usgs.anss.query.NSCL;
import gov.usgs.anss.seed.MiniSeed;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 *
 * @author geoffc
 */
public class CWBServerMSEED {

    static DecimalFormat df6 = new DecimalFormat("000000");
    private static final Logger logger = Logger.getLogger(CWBServerMSEED.class.getName());
    private static DateTimeFormatter hmsFormat = ISODateTimeFormat.time().withZone(DateTimeZone.forID("UTC"));


    static {
        logger.fine("$Id: CWBServerImpl.java 1806 2010-02-03 02:59:12Z geoffc $");
    }
    private String host;
    private int port;
    private Socket ds = null;
    private InputStream inStream;
    private OutputStream outStream;
    private LinkedBlockingQueue<MiniSeed> incomingMiniSEED;
    private NSCL newNSCL = null;
    private NSCL lastNSCL = null;

    public CWBServerMSEED(String host, int port, DateTime begin, Double duration, NSCL nscl) {
        this.host = host;
        this.port = port;

        // particularly for the DCC we want this program to not error out if we cannot connect to the server
        // So make sure we can connect and print messages

        while (ds == null) {
            try {
                ds = new Socket(this.host, this.port);
            } catch (IOException e) {
                ds = null;
                if (e != null) {
                    if (e.getMessage() != null) {
                        if (e.getMessage().indexOf("Connection refused") >= 0) {
                            logger.warning("Got a connection refused. " + this.host + "/" + this.port + "  Is the server up?  Wait 20 and try again");
                        }
                    } else {
                        logger.warning("Got IOError opening socket to server e=" + e);
                    }
                } else {
                    logger.warning("Got IOError opening socket to server e=" + e);
                }
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException ex) {
                    logger.log(Level.FINE, "sleep interrupted.", ex);
                }
            }
        }
        try {
            inStream = ds.getInputStream();
            outStream = ds.getOutputStream();
            outStream.write(CWBQueryFormatter.miniSEED(begin, duration, nscl).getBytes());
        } catch (IOException ex) {
            Logger.getLogger(CWBServerMSEED.class.getName()).log(Level.SEVERE, null, ex);
        }

        incomingMiniSEED = new LinkedBlockingQueue<MiniSeed>();
    }

    public TreeSet<MiniSeed> query(EdgeQueryOptions options) {

        TreeSet<MiniSeed> blks = new TreeSet<MiniSeed>();

        byte[] b = new byte[4096];
        try {
            read:
            while (read(inStream, b, 0, options.gapsonly ? 64 : 512)) {
                MiniSeed ms = null;
                // It doens't look like the GeoNet CWB server actually does this
                // but I'm going to leave this in anyway.
                if (b[0] == '<' && b[1] == 'E' && b[2] == 'O' && b[3] == 'R' && b[4] == '>') {
                    logger.fine("EOR found");
                } else {

                    try {
                        ms = new MiniSeed(b);
                    } catch (IllegalSeednameException ex) {
                        Logger.getLogger(CWBServerMSEED.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    if (!options.gapsonly && ms.getBlockSize() != 512) {
                        read(inStream, b, 512, ms.getBlockSize() - 512);

                        try {
                            ms = new MiniSeed(b);
                        } catch (IllegalSeednameException ex) {
                            Logger.getLogger(CWBServerMSEED.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                }

                if (ms != null) {

                    // This sets up the NSCL on the very first miniSEED block
                    if (lastNSCL == null) {
                        lastNSCL = NSCL.stringToNSCL(ms.getSeedName());
                    }

                    newNSCL = NSCL.stringToNSCL(ms.getSeedName());

                    if (newNSCL.equals(lastNSCL)) {
                        incomingMiniSEED.add(ms);
                        lastNSCL = newNSCL;
                    } else {
                        incomingMiniSEED.drainTo(blks);
                        incomingMiniSEED.add(ms);
                        lastNSCL = newNSCL;
                        break read;
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(CWBServerMSEED.class.getName()).log(Level.SEVERE, null, ex);
        }

        // This is triggered for the last channel off the stream.
        if (blks.isEmpty()) {
            incomingMiniSEED.drainTo(blks);
        }

        return blks;
    }

    public boolean hasNext() {
        if (lastNSCL == null) {
            return true;
        }
        return !incomingMiniSEED.isEmpty();
    }

    public static boolean read(InputStream in, byte[] b, int off, int l)
            throws IOException {
        int len;
        while ((len = in.read(b, off, l)) > 0) {
            off += len;
            l -=
                    len;
            if (l == 0) {
                return true;
            }

        }
        return false;
    }
}
