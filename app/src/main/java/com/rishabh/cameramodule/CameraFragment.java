package com.rishabh.cameramodule;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Flash;
import com.otaliastudios.cameraview.Mode;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.Size;
import com.otaliastudios.cameraview.SizeSelector;
import com.otaliastudios.cameraview.VideoResult;
import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment implements View.OnClickListener {


    CameraView camera;
    private ICameraCallback mHost;

    private ViewGroup controlPanel;

    // To show stuff in the callback
    private long mCaptureTime;


    public CameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


    public interface ICameraCallback {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ICameraCallback) {
            mHost = (ICameraCallback) context;
        } else throw new IllegalStateException("Host must implement ICameraCallback");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        camera = view.findViewById(R.id.camera);

        return view;
    }
    @Override
    public void onViewCreated(@androidx.annotation.NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    private void initViews(View view) {
        camera.setLifecycleOwner(this);
        camera.addCameraListener(new Listener());
        view.findViewById(R.id.btn_camera).setOnClickListener(this);
        view.findViewById(R.id.btn_flash).setOnClickListener(this);
        view.findViewById(R.id.btn_toggle).setOnClickListener(this);
        view.findViewById(R.id.btn_video).setOnClickListener(this);
        view.findViewById(R.id.btn_mode).setOnClickListener(this);
    }

    private void capturePicture() {
        if (camera.getMode() == Mode.VIDEO) {
            message("Can't take HQ pictures while in VIDEO mode.", false);
            return;
        }
        if (camera.isTakingPicture()) return;
        mCaptureTime = System.currentTimeMillis();
        message("Capturing picture...", false);
        camera.takePicture();
    }

    private void captureVideo() {
        if (camera.getMode() == Mode.PICTURE) {
            message("Can't record HQ videos while in PICTURE mode.", false);
            return;
        }
        if (camera.isTakingPicture() || camera.isTakingVideo()) return;
        message("Recording for 5 seconds...", true);
        camera.takeVideo(new File(getActivity().getFilesDir(), "video.mp4"), 5000);
    }

    private void toggleCamera() {
        if (camera.isTakingPicture() || camera.isTakingVideo()) return;
        switch (camera.toggleFacing()) {
            case BACK:
                message("Switched to back camera!", false);
                break;

            case FRONT:
                message("Switched to front camera!", false);
                break;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !camera.isOpened()) {
            camera.open();
        }
    }

    private void message(String content, boolean important) {
        int length = important ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(getActivity(), content, length).show();
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_toggle:
                toggleCamera();
                break;
            case R.id.btn_camera:
                capturePicture();
                //capturePictureSnapshot();
                break;
            case R.id.btn_video:
                captureVideo();
                break;
            case R.id.btn_flash:
                if(camera.getFlash().equals(Flash.AUTO))
                    camera.setFlash(Flash.OFF);
                else if(camera.getFlash().equals(Flash.OFF))
                    camera.setFlash(Flash.ON);
                else if(camera.getFlash().equals(Flash.ON))
                    camera.setFlash(Flash.TORCH);
                else if(camera.getFlash().equals(Flash.TORCH))
                    camera.setFlash(Flash.AUTO);

                ((Button)view).setText(camera.getFlash().name());

                break;
            case R.id.btn_mode:
                if (camera.getMode().equals(Mode.PICTURE)) {
                    camera.setMode(Mode.VIDEO);
                    ((Button)view).setText("Mode - Video");
                } else {
                    camera.setMode(Mode.PICTURE);
                    ((Button)view).setText("Mode - Camera");
                }
                break;
        }
    }


    private class Listener extends CameraListener {

        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {
            /*ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
            for (int i = 0; i < group.getChildCount(); i++) {
                ControlView view = (ControlView) group.getChildAt(i);
                view.onCameraOpened(camera, options);
            }*/
        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
            message("Got CameraException #" + exception.getReason(), true);
        }

        @Override
        public void onPictureTaken(@NonNull PictureResult result) {
            super.onPictureTaken(result);
            if (camera.isTakingVideo()) {
                message("Captured while taking video. Size=" + result.getSize(), false);
                return;
            }

            // This can happen if picture was taken with a gesture.
            long callbackTime = System.currentTimeMillis();
            if (mCaptureTime == 0) mCaptureTime = callbackTime - 300;
            PicturePreviewActivity.setPictureResult(result);
            Intent intent = new Intent(getActivity(), PicturePreviewActivity.class);
            intent.putExtra("delay", callbackTime - mCaptureTime);
            startActivity(intent);
            message("Captured Image", true);
            mCaptureTime = 0;
        }

        @Override
        public void onVideoTaken(@NonNull VideoResult result) {
            super.onVideoTaken(result);
            message("Video Captured ", true);
            VideoPreviewActivity.setVideoResult(result);
            Intent intent = new Intent(getActivity(), VideoPreviewActivity.class);
            startActivity(intent);
        }

    }

    private void capturePictureSnapshot() {
        if (camera.isTakingPicture()) return;
        mCaptureTime = System.currentTimeMillis();
        message("Capturing picture snapshot...", false);
        camera.takePictureSnapshot();
    }
}
