package tutorials.cs371m.androidtictactoe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadImage extends AppCompatActivity {

    //Instance variables
    private TextView mDownloadingMessage;
    private TextView mWinnerMessage;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //These first two lines should have been auto-generated...if not, add them:
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_image);
        //Initialize TextViews and ImageView:
        mDownloadingMessage = (TextView) findViewById(R.id.message_downloading);
        mWinnerMessage = (TextView) findViewById(R.id.message_winner);
        mImageView = (ImageView) findViewById(R.id.image);
        //Set the TextView with the downloading image status to its initial value:
        mDownloadingMessage.setText(R.string.downloading_image);
        int winner = getIntent().getIntExtra("winner", 0);
        String message = getIntent().getStringExtra("message");

        String urlString = displayWinnerInfo(winner, message);
        downloadImage(urlString);

    }//end onCreate

    private String displayWinnerInfo(int winner, String message) {
        String urlString = "";
        if(winner == 1){
            urlString = getString(R.string.url_tie);
        }else if(winner == 2){
            urlString = getString(R.string.url_winner);
        }else if (winner == 3){
            urlString = getString(R.string.url_loser);
        } else {
            message = "Error!!!!";
        }
        mWinnerMessage.setText(message);
        return urlString;
    }

    private void downloadImage(String urlString) {
//check to see if device is connected to the internet before proceeding to download image:
        ConnectivityManager connMgr
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // fetch data:
            new DownloadImageTask().execute(urlString); // this will cause syntax error
        } else {
            // display error:
            mDownloadingMessage.setText(R.string.no_network_connection);
        }//end if else
    }

    //INNER CLASS FOR ASYNC TASK:
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        protected Bitmap doInBackground(String... params) {
            Bitmap returnImage = null;
            // params comes from the execute() call: params[0] is the urlString.
            URL url = null;
            try {
                url = new URL(params[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }//end try catch block

            HttpURLConnection urlConnection = null;
            if(url != null){
                try {
                    urlConnection = (HttpURLConnection) url.openConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }//end try catch block
            }//end if

            if(urlConnection != null){
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    returnImage = BitmapFactory.decodeStream(in);
                }catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    urlConnection.disconnect();
                }//end try catch finally block
            }//end if
            return returnImage;

        }//end doInBackground method
        protected void onPostExecute(Bitmap result) {
            if(result != null){
                mImageView.setImageBitmap(result);
                mDownloadingMessage.setText(R.string.download_complete);
            }//end if
        }//end onPostExecute method
    }//end DownloadImageTask private inner Async Task Class.
}//end DownloadImage Class.

