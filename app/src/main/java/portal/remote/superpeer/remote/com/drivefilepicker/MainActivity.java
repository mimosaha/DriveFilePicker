package portal.remote.superpeer.remote.com.drivefilepicker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button download;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        download = findViewById(R.id.download);
        DataReceiveFromDrive.getInstance().setActivityContext(this);

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataReceiveFromDrive.getInstance().signIn();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == DataReceiveFromDrive.REQUEST_CODE_SIGN_IN) {

            DataReceiveFromDrive.getInstance().initializeAfterResult(intent);

        } else if (requestCode == DataReceiveFromDrive.REQUEST_CODE_OPEN_ITEM) {

            if (resultCode == RESULT_OK) {
                DataReceiveFromDrive.getInstance().openDriveInfo(intent);
            }
        }
    }
}
