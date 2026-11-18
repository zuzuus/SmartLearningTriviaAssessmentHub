package com.example.smartlearningtriviahub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

import com.example.smartlearningtriviahub.databinding.ActivityMainBinding;

import java.util.Locale;

// Registration screen. Checks the student details, then opens the quiz.
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Bundle keys used by all 3 activities
    public static final String KEY_NAME = "name";
    public static final String KEY_STUDENT_ID = "student_id";
    public static final String KEY_PROGRAM = "program";

    // Name: letters only, 3 to 50 characters
    private static final String NAME_PATTERN = "^[A-Za-z@'. -]{3,50}$";
    // Student ID: 3 letters + 8 digits, e.g. CBS24070636
    private static final String STUDENT_ID_PATTERN = "^[A-Z]{3}\\d{8}$";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        Log.d(TAG, "onCreate");

        handleSystemBars();

        binding.btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQuiz();
            }
        });
    }

    private void startQuiz() {
        // Stop here if any field is wrong.
        if (!validateInputs()) {
            Log.w(TAG, "Start stopped: invalid input");
            Toast.makeText(this, R.string.toast_fix_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String name = getText(binding.etName);
        String studentId = getText(binding.etStudentId).toUpperCase(Locale.ROOT);
        String program = getText(binding.etProgram).toUpperCase(Locale.ROOT);

        // Put the details in a Bundle and send them with an explicit Intent.
        Bundle bundle = new Bundle();
        bundle.putString(KEY_NAME, name);
        bundle.putString(KEY_STUDENT_ID, studentId);
        bundle.putString(KEY_PROGRAM, program);

        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtras(bundle);

        Log.i(TAG, "Opening quiz for " + name + " (" + studentId + ")");
        Toast.makeText(this, getString(R.string.toast_welcome, name), Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }

    // Checks every field. Wrong fields get a red border and a message.
    private boolean validateInputs() {
        boolean valid = true;

        String name = getText(binding.etName);
        String studentId = getText(binding.etStudentId).toUpperCase(Locale.ROOT);
        String program = getText(binding.etProgram);

        // Validate name.
        if (name.isEmpty()) {
            showError(binding.etName, binding.tvNameError, R.string.error_name_empty);
            valid = false;
        } else if (!name.matches(NAME_PATTERN)) {
            showError(binding.etName, binding.tvNameError, R.string.error_name_invalid);
            valid = false;
        } else {
            clearError(binding.etName, binding.tvNameError);
        }

        // Validate student ID.
        if (studentId.isEmpty()) {
            showError(binding.etStudentId, binding.tvIdError, R.string.error_id_empty);
            valid = false;
        } else if (!studentId.matches(STUDENT_ID_PATTERN)) {
            showError(binding.etStudentId, binding.tvIdError, R.string.error_id_invalid);
            valid = false;
        } else {
            clearError(binding.etStudentId, binding.tvIdError);
        }

        // Validate program / group.
        if (program.isEmpty()) {
            showError(binding.etProgram, binding.tvProgramError, R.string.error_program_empty);
            valid = false;
        } else {
            clearError(binding.etProgram, binding.tvProgramError);
        }

        return valid;
    }

    private String getText(EditText field) {
        return field.getText().toString().trim();
    }

    private void showError(EditText field, TextView errorText, int message) {
        field.setBackgroundResource(R.drawable.bg_input_error);
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void clearError(EditText field, TextView errorText) {
        field.setBackgroundResource(R.drawable.bg_input);
        errorText.setVisibility(View.GONE);
    }

    // Keeps the layout away from the status bar and navigation bar.
    private void handleSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(),
                new OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(View view,
                                                                  WindowInsetsCompat insets) {
                        Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                        view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                        return insets;
                    }
                });
    }

    // ----- Activity life cycle (open Logcat and filter "MainActivity") -----

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
