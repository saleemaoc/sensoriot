package com.edexelroots.android.sensoriot.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.edexelroots.android.sensoriot.R;
import com.edexelroots.android.sensoriot.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFaceFragmentListener}
 * interface.
 */
public class FaceMatchFragment extends Fragment {

    private OnFaceFragmentListener mListener;

    public static List<FaceMatchItem> mItems = new ArrayList<>();
    FaceMatchAdapter mAdapter = null;
    private String threeDots = " Loading ... ";
    RecyclerView recyclerView = null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FaceMatchFragment() {
    }

    @SuppressWarnings("unused")
    public static FaceMatchFragment newInstance(int columnCount) {
        FaceMatchFragment fragment = new FaceMatchFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_facematch_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            mAdapter = new FaceMatchAdapter(mItems, mListener);
            recyclerView.setAdapter(mAdapter);
        }
        return view;
    }

/*    public FaceMatchItem addNewFace(String id, float similarity, Bitmap image) {
        FaceMatchItem fmi = new FaceMatchItem(id, similarity, threeDots, image);
        mItems.add(0, fmi);
        return fmi;
    }*/

/*
* @return true if this is a new face being added to the list.. false otherwise
* */
    public boolean addNewFace(FaceMatchItem fmi) {
        FaceMatchItem existing = null;
        for (FaceMatchItem i : mItems) {
            if (i.awsFaceId.contentEquals(fmi.awsFaceId)) {
                // already have recognized this face..
                Utils.logE(getClass().getName(), "Face already exists");
                existing = i;
                break;
            }
        }
        if (existing == null) {
            Utils.logE(getClass().getName(), "Adding Face to List");
            mItems.add(0, fmi);
            notifyDataSetChanged();
            recyclerView.scheduleLayoutAnimation();
        } else if (existing.similarity < fmi.similarity) {
            existing.similarity = fmi.similarity;
            existing.image = fmi.image;
            // existing.blink = true;
            notifyDataSetChanged();
        } else {
            existing.counter++;
            notifyDataSetChanged();
        }
        return existing == null;
    }

    public void removeFace(FaceMatchItem fmi) {
        mItems.remove(fmi);
        mAdapter.notifyItemRemoved(mItems.indexOf(fmi));
        mAdapter.notifyDataSetChanged();
    }

    public void clear() {
        this.mAdapter.clear();
    }

//    HashMap<String, FaceMatchItem> map = new HashMap<>();

    public void notifyDataSetChanged() {
        if(mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFaceFragmentListener) {
            mListener = (OnFaceFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFaceFragmentListener {
        void OnFaceItemClicked(FaceMatchItem item);
    }
}
