package com.reffum.notepadd;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.reffum.notepadd.databinding.ActivityMainBinding;

import android.os.ParcelFileDescriptor;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    /**
     * Current edited file
     */
    FileDescriptor fileDescriptor = null;

    /**
     * Activity launcher for open existing document
     */
    private final ActivityResultLauncher<String[]> openDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    String fileName = result.getPath();
                    try {
                        fileDescriptor = getContentResolver().openFileDescriptor(result, "rw").getFileDescriptor();

                        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileDescriptor));

                        StringBuilder stringBuiler = new StringBuilder();

                        String line;
                        while(true) {
                            line = bufferedReader.readLine();
                            if(line == null)
                                break;

                            stringBuiler.append(line).append(System.lineSeparator());
                        }

                        binding.editText.setText(stringBuiler.toString());
                        binding.toolbar.setTitle(fileName);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(
                                getApplicationContext(),
                                "File " + fileName + " not found",
                                Toast.LENGTH_SHORT).show();
                    }catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(
                                getApplicationContext(),
                                "Read file " + result.getPath() + " error.",
                                Toast.LENGTH_SHORT
                                ).show();
                    }
                }
            }
    );

    /**
     * Activity launcher for create new document
     */
    private final ActivityResultLauncher<String> createDocumentLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument(),
                    new ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri result) {
                            try {
                                fileDescriptor = getContentResolver()
                                        .openFileDescriptor(result, "rw")
                                        .getFileDescriptor();

                                // Write editText content in the file
                                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileDescriptor));
                                bufferedWriter.write(binding.editText.getText().toString());
                                bufferedWriter.flush();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                Toast.makeText(
                                        getApplicationContext(),
                                        "Can not open file " + result.getPath(),
                                        Toast.LENGTH_SHORT
                                        ).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(
                                        getApplicationContext(),
                                        "Write to file " + result.getPath() + " error",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_new) {

            // Save text to file. Close file and clear current descriptor. Clear text.
            saveTextToFile();
            binding.editText.getText().clear();
            fileDescriptor = null;

            return true;
        }
        else if(id == R.id.action_open) {
            openDocumentLauncher.launch(new String[]{"*/*"});
            return true;
        }
        else if(id == R.id.action_save) {
            if(fileDescriptor == null)
                createDocumentLauncher.launch("new_file.txt");
            else {
                saveTextToFile();
            }

            return true;
        }
        else if(id == R.id.action_save_as) {
            saveTextToFile();
            createDocumentLauncher.launch("new_file.txt");
        }
        else if(id == R.id.action_bug_report) {
            sendBugReportEmail();
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveTextToFile()
    {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileDescriptor));

            // Truncate file
            FileOutputStream fileOutputStream = new FileOutputStream(fileDescriptor);
            fileOutputStream.getChannel().truncate(0);

            // Write EditText content to file
            String notePadText = binding.editText.getText().toString();
            bufferedWriter.write(notePadText);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Write to file error", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendBugReportEmail()
    {
        String uriText = "mailto:reffum@bk.ru" +
                "?subject=" + Uri.encode("NotePadd bug report") +
                "&body" + Uri.encode("Application BUG description");

        Uri uri = Uri.parse(uriText);

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(uri);
        startActivity(Intent.createChooser(intent, "Send email"));
    }
}
