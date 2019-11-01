package com.tungjobs.chromakeyvideo.gpuimage;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import org.jcodec.api.SequenceEncoder;
import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tuan.np on 3/22/2017.
 */

public class ImageAdapter extends BaseAdapter {
    final String DIRECTORY = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    private Context mContext;
    private List<Bitmap> array = new ArrayList<Bitmap>();
    private SequenceEncoder sequenceEncoder = null;

    public ImageAdapter(Context c) {
        mContext = c;
    }

    public int getCount() {
        return array.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(300, 300));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageBitmap(array.get(position));
        return imageView;
    }

    public void addImage(Bitmap bitmap) {
        array.add(bitmap);
        notifyDataSetChanged();
    }
    public void deleteImage(int pos) {
        array.remove(pos);
        notifyDataSetChanged();
    }
    public void exportToMP4(File file) {
        if (array.size() == 0)
            return;

        Log.d("convertImagetoVideo", "-----Progressing-----");
        FileChannelWrapper out = null;
        try {
            out = NIOUtils.writableFileChannel(file.getAbsolutePath());
            AndroidSequenceEncoder encoder = new AndroidSequenceEncoder(out, Rational.R(15, 1));
            for (Bitmap bitmap : array) {
                encoder.encodeImage(bitmap);
            }
            encoder.finish();
            Log.d("convertImagetoVideo", "-----Finish-----");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            NIOUtils.closeQuietly(out);
        }
    }
}
