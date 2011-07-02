package mirc.util;

import java.io.File;

import org.apache.batik.apps.rasterizer.DestinationType;
import org.apache.batik.apps.rasterizer.SVGConverter;
import org.apache.batik.apps.rasterizer.SVGConverterException;
import org.apache.log4j.Logger;

/**
 * Utility class for converting SVG files to other image formats
 * using the batik framework.
 * @author RBoden
 */
public class SvgUtil {

	static final Logger logger = Logger.getLogger(SvgUtil.class);

	/**
	 * Save an SVG file as a jpg. The converted file is placed in the outputDirectory
	 * with a name equal to that of the svg file, but with the extension ".jpg".
	 * @param svgFile the SVG file to be converted.
	 * @param outputDirectory the output directory into which to place the converted file.
	 */
	public static void saveAsJPEG(File svgFile, File outputDirectory) throws SVGConverterException {
		SVGConverter converter = new SVGConverter();
		String[] source = { svgFile.toString() };
		converter.setSources(source);
		converter.setDst(outputDirectory);
		converter.setQuality(.99f);
		converter.setDestinationType(DestinationType.JPEG);
		converter.execute();
	}

}
