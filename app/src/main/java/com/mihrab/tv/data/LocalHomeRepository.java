package com.mihrab.tv.data;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.json.JSONObject;

public final class LocalHomeRepository implements HomeRepository {
  private final Context context;

  public LocalHomeRepository(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public HomeSnapshot load(Instant instant) throws Exception {
    try (InputStream input = context.getAssets().open("content.json")) {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      int count;
      while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
      JSONObject data = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
      return new HomeSnapshot(
          data.getJSONArray("hadiths"), data.getJSONArray("verses"), data.getString("retrieved"));
    }
  }
}
