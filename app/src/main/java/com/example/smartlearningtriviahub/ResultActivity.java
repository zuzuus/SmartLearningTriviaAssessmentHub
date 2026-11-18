package com.example.smartlearningtriviahub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

import com.example.smartlearningtriviahub.databinding.ActivityResultBinding;
import com.example.smartlearningtriviahub.databinding.ItemBreakdownBinding;

import java.util.ArrayList;

// Result screen. Calculates the percentage and rank, shows every answer
// in a scrollable list, and lets the user play again.
public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    private static final int ALPHA_MIN_PERCENT = 80;
    private static final int BETA_MIN_PERCENT = 50;

    private ActivityResultBinding binding;

    private String name;
    private String studentId;
    private String program;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_result);
        Log.d(TAG, "onCreate");

        handleSystemBars();

        // Receive the score and answers from QuizActivity.
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            Log.e(TAG, "No result received");
            Toast.makeText(this, R.string.toast_missing_player, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        name = bundle.getString(MainActivity.KEY_NAME);
        studentId = bundle.getString(MainActivity.KEY_STUDENT_ID);
        program = bundle.getString(MainActivity.KEY_PROGRAM);
        int score = bundle.getInt(QuizActivity.KEY_SCORE);
        int total = bundle.getInt(QuizActivity.KEY_TOTAL);
        ArrayList<String> questions = bundle.getStringArrayList(QuizActivity.KEY_QUESTIONS);
        ArrayList<String> userAnswers = bundle.getStringArrayList(QuizActivity.KEY_USER_ANSWERS);
        ArrayList<String> correctAnswers = bundle.getStringArrayList(QuizActivity.KEY_CORRECT_ANSWERS);

        binding.tvPlayer.setText(getString(R.string.player_format, name, studentId, program));

        showSummary(score, total);
        showBreakdown(questions, userAnswers, correctAnswers);

        // Play again with the same student.
        binding.btnPlayAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAgain();
            }
        });

        // Go back to the registration screen.
        binding.btnNewPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newPlayer();
            }
        });
    }

    // Percentage = score / total x 100 (rounded)
    static int calculatePercentage(int score, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(score * 100f / total);
    }

    // How many more correct answers are needed to reach ALPHA (80%)
    static int answersNeededForAlpha(int score, int total) {
        int needed = (int) Math.ceil(ALPHA_MIN_PERCENT * total / 100.0) - score;
        return Math.max(needed, 0);
    }

    private void showSummary(int score, int total) {
        int percent = calculatePercentage(score, total);
        int wrong = total - score;
        Log.i(TAG, "Score " + score + "/" + total + " = " + percent + "%");

        binding.tvPercent.setText(getString(R.string.percent_format, percent));
        binding.pbScore.setProgress(percent);
        binding.tvCorrectCount.setText(String.valueOf(score));
        binding.tvWrongCount.setText(String.valueOf(wrong));
        binding.tvTotalCount.setText(String.valueOf(total));

        // Pick the rank: ALPHA 80%+, BETA 50-79%, OMEGA below 50%.
        if (percent >= ALPHA_MIN_PERCENT) {
            binding.tvRank.setText(R.string.rank_alpha);
            binding.tvRank.setTextColor(ContextCompat.getColor(this, R.color.moon_yellow));
            binding.ivRank.setImageResource(R.drawable.img_rank_alpha);
            binding.tvTag.setText(R.string.tag_alpha);
            binding.tvRankCaption.setText(R.string.caption_alpha);
        } else if (percent >= BETA_MIN_PERCENT) {
            binding.tvRank.setText(R.string.rank_beta);
            binding.tvRank.setTextColor(ContextCompat.getColor(this, R.color.sky_blue));
            binding.ivRank.setImageResource(R.drawable.img_rank_beta);
            binding.tvTag.setText(getString(R.string.tag_beta, answersNeededForAlpha(score, total)));
            binding.tvRankCaption.setText(R.string.caption_beta);
        } else {
            binding.tvRank.setText(R.string.rank_omega);
            binding.tvRank.setTextColor(ContextCompat.getColor(this, R.color.silver));
            binding.ivRank.setImageResource(R.drawable.img_rank_omega);
            binding.tvTag.setText(R.string.tag_omega);
            binding.tvRankCaption.setText(R.string.caption_omega);
        }
    }

    // Adds one row per question to the battle log.
    private void showBreakdown(ArrayList<String> questions,
                               ArrayList<String> userAnswers,
                               ArrayList<String> correctAnswers) {
        if (questions == null || userAnswers == null || correctAnswers == null) {
            Log.w(TAG, "Answer lists are missing");
            return;
        }

        binding.llBreakdown.removeAllViews();

        for (int i = 0; i < questions.size(); i++) {
            String user = userAnswers.get(i);
            if (user == null) {
                user = "-";
            }
            String correct = correctAnswers.get(i);

            // Each row also uses Data Binding (item_breakdown.xml)
            ItemBreakdownBinding row = ItemBreakdownBinding.inflate(
                    getLayoutInflater(), binding.llBreakdown, false);
            row.setNumber(String.valueOf(i + 1));
            row.setQuestionText(questions.get(i));
            row.setYourAnswer(user);
            row.setCorrectAnswer(correct);
            row.setCorrect(correct.equals(user));
            row.executePendingBindings();

            binding.llBreakdown.addView(row.getRoot());
        }
    }

    // Same student, 10 new questions.
    private void playAgain() {
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.KEY_NAME, name);
        bundle.putString(MainActivity.KEY_STUDENT_ID, studentId);
        bundle.putString(MainActivity.KEY_PROGRAM, program);

        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtras(bundle);

        Log.i(TAG, "Play again: " + name);
        startActivity(intent);
        finish();
    }

    // Back to MainActivity and clear the screens on top of it.
    private void newPlayer() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Log.i(TAG, "New player");
        startActivity(intent);
        finish();
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

    // ----- Activity life cycle (open Logcat and filter "ResultActivity") -----

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
