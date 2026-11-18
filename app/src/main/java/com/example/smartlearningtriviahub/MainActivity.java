package com.example.smartlearningtriviahub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

import com.example.smartlearningtriviahub.databinding.ActivityMainBinding;

import java.util.Locale;

/**
 * MAIN ACTIVITY - User Registration & Entrance.
 * Collects and validates student details, logs all 7 lifecycle callbacks,
 * then starts QuizActivity with an explicit Intent + Bundle.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Bundle keys shared by all three activities
    public static final String KEY_NAME = "key_name";
    public static final String KEY_STUDENT_ID = "key_student_id";
    public static final String KEY_PROGRAM = "key_program";

    // Validation patterns
    private static final String NAME_PATTERN = "^[A-Za-z@'. -]{3,50}$";
    private static final String STUDENT_ID_PATTERN = "^[A-Z]{3}\\d{8}$";   // e.g. CBS24070636

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Data Binding replaces setContentView + findViewById
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        applySystemBarPadding();
        Log.d(TAG, "onCreate: MainActivity created, layout bound");

        binding.btnStart.setOnClickListener(v -> onStartClicked());
    }

    private void onStartClicked() {
        Log.d(TAG, "onStartClicked: START GAME pressed, validating input");
        if (!validateInputs()) {
            Toast.makeText(this, R.string.toast_fix_fields, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "onStartClicked: validation failed");
            return;
        }

        String name = binding.etName.getText().toString().trim();
        String studentId = binding.etStudentId.getText().toString().trim().toUpperCase(Locale.ROOT);
        String program = binding.etProgram.getText().toString().trim().toUpperCase(Locale.ROOT);

        // Explicit Intent + Bundle payload
        Bundle bundle = new Bundle();
        bundle.putString(KEY_NAME, name);
        bundle.putString(KEY_STUDENT_ID, studentId);
        bundle.putString(KEY_PROGRAM, program);

        Intent intent = new Intent(MainActivity.this, QuizActivity.class);
        intent.putExtras(bundle);

        Toast.makeText(this, getString(R.string.toast_welcome, name), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onStartClicked: navigating to QuizActivity with " + name + " / " + studentId);
        startActivity(intent);
    }

    /** Checks every field, shows red borders + messages, returns true only if all are valid. */
    private boolean validateInputs() {
        boolean valid = true;

        String name = binding.etName.getText().toString().trim();
        String studentId = binding.etStudentId.getText().toString().trim().toUpperCase(Locale.ROOT);
        String program = binding.etProgram.getText().toString().trim();

        // NAME
        if (name.isEmpty()) {
            showError(binding.etName, binding.tvNameError, R.string.error_name_empty);
            valid = false;
        } else if (!name.matches(NAME_PATTERN)) {
            showError(binding.etName, binding.tvNameError, R.string.error_name_invalid);
            valid = false;
        } else {
            clearError(binding.etName, binding.tvNameError);
        }

        // STUDENT ID
        if (studentId.isEmpty()) {
            showError(binding.etStudentId, binding.tvIdError, R.string.error_id_empty);
            valid = false;
        } else if (!studentId.matches(STUDENT_ID_PATTERN)) {
            showError(binding.etStudentId, binding.tvIdError, R.string.error_id_invalid);
            valid = false;
        } else {
            clearError(binding.etStudentId, binding.tvIdError);
        }

        // PROGRAM / GROUP
        if (program.isEmpty()) {
            showError(binding.etProgram, binding.tvProgramError, R.string.error_program_empty);
            valid = false;
        } else {
            clearError(binding.etProgram, binding.tvProgramError);
        }

        return valid;
    }

    private void showError(EditText field, TextView errorView, int messageRes) {
        field.setBackgroundResource(R.drawable.bg_input_error);
        errorView.setText(messageRes);
        errorView.setVisibility(TextView.VISIBLE);
    }

    private void clearError(EditText field, TextView errorView) {
        field.setBackgroundResource(R.drawable.bg_input);
        errorView.setVisibility(TextView.GONE);
    }

    /** Keeps content out from under the status / navigation bars (edge-to-edge on Android 15+). */
    private void applySystemBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    // ---------------- Activity lifecycle (all 7 callbacks) ----------------

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: MainActivity is becoming visible");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: MainActivity is in the foreground and interactive");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: MainActivity is partially hidden");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: MainActivity is no longer visible");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: MainActivity is coming back after being stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: MainActivity is being destroyed");
    }
}