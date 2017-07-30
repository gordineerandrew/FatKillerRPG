package tutorials.cs371m.androidtictactoe;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class AndroidTicTacToe extends AppCompatActivity {

    private static final String TAG = "Tic Tac Toe Activity";

    // Represents the internal state of the game
    private TicTacToeGame mGame;

    private boolean mHumanGoesFirst;

    private boolean mHumansTurnToMove;

    // is the game over or not?
    private boolean mGameOver;

    // Buttons making up the board
    private BoardView mBoardView;

    // Various text display
    private TextView mInfoTextView;

    // tracks how many time each outcome occurs (human wins,
    // tie, android wins
    private WinData mWinData;

    // displays for the number of each outcome
    private TextView[] mOutcomeCounterTextViews;

    // for all the sounds we play
    private SoundPool mSounds;
    private int mHumanMoveSoundID;
    private int mComputerMoveSoundID;
    private int mHumanWinSoundID;
    private int mComputerWinSoundID;
    private int mTieGameSoundID;

    // for pausing
    private Handler mPauseHandler;
    private Runnable mRunnable;

    // for settings
    private int SETTINGS_REQUEST = 1;
    private boolean mSoundOn;
    private boolean mShowResultImage;
    private String mHumanWinString;
    private String infoString;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_tic_tac_toe);
        mInfoTextView = (TextView) findViewById(R.id.information);
        mBoardView = (BoardView) findViewById(R.id.boardView);
        setInstanceVarsFromSharedPrefs();
        mBoardView.setGame(mGame);
        mBoardView.setOnTouchListener(mTouchListener);
        mPauseHandler = new Handler();
        initOutcomeTextViews();
        restoreScores();
        // on resume will start computer turn if necessary
    }

    private void initOutcomeTextViews() {
        mOutcomeCounterTextViews = new TextView[3];
        mOutcomeCounterTextViews[0] = (TextView) findViewById(R.id.human_wins_tv);
        mOutcomeCounterTextViews[1] = (TextView) findViewById(R.id.ties_tv);
        mOutcomeCounterTextViews[2] = (TextView) findViewById(R.id.android_wins_tv);
    }

    // Set up the game board.
    private void startNewGame() {
        mGameOver = false;
        mGame.clearBoard();
        mBoardView.invalidate();

        if (mHumanGoesFirst) {
            // Human goes first
            mHumansTurnToMove = true;
            mInfoTextView.setText(R.string.human_first);
        } else {
            mHumansTurnToMove = false;
            // Android goes first
            startComputerDelay();
        }
    }

    // makes the computer move
    private void computerMove() {
        Log.d(TAG, "In computerMove");
        int move = mGame.getComputerMove();
        setMove(TicTacToeGame.COMPUTER_PLAYER, move, mComputerMoveSoundID);
        int winner = mGame.checkForWinner();
        if (winner == 0) {
            mHumansTurnToMove = true;
            mInfoTextView.setText(R.string.human_turn);
        } else {
            handleEndGame(winner);
        }
    }

    private void handleEndGame(int winner) {
        // Log.d(TAG, mGame.toString());
        WinData.Outcome outcome;
        if (winner == 1) {
            outcome = WinData.Outcome.TIE;
            endGameActions(R.string.result_tie, mTieGameSoundID);
            mInfoTextView.setText(R.string.result_tie);
        } else if (winner == 2) {
            outcome = WinData.Outcome.HUMAN;
            endGameActions(R.string.result_human_wins, mHumanWinSoundID);
        } else {
            outcome = WinData.Outcome.ANDROID;
            endGameActions(R.string.result_computer_wins, mComputerWinSoundID);
        }
        mWinData.incrementWin(outcome);
        int index = outcome.ordinal();
        String display = "" + mWinData.getCount(outcome);
        mOutcomeCounterTextViews[index].setText(display);
        mGameOver = true;
        mHumanGoesFirst = !mHumanGoesFirst;
        // added in tutorial 6
        if (mShowResultImage) { // recall from Settings activity
            prepDownloadImageActivity(winner, infoString);
        }

    }

    private void endGameActions(int messageId, int soundId) {
        if (messageId == R.string.result_human_wins){
            mInfoTextView.setText(mHumanWinString);
        }else{
            mInfoTextView.setText(messageId);
        }
        if ( mSoundOn ){
            mSounds.play(soundId, 1, 1, 1, 0, 1);
        }
    }

    // Code below this point was added in tutorial 3.

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentManager fm = getFragmentManager();
        switch (item.getItemId()) {
            case R.id.new_game:
                // if computer is in middle of pause, stop it
                mPauseHandler.removeCallbacks(mRunnable);
                startNewGame();
                return true;
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, SETTINGS_REQUEST);
                return true;
            case R.id.reset_scores:
                ResetScoresDialogFragment resetScoresDialogFragment = new ResetScoresDialogFragment();
                resetScoresDialogFragment.show(fm, "reset");
                return true;
            case R.id.about:
                startActivity(new Intent(this, StepCounterActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Set the difficulty. Presumably called by DifficultyDialogFragment;
     *
     * @param difficulty The new difficulty for the game.
     */
    public void setDifficulty(int difficulty) {
        // check bounds;
        if (difficulty < 0 || difficulty >= TicTacToeGame.DifficultyLevel.values().length) {
            Log.d(TAG, "Unexpected difficulty: " + difficulty + "." +
                    " Setting difficulty to Easy / 0.");
            difficulty = 0; // if out of bounds set to 0
        }
        TicTacToeGame.DifficultyLevel newDifficulty
                = TicTacToeGame.DifficultyLevel.values()[difficulty];

        mGame.setDifficultyLevel(newDifficulty);
        String message = "Difficulty set to " +
                newDifficulty.toString().toLowerCase() + " .";

        // Display the selected difficulty level
        Toast.makeText(getApplicationContext(), message,
                Toast.LENGTH_SHORT).show();

    }

    // Code below here was added / updated in tutorial 4.

    // Set move in game logic and tell board view to redraw itself.
    private void setMove(char player, int location, int soundID) {
        // Log.d(TAG, "in setMove. player is: " + player + ", location is: " + location);
        // Log.d(TAG, "in setMove. old occupant in cell is " + mGame.getBoardOccupant(location));
        mGame.setMove(player, location);
        // Log.d(TAG, "in setMove. newBoard is "+ mGame);
        mBoardView.invalidate();
        if ( mSoundOn ){
            mSounds.play(soundID, 1, 1, 1, 0, 1);
        }
    }

    // Listen for touches on the board. Only apply move if game not over.
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (!mGameOver && mHumansTurnToMove) {
                // Determine which cell was touched
                int col = (int) event.getX() / mBoardView.getBoardCellWidth();
                int row = (int) event.getY() / mBoardView.getBoardCellHeight();
                int pos = row * 3 + col;
                // is that an open spot?
                if (mGame.getBoardOccupant(pos) == TicTacToeGame.OPEN_SPOT) {
                    mHumansTurnToMove = false;
                    // make the human move
                    setMove(TicTacToeGame.HUMAN_PLAYER, pos, mHumanMoveSoundID);
                    int winner = mGame.checkForWinner();
                    if (winner == 0) {
                        startComputerDelay();
                    } else {
                        handleEndGame(winner);
                    }
                }
            }

            // So we aren't notified of continued events when finger is moved
            return false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Log.d(TAG, "in onResume");
        mSounds = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        // 2 = maximum sounds to play at the same time,
        // AudioManager.STREAM_MUSIC is the stream type typically used for games
        // 0 is the "the sample-rate converter quality. Currently has no effect. Use 0 for the default."

        mHumanMoveSoundID = mSounds.load(this, R.raw.human_move, 1);
        // Context, id of resource, priority (currently no effect)
        mComputerMoveSoundID = mSounds.load(this, R.raw.computer_move, 1);
        mHumanWinSoundID = mSounds.load(this, R.raw.human_win, 1);
        mComputerWinSoundID = mSounds.load(this, R.raw.computer_win, 1);
        mTieGameSoundID = mSounds.load(this, R.raw.tie_game, 1);

        // if the game is not over and it is the computer's turn,
        // start the delay again
        if (!mGameOver && !mHumansTurnToMove) {
            startComputerDelay();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        // Log.d(TAG, "in onPause");
        if (mSounds != null) {
            mSounds.release();
            mSounds = null;
        }
        // since we are pausing, we want to stop the computer delay,
        // but restart it when we resume
        mPauseHandler.removeCallbacks(mRunnable);
    }

    private void startComputerDelay() {
        // Log.d(TAG, "Starting computer delay");
        mInfoTextView.setText(R.string.computer_turn);
        mRunnable = createRunnable();
        mPauseHandler.postDelayed(mRunnable, 750); // Pause for three quarters of a second
    }

    private Runnable createRunnable() {
        return new Runnable() {
            public void run() {
                // Done thinking, time to move.
                Log.d(TAG, "delay over making move.");
                computerMove();
            }//end run override method
        };//end of new Runnable()
    }//end of createRunnable method

    // Code below added /updated in tutorial 5

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putCharArray("board", mGame.getBoardState());
        outState.putCharSequence("info", mInfoTextView.getText());
        outState.putBoolean("mGameOver", mGameOver);
        outState.putBoolean("mHumansTurnToMove", mHumansTurnToMove);
        outState.putBoolean("mHumanGoesFirst", mHumanGoesFirst);
        outState.putInt("difficulty", mGame.getDifficultyLevel().ordinal());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        mGame.setBoardState(savedInstanceState.getCharArray("board"));
        int difficultyInt = savedInstanceState.getInt("difficulty");
        TicTacToeGame.DifficultyLevel difficulty;
        difficulty = TicTacToeGame.DifficultyLevel.values()[difficultyInt];
        mGame.setDifficultyLevel(difficulty);
        mGameOver = savedInstanceState.getBoolean("mGameOver");
        mHumansTurnToMove = savedInstanceState.getBoolean("mHumansTurnToMove");
        mHumanGoesFirst = savedInstanceState.getBoolean("mHumanGoesFirst");
        mInfoTextView.setText(savedInstanceState.getCharSequence("info"));

        restoreScores();
    }

    private void displayScores() {
        WinData.Outcome[] outcomes = WinData.Outcome.values();
        for (WinData.Outcome outcome : outcomes) {
            String counter = String.valueOf(mWinData.getCount(outcome));
            mOutcomeCounterTextViews[outcome.ordinal()].setText(counter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        for (WinData.Outcome outcome : WinData.Outcome.values()) {
            editor.putInt(outcome.name(), mWinData.getCount(outcome));
        }
        saveGameState(editor);
        editor.apply();
    }

    private void saveGameState(SharedPreferences.Editor editor) {
        // save all relevant information about current game
        // so we can reconstruct in onCreate
        editor.putInt("difficulty", mGame.getDifficultyLevel().ordinal());
        editor.putString("board_state", mGame.toStringSimple());
        editor.putBoolean("mGameOver", mGameOver);
        editor.putBoolean("mHumansTurnToMove", mHumansTurnToMove);
        editor.putBoolean("mHumanGoesFirst", mHumanGoesFirst);
        editor.putString("info", mInfoTextView.getText().toString());
    }


    private void restoreScores() {
        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        // restore the number of outcomes
        WinData.Outcome[] outcomes = WinData.Outcome.values();
        int[] counters = new int[outcomes.length];
        for (int i = 0; i < counters.length; i++) {
            counters[i] = sharedPref.getInt(outcomes[i].name(), 0);
            // parameters to SharedPreferences.getInt() are
            // (key, value if not present)
        }
        mWinData = new WinData(counters);
        displayScores();
    }

    // reset the scores for wins and ties
    public void resetScores() {
        mWinData = new WinData();
        displayScores();
    }


    private void setInstanceVarsFromSharedPrefs() {
        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        int difficulty = sharedPref.getInt("difficulty", 0);
        TicTacToeGame.DifficultyLevel diff =
                TicTacToeGame.DifficultyLevel.values()[difficulty];
        String boardState = sharedPref.getString("board_state", "*");
        if (boardState.length() == 1) {
            mGame = new TicTacToeGame();
            mGame.setDifficultyLevel(diff);
        } else {
            mGame = new TicTacToeGame(boardState, diff);
        }
        mGameOver = sharedPref.getBoolean("mGameOver", false);
        mHumanGoesFirst = sharedPref.getBoolean("mHumanGoesFirst", true);
        mHumansTurnToMove = sharedPref.getBoolean("mHumansTurnToMove", true);
        mSoundOn = sharedPref.getBoolean("sound", true);
        mShowResultImage = sharedPref.getBoolean("result_image", true);
        mInfoTextView.setText(sharedPref.getString("info",
                getString(R.string.human_first)));
        mHumanWinString = sharedPref.getString("victory_message", "You Won!");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTINGS_REQUEST) {
            SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
            // Apply potentially new settings
            mSoundOn = sharedPref.getBoolean("sound", true);
            mHumanWinString = sharedPref.getString ("victory_message", "You Won!");
            String[] levels = getResources().getStringArray(R.array.difficulty_levels);
            // set difficulty, or use hardest if not present,
            String difficultyLevel
                    = sharedPref.getString("difficulty_level", levels[levels.length - 1]);
            int i = 0;
            while(i < levels.length) {
                if(difficultyLevel.equals(levels[i])) {
                    mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[i]);
                    i = levels.length; // to stop loop
                }
                i++;
            }
            // Do we show images?
            mShowResultImage = sharedPref.getBoolean("result_image", true);
        }
    }

    private void prepDownloadImageActivity(int winner, String message){
        Intent intent = new Intent(this, DownloadImage.class);
        intent.putExtra("winner", winner);
        intent.putExtra("message", message);
        startActivity(intent);
    }//end displayImage method

}
