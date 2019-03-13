package com.edexelroots.android.sensoriot.vision;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.edexelroots.android.sensoriot.R;
import com.edexelroots.android.sensoriot.vision.FaceMatchFragment.OnFaceFragmentListener;

import org.apache.commons.text.StringEscapeUtils;
import java.util.List;


/**
 * {@link RecyclerView.Adapter} that can display a {@link FaceMatchItem} and makes a call to the
 * specified {@link OnFaceFragmentListener}.
 */
public class FaceMatchAdapter extends RecyclerView.Adapter<FaceMatchAdapter.ViewHolder> {

    private final List<FaceMatchItem> mValues;
    private final OnFaceFragmentListener mListener;

    public FaceMatchAdapter(List<FaceMatchItem> items, OnFaceFragmentListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.fragment_facematch, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mNameView.setText(holder.mItem.name + "  (" + holder.mItem.similarity + ")");

        String unescaped = StringEscapeUtils.unescapeHtml4(holder.mItem.subtitle);
        String out = StringEscapeUtils.unescapeJava(unescaped);
        holder.mSubtitleView.setText(out);

        // holder.mCounter.setText(holder.mItem.counter + "");
        holder.mImageView.setImageBitmap(holder.mItem.image);

        holder.mView.setOnClickListener(v -> {
            if (null != mListener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                mListener.OnFaceItemClicked(holder.mItem);
            }
        });
    }

    public void clear() {
        if (this.mValues != null) {
            this.mValues.clear();
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public FaceMatchItem mItem;
        public final View mView;
        public final TextView mNameView;
        public final TextView mSubtitleView;
        public final ImageView mImageView;
        public final View mHighlightView;

        // public final TextView mCounter;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mNameView = view.findViewById(R.id.name);
            mSubtitleView = view.findViewById(R.id.subtitle);
            mImageView = view.findViewById(R.id.cropped);
            //mCounter = view.findViewById(R.id.counter);
            this.mHighlightView = view.findViewById(R.id.highlight_view);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }
    }


}
