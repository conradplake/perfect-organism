import gframe.engine.ImageRaster;

public final class FilterUtils {
	
	public static ImageRaster add(ImageRaster frame1, ImageRaster frame2) {
		return add(frame1, frame2, false);
	}

	public static ImageRaster add(ImageRaster frame1, ImageRaster frame2, boolean average) {
		ImageRaster result = new ImageRaster(frame1.getWidth(), frame1.getHeight());

		for (int y = 0; y < frame1.getHeight(); y++) {
			for (int x = 0; x < frame1.getWidth(); x++) {

				int rgb1 = frame1.getPixel(x, y);
				int a1 = (rgb1 >> 24) & 0xff;
				int r1 = (rgb1 >> 16) & 0xff;
				int g1 = (rgb1 >> 8) & 0xff;
				int b1 = (rgb1) & 0xff;

				int rgb2 = frame2.getPixel(x, y);
				int a2 = (rgb2 >> 24) & 0xff;
				int r2 = (rgb2 >> 16) & 0xff;
				int g2 = (rgb2 >> 8) & 0xff;
				int b2 = (rgb2) & 0xff;

				int newAlpha = a1 + a2;
				int newRed = r1 + r2;
				int newGreen = g1 + g2;
				int newBlue = b1 + b2;
				
				if(average) {
					newAlpha /= 2;
					newRed /= 2;
					newGreen /= 2;
					newBlue /= 2;
				}

				if (newAlpha > 255) {
					newAlpha = 255;
				}
				if (newRed > 255) {
					newRed = 255;
				}
				if (newGreen > 255) {
					newGreen = 255;
				}
				if (newBlue > 255) {
					newBlue = 255;
				}

				int newRgb = (((int) newAlpha & 0xFF) << 24) | (((int) newRed & 0xFF) << 16)
						| (((int) newGreen & 0xFF) << 8) | (((int) newBlue & 0xFF) << 0);
				result.setPixel(x, y, newRgb);
			}
		}

		return result;
	}

	public static ImageRaster filter(ImageRaster inputFrame, float[] filterKernel) {
		ImageRaster result = inputFrame.copy();

		int halfFilterLength = filterKernel.length / 2;

		for (int y = 0; y < inputFrame.getHeight(); y++) {
			for (int x = halfFilterLength; x < inputFrame.getWidth() - halfFilterLength; x++) {

				float newAlpha = 0;
				float newRed = 0;
				float newGreen = 0;
				float newBlue = 0;

				for (int i = 0; i < filterKernel.length; i++) {
					int rgb = inputFrame.getPixel(x + i - halfFilterLength, y);
					newAlpha += filterKernel[i] * ((rgb >> 24) & 0xff);
					newRed += filterKernel[i] * ((rgb >> 16) & 0xff);
					newGreen += filterKernel[i] * ((rgb >> 8) & 0xff);
					newBlue += filterKernel[i] * ((rgb) & 0xff);
				}
				int newRgb = (((int) newAlpha & 0xFF) << 24) | (((int) newRed & 0xFF) << 16)
						| (((int) newGreen & 0xFF) << 8) | (((int) newBlue & 0xFF) << 0);
				result.setPixel(x, y, newRgb);
			}
		}
		return result;
	}

	/**
	 * Runs the given matrix over the grid of pixels.
	 *
	 *
	 * Possible filter matrices: see e.g. gframe.engine.Toolbox.TPFILTER33
	 */
	public static ImageRaster filter(ImageRaster inputRaster, float[][] filterMtx) {

		int w = inputRaster.getWidth();
		int h = inputRaster.getHeight();

		int fmLX = filterMtx.length;
		int fmLX2 = fmLX / 2;

		int fmLY = filterMtx[0].length;
		int fmLY2 = fmLY / 2;

		ImageRaster result = new ImageRaster(w, h);

		for (int x = fmLX2; x < w - fmLX2; x++) {
			for (int y = fmLY2; y < h - fmLY2; y++) {

				float newRed = 0;
				float newGreen = 0;
				float newBlue = 0;

				for (int fx = 0; fx < fmLX; fx++) {
					for (int fy = 0; fy < fmLY; fy++) {

						int rgb = inputRaster.getPixel(x + fmLX2 - fx, y + fmLY2 - fy);
						newRed += filterMtx[fx][fy] * ((rgb >> 16) & 0xff);
						newGreen += filterMtx[fx][fy] * ((rgb >> 8) & 0xff);
						newBlue += filterMtx[fx][fy] * ((rgb) & 0xff);
					}
				}

				int newRgb = ((255 & 0xFF) << 24) | (((int) newRed & 0xFF) << 16) | (((int) newGreen & 0xFF) << 8)
						| (((int) newBlue & 0xFF) << 0);
				result.setPixel(x, y, newRgb);
			}
		}

		return result;
	}

}
