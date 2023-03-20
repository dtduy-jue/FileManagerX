package com.example.filemanagerx;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FileListActivity extends AppCompatActivity {

    String path;
    File root;
    File[] fileAndFolders;
    RecyclerView recyclerView;
    FileAdapter fileAdapter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_file_manager_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int menuItemId = item.getItemId();
        if (menuItemId == R.id.create_new_folder) {
            createNewFolder(root, 0);

        }
        if (menuItemId == R.id.paste) {

        }
        return super.onOptionsItemSelected(item);
    }

    private void createNewFolder(File root, int duplicate) {
        AtomicBoolean createStatus = new AtomicBoolean(false);
        StringBuilder stringBuilder = new StringBuilder("New folder");
        File tempFile = new File(root.getAbsolutePath() + "/" + stringBuilder);
        while (tempFile.exists() && tempFile.isDirectory()) {
            duplicate++;
            stringBuilder = new StringBuilder(stringBuilder.substring(0, 10));
            stringBuilder.append(" (").append(duplicate).append(")");
            tempFile = new File(root.getAbsolutePath() + "/" + stringBuilder);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Create new folder")
                .setMessage("Set folder's name");
        EditText input = new EditText(this);
        input.setText(tempFile.getName());
        input.setMaxLines(1);
        input.setLines(1);
        input.setSingleLine(true);
        input.setSelection(input.getText().length());
        builder.setView(input);
        builder.setPositiveButton("Ok",
                (dialog13, which) -> {
                    String fileName = input.getText().toString();
                    File duplicatedFile = new File(root.getAbsolutePath() + "/" + fileName);
                    boolean isFileExist = duplicatedFile.exists();
                    if (isFileExist) {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                        builder1.setTitle("Folder existed")
                                .setIcon(R.drawable.ic_baseline_info_24)
                                .setMessage("Folder existed at this directory. Tap OK to overwrite.\nCAUTION! Overwrite folder will delete all file contained in old folder.\nDo you want to overwrite anyway?")
                                .setPositiveButton("ok", (dialog1, which1) -> {
                                    deleteFolder(duplicatedFile);
                                    duplicatedFile.mkdirs();
                                    recreateFile();
                                })
                                .setNegativeButton("cancel", (dialog1, which1) -> {
                                });
                        AlertDialog fileInfoDialog = builder1.create();
                        fileInfoDialog.show();
                    } else {
                        duplicatedFile.mkdirs();
                        recreateFile();
                    }
                });
        builder.setNegativeButton("Cancel",
                (dialog12, which) -> dialog12.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        recyclerView = findViewById(R.id.recycler_view);
        TextView noFilesText = findViewById(R.id.nofiles_textview);

        path = getIntent().getStringExtra("path");
        root = new File(path);
        fileAndFolders = root.listFiles();

        if (fileAndFolders == null || fileAndFolders.length == 0) {
            noFilesText.setVisibility(View.VISIBLE);
            return;
        }

        noFilesText.setVisibility(View.INVISIBLE);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new FileAdapter(this, fileAndFolders, path);
        recyclerView.setAdapter(fileAdapter);
    }

    private void recreateFile() {
        root = new File(path);
        fileAndFolders = root.listFiles();
        fileAdapter.setFilesAndFolders(fileAndFolders);
        fileAdapter.notifyDataSetChanged();
        Log.d("FileManagerX", String.valueOf(fileAndFolders.length));
    }

    public static boolean deleteFolder(File removableFolder) {
        File[] files = removableFolder.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                boolean success;
                if (file.isDirectory())
                    success = deleteFolder(file);
                else success = file.delete();
                if (!success) return false;
            }
        }
        return removableFolder.delete();
    }
}

