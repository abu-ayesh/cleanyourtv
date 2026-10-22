package com.mihrab.tv.ui.home;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import com.mihrab.tv.R;
import com.mihrab.tv.core.*;
import com.mihrab.tv.data.*;
import com.mihrab.tv.launcher.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public final class HomeActivity extends Activity {
  private FrameLayout stage, overlay, modalPanel;
  private WallpaperView wallpaper;
  private HomeSnapshot snapshot;
  private AppRepository appRepository;
  private List<LaunchableApp> apps = Collections.emptyList();
  private final LinkedHashMap<String, View> controls = new LinkedHashMap<>(),
      modalControls = new LinkedHashMap<>();
  private final List<GlassDrawable> glassSurfaces = new ArrayList<>();
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private SharedPreferences preferences;
  private boolean resumed, receiverRegistered, glass, h24, arabic, motion;
  private int scene, sceneGeneration, appGeneration, hadithOffset, appPage;
  private String savedFocus = "nav_home", beforeModal = "nav_home", lastDay = "";
  private Typeface textFont, displayFont;
  private ScrollView reader;
  private long nextRotation;
  private static final String[] BACKGROUNDS = {
    "قوس من نور", "ليلة رمضانية", "سكينة الغابة", "ذهب الغروب", "فانوس السحاب", "دفء الحكاية"
  };
  private final Runnable tick =
      new Runnable() {
        @Override
        public void run() {
          if (!resumed) return;
          updateClock();
          if (snapshot != null) updateDaily();
          if (overlay == null
              && preferences.getBoolean("rotate", true)
              && SystemClock.elapsedRealtime() >= nextRotation) selectScene(scene % 6 + 1);
          main.postDelayed(this, 1000);
        }
      };
  private final BroadcastReceiver packages =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
          reloadApps();
        }
      };

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    setContentView(R.layout.activity_home);
    stage = findViewById(R.id.stage);
    wallpaper = findViewById(R.id.wallpaper);
    preferences = getSharedPreferences("mihrab", MODE_PRIVATE);
    textFont = getResources().getFont(R.font.mihrab_text);
    displayFont = getResources().getFont(R.font.mihrab_display);
    glass = preferences.getBoolean("glass", true);
    h24 = preferences.getBoolean("h24", false);
    arabic = preferences.getBoolean("arabic", false);
    motion = preferences.getBoolean("motion", true);
    if (state != null) {
      savedFocus = state.getString("focus", "nav_home");
      hadithOffset = state.getInt("hadithOffset");
      appPage = state.getInt("appPage");
    }
    appRepository = new AppRepository(this);
    surface(findViewById(R.id.hadith_panel), 27, 0x80081321);
    surface(findViewById(R.id.clock_panel), 27, 0x400a1724);
    surface(findViewById(R.id.navigation), 22, 0x380a1623);
    surface(findViewById(R.id.prayer_band), 22, 0x520f1b2a);
    View next = findViewById(R.id.next_prayer);
    surface(next, 21, 0x15ead5ae);
    surface(findViewById(R.id.notifications), 20, 0x3d081420);
    setupControl("nav_home", R.id.nav_home, () -> focus("hadith_detail"));
    findViewById(R.id.nav_home).setBackground(round(0x1cffffff, 15, 0));
    setupControl("nav_quran", R.id.nav_quran, this::laterQuran);
    setupControl("nav_apps", R.id.nav_apps, this::showApps);
    setupIcon("settings", R.id.settings, R.drawable.ic_settings, this::showSettings);
    setupIcon("appearance", R.id.appearance, R.drawable.ic_landscape, this::showAppearance);
    setupIcon(
        "standby",
        R.id.standby,
        R.drawable.ic_standby,
        () ->
            showMessage(
                "شاشة السكون",
                "تصميم السكون معتمد. تحويله إلى DreamService مؤجل إلى المرحلة التالية."));
    setupControl("location", R.id.location, this::showSettings);
    ((TextView) findViewById(R.id.location))
        .setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_pin, 0);
    setupControl("hadith_detail", R.id.hadith_detail, this::showHadith);
    findViewById(R.id.hadith_detail).setBackground(round(0xffd5c5a4, 12, 0xfff5e4c7));
    setupControl(
        "hadith_next",
        R.id.hadith_next,
        () -> {
          hadithOffset++;
          renderHadith();
        });
    setupControl(
        "notifications",
        R.id.notifications,
        () ->
            showMessage(
                "تنبيهات الصلاة",
                "تنبيهات الصلاة معطّلة في المرحلة الأولى. لا توجد أصوات أو جدولة أو تنبيهات فوق"
                    + " التطبيقات."));
    setupControl("prayer_source", R.id.prayer_source, this::showPrayers);
    setupControl("next_prayer", R.id.next_prayer, this::showPrayers);
    ((ImageView) findViewById(R.id.mosque)).setImageResource(R.drawable.ic_mosque);
    setupControl("recitation", R.id.recitation, this::laterQuran);
    setupControl("verse", R.id.verse_card, this::showVerse);
    setupControl("dhikr", R.id.dhikr_card, this::showDhikr);
    setupControl(
        "ramadan",
        R.id.ramadan_card,
        () ->
            showMessage(
                "شوقًا إلى رمضان",
                "عدّاد رمضان هنا نموذج عرض ثابت؛ ليس حسابًا لتاريخ الشهر القادم. سيُربط بتقويم"
                    + " حسابي موضح المصدر، مع بقاء ثبوت الهلال مرجعًا لبداية الشهر."));
    surface(findViewById(R.id.verse_card), 22, 0x520f1b2a);
    surface(findViewById(R.id.dhikr_card), 22, 0x520f1b2a);
    surface(findViewById(R.id.ramadan_card), 22, 0x52151b26);
    findViewById(R.id.architecture)
        .setVisibility(preferences.getBoolean("pattern", true) ? View.VISIBLE : View.INVISIBLE);
    for (View control : controls.values()) focusFrame(control);
    selectScene(Math.max(1, Math.min(6, preferences.getInt("scene", 5))));
    worker.execute(
        () -> {
          try {
            HomeSnapshot loaded = new LocalHomeRepository(this).load(Instant.now());
            main.post(
                () -> {
                  if (isDestroyed()) return;
                  snapshot = loaded;
                  renderHadith();
                  renderPrayers();
                  lastDay = "";
                  updateDaily();
                });
          } catch (Exception e) {
            main.post(() -> showMessage("المحتوى المحلي", "تعذّرت قراءة المحتوى المحفوظ."));
          }
        });
  }

  @Override
  protected void onResume() {
    super.onResume();
    resumed = true;
    main.removeCallbacks(tick);
    tick.run();
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_PACKAGE_ADDED);
    filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
    filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
    filter.addDataScheme("package");
    if (Build.VERSION.SDK_INT >= 33)
      registerReceiver(packages, filter, Context.RECEIVER_NOT_EXPORTED);
    else registerReceiver(packages, filter);
    receiverRegistered = true;
    reloadApps();
    stage.post(this::restoreFocus);
  }

  @Override
  protected void onPause() {
    rememberFocus();
    resumed = false;
    main.removeCallbacks(tick);
    if (receiverRegistered) {
      unregisterReceiver(packages);
      receiverRegistered = false;
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    main.removeCallbacksAndMessages(null);
    worker.shutdownNow();
    super.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    rememberFocus();
    out.putString("focus", savedFocus);
    out.putInt("hadithOffset", hadithOffset);
    out.putInt("appPage", appPage);
    super.onSaveInstanceState(out);
  }

  private void rememberFocus() {
    View focused = getCurrentFocus();
    if (focused != null && focused.getTag() instanceof String && overlay == null)
      savedFocus = (String) focused.getTag();
  }

  private void restoreFocus() {
    if (overlay != null) return;
    focus(FocusNavigator.restore(new ArrayList<>(controls.keySet()), savedFocus, "nav_home"));
  }

  private void focus(String key) {
    View view = controls.get(key);
    if (view != null) view.requestFocus();
  }

  private int dp(float value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private TextView text(String value, float size, int color, boolean display) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setTypeface(display ? displayFont : textFont);
    t.setIncludeFontPadding(false);
    t.setFontFeatureSettings("kern,liga,ss01");
    t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
    return t;
  }

  private void place(FrameLayout parent, View view, float x, float y, float w, float h) {
    FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(w), dp(h));
    p.gravity = Gravity.TOP | Gravity.LEFT;
    p.leftMargin = dp(x);
    p.topMargin = dp(y);
    parent.addView(view, p);
  }

  private GradientDrawable round(int color, float radius, int stroke) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    if (stroke != 0) d.setStroke(dp(1), stroke);
    return d;
  }

  private GlassDrawable surface(View view, float radius, int tint) {
    GlassDrawable d = new GlassDrawable(view, wallpaper, radius, tint);
    d.setGlass(glass);
    view.setBackground(d);
    glassSurfaces.add(d);
    return d;
  }

  private void setupIcon(String key, int id, int drawable, Runnable action) {
    ImageView v = findViewById(id);
    v.setImageResource(drawable);
    surface(v, 30, 0x4d0a1522);
    setupControl(key, id, action);
  }

  private void setupControl(String key, int id, Runnable action) {
    register(controls, key, findViewById(id), action, false);
  }

  private void register(
      Map<String, View> map, String key, View view, Runnable action, boolean app) {
    map.put(key, view);
    view.setTag(key);
    view.setFocusable(true);
    view.setFocusableInTouchMode(true);
    view.setClickable(true);
    view.setDefaultFocusHighlightEnabled(false);
    view.setOnClickListener(v -> action.run());
    focusFrame(view);
    view.setOnFocusChangeListener(
        (v, focused) -> {
          v.animate().cancel();
          float scale = app && focused ? 1.025f : 1;
          float lift = app && focused ? -dp(6) : 0;
          if (motion)
            v.animate().scaleX(scale).scaleY(scale).translationY(lift).setDuration(180).start();
          else {
            v.setScaleX(scale);
            v.setScaleY(scale);
            v.setTranslationY(lift);
          }
          v.setElevation(focused ? dp(6) : 0);
          v.invalidate();
          if (focused && map == controls) savedFocus = key;
        });
  }

  private void focusFrame(View view) {
    if (!(view.getBackground() instanceof GlassDrawable)
        && !(view.getBackground() instanceof FocusFrameDrawable))
      view.setBackground(new FocusFrameDrawable(view, view.getBackground()));
  }

  private void selectScene(int id) {
    scene = id;
    nextRotation = SystemClock.elapsedRealtime() + 20 * 60 * 1000L;
    int generation = ++sceneGeneration;
    worker.execute(
        () -> {
          int
              original =
                  getResources().getIdentifier("wallpaper_" + id, "drawable", getPackageName()),
              blur = getResources().getIdentifier("glass_" + id, "drawable", getPackageName());
          BitmapFactory.Options options = new BitmapFactory.Options();
          options.inScaled = false;
          options.inPreferredConfig = Bitmap.Config.RGB_565;
          // Source art is retained unchanged in resources; decoding is bounded for a 2 GB TV.
          options.inJustDecodeBounds = true;
          BitmapFactory.decodeResource(getResources(), original, options);
          int sample = 1;
          while (options.outWidth / sample > 2560 || options.outHeight / sample > 1440) sample *= 2;
          options.inJustDecodeBounds = false;
          options.inSampleSize = sample;
          Bitmap image = BitmapFactory.decodeResource(getResources(), original, options);
          options.inSampleSize = 1;
          Bitmap blurred = BitmapFactory.decodeResource(getResources(), blur, options);
          main.post(
              () -> {
                if (isDestroyed() || generation != sceneGeneration) return;
                scene = id;
                wallpaper.setScene(id, image, blurred);
                for (GlassDrawable d : glassSurfaces) d.invalidateSelf();
                preferences.edit().putInt("scene", id).apply();
                nextRotation = SystemClock.elapsedRealtime() + 20 * 60 * 1000L;
              });
        });
  }

  private void updateClock() {
    Instant now = Instant.now();
    String clockValue = HomeClock.clock(now, h24, arabic);
    android.text.SpannableString clockText = new android.text.SpannableString(clockValue);
    if (!h24)
      clockText.setSpan(
          new android.text.style.AbsoluteSizeSpan(
              Math.round(16 * getResources().getDisplayMetrics().scaledDensity)),
          clockValue.lastIndexOf(' ') + 1,
          clockValue.length(),
          0);
    setTextIfChanged(R.id.clock, clockText);
    ((TextView) findViewById(R.id.clock)).setTextSize(72);
    setTextIfChanged(R.id.hijri_date, HomeClock.hijri(now, arabic));
    setTextIfChanged(R.id.gregorian_date, HomeClock.gregorian(now, arabic));
    setTextIfChanged(
        R.id.greeting,
        HomeClock.friday(now)
            ? "جمعةٌ طيّبة، وذكرٌ لا ينقطع"
            : HomeClock.morning(now) ? "صباحك طمأنينة" : "مساؤك سكينة");
  }

  private void setTextIfChanged(int id, CharSequence value) {
    TextView view = findViewById(id);
    if (!value.toString().contentEquals(view.getText())) view.setText(value);
  }

  private JSONObject hadith() {
    return snapshot.hadiths.optJSONObject(
        Math.floorMod(
            HomeClock.dayIndex(Instant.now(), snapshot.hadiths.length()) + hadithOffset,
            snapshot.hadiths.length()));
  }

  private void renderHadith() {
    if (snapshot == null) return;
    JSONObject h = hadith();
    Map<String, String> names = new HashMap<>();
    names.put("4792", "عائشة رضي الله عنها");
    names.put("5512", "أبي هريرة رضي الله عنه");
    names.put("5805", "أبي هريرة رضي الله عنه");
    names.put("6460", "أبي مسعود رضي الله عنه");
    names.put("65089", "محمود بن لبيد، عن عثمان بن عفان رضي الله عنهما");
    names.put("3107", "أبي هريرة رضي الله عنه");
    names.put("5520", "عبد الله بن سلام رضي الله عنه");
    ((TextView) findViewById(R.id.hadith_narrator))
        .setText("عن " + names.getOrDefault(h.optString("id"), "راوي الحديث — التفاصيل في النصّ"));
    TextView title = findViewById(R.id.hadith_text);
    title.setText(h.optString("title"));
    title.setTextSize(h.optString("title").length() > 55 ? 30 : 40);
    ((TextView) findViewById(R.id.hadith_reference))
        .setText(h.optString("attribution") + " · " + h.optString("reference").split("\n")[0]);
    TextView grade = findViewById(R.id.hadith_grade);
    grade.setText(h.optString("grade"));
    grade.setBackground(round(0x09a0c4a8, 6, 0x3596bea8));
  }

  private JSONObject verse() {
    return snapshot.verses.optJSONObject(
        HomeClock.dayIndex(Instant.now(), snapshot.verses.length()));
  }

  private String verseReference(JSONObject v) {
    return v.optString("surah") + " · الآية " + HomeClock.numbers(v.optString("ayah"), arabic);
  }

  private void renderPrayers() {
    FrameLayout row = findViewById(R.id.prayer_times);
    for (int i = 0; i < row.getChildCount(); i++)
      glassSurfaces.remove(row.getChildAt(i).getBackground());
    row.removeAllViews();
    controls.keySet().removeIf(k -> k.startsWith("prayer:"));
    for (int i = 0; i < 5; i++) {
      int index = i;
      FrameLayout tile = new FrameLayout(this);
      place(row, tile, 14 + (4 - i) * 217, 9, 208, 75);
      TextView name = text(snapshot.prayerNames[i], 15, 0xffd2d9df, false);
      name.setGravity(Gravity.CENTER);
      place(tile, name, 0, 7, 208, 22);
      TextView time =
          text(
              HomeClock.time(snapshot.prayerTimes[i], h24, arabic),
              24,
              i == 2 ? 0xffe9d6af : 0xffedf0eb,
              false);
      time.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
      time.setGravity(Gravity.CENTER);
      time.setTextDirection(View.TEXT_DIRECTION_LTR);
      place(tile, time, 0, 37, 208, 29);
      if (i == 2) tile.setBackground(round(0x1aefd6a9, 13, 0x80eed9b4));
      register(controls, "prayer:" + index, tile, this::showPrayers, false);
    }
  }

  private void card(int id, String title, String body, String foot, boolean display) {
    FrameLayout card = findViewById(id);
    card.removeAllViews();
    float width = card.getLayoutParams().width / getResources().getDisplayMetrics().density;
    TextView heading = text(title + "  ↖", 12, 0xffe2e6e7, false);
    int iconId =
        id == R.id.verse_card ? R.drawable.ic_book : id == R.id.dhikr_card ? R.drawable.ic_sun : 0;
    if (iconId != 0) {
      android.graphics.drawable.Drawable icon = getDrawable(iconId);
      icon.setBounds(0, 0, dp(17), dp(17));
      heading.setCompoundDrawables(null, null, icon, null);
      heading.setCompoundDrawablePadding(dp(8));
    }
    place(card, heading, 22, 13, width - 44, 23);
    TextView mainText = text(body, id == R.id.ramadan_card ? 32 : 26, 0xffece5d7, display);
    if (id == R.id.ramadan_card) {
      mainText.setTextSize(14);
      mainText.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
      android.text.SpannableString counter = new android.text.SpannableString(body);
      counter.setSpan(
          new android.text.style.AbsoluteSizeSpan(
              Math.round(38 * getResources().getDisplayMetrics().scaledDensity)),
          0,
          3,
          0);
      mainText.setText(counter);
    }
    mainText.setMaxLines(1);
    mainText.setEllipsize(android.text.TextUtils.TruncateAt.END);
    place(card, mainText, 22, 39, width - 44, 46);
    TextView bottom = text(foot, 11, 0xffbecbd5, false);
    place(card, bottom, 22, 94, width - 44, 21);
  }

  private void updateDaily() {
    Instant now = Instant.now();
    String day = HomeClock.local(now).toLocalDate() + ":" + HomeClock.morning(now) + ":" + arabic;
    if (day.equals(lastDay)) return;
    lastDay = day;
    renderHadith();
    JSONObject v = verse();
    card(R.id.verse_card, "آيةٌ تُطمئنّ القلب", v.optString("text"), verseReference(v), true);
    card(
        R.id.dhikr_card,
        HomeClock.friday(now)
            ? "نور يوم الجمعة"
            : HomeClock.morning(now) ? "أذكار الصباح" : "أذكار المساء",
        HomeClock.friday(now)
            ? "اللهمّ صلِّ وسلِّم على نبيّنا محمّد"
            : HomeClock.morning(now)
                ? "اللهمّ بك أصبحنا، وبك أمسينا"
                : "اللهمّ بك أمسينا، وبك أصبحنا",
        "افتح الأذكار · اقرأ على مهل",
        true);
    card(
        R.id.ramadan_card,
        "شوقًا إلى رمضان",
        HomeClock.numbers("108 يومًا  |  12 ساعة", arabic),
        "نموذج عرض تجريبي · رهن ثبوت الهلال",
        false);
    ImageView crescent = new ImageView(this);
    crescent.setImageResource(R.drawable.ic_crescent);
    crescent.setAlpha(.14f);
    place(findViewById(R.id.ramadan_card), crescent, 40, 28, 80, 80);
  }

  private void reloadApps() {
    int generation = ++appGeneration;
    worker.execute(
        () -> {
          List<LaunchableApp> loaded = appRepository.load();
          main.post(
              () -> {
                if (isDestroyed() || generation != appGeneration) return;
                boolean changed = apps.size() != loaded.size();
                if (!changed)
                  for (int i = 0; i < apps.size(); i++)
                    if (!apps.get(i).key.equals(loaded.get(i).key)
                        || !apps.get(i).label.equals(loaded.get(i).label)) {
                      changed = true;
                      break;
                    }
                apps = loaded;
                if (changed || ((FrameLayout) findViewById(R.id.app_row)).getChildCount() == 0)
                  renderApps();
              });
        });
  }

  private void renderApps() {
    rememberFocus();
    FrameLayout row = findViewById(R.id.app_row);
    for (int i = 0; i < row.getChildCount(); i++) {
      View child = row.getChildAt(i);
      if (child.getBackground() instanceof GlassDrawable)
        glassSurfaces.remove(child.getBackground());
    }
    row.removeAllViews();
    controls.keySet().removeIf(k -> k.startsWith("app:"));
    if (apps.isEmpty()) {
      TextView empty = text(getString(R.string.no_apps), 15, 0xffc1cbd3, false);
      place(row, empty, 0, 0, 1466, 112);
      return;
    }
    int pages = (apps.size() + 7) / 8;
    appPage = Math.min(appPage, pages - 1);
    for (int slot = 0; slot < 8; slot++) {
      int index = appPage * 8 + slot;
      if (index >= apps.size()) break;
      LaunchableApp app = apps.get(index);
      FrameLayout tile = new FrameLayout(this);
      tile.setContentDescription(app.label);
      place(row, tile, (7 - slot) * 184.875f, 0, 171.875f, 112);
      surface(tile, 19, 0x520f1b2a);
      ImageView icon = new ImageView(this);
      icon.setImageDrawable(app.icon);
      place(tile, icon, 58, 12, 56, 56);
      TextView name = text(app.label, 14, 0xfff3f2ec, false);
      if (app.label.chars().allMatch(c -> c < 128))
        name.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
      name.setGravity(Gravity.CENTER);
      name.setMaxLines(1);
      name.setEllipsize(android.text.TextUtils.TruncateAt.END);
      place(tile, name, 6, 77, 160, 25);
      register(controls, "app:" + app.key, tile, () -> launch(app), true);
    }
    stage.post(this::restoreFocus);
  }

  private void launch(LaunchableApp app) {
    if (!AppRepository.safeComponent(app.component.getPackageName(), app.component.getClassName()))
      return;
    try {
      rememberFocus();
      startActivity(appRepository.intent(app));
    } catch (ActivityNotFoundException | SecurityException e) {
      showMessage("تعذّر فتح التطبيق", app.label + " غير متاح الآن.");
      reloadApps();
    }
  }

  private void laterQuran() {
    showMessage(
        "القرآن والأذكار",
        "تبويب القرآن والمشغّل الصوتي مؤجلان إلى مرحلة لاحقة بعد اعتماد الشاشة الرئيسية.");
  }

  private void openModal(String title) {
    if (overlay == null) {
      rememberFocus();
      beforeModal = savedFocus;
    } else {
      stage.removeView(overlay);
      glassSurfaces.removeIf(
          d -> d.getCallback() instanceof View && !((View) d.getCallback()).isAttachedToWindow());
    }
    modalControls.clear();
    overlay = new FrameLayout(this);
    overlay.setBackgroundColor(0x90030911);
    overlay.setClickable(true);
    place(stage, overlay, 0, 0, 1600, 900);
    modalPanel = new FrameLayout(this);
    place(overlay, modalPanel, 360, 70, 880, 760);
    surface(modalPanel, 28, 0xeb0b1725);
    TextView heading = text(title, 29, 0xffe7d9bc, false);
    place(modalPanel, heading, 43, 65, 720, 53);
    TextView kicker = text("M I H R A B  /  مساحة للسكينة", 10, 0xffacb8c1, false);
    place(modalPanel, kicker, 43, 31, 720, 22);
    TextView close = text("×", 27, 0xfff3f2ec, false);
    close.setGravity(Gravity.CENTER);
    place(modalPanel, close, 35, 27, 41, 41);
    register(modalControls, "modal:close", close, this::closeModal, false);
    reader = new ScrollView(this);
    reader.setFillViewport(false);
    reader.setClipToPadding(false);
    place(modalPanel, reader, 43, 135, 794, 575);
  }

  private LinearLayout modalContent() {
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    content.setPadding(dp(8), dp(6), dp(8), dp(15));
    reader.addView(content);
    return content;
  }

  private void paragraph(LinearLayout parent, String value, boolean scripture) {
    TextView body =
        text(value, scripture ? 28 : 17, scripture ? 0xffeee6d7 : 0xffb9c6ce, scripture);
    body.setPadding(dp(12), dp(10), dp(12), dp(10));
    body.setLineSpacing(dp(5), 1.25f);
    parent.addView(body, new LinearLayout.LayoutParams(-1, -2));
  }

  private TextView option(LinearLayout parent, String key, String label, Runnable action) {
    TextView button = text(label, 17, 0xffe4e5df, false);
    button.setPadding(dp(17), dp(12), dp(17), dp(12));
    button.setMinHeight(dp(49));
    button.setBackground(round(0x0dffffff, 9, 0x40d5c5a4));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.topMargin = dp(10);
    parent.addView(button, params);
    register(modalControls, key, button, action, false);
    return button;
  }

  private void finishModal() {
    overlay.post(
        () -> {
          View first = modalControls.get("modal:close");
          if (first != null) first.requestFocus();
        });
  }

  private void closeModal() {
    if (overlay == null) return;
    stage.removeView(overlay);
    glassSurfaces.removeIf(
        d -> d.getCallback() instanceof View && !((View) d.getCallback()).isAttachedToWindow());
    overlay = null;
    modalPanel = null;
    reader = null;
    modalControls.clear();
    savedFocus = beforeModal;
    restoreFocus();
  }

  private void showMessage(String title, String body) {
    openModal(title);
    LinearLayout content = modalContent();
    paragraph(content, body, false);
    finishModal();
  }

  private void showHadith() {
    if (snapshot == null) return;
    JSONObject h = hadith();
    openModal("حديث اليوم — النصّ والشرح");
    LinearLayout content = modalContent();
    paragraph(content, h.optString("hadeeth"), true);
    paragraph(content, h.optString("grade") + " · " + h.optString("attribution"), false);
    paragraph(content, "شرح الحديث\n" + h.optString("explanation"), false);
    paragraph(
        content,
        h.optString("reference")
            + "\nالمصدر والناشر: HadeethEnc.com\nhttps://hadeethenc.com/ar/browse/hadith/"
            + h.optString("id")
            + "\nنص محلي محفوظ كما ورد · آخر جلب: "
            + snapshot.retrieved,
        false);
    option(
        content,
        "modal:next",
        "حديث آخر",
        () -> {
          hadithOffset++;
          renderHadith();
          showHadith();
          modalControls.get("modal:next").requestFocus();
        });
    finishModal();
  }

  private void showVerse() {
    if (snapshot != null) {
      JSONObject v = verse();
      showMessage(
          "آيةٌ تُطمئنّ القلب",
          v.optString("text")
              + "\n\n"
              + verseReference(v)
              + "\nنص محلي من مرجع التصميم · AlQuran Cloud / quran-uthmani");
    }
  }

  private void showDhikr() {
    if (snapshot == null) return;
    showMessage(
        HomeClock.morning(Instant.now()) ? "أذكار الصباح" : "أذكار المساء",
        HomeClock.morning(Instant.now())
            ? "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ،"
                + " وَإِلَيْكَ النُّشُورُ\n\n"
                + "أبو داود (5068)، الترمذي (3391)"
            : "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ،"
                + " وَإِلَيْكَ الْمَصِيرُ\n\n"
                + "أبو داود (5068)، الترمذي (3391)");
  }

  private void showPrayers() {
    showMessage(
        "مواقيت الصلاة — عرض تجريبي",
        "الأوقات الظاهرة نموذج ثابت من التصميم وليست مواقيت اليوم؛ لذلك لا يعمل العدّاد أو"
            + " التنبيه.\n\n"
            + "الملف المقترح للربط لاحقًا: القدس، فلسطين · Asia/Hebron · طريقة رابطة العالم"
            + " الإسلامي (3). يلزم تحقق محلي قبل الاستخدام اليومي، ولا تُعد مواقيت الأوقاف.");
  }

  private void showSettings() {
    openModal("الإعدادات");
    LinearLayout content = modalContent();
    paragraph(
        content,
        "القدس، فلسطين · التاريخ الهجري حسابي\nمواقيت الصلاة وعدّاد رمضان بيانات عرض تجريبية.",
        false);
    option(
        content,
        "settings:format",
        "نظام الوقت: " + (h24 ? "24 ساعة" : "12 ساعة"),
        () -> {
          h24 = !h24;
          preferences.edit().putBoolean("h24", h24).apply();
          updateClock();
          if (snapshot != null) renderPrayers();
          ((TextView) modalControls.get("settings:format"))
              .setText("نظام الوقت: " + (h24 ? "24 ساعة" : "12 ساعة"));
        });
    option(
        content,
        "settings:numerals",
        "شكل الأرقام: " + (arabic ? "عربية" : "إنجليزية"),
        () -> {
          arabic = !arabic;
          preferences.edit().putBoolean("arabic", arabic).apply();
          lastDay = "";
          updateClock();
          if (snapshot != null) {
            updateDaily();
            renderPrayers();
          }
          ((TextView) modalControls.get("settings:numerals"))
              .setText("شكل الأرقام: " + (arabic ? "عربية" : "إنجليزية"));
        });
    option(
        content,
        "settings:motion",
        "حركة التركيز: " + (motion ? "مفعّلة" : "معطّلة"),
        () -> {
          motion = !motion;
          preferences.edit().putBoolean("motion", motion).apply();
          ((TextView) modalControls.get("settings:motion"))
              .setText("حركة التركيز: " + (motion ? "مفعّلة" : "معطّلة"));
        });
    option(
        content,
        "settings:pattern",
        "القوس والزخرفة",
        () -> {
          boolean enabled = !preferences.getBoolean("pattern", true);
          preferences.edit().putBoolean("pattern", enabled).apply();
          findViewById(R.id.architecture).setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        });
    option(content, "settings:appearance", "الخلفيات والمظهر", this::showAppearance);
    option(
        content,
        "settings:android",
        "إعدادات Android",
        () -> {
          try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
          } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "تعذّر فتح إعدادات Android", Toast.LENGTH_SHORT).show();
          }
        });
    finishModal();
  }

  private void showAppearance() {
    openModal("الخلفيات والمظهر");
    LinearLayout content = modalContent();
    paragraph(content, "مشاهد تتبدّل، والسكينة باقية.", false);
    FrameLayout gallery = new FrameLayout(this);
    content.addView(gallery, new LinearLayout.LayoutParams(-1, dp(258)));
    for (int i = 1; i <= 6; i++) {
      int id = i;
      FrameLayout choice = new FrameLayout(this);
      place(gallery, choice, (2 - (i - 1) % 3) * 262, ((i - 1) / 3) * 135, 250, 123);
      choice.setBackground(round(0xff142131, 15, 0x40ffffff));
      choice.setClipToOutline(true);
      ImageView thumbnail = new ImageView(this);
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inSampleSize = 8;
      thumbnail.setImageBitmap(
          BitmapFactory.decodeResource(
              getResources(),
              getResources().getIdentifier("wallpaper_" + i, "drawable", getPackageName()),
              o));
      thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
      choice.addView(thumbnail, new FrameLayout.LayoutParams(-1, -1));
      TextView label = text(BACKGROUNDS[i - 1] + (scene == i ? "  ✓" : ""), 14, 0xfffff4de, false);
      label.setPadding(dp(12), 0, dp(12), 0);
      label.setBackground(
          new GradientDrawable(
              GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0x00050d1b, 0xd9050d1b}));
      place(choice, label, 0, 70, 250, 53);
      choice.setContentDescription(BACKGROUNDS[i - 1]);
      register(
          modalControls,
          "wallpaper:" + i,
          choice,
          () -> {
            selectScene(id);
            for (int n = 1; n <= 6; n++) {
              FrameLayout tile = (FrameLayout) modalControls.get("wallpaper:" + n);
              ((TextView) tile.getChildAt(1)).setText(BACKGROUNDS[n - 1] + (id == n ? "  ✓" : ""));
            }
          },
          false);
    }
    option(
        content,
        "appearance:rotate",
        "تبديل الخلفيات تلقائيًا: " + (preferences.getBoolean("rotate", true) ? "مفعّل" : "معطّل"),
        () -> {
          boolean rotate = !preferences.getBoolean("rotate", true);
          preferences.edit().putBoolean("rotate", rotate).apply();
          nextRotation = SystemClock.elapsedRealtime() + 20 * 60 * 1000L;
          ((TextView) modalControls.get("appearance:rotate"))
              .setText("تبديل الخلفيات تلقائيًا: " + (rotate ? "مفعّل" : "معطّل"));
        });
    option(
        content,
        "appearance:glass",
        "الزجاج المضيء: " + (glass ? "مفعّل" : "معطّل"),
        () -> {
          glass = !glass;
          preferences.edit().putBoolean("glass", glass).apply();
          for (GlassDrawable d : glassSurfaces) d.setGlass(glass);
          ((TextView) modalControls.get("appearance:glass"))
              .setText("الزجاج المضيء: " + (glass ? "مفعّل" : "معطّل"));
        });
    finishModal();
  }

  private void showApps() {
    openModal("تطبيقاتي");
    LinearLayout content = modalContent();
    if (apps.isEmpty()) paragraph(content, getString(R.string.no_apps), false);
    for (LaunchableApp app : apps)
      option(content, "library:" + app.key, app.label, () -> launch(app));
    finishModal();
  }

  @Override
  public void onBackPressed() {
    if (overlay != null) closeModal();
    else if (!"nav_home".equals(savedFocus)) focus("nav_home");
    else super.onBackPressed();
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
      if (event.getAction() == KeyEvent.ACTION_UP) onBackPressed();
      return true;
    }
    int direction =
        switch (event.getKeyCode()) {
          case KeyEvent.KEYCODE_DPAD_LEFT -> FocusNavigator.LEFT;
          case KeyEvent.KEYCODE_DPAD_RIGHT -> FocusNavigator.RIGHT;
          case KeyEvent.KEYCODE_DPAD_UP -> FocusNavigator.UP;
          case KeyEvent.KEYCODE_DPAD_DOWN -> FocusNavigator.DOWN;
          default -> -1;
        };
    if (direction < 0) return super.dispatchKeyEvent(event);
    if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
    View current = getCurrentFocus();
    Map<String, View> active = overlay == null ? controls : modalControls;
    if (current == null || !(current.getTag() instanceof String)) {
      if (!active.isEmpty()) active.values().iterator().next().requestFocus();
      return true;
    }
    List<FocusNavigator.Node> nodes = new ArrayList<>();
    for (Map.Entry<String, View> entry : active.entrySet()) {
      View v = entry.getValue();
      if (!v.isShown()) continue;
      int[] at = new int[2];
      v.getLocationOnScreen(at);
      nodes.add(
          new FocusNavigator.Node(
              entry.getKey(),
              at[0],
              at[1],
              at[0] + v.getWidth() * stage.getScaleX(),
              at[1] + v.getHeight() * stage.getScaleY()));
    }
    String key = (String) current.getTag(), next = FocusNavigator.next(nodes, key, direction);
    if (overlay != null
        && reader != null
        && (direction == FocusNavigator.UP || direction == FocusNavigator.DOWN)) {
      int sign = direction == FocusNavigator.UP ? -1 : 1;
      // Long source text can be scrolled even when the close button is the only control.
      if (key.equals("modal:close") && reader.canScrollVertically(sign)) {
        reader.smoothScrollBy(0, sign * dp(150));
        return true;
      }
    }
    if (overlay == null && FocusNavigator.appBoundary(key, next, direction)) {
      int nextPage = appPage + (direction == FocusNavigator.LEFT ? 1 : -1);
      if (nextPage >= 0 && nextPage * 8 < apps.size()) {
        appPage = nextPage;
        renderApps();
        int offset = direction == FocusNavigator.LEFT ? 0 : 7;
        savedFocus = "app:" + apps.get(Math.min(appPage * 8 + offset, apps.size() - 1)).key;
      }
      return true;
    }
    View target = active.get(next);
    if (target != null) {
      target.requestFocus();
      if (overlay != null && target != current)
        target.post(
            () -> {
              android.graphics.Rect rect = new android.graphics.Rect();
              target.getDrawingRect(rect);
              target.requestRectangleOnScreen(rect, false);
            });
    }
    return true;
  }
}
