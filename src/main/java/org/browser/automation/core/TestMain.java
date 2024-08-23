package org.browser.automation.core;

public class TestMain {

    public static void main(String[] args) {
        BrowserDetector bd = new BrowserDetector();
        System.out.println(bd.getDefaultBrowserInfo());
        System.out.println("----------");
        //bd.getInstalledBrowsers().forEach(System.out::println);
    }
}
