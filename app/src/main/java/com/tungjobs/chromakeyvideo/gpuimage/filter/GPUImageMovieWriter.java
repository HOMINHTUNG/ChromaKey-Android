package com.tungjobs.chromakeyvideo.gpuimage.filter;

import android.graphics.PixelFormat;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;

import com.tungjobs.chromakeyvideo.gpuimage.encoder.EglCore;
import com.tungjobs.chromakeyvideo.gpuimage.encoder.MediaAudioEncoder;
import com.tungjobs.chromakeyvideo.gpuimage.encoder.MediaEncoder;
import com.tungjobs.chromakeyvideo.gpuimage.encoder.MediaMuxerWrapper;
import com.tungjobs.chromakeyvideo.gpuimage.encoder.MediaVideoEncoder;
import com.tungjobs.chromakeyvideo.gpuimage.encoder.WindowSurface;

import java.io.IOException;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class GPUImageMovieWriter extends GPUImageFilter {
    private MediaMuxerWrapper mMuxer;
    private MediaVideoEncoder mVideoEncoder;
    private MediaAudioEncoder mAudioEncoder;
    private WindowSurface mCodecInput;

    private EGLSurface mEGLScreenSurface;
    private EGL10 mEGL;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EglCore mEGLCore;

    private boolean mIsRecording = false;

    @Override
    public void onInit() {
        super.onInit();
        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetCurrentDisplay();
        mEGLContext = mEGL.eglGetCurrentContext();
        mEGLScreenSurface = mEGL.eglGetCurrentSurface(EGL10.EGL_DRAW);
    }

//    public void setGLSurfaceView(final GLSurfaceView view) {
//        mGlSurfaceView = view;
//        mGlSurfaceView.setEGLContextClientVersion(2);
//        mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
//        mGlSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
//        mGlSurfaceView.setRenderer(mRenderer);
//        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//        mGlSurfaceView.requestRender();
//    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        // Draw on screen surface
        super.onDraw(textureId, cubeBuffer, textureBuffer);

        if (mIsRecording) {
            // create encoder surface
            if (mCodecInput == null) {
                mEGLCore = new EglCore(EGL14.eglGetCurrentContext(), EglCore.FLAG_RECORDABLE);
                mCodecInput = new WindowSurface(mEGLCore, mVideoEncoder.getSurface(), false);
            }

            // Draw on encoder surface
            mCodecInput.makeCurrent();
            super.onDraw(textureId, cubeBuffer, textureBuffer);
            mCodecInput.swapBuffers();
            mVideoEncoder.frameAvailableSoon();
        }

        // Make screen surface be current surface
        mEGL.eglMakeCurrent(mEGLDisplay, mEGLScreenSurface, mEGLScreenSurface, mEGLContext);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseEncodeSurface();
    }

    public void startRecording(final String outputPath, final int width, final int height) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                if (mIsRecording) {
                    return;
                }

                try {
                    mMuxer = new MediaMuxerWrapper(outputPath);

                    // for video capturing
                    mVideoEncoder = new MediaVideoEncoder(mMuxer, mMediaEncoderListener, width, height);
                    // for audio capturing
                    mAudioEncoder = new MediaAudioEncoder(mMuxer, mMediaEncoderListener);

                    mMuxer.prepare();
                    mMuxer.startRecording();

                    mIsRecording = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stopRecording() {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                if (!mIsRecording) {
                    return;
                }

                mMuxer.stopRecording();
                mIsRecording = false;
                releaseEncodeSurface();
            }
        });
    }

    private void releaseEncodeSurface() {
        if (mEGLCore != null) {
            mEGLCore.makeNothingCurrent();
            mEGLCore.release();
            mEGLCore = null;
        }

        if (mCodecInput != null) {
            mCodecInput.release();
            mCodecInput = null;
        }
    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
        }

        @Override
        public void onMuxerStopped() {
        }
    };
}
