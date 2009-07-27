package gov.usgs.anss.query;

import gov.usgs.anss.seed.MiniSeed;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.GregorianCalendar;
//import gov.usgs.anss.util.*;

/**
 * TextOutputer, largely based on MSOutputer.java and SacOutputer.java.
 *
 * @author	richardg
 * @version	$Id$
 */
public class TextOutputer extends Outputer {

	public static final int WINSTON_NO_DATA = Integer.MIN_VALUE;	// chosen to be the same as Winston Waves.
//	public static final int SAC_UNDEFINED = -12345;		// this is the undefined value for the Sac data format.
	static {logger.finest("$Id$");}

    public void makeFile(String comp, String filename, String filemask, ArrayList<MiniSeed> blks,
            java.util.Date beg, double duration, String[] args) throws IOException {

        // Process the args for things that affect us
        if (blks.isEmpty()) {
            return;    // no data to save
        }
		int fill = WINSTON_NO_DATA;
        boolean nogaps = false;		// if true, do not generate a file if it has any gaps!

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-fill")) {
                fill = Integer.parseInt(args[i + 1]);
            }
            if (args[i].equals("-nogaps")) {
                nogaps = true;
            }
        }
        if (filemask.equals("%N")) {
            filename += ".txt";
        }
		logger.info("filename=" + filename);
        filename = filename.replaceAll("[__]", "_");
        final PrintWriter out = new PrintWriter(new FileOutputStream(filename), false);

        // Use the span to populate a sac file
        GregorianCalendar start = new GregorianCalendar();
        start.setTimeInMillis(beg.getTime());

        // build the zero filled area (either with exact limits or with all blocks)
        final ZeroFilledSpan span = new ZeroFilledSpan(blks, start, duration, fill);
        if (span.getRate() <= 0.00) {
            return;         // There is no real data to put in SAC
        }
		logger.fine("ZeroSpan=" + span.toString());

		GregorianCalendar spanStart = span.getStart();

		double currentTime = spanStart.getTimeInMillis();
		final double period = 1000.0 / span.getRate();
		int data;

		for (int i = 0; i < span.getNsamp(); i++) {
			data = span.getData(i);
			if (nogaps || data != fill) {
				out.println((long)Math.round(currentTime) + " " + span.getData(i));
			}
			currentTime += period;
		}


        out.close();
    }
}