package com.jossecm.myapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.User;

public class FitnessDataFragment extends Fragment {

    private RadioGroup rgExperienceLevel, rgFitnessGoal;
    private SeekBar seekBarDays;
    private TextView tvDaysValue;

    public static FitnessDataFragment newInstance() {
        return new FitnessDataFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fitness_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupSeekBar();
    }

    private void initViews(View view) {
        rgExperienceLevel = view.findViewById(R.id.rgExperienceLevel);
        rgFitnessGoal = view.findViewById(R.id.rgFitnessGoal);
        seekBarDays = view.findViewById(R.id.seekBarDays);
        tvDaysValue = view.findViewById(R.id.tvDaysValue);
    }

    private void setupSeekBar() {
        seekBarDays.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int days = progress + 1; // SeekBar va de 0-6, necesitamos 1-7
                tvDaysValue.setText(days + " día" + (days > 1 ? "s" : ""));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Establecer valor inicial
        int initialDays = seekBarDays.getProgress() + 1;
        tvDaysValue.setText(initialDays + " día" + (initialDays > 1 ? "s" : ""));
    }

    public boolean isValid() {
        return rgExperienceLevel.getCheckedRadioButtonId() != -1 &&
               rgFitnessGoal.getCheckedRadioButtonId() != -1;
    }

    public void fillUserData(User user) {
        if (!isValid()) return;

        // Establecer nivel de experiencia
        int experienceId = rgExperienceLevel.getCheckedRadioButtonId();
        if (experienceId == R.id.rbBeginner) {
            user.setExperienceLevel(User.ExperienceLevel.BEGINNER);
        } else if (experienceId == R.id.rbIntermediate) {
            user.setExperienceLevel(User.ExperienceLevel.INTERMEDIATE);
        } else if (experienceId == R.id.rbAdvanced) {
            user.setExperienceLevel(User.ExperienceLevel.ADVANCED);
        }

        // Establecer objetivo fitness
        int goalId = rgFitnessGoal.getCheckedRadioButtonId();
        if (goalId == R.id.rbLoseWeight) {
            user.setFitnessGoal(User.FitnessGoal.LOSE_WEIGHT);
        } else if (goalId == R.id.rbGainMuscle) {
            user.setFitnessGoal(User.FitnessGoal.GAIN_MUSCLE);
        } else if (goalId == R.id.rbMaintenance) {
            user.setFitnessGoal(User.FitnessGoal.MAINTENANCE);
        } else if (goalId == R.id.rbStrength) {
            user.setFitnessGoal(User.FitnessGoal.STRENGTH);
        }

        // Establecer días por semana
        user.setDaysPerWeek(seekBarDays.getProgress() + 1);
    }
}
