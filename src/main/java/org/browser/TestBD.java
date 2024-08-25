package org.browser;

import org.browser.automation.core.BrowserDetector;

import java.util.Arrays;

public class TestBD {

    public static void main(String[] args) {
        BrowserDetector bd = new BrowserDetector();
        System.out.println(bd.getDefaultBrowserInfo());
        System.out.println(Arrays.toString(bd.getInstalledBrowsers().toArray(new BrowserDetector.BrowserInfo[0])));
    }
}
