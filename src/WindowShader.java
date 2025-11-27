import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import gframe.engine.AbstractShader;
import gframe.engine.ImageRaster;
import gframe.engine.Lightsource;
import gframe.engine.Particle;
import gframe.engine.Point3D;
import gframe.engine.RenderFace;
import gframe.engine.Toolbox;

public class WindowShader extends AbstractShader {

	private long lastTimeInMillis = 0;
	private long timePassedInMillis = 0;

	private ImageRaster texture;
	
	int textureWidth;
	int textureHeight;
	
	int windowPadding = 2;
	
	float speed = 1f;
	
	private List<Particle> stars;

	public WindowShader(Lightsource lightsource, int textureWidth, int textureHeight) {
		this(lightsource, textureWidth, textureHeight, 0.05f, 1f);
	}
	
	public WindowShader(Lightsource lightsource, int textureWidth, int textureHeight, float starPercentage, float speed) {
		super(lightsource);
		this.texture = new ImageRaster(textureWidth, textureHeight);
		this.textureWidth = texture.getWidth();
		this.textureHeight = texture.getHeight();
		this.speed = speed;

		stars = new ArrayList<>();
		for (int x = 1; x < textureWidth - 1; x++) {
			for (int y = 1; y < textureHeight - 1; y++) {
				if (Math.random() > starPercentage) {
					continue;
				}
				float depth = (float) Math.random();
				Color starColor = new Color(Math.max((int) (255 * depth), 0), Math.max((int) (255 * depth), 0),
						Math.max((int) (255 * depth), 0), 255);
				Particle star = new Particle(new Point3D(x, y, depth));
				star.setRgb(starColor.getRGB());
				stars.add(star);
				texture.setPixel(x, y, starColor.getRGB());
			}
		}
		
		lastTimeInMillis = System.currentTimeMillis();
	}

	@Override
	public void preShade(RenderFace renderFace) {

		super.preShade(renderFace);

		long currentTimeInMillis = System.currentTimeMillis();
		timePassedInMillis += (currentTimeInMillis - lastTimeInMillis);

		long timestepInMillis = 30;

		while (timePassedInMillis > timestepInMillis) {			
			for (Particle star : stars) {
				texture.setPixel((int) star.getOrigin().x, (int) star.getOrigin().y, Color.black.getRGB());
			}
			for (Particle star : stars) {
				//texture.setPixel((int) star.getOrigin().x, (int) star.getOrigin().y, Color.black.getRGB());
				
				float dx = star.getOrigin().z * 1f;
				star.getOrigin().x += dx * speed;
				edgeHandling(star);
				texture.setPixel((int) star.getOrigin().x, (int) star.getOrigin().y, star.getRgb());
			}

			timePassedInMillis -= timestepInMillis;
		}

		lastTimeInMillis = currentTimeInMillis;
	};

	private void edgeHandling(Particle p) {

		if (p.getOrigin().x < windowPadding) {
			p.getOrigin().x = textureWidth - 1 - windowPadding;
		} else if (p.getOrigin().x >= textureWidth - windowPadding) {
			p.getOrigin().x = windowPadding;
		}

		if (p.getOrigin().y < windowPadding) {
			p.getOrigin().y = textureHeight - 1 - windowPadding;
		} else if (p.getOrigin().y >= textureHeight - windowPadding) {
			p.getOrigin().y = windowPadding;
		}
	}

	@Override
	public int shade(RenderFace renderFace, float world_x, float world_y, float world_z, float normal_x, float normal_y,
			float normal_z, float texel_u, float texel_v, int screen_x, int screen_y) {

		float x = Toolbox.clamp(texel_u * (textureWidth), 0, textureWidth - 1);
		float y = Toolbox.clamp(texel_v * (textureHeight), 0, textureHeight - 1);

		int texel = 0;
		if(textureWidth > 0 && textureHeight > 0) {
			texel = texture.getPixel((int) x, (int) y);	
		}
		
		return super.shade(texel, world_x, world_y, world_z, normal_x, normal_y, normal_z);
	}

	@Override
	public boolean isPerPixelShader() {
		return true;
	}

}
