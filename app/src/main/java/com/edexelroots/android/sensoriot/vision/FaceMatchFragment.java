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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class FaceMatchFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FaceMatchFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static FaceMatchFragment newInstance(int columnCount) {
        FaceMatchFragment fragment = new FaceMatchFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    List<FaceMatchItem> mItems = new ArrayList<>();
    FaceMatchAdapter mAdapter = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_facematch_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            mAdapter = new FaceMatchAdapter(mItems, mListener);
            recyclerView.setAdapter(mAdapter);
        }
        return view;
    }

    private String threeDots = "...";
    public FaceMatchItem addNewFace(long id, float similarity, Bitmap image) {
        FaceMatchItem fmi = new FaceMatchItem(id, similarity,threeDots, image);
        mItems.add(0, fmi);
        mAdapter.notifyDataSetChanged();
        return fmi;
    }

    public boolean contains(String externalImageId) {
        return map.containsKey(externalImageId);
    }

    public void removeFace(FaceMatchItem fmi) {
        mItems.remove(fmi);
        mAdapter.notifyDataSetChanged();
    }

    HashMap<String, FaceMatchItem> map = new HashMap<>();
    public void notifyDataSetChanged() {
        for(FaceMatchItem f: mItems) {
            if(f.name.contentEquals(threeDots)) {
                continue;
            }
            map.put(f.name, f);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(FaceMatchItem item);
    }
}
