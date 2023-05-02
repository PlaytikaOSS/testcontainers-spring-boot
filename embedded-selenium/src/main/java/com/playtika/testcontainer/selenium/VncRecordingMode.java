package com.playtika.testcontainer.selenium;


import org.testcontainers.containers.BrowserWebDriverContainer;

/**
 * See {@link org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode}
 */
public enum VncRecordingMode {
    SKIP, RECORD_ALL, RECORD_FAILING;

    public BrowserWebDriverContainer.VncRecordingMode convert() {
        return BrowserWebDriverContainer.VncRecordingMode.valueOf(this.name());
    }
}