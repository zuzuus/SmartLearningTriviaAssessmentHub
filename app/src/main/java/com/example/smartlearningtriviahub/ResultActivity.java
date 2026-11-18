package com.example.smartlearningtriviahub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

import com.example.smartlearningtriviahub.databinding.ActivityResultBinding;
import com.example.smartlearningtriviahub.databinding.ItemBreakdownBinding;

import java.util.ArrayList;

/**
 * RESULT ACTIVITY - Performance Analytics & Breakdown.
 * Calculates the percentage, picks a pack rank, lists every question in a scrollable
 * battle log, and lets the player restart the assessment.
 */
public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    private static final int ALPHA_MIN_PERCENT = 80;
    private static final int BETA_MIN_PERCENT = 50;

    private ActivityResultBinding binding;

    private String playerName;
    private String studentId;
    private String program;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_result);
        applySystemBarPadding();
        Log.d(TAG, "onCreate: ResultActivity created, layout bound");

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Toast.makeText(this, R.string.toast_missing_player, Toast.LENGTH_LONG).show();
            Log.e(TAG, "onCreate: no result Bundle received, closing");
            finish();
            return;
        }

        playerName = extras.getString(MainActivity.KEY_NAME);
        studentId = extras.getString(MainActivity.KEY_STUDENT_ID);
        program = extras.getString(MainActivity.KEY_PROGRAM);
        int score = extras.getInt(QuizActivity.KEY_SCORE);
        int total = extras.getInt(QuizActivity.KEY_TOTAL);
        ArrayList<String> questions = extras.getStringArrayList(QuizActivity.KEY_QUESTIONS);
        ArrayList<String> userAnswers = extras.getStringArrayList(QuizActivity.KEY_USER_ANSWERS);
        ArrayList<String> correctAnswers = extras.getStringArrayList(QuizActivity.KEY_CORRECT_ANSWERS);

        binding.tvPlayer.setText(getString(R.string.player_format, playerName, studentId, program));

        showSummary(score, total);
        buildBreakdown(questions, userAnswers, correctAnswers);

        binding.btnPlayAgain.setOnClickListener(v -> restartQuiz());
        binding.btnNewPlayer.setOnClickListener(v -> backToRegistration());
    }

    // ---------------- Calculation ----------------

    /** Percentage = score / total x 100, rounded to the nearest whole number. */
    static int calculatePercentage(int score, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(score * 100f / total);
    }

    /** Smallest extra correct answers needed to reach ALPHA. */
    static int answersNeededForAlpha(int score, int total) {
        int needed = (int) Math.ceil(ALPHA_MIN_PERCENT * total / 100.0) - score;
        return Math.max(needed, 0);
    }

    private void showSummary(int score, int total) {
        int percent = calculatePercentage(score, total);
        int wrong = total - score;
        Log.d(TAG, "showSummary: score " + score + "/" + total + " = " + percent + "%");

        binding.tvPercent.setText(getString(R.string.percent_format, percent));
        binding.pbScore.setProgress(percent);
        binding.tvCorrectCount.setText(String.valueOf(score));
        binding.tvWrongCount.setText(String.valueOf(wrong));
        binding.tvTotalCount.setText(String.valueOf(total));

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

    // ---------------- Itemised breakdown ----------------

    private void buildBreakdown(ArrayList<String> questions, ArrayList<String> userAnswers,
                                ArrayList<String> correctAnswers) {
        if (questions == null || userAnswers == null || correctAnswers == null) {
            Log.w(TAG, "buildBreakdown: missing answer lists");
            return;
        }
        binding.llBreakdown.removeAllViews();
        for (int i = 0; i < questions.size(); i++) {
            String user = userAnswers.get(i) == null ? "-" : userAnswers.get(i);
            String correct = correctAnswers.get(i);
            boolean isCorrect = correct.equals(user);

            // Each row uses Data Binding too (item_breakdown.xml -> ItemBreakdownBinding)
            ItemBreakdownBinding row = ItemBreakdownBinding.inflate(getLayoutInflater(), binding.llBreakdown, false);
            row.setNumber(String.valueOf(i + 1));
            row.setQuestionText(questions.get(i));
            row.setYourAnswer(user);
            row.setCorrectAnswer(correct);
            row.setCorrect(isCorrect);
            row.executePendingBindings();
            binding.llBreakdown.addView(row.getRoot());
        }
        Log.d(TAG, "buildBreakdown: added " + questions.size() + " rows");
    }

    // ---------------- Navigation ----------------

    /** PLAY AGAIN: same player, fresh 10 questions. */
    private void restartQuiz() {
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.KEY_NAME, playerName);
        bundle.putString(MainActivity.KEY_STUDENT_ID, studentId);
        bundle.putString(MainActivity.KEY_PROGRAM, program);

        Intent intent = new Intent(ResultActivity.this, QuizActivity.class);
        intent.putExtras(bundle);
        Log.d(TAG, "restartQuiz: restarting assessment for " + playerName);
        startActivity(intent);
        finish();
    }

    /** NEW PLAYER: back to the registration screen. */
    private void backToRegistration() {
        Intent intent = new Intent(ResultActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Log.d(TAG, "backToRegistration: returning to MainActivity");
        startActivity(intent);
        finish();
    }

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
        Log.d(TAG, "onStart: ResultActivity is becoming visible");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ResultActivity is in the foreground and interactive");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ResultActivity is partially hidden");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ResultActivity is no longer visible");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: ResultActivity is coming back after being stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ResultActivity is being destroyed");
    }
}
