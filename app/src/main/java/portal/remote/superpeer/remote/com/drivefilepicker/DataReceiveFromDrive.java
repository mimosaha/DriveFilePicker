package portal.remote.superpeer.remote.com.drivefilepicker;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.OpenFileActivityOptions;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import static android.content.ContentValues.TAG;

/**
 * Created by USER03 on 1/3/2018.
 */

public class DataReceiveFromDrive {

    private static DataReceiveFromDrive dataReceiveFromDrive = null;
    private Activity activity;
    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;
    private TaskCompletionSource<DriveId> mOpenItemTaskSource;

    public static final int REQUEST_CODE_SIGN_IN = 10;
    public static final int REQUEST_CODE_OPEN_ITEM = 11;

    private DataReceiveFromDrive() {

    }

    public static DataReceiveFromDrive getInstance() {
        if (dataReceiveFromDrive == null)
            dataReceiveFromDrive = new DataReceiveFromDrive();
        return dataReceiveFromDrive;
    }

    public DataReceiveFromDrive setActivityContext(Activity activityContext) {
        this.activity = activityContext;
        return this;
    }

    public void signIn() {
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(Drive.SCOPE_FILE);
        requiredScopes.add(Drive.SCOPE_APPFOLDER);
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(activity);
        if (signInAccount != null && signInAccount.getGrantedScopes().containsAll(requiredScopes)) {
            initializeDriveClient(signInAccount);
        } else {
            GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestScopes(Drive.SCOPE_FILE)
                            .requestScopes(Drive.SCOPE_APPFOLDER)
                            .setAccountName("mmimosaha@gmail.com")
                            .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(activity, signInOptions);
            activity.startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }
    }

    public void initializeAfterResult(Intent data) {
        Task<GoogleSignInAccount> getAccountTask =
                GoogleSignIn.getSignedInAccountFromIntent(data);
        if (getAccountTask.isSuccessful()) {
            initializeDriveClient(getAccountTask.getResult());
        } else {
            Log.e(TAG, "Sign-in failed.");
        }
    }

    public void openDriveInfo(Intent data) {
        DriveId driveId = data.getParcelableExtra(
                OpenFileActivityOptions.EXTRA_RESPONSE_DRIVE_ID);
        mOpenItemTaskSource.setResult(driveId);
    }

    private void initializeDriveClient(GoogleSignInAccount signInAccount) {
        mDriveClient = Drive.getDriveClient(activity, signInAccount);
        mDriveResourceClient = Drive.getDriveResourceClient(activity, signInAccount);
        onDriveClientReady();
    }

    protected Task<DriveId> pickTextFile() {
        OpenFileActivityOptions openOptions =
                new OpenFileActivityOptions.Builder()
                        .setSelectionFilter(Filters.eq(SearchableField.MIME_TYPE, "image/*"))
                        .setActivityTitle("Select File")
                        .build();
        return pickItem(openOptions);
    }

    private Task<DriveId> pickItem(OpenFileActivityOptions openOptions) {
        mOpenItemTaskSource = new TaskCompletionSource<>();
        mDriveClient
                .newOpenFileActivityIntentSender(openOptions)
                .continueWith(new Continuation<IntentSender, Void>() {
                    @Override
                    public Void then(@NonNull Task<IntentSender> task) throws Exception {
                        activity.startIntentSenderForResult(
                                task.getResult(), REQUEST_CODE_OPEN_ITEM,
                                null,
                                0,
                                0,
                                0);
                        return null;
                    }
                });
        return mOpenItemTaskSource.getTask();
    }

    protected void onDriveClientReady() {
        pickTextFile()
                .addOnSuccessListener(activity,
                        new OnSuccessListener<DriveId>() {
                            @Override
                            public void onSuccess(DriveId driveId) {
                                retrieveContents(driveId.asDriveFile());
                            }
                        })
                .addOnFailureListener(activity, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void retrieveContents(DriveFile file) {

        Task<DriveContents> openFileTask =
                mDriveResourceClient.openFile(file, DriveFile.MODE_READ_ONLY);

        openFileTask
                .continueWithTask(new Continuation<DriveContents, Task<Void>>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                        DriveContents contents = task.getResult();

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(contents.getInputStream()))) {
                            StringBuilder builder = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                builder.append(line).append("\n");
                            }

                            Log.v("MIMO_SAHA::", "Data: " + builder.toString());
                        }
                        return mDriveResourceClient.discardContents(contents);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to read contents", e);
                    }
                });
    }
}
