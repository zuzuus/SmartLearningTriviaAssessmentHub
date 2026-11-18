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
import androidx.core.view.OnApplyWindowInsetsListener;
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
import java.util.Random;

// Quiz screen. Downloads 10 questions with Volley, shows them with
// Data Binding, checks each answer and keeps the score.
public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";

    // Open Trivia DB: 10 multiple-choice questions
    private static final String API_URL =
            "https://opentdb.com/api.php?amount=10&type=multiple";

    // Bundle keys sent to ResultActivity
    public static final String KEY_SCORE = "score";
    public static final String KEY_TOTAL = "total";
    public static final String KEY_QUESTIONS = "questions";
    public static final String KEY_USER_ANSWERS = "user_answers";
    public static final String KEY_CORRECT_ANSWERS = "correct_answers";

    // Keys for saving the quiz when the screen rotates
    private static final String STATE_QUESTIONS = "state_questions";
    private static final String STATE_INDEX = "state_index";
    private static final String STATE_SCORE = "state_score";

    // Reaction pictures and captions (one is picked at random)
    private static final int[] CORRECT_IMAGES = {
            R.drawable.img_correct_goated, R.drawable.img_correct_pet};
    private static final int[] CORRECT_CAPTIONS = {
            R.string.caption_goated, R.string.caption_pet};
    private static final int[] WRONG_IMAGES = {
            R.drawable.img_wrong_dumb, R.drawable.img_wrong_nose};
    private static final int[] WRONG_CAPTIONS = {
            R.string.caption_dumb, R.string.caption_nose};

    private ActivityQuizBinding binding;
    private RequestQueue queue;
    private Button[] optionButtons;
    private final Random random = new Random();

    private ArrayList<Question> questionList = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;

    private String name;
    private String studentId;
    private String program;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quiz);
        Log.d(TAG, "onCreate");

        handleSystemBars();

        // Receive the student details from MainActivity.
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            Log.e(TAG, "No player details received");
            Toast.makeText(this, R.string.toast_missing_player, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        name = bundle.getString(MainActivity.KEY_NAME);
        studentId = bundle.getString(MainActivity.KEY_STUDENT_ID);
        program = bundle.getString(MainActivity.KEY_PROGRAM);

        // One click listener for each answer button.
        optionButtons = new Button[]{
                binding.btnOption1, binding.btnOption2,
                binding.btnOption3, binding.btnOption4};
        for (int i = 0; i < optionButtons.length; i++) {
            final int choice = i;
            optionButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkAnswer(choice);
                }
            });
        }

        binding.btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToNextQuestion();
            }
        });

        binding.btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadQuestionsFromWeb();
            }
        });

        queue = Volley.newRequestQueue(this);

        // After rotation: restore the quiz. Otherwise: download new questions.
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_QUESTIONS)) {
            questionList = (ArrayList<Question>) savedInstanceState.getSerializable(STATE_QUESTIONS);
            currentIndex = savedInstanceState.getInt(STATE_INDEX);
            score = savedInstanceState.getInt(STATE_SCORE);
            Log.d(TAG, "Restored quiz at question " + (currentIndex + 1));
            showQuizContent();
            showQuestion();
        } else {
            loadQuestionsFromWeb();
        }
    }

    // Request -> Response -> Parse -> Display
    private void loadQuestionsFromWeb() {
        showLoading();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, API_URL, null,
                response -> {
                    Log.i(TAG, "Download OK");
                    parseQuestions(response);
                },
                error -> {
                    Log.e(TAG, "Download failed: " + error.toString());
                    showError(getString(R.string.error_network));
                });

        request.setTag(TAG);
        request.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1f));
        queue.add(request);
    }

    // JSONObject -> "results" JSONArray -> JSONObject -> getString()
    private void parseQuestions(JSONObject response) {
        try {
            // response_code 0 means OK
            int responseCode = response.getInt("response_code");
            if (responseCode != 0) {
                Log.w(TAG, "API response_code = " + responseCode);
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

                // Put the correct answer with the 3 wrong ones, then shuffle.
                ArrayList<String> options = new ArrayList<>();
                options.add(correct);
                for (int j = 0; j < wrong.length(); j++) {
                    options.add(decode(wrong.getString(j)));
                }
                Collections.shuffle(options);

                questionList.add(new Question(text, category, difficulty, correct, options));
            }

            Log.i(TAG, questionList.size() + " questions received");
            currentIndex = 0;
            score = 0;
            showQuizContent();
            showQuestion();
        } catch (JSONException e) {
            Log.e(TAG, "JSON parse error: " + e.getMessage());
            showError(getString(R.string.error_parse));
        }
    }

    // The API sends symbols as codes like &quot; - change them back to normal text.
    private String decode(String text) {
        return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();
    }

    // Shows the current question and resets the buttons.
    private void showQuestion() {
        Question question = questionList.get(currentIndex);

        // Data Binding fills in the category, question and 4 answers.
        binding.setQuestion(question);
        binding.executePendingBindings();

        binding.tvQuestionCounter.setText(
                getString(R.string.question_counter, currentIndex + 1, questionList.size()));
        binding.tvScore.setText(getString(R.string.score_format, score));
        binding.pbQuiz.setMax(questionList.size());
        binding.pbQuiz.setProgress(currentIndex);

        // Last question shows FINISH instead of NEXT.
        if (currentIndex == questionList.size() - 1) {
            binding.btnNext.setText(R.string.btn_finish);
        } else {
            binding.btnNext.setText(R.string.btn_next);
        }

        if (question.isAnswered()) {
            // Screen was rotated after answering: show the answer again, no popup.
            showAnswerFeedback(question, false);
        } else {
            resetButtons();
        }
        binding.scrollQuiz.smoothScrollTo(0, 0);
    }

    private void resetButtons() {
        for (Button button : optionButtons) {
            button.setEnabled(true);
            button.setBackgroundResource(R.drawable.bg_option_default);
            button.setTextColor(ContextCompat.getColor(this, R.color.bone_white));
        }

        binding.ivFeedback.setImageResource(R.drawable.img_wolf);
        binding.ivFeedback.setBackgroundResource(R.drawable.bg_frame_neutral);
        binding.bubble.setBackgroundResource(R.drawable.bg_bubble_neutral);
        setBubbleText(R.color.black,
                getString(R.string.verdict_ready),
                getString(R.string.caption_ready),
                getString(R.string.sub_ready));

        binding.btnNext.setEnabled(false);
    }

    private void checkAnswer(int choice) {
        Question question = questionList.get(currentIndex);

        // Ignore a second tap on the same question.
        if (question.isAnswered()) {
            return;
        }

        String selected = question.getOptions().get(choice);
        question.setUserAnswer(selected);

        if (question.isCorrect()) {
            score++;
        }
        Log.d(TAG, "Q" + (currentIndex + 1) + ": picked " + selected
                + ", correct = " + question.isCorrect() + ", score = " + score);

        showAnswerFeedback(question, true);
    }

    // Colours the buttons, changes the picture and shows a Snackbar.
    private void showAnswerFeedback(Question question, boolean showPopup) {
        boolean correct = question.isCorrect();

        // Green = correct answer, red = wrong pick, grey = the rest.
        for (int i = 0; i < optionButtons.length; i++) {
            Button button = optionButtons[i];
            String option = question.getOptions().get(i);
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

        // Change the ImageView to a random happy or sad wolf.
        if (correct) {
            int pick = random.nextInt(CORRECT_IMAGES.length);
            binding.ivFeedback.setImageResource(CORRECT_IMAGES[pick]);
            binding.ivFeedback.setBackgroundResource(R.drawable.bg_frame_correct);
            binding.bubble.setBackgroundResource(R.drawable.bg_bubble_correct);
            setBubbleText(R.color.black,
                    getString(R.string.verdict_correct),
                    getString(CORRECT_CAPTIONS[pick]),
                    getString(R.string.sub_correct));
        } else {
            int pick = random.nextInt(WRONG_IMAGES.length);
            binding.ivFeedback.setImageResource(WRONG_IMAGES[pick]);
            binding.ivFeedback.setBackgroundResource(R.drawable.bg_frame_wrong);
            binding.bubble.setBackgroundResource(R.drawable.bg_bubble_wrong);
            setBubbleText(R.color.white,
                    getString(R.string.verdict_wrong),
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

    private void setBubbleText(int colour, String verdict, String caption, String sub) {
        int textColour = ContextCompat.getColor(this, colour);
        binding.tvVerdict.setText(verdict);
        binding.tvCaption.setText(caption);
        binding.tvSub.setText(sub);
        binding.tvVerdict.setTextColor(textColour);
        binding.tvCaption.setTextColor(textColour);
        binding.tvSub.setTextColor(textColour);
    }

    private void showSnackbar(boolean correct, String correctAnswer) {
        String message;
        if (correct) {
            message = getString(R.string.snack_correct);
        } else {
            message = getString(R.string.snack_wrong, correctAnswer);
        }

        final Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.black));
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.bone_white));
        snackbar.setActionTextColor(ContextCompat.getColor(this,
                correct ? R.color.moon_yellow : R.color.pink_error));
        snackbar.setAction(R.string.snack_ok, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    private void goToNextQuestion() {
        if (currentIndex < questionList.size() - 1) {
            currentIndex++;
            showQuestion();
        } else {
            openResult();
        }
    }

    // Sends the score and every answer to ResultActivity.
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
        bundle.putString(MainActivity.KEY_NAME, name);
        bundle.putString(MainActivity.KEY_STUDENT_ID, studentId);
        bundle.putString(MainActivity.KEY_PROGRAM, program);
        bundle.putInt(KEY_SCORE, score);
        bundle.putInt(KEY_TOTAL, questionList.size());
        bundle.putStringArrayList(KEY_QUESTIONS, questions);
        bundle.putStringArrayList(KEY_USER_ANSWERS, userAnswers);
        bundle.putStringArrayList(KEY_CORRECT_ANSWERS, correctAnswers);

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtras(bundle);

        Log.i(TAG, "Quiz finished: " + score + "/" + questionList.size());
        startActivity(intent);
        finish();
    }

    // ----- Loading / quiz / error views -----

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

    // Save the quiz before the screen rotates.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!questionList.isEmpty()) {
            outState.putSerializable(STATE_QUESTIONS, questionList);
            outState.putInt(STATE_INDEX, currentIndex);
            outState.putInt(STATE_SCORE, score);
        }
    }

    // ----- Activity life cycle (open Logcat and filter "QuizActivity") -----

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
        // Stop the download if the user leaves the screen.
        if (queue != null) {
            queue.cancelAll(TAG);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
        // The download was stopped in onStop, so try again.
        if (questionList.isEmpty()) {
            loadQuestionsFromWeb();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
