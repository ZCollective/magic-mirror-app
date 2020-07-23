package org.zcollective.mirrorconfiger.qrscanner;

import android.Manifest;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.google.zxing.BarcodeFormat;
import com.petarmarijanovic.rxactivityresult.ActivityResult;
import com.petarmarijanovic.rxactivityresult.RxActivityResult;
import com.vanniktech.rxpermission.Permission;
import com.vanniktech.rxpermission.RealRxPermission;
import com.vanniktech.rxpermission.RxPermission;

import org.zcollective.mirrorconfiger.BuildConfig;
import org.zcollective.mirrorconfiger.R;
import org.zcollective.mirrorconfiger.qrscanner.dialogs.EnableWifiDialog;
import org.zcollective.mirrorconfiger.qrscanner.dialogs.EnableWifiDialog.WifiDialogResult;
import org.zcollective.mirrorconfiger.qrscanner.dialogs.GrantPermissionsDialog;
import org.zcollective.mirrorconfiger.qrscanner.dialogs.GrantPermissionsDialog.GrantPermissionsDialogResult;
import org.zcollective.mirrorconfiger.qrscanner.dialogs.ProceedToConnectDialog;
import org.zcollective.mirrorconfiger.qrscanner.dialogs.ProceedToConnectDialog.ProceedToConnectDialogResult;
import org.zcollective.mirrorconfiger.util.wifi.parser.WifiScheme;
import org.zcollective.mirrorconfiger.wifi.NetworkStateTracker;
import org.zcollective.mirrorconfiger.wifi.NetworkStateTracker.NetworkState;
import org.zcollective.mirrorconfiger.qrscanner.dialogs.WifiDialogHelper;
import org.zcollective.mirrorconfiger.qrscanner.dialogs.WifiDialogHelper.ConnectWifiDialogCallback;
import org.zcollective.mirrorconfiger.webconfig.WebConfigActivity;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.vanniktech.rxpermission.Permission.State.GRANTED;
import static org.zcollective.mirrorconfiger.qrscanner.dialogs.EnableWifiDialog.WifiDialogResult.PROCEED_ENABLE_WIFI;
import static org.zcollective.mirrorconfiger.qrscanner.dialogs.GrantPermissionsDialog.GrantPermissionsDialogResult.ALLOW_PERMISSION;
import static org.zcollective.mirrorconfiger.qrscanner.dialogs.ProceedToConnectDialog.ProceedToConnectDialogResult.START_CONNECT;

public class QrScannerActivity extends AppCompatActivity implements ConnectWifiDialogCallback {

    // TODO: manage states correctly

    @Override
    public void onProceedConnectingToWifi() {
        String[] wifiResult = result.split(";");
        String password = "";
        for (String asdf : wifiResult) {
            if (asdf.startsWith("P:")) {
                password = asdf.substring(2);
            }

            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(';');
            splitter.setString(result);
        }

        Timber.tag(TAG).wtf("Password: %s", password);

        checkWifiAvailability()
                .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> {
                    Timber.tag(TAG).wtf(throwable, "CheckWifiAvailability::onErrorResumeNext()");
                    return showEnableWifiDialog().andThen(startWifiSettings());
                 })
                .andThen(connectToWifiNetwork("Mirror", password))
                .subscribeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(
                            io.reactivex.rxjava3.disposables.@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        Timber.tag(TAG).d("Starting WIFI Connect");
                        compositeDisposable.add(d);
                        wifiConnect = d;
                    }

                    @Override
                    public void onComplete() {
                        Timber.tag(TAG).d("Completing WIFI Connected");
                        wifiConnect.dispose();
                        wifiConnect = null;
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Timber.tag(TAG).w(e);
                        wifiConnect.dispose();
                        wifiConnect = null;
                        finish();
                    }
                });
    }

    @Override
    public void onStopConnectingToWifi() {
        onSupportNavigateUp();
        wifiDialogHelper.dismissProceedToConnectDialog();
        finish();
    }

    private static final String TAG = "QrScanner";
    private static final String QR_CHAIN_TAG = "QrScannerInit";
    private static final String NET_CHAIN_TAG = "NetStateObserver";
    private static final String NET_ID_CHAIN_TAG = "NetIdChecker";
    private static final String HOST_CONNECTION_CHAIN_TAG = "HostConnectChain";

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private volatile boolean retrying = false;
    private volatile int netId;

    private ProgressBar bar;
    private String result;

    private boolean isConnecting;

    // Helper-Dialogs
    private GrantPermissionsDialog grantPermissionsDialog;
    private ProceedToConnectDialog proceedToConnectDialog;
    private EnableWifiDialog enableWifiDialog;
    private WifiDialogHelper wifiDialogHelper;

    // System-Services
    private ConnectivityManager connMgr;
    private WifiManager wifiManager;

    // Rx-Helpers
    private RxActivityResult rxActivityResult;
    private RxPermission rxPermission;

    // QR-Code-Scanner
    private CodeScannerView scannerView;
    private CodeScanner codeScanner;

    // Disposables
    private Disposable networkCheck;
    private Disposable qrScannerInit;
    private Disposable connectToHost;
    private Disposable wifiConnect;
    private io.reactivex.disposables.Disposable permDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("Running QrScannerActivityNew::onCreate()");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_qr_scanner);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        bar = findViewById(R.id.webconfig_loading);
        scannerView = findViewById(R.id.scanner_view);

        connMgr = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        networkCheck = startNetworkListener();
        rxActivityResult = new RxActivityResult(this);
        rxPermission = RealRxPermission.getInstance(getApplicationContext());

        // Fragments
        initFragments();

//        startQrScanning();
        initQrScanningChain();
//                .subscribeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
//                .subscribe(new CompletableObserver() {
//                    @Override
//                    public void onSubscribe(
//                            io.reactivex.rxjava3.disposables.@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
//                        Timber.tag(TAG).d("Starting WIFI Connect");
//                        compositeDisposable.add(d);
////                        wifiConnect = d;
//                    }
//
//                    @Override
//                    public void onComplete() {
//                        Timber.tag(TAG).d("Completing WIFI Connected");
////                        wifiConnect.dispose();
////                        wifiConnect = null;
//                    }
//
//                    @Override
//                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
//                        Timber.tag(TAG).w(e);
////                        wifiConnect.dispose();
////                        wifiConnect = null;
//                        finish();
//                    }
//                });
        Timber.d("Finished QrScannerActivityNew::onCreate()");
    }

    private void initFragments() {
        Timber.d("Running QrScannerActivityNew::initFragments()");

        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();

        wifiDialogHelper =
                (WifiDialogHelper) getSupportFragmentManager().findFragmentByTag(WifiDialogHelper.TAG);

        if (wifiDialogHelper == null) {
            wifiDialogHelper = new WifiDialogHelper();
            fragTransaction.add(wifiDialogHelper, WifiDialogHelper.TAG);
        }

        enableWifiDialog =
                (EnableWifiDialog) getSupportFragmentManager().findFragmentByTag(EnableWifiDialog.TAG);

        if (enableWifiDialog == null) {
            enableWifiDialog = new EnableWifiDialog();
            fragTransaction.add(enableWifiDialog, EnableWifiDialog.TAG);
        }

        grantPermissionsDialog =
                (GrantPermissionsDialog) getSupportFragmentManager().findFragmentByTag(GrantPermissionsDialog.TAG);

        if (grantPermissionsDialog == null) {
            grantPermissionsDialog = new GrantPermissionsDialog();
            fragTransaction.add(grantPermissionsDialog, GrantPermissionsDialog.TAG);
        }

        proceedToConnectDialog =
                (ProceedToConnectDialog) getSupportFragmentManager().findFragmentByTag(ProceedToConnectDialog.TAG);

        if (proceedToConnectDialog == null) {
            proceedToConnectDialog = new ProceedToConnectDialog();
            fragTransaction.add(proceedToConnectDialog, ProceedToConnectDialog.TAG);
        }

        fragTransaction.commitNow();

        Timber.d("Finished QrScannerActivityNew::initFragments()");
    }

    private Completable requestCameraPermission() {
        return Completable.create(emitter ->
            rxPermission
                    .request(Manifest.permission.CAMERA)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(new io.reactivex.SingleObserver<Permission>() {
//                        private Disposable permDisposable = null;

                        @Override
                        public void onSubscribe(io.reactivex.disposables.Disposable d) {
                            Timber.tag(TAG).i("RxActivityResult::onSubscribe()");
                            permDisposable = d;
                        }

                        @Override
                        public void onSuccess(Permission permission) {
                            Timber.tag(TAG).i("RxPermission::onSuccess(): %s", permission.state().name());

                            if (permission.state() == GRANTED) {
                                emitter.onComplete();
                            } else {
                                emitter.tryOnError(new IllegalStateException());
                            }

                            permDisposable.dispose();
                            permDisposable = null;
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Timber.tag(TAG).e(throwable, "RxActivityResult::onError()");
                            emitter.tryOnError(throwable);
                            permDisposable.dispose();
                            permDisposable = null;
                        }
                    })
        );
    }

    private Completable initQrScannerObject() {
        return Completable.create(emitter -> {
            codeScanner = new CodeScanner(this, scannerView);
            codeScanner.setAutoFocusInterval(200);
            codeScanner.setFormats(Collections.singletonList(BarcodeFormat.QR_CODE));
            codeScanner.setDecodeCallback(result -> { // TODO: rx mapping, filtering, ...
                String text = result.getText();
                Timber.tag(TAG).wtf(text);

                runOnUiThread(() -> {
                    // TODO: check if text from qr-code is correct
                    Toast.makeText(QrScannerActivity.this, text, Toast.LENGTH_SHORT)
                         .show();
                    QrScannerActivity.this.result = text;
                    wifiDialogHelper.showProceedToConnectDialog();
                });
            });
            emitter.onComplete();
        });
    }

//    private Observable<String> initQrScanningNew() {
//    private Completable initQrScanningChain() {
    private void initQrScanningChain() {
        if (codeScanner == null) {
            codeScanner = new CodeScanner(this, scannerView);
            codeScanner.setAutoFocusInterval(200);
            codeScanner.setFormats(Collections.singletonList(BarcodeFormat.QR_CODE));
        } else {
            codeScanner.startPreview();
        }

        Observable.<String>create(emitter -> codeScanner.setDecodeCallback(code -> {
            String codeString = code.getText();
            Timber.tag(TAG).wtf("Read QR-Code: '%s'", codeString);
            emitter.onNext(codeString);
        })).map(WifiScheme::parse)
                .filter(WifiScheme::isMirrorConfiguration)
                .map(WifiScheme::generateWifiConfiguration)
//           .concatMapCompletable(wifiConfiguration -> Completable.defer(() -> connectToWifiNetwork(wifiConfiguration)));
                //TODO: experimental idea
                .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .flatMapCompletable(wifiConfiguration -> {
                    Timber.tag(TAG).wtf(wifiConfiguration.toString());

                    return showProceedToConnectDialog()
                            .doOnError(throwable -> {
                                Timber.tag(TAG).wtf(throwable, "ShowProceedToConnectDialog::doOnError()");
                                wifiDialogHelper.dismissProceedToConnectDialog();
                                onSupportNavigateUp();
                            })
                            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                            .andThen(checkWifiAvailability()
                                    .onErrorResumeNext(throwable -> {
                                        Timber.tag(TAG).wtf(throwable, "CheckWifiAvailability::onErrorResumeNext()");
                                        return showEnableWifiDialog().andThen(startWifiSettings());
                                    }))
                            .andThen(connectToWifiNetwork(wifiConfiguration));
                })
                .subscribeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(
                            io.reactivex.rxjava3.disposables.@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        Timber.tag(TAG).d("Starting WIFI Connect");
                        compositeDisposable.add(d);
//                        wifiConnect = d;
                    }

                    @Override
                    public void onComplete() {
                        Timber.tag(TAG).d("Completing WIFI Connected");
//                        wifiConnect.dispose();
//                        wifiConnect = null;
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Timber.tag(TAG).w(e);
//                        wifiConnect.dispose();
//                        wifiConnect = null;
                        finish();
                    }
                });
            //).subscribeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread());
//           .subscribe(new CompletableObserver() {
//               @Override
//               public void onSubscribe(
//                       io.reactivex.rxjava3.disposables.@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
//                   Timber.tag(TAG).d("Starting WIFI Connect");
//                   compositeDisposable.add(d);
//                   wifiConnect = d;
//               }
//
//               @Override
//               public void onComplete() {
//                   Timber.tag(TAG).d("Completing WIFI Connected");
//                   wifiConnect.dispose();
//                   wifiConnect = null;
//               }
//
//               @Override
//               public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
//                   Timber.tag(TAG).w(e);
//                   wifiConnect.dispose();
//                   wifiConnect = null;
//                   finish();
//               }
//           });


//            mCodeScanner.setDecodeCallback(result -> { // TODO: rx mapping, filtering, ...
//                String text = result.getText();
//                Timber.tag(TAG).wtf(text);
//
//                runOnUiThread(() -> {
//                    // TODO: check if text from qr-code is correct
//                    Toast.makeText(QrScannerActivityNew.this, text, Toast.LENGTH_SHORT)
//                         .show();
//                    QrScannerActivityNew.this.result = text;
//                    wifiDialogHelper.showProceedToConnectDialog();
//                });
//            });
//        }).mapOptional(code -> {
//          .filter(code -> {
//            // WIFI:S:<SSID>;T:<WPA|WEP|>;P:<password>;;
//            // Taken from: https://github.com/zxing/zxing/wiki/Barcode-Contents#wi-fi-network-config-android-ios-11
//
//
////            return !code.isEmpty() && code.matches(".*");
////            return !code.isEmpty() && code.matches("WIFI:S:\"()\";T:WPA;P:();;");
////        })
//        .map(code -> {
//            WifiConfiguration wifiConfig = new WifiConfiguration();
//            wifiConfig.SSID = String.format("\"%s\"", ssid);
//            wifiConfig.preSharedKey = String.format("\"%s\"", pw);
//            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//            return wifiConfig;
//        }).concatMapCompletable(wifiConfiguration -> Completable.defer(() -> connectToWifiNetwork(wifiConfiguration)));
////          .subscribe(this::connectToWifiNetwork, throwable -> Timber.tag(TAG).w(throwable)); // TODO: don't do this regex
    }

    @NonNull
    private Completable connectToWifiNetwork(@NonNull WifiConfiguration wifiConfig) {
        return Completable.create(emitter -> {
            netId = wifiManager.addNetwork(wifiConfig);

            Timber.tag(TAG).wtf("NetId: %d | %s", netId, wifiConfig.SSID);

            if (netId == -1) {
                emitter.tryOnError(new IllegalStateException());
            } else if (isConnecting = wifiManager.disconnect()) {
                emitter.onComplete();
            } else {
                emitter.tryOnError(new IllegalStateException());
            }
        }).retry(5);

        // TODO: retry connecting
    }

    private Completable showProceedToConnectDialog() {
        return Completable.create(emitter ->
            proceedToConnectDialog
                    .show()
                    .subscribe(new SingleObserver<ProceedToConnectDialogResult>() {
                        @Override
                        public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull io.reactivex.rxjava3.disposables.Disposable d) {
                            Timber.tag(TAG).i("ProceedToConnectDialog::onSubscribe()");
                            compositeDisposable.add(d);
                        }

                        @Override
                        public void onSuccess(ProceedToConnectDialogResult dialogResult) {
                            Timber.tag(TAG).i("ProceedToConnectDialog::onSuccess()");

                            if (dialogResult == START_CONNECT) {
                                emitter.onComplete();
                            } else {
                                emitter.tryOnError(new IllegalStateException());
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Timber.tag(TAG).e(throwable, "ProceedToConnectDialog::onError()");
                            emitter.tryOnError(throwable);
                        }
        }));
    }

    private Completable showGrantPermissionsDialog() {
        return Completable.create(emitter ->
            grantPermissionsDialog
                    .show()
                    .subscribe(new SingleObserver<GrantPermissionsDialogResult>() {
                        @Override
                        public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull io.reactivex.rxjava3.disposables.Disposable d) {
                            Timber.tag(TAG).i("GrantPermissionsDialog::onSubscribe()");
                            compositeDisposable.add(d);
                        }

                        @Override
                        public void onSuccess(GrantPermissionsDialogResult dialogResult) {
                            Timber.tag(TAG).i("GrantPermissionsDialog::onSuccess()");

                            if (dialogResult == ALLOW_PERMISSION) {
                                emitter.onComplete();
                            } else {
                                emitter.tryOnError(new IllegalStateException());
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Timber.tag(TAG).e(throwable, "GrantPermissionsDialog::onError()");
                            emitter.tryOnError(throwable);
                        }
        }));
    }

    private void startQrScanning() {
        requestCameraPermission()
                .subscribeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> {
                    Timber.tag(QR_CHAIN_TAG).wtf(throwable, "RequestCameraPermission::onErrorResumeNext()");
                    return showGrantPermissionsDialog();
                })
                .andThen(initQrScannerObject())
                .andThen(Completable.fromRunnable(() -> codeScanner.startPreview()))
                .doOnSubscribe(d -> Timber.tag(QR_CHAIN_TAG).d("Starting QR-Scanning"))
//                .subscribe(() -> {
//                    Timber.tag(QR_CHAIN_TAG).d("Completing Initialization of QR-Scanning");
//                }, throwable -> {
//                    Timber.tag(QR_CHAIN_TAG).w(throwable);
//                    finish();
//                });
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(
                            io.reactivex.rxjava3.disposables.@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        Timber.tag(TAG).d("Starting QR-Scanning");
                        compositeDisposable.add(d);
                        qrScannerInit = d;
                    }

                    @Override
                    public void onComplete() {
                        Timber.tag(TAG).d("Completing Initialization of QR-Scanning");
                        qrScannerInit = null;
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Timber.tag(TAG).w(e);
                        finish();
                    }
                });
    }

    @NonNull
    private Disposable startNetworkListener() {
        return NetworkStateTracker.trackCurrentNetwork(this)
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(io.reactivex.rxjava3.schedulers.Schedulers.single())
                .subscribe(networkStateObject -> {
                    NetworkState state = networkStateObject.state;
                    Timber.tag(NET_CHAIN_TAG).d("State=%s", state.name());

                    if (state == NetworkState.AVAILABLE) {

                        Timber.tag(NET_CHAIN_TAG).d("Connection available.");

                        if (isConnecting && connectToHost == null) {
                            Timber.tag(NET_CHAIN_TAG).d("Connecting to mirror.");
                            connToHost(networkStateObject.network);
                        }
                    } else if (state == NetworkState.LOST) {

                        Timber.tag(NET_CHAIN_TAG).d("Lost connection.");
                        if (!retrying && connectToHost != null) connectToHost.dispose();
                    }

                }, throwable -> Timber.tag(NET_CHAIN_TAG).e(throwable, "error"));
    }

    private Completable checkNetworkId() {
        return Completable.create(emitter -> {
            if (wifiManager.getConnectionInfo().getNetworkId() != netId) {
                Timber.tag(NET_ID_CHAIN_TAG)
                      .wtf("Wrong network, looking for %d, found %d -> reconnecting!",
                              netId, wifiManager.getConnectionInfo().getNetworkId());
                retrying = true;
                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();
                emitter.tryOnError(new IllegalStateException());
            } else {
                emitter.onComplete();
            }
        }).retry(5);
        // TODO: somehow this triggers infinitely
    }

    private void connToHost(Network network) {
        checkNetworkId()
                .andThen(Completable.fromRunnable(() -> {
                    isConnecting = false;
                    Timber.tag(HOST_CONNECTION_CHAIN_TAG)
                          .wtf(String.valueOf(wifiManager.getConnectionInfo().getIpAddress()));
                    runOnUiThread(() -> bar.setVisibility(View.VISIBLE));
                }))
                .andThen(bindTrafficToNetwork(network))
                .andThen(pingConfigurationServer(network))
                .andThen(Completable.create(emitter -> {
                    Intent intent = new Intent(QrScannerActivity.this, WebConfigActivity.class);
                    intent.putExtra(WebConfigActivity.EXTRA_WEBPAGE, BuildConfig.MIRROR_SETUP_PAGE);

                    if (networkCheck != null) networkCheck.dispose();

                    io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread().scheduleDirect(() -> {
                        Timber.tag(HOST_CONNECTION_CHAIN_TAG).wtf("Starting WebView!");
                        bar.setVisibility(View.GONE);
                        startActivity(intent);
                    }, 20, TimeUnit.MILLISECONDS);
//                    runOnUiThread(() -> {
//                        Timber.tag(HOST_CONNECTION_CHAIN_TAG).wtf("Starting WebView!");
//                        bar.setVisibility(View.GONE);
//                        startActivity(intent);
//                    });

                    emitter.onComplete();
                }))
//                .andThen(Completable.fromRunnable(() -> { // TODO: this last step should be a flowable
//                    Intent intent = new Intent(QrScannerActivityNew.this, WebConfigActivity.class);
//                    intent.putExtra(WebConfigActivity.EXTRA_WEBPAGE, BuildConfig.MIRROR_SETUP_PAGE);
//
//                    if (networkCheck != null) networkCheck.dispose();
//
//                    runOnUiThread(() -> {
//                        Timber.tag(HOST_CONNECTION_CHAIN_TAG).wtf("Starting WebView!");
//                        bar.setVisibility(View.GONE);
//                        startActivity(intent);
//                    });
//                }))
                .subscribeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .doOnSubscribe(d -> connectToHost = d)
                .doAfterTerminate(() -> {
                    connectToHost = null;
                    retrying = false;
                })
//                .subscribe(() -> {
//                    Timber.tag("Chain").i("Completing connect-to-host chain");
//                }, throwable -> {
//                    Timber.tag("Chain").e(throwable, "Error occurred in chain!");
//                });
//                .doOnLifecycle()
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    protected void onStart() {
                        Timber.tag(HOST_CONNECTION_CHAIN_TAG)
                              .i("Starting connect-to-host chain");
                    }

                    @Override
                    public void onComplete() {
                        Timber.tag(HOST_CONNECTION_CHAIN_TAG)
                              .i("Completing connect-to-host chain");
                        dispose();
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable throwable) {
                        Timber.tag(HOST_CONNECTION_CHAIN_TAG)
                              .e(throwable, "Error occurred in chain!");
                        dispose();
//                        isConnecting = true;
                    }
                });
    }

    private Completable bindTrafficToNetwork(Network network) {
        return Completable.create(emitter -> {
            if (network != null && connMgr.bindProcessToNetwork(network)) {
                emitter.onComplete();
            } else {
                Timber.d("Cannot bind traffic to given network");
                emitter.tryOnError(new IllegalStateException());
            }
        });
    }

    private Completable pingConfigurationServer(Network network) {
        return Completable.create(emitter -> {
            boolean isReachable;

            try {
                InetAddress ping = network.getByName(BuildConfig.MIRROR_SETUP_IP);
                Timber.tag(TAG).wtf("Ping is local address: %B", ping.isAnyLocalAddress());

                long start = System.currentTimeMillis();
                isReachable = ping.isReachable(5000);
                long end = System.currentTimeMillis() - start;
                String ip = Arrays.toString(ping.getAddress());

                Timber.tag(TAG).wtf("IP=%s, isReachable=%B, time=%d", ip, isReachable, end);
            } catch (Exception e) {
                Timber.tag(TAG).wtf(e, "WTF happened?");
                isReachable = false;
            }

            if (isReachable) {
                emitter.onComplete();
            } else {
                emitter.tryOnError(new IllegalStateException());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (codeScanner != null) codeScanner.startPreview();
    }

    @Override
    protected void onDestroy() {
        Timber.d("Running QrScannerActivityNew::onDestroy()");

        // Disposing of all - yet - unfinished Disposables
        compositeDisposable.clear();

        if (qrScannerInit != null) {
            qrScannerInit.dispose();
            qrScannerInit = null;
        }

        if (wifiConnect != null) {
            wifiConnect.dispose();
            wifiConnect = null;
        }

        if (networkCheck != null) {
            networkCheck.dispose();
            networkCheck = null;
        }

        if (codeScanner != null) {
            codeScanner.releaseResources();
            codeScanner = null;
        }

        // Removing all views from underlying View-Group deals with memory-leakage
        if (scannerView != null) {
            scannerView.removeAllViews();
            scannerView = null;
        }

        // Dispose of all started Fragments
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (wifiDialogHelper != null) fragmentTransaction.remove(wifiDialogHelper);
        if (enableWifiDialog != null) fragmentTransaction.remove(enableWifiDialog);
        if (grantPermissionsDialog != null) fragmentTransaction.remove(grantPermissionsDialog);
        if (proceedToConnectDialog != null) fragmentTransaction.remove(proceedToConnectDialog);
        fragmentTransaction.commitNowAllowingStateLoss();

        grantPermissionsDialog = null;
        proceedToConnectDialog = null;
        connMgr = null;
        rxActivityResult = null;
        wifiDialogHelper = null;
        enableWifiDialog = null;
        scannerView = null;
        rxPermission = null;
        codeScanner = null;
        bar = null;
        wifiManager = null;
        networkCheck = null;
        result = null;
        qrScannerInit = null;
        connectToHost = null;
        wifiConnect = null;

        Timber.d("Finished custom part of QrScannerActivityNew::onDestroy()");

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (codeScanner != null) codeScanner.releaseResources();
        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    private Completable connectToWifiNetwork(String ssid, String pw) {
        return Completable.create(emitter -> {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", ssid);
            wifiConfig.preSharedKey = String.format("\"%s\"", pw);
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

            netId = wifiManager.addNetwork(wifiConfig);

            if (netId == -1) {
                emitter.tryOnError(new IllegalStateException());
            } else if (isConnecting = wifiManager.disconnect()) {
                emitter.onComplete();
            } else {
                emitter.tryOnError(new IllegalStateException());
            }
        }).retry(5);

        // TODO: retry connecting
    }

    private Completable startWifiSettings() {
        return Completable.create(emitter ->
            rxActivityResult
                    .start(new Intent(Settings.ACTION_WIFI_SETTINGS))
                    .subscribeOn(Schedulers.trampoline())
                    .subscribe(new io.reactivex.SingleObserver<ActivityResult>() {

                        @Override
                        public void onSubscribe(io.reactivex.disposables.Disposable d) {
                            Timber.tag(TAG).i("RxActivityResult::onSubscribe()");
//                            compositeDisposable.add(d);
                        }

                        @Override
                        public void onSuccess(@NonNull ActivityResult activityResult) {
                            Timber.tag(TAG).i("RxActivityResult::onSuccess()");

                            if (activityResult.getResultCode() == 0 && (wifiManager.isWifiEnabled() ||
                                wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING)) {
                                emitter.onComplete();
                            } else {
                                emitter.tryOnError(new IllegalStateException());
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable throwable) {
                            Timber.tag(TAG).e(throwable, "RxActivityResult::onError()");
                            emitter.tryOnError(throwable);
                        }
                    })
        );
    }

    private Completable showEnableWifiDialog() {
        return Completable.create(emitter ->
            enableWifiDialog
                    .show()
                    .subscribe(new SingleObserver<WifiDialogResult>() {
                        @Override
                        public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull io.reactivex.rxjava3.disposables.Disposable d) {
                            Timber.tag(TAG).i("EnableWifiDialog::onSubscribe()");
                            compositeDisposable.add(d);
                        }

                        @Override
                        public void onSuccess(WifiDialogResult wifiDialogResult) {
                            Timber.tag(TAG).i("EnableWifiDialog::onSuccess()");

                            if (wifiDialogResult == PROCEED_ENABLE_WIFI) {
                                emitter.onComplete();
                            } else {
                                emitter.tryOnError(new IllegalStateException());
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Timber.tag(TAG).e(throwable, "EnableWifiDialog::onError()");
                            emitter.tryOnError(throwable);
                        }
                    })
        );
    }

    private Completable checkWifiAvailability() {
        return Completable.create(emitter -> {
            if (wifiManager.isWifiEnabled()) {
                emitter.onComplete();
            } else {
                emitter.tryOnError(new IllegalStateException());
            }
        });
    }
}
