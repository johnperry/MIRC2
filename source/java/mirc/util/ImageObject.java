/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.util;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.Iterator;
import javax.imageio.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.*;
import javax.imageio.stream.FileImageInputStream;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.objects.FileObject;
import org.shetline.io.GIFOutputStream;

/**
  * A class to encapsulate a generic image.
  */
public class ImageObject {

	static final Logger logger = Logger.getLogger(ImageObject.class);

	File file = null;
	String resource = null;
	BufferedImage bufferedImage = null;
	String formatName = "";
	boolean isDicom = false;
	int numberOfFrames = 0;
	int frame = 0;

	/**
	 * Class constructor; creates a new MircImage from a File.
	 * @param file the file containing the image.
	 */
	public ImageObject(File file) throws Exception {
		this.file = file;
		getBufferedImage(0);
	}

	/**
	 * Class constructor; creates a new MircImage from a resource path.
	 * @param resource the path to the resource containing the image.
	 */
	public ImageObject(String resource) throws Exception {
		this.resource = resource;
		getBufferedImage(0);
	}

	/**
	 * Get a frame from this image.
	 * @param frame the frame number.
	 * @return the BufferedImage for the frame, or null
	 * if the frame cannot be obtained.
	 * @throws Exception if the frame cannot be obtained.
	 */
	private BufferedImage getBufferedImage(int frame) throws Exception {
		//See if we already have the frame.
		if ((bufferedImage != null) && (this.frame == frame)) return bufferedImage;

		//No; get an ImageInputStream for the image.
		bufferedImage = null;
		ImageInputStream iis = null;
		if (file != null) {
			iis = new FileImageInputStream(file);
		}
		else {
			InputStream ris = getClass().getResourceAsStream(resource);
			iis = ImageIO.createImageInputStream(ris);
		}

		//Get the best reader for the image
		ImageReader reader = getImageReader(iis);
		if (reader != null) {
			try {
				reader.setInput(iis);
				bufferedImage = reader.read(frame);
				formatName = reader.getFormatName();
				numberOfFrames = reader.getNumImages(false);
			}
			catch (Exception ex) { bufferedImage = null; }
		}
		try { iis.close(); }
		catch (Exception ignore) { }
		if (bufferedImage == null) throw new Exception("Unable to find the the requested frame.");
		return bufferedImage;
	}

	//Get an ImageReader for an ImagerInputStream.
	private ImageReader getImageReader(ImageInputStream iis) {

		//Find out what service providers can handle this stream.
		Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

		//Because dcm4che seems to report that it can read anything, but
		//it really only reads DICOM images, we will supply the first
		//non-DICOM reader that we find, and only supply a DICOM reader if
		//no other readers can be found.
		ImageReader dicomReader = null;
		while (readers.hasNext()) {
			ImageReader reader = readers.next();
			if (!reader.toString().contains("dcm4che")) return reader;
			dicomReader = reader;
		}
		return dicomReader;
	}

	/**
	 * Check to see if this ImageObject has an extension corresponding to an image
	 * that is supported by MIRC (jpeg, jpg, gif, png, tif, tiff, dcm).
	 * The test is not case sensitive.
	 * @return true if the file or resource path has an image extension; false otherwise.
	 */
	public boolean hasImageExtension() {
		String name = (file != null) ? file.getName() : resource;
		return hasImageExtension(name);
	}

	/**
	 * Check to see if a name has an extension corresponding to an image
	 * that is supported by MIRC (jpeg, jpg, gif, png, tif, tiff, bmp, dcm).
	 * The test is not case sensitive.
	 * @return true if the file has an image extension; false otherwise.
	 */
	public static boolean hasImageExtension(String name) {
		return hasStandardImageExtension(name) ||
					hasNonStandardImageExtension(name) ||
						hasStandardDicomExtension(name);
	}

	/**
	 * Check to see if this MircImage has an extension corresponding
	 * to a standard browser-viewable image (jpeg, jpg, gif, png,
	 * bmp). The test is not case sensitive.
	 * @return true if the file has a standard image extension; false otherwise.
	 */
	public boolean hasStandardImageExtension() {
		String name = (file != null) ? file.getName() : resource;
		return hasStandardImageExtension(name);
	}

	/**
	 * Check to see if a name has an extension corresponding
	 * to a standard browser-viewable image (jpeg, jpg, gif,
	 * png, bmp). The test is not case sensitive.
	 * @return true if the file or resource has a standard
	 * image extension; false otherwise.
	 */
	public static boolean hasStandardImageExtension(String name) {
		name = name.toLowerCase();
		if (name.endsWith(".jpg") || name.endsWith(".jpeg") ||
			name.endsWith(".gif") || name.endsWith(".png")  ||
			name.endsWith(".bmp")) return true;
		return false;
	}

	/**
	 * Check to see if this ImageObject has an extension corresponding
	 * to an image that is not viewable in a standard browser.
	 * The test is not case sensitive.
	 * @return true if the file or resource has a nonstandard
	 * image extension; false otherwise.
	 */
	public boolean hasNonStandardImageExtension() {
		String name = (file != null) ? file.getName() : resource;
		return hasNonStandardImageExtension(name);
	}

	/**
	 * Check to see if a name has an extension corresponding
	 * to an image that is not viewable in a standard browser.
	 * The test is not case sensitive.
	 * @return true if the file has a nonstandard image extension; false otherwise.
	 */
	public static boolean hasNonStandardImageExtension(String name) {
		name = name.toLowerCase();
		if (name.endsWith(".tif") || name.endsWith(".tiff")) return true;
		return false;
	}

	/**
	 * Check to see if this ImageObject has the standard DICOM extension
	 * (dcm). The test is not case sensitive.
	 * @return true if the file has the standard DICOM extension; false otherwise.
	 */
	public boolean hasStandardDicomExtension() {
		String name = (file != null) ? file.getName() : resource;
		return hasStandardDicomExtension(name);
	}

	/**
	 * Check to see if a name has the standard DICOM extension
	 * (dcm). The test is not case sensitive.
	 * @return true if the file has the standard DICOM extension; false otherwise.
	 */
	public static boolean hasStandardDicomExtension(String name) {
		name = name.toLowerCase();
		if (name.endsWith(".dcm")) return true;
		return false;
	}

	/**
	 * See if this ImageObject is a DICOM image.
	 * @return the format name of the ImageObject.
	 */
	public boolean isDicom() {
		return isDicom;
	}

	/**
	 * Get the format name of the ImageObject. Note that the name may be
	 * "unknown", if the ImageObject was constructed from a buffered image.
	 * @return the format name of the ImageObject.
	 */
	public String getFormatName() {
		return formatName;
	}

	/**
	 * Get the number of frames in the ImageObject.
	 * @return the number of frames in the ImageObject, or -1 if the number is unknown.
	 */
	public int getNumberOfFrames() {
		return numberOfFrames;
	}

	/**
	 * Get the width of the ImageObject.
	 * @return the width of the image, or -1 if no image is loaded.
	 */
	public int getWidth() {
		if (bufferedImage == null) return -1;
		return bufferedImage.getWidth();
	}

	/**
	 * Get the height of the ImageObject.
	 * @return the height of the image, or -1 if no image is loaded.
	 */
	public int getHeight() {
		if (bufferedImage == null) return -1;
		return bufferedImage.getHeight();
	}

	/**
	 * Get the number of columns of pixels in the ImageObject.
	 * This method is the same as getWidth.
	 * @return the width of the image, or -1 if no image is loaded.
	 */
	public int getColumns() {
		return getWidth();
	}

	/**
	 * Get the number of rows of pixels in the ImageObject.
	 * This method is the same as getHeight.
	 * @return the height of the image, or -1 if no image is loaded.
	 */
	public int getRows() {
		return getHeight();
	}

	/**
	 * Get the pixel bit-depth of the ImageObject.
	 * @return the bit-depth of the pixels, or -1 if no image is loaded.
	 */
	public int getPixelSize() {
		if (bufferedImage == null) return -1;
		return bufferedImage.getColorModel().getPixelSize();
	}

	/**
	 * Get a BufferedImage scaled in accordance with the size rules for saveAsJPEG.
	 * @param maxSize the maximum width of the created JPEG;
	 * @param minSize the minimum width of the created JPEG;
	 * @return a Buffered Image scaled to the required size, or null if the image
	 * could not be created.
	 */
	public BufferedImage getScaledBufferedImage(int frame, int maxSize, int minSize) {
		int maxCubic = 1100; //The maximum dimension for which bicubic interpolation is done.
		try {
			//Check that all is well
			getBufferedImage(frame);
			if (bufferedImage == null) return null;
			int width = bufferedImage.getWidth();
			int height = bufferedImage.getHeight();
			if (minSize > maxSize) minSize = maxSize;

			int pixelSize = getPixelSize();

			//See if we need to do anything at all
			if ((pixelSize == 24) && (minSize <= width) && (width <= maxSize)) return bufferedImage;

			// Set the scale.
			double scale;
			double minScale = (double)minSize/(double)width;
			double maxScale = (double)maxSize/(double)width;

			if (width >= minSize)
				scale = (width > maxSize) ? maxScale : 1.0D;
			else
				scale = minScale;

			// Set up the transform
			AffineTransform at = AffineTransform.getScaleInstance(scale,scale);
			AffineTransformOp atop;
			if ((pixelSize == 8) || (width > maxCubic) || (height > maxCubic) )
				atop = new AffineTransformOp(at,AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			else
				atop = new AffineTransformOp(at,AffineTransformOp.TYPE_BICUBIC);

			// Make a destination image
			BufferedImage scaledImage =
							new BufferedImage(
									(int)(width*scale),
									(int)(height*scale),
									BufferedImage.TYPE_INT_RGB);

			// Paint the transformed image.
			Graphics2D g2d = scaledImage.createGraphics();
			g2d.drawImage(bufferedImage, atop, 0, 0);
			g2d.dispose();
			return scaledImage;
		}
		catch (Exception e) {
			logger.warn("Unable to get Scaled Buffered Image",e);
			return null;
		}
	}

	/**
	 * Save the image as a JPEG, scaling it to a specified size
	 * and using the default quality setting.
	 * @param file the file into which to write the encoded image.
	 * @param maxSize the maximum width of the created JPEG;
	 * @param minSize the minimum width of the created JPEG;
	 * @return the dimensions of the JPEG that was created.
	 */
	public boolean saveAsJPEG(File file, int frame, int maxSize, int minSize) {
		return saveAsJPEG(file, frame, maxSize, minSize, -1);
	}

	/**
	 * Save the specified frame as a JPEG, scaling it to a specified size
	 * and using the specified quality setting.
	 * @param file the file into which to write the encoded image.
	 * @param frame the frame to save (the first frame is zero).
	 * @param maxSize the maximum width of the created JPEG.
	 * @param minSize the minimum width of the created JPEG.
	 * @param quality the quality parameter, ranging from 0 to 100;
	 * a negative value uses the default setting supplied by by ImageIO.
	 * @return true if the operation was successful; false otherwise.
	 */
	public boolean saveAsJPEG(File file, int frame, int maxSize, int minSize, int quality) {
		FileImageOutputStream out = null;
		ImageWriter writer = null;
		boolean result = true;
		try {
			BufferedImage scaledImage = getScaledBufferedImage(frame, maxSize, minSize);
			if (scaledImage == null) return false;

			// JPEG-encode the image and write it in the specified file.
			writer = ImageIO.getImageWritersByFormatName("jpeg").next();
			ImageWriteParam iwp = writer.getDefaultWriteParam();
			if (quality >= 0) {
				quality = Math.min(quality,100);
				float fQuality = ((float)quality) / 100.0F;
				iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				iwp.setCompressionQuality(fQuality);
			}
			out = new FileImageOutputStream(file);
			writer.setOutput(out);
			IIOImage image = new IIOImage(scaledImage, null, null);
			writer.write(null, image, iwp);
		}
		catch (Exception ex) { result = false; logger.warn("Unable to save the image as a JPEG", ex); }
		finally {
			if (out != null) {
				try { out.flush(); out.close(); }
				catch (Exception ignore) { }
			}
			if (writer != null) writer.dispose();
		}
		return result;
	}

	/**
	 * Save the image as a square GIF Icon.
	 * This method is specific to the standard GIF icon files
	 * used by the MIRC File Service. Using it on other images
	 * may produce interesing results.
	 * @param file the file into which to write the icon.
	 * @param size the height and width of the created GIF;
	 * @param text the text caption to be written near the bottom of the icon.
	 * @return true if the operation was successful; false otherwise.
	 */
	public boolean saveAsIconGIF(File file, int size, String text) {
		try {
			int height = size;
			int width = size;

			// Make a transparent color that is in the GIF encoder's standard256 table
			Color transparent = new Color(0,0,17);

			// Create an image buffer on which to paint
			BufferedImage outImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			// Get an identity transform
			AffineTransform at = new AffineTransform();

			// Paint the input image on the buffer
			Graphics2D g2d = outImage.createGraphics();
			g2d.setColor(transparent);
			g2d.fillRect(0,0,width,height);
			g2d.drawRenderedImage(bufferedImage, at);

			// Paint the text on the buffer at the bottom
			g2d.setColor(Color.black);
			FontMetrics fm = g2d.getFontMetrics();
			int descent = fm.getDescent();
			int leading = fm.getLeading();
			int baseline = descent + leading;
			int lineHeight = fm.getHeight();
			int bottom = height - 6;
			int left = 6;
			int right = width - 10;
			int lineWidth = right - left;
			g2d.setClip(left,bottom-2*lineHeight-1,right,bottom);
			int k = text.lastIndexOf(".");
			if (k >= 0) {
				String name = text.substring(0,k);
				int w = fm.stringWidth(name);
				g2d.drawString(name,left,bottom-lineHeight-baseline);
				name = text.substring(k);
				w = fm.stringWidth(name);
				g2d.drawString(name,right-w,bottom-baseline);
			}
			else g2d.drawString(text,left,bottom-baseline);

			// GIF-encode the image and write to file.
			OutputStream out = new BufferedOutputStream(new FileOutputStream(file));

			int status =
				GIFOutputStream.writeGIF(
					out, outImage, GIFOutputStream.STANDARD_256_COLORS, transparent);

			out.close();
			if (status != GIFOutputStream.NO_ERROR) return false;
		}
		catch (Exception e) { return false; }
		return true;
	}
}
