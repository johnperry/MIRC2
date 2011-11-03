/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.util;

import java.io.*;
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import org.rsna.util.XmlUtil;
import org.w3c.dom.*;

/**
 * A class containing a static method for serializing a
 * BufferedImage to bytes. The result can only be
 * deserialized by the BufferedImageDeserializer class.
 */
public class BufferedImageSerializer {

	ByteArrayOutputStream baos;

	/**
	 * Serialize a BufferedImage to a byte array.
	 * @param image the image
	 * @return the byte array containing the bytes of the image,
	 * preceded by an XML structure containing the information
	 * necessary to deserialize the bytes
	 */
	public static byte[] getByteArray(BufferedImage image) {
		return getByteStream(image).toByteArray();
	}

	/**
	 * Serialize a BufferedImage to a ByteArrayOutputStream.
	 * @param image the image
	 * @return the byte stream containing the bytes of the image,
	 * preceded by an XML structure containing the information
	 * necessary to deserialize the bytes
	 */
	public static ByteArrayOutputStream getByteStream(BufferedImage image) {

		int imgType = image.getType();
		int imgWidth = image.getWidth();
		int imgHeight = image.getHeight();
		boolean imgIsAlphaPremultiplied = image.isAlphaPremultiplied();

		String[] propnames = image.getPropertyNames();
		if (propnames == null) propnames = new String[0];

		ColorModel cm = image.getColorModel();
		int cmPixelSize = cm.getPixelSize();
		String cmClass = cm.getClass().getName();
		boolean cmHasAlpha = cm.hasAlpha();
		boolean cmIsAlphaPremultiplied = cm.isAlphaPremultiplied();
		int cmTransparency = cm.getTransparency();
		int cmTransferType = cm.getTransferType();
		int[] cmComponentSize = cm.getComponentSize();

		ColorSpace cs = cm.getColorSpace();
		int csType = cs.getType();
		int csNumComponents = cs.getNumComponents();

		WritableRaster raster = image.getRaster();
		String rClass = raster.getClass().getName();
		int rNumBands = raster.getNumBands();

		SampleModel sm = raster.getSampleModel();
		String smClass = sm.getClass().getName();
		int smDataType = sm.getDataType();
		int smNumBands = sm.getNumBands();
		int smHeight = sm.getHeight();
		int smWidth = sm.getWidth();

		DataBuffer db = raster.getDataBuffer();
		String dbClass = db.getClass().getName();
		int dbDataType = db.getDataType();
		int dbNumBanks = db.getNumBanks();
		int dbSize = db.getSize();

		try {
			Document doc = XmlUtil.getDocument();

			Element root = doc.createElement("BufferedImage");
			doc.appendChild(root);
			root.setAttribute("height", imgHeight+"");
			root.setAttribute("width", imgWidth+"");
			root.setAttribute("isRasterPremultiplied", imgIsAlphaPremultiplied+"");

			Element colorModel = doc.createElement("ColorModel");
			root.appendChild(colorModel);
			colorModel.setAttribute("class", cmClass);
			colorModel.setAttribute("pixelSize", cmPixelSize+"");

			//Element

			Element props = doc.createElement("Properties");
			root.appendChild(props);
			for (String name : propnames) {
				Element prop = doc.createElement("Property");
				props.appendChild(prop);
				prop.setAttribute("name", name);
				prop.setAttribute("value", image.getProperty(name).toString());
			}
			//*********
		}
		catch (Exception ex) { }
		return null; //***********
	}

}
