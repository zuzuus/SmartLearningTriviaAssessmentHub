package com.example.smartlearningtriviahub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartlearningtriviahub.databinding.ActivityQuizBinding;
import com.example.smartlearningtriviahub.model.Question;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * QUIZ ACTIVITY - Web-Connected Trivia Quiz Hub.
 * Downloads 10 questions with Volley, shows them through Data Binding,
 * checks answers, updates the feedback ImageView, shows Snackbar popups and tracks the score.
 */
public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";

    // Open Trivia DB: 10 multiple-choice questions, returned as a JSONObject with a "results" JSONArray
    private static final String API_URL = "https://opentdb.com/api.php?amount=10&type=multiple";

    // Keys for the Bundle sent to ResultActivity
    public static final String KEY_SCORE = "key_score";
    public static final String KEY_TOTAL = "key_total";
    public static final String KEY_QUESTIONS = "key_questions";
    public static final String KEY_USER_ANSWERS = "key_user_answers";
    public static final String KEY_CORRECT_ANSWERS = "key_correct_answers";

    // Keys for saving state on rotation
    private static final String STATE_QUESTIONS = "state_questions";
    private static final String STATE_INDEX = "state_index";
    private static final String STATE_SCORE = "state_score";

    // Feedback pictures + captions (picked at random)
    private static final int[] CORRECT_IMAGES = {R.drawable.img_correct_goated, R.drawable.img_correct_pet};
    private static final int[] CORRECT_CAPTIONS = {R.string.caption_goated, R.string.caption_pet};
    private static final int[] WRONG_IMAGES = {R.drawable.img_wrong_dumb, R.drawable.img_wrong_nose};
    private static final int[] WRONG_CAPTIONS = {R.string.caption_dumb, R.string.caption_nose};

    private ActivityQuizBinding binding;
    private RequestQueue requestQueue;
    private Button[] optionButtons;
    private final Random random = new Random();

    private ArrayList<Question> questionList = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;

    private String playerName;
    private String studentId;
    private String program;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quiz);
        applySystemBarPadding();
        Log.d(TAG, "onCreate: QuizActivity created, layout bound");

        // Receive the player Bundle from MainActivity
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Toast.makeText(this, R.string.toast_missing_player, Toast.LENGTH_LONG).show();
            Log.e(TAG, "onCreate: no player Bundle received, closing");
            finish();
            return;
        }
        playerName = extras.getString(MainActivity.KEY_NAME);
        studentId = extras.getString(MainActivity.KEY_STUDENT_ID);
        program = extras.getString(MainActivity.KEY_PROGRAM);
        Log.d(TAG, "onCreate: player = " + playerName + ", " + studentId + ", " + program);

        // Event listeners for the 4 choice buttons
        optionButtons = new Button[]{binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4};
        for (int i = 0; i < optionButtons.length; i++) {
            final int choiceIndex = i;
            optionButtons[i].setOnClickListener(v -> checkAnswer(choiceIndex));
        }
        binding.btnNext.setOnClickListener(v -> goToNextQuestion());
        binding.btnRetry.setOnClickListener(v -> fetchQuestions());

        requestQueue = Volley.newRequestQueue(this);

        // Restore after rotation, otherwise download a fresh set
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_QUESTIONS)) {
            questionList = (ArrayList<Question>) savedInstanceState.getSerializable(STATE_QUESTIONS);
            currentIndex = savedInstanceState.getInt(STATE_INDEX);
            score = savedInstanceState.getInt(STATE_SCORE);
            Log.d(TAG, "onCreate: restored " + questionList.size() + " questions at index " + currentIndex);
            showQuizContent();
            showQuestion();
        } else {
            fetchQuestions();
        }
    }

    // ---------------- Volley: download + parse ----------------

    private void fetchQuestions() {
        showLoading();
        Log.d(TAG, "fetchQuestions: GET " + API_URL);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                API_URL,
                null,
                response -> {
                    try {
                        int responseCode = response.getInt("response_code");
                        if (responseCode != 0) {
                            Log.w(TAG, "fetchQuestions: API returned response_code " + responseCode);
                            showError(getString(R.string.error_api_code, responseCode));
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        questionList.clear();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject item = results.getJSONObject(i);
                            String text = decode(item.getString("question"));
                            String category = decode(item.getString("category"));
                            String difficulty = item.getString("difficulty");
                            String correct = decode(item.getString("correct_answer"));
                            JSONArray wrong = item.getJSONArray("incorrect_answers");

                            List<String> options = new ArrayList<>();
                            options.add(correct);
                            for (int j = 0; j < wrong.length(); j++) {
                                options.add(decode(wrong.getString(j)));
                            }
                            Collections.shuffle(options);

                            questionList.add(new Question(text, category, difficulty, correct, options));
                        }

                        Log.d(TAG, "fetchQuestions: parsed " + questionList.size() + " questions");
                        currentIndex = 0;
                        score = 0;
                        showQuizContent();
                        showQuestion();
                    } catch (JSONException e) {
                        Log.e(TAG, "fetchQuestions: JSON parsing failed", e);
                        showError(getString(R.string.error_parse));
                    }
                },
                error -> {
                    Log.e(TAG, "fetchQuestions: Volley error " + error);
                    showError(getString(R.string.error_network));
                });

        request.setTag(TAG);
        request.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1f));
        requestQueue.add(request);
    }

    /** The API sends HTML entities like &quot; and &#039; - turn them into normal text. */
    private String decode(String raw) {
        return Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString();
    }

    // ---------------- Showing a question ----------------

    private void showQuestion() {
        Question question = questionList.get(currentIndex);

        // Data Binding: question text, category and the 4 options update automatically
        binding.setQuestion(question);
        binding.executePendingBindings();

        binding.tvQuestionCounter.setText(getString(R.string.question_counter, currentIndex + 1, questionList.size()));
        binding.tvScore.setText(getString(R.string.score_format, score));
        binding.pbQuiz.setMax(questionList.size());
        binding.pbQuiz.setProgress(currentIndex);

        boolean isLast = currentIndex == questionList.size() - 1;
        binding.btnNext.setText(isLast ? R.string.btn_finish : R.string.btn_next);

        if (question.isAnswered()) {
            // Coming back after rotation: redraw the answered state without a popup
            showAnswerFeedback(question, false);
        } else {
            resetForNewQuestion();
        }
        binding.scrollQuiz.smoothScrollTo(0, 0);
    }

    private void resetForNewQuestion() {
        for (Button button : optionButtons) {
            button.setEnabled(true);
            button.setBackgroundResource(R.drawable.bg_option_default);
            button.setTextColor(ContextCompat.getColor(this, R.color.bone_white));
        }
        binding.ivFeedback.setImageResource(R.drawable.img_wolf);
        binding.ivFeedback.setBackgroundResource(R.drawable.bg_frame_neutral);
        binding.bubble.setBackgroundResource(R.drawable.bg_bubble_neutral);
        setBubbleText(R.color.black, getString(R.string.verdict_ready),
                getString(R.string.caption_ready), getString(R.string.sub_ready));
        binding.btnNext.setEnabled(false);
    }

    // ---------------- Answer checking + feedback ----------------

    private void checkAnswer(int choiceIndex) {
        Question question = questionList.get(currentIndex);
        if (question.isAnswered()) {
            return;  // ignore double taps
        }

        String selected = question.getOptions().get(choiceIndex);
        question.setUserAnswer(selected);
        if (question.isCorrect()) {
            score++;
        }
        Log.d(TAG, "checkAnswer: Q" + (currentIndex + 1) + " picked '" + selected
                + "' correct=" + question.isCorrect() + " score=" + score);

        showAnswerFeedback(question, true);
    }

    private void showAnswerFeedback(Question question, boolean showPopup) {
        boolean correct = question.isCorrect();

        // Colour the choice buttons
        List<String> options = question.getOptions();
        for (int i = 0; i < optionButtons.length; i++) {
            Button button = optionButtons[i];
            String option = options.get(i);
            button.setEnabled(false);
            if (option.equals(question.getCorrectAnswer())) {
                button.setBackgroundResource(R.drawable.bg_option_correct);
                button.setTextColor(ContextCompat.getColor(this, R.color.black));
            } else if (option.equals(question.getUserAnswer())) {
                button.setBackgroundResource(R.drawable.bg_option_wrong);
                button.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                button.setBackgroundResource(R.drawable.bg_option_disabled);
                button.setTextColor(ContextCompat.getColor(this, R.color.muted_blue));
            }
        }

        // Update the feedback ImageView + bubble with a random reaction picture
        if (correct) {
            int pick = random.nextInt(CORRECT_IMAGES.length);
            binding.ivFeedback.setImageResource(CORRECT_IMAGES[pick]);
            binding.ivFeedback.setBackgroundResource(R.drawable.bg_frame_correct);
            binding.bubble.setBackgroundResource(R.drawable.bg_bubble_correct);
            setBubbleText(R.color.black, getString(R.string.verdict_correct),
                    getString(CORRECT_CAPTIONS[pick]), getString(R.string.sub_correct));
        } else {
            int pick = random.nextInt(WRONG_IMAGES.length);
            binding.ivFeedback.setImageResource(WRONG_IMAGES[pick]);
            binding.ivFeedback.setBackgroundResource(R.drawable.bg_frame_wrong);
            binding.bubble.setBackgroundResource(R.drawable.bg_bubble_wrong);
            setBubbleText(R.color.white, getString(R.string.verdict_wrong),
                    getString(WRONG_CAPTIONS[pick]),
                    getString(R.string.sub_wrong, question.getCorrectAnswer()));
        }

        binding.tvScore.setText(getString(R.string.score_format, score));
        binding.pbQuiz.setProgress(currentIndex + 1);
        binding.btnNext.setEnabled(true);

        if (showPopup) {
            showSnackbar(correct, question.getCorrectAnswer());
        }
    }

    private void setBubbleText(int colorRes, String verdict, String caption, String sub) {
        int color = ContextCompat.getColor(this, colorRes);
        binding.tvVerdict.setText(verdict);
        binding.tvCaption.setText(caption);
        binding.tvSub.setText(sub);
        binding.tvVerdict.setTextColor(color);
        binding.tvCaption.setTextColor(color);
        binding.tvSub.setTextColor(color);
    }

    private void showSnackbar(boolean correct, String correctAnswer) {
        String message = correct
                ? getString(R.string.snack_correct)
                : getString(R.string.snack_wrong, correctAnswer);
        Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.black));
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.bone_white));
        snackbar.setActionTextColor(ContextCompat.getColor(this,
                correct ? R.color.moon_yellow : R.color.pink_error));
        snackbar.setAction(R.string.snack_ok, v -> snackbar.dismiss());
        snackbar.show();
    }

    // ---------------- Navigation ----------------

    private void goToNextQuestion() {
        if (currentIndex < questionList.size() - 1) {
            currentIndex++;
            Log.d(TAG, "goToNextQuestion: moving to Q" + (currentIndex + 1));
            showQuestion();
        } else {
            openResult();
        }
    }

    private void openResult() {
        ArrayList<String> questions = new ArrayList<>();
        ArrayList<String> userAnswers = new ArrayList<>();
        ArrayList<String> correctAnswers = new ArrayList<>();
        for (Question q : questionList) {
            questions.add(q.getQuestionText());
            userAnswers.add(q.getUserAnswer());
            correctAnswers.add(q.getCorrectAnswer());
        }

        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.KEY_NAME, playerName);
        bundle.putString(MainActivity.KEY_STUDENT_ID, studentId);
        bundle.putString(MainActivity.KEY_PROGRAM, program);
        bundle.putInt(KEY_SCORE, score);
        bundle.putInt(KEY_TOTAL, questionList.size());
        bundle.putStringArrayList(KEY_QUESTIONS, questions);
        bundle.putStringArrayList(KEY_USER_ANSWERS, userAnswers);
        bundle.putStringArrayList(KEY_CORRECT_ANSWERS, correctAnswers);

        Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
        intent.putExtras(bundle);
        Log.d(TAG, "openResult: final score " + score + "/" + questionList.size() + ", opening ResultActivity");
        startActivity(intent);
        finish();
    }

    // ---------------- Loading / error UI ----------------

    private void showLoading() {
        binding.groupQuiz.setVisibility(View.GONE);
        binding.btnRetry.setVisibility(View.GONE);
        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.tvStatus.setVisibility(View.VISIBLE);
        binding.tvStatus.setText(R.string.loading_questions);
        binding.tvQuestionCounter.setText(getString(R.string.question_counter, 0, 10));
        binding.tvScore.setText(getString(R.string.score_format, 0));
        binding.pbQuiz.setProgress(0);
    }

    private void showQuizContent() {
        binding.pbLoading.setVisibility(View.GONE);
        binding.tvStatus.setVisibility(View.GONE);
        binding.btnRetry.setVisibility(View.GONE);
        binding.groupQuiz.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        binding.groupQuiz.setVisibility(View.GONE);
        binding.pbLoading.setVisibility(View.GONE);
        binding.tvStatus.setVisibility(View.VISIBLE);
        binding.tvStatus.setText(message);
        binding.btnRetry.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void applySystemBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    // ---------------- Save state for rotation ----------------

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!questionList.isEmpty()) {
            outState.putSerializable(STATE_QUESTIONS, questionList);
            outState.putInt(STATE_INDEX, currentIndex);
            outState.putInt(STATE_SCORE, score);
            Log.d(TAG, "onSaveInstanceState: saved index " + currentIndex + ", score " + score);
        }
    }

    // ---------------- Activity lifecycle (all 7 callbacks) ----------------

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: QuizActivity is becoming visible");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: QuizActivity is in the foreground and interactive");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: QuizActivity is partially hidden");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);   // stop any download still running
        }
        Log.d(TAG, "onStop: QuizActivity is no longer visible");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: QuizActivity is coming back after being stopped");
        if (questionList.isEmpty()) {
            fetchQuestions();   // the download was cancelled in onStop, try again
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: QuizActivity is being destroyed");
    }
}