package es.udc.fic.ipm;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Spinner;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;


public class IPMAuthenticatorActivity extends AccountAuthenticatorActivity {

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_ACCOUNT = "IS_ADDING_ACCOUNT";

    public final static String KEY_ERROR_MESSAGE = "ERR_MSG";

    public final static String PARAM_USER_PASS = "USER_PASS";

    private final int REQ_SIGNUP = 1;

    private static final String TAG = IPMAccountAuthenticatorService.class.getSimpleName();
    private final String CLASSNAME = this.getClass().getSimpleName();

    private AccountManager _accountManager;
    private String _authTokenType;

    private String[] _serverTypes = {IPMAuthenticator.ACCOUNT_NAME_DEVELOPMENT_SERVER_SUFFIX,
            IPMAuthenticator.ACCOUNT_NAME_PRODUCTION_SERVER_SUFFIX};
    private String _serverType = _serverTypes[0];



    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        Log.d(TAG, CLASSNAME + "onCreate()");
        setContentView(R.layout.activity_login);

        _accountManager = AccountManager.get(getBaseContext());

        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
        _authTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);

        Spinner spinner = (Spinner) findViewById(R.id.serverSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, _serverTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                _serverType = _serverTypes[pos];
            }

            @Override
            public void onNothingSelected(AdapterView<?> view) {
            }
        });

        if (accountName != null) {
            ((TextView)findViewById(R.id.accountName)).setText(accountName);
        }

        findViewById(R.id.logIn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logIn();
            }
        });
    }

    private void logIn() {
        final String userName = ((TextView)findViewById(R.id.accountName)).getText().toString().trim()+_serverType;
        final String userPass = ((TextView)findViewById(R.id.accountPassword)).getText().toString();

        final String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);

        new AsyncTask<String, Void, Intent>() {

            @Override
            protected Intent doInBackground(String... params) {
                Log.d(TAG, CLASSNAME + ".logIn(): Started authenticating");

                String authToken = null;
                Bundle data = new Bundle();
                try {
                    MovieDatabaseAuthClient client = new MovieDatabaseAuthClient(PreferenceManager.getDefaultSharedPreferences(IPMAuthenticatorActivity.this));

                    authToken = client.userLogIn(userName, userPass, _authTokenType);
                    Log.d(TAG, "Got " + authToken);

                    data.putString(AccountManager.KEY_ACCOUNT_NAME, userName);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                    data.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                    data.putString(PARAM_USER_PASS, userPass);
                }
                catch (Exception e) {
                    data.putString(KEY_ERROR_MESSAGE, e.getMessage());
                }
                Intent result = new Intent();
                result.putExtras(data);
                return result;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                    String msg = intent.getStringExtra(KEY_ERROR_MESSAGE);
                    Log.d(TAG, CLASSNAME + " login error:" + msg);
                    AlertDialog.Builder  builder = new AlertDialog.Builder(IPMAuthenticatorActivity.this);
                    builder.setMessage(msg).setTitle(R.string.dialog_server_error_title);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    builder.create().show();
                }
                else {
                    finishLogin(intent);
                }
            }

        }.execute();
    }


    private void finishLogin(Intent intent) {
        Log.d(TAG, CLASSNAME + ".finishLogin");

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_ACCOUNT, false)) {
            Log.d(TAG, CLASSNAME + ".finishLogin(): addAccountExplicitly");
            String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
            String authtokenType = _authTokenType;

            _accountManager.addAccountExplicitly(account, accountPassword, null);
            _accountManager.setAuthToken(account, authtokenType, authtoken);
        }
        else {
            Log.d(TAG, CLASSNAME + ".finishLogin(): setPassword");
            _accountManager.setPassword(account, accountPassword);
        }

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        AlertDialog.Builder  builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_account_created_msg)
                .setTitle(R.string.dialog_account_created_title);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

}
