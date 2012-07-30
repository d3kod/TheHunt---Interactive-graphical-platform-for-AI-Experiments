package d3kod.d3gles20;

import java.nio.FloatBuffer;

import android.opengl.GLES20;

public class D3Circle extends D3Shape {

	public D3Circle(float r, float[] color, int vertices, boolean useDefaultShaders) {
		super(vertices, makeCircleVerticesBuffer(r, vertices), color, GLES20.GL_LINE_LOOP, useDefaultShaders);
	}

	private static FloatBuffer makeCircleVerticesBuffer(float r, int vertices) {
		FloatBuffer buffer = D3GLES20.newFloatBuffer(D3GLES20.circleVerticesData(new float[] {0, 0}, r, vertices));
		return buffer;
	}

}
