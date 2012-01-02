package ch.fixme.cowsay;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {
    private Cow cow;
    private EditText messageView;
    private TextView outputView;
    private static final String TAG = "Main";

    // Menu
    public static final int MENU_SHARE_TEXT = Menu.FIRST;
    public static final int MENU_COPY = Menu.FIRST + 1;
    public static final int MENU_ABOUT = Menu.FIRST + 2;

    // public static final int MENU_SHARE_IMAGE = Menu.FIRST + 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        // Think button
        final Button togglebutton = (Button) findViewById(R.id.think_toggle);
        togglebutton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                cow.think = (cow.think == 1) ? 0 : 1;
                cow.constructFace(cow.face);
                cowRefresh();
                if (cow.think == 1) {
                    togglebutton.setText(R.string.think_on);
                } else {
                    togglebutton.setText(R.string.think_off);
                }
            }
        });

        // Initialize objects and ui access
        cow = new Cow(getApplicationContext());
        outputView = (TextView) findViewById(R.id.thecow);
        messageView = (EditText) findViewById(R.id.message);
        populateCowTypes();
        populateCowFaces();

        // Bidirectionnal scrollview
        // FIXME: Vertical scrolling doesn't work on 1.6 but works on 1.5
        outputView.setMovementMethod(ScrollingMovementMethod.getInstance());
        ((WScrollView) findViewById(R.id.wsv)).sv = outputView;

        // Real time update
        TextWatcher myTextWatcher = new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cowRefresh();
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
        messageView.addTextChangedListener(myTextWatcher);
    }

    /* Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SHARE_TEXT, 0, R.string.menu_share_text).setIcon(
                android.R.drawable.ic_menu_share);
        menu.add(0, MENU_COPY, 0, R.string.menu_copy).setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_ABOUT, 0, R.string.about_title).setIcon(
                android.R.drawable.ic_menu_info_details);
        // menu.add(0, MENU_SHARE_IMAGE, 0, "Share as image");
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        switch (item.getItemId()) {
            case MENU_SHARE_TEXT:
                // TODO: Doesn't work on facebook
                // http://bugs.developers.facebook.net/show_bug.cgi?id=16728
                Log.d(TAG, "Share as text");
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name);
                intent.putExtra(Intent.EXTRA_TEXT, Cow.LF + cow.getFinalCow());
                startActivity(Intent.createChooser(intent, getString(R.string.share_chooser)));
                break;
            case MENU_COPY:
                ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(cow.getFinalCow());
                Toast.makeText(this, R.string.toast_copy, Toast.LENGTH_SHORT).show();
                break;
            case MENU_ABOUT:
                showDialog(MENU_ABOUT);
                break;
            // case MENU_SHARE_IMAGE:
            // Log.d(TAG, "Share as image");
            // View thecow = findViewById(R.id.thecow);
            // Bitmap screenshot = Bitmap.createBitmap(thecow.getWidth(),
            // thecow.getHeight(),
            // Bitmap.Config.ARGB_8888);
            // thecow.draw(new Canvas(screenshot));
            // String path = Images.Media.insertImage(getContentResolver(),
            // screenshot, "title",
            // null);
            // Uri screenshotUri = Uri.parse(path);
            // final Intent emailIntent = new
            // Intent(android.content.Intent.ACTION_SEND);
            // emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // emailIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
            // emailIntent.setType("image/png");
            // startActivity(Intent.createChooser(emailIntent,
            // "Send email using"));
            // break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
    }

    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case MENU_ABOUT:
                // TODO: Make the link clickable with setMovementMethod()
                final SpannableString msg = new SpannableString(getString(R.string.about_message));
                Linkify.addLinks(msg, Linkify.WEB_URLS);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.about_title).setMessage(msg)
                        .setNeutralButton(R.string.btn_close, null);
                dialog = builder.create();
                break;
        }
        return dialog;
    }

    private void populateCowTypes() {
        // Populate the cow type Spinner widget
        final String[] items = cow.getCowTypes();
        final Spinner spinner = (Spinner) findViewById(R.id.type);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(adapter);
        spinner.setSelection(11);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View v, int position, long id) {
                cow.style = items[position];
                cow.getCowFile();
                cowRefresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void populateCowFaces() {
        // Populate the cow face Spinner widget
        Spinner s = (Spinner) findViewById(R.id.face);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.faces,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        s.setAdapter(adapter);
        s.setSelection(0);
        s.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View v, int position, long id) {
                cow.constructFace(position);
                cowRefresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    // FIXME: find a fix so it's not fired 2 times when launching the app...
    private void cowRefresh() {
        final String msg = messageView.getText().toString();
        if (msg.length() > 0) {
            cow.message = msg;
        }
        outputView.setText(cow.getFinalCow());
    }
}
