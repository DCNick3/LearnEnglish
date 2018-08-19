package org.duckdns.dcnick3.learnenglish.layout;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import org.duckdns.dcnick3.learnenglish.R;

public class PrefsFragment extends PreferenceFragment {

    public PrefsFragment() {
    }

    public static PrefsFragment newInstance() {
        PrefsFragment fragment = new PrefsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}
