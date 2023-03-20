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
import java.util.concurrent.atomic.AtomicReference;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{
    private static final long TWO_GB = 2147483648L;
    Context context;
    File[] filesAndFolders;
    static File holdingFile;
    List<File> holdingFiles;
    public MyAdapter(Context context, File[] filesAndFolders){
        this.context = context;
        this.filesAndFolders = filesAndFolders;
    }

    SharedPreferences mSharedPreferences;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.recycler_item,parent,false);
        holdingFiles = new ArrayList<>();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        File selectedFile = filesAndFolders[position];
        holder.textView.setText(selectedFile.getName());


        if(selectedFile.isDirectory()){
            holder.imageView.setImageResource(R.drawable.ic_baseline_folder_24);
        }else {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeFile(selectedFile.getPath(), options);
            if (options.outWidth != -1 && options.outHeight != -1) {
                holder.imageView.setImageBitmap(BitmapFactory.decodeFile(selectedFile.getAbsolutePath()));
            } else {
                holder.imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedFile.isDirectory()){
                    Intent intent = new Intent(context, FileListActivity.class);
                    String path = selectedFile.getAbsolutePath();
                    intent.putExtra("path", path);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }else {
                    //open the file
                    try{
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        String type = "image/*";
                        intent.setDataAndType(Uri.parse(selectedFile.getAbsolutePath()), type);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }catch (Exception e){
                        Toast.makeText(context.getApplicationContext(),"Cannot open the file", Toast.LENGTH_SHORT).show();
                    }
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
                            notifyItemRemoved(holder.getAdapterPosition());
                            if(deleted){
                                Toast.makeText(context.getApplicationContext(),"DELETED",Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }
                        case 1:         // Move
                        {
                            holdingFile = selectedFile;
                            Toast.makeText(context.getApplicationContext(),"MOVED" + holdingFile.getPath(),Toast.LENGTH_SHORT).show();
                            editor.putInt("COPY_FILE_FLAG", 1);
                            editor.apply();
                            break;
                        }
                        case 2:         // Copy
                        {
                            Toast.makeText(context.getApplicationContext(),"COPIED",Toast.LENGTH_SHORT).show();
                            holdingFile = selectedFile;
                            editor.putInt("COPY_FILE_FLAG", 2);
                            editor.apply();
                            break;
                        }
                        case 3:         // Paste here
                        {
                            if (holdingFile != null) {
                                AtomicReference<String> holdingFileRenamed = new AtomicReference<>(holdingFile.getName());
                                Toast.makeText(context.getApplicationContext(),mSharedPreferences.getInt("COPY_FILE_FLAG", 0) + "Pasting file" + selectedFile.getAbsolutePath() + "/" + holdingFile.getName(),Toast.LENGTH_SHORT).show();
                                File checkDuplicatedFile = new File(selectedFile.getAbsolutePath() + "/" + holdingFile.getName());
                                boolean isFileExist = checkDuplicatedFile.exists();
                                if (isFileExist) {
                                    AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                                    builder1.setTitle((holdingFile.isDirectory() ? "Folder" : "File") + " existed")
                                            .setIcon(R.drawable.ic_baseline_info_24)
                                            .setMessage((holdingFile.isDirectory() ? "Folder" : "File") + " existed at selected directory. Tap OK to overwrite.")
                                            .setPositiveButton("ok", (dialog1, which1) -> {
                                                new File(selectedFile.getAbsolutePath() + "/" + holdingFileRenamed).delete();
                                            })
                                            .setNeutralButton("skip", (dialog1, which1) -> {
                                                holdingFileRenamed.set(holdingFileRenamed + " - Copy");
                                            })
                                            .setNegativeButton("cancel", (dialog1, which1) -> {
                                            });
                                    AlertDialog fileInfoDialog = builder1.create();
                                    fileInfoDialog.show();
                                }
                                if (mSharedPreferences.getInt("COPY_FILE_FLAG", 0) == 2) {
                                    copyFileToDirectory(holdingFile, selectedFile, isFileExist);
                                    holdingFile = null;
                                } else if (mSharedPreferences.getInt("COPY_FILE_FLAG", 0) == 1) {
                                    holdingFile.renameTo(new File(selectedFile.getAbsolutePath() + "/" + holdingFileRenamed));
                                }
                            } else {
                                Toast.makeText(context.getApplicationContext(),"No file selected!",Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }
                        case 4:         // Rename
                        {
                            Toast.makeText(context.getApplicationContext(),"RENAME",Toast.LENGTH_SHORT).show();
                            break;
                        }
                        case 5:         // Detail
                        {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                            builder1.setTitle((selectedFile.isDirectory() ? "Folder" : "File") + " detail")
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

    private boolean copyFileToDirectory(File sourceFile, File destinationFile, boolean isFileExists) {
        String srcFileName = isFileExists ? sourceFile.getName() + " - Copy" : sourceFile.getName();
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
            return String.format("%.1f KB",  ((double)fileSize / 1024));
        } else if (fileSize < 1073741824) {
            return String.format("%.1f MB",  ((double)fileSize / 1048576));
        } else {
            return String.format("%.1f GB",  ((double)fileSize / 1073741824));
        }

    }

    @Override
    public int getItemCount() {
        return filesAndFolders.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView textView;
        ImageView imageView;

        public ViewHolder(View itemView){
            super(itemView);
            textView = itemView.findViewById(R.id.file_name_text_view);
            imageView = itemView.findViewById(R.id.icon_view);
        }
    }
}
