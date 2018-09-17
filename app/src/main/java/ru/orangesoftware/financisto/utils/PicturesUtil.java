/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.orangesoftware.financisto.utils;

import android.content.Context;
import android.os.Environment;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PicturesUtil {

    private static final File PICTURES_DIR = new File(Environment.getExternalStorageDirectory(), "financisto/pictures");
    private static final File LEGACY_PICTURES_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

    private static final SimpleDateFormat PICTURE_FILE_NAME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSS");

    public static File createEmptyImageFile() {
        return pictureFile(PicturesUtil.PICTURE_FILE_NAME_FORMAT.format(new Date()) + ".jpg", false);
    }

    public static File pictureFile(String pictureFileName, boolean fallbackToLegacy) {
        if (!PICTURES_DIR.exists()) PICTURES_DIR.mkdirs();
        
        File file = new File(PICTURES_DIR, pictureFileName);
        if (fallbackToLegacy && !file.exists()) {
            file = new File(LEGACY_PICTURES_DIR, pictureFileName);
        }
        return file;
    }

    public static void showImage(Context context, ImageView imageView, String pictureFileName) {
        if (pictureFileName == null || imageView == null) return;
        Glide.with(context)
                .load(PicturesUtil.pictureFile(pictureFileName, true))
                .crossFade()
                //.override(320, 320)
                .into(imageView);
    }
}