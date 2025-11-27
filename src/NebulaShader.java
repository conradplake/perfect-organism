import gframe.engine.AbstractShader;
import gframe.engine.Lightsource;
import gframe.engine.RenderFace;
import gframe.engine.Toolbox;
import gframe.engine.generator.NoiseGenerator;

public class NebulaShader extends AbstractShader {

	public NebulaShader(Lightsource lightsource) {
		super(lightsource);
	}

	@Override
	public int shade(RenderFace renderFace, float world_x, float world_y, float world_z, float normal_x, float normal_y,
			float normal_z, float texel_u, float texel_v, int screen_x, int screen_y) {

		float noise = (float) NoiseGenerator.improvedPerlinNoise(world_x * 0.5, world_y * 0.5, world_z * 0.5);

		float gray = (Math.abs(1f * (float) Math.sin(0.5f * world_x + noise * 0.5)));
		gray = (float) Toolbox.map(gray, 0, 1, 0, 4);

		int r = (int) (gray * 7 + 3);
		int g = (int) (gray * 7 + 5);
		int b = (int) (gray * 17 + 10);

		return ((renderFace.getColor().getAlpha() & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
	}

	@Override
	public boolean isPerPixelShader() {
		return true;
	}

}