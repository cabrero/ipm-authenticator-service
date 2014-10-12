package es.udc.fic.ipm;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.preference.PreferenceManager;


public class IPMAuthenticator extends AbstractAccountAuthenticator {

    public static final String ACCOUNT_TYPE = "es.udc.fic.ipm.authenticator";
    public static final String ACCOUNT_NAME = "IPM Account";
    public static final String AUTH_TOKEN_TYPE = "Movie Database Token";
    public static final String ACCOUNT_NAME_DEVELOPMENT_SERVER_SUFFIX = "@development";
    public static final String ACCOUNT_NAME_PRODUCTION_SERVER_SUFFIX = "@production";
    public static final String HTTP_USER_AGENT = "IPM-HttpClient/UNAVAILABLE";

    private static final String TAG = IPMAccountAuthenticatorService.class.getSimpleName();
    private final String CLASSNAME = this.getClass().getSimpleName();

    private final Context _context;


    public IPMAuthenticator(Context context) {
        super(context);
        _context = context;
        PreferenceManager.setDefaultValues(_context, R.xml.authenticator_prefs, false);
    }


    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
                             String accountType,
                             String authTokenType,
                             String[] requiredFeatures,
                             Bundle options)
            throws NetworkErrorException {

        Log.d(TAG, CLASSNAME + ".addAccount()");
        if (authTokenType == null) {
            authTokenType = AUTH_TOKEN_TYPE;
        }

        Intent intent = new Intent(_context, IPMAuthenticatorActivity.class);
        intent.putExtra(IPMAuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(IPMAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(IPMAuthenticatorActivity.ARG_IS_ADDING_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;

    }



    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
                               Account account,
                               String authTokenType,
                               Bundle options)
            throws NetworkErrorException {

        Log.d(TAG, CLASSNAME + ".getAuthToken");

        AccountManager am = AccountManager.get(_context);
        String authToken = am.peekAuthToken(account, authTokenType);
        Log.d(TAG, CLASSNAME + ".getAuthToken: peekAuthToken = " + authToken);

        // @TRADE-OFF Si el token ha expirado
        //            a) Volvemos a pedir la clave. Inconveniente para el usuario
        //            b) Usamos la clave que habíamos guardado. Menos seguro
        if (TextUtils.isEmpty(authToken)) {
            String password = am.getPassword(account);
            if (password != null) {
                Log.d(TAG, CLASSNAME + ".getAuthToken: re-authenticating with the existing password");
                try {
                    MovieDatabaseAuthClient client = new MovieDatabaseAuthClient(PreferenceManager.getDefaultSharedPreferences(IPMAuthenticator.this._context));
                    authToken = client.userLogIn(account.name,
                            password,
                            authTokenType);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (!TextUtils.isEmpty(authToken)) {
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }
        else {
            // Si lo anterior falla, volvemos a pedir las credenciales
            Intent intent = new Intent(_context, IPMAuthenticatorActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            intent.putExtra(IPMAuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type);
            intent.putExtra(IPMAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
            intent.putExtra(IPMAuthenticatorActivity.ARG_ACCOUNT_NAME, account.name);
            Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }
    }


    @Override
    public String getAuthTokenLabel(String authTokenType) {

        if (AUTH_TOKEN_TYPE.equals(authTokenType)) {
            return AUTH_TOKEN_TYPE;
        }
        else {
            return "IPM Unknown Token Type";
        }
    }



    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
                              Account account,
                              String[] features)
            throws NetworkErrorException {

        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }


    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response,
                                 String accountType) {
        throw new UnsupportedOperationException();
    }


    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                                     Account account,
                                     Bundle options)
            throws NetworkErrorException {
        return null;
    }


    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                    Account account,
                                    String authTokenType,
                                    Bundle options)
            throws NetworkErrorException {

        throw new UnsupportedOperationException();
    }


}
