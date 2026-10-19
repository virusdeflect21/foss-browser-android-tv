package de.baumann.browser.activity;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.baumann.browser.R;
import de.baumann.browser.unit.BackupUtils;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;

public class Settings_Backup extends AppCompatActivity {

    private ProgressBar progressBar;
    private Button btnBackupAll;
    private Button btnRestoreAll;
    private Button btnRestoreDb;
    private Button btnBackupDb;
    private Button btnExportBookmarks;
    private Button btnImportBookmarks;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<Intent> createFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        startRunningState();
                        executorService.execute(() -> {
                            boolean success = BackupUtils.backupAllData(Settings_Backup.this, uri);
                            mainHandler.post(() -> {
                                stopRunningState();
                                showToast(success ? getString(R.string.app_done) : getString(R.string.app_error));
                            });
                        });
                    }
                }
            }
    );
    private final ActivityResultLauncher<Intent> openFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        android.view.View rootView = findViewById(android.R.id.content);
                        Snackbar snackbar = Snackbar.make(rootView, R.string.hint_database, Snackbar.LENGTH_SHORT);
                        HelperUnit.makeSnackbarRound(snackbar);
                        snackbar.setAction(this.getString(R.string.app_ok), (v -> {
                            // Dieser Code wird erst nach dem Klick auf OK ausgeführt
                            startRunningState();
                            executorService.execute(() -> {
                                boolean success = BackupUtils.restoreAllData(Settings_Backup.this, uri);
                                mainHandler.post(() -> {
                                    stopRunningState();
                                    if (success) {
                                        showToast(getString(R.string.menu_restart));
                                        btnRestoreAll.postDelayed(() -> BackupUtils.triggerAppRestart(Settings_Backup.this), 1000);
                                    } else {
                                        showToast(getString(R.string.app_error));
                                    }
                                });
                            });
                        }));
                        snackbar.show();
                    }
                }
            }
    );
    private final ActivityResultLauncher<Intent> createDbBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        startRunningState();
                        executorService.execute(() -> {
                            boolean success = BackupUtils.backupDatabasesOnly(Settings_Backup.this, uri);
                            mainHandler.post(() -> { stopRunningState(); showToast(success ? "Datenbank-Backup erfolgreich gespeichert!" : "Fehler beim Datenbank-Backup."); });
                        });
                    }
                }
            }
    );
    private final ActivityResultLauncher<Intent> openDbRestoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        android.view.View rootView = findViewById(android.R.id.content);
                        Snackbar snackbar = Snackbar.make(rootView, R.string.hint_database, Snackbar.LENGTH_SHORT);
                        HelperUnit.makeSnackbarRound(snackbar);
                        snackbar.setAction(this.getString(R.string.app_ok), (v -> {
                            startRunningState();
                            executorService.execute(() -> {
                                boolean success = BackupUtils.restoreDatabasesOnly(Settings_Backup.this, uri);
                                mainHandler.post(() -> {
                                    stopRunningState();
                                    if (success) { showToast(getString(R.string.menu_restart)); btnRestoreDb.postDelayed(() -> BackupUtils.triggerAppRestart(Settings_Backup.this), 1000); }
                                    else { showToast(getString(R.string.app_error)); }
                                });
                            });
                        }));
                        snackbar.show();
                    }
                }
            }
    );
    private final ActivityResultLauncher<Intent> exportBookmarksLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        startRunningState();
                        executorService.execute(() -> {
                            boolean success = BackupUtils.exportBookmarksToUri(Settings_Backup.this, uri);
                            mainHandler.post(() -> {
                                stopRunningState();
                                showToast(success ? getString(R.string.app_done) : getString(R.string.app_error));
                            });
                        });
                    }
                }
            }
    );
    private final ActivityResultLauncher<Intent> importBookmarksLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        android.view.View rootView = findViewById(android.R.id.content);
                        Snackbar snackbar = Snackbar.make(rootView, R.string.hint_database, Snackbar.LENGTH_SHORT);
                        HelperUnit.makeSnackbarRound(snackbar);
                        snackbar.setAction(this.getString(R.string.app_ok), (v -> {
                            startRunningState();
                            executorService.execute(() -> {
                                boolean success = BackupUtils.importBookmarksFromUri(Settings_Backup.this, uri);
                                mainHandler.post(() -> {
                                    stopRunningState();
                                    showToast(success ? getString(R.string.app_done) : getString(R.string.app_error));
                                });
                            });
                        }));
                        snackbar.show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HelperUnit.initTheme(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings_backup);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        progressBar = findViewById(R.id.progress_bar);
        btnBackupAll = findViewById(R.id.btn_backup_all);
        btnRestoreAll = findViewById(R.id.btn_restore_all);
        btnBackupDb = findViewById(R.id.btn_backup_db);
        btnRestoreDb = findViewById(R.id.btn_restore_db);
        btnExportBookmarks = findViewById(R.id.btn_export_bookmarks);
        btnImportBookmarks = findViewById(R.id.btn_import_bookmarks);

        btnBackupAll.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE, "FOSS_Browser_backup_full.zip");
            createFileLauncher.launch(intent);
        });

        btnRestoreAll.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            openFileLauncher.launch(intent);
        });

        btnBackupDb.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/zip")
                    .putExtra(Intent.EXTRA_TITLE, "FOSS_Browser_backup_databases.zip");
            createDbBackupLauncher.launch(intent);
        });

        btnRestoreDb.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/zip");
            openDbRestoreLauncher.launch(intent);
        });

        btnExportBookmarks.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/html");
            intent.putExtra(Intent.EXTRA_TITLE, "FOSS_Browser_backup_bookmarks.html");
            exportBookmarksLauncher.launch(intent);
        });

        btnImportBookmarks.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/html");
            importBookmarksLauncher.launch(intent);
        });
    }

    private void startRunningState() {
        progressBar.setVisibility(View.VISIBLE);
        btnBackupAll.setEnabled(false);
        btnRestoreAll.setEnabled(false);
        btnExportBookmarks.setEnabled(false);
        btnImportBookmarks.setEnabled(false);
        btnBackupDb.setEnabled(false);
        btnRestoreDb.setEnabled(false);
    }

    private void stopRunningState() {
        progressBar.setVisibility(View.GONE);
        btnBackupAll.setEnabled(true);
        btnRestoreAll.setEnabled(true);
        btnExportBookmarks.setEnabled(true);
        btnImportBookmarks.setEnabled(true);
        btnBackupDb.setEnabled(true);
        btnRestoreDb.setEnabled(true);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_help, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) finish();
        else if (menuItem.getItemId() == R.id.menu_help) {
            Uri webpage = Uri.parse("https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki/Backup");
            BrowserUnit.intentURL(this, webpage);
        }
        return true;
    }
}