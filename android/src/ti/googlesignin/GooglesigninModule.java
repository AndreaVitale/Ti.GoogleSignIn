/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package ti.googlesignin;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiActivityResultHandler;
import org.appcelerator.titanium.util.TiActivitySupport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

@Kroll.module(name = "Googlesignin", id = "ti.googlesignin")
public class GooglesigninModule extends KrollModule implements
		ConnectionCallbacks, OnConnectionFailedListener {
	GoogleApiClient googleApiClient;
	// Standard Debugging variables
	public static final String LCAT = "GSignin";
	private static final boolean DBG = TiConfig.LOGD;
	private static int RC_SIGN_IN = 34;
	private Context ctx;
	private String packageName;
	private String serverClientId;
	private KrollFunction onLogin;

	// You can define constants with @Kroll.constant, for example:
	// @Kroll.constant
	// public static final int DEFAULT_SIGN_IN =
	// GoogleSignInOptions.DEFAULT_SIGN_IN;
	public GooglesigninModule() {
		super();
		ctx = TiApplication.getInstance().getApplicationContext();
		packageName = TiApplication.getAppCurrentActivity().getPackageName();
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app) {
		Log.d(LCAT, "inside onAppCreate");
		// put module init code that needs to run when the application is
		// created
	}

	@Override
	public void onStart(Activity activity) {
		Log.d(LCAT, "[MODULE LIFECYCLE EVENT] start");
		if (googleApiClient != null)
			googleApiClient.connect();
		super.onStart(activity);
	}

	@Override
	public void onStop(Activity activity) {
		Log.d(LCAT, "[MODULE LIFECYCLE EVENT] stop");
		// if (googleApiClient != null)
		// googleApiClient.disconnect();
		super.onStop(activity);
	}

	@Kroll.method
	protected synchronized void initialize(KrollDict opts) {
		Log.d(LCAT, "try to initialize the client");
		if (opts.containsKeyAndNotNull("onLogin")) {
			Object o = opts.get("onLogin");
			if (o instanceof KrollFunction) {
				onLogin = (KrollFunction) o;
			}
		}
		if (opts.containsKeyAndNotNull("clientID")) {
			serverClientId = opts.getString("clientID");
			Log.d(LCAT, serverClientId + " read");
		} else {
			Log.d(LCAT, "no clientID found!");
			return;
		}

		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
				GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(serverClientId).requestProfile().requestEmail()
				.build();
		Log.d(LCAT, gso.toString());
		Log.d(LCAT, "gso built, try to build googleApiClient");
		googleApiClient = new GoogleApiClient.Builder(ctx)
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).build();
		Log.d(LCAT, "googleApiClient built, finished initialized");
	}

	@Kroll.method
	protected synchronized void signIn() {
		Log.d(LCAT, "signIn with " + googleApiClient.toString());
		// Building of intent
		final Intent signInIntent = Auth.GoogleSignInApi
				.getSignInIntent(googleApiClient);
		Log.d(LCAT, "signInIntent built");
		// building new activity with result handler
		final TiActivitySupport activitySupport = (TiActivitySupport) TiApplication
				.getInstance().getCurrentActivity();
		Log.d(LCAT, "TiActivitySupport built");
		if (TiApplication.isUIThread()) {
			activitySupport.launchActivityForResult(signInIntent, RC_SIGN_IN,
					new SignInResultHandler());
		} else {
			TiMessenger.postOnMain(new Runnable() {
				@Override
				public void run() {
					activitySupport.launchActivityForResult(signInIntent,
							RC_SIGN_IN, new SignInResultHandler());
				}
			});
		}
	}

	@Kroll.method
	protected synchronized void signOut() {
		if (googleApiClient == null)
			return;
		Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(
				new ResultCallback<Status>() {
					@Override
					public void onResult(Status status) {
						Log.d(LCAT, "oResult SignOut");
						KrollDict kd = new KrollDict();
						kd.put("status", status.getStatusCode());
						if (hasListeners("onsignout")) {
							Log.e(LCAT,
									"The 'onsignout' event is deprecated, use 'disconnect' instead.");
							fireEvent("onsignout", kd);
						}

						if (hasListeners("disconnect")) {
							fireEvent("disconnect", kd);
						}
					}
				});

	}

	private final class SignInResultHandler implements TiActivityResultHandler {
		public void onError(Activity arg0, int arg1, Exception e) {
			Log.e(LCAT, e.getMessage());
		}

		public void onResult(Activity dummy, int requestCode, int resultCode,
				Intent data) {
			Log.d(LCAT, "onResult");
			if (requestCode == RC_SIGN_IN) {
				GoogleSignInResult result = Auth.GoogleSignInApi
						.getSignInResultFromIntent(data);
				KrollDict kd = new KrollDict();
				if (result.isSuccess()) {
					Log.d(LCAT, "Success");
					GoogleSignInAccount acct = result.getSignInAccount();
					Log.d(LCAT, acct.getDisplayName());
					Log.d(LCAT, "Login Success");

					kd.put("fullName", acct.getDisplayName());
					kd.put("email", acct.getEmail());
					kd.put("photo", acct.getPhotoUrl().toString());
					kd.put("displayName", acct.getDisplayName());
					kd.put("familyName", acct.getFamilyName());
					kd.put("givenName", acct.getGivenName());
					kd.put("accountName", acct.getAccount().name);
					kd.put("token", acct.getIdToken());

					if (hasListeners("onsuccess")) {
						Log.e(LCAT,
								"The 'onsuccess' event is deprecated, use 'login' instead.");
						fireEvent("onsuccess", kd);
					}
					if (hasListeners("login")) {
						fireEvent("login", kd);
					}
					if (onLogin != null)
						onLogin.call(getKrollObject(), kd);
				} else {
					kd.put("status", result.getStatus().getStatusCode());
					kd.put("message", result.getStatus().getStatusMessage());

					kd.put("success", false);
					if (hasListeners("onerror")) {
						Log.e(LCAT,
								"The 'onerror' event is deprecated, use 'error' instead.");
						fireEvent("onerror", kd);
					}
					if (hasListeners("error")) {
						fireEvent("error", kd);
					}
				}
			}
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.e(LCAT, "onConnectionFailed");
		if (hasListeners("error")) {
			KrollDict kd = new KrollDict();
			kd.put("error", result.getErrorMessage());
			kd.put("code", result.getErrorCode());
			fireEvent("error", kd);
		}

	}

	/*
	 * After calling connect(), this method will be invoked asynchronously when
	 * the connect request has successfully completed. After this callback, the
	 * application can make requests on other methods provided by the client and
	 * expect that no user intervention is required to call methods that use
	 * account and scopes provided to the client constructor.
	 * 
	 * Note that the contents of the connectionHint Bundle are defined by the
	 * specific services. Please see the documentation of the specific
	 * implementation of GoogleApiClient you are using for more information.
	 */
	@Override
	public void onConnected(Bundle bundle) {
		KrollDict kd = new KrollDict();
		kd.put("result", bundle.toString());
		if (hasListeners("connect"))
			fireEvent("connect", kd);
		Log.d(LCAT, "onConnected");
	}

	/*
	 * Called when the client is temporarily in a disconnected state. This can
	 * happen if there is a problem with the remote service (e.g. a crash or
	 * resource problem causes it to be killed by the system). When called, all
	 * requests have been canceled and no outstanding listeners will be
	 * executed. GoogleApiClient will automatically attempt to restore the
	 * connection. Applications should disable UI components that require the
	 * service, and wait for a call to onConnected(Bundle) to re-enable
	 */

	@Override
	public void onConnectionSuspended(int result) {
		KrollDict kd = new KrollDict();
		Log.d(LCAT, "onConnectionSuspended");
	}

	/*
	 * private String loadJSONFromAsset(String jsonfile) { String jsonString =
	 * null; try { InputStream inStream = TiFileFactory.createTitaniumFile( new
	 * String[] { resolveUrl(null, jsonfile) }, false) .getInputStream(); byte[]
	 * buffer = new byte[inStream.available()]; inStream.read(buffer);
	 * inStream.close(); jsonString = new String(buffer, "UTF-8"); } catch
	 * (IOException ex) { ex.printStackTrace(); return null; } return
	 * jsonString; }
	 */
}
