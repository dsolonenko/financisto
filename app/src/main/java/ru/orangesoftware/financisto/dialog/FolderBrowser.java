/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import ru.orangesoftware.financisto.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.orangesoftware.financisto.utils.Utils.text;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 12/23/11 12:53 AM
 *
 */
public class FolderBrowser extends ListActivity {

    public static final String PATH = "PATH";
    
    private final List<FileItem> files = new ArrayList<FileItem>();

    private Button selectButton;
    private Button createButton;
    private File selectedFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.folder_browser);

        selectButton = (Button)findViewById(R.id.selectButton);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent result = new Intent();
                result.putExtra(PATH, selectedFolder.getAbsolutePath());
                setResult(RESULT_OK, result);
                finish();
            }
        });

        createButton = (Button)findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewFolder();
            }
        });
        
        if (!browseToCurrentFolder()) {
            browseToRoot();
        }
    }

    private boolean browseToCurrentFolder() {
        Intent intent = getIntent();
        if (intent != null) {
            String path = intent.getStringExtra(PATH);
            if (path != null) {
                browseTo(new File(path));
                return true;
            }
        }
        return false;
    }

    private void createNewFolder() {
        final EditText editText = new EditText(this);
        Dialog d = new AlertDialog.Builder(this)
                .setTitle(R.string.create_new_folder_title)
                .setView(editText)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        createNewFolder(text(editText));
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        d.show();
    }
    
    private void createNewFolder(String name) {
        boolean result = false;
        try {
            result = new File(selectedFolder, name).mkdirs();
        } catch (Exception e) {
            result = false;
        } finally {
            if (!result) {
                Toast.makeText(this, R.string.create_new_folder_fail, Toast.LENGTH_LONG).show();
            }
            browseTo(selectedFolder);
        }
    }

    private void browseToRoot() {
        browseTo(new File("/"));
    }

    private void browseTo(File current) {
        files.clear();
        upOneLevel(current);
        browse(current);
        setAdapter();
        selectCurrentFolder(current);
    }

    private void selectCurrentFolder(File current) {
        boolean isWritable = current.canWrite();
        selectButton.setEnabled(isWritable);
        createButton.setEnabled(isWritable);
        selectedFolder = isWritable ? current : null;
        setTitle(current.getAbsolutePath());
    }

    private void upOneLevel(File current) {
        File parent = current.getParentFile();
        if (parent != null) {
            files.add(new OnLevelUp(parent));
        }
    }

    private void browse(File current) {
        File[] files = current.listFiles();
        if (files != null) {
            Arrays.sort(files);
            for (File file : files) {
                if (isWritableDirectory(file)) {
                    this.files.add(new FileItem(file));
                }
            }
        }
    }

    private boolean isWritableDirectory(File file) {
        return file.isDirectory() && file.canRead() && file.canWrite();
    }

    private void setAdapter() {
        ListAdapter adapter = new ArrayAdapter<FileItem>(this, android.R.layout.simple_list_item_1, files);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        FileItem selected = files.get(position);
        browseTo(selected.file);
    }
    
    private static class FileItem {
        private final File file;

        private FileItem(File file) {
            this.file = file;
        }

        @Override
        public String toString() {
            return file.getName();
        }

    }

    private static class OnLevelUp extends FileItem {

        private OnLevelUp(File file) {
            super(file);
        }

        @Override
        public String toString() {
            return "..";
        }
    }

}
