package com.tungjobs.chromakeyvideo.gpuimage.filter;

import android.opengl.GLES20;
import android.util.Log;

import jp.co.cyberagent.android.gpuimage.filter.GPUImage3x3TextureSamplingFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageTwoInputFilter;

public class ChromaKeyFilter extends GPUImageTwoInputFilter {
    public static final String CHROMA_KEY_BLEND_FRAGMENT_SHADER_CAMERA =
            " precision highp float;\n" +
                    " \n" + "const vec3 W = vec3(1.0, 1.0, 1.0);\n" + " \n"+
                    " varying highp vec2 textureCoordinate;\n" +
                    " varying highp vec2 textureCoordinate2;\n" +
                    "\n" +
                    " uniform float thresholdSensitivity;\n" +
                    " uniform float smoothing;\n" +
                    " uniform vec3 colorToReplace;\n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " uniform sampler2D inputImageTexture2;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "     vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "     vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n" +
                    "     \n" +
                    "     float maskY = 0.2989 * colorToReplace.r + 0.5866 * colorToReplace.g + 0.1145 * colorToReplace.b;\n" +
                    "     float maskCr = 0.7132 * (colorToReplace.r - maskY);\n" +
                    "     float maskCb = 0.5647 * (colorToReplace.b - maskY);\n" +
                    "     \n" +
                    "     float Y = 0.2989 * textureColor.r + 0.5866 * textureColor.g + 0.1145 * textureColor.b;\n" +
                    "     float Cr = 0.7132 * (textureColor.r - Y);\n" +
                    "     float Cb = 0.5647 * (textureColor.b - Y);\n" +
                    "     float L = dot(textureColor.rgb, W);\n" +
                    "     float maskL = dot(colorToReplace.rgb, W);\n" +
                    "     \n" +
                    "     float blendValue = smoothstep(thresholdSensitivity, thresholdSensitivity + smoothing, distance(vec3(Cr, Cb, L), vec3(maskCr, maskCb, maskL)));\n" +
//                    "     gl_FragColor = mix(textureColor, textureColor2, blendValue);\n" +
                    "     gl_FragColor = vec4(textureColor.rgb, textureColor.a * blendValue);\n" +
                    " }";

//
    private int thresholdSensitivityLocation;
    private int smoothingLocation;
    private int colorToReplaceLocation;
    private float thresholdSensitivity = 0.2f;
    private float smoothing = 1.0f;
    static float[] colorToReplace = new float[]{1.0f, 1.0f, 1.0f};

    public ChromaKeyFilter() {
        super(CHROMA_KEY_BLEND_FRAGMENT_SHADER_CAMERA);
    }

    public ChromaKeyFilter(String type) {
        super(type);
    }

    public ChromaKeyFilter(String type, float red, float green, float blue) {
        super(type);
        colorToReplace = new float[]{red, green, blue};
        Log.d("ChromaKeyFilter", "colorToReplace");
    }

    @Override
    public void onInit() {
        super.onInit();
        thresholdSensitivityLocation = GLES20.glGetUniformLocation(getProgram(), "thresholdSensitivity");
        smoothingLocation = GLES20.glGetUniformLocation(getProgram(), "smoothing");
        colorToReplaceLocation = GLES20.glGetUniformLocation(getProgram(), "colorToReplace");
        Log.d("ChromaKeyFilter", "onInit");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setSmoothing(smoothing);
        setThresholdSensitivity(thresholdSensitivity);
        setColorToReplace(colorToReplace[0], colorToReplace[1], colorToReplace[2]);
        Log.d("ChromaKeyFilter", "onInitialized");
    }

    /**
     * The degree of smoothing controls how gradually similar colors are replaced in the image
     * The default value is 0.1
     */
    public void setSmoothing(final float smoothing) {
        this.smoothing = smoothing;
        setFloat(smoothingLocation, this.smoothing);
    }

    /**
     * The threshold sensitivity controls how similar pixels need to be colored to be replaced
     * The default value is 0.3
     */
    public void setThresholdSensitivity(final float thresholdSensitivity) {
        this.thresholdSensitivity = thresholdSensitivity;
        setFloat(thresholdSensitivityLocation, this.thresholdSensitivity);
    }

    /**
     * The color to be replaced is specified using individual red, green, and blue components (normalized to 1.0).
     * The default is green: (0.0, 1.0, 0.0).
     *
     * @param redComponent   Red component of color to be replaced
     * @param greenComponent Green component of color to be replaced
     * @param blueComponent  Blue component of color to be replaced
     */
    public void setColorToReplace(float redComponent, float greenComponent, float blueComponent) {
        colorToReplace = new float[]{redComponent, greenComponent, blueComponent};
        setFloatVec3(colorToReplaceLocation, colorToReplace);
    }
}