package com.datasyn.transcreator;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static com.datasyn.transcreator.R.id.btnConvert;
import static com.datasyn.transcreator.R.id.fab1;
import static com.datasyn.transcreator.R.id.fab2;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,View.OnClickListener {

    private FloatingActionButton fab, fabgal, fabcam;
    private Boolean isFabOpen = false;
    private Animation fab_open, fab_close, rotate_forward, rotate_backward;

    private static final String LOG_TAG = "Text API";
    private static final int PHOTO_REQUEST = 10;
    private static final int REQUEST_GALLERY = 0;

    private TextView scanResults;
    final Context context = this;
    GoogleTranslate translator;
    File mediaFile;
    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "EatLab");
    private Uri imageUri;
    private TextRecognizer detector;
    private static final String TAG = MainActivity.class.getSimpleName();
    private ProgressDialog progress = null;
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_RESULT = "result";
    String strScanResults, fname;
    ImageButton button_share;
    Button button_done,button_cnvrt, button_clear,button_save;;
    EditText editOutput;
    NavigationView navigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideItem();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabgal = (FloatingActionButton) findViewById(fab1);
        fabcam = (FloatingActionButton) findViewById(fab2);
        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_backward);
        fab.setOnClickListener(this);
        //fabgal.setOnClickListener(this);
        //fabcam.setOnClickListener(this);
        editOutput=(EditText)findViewById(R.id.editOutput);
        editOutput.setVisibility(GONE);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        drawer.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this);


        fabgal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, REQUEST_GALLERY);


            }
        });
        fabcam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ActivityCompat.requestPermissions(MainActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);


            }
        });


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //Add buttons
        button_share = (ImageButton) findViewById(R.id.button_share);
        button_save = (Button) findViewById(R.id.button_save);
        button_cnvrt = (Button) findViewById(R.id.buttoncnvrt);
        button_clear = (Button) findViewById(R.id.buttonclr);
        button_done = (Button)findViewById(R.id.doneEiting);
        button_done.setVisibility(INVISIBLE );
        scanResults = (TextView) findViewById(R.id.results);
        scanResults.setMovementMethod(new ScrollingMovementMethod());



        //Share Button

        button_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                mediaFile = new File(mediaStorageDir.getPath() + File.separator + fname + ".doc");
                if (mediaStorageDir.exists() && mediaFile.exists()) {


                    intentShareFile.setType("application/doc");
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + mediaFile));

                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                            fname);
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");

                    startActivity(Intent.createChooser(intentShareFile, "Share File"));
                } else {
                    Toast toast = Toast.makeText(MainActivity.this, "Failed!! no file to share,Please save your data in file first!!", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }


            }
        });


        //Convert English To Thai Language

        button_cnvrt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                checkInternetConenction();

               if (!scanResults.getText().toString().isEmpty()) {
                    new EnglishToTagalog().execute();
                } else {
                    Toast toast = Toast.makeText(MainActivity.this, "No Text to Convert!!", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                }


            }
        });

        //Clear Text of Textview

        button_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (scanResults.getText().toString().isEmpty()) {

                    Toast toast = Toast.makeText(MainActivity.this, "No Text to clear!!", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    //Yes button clicked
                                    scanResults.setText("");

                                    Toast toast = Toast.makeText(MainActivity.this, "Text cleared!!", Toast.LENGTH_SHORT);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked

                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Are you sure to clear data?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();

                }


            }
        });


        //Save Doc in storage
        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                strScanResults = scanResults.getText().toString();

                if (!strScanResults.isEmpty()) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(context);
                    alert.setTitle("File Name"); //Set Alert dialog title here
                    alert.setMessage("Enter Your File Name Here"); //Message here

                    // Set an EditText view to get user input
                    final EditText input = new EditText(context);
                    alert.setView(input);

                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //You will get as string input data in this variable.
                            // here we convert the input to a string and show in a toast.
                            fname = input.getEditableText().toString();


                            //Toast.makeText(context,srt, Toast.LENGTH_LONG).show();


                            // Create the storage directory if it does not exist
                            if (!mediaStorageDir.exists()) {
                                if (!mediaStorageDir.mkdirs()) {
                                    Log.d(MainActivity.LOG_TAG, "Oops! Failed create "
                                            + "EatLab" + " directory");
                                    Toast toast = Toast.makeText(MainActivity.this, "Folder is already exist! please move it to different place and try again",
                                            Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                }
                            }

                            // Create file if it does not exist
                            if (!strScanResults.isEmpty()) {


                                mediaFile = new File(mediaStorageDir.getPath() + File.separator + fname + ".doc");

                                if (mediaFile.exists()) {


                                    Toast toast = Toast.makeText(MainActivity.this, "File name " + fname + " is already exist! please choose different file name.",
                                            Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();

                                } else {

                                    Toast toast = Toast.makeText(MainActivity.this, "Your Text File is Created at Location : " + mediaFile,
                                            Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                }
                                //Write Data To File

                                try {
                                    FileWriter writer = new FileWriter(mediaFile);
                                    BufferedWriter br = new BufferedWriter(writer);
                                    br.write("\t\t\t Powerd By-EAT LAB \n");
                                    br.write("\t\t-------------------------------\n\n\n\n");
                                    br.append(scanResults.getText().toString());


                                    br.flush();
                                    br.close();
                                    writer.close();


                                } catch (IOException e) {
                                    e.printStackTrace();

                                }


                            }


                        }
                        // End of onClick(DialogInterface dialog, int whichButton)
                    }); //End of alert.setPositiveButton
                    alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                            dialog.cancel();
                        }
                    }); //End of alert.setNegativeButton
                    AlertDialog alertDialog = alert.create();
                    alertDialog.show();
       /* Alert Dialog Code End*/
                } else {
                    Toast toast = Toast.makeText(MainActivity.this, "Can't save document! No data found!!", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }

            }
        });


        if (savedInstanceState != null) {
            imageUri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI));
            scanResults.setText(savedInstanceState.getString(SAVED_INSTANCE_RESULT));
        }
        detector = new TextRecognizer.Builder(getApplicationContext()).build();
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK) {
            launchMediaScanIntent();
            try {
                Bitmap bitmap = decodeBitmapUri(this, imageUri);
                if (detector.isOperational() && bitmap != null) {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> textBlocks = detector.detect(frame);
                    String blocks = "";
                    String lines = "";
                    String words = "";
                    for (int index = 0; index < textBlocks.size(); index++) {
                        //extract scanned text blocks here
                        TextBlock tBlock = textBlocks.valueAt(index);
                        blocks = blocks + tBlock.getValue() + "\n" + "\n";
                        for (Text line : tBlock.getComponents()) {
                            //extract scanned text lines here
                            lines = lines + line.getValue() + "\n";
                            for (Text element : line.getComponents()) {
                                //extract scanned text words here
                                words = words + element.getValue() + ", ";
                            }
                        }
                    }
                    if (textBlocks.size() == 0) {

                        Toast toast = Toast.makeText(MainActivity.this, "Scanning Faild,Please Try Again!!", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    } else {

                        scanResults.setText(scanResults.getText() + blocks + "\n");
                    }
                } else {
                    scanResults.setText(getString(R.string.detector_error));
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
                        .show();
                Log.e(LOG_TAG, e.toString());
            }
        } else if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK) {
            inspect(data.getData());

        }

    }


    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "picture.jpg");
        //  imageUri = Uri.fromFile(photo);
        imageUri = FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getApplicationContext().getPackageName() + ".provider", photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, PHOTO_REQUEST);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (imageUri != null) {
            outState.putString(SAVED_INSTANCE_URI, imageUri.toString());
            outState.putString(SAVED_INSTANCE_RESULT, scanResults.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }

    //Translation Class

    private class EnglishToTagalog extends AsyncTask<Void, Void, Void> {


      /*  protected void onError(Exception ex) {

        }*/

        @Override
        protected Void doInBackground(Void... params) {

            try {
                translator = new GoogleTranslate(getString(R.string.apiKey));

                Thread.sleep(1000);


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPreExecute() {
            //start the progress dialog

            progress = ProgressDialog.show(MainActivity.this, null, "Translating...");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void result) {

            super.onPostExecute(result);
            translated();
            progress.dismiss();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

    }


    public void translated() {

        String translatetotagalog = scanResults.getText().toString(); //get the value of text

        String text;
        text = translator.translte(translatetotagalog, "en", "th");

        scanResults.setText(text);


    }


    private boolean checkInternetConenction() {
        // get Connectivity Manager object to check connection
        ConnectivityManager connec
                = (ConnectivityManager) getSystemService(getBaseContext().CONNECTIVITY_SERVICE);

        // Check for network connections
        if (connec.getNetworkInfo(0).getState() ==
                android.net.NetworkInfo.State.CONNECTED ||
                connec.getNetworkInfo(0).getState() ==
                        android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() ==
                        android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED) {
            //  Toast.makeText(this, "You Connected ", Toast.LENGTH_LONG).show();
            return true;
        } else if (
                connec.getNetworkInfo(0).getState() ==
                        android.net.NetworkInfo.State.DISCONNECTED ||
                        connec.getNetworkInfo(1).getState() ==
                                android.net.NetworkInfo.State.DISCONNECTED) {
            Toast toast = Toast.makeText(this, "No internet access", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return false;
        }
        return false;
    }


    private void inspectFromBitmap(Bitmap bitmap) {
        detector = new TextRecognizer.Builder(MainActivity.this).build();
        try {
            if (!detector.isOperational()) {
                new android.app.AlertDialog.
                        Builder(this).
                        setMessage("Text recognizer could not be set up on your device").show();
                return;
            }

            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> origTextBlocks = detector.detect(frame);
            List<TextBlock> textBlocks = new ArrayList<>();
            for (int i = 0; i < origTextBlocks.size(); i++) {
                TextBlock textBlock = origTextBlocks.valueAt(i);
                textBlocks.add(textBlock);
            }
            Collections.sort(textBlocks, new Comparator<TextBlock>() {
                @Override
                public int compare(TextBlock o1, TextBlock o2) {
                    int diffOfTops = o1.getBoundingBox().top - o2.getBoundingBox().top;
                    int diffOfLefts = o1.getBoundingBox().left - o2.getBoundingBox().left;
                    if (diffOfTops != 0) {
                        return diffOfTops;
                    }
                    return diffOfLefts;
                }
            });

            StringBuilder detectedText = new StringBuilder();
            for (TextBlock textBlock : textBlocks) {
                if (textBlock != null && textBlock.getValue() != null) {
                    detectedText.append(textBlock.getValue());
                    detectedText.append("\n");
                }
            }
            if (textBlocks.size() == 0) {

                Toast toast = Toast.makeText(MainActivity.this, "Scanning Faild,Please Try Again!!", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else {
                scanResults.append(detectedText);
            }
        } finally {
            detector.release();
        }
    }

    private void inspect(Uri uri) {
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = 2;
            options.inScreenDensity = DisplayMetrics.DENSITY_LOW;
            bitmap = BitmapFactory.decodeStream(is, null, options);
            inspectFromBitmap(bitmap);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed to find the file: " + uri, e);
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close InputStream", e);
                }
            }
        }
    }


    public void animateFAB() {

        if (isFabOpen) {

            fab.startAnimation(rotate_backward);
            fabcam.startAnimation(fab_close);
            fabgal.startAnimation(fab_close);
            fabcam.setClickable(false);
            fabgal.setClickable(false);
            isFabOpen = false;


        } else {

            fab.startAnimation(rotate_forward);
            fabcam.startAnimation(fab_open);
            fabgal.startAnimation(fab_open);
            fabcam.setClickable(true);
            fabgal.setClickable(true);
            isFabOpen = true;


        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.fab:

                animateFAB();
                break;
            case fab1:

                break;
            case fab2:

                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent intent = new Intent(MainActivity.this, AppInfoActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
            ActivityCompat.requestPermissions(MainActivity.this, new
                    String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);

        } else if (id == R.id.nav_gallery) {

            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, REQUEST_GALLERY);


        }else if (id == R.id.nav_edit) {

            showItem();
            scanResults.setVisibility(GONE);
            editOutput.setVisibility(View.VISIBLE);

            button_cnvrt.setVisibility(View.INVISIBLE);
            button_clear.setVisibility(View.INVISIBLE);
            button_save.setVisibility(View.INVISIBLE);
            fab.setVisibility(View.INVISIBLE);
            button_share.setVisibility(View.INVISIBLE);
            button_done.setVisibility(View.VISIBLE);

            editOutput.setText(scanResults.getText().toString());

            button_done.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideItem();
                    scanResults.setVisibility(View.VISIBLE);
                    editOutput.setVisibility(View.INVISIBLE);

                    button_cnvrt.setVisibility(View.VISIBLE);
                    button_clear.setVisibility(View.VISIBLE);
                    button_save.setVisibility(View.VISIBLE);
                    fab.setVisibility(View.VISIBLE);
                    button_share.setVisibility(View.VISIBLE);
                    button_done.setVisibility(INVISIBLE);
                    scanResults.setText(editOutput.getText().toString());

                }
            });
        }
        else if (id == R.id.nav_exitEditMode) {
            scanResults.setVisibility(View.VISIBLE);
            editOutput.setVisibility(View.INVISIBLE);

            button_cnvrt.setVisibility(View.VISIBLE);
            button_clear.setVisibility(View.VISIBLE);
            button_save.setVisibility(View.VISIBLE);
            fab.setVisibility(View.VISIBLE);
            button_share.setVisibility(View.VISIBLE);
            button_done.setVisibility(INVISIBLE);

            scanResults.setText(editOutput.getText().toString());
            hideItem();
        }
        else if (id == R.id.nav_manage) {
            Intent intent = new Intent(MainActivity.this, UserTranslationActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_share) {
            try {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, "TransCreator");
                String sAux = "\nTranscreator! Let me recommend you this application\n";
                sAux = sAux + "https://play.google.com/store/apps/details?id=Transcreator \n\n";
                i.putExtra(Intent.EXTRA_TEXT, sAux);
                startActivity(Intent.createChooser(i, "choose one"));
            } catch (Exception e) {
                //e.toString();
            }
        } else if (id == R.id.nav_send) {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setType("text/email");
            email.setData(Uri.parse("mailto:" + getString(R.string.email_admin)));
            email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.title_feedback));
            email.putExtra(Intent.EXTRA_TEXT, "\n \n" + getResources().getString(R.string.desc_app_version) + BuildConfig.VERSION_NAME +
                    "\n" + getResources().getString(R.string.desc_device_info) + Build.BRAND.toUpperCase() + " " + Build.MODEL + ", OS : " + Build.VERSION.RELEASE);
            email.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(email, getString(R.string.intent_desc_link)));
            return true;

}

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private void hideItem()
    {
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu nav_Menu = navigationView.getMenu();
        nav_Menu.findItem(R.id.nav_exitEditMode).setVisible(false);
    }
    private void showItem()
    {
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu nav_Menu = navigationView.getMenu();
        nav_Menu.findItem(R.id.nav_exitEditMode).setVisible(true);
    }
}
