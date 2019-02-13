package com.example;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import reactor.core.scheduler.Schedulers;

public class DemoClientApp {

    public static void main(String[] args) throws Exception {
        URL u = new URL("http://localhost:8080/demo/repro");
        
        CountDownLatch cdl = new CountDownLatch(8);
        
        Runnable r = () -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    System.out.println("Connecting #" + i);
                    try (InputStream in = u.openStream()) {
                        while (true) {
                            int v = in.read();
                            if (v < 0) {
                                break;
                            }
                            System.out.print((char)v);
                        }
                    }
        //            Thread.sleep(200);
                    System.out.println();
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            cdl.countDown();
        };
        
        for (int i = 0; i < 7; i++) {
            Schedulers.elastic().schedule(r);
        }
        
        cdl.await();
    }
}
