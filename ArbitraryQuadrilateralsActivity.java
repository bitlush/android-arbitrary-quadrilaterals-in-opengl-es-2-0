import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class ArbitraryQuadrilateralsActivity extends Activity implements Renderer {
	private GLSurfaceView view;
	int[] status = new int[1];
	int program;
	FloatBuffer attributeBuffer;
	ShortBuffer indicesBuffer;
	short[] indicesData;
	float[] attributesData;
	private int[] textureIds = new int[1];
	int textureId;
	int attributePosition;
	int attributeRegion;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		view = new GLSurfaceView(this);
		view.setEGLContextClientVersion(2);
		view.setRenderer(this);

		setContentView(view);
	}

	@Override
	public void onPause() {
		view.onPause();

		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();

		view.onResume();
	}

	public void onDrawFrame(GL10 gl) {
		GLES20.glClearColor(1f, 1f, 1f, 1f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

		drawNonAffine(100, 100, 600, 100, 500, 400, 200, 600);

		attributeBuffer.position(0);
		attributeBuffer.put(attributesData);

		attributeBuffer.position(0);
		GLES20.glVertexAttribPointer(attributePosition, 2, GLES20.GL_FLOAT, false, 5 * 4, attributeBuffer);
		GLES20.glEnableVertexAttribArray(attributePosition);

		attributeBuffer.position(2);
		GLES20.glVertexAttribPointer(attributeRegion, 3, GLES20.GL_FLOAT, false, 5 * 4, attributeBuffer);
		GLES20.glEnableVertexAttribArray(attributeRegion);

		indicesBuffer.position(0);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indicesBuffer);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		String vertexShaderSource =
			"attribute vec2 a_Position;" +
			"attribute vec3 a_Region;" +
			"varying vec3 v_Region;" +
			"uniform mat3 u_World;" +
			"void main()" +
			"{" +
			"   v_Region = a_Region;" +
			"   vec3 xyz = u_World * vec3(a_Position, 1);" +
			"   gl_Position = vec4(xyz.xy, 0, 1);" +
			"}";

		String fragmentShaderSource =
			"precision mediump float;" +
			"varying vec3 v_Region;" +
			"uniform sampler2D u_TextureId;" +
			"void main()" +
			"{" +
			"   gl_FragColor = texture2D(u_TextureId, v_Region.xy / v_Region.z);" +
			"}";

		attributeBuffer = ByteBuffer.allocateDirect(5 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		attributesData = new float[5 * 4];

		indicesBuffer = ByteBuffer.allocateDirect(6 * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
		indicesData = new short[] { 0, 1, 2, 2, 3, 0 };

		indicesBuffer.position(0);
		indicesBuffer.put(indicesData);

		program = loadProgram(vertexShaderSource, fragmentShaderSource);
		textureId = loadTexture("Grid.png");

		GLES20.glUseProgram(program);

		float width = view.getWidth();
		float height = view.getHeight();

		float world[] = new float[] {
			2f / width, 0, 0,
			0, 2f / height, 0,
			-1f, -1f, 1
		};

		int uniformWorld = GLES20.glGetUniformLocation(program, "u_World");
		int uniformTextureId = GLES20.glGetUniformLocation(program, "u_TextureId");

		GLES20.glUniformMatrix3fv(uniformWorld, 1, false, world, 0);
		GLES20.glUniform1i(uniformTextureId, 0);

		attributePosition = GLES20.glGetAttribLocation(program, "a_Position");
		attributeRegion = GLES20.glGetAttribLocation(program, "a_Region");
	}

	public void setFilters(int minFilter, int magFilter) {
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, minFilter);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, magFilter);
	}

	public void setWrapping(int wrapS, int wrapT) {
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrapS);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrapT);
	}

	public void drawNonAffine(float bottomLeftX, float bottomLeftY, float bottomRightX, float bottomRightY, float topRightX, float topRightY, float topLeftX, float topLeftY) {
		float ax = topRightX - bottomLeftX;
		float ay = topRightY - bottomLeftY;
		float bx = topLeftX - bottomRightX;
		float by = topLeftY - bottomRightY;

		float cross = ax * by - ay * bx;

		boolean rendered = false;

		if (cross != 0) {
			float cy = bottomLeftY - bottomRightY;
			float cx = bottomLeftX - bottomRightX;

			float s = (ax * cy - ay * cx) / cross;

			if (s > 0 && s < 1) {
				float t = (bx * cy - by * cx) / cross;

				if (t > 0 && t < 1) {
					//uv coordinates for texture
					float u0 = 0; // texture bottom left u
					float v0 = 0; // texture bottom left v
					float u2 = 1; // texture top right u
					float v2 = 1; // texture top right v

					int bufferIndex = 0;

					float q0 = 1 / (1 - t);
					float q1 = 1 / (1 - s);
					float q2 = 1 / t;
					float q3 = 1 / s;

					attributesData[bufferIndex++] = bottomLeftX;
					attributesData[bufferIndex++] = bottomLeftY;
					attributesData[bufferIndex++] = u0 * q0;
					attributesData[bufferIndex++] = v2 * q0;
					attributesData[bufferIndex++] = q0;

					attributesData[bufferIndex++] = bottomRightX;
					attributesData[bufferIndex++] = bottomRightY;
					attributesData[bufferIndex++] = u2 * q1;
					attributesData[bufferIndex++] = v2 * q1;
					attributesData[bufferIndex++] = q1;

					attributesData[bufferIndex++] = topRightX;
					attributesData[bufferIndex++] = topRightY;
					attributesData[bufferIndex++] = u2 * q2;
					attributesData[bufferIndex++] = v0 * q2;
					attributesData[bufferIndex++] = q2;

					attributesData[bufferIndex++] = topLeftX;
					attributesData[bufferIndex++] = topLeftY;
					attributesData[bufferIndex++] = u0 * q3;
					attributesData[bufferIndex++] = v0 * q3;
					attributesData[bufferIndex++] = q3;

					rendered = true;
				}
			}
		}

		if (!rendered) {
			throw new RuntimeException("Shape must be concave and vertices must be clockwise.");
		}
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
	}

	private int loadTexture(String assetName) {
		Bitmap bitmap;

		try {
			bitmap = BitmapFactory.decodeStream(getAssets().open(assetName));
		}
		catch (Exception e) {
			throw new RuntimeException("Couldn't load image '" + assetName + "'.", e);
		}

		if (bitmap == null) {
			throw new RuntimeException("Couldn't load image '" + assetName + "'.");
		}

		GLES20.glGenTextures(1, textureIds, 0);

		int textureId = textureIds[0];

		if (textureId == 0) {
			throw new RuntimeException("Could not generate texture.");
		}

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

		setFilters(GLES20.GL_LINEAR, GLES20.GL_LINEAR);
		setWrapping(GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

		bitmap.recycle();

		return textureId;
	}

	private int loadProgram(String vertexShaderSource, String fragmentShaderSource) {
		int id = GLES20.glCreateProgram();

		int vertexShaderId = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource);
		int fragmentShaderId = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource);

		GLES20.glAttachShader(id, vertexShaderId);
		GLES20.glAttachShader(id, fragmentShaderId);
		GLES20.glLinkProgram(id);
		GLES20.glDeleteShader(vertexShaderId);
		GLES20.glDeleteShader(fragmentShaderId);
		GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, status, 0);

		if (status[0] == 0) {
			String log = GLES20.glGetProgramInfoLog(id);

			GLES20.glDeleteProgram(id);

			throw new RuntimeException("Shader error:" + log);
		}

		return id;
	}

	private int loadShader(int type, String source) {
		int id = GLES20.glCreateShader(type);

		GLES20.glShaderSource(id, source);
		GLES20.glCompileShader(id);
		GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, status, 0);

		if (status[0] == 0) {
			String log = GLES20.glGetShaderInfoLog(id);

			GLES20.glDeleteShader(id);

			throw new RuntimeException("Shader error:" + log);
		}

		return id;
	}

	private void checkGlError(String op) {
		int error;

		if ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			throw new RuntimeException("GL error code: " + error + " for " + op + ".");
		}
	}
}
