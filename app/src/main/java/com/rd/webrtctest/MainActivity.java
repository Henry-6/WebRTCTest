package com.rd.webrtctest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import org.webrtc.EglBase;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private SurfaceViewRenderer surfaceViewRenderer1;
    private SurfaceViewRenderer surfaceViewRenderer2;

    private EditText ed1;
    private EditText ed2;
    private RadioButton rbPushAudio;
    private RadioButton rbPushVideo;
    private RadioButton rbPlayVideo;
    private RadioButton rbPlayAudio;
    private EglBase mRootEglBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceViewRenderer1 = findViewById(R.id.activity_main_svr_video);
        surfaceViewRenderer2 = findViewById(R.id.activity_main_svr_video1);
        ed1 = findViewById(R.id.ed1);
        ed2 = findViewById(R.id.ed2);
        long time = System.currentTimeMillis() / 1000;
        ed1.setText("https://zlv.runde.pro/index/api/webrtc?app=live&stream=" + time + "&type=push");
        ed2.setText("https://zlv.runde.pro/index/api/webrtc?app=live&stream=" + time + "&type=play");
        rbPushAudio = findViewById(R.id.rbPushAudio);
        rbPushVideo = findViewById(R.id.rbPushVideo);
        rbPlayVideo = findViewById(R.id.rbPlayVideo);
        rbPlayAudio = findViewById(R.id.rbPlayAudio);
        mRootEglBase = EglBase.create();

        //初始化SurfaceViewRenderer
        surfaceViewRenderer1.init(mRootEglBase.getEglBaseContext(), new RendererCommon.RendererEvents() {
            @Override
            public void onFirstFrameRendered() {

            }

            @Override
            public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {

            }
        });
        surfaceViewRenderer1.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
//        surfaceViewRenderer.setMirror(false);
        surfaceViewRenderer1.setEnableHardwareScaler(true);
        surfaceViewRenderer1.setZOrderMediaOverlay(true);

        surfaceViewRenderer2.init(mRootEglBase.getEglBaseContext(), new RendererCommon.RendererEvents() {
            @Override
            public void onFirstFrameRendered() {

            }

            @Override
            public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {

            }
        });

        surfaceViewRenderer2.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
//        surfaceViewRenderer1.setMirror(true);
        surfaceViewRenderer2.setEnableHardwareScaler(true);
        surfaceViewRenderer2.setZOrderMediaOverlay(true);

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
        surfaceViewRenderer1.release();
        surfaceViewRenderer2.release();
        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();
    }

    public void doPush(View view) {
        doPush();
    }

    private WebRtcUtil webRtcUtil1;

    private void doPush() {
        String text = ed1.getEditableText().toString();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(MainActivity.this, "推流地址为空!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (webRtcUtil1 != null) {
            webRtcUtil1.destroy();
        }
        webRtcUtil1 = new WebRtcUtil(MainActivity.this);
        webRtcUtil1.create(mRootEglBase, rbPushVideo.isChecked() ? surfaceViewRenderer1 : null, true, rbPushVideo.isChecked(), text, new WebRtcUtil.WebRtcCallBack(){
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail() {

            }
        });
    }

    public void doPlay(View view) {
        doPlay();
    }

    private WebRtcUtil webRtcUtil2;

    private void doPlay() {
        String text = ed2.getEditableText().toString();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(MainActivity.this, "拉流地址为空!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (webRtcUtil2 != null) {
            webRtcUtil2.destroy();
        }
        webRtcUtil2 = new WebRtcUtil(MainActivity.this);
        webRtcUtil2.create(mRootEglBase, rbPlayVideo.isChecked() ? surfaceViewRenderer2 : null, false, rbPlayVideo.isChecked(), text, new WebRtcUtil.WebRtcCallBack(){
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail() {

            }
        });
    }
}