package org.duckdns.dcnick3.learnenglish.layout;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.duckdns.dcnick3.learnenglish.R;


public class VocaburaryFragment extends Fragment {

    public VocaburaryFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vocablurary, container, false);

        view.findViewById(R.id.learn_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), LearningActivity.class);
                startActivity(i);
            }
        });

        return view;
    }
}
