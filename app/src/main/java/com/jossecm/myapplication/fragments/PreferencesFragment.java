package com.jossecm.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.User;

public class PreferencesFragment extends Fragment {

    private RadioGroup rgAiPreference;

    public static PreferencesFragment newInstance() {
        return new PreferencesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preferences, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    private void initViews(View view) {
        rgAiPreference = view.findViewById(R.id.rgAiPreference);
    }

    public boolean isValid() {
        return rgAiPreference.getCheckedRadioButtonId() != -1;
    }

    public void fillUserData(User user) {
        if (!isValid()) return;

        boolean wantsAi = rgAiPreference.getCheckedRadioButtonId() == R.id.rbAiYes;
        user.setWantsAiRoutines(wantsAi);
    }
}
