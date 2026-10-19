package de.baumann.browser.unit;



import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class PermissionUtils {

    public enum PermissionType {
        LOCATION, CAMERA, MICROPHONE
    }

    public static boolean hasPermission(Context context, PermissionType type) {
        String permission = getManifestString(type);
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static String getManifestString(PermissionType type) {
        return switch (type) {
            case LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION;
            case CAMERA -> Manifest.permission.CAMERA;
            case MICROPHONE -> Manifest.permission.RECORD_AUDIO;
        };
    }
}
