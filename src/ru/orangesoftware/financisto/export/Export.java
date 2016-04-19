/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.export;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.export.docs.GoogleDriveClient;
import ru.orangesoftware.financisto.export.dropbox.Dropbox;
import ru.orangesoftware.financisto.utils.MyPreferences;

import java.io.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public abstract class Export {
	
	public static final File DEFAULT_EXPORT_PATH =  new File(Environment.getExternalStorageDirectory(), "financisto");
    public static final String BACKUP_MIME_TYPE = "application/x-gzip";

    private final Context context;
    private final boolean useGzip;

    protected Export(Context context, boolean useGzip) {
        this.context = context;
        this.useGzip = useGzip;
    }

    public String export() throws Exception {
		File path = getBackupFolder(context);
        String fileName = generateFilename();
        File file = new File(path, fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            if (useGzip) {
                export(new GZIPOutputStream(outputStream));
            } else {
                export(outputStream);
            }
        } finally {
            outputStream.flush();
            outputStream.close();
        }
        return fileName;
	}

    protected void export(OutputStream outputStream) throws Exception {
        generateBackup(outputStream);
    }
	
	/**
	 * Backup database to google docs
	 * 
	 * @param drive Google docs connection
	 * @param targetFolder Google docs folder name
	 * */
	public String exportOnline(Drive drive, String targetFolder) throws Exception {
		// get folder first
        String folderId = GoogleDriveClient.getOrCreateDriveFolder(drive, targetFolder);
		if (folderId == null) {
            throw new ImportExportException(R.string.gdocs_folder_not_found);
		}

		// generation backup file
		String fileName = generateFilename();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStream out = new BufferedOutputStream(new GZIPOutputStream(outputStream));
		generateBackup(out);

		// transforming streams
		InputStream backup = new ByteArrayInputStream(outputStream.toByteArray());

        InputStreamContent mediaContent = new InputStreamContent(BACKUP_MIME_TYPE, new  BufferedInputStream(backup));
        mediaContent.setLength(outputStream.size());
        // File's metadata.
        com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
        body.setTitle(fileName);
        body.setMimeType(BACKUP_MIME_TYPE);
        body.setFileSize((long)outputStream.size());
        List<ParentReference> parentReference = new ArrayList<ParentReference>();
        parentReference.add(new ParentReference().setId(folderId)) ;
        body.setParents(parentReference);
        com.google.api.services.drive.model.File file = drive.files().insert(body, mediaContent).execute();

		return fileName;
	}
	
	private String generateFilename() {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'_'HHmmss'_'SSS");
		return df.format(new Date())+getExtension();
	}
	
	private void generateBackup(OutputStream outputStream) throws Exception {
		OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
		BufferedWriter bw = new BufferedWriter(osw, 65536);
		try {
			writeHeader(bw);
			writeBody(bw);
			writeFooter(bw);
		} finally {
			bw.close();
		}	
	}

	protected abstract void writeHeader(BufferedWriter bw) throws IOException, NameNotFoundException;

	protected abstract void writeBody(BufferedWriter bw) throws IOException;

	protected abstract void writeFooter(BufferedWriter bw) throws IOException;

	protected abstract String getExtension();
	
	public static File getBackupFolder(Context context) {
        String path = MyPreferences.getDatabaseBackupFolder(context);
        File file = new File(path);
        file.mkdirs();
        if (file.isDirectory() && file.canWrite()) {
            return file;
        }
        file = Export.DEFAULT_EXPORT_PATH;
        file.mkdirs();
        return file;
	}

    public static File getBackupFile(Context context, String backupFileName) {
        File path = getBackupFolder(context);
        return new File(path, backupFileName);
    }

    public static void uploadBackupFileToDropbox(Context context, String backupFileName) throws Exception {
        File file = getBackupFile(context, backupFileName);
        Dropbox dropbox = new Dropbox(context);
        dropbox.uploadFile(file);
    }

}
