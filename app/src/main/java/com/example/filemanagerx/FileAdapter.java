package com.example.filemanagerx;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    Context context;
    File[] filesAndFolders;
    static File holdingFile;
    List<File> holdingFiles;
    String path;

    public FileAdapter(Context context, File[] filesAndFolders, String path) {
        this.context = context;
        this.filesAndFolders = filesAndFolders;
        this.path = path;
    }

    public void setFilesAndFolders(File[] filesAndFolders) {
        this.filesAndFolders = filesAndFolders;
    }

    SharedPreferences mSharedPreferences;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false);
        holdingFiles = new ArrayList<>();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        File selectedFile = filesAndFolders[position];
        holder.textView.setText(selectedFile.getName());


        if (selectedFile.isDirectory()) {
            holder.imageView.setImageResource(R.drawable.ic_baseline_folder_24);
        } else {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeFile(selectedFile.getPath(), options);
            if (options.outWidth != -1 && options.outHeight != -1) {
                holder.imageView.setImageBitmap(BitmapFactory.decodeFile(selectedFile.getAbsolutePath()));
            } else {
                holder.imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
            }
        }

        holder.itemView.setOnClickListener(view -> {
            if (selectedFile.isDirectory()) {
                Intent intent = new Intent(context, FileListActivity.class);
                String path = selectedFile.getAbsolutePath();
                intent.putExtra("path", path);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                //open the file
                try {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    String type = "image/*";
                    intent.setDataAndType(Uri.parse(selectedFile.getAbsolutePath()), type);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context.getApplicationContext(), "Cannot open the file", Toast.LENGTH_SHORT).show();
                }
            }
        });


        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
                builder.setTitle("Choose an Option");
                String[] options = {"Delete", "Move", "Copy", "Paste here", "Rename", "Detailed information"};
                View tempView = LayoutInflater.from(context).inflate(R.layout.alert_dialog_file_options, null);
                ListView lvAlertDialogFileOptions = tempView.findViewById(R.id.list_view);
                ArrayAdapter arrayAdapter = new ArrayAdapter(context, R.layout.alert_dialog_file_options_item, R.id.tv1, options);

                lvAlertDialogFileOptions.setAdapter(arrayAdapter);
                AlertDialog dialog;
                builder.setView(lvAlertDialogFileOptions);
                dialog = builder.create();
                arrayAdapter.getItem(3);        // Paste here item
                lvAlertDialogFileOptions.setOnItemClickListener((parent, view, position1, id) -> {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    switch (position1) {
                        case 0:         // Delete
                        {
                            boolean deleted = selectedFile.delete();
                            if (deleted) {
                                recreateFile();
                                notifyItemRemoved(holder.getAdapterPosition());
                                Toast.makeText(context.getApplicationContext(), "DELETED", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context.getApplicationContext(), "ERROR: File can't be deleted.", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }
                        case 1:         // Move
                        {
                            holdingFile = selectedFile;
                            Toast.makeText(context.getApplicationContext(), "MOVED" + holdingFile.getPath(), Toast.LENGTH_SHORT).show();
                            editor.putInt("COPY_FILE_FLAG", 1);
                            editor.putInt("COPY_FILE_POSITION", holder.getAdapterPosition());
                            editor.apply();
                            break;
                        }
                        case 2:         // Copy
                        {
                            Toast.makeText(context.getApplicationContext(), "COPIED", Toast.LENGTH_SHORT).show();
                            holdingFile = selectedFile;
                            editor.putInt("COPY_FILE_FLAG", 2);
                            editor.putInt("COPY_FILE_POSITION", holder.getAdapterPosition());
                            editor.apply();
                            break;
                        }
                        case 3:         // Paste here
                        {
                            if (holdingFile != null) {
                                File checkDuplicatedFile = new File(selectedFile.getAbsolutePath() + "/" + holdingFile.getName());
                                if (!checkDuplicatedFile.getAbsolutePath().equals(holdingFile.getAbsolutePath())) {
                                    AtomicReference<String> holdingFileRenamed = new AtomicReference<>(holdingFile.getName());
                                    boolean isFileExist = checkDuplicatedFile.exists();
                                    if (isFileExist) {
                                        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                                        builder1.setTitle((holdingFile.isDirectory() ? "Folder" : "File") + " existed")
                                                .setIcon(R.drawable.ic_baseline_info_24)
                                                .setMessage((holdingFile.isDirectory() ? "Folder" : "File") + " existed at selected directory. Tap OK to overwrite.")
                                                .setPositiveButton("ok", (dialog1, which1) -> {
                                                    new File(selectedFile.getAbsolutePath() + "/" + holdingFileRenamed).delete();
                                                    if (mSharedPreferences.getInt("COPY_FILE_FLAG", 0) == 2) {
                                                        copyFileToDirectory(holdingFile, selectedFile, null);
                                                        holdingFile = null;
                                                    } else if (mSharedPreferences.getInt("COPY_FILE_FLAG", 0) == 1) {
                                                        holdingFile.renameTo(new File(selectedFile.getAbsolutePath() + "/" + holdingFileRenamed));
                                                        recreateFile();
                                                        notifyItemRemoved(mSharedPreferences.getInt("COPY_FILE_POSITION", -1));
                                                    }
                                                })
                                                .setNeutralButton("skip", (dialog1, which1) -> {
                                                    StringBuilder stringBuilder = new StringBuilder(holdingFileRenamed.get());
                                                    File tempFile = new File(selectedFile.getAbsolutePath() + "/" + stringBuilder);
                                                    int duplicate = 0;
                                                    while (tempFile.exists()) {
                                                        duplicate++;
                                                        stringBuilder = new StringBuilder(stringBuilder.substring(0, holdingFileRenamed.toString().length()));
                                                        stringBuilder.append(" (").append(duplicate).append(")");
                                                        tempFile = new File(selectedFile.getAbsolutePath() + "/" + stringBuilder);
                                                    }
                                                    holdingFileRenamed.set(tempFile.getName());
                                                    if (mSharedPreferences.getInt("COPY_FILE_FLAG", 0) == 2) {
                                                        copyFileToDirectory(holdingFile, selectedFile, holdingFileRenamed.get());
                                                        holdingFile = null;
                                                    } else if (mSharedPreferences.getInt("COPY_FILE_FLAG", 0) == 1) {
                                                        holdingFile.renameTo(new File(selectedFile.getAbsolutePath() + "/" + holdingFileRenamed));
                                                        recreateFile();
                                                        notifyItemRemoved(mSharedPreferences.getInt("COPY_FILE_POSITION", -1));
                                                    }
                                                })
                                                .setNegativeButton("cancel", (dialog1, which1) -> {
                                                });
                                        AlertDialog fileInfoDialog = builder1.create();
                                        fileInfoDialog.show();
                                    }
                                } else {
                                    Toast.makeText(context.getApplicationContext(), (holdingFile.isDirectory() ? "Folder" : "File") + " is currently in this directory.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(context.getApplicationContext(), "No file selected!", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }
                        case 4:         // Rename
                        {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                            builder1.setTitle("Rename File");
                            EditText input = new EditText(context);
                            input.setText(selectedFile.getName());
                            input.setSelection(input.getText().length());
                            builder1.setView(input);
                            builder1.setPositiveButton("Ok",
                                    (dialog13, which) -> {
                                        String fileName = input.getText().toString();

                                        boolean rename = selectedFile.renameTo(new File(selectedFile.getParent(), fileName));

                                        if (rename) {
                                            recreateFile();
                                            notifyItemChanged(holder.getAdapterPosition());
                                            Toast.makeText(context.getApplicationContext(), "RENAME successfully", Toast.LENGTH_SHORT).show();
                                        }

                                    });
                            builder1.setNegativeButton("Cancel",
                                    (dialog12, which) -> dialog12.dismiss());
                            AlertDialog fileInfoDialog = builder1.create();
                            fileInfoDialog.show();
                            break;
                        }
                        case 5:         // Detail
                        {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                            builder1.setTitle((selectedFile.isDirectory() ? "Folder" : "File") + " information")
                                    .setIcon(R.drawable.ic_baseline_info_24)
                                    .setMessage((selectedFile.isDirectory() ? "Folder" : "File") + " name: " + selectedFile.getName()
                                            + "\nLocation: " + selectedFile.getPath()
                                            + "\nSize: " + getFileSizeString(selectedFile)
                                            + "\nLast modified: " + new Date(selectedFile.lastModified()))
                                    .setPositiveButton("close", (dialog1, which1) -> {
                                    });
                            AlertDialog fileInfoDialog = builder1.create();
                            fileInfoDialog.show();
                            break;
                        }
                        default:
                            throw new IllegalStateException("Unexpected value: " + position1);
                    }
                    dialog.cancel();
                });
                ((ViewGroup) lvAlertDialogFileOptions.getParent()).removeView(lvAlertDialogFileOptions);
                dialog.show();


                return true;
            }
        });
    }

    private void recreateFile() {
        File root = new File(path);
        File[] fileAndFolders = root.listFiles();
        filesAndFolders = fileAndFolders;
    }

    private boolean copyFileToDirectory(File sourceFile, File destinationFile, String alternateFileName) {
        String srcFileName = alternateFileName != null ? alternateFileName : sourceFile.getName();
        String srcFilePath = sourceFile.getAbsolutePath();
        String desFilePath = destinationFile.getAbsolutePath();
        if (destinationFile.isDirectory()) {
            if (sourceFile.isDirectory()) {
                return false;
            } else {
                try {
                    InputStream in = new FileInputStream(srcFilePath);
                    OutputStream out = new FileOutputStream(desFilePath + "/" + srcFileName);

                    byte[] buffer = new byte[(int) sourceFile.length()];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();

                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }

    private String getFileSizeString(File selectedFile) {
        long fileSize = selectedFile.length();
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1048576) {
            return String.format("%.1f KB", ((double) fileSize / 1024));
        } else if (fileSize < 1073741824) {
            return String.format("%.1f MB", ((double) fileSize / 1048576));
        } else {
            return String.format("%.1f GB", ((double) fileSize / 1073741824));
        }

    }

    @Override
    public int getItemCount() {
        return filesAndFolders.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;
        ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.file_name_text_view);
            imageView = itemView.findViewById(R.id.icon_view);
        }
    }
}
