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
  * A class to encapsulate a BufferedImage.
  */
public class MircImage {

	static final Logger logger = Logger.getLogger(MircImage.class);

	BufferedImage bufferedImage = null;
	boolean isDicomImage = false;
	DicomObject dicomObject = null;
	File file = null;
	String formatName = "";
	int frame = 0;

	/**
	 * Class constructor; creates a new MircImage from a BufferedImage.
	 * @param bufferedImage the image.
	 */
	public MircImage(BufferedImage bufferedImage) {
		this.bufferedImage = bufferedImage;
		this.isDicomImage = false;
		this.dicomObject = null;
		this.file = null;
		this.frame = 0;
		this.formatName = "unknown";
	}

	/**
	 * Class constructor; creates a new MircImage from a DicomObject.
	 * @param dicomObject the DicomObject.
	 * @throws Exception if the DicomObject is not an image or if
	 * the image cannot be loaded.
	 */
	public MircImage(DicomObject dicomObject) throws Exception {
		if (!dicomObject.isImage())
			throw new Exception("DicomObject is not a supported image.");
		this.bufferedImage = dicomObject.getBufferedImage(0, false); //frame 0
		this.dicomObject = dicomObject;
		this.isDicomImage = true;
		this.file = dicomObject.getFile();
		this.frame = 0;
		this.formatName = "dicom";
	}

	/**
	 * Class constructor; creates a new MircImage from a FileObject.
	 * @param fileObject the FileObject.
	 * @throws Exception if the FileObject is not an image or if
	 * the image cannot be loaded.
	 */
	public MircImage(FileObject fileObject) throws Exception {
		this(fileObject.getFile());
	}

	/**
	 * Class constructor; creates a new MircImage from a File.
	 * @param imageFile the file containing the image.
	 * @throws Exception if the image cannot be created.
	 */
	public MircImage(File imageFile) throws Exception {
		if (!imageFile.exists())
			throw new IOException("File not found: "+imageFile);

		//First try to load it as a DicomObject so we can get the
		//filtering of the overlays to protect the JPEG converter.
		try {
			this.dicomObject = new DicomObject(imageFile);
			if (!dicomObject.isImage()) {
				throw new Exception("DicomObject is not a supported image.");
			}
			this.bufferedImage = dicomObject.getBufferedImage(0, false);
			this.isDicomImage = true;
			this.file = dicomObject.getFile();
			this.frame = 0;
			this.formatName = "dicom";
		}
		catch (Exception notDicomImage) {
			//Try to load the file as any other image type.
			//This will throw an IOException if it can't read
			//the file or return null if no decoder can be found.
			//In any case, if we aren't successful, throw an Exception.
			this.isDicomImage = false;
			this.dicomObject = null;
			getBufferedImage(imageFile, frame);
		}
	}

	private void getBufferedImage(File imageFile, int frame) throws Exception {
		if ((bufferedImage != null) && (this.frame == frame)) return;
		FileImageInputStream fiis = new FileImageInputStream(imageFile);
		ImageReader reader = getImageReader(fiis);
		if (reader != null) {
			try {
				reader.setInput(fiis);
				bufferedImage = reader.read(frame);
				formatName = reader.getFormatName();
			}
			catch (Exception ex) { }
			if (this.bufferedImage != null) this.file = imageFile;
		}
		fiis.close();
		if (reader == null)
			throw new IOException("An ImageReader cannot be be found for "+imageFile);
		if (this.bufferedImage == null)
			throw new IOException("Image cannot be decoded: "+imageFile);
	}

	//Get an ImageReader for an image file.
	private ImageReader getImageReader(FileImageInputStream fiis) {
		//Find out what service providers can handle this stream.
		Iterator<ImageReader> readers = ImageIO.getImageReaders(fiis);
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
	 * Get the File pointing to the data file from which the MircImage was created.
	 * If the MircImage is a DicomObject, the DicomObject's getFile() method is used
	 * to obtain the File so that the File will track renames done on the DicomObject.
	 * If the MircImage is not a DicomObject, the original File is returned. If the
	 * MircImage was not created from a data file, null is returned.
	 * @return the File pointing to the data file containing the MircImage, or null
	 * if the MircImage was not created from a data file.
	 */
	public File getFile() {
		if (dicomObject != null) return dicomObject.getFile();
		return file;
	}

	/**
	 * Rename the MircImage's data file. This method also changes
	 * the File returned by getFile() so that it points to the renamed
	 * data file. If the MircImage is a DicomObject, the DicomObject's
	 * renameTo(newFile) method is used so that it properly tracks
	 * the data file's location. If the MircImage was not created from
	 * a file or DicomObject, this method returns false.
	 * @param newFile the new name of the file.
	 * @return true if the rename was successful, false otherwise.
	 */
	public boolean renameTo(File newFile) {
		boolean ok;
		if (file == null) return false;
		if (dicomObject != null) ok = dicomObject.renameTo(newFile);
		else ok = file.renameTo(newFile);
		if (ok) file = newFile;
		return ok;
	}

	/**
	 * Get the flag indicating whether this image was created from a DicomObject.
	 * @return true if the image was created from a DicomObject; false otherwise.
	 */
	public boolean isDicomImage() {
		return isDicomImage;
	}

	/**
	 * Get the DicomObject corresponding to this MircImage.
	 * @return the DicomObject from which this image was created, or
	 * null if it was not created from a DicomObject.
	 */
	public DicomObject getDicomObject() {
		return dicomObject;
	}

	/**
	 * Check to see if this MircImage has an extension corresponding to an image
	 * that is supported by MIRC (jpeg, jpg, gif, png, tif, tiff, dcm).
	 * The test is not case sensitive.
	 * @return true if the file has an image extension; false otherwise.
	 */
	public boolean hasImageExtension() {
		return hasImageExtension(this.file);
	}

	/**
	 * Check to see if a file has an extension corresponding to an image
	 * that is supported by MIRC (jpeg, jpg, gif, png, tif, tiff, bmp, dcm).
	 * The test is not case sensitive.
	 * @return true if the file has an image extension; false otherwise.
	 */
	public static boolean hasImageExtension(File file) {
		return hasStandardImageExtension(file) ||
					hasNonStandardImageExtension(file) ||
						hasStandardDicomExtension(file);
	}

	/**
	 * Check to see if this MircImage has an extension corresponding
	 * to a standard browser-viewable image (jpeg, jpg, gif, png,
	 * bmp). The test is not case sensitive.
	 * @return true if the file has a standard image extension; false otherwise.
	 */
	public boolean hasStandardImageExtension() {
		return hasStandardImageExtension(this.file);
	}

	/**
	 * Check to see if a file has an extension corresponding
	 * to a standard browser-viewable image (jpeg, jpg, gif,
	 * png, bmp). The test is not case sensitive.
	 * @return true if the file has a standard image extension;
	 * false otherwise.
	 */
	public static boolean hasStandardImageExtension(File file) {
		String name = file.getName().toLowerCase().trim();
		name = name.substring(name.lastIndexOf(".") + 1);
		if (name.equals("jpg") || name.equals("jpeg") ||
			name.equals("gif") || name.equals("png")  ||
			name.equals("bmp")) return true;
		return false;
	}

	/**
	 * Check to see if this MircImage has an extension corresponding
	 * to an image that is not viewable in a standard browser.
	 * The test is not case sensitive.
	 * @return true if the file has a nonstandard image extension; false otherwise.
	 */
	public boolean hasNonStandardImageExtension() {
		return hasNonStandardImageExtension(this.file);
	}

	/**
	 * Check to see if a file has an extension corresponding
	 * to an image that is not viewable in a standard browser.
	 * The test is not case sensitive.
	 * @return true if the file has a nonstandard image extension; false otherwise.
	 */
	public static boolean hasNonStandardImageExtension(File file) {
		String name = file.getName().toLowerCase().trim();
		name = name.substring(name.lastIndexOf(".") + 1);
		if (name.equals("tif") || name.equals("tiff")) return true;
		return false;
	}

	/**
	 * Check to see if this MircImage has the standard DICOM extension
	 * (dcm). The test is not case sensitive.
	 * @return true if the file has the standard DICOM extension; false otherwise.
	 */
	public boolean hasStandardDicomExtension() {
		return hasStandardDicomExtension(this.file);
	}

	/**
	 * Check to see if a file has the standard DICOM extension
	 * (dcm). The test is not case sensitive.
	 * @return true if the file has the standard DICOM extension; false otherwise.
	 */
	public static boolean hasStandardDicomExtension(File file) {
		String name = file.getName().toLowerCase().trim();
		name = name.substring(name.lastIndexOf(".") + 1);
		if (name.equals("dcm")) return true;
		return false;
	}

	/**
	 * Get the format name of the MircImage. Note that the name may be
	 * "unknown", if the MircImage was constructed from a buffered image.
	 * @return the format name of the MircImage.
	 */
	public String getFormatName() {
		return formatName;
	}

	/**
	 * Get the width of the MircImage.
	 * @return the width of the image, or -1 if no image is loaded.
	 */
	public int getWidth() {
		if (bufferedImage == null) return -1;
		return bufferedImage.getWidth();
	}

	/**
	 * Get the height of the MircImage.
	 * @return the height of the image, or -1 if no image is loaded.
	 */
	public int getHeight() {
		if (bufferedImage == null) return -1;
		return bufferedImage.getHeight();
	}

	/**
	 * Get the number of columns of pixels in the MircImage.
	 * This method is the same as getWidth.
	 * @return the width of the image, or -1 if no image is loaded.
	 */
	public int getColumns() {
		return getWidth();
	}

	/**
	 * Get the number of rows of pixels in the MircImage.
	 * This method is the same as getHeight.
	 * @return the height of the image, or -1 if no image is loaded.
	 */
	public int getRows() {
		return getHeight();
	}

	/**
	 * Get the pixel bit-depth of the MircImage.
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
		int maxCubic = 1500; //The maximum dimension for which bicubic interpolation is done.
		try {
			//Check that all is well
			getBufferedImage(file, frame);
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
				atop = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			else
				atop = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);

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
	 * @return the dimensions of the JPEG that was create, or null if an error occurred.
	 */
	public Dimension saveAsJPEG(File file, int frame, int maxSize, int minSize) {
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
	 * @return the dimensions of the JPEG that was created, or null if an error occurred.
	 */
	public Dimension saveAsJPEG(File file, int frame, int maxSize, int minSize, int quality) {
		FileImageOutputStream out = null;
		ImageWriter writer = null;
		Dimension result = null;

		try {
			BufferedImage scaledImage = getScaledBufferedImage(frame, maxSize, minSize);
			result = new Dimension(scaledImage.getWidth(), scaledImage.getHeight());
			if (scaledImage == null) return null;

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
		catch (Exception ex) { result = null; logger.warn("Unable to save the image as a JPEG", ex); }
		finally {
			if (out != null) {
				try { out.flush(); out.close(); }
				catch (Exception ignore) { }
			}
			if (writer != null) writer.dispose();
		}
		return result;
	}

	//****************************************
	//Debugging code to trace slow performance
	//****************************************
	static long startTime = 0L;
	void logTime(String text) {
		long delta = System.currentTimeMillis() - startTime;
		logger.warn(delta + ": "+text);
	}

	/**
	 * Save the image as a square GIF Icon for the File Service.
	 * This method is specific to the standard GIF icon files
	 * used by the File Service. Using it on other images may
	 * produce interesing results.
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
		catch (Exception e) {
			return false;
		}
		return true;
	}
}
