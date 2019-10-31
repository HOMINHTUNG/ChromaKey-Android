package com.tungjobs.chromakeyvideo.gpuimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tuan.np on 3/22/2017.
 */

public class ImageAdapter extends BaseAdapter {
    final String DIRECTORY = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    private Context mContext;
    private List<Bitmap> array = new ArrayList<Bitmap>();

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
    public boolean exportToMP4() {
        if (array.size() == 0)
            return false;

        File directory = new File(DIRECTORY + "/video_frame");
        if (directory.isDirectory())
        {
            String[] children = directory.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(directory, children[i]).delete();
            }
        }
        //create image frame
        File myDir = new File(DIRECTORY + "/video_frame");
        myDir.mkdirs();
        for (int i=0; i<array.size();i++) {
            String fname = "frame-" + i + ".png";
            File file = new File(myDir, fname);
            if (file.exists())
                file.delete();
            try {
                FileOutputStream out = new FileOutputStream(file);
                array.get(i).setHasAlpha(true);
                array.get(i).compress(Bitmap.CompressFormat.PNG, 80, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    }
}
