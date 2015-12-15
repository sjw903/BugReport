/*
 * Copyright (C), 2002-2014, 苏宁易购电子商务有限公司
 * FileName: ScreenShotsAdapt.java
 * Author:   Nevo
 * Date:     Mar 4, 2014 5:06:58 PM
 * Description: //模块目的、功能描述
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package com.tronxyz.bug_report.ui;


import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.tronxyz.bug_report.R;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉
 *
 * @author Nevo
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class ScreenShotsAdapter extends BaseAdapter {

    private static final int MAX_NUM = 5;

    private Context mContext;
    private LinkedList<String> mScreenShots = new LinkedList<String>();
    public Map<String, SoftReference<Bitmap>> imageCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>();
    /**
    *
    */
    public ScreenShotsAdapter(Context context) {
        // TODO Auto-generated constructor stub
        mContext = context;
    }

    OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            int index = (Integer) v.getTag();
            mScreenShots.remove(index);
            notifyDataSetChanged();
        }
    };

    public void clear() {
        mScreenShots.clear();
        notifyDataSetChanged();
    }

    public void addShotsFile(String path) {
        mScreenShots.addLast(path);
    }

    public void updateShotsFile(int index, String path) {
        mScreenShots.set(index, path);
    }

    public void removeShotsFile(int index) {
        mScreenShots.remove(index);
    }

    public String getShotsFile(int index) {
        if(index < mScreenShots.size())
            return mScreenShots.get(index);
        else
            return null;
    }

    public boolean contains (String path) {
        return mScreenShots.contains(path);
    }

    public LinkedList<String> getFileList() {
        return mScreenShots;
    }

    public int getActualCount() {
        // TODO Auto-generated method stub
        return mScreenShots.size();
    }

    /* (non-Javadoc)
    * @see android.widget.Adapter#getCount()
    */
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mScreenShots.size() >= MAX_NUM ? mScreenShots.size() : mScreenShots.size() + 1;
    }

    /* (non-Javadoc)
    * @see android.widget.Adapter#getItem(int)
    */
    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
    * @see android.widget.Adapter#getItemId(int)
    */
    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    /* (non-Javadoc)
    * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
    */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(null == convertView) {
            convertView = inflater.inflate(R.layout.grid_view_item, null);
        }

        ImageView del = (ImageView)convertView.findViewById(R.id.grid_del);
        ImageView imageView = (ImageView)convertView.findViewById(R.id.grid_item);
        if(mScreenShots.size()-1 >= position) {
            String path = mScreenShots.get(position);
            Bitmap bit = null;

            if (imageCache.containsKey(path)) {

                SoftReference<Bitmap> softReference = imageCache.get(path);
                if (softReference.get() != null) {
                    bit = softReference.get();
                }
            }
            if(null == bit) {
                bit = createSpecifyBitmap(path);
                imageCache.put(path, new SoftReference<Bitmap>(bit));
            }
            imageView.setImageBitmap(bit);

            del.setVisibility(View.VISIBLE);
            del.setTag(position);
            del.setOnClickListener(mOnClickListener);

        } else {
            del.setVisibility(View.GONE);
            imageView.setImageResource(R.drawable.report_fragment_add);
        }

        return convertView;
    }

    private Bitmap createSpecifyBitmap(String path) {
        Options op = new Options();
        op.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, op);
        op.inSampleSize = calculateInSampleSize(op, 120, 120);

        // Decode with inSampleSize
        op.inJustDecodeBounds = false;
        op.inDither = false;
        op.inScaled = false;
        op.inPreferredConfig = Bitmap.Config.RGB_565;
        op.inPurgeable = true;
        return BitmapFactory.decodeFile(path, op);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio > widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    public boolean isShotsEmpty() {
        if (null != mScreenShots && mScreenShots.size() > 0) {
            return false;
        }
        return true;
    }
}
