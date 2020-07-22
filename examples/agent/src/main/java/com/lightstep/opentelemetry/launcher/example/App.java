package com.lightstep.opentelemetry.launcher.example;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class App {
  public static void main(String[] args) throws Exception {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder()
        .url("https://www.google.com")
        .build();

    try (Response response = client.newCall(request).execute()) {
      System.out.println("Response code: " + response.code());
    }

    client.dispatcher().executorService().shutdown();
    client.connectionPool().evictAll();

    TimeUnit.SECONDS.sleep(30);
  }
}
