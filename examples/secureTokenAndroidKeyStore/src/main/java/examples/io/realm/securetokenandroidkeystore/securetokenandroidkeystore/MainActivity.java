/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package examples.io.realm.securetokenandroidkeystore.securetokenandroidkeystore;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.example.securetokenandroidkeystore.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStoreException;
import java.util.UUID;

import io.realm.Realm;
import io.realm.SyncConfiguration;
import io.realm.SyncManager;
import io.realm.User;
import io.realm.android.SecureUserStore;
import io.realm.internal.android.crypto.CipherClient;
import io.realm.internal.objectserver.SyncUser;
import io.realm.internal.objectserver.Token;

/**
 * Activity responsible of unlocking the KeyStore
 * before using the {@link io.realm.android.SecureUserStore} to encrypt
 * the Token we get from the session
 */
public class MainActivity extends AppCompatActivity {
    private CipherClient cryptoClient;
    private TextView txtKeystoreState;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtKeystoreState = (TextView) findViewById(R.id.txtLabelKeyStore);

        try {
            cryptoClient = new CipherClient(this);
            if (cryptoClient.isKeystoreUnlocked()) {
                buildSyncConf();
                keystoreUnlockedMessage();
            } else {
                cryptoClient.unlockKeystore();
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // We return to the app after the KeyStore is unlocked or not.
            if (cryptoClient.isKeystoreUnlocked()) {
                buildSyncConf();
                keystoreUnlockedMessage ();
            } else {
                keystoreLockedMessage ();
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    // build SyncConfiguration with a user store to store encrypted Token.
    private void buildSyncConf () {
        try {
            SyncManager.setUserStore(new SecureUserStore(MainActivity.this));
            // the rest of Sync logic ...
            User user = createTestUser(0);
            String url = "realm://objectserver.realm.io/default";
            SyncConfiguration secureConfig = new SyncConfiguration.Builder(user, url).build();
            Realm realm = Realm.getInstance(secureConfig);
            // ... 

        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }
    // Helpers
    private final static String USER_TOKEN = UUID.randomUUID().toString();
    private final static String REALM_TOKEN = UUID.randomUUID().toString();

    private static User createTestUser(long expires) {
        Token userToken = new Token(USER_TOKEN, "JohnDoe", null, expires, null);
        Token accessToken = new Token(REALM_TOKEN, "JohnDoe", "/foo", expires, new Token.Permission[] {Token.Permission.DOWNLOAD });
        SyncUser.AccessDescription desc = new SyncUser.AccessDescription(accessToken, "/data/data/myapp/files/default", false);

        JSONObject obj = new JSONObject();
        try {
            JSONArray realmList = new JSONArray();
            JSONObject realmDesc = new JSONObject();
            realmDesc.put("uri", "realm://objectserver.realm.io/default");
            realmDesc.put("description", desc.toJson());
            realmList.put(realmDesc);

            obj.put("authUrl", "http://objectserver.realm.io/auth");
            obj.put("userToken", userToken.toJson());
            obj.put("realms", realmList);
            return User.fromJson(obj.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void keystoreLockedMessage () {
        txtKeystoreState.setBackgroundColor(ContextCompat.getColor(this, R.color.colorLocked));
        txtKeystoreState.setText(R.string.locked_text);
    }

    private void keystoreUnlockedMessage () {
        txtKeystoreState.setBackgroundColor(ContextCompat.getColor(this, R.color.colorActivated));
        txtKeystoreState.setText(R.string.unlocked_text);
    }
}

