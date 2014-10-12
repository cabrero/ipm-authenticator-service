package es.udc.fic.ipm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class IPMAccountAuthenticatorService extends Service {

    private IPMAuthenticator _authenticator;

    @Override
    public void onCreate() {
	_authenticator = new IPMAuthenticator(this);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return _authenticator.getIBinder();
    }

}
