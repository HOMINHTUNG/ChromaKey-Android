package com.tungjobs.chromakeyvideo.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.tungjobs.chromakeyvideo.R;
import com.tungjobs.chromakeyvideo.gpuimage.ImageAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FFMPEGObject {
    private final String PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    private ImageAdapter imageAdapter;
    private ProgressDialog progress;
    private Context context;

    public FFMPEGObject(Context context,ImageAdapter imageAdapter){
        this.setContext(context);
        this.setImageAdapter(imageAdapter);
        init();
    }

    public void init(){
        initFFmpeg();
    }

    private void initFFmpeg() {
        FFmpeg ffmpeg = FFmpeg.getInstance(getContext());
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onStart() {}
                @Override
                public void onFailure() {}
                @Override
                public void onSuccess() {}
                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            Toast.makeText(getContext(),"Device not supported",Toast.LENGTH_SHORT).show();
        }
    }

    private void addAudio() {
        try {
            copyAudiotoSDCard(R.raw.audio, PATH + "/audio.mp3");
        }
        catch (IOException e) {

        }
        FFmpeg ffmpeg = ffmpeg = FFmpeg.getInstance(context);
        //add audio to video, do not care length
        //-i input.mp4 -i input.mp3 -c copy -map 0:0 -map 1:0 output.mp4
        //add audio to video, care length, priority video length.
        //-i input.mp4 -i input.mp3 -filter_complex [1:0]apad -map 0:0 -map 1:0 -shortest output.mp4
        String[] cmdAddAudio = {
                "-i",
                PATH +"/output.mp4",
                "-i",
                PATH + "/audio.mp3",
//                "-filter_complex",
//                "[1:0]apad",
                "-c",
                "copy",
                "-map",
                "0:v",
                "-map",
                "1:a",
//                "-shortest",
                PATH +"/video.mp4"};
        try {
            ffmpeg.execute(cmdAddAudio, new ExecuteBinaryResponseHandler() {
                @Override
                public void onStart() {
                    progress = ProgressDialog.show(context, "Progressing", "Adding audio", true);
                }
                @Override
                public void onProgress(String message) {}
                @Override
                public void onFailure(String message) {
                    String s = message;
                    progress.dismiss();
                    Toast.makeText(context,"FFmpeg add audio onFailure",Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onSuccess(String message) {
                    progress.dismiss();
                    clearTempFile();
                }
                @Override
                public void onFinish() {}
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }
    }

    private void copyAudiotoSDCard(int resourceID, String path) throws IOException {
        InputStream in = context.getResources().openRawResource(resourceID);
        FileOutputStream out = new FileOutputStream(path);
        byte[] buff = new byte[1024];
        int read = 0;
        try {
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
        } finally {
            in.close();
            out.close();
        }
    }

    public void createMP4() {
        File file = new File(PATH +"/output.mp4");
        file.delete();
        file = new File(PATH +"/video.mp4");
        file.delete();
        FFmpeg ffmpeg = FFmpeg.getInstance(getContext());
        String[] cmdCreateMP4 = {
                "-framerate",
                "1",
                "-i",
                PATH +"/video_frame/frame-%00d.png",
                "-c:v",
                "libx264",
                "-profile:v",
                "high",
                "-crf",
                "20",
                "-pix_fmt",
                "yuv420p",
                PATH +"/output.mp4"};
        try {
            ffmpeg.execute(cmdCreateMP4, new ExecuteBinaryResponseHandler() {
                @Override
                public void onStart() {progress = ProgressDialog.show(getContext(), "Progressing",
                        "Creating MP4", true);}
                @Override
                public void onProgress(String message) {}
                @Override
                public void onFailure(String message) {
                    String s = message;
                    progress.dismiss();
                    Toast.makeText(getContext(),"FFmpeg create mp4 onFailure",Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onSuccess(String message) {
                    progress.dismiss();
                    addAudio();
                }
                @Override
                public void onFinish() {}
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }
    }

    private void clearTempFile() {
        File directory = new File(PATH + "/video_frame");
        if (directory.isDirectory())
        {
            String[] children = directory.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(directory, children[i]).delete();
            }
        }
        directory.delete();
        new File(PATH + "/audio.mp3").delete();
        new File(PATH + "/output.mp4").delete();
    }

    public ImageAdapter getImageAdapter() {
        return imageAdapter;
    }

    public void setImageAdapter(ImageAdapter imageAdapter) {
        this.imageAdapter = imageAdapter;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
