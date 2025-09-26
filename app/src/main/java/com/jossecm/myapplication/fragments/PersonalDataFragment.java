package com.jossecm.myapplication.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.User;
import android.widget.TextView;

public class PersonalDataFragment extends Fragment {

    private TextInputEditText etName, etAge, etHeight, etWeight;
    private RadioGroup rgGender;
    private TextView tvBMI, tvBMIDescription;

    public static PersonalDataFragment newInstance() {
        return new PersonalDataFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_personal_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupBMICalculation();
    }

    private void initViews(View view) {
        etName = view.findViewById(R.id.etName);
        etAge = view.findViewById(R.id.etAge);
        etHeight = view.findViewById(R.id.etHeight);
        etWeight = view.findViewById(R.id.etWeight);
        rgGender = view.findViewById(R.id.rgGender);
        tvBMI = view.findViewById(R.id.tvBMI);
        tvBMIDescription = view.findViewById(R.id.tvBMIDescription);
    }

    private void setupBMICalculation() {
        TextWatcher bmiWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateBMI();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etHeight.addTextChangedListener(bmiWatcher);
        etWeight.addTextChangedListener(bmiWatcher);
    }

    private void calculateBMI() {
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();

        if (!heightStr.isEmpty() && !weightStr.isEmpty()) {
            try {
                double height = Double.parseDouble(heightStr);
                double weight = Double.parseDouble(weightStr);

                if (height > 0 && weight > 0) {
                    double heightInMeters = height / 100.0;
                    double bmi = weight / (heightInMeters * heightInMeters);

                    tvBMI.setText(String.format("%.1f", bmi));

                    String description;
                    if (bmi < 18.5) description = "Bajo peso";
                    else if (bmi < 25) description = "Peso normal";
                    else if (bmi < 30) description = "Sobrepeso";
                    else description = "Obesidad";

                    tvBMIDescription.setText(description);
                    return;
                }
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        }

        tvBMI.setText("--");
        tvBMIDescription.setText("Introduce tu altura y peso");
    }

    public boolean isValid() {
        return !etName.getText().toString().trim().isEmpty() &&
               !etAge.getText().toString().trim().isEmpty() &&
               !etHeight.getText().toString().trim().isEmpty() &&
               !etWeight.getText().toString().trim().isEmpty() &&
               rgGender.getCheckedRadioButtonId() != -1;
    }

    public void fillUserData(User user) {
        if (!isValid()) return;

        user.setName(etName.getText().toString().trim());
        user.setAge(Integer.parseInt(etAge.getText().toString().trim()));
        user.setHeight(Double.parseDouble(etHeight.getText().toString().trim()));
        user.setWeight(Double.parseDouble(etWeight.getText().toString().trim()));

        if (rgGender.getCheckedRadioButtonId() == R.id.rbMale) {
            user.setGender(User.Gender.MALE);
        } else {
            user.setGender(User.Gender.FEMALE);
        }
    }
}
