import java.awt.Color;
import java.util.Iterator;

import gframe.engine.Camera;
import gframe.engine.Clipper3D;
import gframe.engine.Face;
import gframe.engine.Matrix3D;
import gframe.engine.Model3D;
import gframe.engine.Point3D;
import gframe.engine.Toolbox;
import gframe.engine.Vector3D;
import gframe.engine.generator.Model3DGenerator;

public final class ModelUtils {

	public static Model3D createTorus(float radius1, float radius2, Color color) {
		return createTorus(radius1, radius2, 0, 360, color);
	}

	public static Model3D createTorus(float radius1, float radius2, int fromDegees, int toDegrees, Color color) {
		Model3D result = new Model3D();

		int ringCount = 0;
		for (int largeDegree = fromDegees; largeDegree < toDegrees; largeDegree += 10) {

			float circleOriginX = (float) Math.cos(Toolbox.degreeToRadiant(largeDegree)) * radius2;
			float circleOriginZ = (float) Math.sin(Toolbox.degreeToRadiant(largeDegree)) * radius2;
			float circleOriginY = 0;
			Point3D circleOrigin = new Point3D(circleOriginX, circleOriginY, circleOriginZ);

			Matrix3D circleMatrix = new Matrix3D();
			circleMatrix.rotate(0, largeDegree, 0);

			for (int smallDegree = 0; smallDegree < 360; smallDegree += 10) {
				float x0 = (float) Math.cos(Toolbox.degreeToRadiant(smallDegree)) * radius1;
				float y0 = (float) Math.sin(Toolbox.degreeToRadiant(smallDegree)) * radius1;
				float z0 = 0;
				Point3D circlePoint = new Point3D(x0, y0, z0);

				circlePoint = circleMatrix.transform(circlePoint);
				circlePoint.add(circleOrigin);
				result.addVertex(circlePoint);
			}

			if (ringCount > 0) {
				int ring0_vertexOffset = (ringCount - 1) * 36;
				int ring1_vertexOffset = ringCount * 36;

				for (int v = 0; v < 36 - 1; v += 1) {
					int p1 = ring0_vertexOffset + v;
					int p2 = ring0_vertexOffset + 1 + v;
					int p3 = ring1_vertexOffset + v;
					int p4 = ring1_vertexOffset + 1 + v;
					result.stretchFace(p1, p2, p4, p3, color);
				}

				// close the gap
				int p1 = ring0_vertexOffset + 35;
				int p2 = ring0_vertexOffset + 0;
				int p3 = ring1_vertexOffset + 35;
				int p4 = ring1_vertexOffset + 0;
				result.stretchFace(p1, p2, p4, p3, color);
			}

			ringCount++;
		}

		if (toDegrees == 360) {
			// connect last to first ring
			int ring0_vertexOffset = (ringCount - 1) * 36;
			int ring1_vertexOffset = 0;

			for (int v = 0; v < 36 - 1; v += 1) {
				int p1 = ring0_vertexOffset + v;
				int p2 = ring0_vertexOffset + 1 + v;
				int p3 = ring1_vertexOffset + v;
				int p4 = ring1_vertexOffset + 1 + v;
				result.stretchFace(p1, p2, p4, p3, color);
			}

			// close the gap
			int p1 = ring0_vertexOffset + 35;
			int p2 = ring0_vertexOffset + 0;
			int p3 = ring1_vertexOffset + 35;
			int p4 = ring1_vertexOffset + 0;
			result.stretchFace(p1, p2, p4, p3, color);
		}

		return result;
	}

	public static Model3D createTube(int length, float radius, Color color) {
		Model3D result = new Model3D();
		for (int len = 0; len < length; len += 1) {
			for (int degree = 0; degree < 360; degree += 10) {

				float x0 = (float) Math.cos(Toolbox.degreeToRadiant(degree)) * radius;
				float y0 = (float) Math.sin(Toolbox.degreeToRadiant(degree)) * radius;
				float z0 = len * 10;
				result.addVertex(x0, y0, z0);
			}

			if (len > 0) {
				int ring0_vertexOffset = (len - 1) * 36;
				int ring1_vertexOffset = len * 36;

				for (int v = 0; v < 36 - 1; v += 1) {
					int p1 = ring0_vertexOffset + v;
					int p2 = ring0_vertexOffset + 1 + v;
					int p3 = ring1_vertexOffset + v;
					int p4 = ring1_vertexOffset + 1 + v;
					result.stretchFace(p1, p2, p4, p3, color);
				}

				// close the gap
				int p1 = ring0_vertexOffset + 35;
				int p2 = ring0_vertexOffset + 0;
				int p3 = ring1_vertexOffset + 35;
				int p4 = ring1_vertexOffset + 0;
				result.stretchFace(p1, p2, p4, p3, color);
			}
		}
		return result;
	}

	public static Model3D createRoundCorner(float radius, Color color) {
		Model3D result = new Model3D();

		result.addVertex(0, 0, 0);
		for (int degree = 0; degree <= 90; degree += 10) {
			result.addVertex((float) Math.cos(Toolbox.degreeToRadiant(degree)) * radius,
					(float) Math.sin(Toolbox.degreeToRadiant(degree)) * radius, 0);
		}

		for (int i = 1; i < result.getVertices().size() - 1; i++) {
			result.stretchFace(0, i + 1, i, color);
		}

		return result;
	}

	public static Model3D createFrame(float cornerRadius, float length, float height, Color color) {
		Model3D result = new Model3D();

		Model3D rightUpperCorner = createRoundCorner(cornerRadius, color);
		rightUpperCorner.move(length / 2, height / 2, 0);
		result.addSubModel(rightUpperCorner);

		Model3D rightLowerCorner = createRoundCorner(cornerRadius, color);
		rightLowerCorner.move(length / 2, -height / 2, 0);
		rightLowerCorner.rotate(0, 0, -90);
		result.addSubModel(rightLowerCorner);

		Model3D leftUpperCorner = createRoundCorner(cornerRadius, color);
		leftUpperCorner.move(-length / 2, height / 2, 0);
		leftUpperCorner.rotate(0, 0, 90);
		result.addSubModel(leftUpperCorner);

		Model3D leftLowerCorner = createRoundCorner(cornerRadius, color);
		leftLowerCorner.move(-length / 2, -height / 2, 0);
		leftLowerCorner.rotate(0, 0, 180);
		result.addSubModel(leftLowerCorner);

		Model3D upperFrame = Model3DGenerator.buildPlane(length, cornerRadius, new Point3D(), color);
		upperFrame.move(0, (height + cornerRadius) / 2f, 0);
		result.addSubModel(upperFrame);

		Model3D lowerFrame = Model3DGenerator.buildPlane(length, cornerRadius, new Point3D(), color);
		lowerFrame.move(0, -(height + cornerRadius) / 2f, 0);
		result.addSubModel(lowerFrame);

		Model3D leftFrame = Model3DGenerator.buildPlane(cornerRadius, height, new Point3D(), color);
		leftFrame.move(-(length + cornerRadius) / 2f, 0, 0);
		result.addSubModel(leftFrame);

		Model3D rightFrame = Model3DGenerator.buildPlane(cornerRadius, height, new Point3D(), color);
		rightFrame.move((length + cornerRadius) / 2f, 0, 0);
		result.addSubModel(rightFrame);

		return result;
	}

	public static Model3D createCurvedBezel(float cornerRadius, float length, float height, Color color) {
		Model3D result = new Model3D();

		Model3D rightUpperCorner = createRoundCorner(cornerRadius, color);
		rightUpperCorner.move(-length / 2, height / 2, 0);
		result.addSubModel(rightUpperCorner);

		Model3D rightLowerCorner = createRoundCorner(cornerRadius, color);
		rightLowerCorner.move(length / 2 - cornerRadius, -height / 2, 0);
		rightLowerCorner.rotate(0, 0, -90);
		result.addSubModel(rightLowerCorner);

		Model3D leftUpperCorner = createRoundCorner(cornerRadius, color);
		leftUpperCorner.move(length / 2, height / 2, 0);
		leftUpperCorner.rotate(0, 0, 90);
		result.addSubModel(leftUpperCorner);

		Model3D leftLowerCorner = createRoundCorner(cornerRadius, color);
		leftLowerCorner.move(-length / 2 + cornerRadius, -height / 2, 0);
		leftLowerCorner.rotate(0, 0, 180);
		result.addSubModel(leftLowerCorner);

		Model3D upperFrameLeft = Model3DGenerator.buildPlane(length, cornerRadius, new Point3D(), color);
		upperFrameLeft.move(-length, (height + cornerRadius) / 2f, 0);
		result.addSubModel(upperFrameLeft);

		Model3D upperFrameRight = Model3DGenerator.buildPlane(length, cornerRadius, new Point3D(), color);
		upperFrameRight.move(length, (height + cornerRadius) / 2f, 0);
		result.addSubModel(upperFrameRight);

		Model3D lowerFrame = Model3DGenerator.buildPlane(length - 2 * cornerRadius, cornerRadius, new Point3D(), color);
		lowerFrame.move(0, -(height + cornerRadius) / 2f, 0);
		result.addSubModel(lowerFrame);
		//
		Model3D leftFrame = Model3DGenerator.buildPlane(cornerRadius, height, new Point3D(), color);
		leftFrame.move(-(length - cornerRadius) / 2f, 0, 0);
		result.addSubModel(leftFrame);

		Model3D rightFrame = Model3DGenerator.buildPlane(cornerRadius, height, new Point3D(), color);
		rightFrame.move((length - cornerRadius) / 2f, 0, 0);
		result.addSubModel(rightFrame);

		return result;
	}

	public static Model3D createCircle(float radius, Color color) {
		Model3D result = new Model3D();
		result.addVertex(0, 0, 0);
		for (int degree = 0; degree <= 360; degree += 10) {
			result.addVertex((float) Math.cos(Toolbox.degreeToRadiant(degree)) * radius, 0,
					(float) Math.sin(Toolbox.degreeToRadiant(degree)) * radius);
		}
		for (int i = 1; i < result.getVertices().size() - 1; i++) {
			result.stretchFace(0, i + 1, i, color);
		}
		return result;
	}

	public static Model3D createRing(float outerRadius, float innerRadius, Color color) {
		Model3D result = new Model3D();

		for (int degree = 0; degree <= 360; degree += 10) {

			result.addVertex((float) Math.cos(Toolbox.degreeToRadiant(degree)) * innerRadius, 0,
					(float) Math.sin(Toolbox.degreeToRadiant(degree)) * innerRadius);

			result.addVertex((float) Math.cos(Toolbox.degreeToRadiant(degree)) * outerRadius, 0,
					(float) Math.sin(Toolbox.degreeToRadiant(degree)) * outerRadius);
		}
		for (int i = 0; i < result.getVertices().size() - 3; i += 2) {
			result.stretchFace(i, i + 1, i + 3, i + 2, color);
		}
		Model3DGenerator.invertFaces(result);

		return result;
	}

	public static void clipAgainstCamera(Model3D model, Camera camera) {
		Matrix3D modelInverse = model.getMatrix().getInverse();
		Point3D camPosInObjectSpace = modelInverse.transform(camera.getOrigin().copy().subtract(model.getOrigin()));
		Vector3D camZInObjectSapce = modelInverse.transform(camera.getZVector().copy());
		Iterator<Face> faceIterator = model.getFaces().iterator();
		while (faceIterator.hasNext()) {
			Face clippedFace = Clipper3D.clip(faceIterator.next(), camPosInObjectSpace, camZInObjectSapce);
			if (clippedFace == null) {
				faceIterator.remove();
			}
		}
	}
}
