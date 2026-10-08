package com.wildcard.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** A file that was written to local disk and is now served from /files. */
@Getter
@AllArgsConstructor
public class StoredFile {

    private final String filename;
    private final String url;
    private final long size;
}
