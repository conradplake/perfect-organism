import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

import gframe.app.DoubleBufferedFrame;
import gframe.engine.Camera;
import gframe.engine.Engine3D;
import gframe.engine.FlatShader;
import gframe.engine.ImageRaster;
import gframe.engine.Lightsource;
import gframe.engine.Material;
import gframe.engine.Matrix3D;
import gframe.engine.Model3D;
import gframe.engine.PhongShader;
import gframe.engine.Point3D;
import gframe.engine.Toolbox;
import gframe.engine.camera.TripodCamera;
import gframe.engine.generator.Model3DGenerator;
import gframe.engine.generator.NoiseGenerator;
import gframe.engine.timing.FadeOut;
import gframe.engine.timing.FadeOutFadeIn;
import gframe.engine.timing.Rotate;
import gframe.engine.timing.Timed;
import gframe.engine.timing.Timer;

public final class Main extends DoubleBufferedFrame {

	private int screenX;
	private int screenY;

	private Engine3D engine;
	private Camera camera;
	private Lightsource lightsource;

	private Model3D alienHead;

	private boolean initialized;

	private ImageRaster lightsFrame;
	private ImageRaster mainFrame;

	private int mainSegment = 0;
	private int lightSegment = 1;

	private Synthesizer synth;

	static float[] filterKernel = new float[9];
	static {
		for (int i = 0; i < filterKernel.length; i++) {
			filterKernel[i] = 1f / (float) filterKernel.length;
		}
	}

	public static void main(String[] args) throws MidiUnavailableException {
		int screenX = 640;
		int screenY = 400;
		if (args.length == 2) {
			screenX = Integer.parseInt(args[0]);
			screenY = Integer.parseInt(args[1]);
		}

		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		DisplayMode displayMode = findBestDisplayMode(device, screenX, screenY);
		System.out.println("Using display mode " + displayMode.getWidth() + " x " + displayMode.getHeight());

		new Main(displayMode.getWidth(), displayMode.getHeight()).start(device, displayMode);
	}

	private static DisplayMode findBestDisplayMode(GraphicsDevice device, int width, int height) {
		// first check for requested size
		for (DisplayMode mode : device.getDisplayModes()) {
			if (mode.getWidth() == width && mode.getHeight() == height && mode.getBitDepth() == 32
					&& mode.getRefreshRate() == 60) {
				return mode;
			}
		}

		// check for alternatives
		for (DisplayMode mode : device.getDisplayModes()) {
			if (mode.getWidth() == 640 && mode.getHeight() == 400 && mode.getBitDepth() == 32
					&& mode.getRefreshRate() == 60) {
				return mode;
			}
		}
		for (DisplayMode mode : device.getDisplayModes()) {
			if (mode.getWidth() == 640 && mode.getHeight() == 480 && mode.getBitDepth() == 32
					&& mode.getRefreshRate() == 60) {
				return mode;
			}
		}
		for (DisplayMode mode : device.getDisplayModes()) {
			if (mode.getWidth() == 800 && mode.getHeight() == 600 && mode.getBitDepth() == 32
					&& mode.getRefreshRate() == 60) {
				return mode;
			}
		}

		// return current if nothing else fits
		return device.getDisplayMode();
	}

	public Main(int screenX, int screenY) throws MidiUnavailableException {
		super();
		this.screenX = screenX;
		this.screenY = screenY;
		setBackground(Color.BLACK);
		setResizable(false);
		lightsFrame = new ImageRaster(screenX, screenY);
		mainFrame = new ImageRaster(screenX, screenY);
		synth = MidiSystem.getSynthesizer();
		synth.open();
	}

	private void start(GraphicsDevice device, DisplayMode displayMode) {
		if (device.isFullScreenSupported()) {
			this.setUndecorated(true);
			this.setResizable(false);
			device.setFullScreenWindow(this);
			device.setDisplayMode(displayMode);
		} else {
			setSize(screenX, screenY);
			setLocation(0, 0);
			setLayout(null);
			setVisible(true);
		}

		enableEvents(AWTEvent.KEY_EVENT_MASK);

		initEngine();

		int repeats = 0;

		for (int i = 0; i < 1 + repeats; i++) {
			initScene1();
			startScene1a();
			startScene1b();

			initScene2();
			startScene2();

			initScene3();
			startScene3();

			initScene4();
			startScene4();

			initScene5();
			startScene5();

			initScene6();
			startScene6();
		}

		exit();
	}

	private void initEngine() {
		engine = new Engine3D(2, screenX, screenY);

		Lightsource.AMBIENT_LIGHT_INTENSITY = 0;
		lightsource = new Lightsource(0, 0, 0, Color.WHITE, Lightsource.MAX_INTENSITY);
		engine.setLightsource(lightsource);

		camera = new TripodCamera();
		engine.setCamera(camera);

		engine.setDefaultShader(new PhongShader(lightsource));
	}

	private void initScene1() {

		initialized = false;

		engine.clear();
		camera.reset();

		camera.setOrigin(new Point3D());
		camera.move(0, 0, 100);
		camera.rotate(0, 20, 0);

		lightsource.setIntensity(Lightsource.MAX_INTENSITY);
		lightsource.x = 0;
		lightsource.y = 0;
		lightsource.z = 0;
		lightsource.move(0, 0, 400);
		lightsource.setShadowsEnabled(false);
		lightsource.setAddAttenuation(true);
		lightsource.setLightAttenuationFalloffFactor(0.000001f);

		int tubeLength = 300;
		int tubeRadius = 100;
		float tubeScaleFactorZ = 10f;
		float tubeScaleFactorY = 0.5f;
		Color tubeColor = Color.orange;
		Material tubeMaterial = Material.GOLD;

		Model3D tube = ModelUtils.createTube(tubeLength, tubeRadius, tubeColor);
		tube.setMaterial(tubeMaterial);
		tube.scale(1, tubeScaleFactorY, tubeScaleFactorZ);
		Model3DGenerator.invertFaces(tube);
		engine.register(tube);

		List<Collection<Model3D>> lightsBucketList = new ArrayList<>();

		// panels left
		for (int i = 7; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.white);
		}
		for (int i = 8; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.white);
		}
		for (int i = 9; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.white);
		}
		for (int i = 10; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.white);
		}
		for (int i = 11; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.white);
		}
		for (int i = 12; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.gray);
		}
		for (int i = 13; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.gray);
		}
		for (int i = 14; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.white);
		}
		for (int i = 15; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.red);
		}
		for (int i = 16; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.white);
		}
		for (int i = 17; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.white);
		}
		for (int i = 18; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.white);
		}
		for (int i = 19; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.red);
		}
		for (int i = 20; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.white);
		}
		for (int i = 21; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.gray);
		}
		for (int i = 22; i < tube.getFaces().size() / 3; i += (1 * 36)) {
			tube.getFaces().get(i).setColor(Color.darkGray);
		}

		// window lights left
		int counter = 0;
		for (int i = 17; i < tube.getFaces().size() / 10; i += (2 * 36)) {
			Model3D wallLight = new Model3D();
			tube.getFaces().get(i).setColor(Color.black);
			for (Point3D p : tube.getFaces().get(i).getVertices()) {
				wallLight.addVertex(p.copy());
			}
			wallLight.stretchFace(0, 1, 2, 3, Color.white);
			wallLight.isVisible = false;
			wallLight.setMaterial(Material.DEFAULT);
			engine.register(wallLight, lightSegment);

			PhongShader frameShader = new PhongShader(lightsource);
			frameShader.setAddSpecularity(false);
			Model3D frame = ModelUtils.createFrame(1, 99, 8, Color.gray);
			frame.setMaterial(Material.BLACK_RUBBER);
			frame.move(-98.8f, 4, 50 + (200 * counter++));
			frame.rotate(0, 90, 0);
			frame.rotate(10, 0, 0);
			engine.register(frame, lightSegment);
			engine.setModelShader(frame, frameShader);

			Point3D lightSourceOrigin = wallLight.getFaces().get(0).getCentroid().copy();
			lightSourceOrigin.move(300, 0, 0);

			float textureScaleFactor;
			if (i == 17) {
				textureScaleFactor = 1f;
			} else if (i == 17 + (2 * 36)) {
				textureScaleFactor = 0.8f;
			} else if (i == 17 + (4 * 36)) {
				textureScaleFactor = 0.5f;
			} else if (i == 17 + (6 * 36)) {
				textureScaleFactor = 0.3f;
			} else {
				textureScaleFactor = 0f;
			}

			WindowShader lightShader = new WindowShader(
					new Lightsource(lightSourceOrigin, Color.white, Lightsource.MAX_INTENSITY),
					(int) (60 * textureScaleFactor), (int) (20 * textureScaleFactor));

			engine.setModelShader(wallLight, lightShader);

			Set<Model3D> bucket = new HashSet<>();
			bucket.add(wallLight);
			lightsBucketList.add(bucket);
		}

		// floor
		Model3D groundFloor = Model3DGenerator.buildPlane(300, tubeLength * tubeScaleFactorZ * 10, new Point3D(),
				Color.gray);
		groundFloor.move(0, -40, tubeLength * tubeScaleFactorZ * 4);
		groundFloor.rotate(-90, 0, 0);
		groundFloor.setMaterial(Material.GOLD);
		engine.register(groundFloor, 0);

		PhongShader floorShader = new PhongShader(lightsource);
		// floorShader.setAddSpecularity(false);
		engine.setModelShader(groundFloor, floorShader);

		// ceiling lights
		Iterator<Collection<Model3D>> lightsBucketIterator = lightsBucketList.iterator();
		for (int i = 0; i < 10; i += 1) {
			Model3D ceilingLight = Model3DGenerator.buildPlane(100, 50, new Point3D(), Color.white);
			ceilingLight.move(0, 100, 1000 * i);
			ceilingLight.rotate(90, 0, 0);
			ceilingLight.isVisible = false;
			engine.register(ceilingLight, lightSegment);

			Point3D lightSourceOrigin = new Point3D();
			lightSourceOrigin.move(0, 0, 1000 * i);

			FlatShader lightShader = new FlatShader(
					new Lightsource(lightSourceOrigin, Color.white, Lightsource.MAX_INTENSITY));
			engine.setModelShader(ceilingLight, lightShader);

			Model3D reflectionLight = Model3DGenerator.buildPlane(100, 50, new Point3D(), Color.white.darker());
			reflectionLight.move(0, -99, 1000 * i);
			reflectionLight.rotate(-90, 0, 0);
			reflectionLight.isVisible = false;
			engine.register(reflectionLight, lightSegment);

			lightShader = new FlatShader(
					new Lightsource(lightSourceOrigin, Color.white, Lightsource.NORM_INTENSITY * 0.4f));
			engine.setModelShader(reflectionLight, lightShader);

			if (i == 2 || i == 5) {
				Timer.getInstance().registerTimedObject(
						new FlickerLight(ceilingLight, reflectionLight, System.currentTimeMillis() + 2000, 100));
			}

			if (lightsBucketIterator.hasNext()) {
				Collection<Model3D> bucket = lightsBucketIterator.next();
				bucket.add(ceilingLight);
				bucket.add(reflectionLight);
			}
		}

		// pipes
		PhongShader pipeShader = new PhongShader(lightsource);

		for (int i = 0; i < 25; i++) {
			Model3D pipe = ModelUtils.createTube(20, 1, Color.white);
			pipe.setMaterial(Material.GOLD);
			pipe.move(-76.3f, 33, i * 190);
			engine.register(pipe, pipeShader);
		}
		for (int i = 0; i < 25; i++) {
			Model3D pipe = ModelUtils.createTube(20, 5, Color.lightGray);
			pipe.setMaterial(Material.GOLD);
			pipe.move(-60, 40, i * 160);
			engine.register(pipe, pipeShader);
		}
		for (int i = 0; i < 25; i++) {
			Model3D pipe = ModelUtils.createTube(20, i % 2 == 0 ? 5 : 6, Color.lightGray);
			pipe.setMaterial(Material.GOLD);
			pipe.move(-48, 42, i * 160);
			engine.register(pipe, pipeShader);
		}
		for (int i = 0; i < 25; i++) {
			Model3D pipe = ModelUtils.createTube(20, i % 2 == 0 ? 5 : 6, Color.lightGray);
			pipe.setMaterial(Material.GOLD);
			pipe.move(-60, -40, i * 160);
			engine.register(pipe, pipeShader);
		}
		for (int i = 0; i < 25; i++) {
			Model3D pipe = ModelUtils.createTube(20, i % 2 == 0 ? 5 : 6, Color.lightGray);
			pipe.setMaterial(Material.GOLD);
			pipe.move(60, -40, i * 160);
			engine.register(pipe, pipeShader);
		}

		// small pipes on left wall
		for (int i = 0; i < 25; i++) {
			Model3D pipe = ModelUtils.createTube(20, 1, Color.white);
			pipe.setMaterial(Material.GOLD);
			pipe.move(-95, 16.5f, i * 190);
			engine.register(pipe, pipeShader);
		}
		for (int i = 0; i < 25; i++) {
			Model3D pipe = ModelUtils.createTube(20, 1, Color.white);
			pipe.setMaterial(Material.GOLD);
			pipe.move(-98, -9.5f, i * 190);
			engine.register(pipe, pipeShader);
		}
		for (int i = 0; i < 25; i++) {
			Model3D pipe = ModelUtils.createTube(20, 2, Color.white);
			pipe.setMaterial(Material.GOLD);
			pipe.move(-83, -28, i * 190);
			engine.register(pipe, pipeShader);
		}
		for (int i = 0; i < 25; i++) {
			Model3D pipe = ModelUtils.createTube(20, 2, Color.lightGray);
			pipe.setMaterial(Material.GOLD);
			pipe.move(-79, -31, i * 190);
			engine.register(pipe, pipeShader);
		}

		// small pipes on ceiling
		for (int i = 0; i < 25; i++) {
			Model3D pipe = ModelUtils.createTube(20, 2, Color.white);
			pipe.setMaterial(Material.GOLD);
			pipe.move(-39, 41, i * 190);
			engine.register(pipe, pipeShader);
		}

		// control boxes
		FlatShader boxShader = new FlatShader(lightsource);
		boxShader.setAddSpecularity(false);
		for (int i = 0; i < 10; i++) {
			if (i > 5 && Math.random() > 0.3) {
				continue;
			}
			Model3D box = Model3DGenerator.buildBlock(2, i % 2 == 0 ? 10 : 5, i % 2 == 0 ? 60 : 30,
					i % 2 == 0 ? Color.white : Color.red);
			box.setMaterial(Material.GOLD);
			box.move(-87, -20, i * 100);

			if (i % 2 == 0) {
				Model3D subBox = Model3DGenerator.buildBlock(2, 5, 30, Color.white.darker());
				subBox.setMaterial(Material.GOLD);
				subBox.move(1, 0, 0);
				box.addSubModel(subBox);

				Model3D frame = ModelUtils.createFrame(1, 30, 5, Color.gray);
				frame.setMaterial(Material.BLACK_RUBBER);
				frame.move(0.2f, 0, 0);
				frame.rotate(0, 90, 0);
				subBox.addSubModel(frame);
			} else {
				Model3D frame = ModelUtils.createFrame(1, 30, 5, Color.gray);
				frame.setMaterial(Material.BLACK_RUBBER);
				frame.move(-1f, 0, 0);
				frame.rotate(0, 90, 0);
				box.addSubModel(frame);
			}

			engine.register(box, boxShader);
		}

		// control boxes
		for (int i = 1; i < 11; i++) {
			if (i > 5 && Math.random() > 0.3 || i % 2 != 0) {
				continue;
			}
			Model3D box = Model3DGenerator.buildBlock(2, i % 2 == 0 ? 10 : 5, i % 2 == 0 ? 60 : 30,
					i % 2 == 0 ? Color.white : Color.gray);
			box.setMaterial(Material.GOLD);
			box.move(-79, 27, i * 150);
			box.rotate(0, 0, -47);

			if (i % 2 == 0) {
				Model3D subBox = Model3DGenerator.buildBlock(2, 5, 30, Color.white.darker());
				subBox.setMaterial(Material.GOLD);
				subBox.move(1, 0, 0);
				box.addSubModel(subBox);

				Model3D frame = ModelUtils.createFrame(1, 30, 5, Color.gray);
				frame.setMaterial(Material.BLACK_RUBBER);
				frame.move(0.2f, 0, 0);
				frame.rotate(0, 90, 0);
				subBox.addSubModel(frame);
			}

			engine.register(box, boxShader);
		}

		Timer.getInstance().registerTimedObject(new FadeOutFadeIn(lightsource, Lightsource.MAX_INTENSITY, 1, 14700));
		Timer.getInstance().registerTimedObject(new ModelSwitchSequence(lightsBucketList, 500, true));

		initialized = true;
	}

	private void initScene2() {
		initialized = false;
		engine.clear();

		camera.reset();
		camera.setOrigin(new Point3D(-912, 0, -1040));
		camera.rotate(0, 70, 0);
		lightsource.x = -1000;
		lightsource.y = 30;
		lightsource.z = -1100;
		lightsource.setIntensity(Lightsource.NORM_INTENSITY * 0.25f);

		Model3D sphere = Model3DGenerator.buildSphere(100, 64, Color.lightGray);
		sphere.rotate(0, 90, 0);
		sphere.rotate(90, 0, 0);
		sphere.setOrigin(new Point3D(-1000, 0, -1000));
		sphere.scale(1f, 1f, 0.33f);
		sphere.setMaterial(Material.DEFAULT);

		for (int i = 0; i < 128; i++) {
			sphere.getFaces().get(64 + (22 * 128) + i).setColor(Color.RED);
		}
		for (int i = 0; i < 128; i++) {
			sphere.getFaces().get(64 + (23 * 128) + i).setColor(Color.RED);
		}
		for (int j = 24; j < 39; j++) {
			for (int i = 0; i < 128; i++) {
				sphere.getFaces().get(64 + (j * 128) + i).setColor(Color.white);
			}
		}
		for (int i = 0; i < 128; i++) {
			sphere.getFaces().get(64 + (37 * 128) + i).setColor(Color.RED);
		}
		for (int i = 0; i < 128; i++) {
			sphere.getFaces().get(64 + (38 * 128) + i).setColor(Color.RED);
		}
		for (int i = 0; i < 128; i++) {
			sphere.getFaces().get(64 + (39 * 128) + i).setColor(Color.gray);
		}
		Model3DGenerator.invertFaces(sphere);
		engine.register(sphere);

		// create windows
		float radius = 99;
		Lightsource windowsLightsource = new Lightsource(
				new Point3D(sphere.getOrigin().x, sphere.getOrigin().y + 100, sphere.getOrigin().z), Color.white,
				Lightsource.NORM_INTENSITY);
		PhongShader frameShader = new PhongShader(lightsource);
		frameShader.setAddSpecularity(false);
		for (int degree = 0; degree < 360; degree += 40) {
			float x0 = (float) Math.cos(Toolbox.degreeToRadiant(degree)) * radius;
			float z0 = (float) Math.sin(Toolbox.degreeToRadiant(degree)) * radius;
			float y0 = -2;

			float x1 = (float) Math.cos(Toolbox.degreeToRadiant(degree + 20)) * radius;
			float z1 = (float) Math.sin(Toolbox.degreeToRadiant(degree + 20)) * radius;
			float y1 = -2;

			float x2 = (float) Math.cos(Toolbox.degreeToRadiant(degree)) * radius;
			float z2 = (float) Math.sin(Toolbox.degreeToRadiant(degree)) * radius;
			float y2 = 2;

			float x3 = (float) Math.cos(Toolbox.degreeToRadiant(degree + 20)) * radius;
			float z3 = (float) Math.sin(Toolbox.degreeToRadiant(degree + 20)) * radius;
			float y3 = 2;

			Model3D window = new Model3D(sphere.getOrigin().copy());
			window.addVertex(x0, y0, z0);
			window.addVertex(x1, y1, z1);
			window.addVertex(x2, y2, z2);
			window.addVertex(x3, y3, z3);
			window.stretchFace(1, 3, 2, 0, Color.black);

			Model3D frame = ModelUtils.createFrame(1f, 32.6f, 4, Color.darkGray);
			frame.setMaterial(Material.BLACK_RUBBER);
			frame.setOrigin(window.getFaces().get(0).getCentroid().copy().add(sphere.getOrigin()));
			frame.rotate(0, degree - 80, 0);
			frame.move(-0.1f);

			engine.register(window, mainSegment);
			engine.setModelShader(window, new WindowShader(windowsLightsource, 50, 20, 0.05f, 0.5f));

			engine.register(frame, lightSegment);
			engine.setModelShader(frame, frameShader);
		}

		// ceiling
		Model3D ceiling = Model3DGenerator.buildPlane(200, sphere.getOrigin().copy(), Color.white);
		ceiling.move(0, 20, 0);
		ceiling.rotate(90, 0, 0);
		engine.register(ceiling);

		Model3D torusOnFloor = ModelUtils.createTorus(2, 90, Color.pink.brighter());
		torusOnFloor.setOrigin(sphere.getOrigin().copy().add(new Point3D(0, -14, 0)));
		engine.register(torusOnFloor);

		Model3D torusOnWall = ModelUtils.createTorus(1.5f, 90, Color.pink.brighter());
		torusOnWall.setOrigin(sphere.getOrigin().copy().add(new Point3D(0, 15, 0)));
		engine.register(torusOnWall, lightSegment);

		Model3D torusOnWall2 = ModelUtils.createTorus(1, 95, Color.pink);
		torusOnWall2.setOrigin(sphere.getOrigin().copy().add(new Point3D(0, -10, 0)));
		engine.register(torusOnWall2);

		Model3D torusOnCeiling = ModelUtils.createTorus(2, 80, Color.white);
		torusOnCeiling.setOrigin(sphere.getOrigin().copy().add(new Point3D(0, 20, 0)));
		engine.register(torusOnCeiling, lightSegment);

		// control boxes between windows
		FlatShader boxShader = new FlatShader(lightsource);
		// boxShader.setAddSpecularity(false);
		for (int i = 20; i < 360; i += 40) {
			Model3D box = Model3DGenerator.buildBlock(10, 4, 2, Color.white.darker());
			box.setOrigin(sphere.getOrigin().copy());
			box.rotate(0, i, 0);
			box.move(100);

			Model3D subBox = Model3DGenerator.buildBlock(5, 2, 1, Color.red);
			subBox.setMaterial(Material.DEFAULT);
			subBox.move(-1);
			box.addSubModel(subBox);

			Model3D frame = ModelUtils.createFrame(0.5f, 5, 2, Color.pink);
			frame.move(-0.2f);
			frame.rotate(0, 0, 0);
			subBox.addSubModel(frame);

			FlatShader subBoxShader = new FlatShader(new Lightsource(sphere.getOrigin(), Color.white, 1f));
			subBoxShader.setAddSpecularity(false);

			engine.register(box, lightSegment);
			engine.setModelShader(box, boxShader);
			engine.setModelShader(subBox, subBoxShader);
		}

		// control boxes below windows
		for (int i = 40; i < 360; i += 40) {
			Model3D box = Model3DGenerator.buildBlock(30, 2, 2, Color.pink);
			box.setOrigin(sphere.getOrigin().copy());
			box.move(0, -6, 0);
			box.rotate(0, i, 0);
			box.move(97f);
			engine.register(box, lightSegment);
			engine.setModelShader(box, boxShader);
		}

		// lights on ceiling
		FlatShader ceilingLightsShader = new FlatShader(
				new Lightsource(sphere.getOrigin(), Color.white, Lightsource.MAX_INTENSITY));
		for (int i = 40; i < 360; i += 20) {
			Model3D light = Model3DGenerator.buildBlock(15, 1, 2, Color.white);
			light.setOrigin(sphere.getOrigin().copy());
			light.move(0, 17, 0);
			light.rotate(0, i, 0);
			light.move(85f);
			engine.register(light, lightSegment);
			engine.setModelShader(light, ceilingLightsShader);

			// if (i == 120) {
			// Timer.getInstance().registerTimedObject(new FlickerLight(light,
			// null, System.currentTimeMillis(), 100));
			// }
		}

		// floor
		Model3D floor = Model3DGenerator.buildPlane(200, sphere.getOrigin().copy(), Color.pink);
		floor.move(0, -15, 0);
		floor.rotate(-90, 0, 0);
		// floor.setMaterial(Material.WHITE_PLASTIC);
		engine.register(floor);

		// table
		PhongShader tableSurfaceShader = new PhongShader(
				new Lightsource(sphere.getOrigin().copy().add(new Point3D(0, 5, 0)), Color.white, 0.7f));

		Model3D table = Model3DGenerator.buildSphere(30, 64, Color.cyan);
		table.setOrigin(sphere.getOrigin().copy());
		table.move(0, -6, 0);
		table.scale(1, 0.01f, 1);
		engine.register(table, lightSegment);
		engine.setModelShader(table, tableSurfaceShader);

		Model3D tableBase = ModelUtils.createTube(2, 20, Color.gray);
		tableBase.setOrigin(table.getOrigin().copy());
		tableBase.move(0, -10, 0);
		tableBase.rotate(90, 0, 0);
		engine.register(tableBase);

		Model3D torusAroundTable = ModelUtils.createTorus(0.5f, 30, Color.pink.brighter());
		torusAroundTable.setOrigin(table.getOrigin().copy());
		engine.register(torusAroundTable);

		Model3D torusAroundTableBase = ModelUtils.createTorus(2, 20, Color.darkGray);
		torusAroundTableBase.setOrigin(tableBase.getOrigin().copy());
		engine.register(torusAroundTableBase);

		Model3D tableShadow = ModelUtils.createCircle(32, Color.gray);
		tableShadow.setOrigin(tableBase.getOrigin().copy().add(new Point3D(0, 1.1f, 0)));
		engine.register(tableShadow);

		Model3D torusShadowRing = ModelUtils.createRing(92, 82, Color.black);
		torusShadowRing.setOrigin(tableBase.getOrigin().copy().add(new Point3D(0, 1.1f, 0)));
		engine.register(torusShadowRing);

		Model3D carpetRing = ModelUtils.createRing(70, 36, Color.pink.brighter());
		carpetRing.setOrigin(tableBase.getOrigin().copy().add(new Point3D(0, 1.1f, 0)));
		engine.register(carpetRing);

		initialized = true;
	}

	private void initScene3() {
		initialized = false;
		engine.clear();

		lightsource.x = 240;
		lightsource.y = 0;
		lightsource.z = 0;
		lightsource.setIntensity(Lightsource.MIN_INTENSITY);

		camera.reset();
		camera.setOrigin(new Point3D());
		camera.move(-100, -5, -135);
		camera.rotate(0, -20, 0);

		Model3D wall = Model3DGenerator.buildPlane(300, 200, new Point3D(0, 0, 101), Color.black);
		engine.register(wall);

		Model3D window = Model3DGenerator.buildPlane(200, 50, new Point3D(0, 0, 100), Color.white);
		engine.register(window, lightSegment);
		engine.setModelShader(window, new WindowShader(lightsource, 200, 50, 0.02f, 1f));

		Model3D frame = ModelUtils.createFrame(4, 200, 50, Color.white);
		frame.move(0, 0, 100);
		engine.register(frame, lightSegment);

		Model3D controlBoxBelowWindow = Model3DGenerator.buildBlock(100, 10, 2, Color.red.brighter().brighter());
		controlBoxBelowWindow.move(0, -45, 98);
		Model3D boxFrame = ModelUtils.createFrame(2, 100, 10, Color.darkGray);
		boxFrame.move(0, 0, 1);
		controlBoxBelowWindow.addSubModel(boxFrame);
		engine.register(controlBoxBelowWindow, new FlatShader(new Lightsource(new Point3D(), Color.white, 0.5f)));

		Model3D tubeAboveWindow = ModelUtils.createTube(31, 4, Color.white);
		tubeAboveWindow.setMaterial(Material.WHITE_PLASTIC);
		tubeAboveWindow.rotate(0, 90, 0);
		tubeAboveWindow.move(100, 50, -150);
		engine.register(tubeAboveWindow);

		Model3D tubeAboveWindow2 = ModelUtils.createTube(31, 10, Color.white);
		tubeAboveWindow2.setMaterial(Material.WHITE_PLASTIC);
		tubeAboveWindow2.rotate(0, 90, 0);
		tubeAboveWindow2.move(90, 60, -150);
		engine.register(tubeAboveWindow2);

		Model3D tubeBelowWindow = ModelUtils.createTube(31, 2, Color.orange);
		tubeBelowWindow.setMaterial(Material.WHITE_PLASTIC);
		tubeBelowWindow.rotate(0, 90, 0);
		tubeBelowWindow.move(90, -34, -150);
		engine.register(tubeBelowWindow);

		Model3D tubeBelowWindow2 = ModelUtils.createTube(31, 5, Color.white);
		tubeBelowWindow2.setMaterial(Material.WHITE_PLASTIC);
		tubeBelowWindow2.rotate(0, 90, 0);
		tubeBelowWindow2.move(90, -71, -150);
		engine.register(tubeBelowWindow2);

		Model3D lowerWall = Model3DGenerator.buildPlane(300, 35, new Point3D(0, 0, 100), Color.orange);
		lowerWall.setMaterial(Material.GOLD);
		lowerWall.move(0, -50, 0);
		engine.register(lowerWall, lightSegment);

		Model3D upperWall = Model3DGenerator.buildPlane(300, 12, new Point3D(0, 0, 100), Color.orange);
		upperWall.setMaterial(Material.GOLD);
		upperWall.move(0, 39, 0);
		engine.register(upperWall, lightSegment);

		Model3D bezel = ModelUtils.createCurvedBezel(4, 120, 10, Color.orange);
		bezel.move(0, -51, 98);
		engine.register(bezel, lightSegment);

		Timer.getInstance().registerTimedObject(new FadeOutFadeIn(lightsource, Lightsource.NORM_INTENSITY, 1, 1000));

		initialized = true;
	}

	private void initScene4() {
		initialized = false;
		engine.clear();

		lightsource.x = 0;
		lightsource.y = 0;
		lightsource.z = 0;
		lightsource.setIntensity(Lightsource.MAX_INTENSITY * 0.5f);
		lightsource.setAddAttenuation(true);
		lightsource.setLightAttenuationFalloffFactor(0.0004f);

		camera.reset();
		camera.setOrigin(new Point3D(-100, 0, 0));
		camera.rotate(0, -25, 0);

		lightsource.x = camera.getOrigin().x + 4;
		lightsource.y = camera.getOrigin().y + 1;
		lightsource.z = camera.getOrigin().z + 4;
		lightsource.getMatrix().rotate(0, -20, 0);
		lightsource.setShadowsEnabled(true);

		Model3D torus = ModelUtils.createTorus(10, 100, Color.lightGray);
		torus.setMaterial(Material.DEFAULT);
		Model3DGenerator.invertFaces(torus);
		torus.scale(1, 0.5f, 1);

		for (int i = 0; i < 36; i++) {
			torus.getFaces().get((36 * i) + 5).setColor(Color.white);
		}
		for (int i = 0; i < 36; i++) {
			torus.getFaces().get((36 * i) + 4).setColor(Color.pink);
		}
		for (int i = 0; i < 36; i++) {
			torus.getFaces().get((36 * i) + 3).setColor(Color.pink);
		}
		for (int i = 0; i < 36; i++) {
			torus.getFaces().get((36 * i) + 2).setColor(Color.pink);
		}
		for (int i = 0; i < 36; i++) {
			torus.getFaces().get((36 * i) + 1).setColor(Color.gray);
		}
		for (int i = 0; i < 36; i++) {
			torus.getFaces().get(36 * i).setColor(Color.orange.darker());
		}
		for (int i = 1; i < 37; i++) {
			torus.getFaces().get((36 * i) - 1).setColor(Color.orange.darker());
		}
		for (int i = 1; i < 37; i++) {
			torus.getFaces().get((36 * i) - 2).setColor(Color.lightGray);
		}
		for (int i = 1; i < 37; i++) {
			torus.getFaces().get((36 * i) - 4).setColor(Color.gray);
		}

		// inner wall
		for (int i = 0; i < 36; i++) {
			torus.getFaces().get((36 * i) + 13).setColor(Color.white);
		}
		for (int i = 0; i < 36; i++) {
			torus.getFaces().get((36 * i) + 14).setColor(Color.white);
		}
		for (int i = 0; i < 36; i++) {
			torus.getFaces().get((36 * i) + 15).setColor(Color.white);
		}
		for (int i = 0; i < 36; i++) {
			torus.getFaces().get((36 * i) + 17).setColor(Color.red);
		}
		for (int i = 0; i < 36; i++) {
			torus.getFaces().get((36 * i) + 18).setColor(Color.red);
		}
		for (int i = 0; i < 36; i++) {
			torus.getFaces().get((36 * i) + 19).setColor(Color.red);
		}

		// Model3D floorPath = ModelUtils.createRing(108, 90, new Color(135,
		// 206, 250));
		Model3D floorPath = ModelUtils.createRing(108, 90, Color.white);
		floorPath.move(0, -3, 0);
		torus.addSubModel(floorPath);

		Model3D innerFloorBorder = ModelUtils.createRing(95, 90, Color.pink.brighter());
		innerFloorBorder.move(0, -2.9f, 0);
		torus.addSubModel(innerFloorBorder);

		Model3D outerFloorBorder = ModelUtils.createRing(108, 104, Color.pink.brighter());
		outerFloorBorder.move(0, -2.9f, 0);
		torus.addSubModel(outerFloorBorder);

		Model3D innerTorusOuterWall = ModelUtils.createTorus(.2f, 108, Color.lightGray);
		innerTorusOuterWall.setOrigin(new Point3D(0, -2, 0));
		torus.addSubModel(innerTorusOuterWall);

		Model3D innerTorusOuterWall2 = ModelUtils.createTorus(.2f, 108, Color.gray.brighter());
		innerTorusOuterWall2.setOrigin(new Point3D(0, -2.3f, 0));
		torus.addSubModel(innerTorusOuterWall2);

		Model3D innerTorusOuterWall3 = ModelUtils.createTorus(.2f, 108, Color.lightGray);
		innerTorusOuterWall3.setOrigin(new Point3D(0, -1.7f, 0));
		torus.addSubModel(innerTorusOuterWall3);

		Model3D innerTorusOuterWall4 = ModelUtils.createTorus(.2f, 108, new Color(135, 206, 250).brighter());
		innerTorusOuterWall4.setOrigin(new Point3D(0, -1.4f, 0));
		torus.addSubModel(innerTorusOuterWall4);

		Model3D innerTorusOuterWall5 = ModelUtils.createTorus(.5f, 109.8f, new Color(135, 206, 250).brighter());
		innerTorusOuterWall5.setOrigin(new Point3D(0, 1.4f, 0));
		torus.addSubModel(innerTorusOuterWall5);

		Model3D innerTorusOuterWall6 = ModelUtils.createTorus(.8f, 105.6f, Color.lightGray);
		innerTorusOuterWall6.setOrigin(new Point3D(0, 4.8f, 0));
		torus.addSubModel(innerTorusOuterWall6);

		//
		Model3D innerTorus = ModelUtils.createTorus(.2f, 91.5f, new Color(135, 206, 250).brighter());
		innerTorus.setOrigin(new Point3D(0, -2, 0));
		torus.addSubModel(innerTorus);

		Model3D innerTorus2 = ModelUtils.createTorus(.2f, 91.8f, new Color(135, 206, 250).brighter().brighter());
		innerTorus2.setOrigin(new Point3D(0, -2.3f, 0));
		torus.addSubModel(innerTorus2);

		Model3D innerTorus3 = ModelUtils.createTorus(.2f, 91, new Color(135, 206, 250).brighter().brighter());
		innerTorus3.setOrigin(new Point3D(0, 1.5f, 0));
		torus.addSubModel(innerTorus3);

		Model3D innerTorus4 = ModelUtils.createTorus(.2f, 90.6f, new Color(135, 206, 250).brighter());
		innerTorus4.setOrigin(new Point3D(0, 1.2f, 0));
		torus.addSubModel(innerTorus4);

		Model3D innerTorus5 = ModelUtils.createTorus(.6f, 93.3f, Color.lightGray);
		innerTorus5.setOrigin(new Point3D(0, 4f, 0));
		torus.addSubModel(innerTorus5);

		Model3D torusOnCeilingL = ModelUtils.createTorus(0.8f, 100.8f, Color.white);
		torusOnCeilingL.setOrigin(new Point3D(0, 5f, 0));
		torus.addSubModel(torusOnCeilingL);

		Model3D torusOnCeilingR = ModelUtils.createTorus(0.8f, 99.2f, Color.white);
		torusOnCeilingR.setOrigin(new Point3D(0, 5f, 0));
		torus.addSubModel(torusOnCeilingR);

		Model3D torusOnCeilingR2 = ModelUtils.createTorus(0.8f, 97.6f, Color.lightGray);
		torusOnCeilingR2.setOrigin(new Point3D(0, 5f, 0));
		torus.addSubModel(torusOnCeilingR2);

		PhongShader innerTorusShader = new PhongShader(lightsource);
		engine.register(torus, mainSegment);
		engine.setModelShader(floorPath, innerTorusShader);
		engine.setModelShader(innerTorusOuterWall, innerTorusShader);
		engine.setModelShader(innerTorus, innerTorusShader);
		engine.setModelShader(innerTorus2, innerTorusShader);
		engine.setModelShader(innerTorus3, innerTorusShader);
		engine.setModelShader(innerTorus4, innerTorusShader);

		// lights and control boxes ..
		Model3D controlBoxRootModel = new Model3D();
		
		// rotate with torus
		controlBoxRootModel.setMatrix(torus.getMatrix());

		Model3D lightspaceControlBoxRootModel = new Model3D();
		
		// rotate with torus
		lightspaceControlBoxRootModel.setMatrix(torus.getMatrix());

		FlatShader boxShader = new FlatShader(lightsource);
		boxShader.setAddSpecularity(false);
		int spanningDegree = 5;
		// .. outer wall
		for (int i = 5; i < 360; i += (2 * spanningDegree)) {
			float radius = 109.5f;
			float posX0 = (float) Math.cos(Toolbox.degreeToRadiant(i)) * radius;
			float posZ0 = (float) Math.sin(Toolbox.degreeToRadiant(i)) * radius;

			float posX1 = (float) Math.cos(Toolbox.degreeToRadiant(i + spanningDegree)) * radius;
			float posZ1 = (float) Math.sin(Toolbox.degreeToRadiant(i + spanningDegree)) * radius;

			float posX = posX0 + (posX0 - posX1) / 2f;
			float posZ = posZ0 + (posZ0 - posZ1) / 2f;

			Model3D box = Model3DGenerator.buildBlock(1f, 0.5f, 5, Color.orange);
			box.setMaterial(Material.DEFAULT);
			box.rotate(0, i, 0);
			box.setOrigin(new Point3D(posX, 0f, posZ));

			lightspaceControlBoxRootModel.addSubModel(box);
			engine.setModelShader(box, boxShader);

			Model3D boxFrame = ModelUtils.createFrame(0.12f, 5, 0.5f, Color.gray);
			boxFrame.rotate(0, -90, 0);
			boxFrame.move(-0.25f);
			box.addSubModel(boxFrame);

			Model3D window = Model3DGenerator.buildPlane(10, 1.5f, new Point3D(), Color.black);
			window.move(0, 4.3f, 2);
			window.rotate(0, -90, 0);
			window.rotate(35, 0, 0);
			window.move(-3f);
			box.addSubModel(window);
			engine.setModelShader(window, new NebulaShader(lightsource));

			Model3D windowFrame = ModelUtils.createFrame(0.2f, 10, 1.5f, Color.white);
			window.addSubModel(windowFrame);
		}

		// .. inner wall
		for (int i = 5; i < 360; i += (2 * spanningDegree)) {
			float radius = 90.5f;
			float posX0 = (float) Math.cos(Toolbox.degreeToRadiant(i)) * radius;
			float posZ0 = (float) Math.sin(Toolbox.degreeToRadiant(i)) * radius;
			float posX1 = (float) Math.cos(Toolbox.degreeToRadiant(i + spanningDegree)) * radius;
			float posZ1 = (float) Math.sin(Toolbox.degreeToRadiant(i + spanningDegree)) * radius;

			float posX = posX0 + (posX0 - posX1) / 2f;
			float posZ = posZ0 + (posZ0 - posZ1) / 2f;

			Model3D controlElement = Model3DGenerator.buildBlock(1f, 0.5f, 5, Color.black);
			controlElement.setMaterial(Material.DEFAULT);
			controlElement.rotate(0, 5 + i - spanningDegree, 0);
			controlElement.rotate(0, 0, -48);
			controlElement.setOrigin(new Point3D(posX, 2.8f, posZ));
			controlBoxRootModel.addSubModel(controlElement);

			Point3D contolLightsourceOrigin = camera.getOrigin().copy().move(0, 0, 10);
			FlatShader controlElementShader = new FlatShader(
					new Lightsource(contolLightsourceOrigin, Color.white, Lightsource.MAX_INTENSITY));
			engine.setModelShader(controlElement, controlElementShader);

			Timer.getInstance().registerTimedObject(
					new BlinkLight(controlElement, Color.red, Color.black, System.currentTimeMillis(), 1000, 14000));
		}
		Timer.getInstance().registerTimedObject(new BlinkLight(innerTorus3, Color.pink,
				new Color(135, 206, 250).brighter().brighter(), System.currentTimeMillis(), 1000, 11000));

		engine.register(controlBoxRootModel, mainSegment);
		engine.register(lightspaceControlBoxRootModel, lightSegment);

		Timer.getInstance().registerTimedObject(new Rotate(torus, 11000, 0.004f, Rotate.AXIS_Y));

		initialized = true;
	}

	private void initScene5() {
		initialized = false;

		lightsource.x = 0;
		lightsource.y = 0;
		lightsource.z = 270;
		lightsource.getMatrix().reset();
		lightsource.setIntensity(Lightsource.NORM_INTENSITY);
		lightsource.setAddAttenuation(true);
		lightsource.setLightAttenuationFalloffFactor(0.0001f);
		lightsource.setShadowsEnabled(false);

		camera.reset();
		camera.setOrigin(new Point3D(0, 0, -100));

		Color pipeColor = Color.red;

		engine.clear();

		Model3D modelL = ModelUtils.createTorus(60, 100, 10, 91, Color.white);
		modelL.setMaterial(Material.WHITE_PLASTIC);
		modelL.rotate(180, 0, 0);
		modelL.move(-100, 0, -300);
		Model3DGenerator.invertFaces(modelL);
		// remove back-faced polygons
		ModelUtils.clipAgainstCamera(modelL, camera);
		modelL.recomputeFaceNormals();
		engine.register(modelL, mainSegment);

		Model3D floorPipeL = ModelUtils.createTorus(2, 65, 0, 91, pipeColor);
		floorPipeL.setMaterial(Material.WHITE_PLASTIC);
		floorPipeL.rotate(180, 0, 0);
		floorPipeL.move(-108, 40, -307);
		engine.register(floorPipeL, mainSegment);

		Model3D ceilingPipeL = ModelUtils.createTorus(2, 65, 0, 91, pipeColor);
		ceilingPipeL.setMaterial(Material.WHITE_PLASTIC);
		ceilingPipeL.rotate(180, 0, 0);
		ceilingPipeL.move(-108, -40, -307);
		engine.register(ceilingPipeL, mainSegment);

		Model3D modelR = ModelUtils.createTorus(60, 100, 10, 91, Color.white);
		modelR.setMaterial(Material.WHITE_PLASTIC);
		modelR.rotate(0, 0, 180);
		modelR.rotate(0, 180, 180);
		modelR.move(-100, 0, -300);
		Model3DGenerator.invertFaces(modelR);
		// remove back-faced polygons
		ModelUtils.clipAgainstCamera(modelR, camera);
		modelR.recomputeFaceNormals();
		engine.register(modelR, mainSegment);

		Model3D floorPipeR = ModelUtils.createTorus(2, 65, 0, 91, pipeColor);
		floorPipeR.setMaterial(Material.WHITE_PLASTIC);
		floorPipeR.rotate(0, 0, 180);
		floorPipeR.rotate(0, 180, 180);
		floorPipeR.move(-108, -40, -307);
		engine.register(floorPipeR, mainSegment);

		Model3D ceilingPipeR = ModelUtils.createTorus(2, 65, 0, 91, pipeColor);
		ceilingPipeR.setMaterial(Material.WHITE_PLASTIC);
		ceilingPipeR.rotate(0, 0, 180);
		ceilingPipeR.rotate(0, 180, 180);
		ceilingPipeR.move(-108, 40, -307);
		engine.register(ceilingPipeR, mainSegment);

		Model3D tube = ModelUtils.createTube(40, 60.2f, Color.white);
		tube.setMaterial(Material.WHITE_PLASTIC);
		Model3DGenerator.invertFaces(tube);
		tube.move(0, 0, 285);
		engine.register(tube, mainSegment);

		Model3D floorTubePipeL = ModelUtils.createTube(40, 2, pipeColor);
		floorTubePipeL.setMaterial(Material.WHITE_PLASTIC);
		floorTubePipeL.move(-43, -40, 305);
		engine.register(floorTubePipeL, mainSegment);

		Model3D floorTubePipeR = ModelUtils.createTube(40, 2, pipeColor);
		floorTubePipeR.setMaterial(Material.WHITE_PLASTIC);
		floorTubePipeR.move(43, -40, 305);
		engine.register(floorTubePipeR, mainSegment);

		Model3D ceilingTubePipeL = ModelUtils.createTube(40, 2, pipeColor);
		ceilingTubePipeL.setMaterial(Material.WHITE_PLASTIC);
		ceilingTubePipeL.move(-43, 40, 305);
		engine.register(ceilingTubePipeL, mainSegment);

		Model3D ceilingTubePipeR = ModelUtils.createTube(40, 2, pipeColor);
		ceilingTubePipeR.setMaterial(Material.WHITE_PLASTIC);
		ceilingTubePipeR.move(43, 40, 305);
		engine.register(ceilingTubePipeR, mainSegment);

		Model3D tubeL = ModelUtils.createTube(60, 60, Color.white);
		tubeL.setMaterial(Material.WHITE_PLASTIC);
		Model3DGenerator.invertFaces(tubeL);
		tubeL.move(-100, 0, 200);
		tubeL.rotate(0, 90, 0);
		ModelUtils.clipAgainstCamera(tubeL, camera);
		engine.register(tubeL, mainSegment);

		Model3D staightfloorPipeL = ModelUtils.createTube(60, 2, pipeColor);
		staightfloorPipeL.setMaterial(Material.WHITE_PLASTIC);
		staightfloorPipeL.move(-108, -40, 242);
		staightfloorPipeL.rotate(0, 90, 0);
		engine.register(staightfloorPipeL, mainSegment);

		Model3D staightCeilingPipeL = ModelUtils.createTube(60, 2, pipeColor);
		staightCeilingPipeL.setMaterial(Material.WHITE_PLASTIC);
		staightCeilingPipeL.move(-108, 40, 242);
		staightCeilingPipeL.rotate(0, 90, 0);
		engine.register(staightCeilingPipeL, mainSegment);

		Model3D tubeR = ModelUtils.createTube(30, 60, Color.white);
		tubeR.setMaterial(Material.WHITE_PLASTIC);
		Model3DGenerator.invertFaces(tubeR);
		tubeR.move(390, 0, 200);
		tubeR.rotate(0, 90, 0);
		ModelUtils.clipAgainstCamera(tubeR, camera);
		engine.register(tubeR, mainSegment);

		Model3D staightfloorPipeR = ModelUtils.createTube(30, 2, pipeColor);
		staightfloorPipeR.setMaterial(Material.WHITE_PLASTIC);
		staightfloorPipeR.move(390, -40, 242);
		staightfloorPipeR.rotate(0, 90, 0);
		engine.register(staightfloorPipeR, mainSegment);

		Model3D staightCeilingPipeR = ModelUtils.createTube(30, 2, pipeColor);
		staightCeilingPipeR.setMaterial(Material.WHITE_PLASTIC);
		staightCeilingPipeR.move(390, 40, 242);
		staightCeilingPipeR.rotate(0, 90, 0);
		engine.register(staightCeilingPipeR, mainSegment);

		Model3D tubeBack = ModelUtils.createTube(120, 60, Color.white);
		tubeBack.setMaterial(Material.WHITE_PLASTIC);
		Model3DGenerator.invertFaces(tubeBack);
		tubeBack.move(-600, 0, 200);
		tubeBack.rotate(0, 90, 0);
		ModelUtils.clipAgainstCamera(tubeBack, camera);
		tubeBack.rotate(0, 180, 0);
		engine.register(tubeBack, mainSegment);

		Model3D floorPipeBack = ModelUtils.createTube(120, 2, pipeColor);
		floorPipeBack.setMaterial(Material.WHITE_PLASTIC);
		floorPipeBack.move(-600, -40, 156);
		floorPipeBack.rotate(0, 270, 0);
		engine.register(floorPipeBack, mainSegment);

		Model3D ceilingPipeBack = ModelUtils.createTube(120, 2, pipeColor);
		ceilingPipeBack.setMaterial(Material.WHITE_PLASTIC);
		ceilingPipeBack.move(-600, 40, 156);
		ceilingPipeBack.rotate(0, 270, 0);
		engine.register(ceilingPipeBack, mainSegment);

		Model3D floor = Model3DGenerator.buildPlane(1500, 500, new Point3D(0, -40, 300), Color.white);
		floor.setMaterial(Material.WHITE_PLASTIC);
		floor.rotate(-90, 0, 0);
		engine.register(floor, mainSegment);

		Model3D ceiling = Model3DGenerator.buildPlane(1500, 500, new Point3D(0, 40, 300), Color.white);
		ceiling.setMaterial(Material.WHITE_PLASTIC);
		ceiling.rotate(90, 0, 0);
		engine.register(ceiling, mainSegment);

		camera.move(0, -10, 0);
		camera.move(300);
		camera.rotate(0, 90, 0);
		camera.move(-470);

		Timer.getInstance()
				.registerTimedObject(new FlickerLightsource(lightsource, 0.3f, Lightsource.NORM_INTENSITY, 200, 13000));

		Timer.getInstance().registerTimedObject(new CamMove(camera, 0.015f, 13000));

		initialized = true;
	}

	private void initScene6() {
		initialized = false;
		engine.clear();

		lightsource.getMatrix().reset();
		lightsource.x = -700;
		lightsource.y = 110;
		lightsource.z = -20;
		lightsource.setIntensity(Lightsource.NORM_INTENSITY);
		lightsource.setAddAttenuation(true);
		lightsource.setLightAttenuationFalloffFactor(0.000001f);
		lightsource.setShadowsEnabled(true);

		camera.reset();
		camera.setOrigin(new Point3D(-100, 50, 0));
		camera.rotate(0, 90, 0);

		lightsource.setMatrix(camera.getMatrix());

		Model3D crossWallTube = ModelUtils.createTube(100, 40, Color.darkGray);
		crossWallTube.setMaterial(Material.BLACK_PLASTIC);
		Model3DGenerator.invertFaces(crossWallTube);
		crossWallTube.scale(2, 2, 1);
		crossWallTube.move(-2100, 85, -600);
		engine.register(crossWallTube);

		Model3D backWallTube = ModelUtils.createTube(300, 40, Color.white);
		Model3DGenerator.invertFaces(backWallTube);
		backWallTube.scale(1, 3, 1);
		backWallTube.move(1000, 85, 100);
		backWallTube.rotate(0, 90, 0);
		engine.register(backWallTube);

		Model3D frontWallTube = ModelUtils.createTube(300, 40, Color.white);
		Model3DGenerator.invertFaces(frontWallTube);
		frontWallTube.scale(1, 3, 1);
		frontWallTube.move(1000, 85, -100);
		frontWallTube.rotate(0, 90, 0);
		engine.register(frontWallTube);

		Model3D floorTube = ModelUtils.createTube(300, 10, Color.white);
		floorTube.scale(1, 16, 1);
		floorTube.move(1000, 0, 0);
		floorTube.rotate(90, 90, 0);
		engine.register(floorTube);

		Model3D topTube = ModelUtils.createTube(300, 10, Color.white);
		Model3DGenerator.invertFaces(topTube);
		topTube.scale(1, 16, 1);
		topTube.move(1000, 170, 0);
		topTube.rotate(90, 90, 0);
		engine.register(topTube);

		Model3D topLeftPipe = ModelUtils.createTube(300, 10, Color.red);
		topLeftPipe.move(1000, 180, -120);
		topLeftPipe.rotate(0, 90, 0);
		engine.register(topLeftPipe);

		Model3D topRightPipe = ModelUtils.createTube(300, 10, Color.red);
		topRightPipe.move(1000, 180, 120);
		topRightPipe.rotate(0, 90, 0);
		engine.register(topRightPipe);

		Model3D bottomLeftPipe = ModelUtils.createTube(300, 10, Color.red);
		bottomLeftPipe.move(1000, 2, -120);
		bottomLeftPipe.rotate(0, 90, 0);
		engine.register(bottomLeftPipe);

		Model3D bottomRightPipe = ModelUtils.createTube(300, 10, Color.red);
		bottomRightPipe.move(1000, 2, 120);
		bottomRightPipe.rotate(0, 90, 0);
		engine.register(bottomRightPipe);

		ModelUtils.clipAgainstCamera(backWallTube, camera);
		ModelUtils.clipAgainstCamera(frontWallTube, camera);
		ModelUtils.clipAgainstCamera(floorTube, camera);
		ModelUtils.clipAgainstCamera(topTube, camera);
		ModelUtils.clipAgainstCamera(topLeftPipe, camera);
		ModelUtils.clipAgainstCamera(topRightPipe, camera);
		ModelUtils.clipAgainstCamera(bottomLeftPipe, camera);
		ModelUtils.clipAgainstCamera(bottomRightPipe, camera);

		// the alien
		alienHead = Model3DGenerator.buildSphere(20, 64, Color.black);
		alienHead.move(-2100, 140, -300);
		alienHead.rotate(0, 0, 0);
		alienHead.rotate(-10, 0, 0);
		alienHead.scale(1, 1, 3);

		Model3D torso = Model3DGenerator.buildSphere(5, 12, Color.black);
		torso.setMaterial(Material.BLACK_PLASTIC);
		torso.move(0, -60, 20);
		torso.scale(1, 16, 2);
		alienHead.addSubModel(torso);

		Model3D mouth = Model3DGenerator.buildSphere(10, 64, Color.black);
		mouth.setMaterial(Material.BLACK_PLASTIC);
		// Model3DGenerator.invertFaces(mouth);
		mouth.move(0, -15, 30);
		mouth.scale(1, 1, 3);
		alienHead.addSubModel(mouth);

		engine.register(alienHead);

		ModelUtils.clipAgainstCamera(crossWallTube, camera);
		ModelUtils.clipAgainstCamera(backWallTube, camera);
		ModelUtils.clipAgainstCamera(frontWallTube, camera);
		ModelUtils.clipAgainstCamera(floorTube, camera);
		ModelUtils.clipAgainstCamera(topTube, camera);

		Model3D bottomLeftBox = Model3DGenerator.buildCube(50, Color.gray);
		bottomLeftBox.setMaterial(Material.DEFAULT);
		bottomLeftBox.move(-1900, 30, -80);
		engine.register(bottomLeftBox);

		Model3D bottomMiddleBox = Model3DGenerator.buildCube(50, Color.lightGray);
		bottomMiddleBox.setMaterial(Material.DEFAULT);
		bottomMiddleBox.move(-1910, 30, -29);
		engine.register(bottomMiddleBox);

		Model3D bottomRightBox = Model3DGenerator.buildCube(50, Color.lightGray);
		bottomRightBox.setMaterial(Material.DEFAULT);
		bottomRightBox.move(-1910, 30, 22);
		engine.register(bottomRightBox);

		Model3D bottomRight2Box = Model3DGenerator.buildCube(50, Color.gray);
		bottomRight2Box.setMaterial(Material.DEFAULT);
		bottomRight2Box.move(-1920, 30, 83);
		engine.register(bottomRight2Box);

		Model3D topLeftBox = Model3DGenerator.buildCube(50, Color.gray);
		topLeftBox.setMaterial(Material.DEFAULT);
		topLeftBox.move(-1895, 80, -70);
		engine.register(topLeftBox);

		Model3D topMiddleBox = Model3DGenerator.buildCube(50, Color.gray);
		topMiddleBox.setMaterial(Material.DEFAULT);
		topMiddleBox.move(-1905, 80, -19);
		engine.register(topMiddleBox);

		Model3D topRightBox = Model3DGenerator.buildCube(50, Color.pink.brighter());
		topRightBox.setMaterial(Material.DEFAULT);
		topRightBox.move(-1905, 80, 52);
		engine.register(topRightBox);

		Model3D topRight2Box = Model3DGenerator.buildCube(50, Color.gray);
		topRight2Box.setMaterial(Material.DEFAULT);
		topRight2Box.move(-1905, 80, 105);
		engine.register(topRight2Box);

		Timer.getInstance().registerTimedObject(new AlienHeadMove(alienHead, System.currentTimeMillis() + 1000));

		camera.move(110, -20, 1400);
		camera.rotate(0, 10, 0);

		initialized = true;
	}

	private void startScene1a() {
		long startTime = System.currentTimeMillis();

		Timer.getInstance().registerTimedObject(new Timed() {
			long timer = 0;
			boolean done = false;

			@Override
			public void timePassedInMillis(long timePassed) {
				timer += timePassed;
				if (timer > 1000) {
					AlienFluteMotif.play(synth);
					done = true;
				}
			}

			@Override
			public boolean done() {
				return done;
			}
		});

		while (true) {
			repaint();
			sleep(2);

			if (System.currentTimeMillis() - startTime > 15500) {
				break;
			}
		}
	}

	private void startScene1b() {
		camera.rotate(0, 70, 0);
		camera.move(150, 0, -160);

		long startTime = System.currentTimeMillis();
		FadeOut fadeOut = null;

		while (true) {
			repaint();
			sleep(2);
			if (fadeOut == null && System.currentTimeMillis() - startTime > 8000) {
				fadeOut = new FadeOut(lightsource, 2000);
				Timer.getInstance().registerTimedObject(fadeOut);
			}
			if (fadeOut != null && fadeOut.done() && System.currentTimeMillis() - startTime > 10100) {
				break;
			}
		}
	}

	private void startScene2() {
		long startTime = System.currentTimeMillis();
		FadeOut fadeOut = null;

		Timer.getInstance().registerTimedObject(new CamMove(camera, 0.003f, 13000));

		while (true) {
			repaint();
			sleep(2);
			if (fadeOut == null && System.currentTimeMillis() - startTime > 6000) {
				fadeOut = new FadeOut(lightsource, 6000);
				Timer.getInstance().registerTimedObject(fadeOut);
			}
			if (System.currentTimeMillis() - startTime > 13500) {
				break;
			}
		}
	}

	private void startScene3() {
		long startTime = System.currentTimeMillis();
		FadeOut fadeOut = null;
		while (true) {
			repaint();
			sleep(2);

			if (fadeOut == null && System.currentTimeMillis() - startTime > 5500) {
				fadeOut = new FadeOut(lightsource, 1300);
				Timer.getInstance().registerTimedObject(fadeOut);
			}

			if (System.currentTimeMillis() - startTime > 6855) {
				break;
			}
		}
	}

	private void startScene4() {
		long startTime = System.currentTimeMillis();
		FadeOut fadeOut = null;

		float noiseXInc = 0;
		float noiseYInc = 0.5f;

		while (true) {
			repaint();
			sleep(2);

			double noiseX = NoiseGenerator.improvedPerlinNoise(noiseXInc);
			double noiseY = NoiseGenerator.improvedPerlinNoise(noiseYInc);

			noiseX = Toolbox.map(noiseX, -1, 1, -0.025, 0.025);
			noiseY = Toolbox.map(noiseY, -1, 1, -0.07, 0.07);

			camera.rotate((float) noiseX, (float) noiseY, 0);

			Matrix3D newLightMatrix = lightsource.getMatrix();
			newLightMatrix.lerp(camera.getMatrix(), 0.1f);
			lightsource.setMatrix(newLightMatrix);

			if (fadeOut == null && System.currentTimeMillis() - startTime > 9000) {
				fadeOut = new FadeOut(lightsource, 1000);
				Timer.getInstance().registerTimedObject(fadeOut);
			}
			if (System.currentTimeMillis() - startTime > 11000) {
				break;
			}

			noiseXInc += 0.001f;
			noiseYInc += 0.001f;
		}
	}

	private void startScene5() {
		long startTime = System.currentTimeMillis();
		while (true) {
			repaint();
			sleep(2);
			if (System.currentTimeMillis() - startTime > 13500) {
				break;
			}
		}
	}

	private void startScene6() {
		long startTime = System.currentTimeMillis();
		FadeOut fadeOut = null;

		Camera viewCam = new TripodCamera(camera.getOrigin());

		AlienStomp.play(synth);

		while (true) {
			repaint();
			sleep(2);

			long currentTime = System.currentTimeMillis();

			if (currentTime - startTime > 4000 && currentTime - startTime < 10000) {
				viewCam.focus(alienHead.getOrigin());
				// let the camera follow/drag behind the view-cam for a dynamic effect
				camera.getMatrix().lerp(viewCam.getMatrix(), 0.01f);
			}

			if (fadeOut == null && currentTime - startTime > 6000) {
				fadeOut = new FadeOut(lightsource, 10000);
				Timer.getInstance().registerTimedObject(fadeOut);
			}
			if (fadeOut != null && fadeOut.done() && currentTime - startTime > 15000) {
				break;
			}
		}
	}

	protected void processKeyEvent(KeyEvent event) {
		if (event.getID() == KeyEvent.KEY_PRESSED) {
			int keycode = event.getKeyCode();
			if (keycode == KeyEvent.VK_ESCAPE) {
				exit();
			} else if (keycode == KeyEvent.VK_F3) {
				engine.shadingEnabled = !engine.shadingEnabled;
			} else if (keycode == KeyEvent.VK_F4) {
				lightsource.setShadowsEnabled(!lightsource.isShadowsEnabled());
			} else if (keycode == KeyEvent.VK_W) {
				camera.move(0, 0, 10);
			} else if (keycode == KeyEvent.VK_S) {
				camera.move(0, 0, -10);
			} else if (keycode == KeyEvent.VK_A) {
				camera.move(-10, 0, 0);
			} else if (keycode == KeyEvent.VK_D) {
				camera.move(10, 0, 0);
			}

			else if (keycode == KeyEvent.VK_N) {
				camera.rotate(0, 10, 0);
			} else if (keycode == KeyEvent.VK_M) {
				camera.rotate(0, -10, 0);
			} else if (keycode == KeyEvent.VK_UP) {
				camera.move(0, 10, 0);
			} else if (keycode == KeyEvent.VK_DOWN) {
				camera.move(0, -10, 0);
			}

			else if (keycode == KeyEvent.VK_LEFT) {
				lightsource.move(-10, 0, 0);
			} else if (keycode == KeyEvent.VK_RIGHT) {
				lightsource.move(10, 0, 0);
			} else if (keycode == KeyEvent.VK_PAGE_UP) {
				lightsource.move(0, 0, 10);
			} else if (keycode == KeyEvent.VK_PAGE_DOWN) {
				lightsource.move(0, 0, -10);
			}

			if (lightsource.isShadowsEnabled()) {
				engine.recomputeShadowMaps();
			}

		}

		super.processKeyEvent(event);
	}

	@Override
	public void paint(Graphics g) {
		if (!initialized) {
			return;
		}		

		long updateTime = System.currentTimeMillis();
		
		if (lightsource.isShadowsEnabled()) {
			lightsource.recomputeDepthMap();
		}

		engine.setActiveSegment(lightSegment);
		engine.drawScene(lightsFrame);

		engine.setActiveSegment(mainSegment);
		engine.drawShadowedScene(mainFrame);

		mainFrame = FilterUtils.filter(mainFrame, Toolbox.GAUSSFILTER33);

		lightsFrame = FilterUtils.filter(lightsFrame, filterKernel);
		lightsFrame = FilterUtils.filter(lightsFrame, Toolbox.GAUSSFILTER33);

		mainFrame = FilterUtils.add(mainFrame, lightsFrame);

		g.drawImage(mainFrame.createImage(), 0, 0, mainFrame.getWidth(), mainFrame.getHeight(), null);
		// g.drawImage(lightsFrame.createImage(), 0, 0, lightsFrame.getWidth(), lightsFrame.getHeight(), null);

		updateTime = System.currentTimeMillis() - updateTime;
		if (updateTime < 33) { // cap at 33ms ~ 30 FPS
			sleep(33 - updateTime);
			updateTime = 33;
		}
	}

	private void sleep(long timeInMillis) {
		try {
			Thread.sleep(timeInMillis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void exit() {
		synth.close();
		System.exit(0);
	}
}
