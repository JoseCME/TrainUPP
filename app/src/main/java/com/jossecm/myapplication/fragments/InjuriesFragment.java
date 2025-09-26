package com.jossecm.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.User;
import java.util.ArrayList;
import java.util.List;

public class InjuriesFragment extends Fragment {

    private CheckBox cbShoulderInjury, cbKneeInjury, cbLowerBackInjury,
                     cbWristInjury, cbAnkleInjury, cbNeckInjury;
    private TextInputEditText etAdditionalInjuries;

    public static InjuriesFragment newInstance() {
        return new InjuriesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_injuries, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    private void initViews(View view) {
        cbShoulderInjury = view.findViewById(R.id.cbShoulderInjury);
        cbKneeInjury = view.findViewById(R.id.cbKneeInjury);
        cbLowerBackInjury = view.findViewById(R.id.cbLowerBackInjury);
        cbWristInjury = view.findViewById(R.id.cbWristInjury);
        cbAnkleInjury = view.findViewById(R.id.cbAnkleInjury);
        cbNeckInjury = view.findViewById(R.id.cbNeckInjury);
        etAdditionalInjuries = view.findViewById(R.id.etAdditionalInjuries);
    }

    public boolean isValid() {
        // Este fragmento siempre es válido ya que las lesiones son opcionales
        return true;
    }

    public void fillUserData(User user) {
        List<String> injuries = new ArrayList<>();

        if (cbShoulderInjury.isChecked()) injuries.add("Lesión de hombro");
        if (cbKneeInjury.isChecked()) injuries.add("Lesión de rodilla");
        if (cbLowerBackInjury.isChecked()) injuries.add("Lesión de espalda baja");
        if (cbWristInjury.isChecked()) injuries.add("Lesión de muñeca");
        if (cbAnkleInjury.isChecked()) injuries.add("Lesión de tobillo");
        if (cbNeckInjury.isChecked()) injuries.add("Lesión de cuello");

        user.setInjuries(injuries);

        String additionalInjuries = etAdditionalInjuries.getText().toString().trim();
        if (!additionalInjuries.isEmpty()) {
            user.setAdditionalInjuries(additionalInjuries);
        }
    }
}
