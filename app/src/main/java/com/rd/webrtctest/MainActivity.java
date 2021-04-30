package com.rd.webrtctest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import org.webrtc.EglBase;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private SurfaceViewRenderer surfaceViewRenderer;

    private EglBase mRootEglBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceViewRenderer = findViewById(R.id.activity_main_svr_video);
        mRootEglBase = EglBase.create();

        //初始化SurfaceViewRenderer
        surfaceViewRenderer.init(mRootEglBase.getEglBaseContext(), new RendererCommon.RendererEvents() {
            @Override
            public void onFirstFrameRendered() {

            }

            @Override
            public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {

            }
        });
        surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        surfaceViewRenderer.setMirror(true);
        surfaceViewRenderer.setEnableHardwareScaler(true);
        surfaceViewRenderer.setZOrderMediaOverlay(true);

        WebRtcUtil webRtcUtil = new WebRtcUtil(MainActivity.this);
        webRtcUtil.create(mRootEglBase, surfaceViewRenderer, "xxxxxx", new WebRtcUtil.WebRtcCallBack(){
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail() {

            }
        });

        XXPermissions.with(this)
                .permission(Permission.CAMERA)
                .permission(Permission.RECORD_AUDIO)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        surfaceViewRenderer.release();
        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();
    }
}